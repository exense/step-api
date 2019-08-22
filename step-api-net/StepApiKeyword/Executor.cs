using log4net;
using Newtonsoft.Json;
using Step.Core.Reports;
using Step.Functions.IO;
using Step.Grid.IO;
using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.IO;
using System.IO.Compression;
using System.Linq;
using System.Reflection;
using System.Text;
using System.Threading;

namespace Step.Handlers.NetHandler
{   
    public class KeywordExecutor : MarshalByRefObject
    {
        [Serializable]
        public class SerializableOutput
        {
            public string Output = "{}";

            public List<Measure> Measures = new List<Measure>();

            public List<Attachment> Attachments = new List<Attachment>();

            public Error Error;
        }

        public class InputObject
        {
            public string Handler = "";
            public Input Payload = new Input();
            public Dictionary<string, string> Properties = new Dictionary<string, string>();
            public int CallTimeout { get; set; }
        }

        private static readonly ILog Logger = LogManager.GetLogger(typeof(KeywordExecutor));

        Assembly KeywordAssembly;
        public string DllPath;

        private Dictionary<string, Assembly> KeywordsDLLs = new Dictionary<string, Assembly>();
        private Dictionary<string, bool> DependenciesDLLs = new Dictionary<string, bool>();

        public KeywordExecutor()
        { }

        public List<MethodInfo> GetFunctionMethods(Assembly assembly)
        {
            return assembly.GetTypes()
                      .SelectMany(t => t.GetMethods())
                      .Where(m => m.GetCustomAttributes(typeof(Keyword), false).Length > 0)
                      .ToList();
        }

        private string GetFunctionName(MethodInfo m)
        {
            Keyword keyword = (Keyword)m.GetCustomAttribute(typeof(Keyword));
            string keywordName = keyword.Name;
            return keywordName != null && keywordName.Length > 0 ? keywordName : m.Name;
        }

        public MethodInfo GetFunctionMethodByName(Assembly assembly, string name)
        {
            return GetFunctionMethods(assembly)
                      .First(m =>
                      {
                          Keyword keyword = (Keyword)m.GetCustomAttribute(typeof(Keyword));
                          return GetFunctionName(m) == name;
                      });
        }

        public void Loadkeyword(string path)
        {
            if (!KeywordsDLLs.ContainsKey(path))
            {
                Logger.Debug("Loading new keyword assembly " + path + " to the AppDomain " + AppDomain.CurrentDomain.FriendlyName);

                string pdbName = DllPath + "\\" + Path.GetFileNameWithoutExtension(path) + ".pdb";

                if (File.Exists(pdbName))
                {
                    KeywordsDLLs[path] = Assembly.Load(File.ReadAllBytes(path), File.ReadAllBytes(pdbName));
                }
                else
                {
                    KeywordsDLLs[path] = Assembly.Load(File.ReadAllBytes(path));
                }
            }
            KeywordAssembly = KeywordsDLLs[path];
        }

        public void LoadDependencies(string path)
        {
            if (!DependenciesDLLs.ContainsKey(path))
            {
                Logger.Debug("Loading new dependencies " + path + " to the AppDomain " + AppDomain.CurrentDomain.FriendlyName);

                ZipArchive z = ZipFile.OpenRead(path);
                foreach (ZipArchiveEntry file in z.Entries)
                {
                    string newFile = DllPath + "\\" + Path.GetFileName(file.FullName);
                    if (file.Name == "") continue;
                    if (!File.Exists(newFile))
                    {
                        file.ExtractToFile(newFile);
                    }
                }
                DependenciesDLLs[path] = true;
            }
        }

        public void LoadDependency(string path)
        {
            if (!DependenciesDLLs.ContainsKey(path))
            {
                Logger.Debug("Loading new dependency " + path + " to the AppDomain " + AppDomain.CurrentDomain.FriendlyName);

                if (!File.Exists(DllPath + "\\" + Path.GetFileName(path)))
                {
                    File.Copy(path, DllPath + "\\" + Path.GetFileName(path), true);
                }
                DependenciesDLLs[path] = true;
            }
        }

        private Attachment TakeThreadDump(Thread thread)
        {
#pragma warning disable CS0618 // Type or member is obsolete
            thread.Suspend();
            StackTrace trace = new StackTrace(thread, true);
            thread.Resume();
#pragma warning restore CS0618

            return AttachmentHelper.GenerateAttachmentFromByteArray(Encoding.ASCII.GetBytes(trace.ToString()),
                "stacktrace_before_interruption.log");
        }

        public SerializableOutput Handle(string methodName, string keywordInput, Dictionary<string, string> properties, TokenSession tokenReservationSession, TokenSession tokenSession, bool alwaysThrowException = false)
        {
            OutputBuilder outputBuilder = new OutputBuilder();
            InputObject inputObject = JsonConvert.DeserializeObject<InputObject>(keywordInput);

            MethodInfo method = GetFunctionMethodByName(KeywordAssembly, methodName);

            Type type = method.DeclaringType;

            var c = Activator.CreateInstance(type);

            if (type.IsSubclassOf(typeof(AbstractKeyword)))
            {
                AbstractKeyword script = (AbstractKeyword)c;
                script.Input = inputObject.Payload.Payload;
                script.Session = tokenReservationSession;
                script.TokenSession = tokenSession;
                script.Output = outputBuilder;
                script.Properties = properties;
            }

            object monitor = new object();
            bool aborted = false;
            Exception kw_exception = null;

            Thread invokerThread = new Thread(() =>
            {
                try
                {
                    method.Invoke(c, new object[] { });
                }
                catch (ThreadAbortException)
                {
                    aborted = true;
                }
                catch (Exception e)
                {
                    kw_exception = e;
                }
                finally
                {
                    lock (monitor)
                    {
                        Monitor.Pulse(monitor);
                    }
                }
            });
            bool timeouted = false;
            lock (monitor)
            {
                invokerThread.Start();
                timeouted = !Monitor.Wait(monitor, TimeSpan.FromMilliseconds(inputObject.CallTimeout));
            }

            if (timeouted)
            {
                outputBuilder.Attachments.Add(TakeThreadDump(invokerThread));
                invokerThread.Abort();
                // Wait max 1 sec:
                invokerThread.Join(1000);

                if (aborted)
                {
                    outputBuilder.SetError("Timeout after " + inputObject.CallTimeout + " milliseconds. " +
                        "The keyword execution could be interrupted on the agent side. You can increase the call timeout in the configuraiton screen of the keyword");
                }
                else
                {
                    outputBuilder.SetError("Timeout after " + inputObject.CallTimeout + " milliseconds. " +
                        "WARNING: The keyword execution couldn't be interrupted on the agent side. You can increase the call timeout in the configuraiton screen of the keyword");
                }
            }
            else
            {
                invokerThread.Join(1000);

                if (kw_exception != null)
                {

                    if (type.IsSubclassOf(typeof(AbstractKeyword)))
                    {
                        AbstractKeyword script = (AbstractKeyword)c;
                        bool throwException = script.OnError(kw_exception);
                        if ((!throwException) && (!alwaysThrowException))
                        {
                            script.Output.Attachments.Add(AttachmentHelper.GenerateAttachmentForException(kw_exception));
                            script.Output.SetError("Error while executing " + methodName + " in .NET agent: " +
                                (kw_exception.GetBaseException() != null ?
                                    kw_exception.GetBaseException().Message :
                                    kw_exception.Message));
                        }
                        else
                        {
                            throw new Exception("Unexpected error when executing " + methodName + " in .NET agent: ", kw_exception);
                        }
                    }
                    else
                    {
                        throw new Exception("Unexpected error when executing " + methodName + " in .NET agent: ", kw_exception);
                    }
                }
            }
            SerializableOutput outputMessage = new SerializableOutput
            {
                Output = JsonConvert.SerializeObject(outputBuilder.Output),
                Error = outputBuilder.Error,
                Attachments = outputBuilder.Attachments,
                Measures = outputBuilder.MeasureHelper.GetMeasures()
            };

            return outputMessage;
        }
        
        public void LoadAssembly(Assembly assembly)
        {
            this.KeywordAssembly = assembly;
        }
    }
}
