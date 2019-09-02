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
        private static readonly ILog Logger = LogManager.GetLogger(typeof(KeywordExecutor));
        
        [Serializable]
        public class SerializableOutput
        {
            public string output = "{}";

            public List<Measure> measures = new List<Measure>();

            public List<Attachment> attachments = new List<Attachment>();

            public Error error;
        }

        public class InputObject
        {
            public string handler = "";
            public Input payload = new Input();
            public Dictionary<string, string> properties = new Dictionary<string, string>();
            public int callTimeout;
        }

        Assembly keywordAssembly;
        public string dllPath;

        private Dictionary<string, Assembly> keywordsDLLs = new Dictionary<string, Assembly>();
        private Dictionary<string, bool> dependenciesDLLs = new Dictionary<string, bool>();

        public KeywordExecutor()
        { }

        public List<MethodInfo> GetFunctionMethods()
        {
            if (keywordAssembly == null)
                throw new Exception("A DLL should be loaded with a call to 'Loadkeyword'");

            return keywordAssembly.GetTypes()
                      .SelectMany(t => t.GetMethods())
                      .Where(m => m.GetCustomAttributes(typeof(Keyword), false).Length > 0)
                      .ToList();
        }

        public string GetFunctionName(MethodInfo m)
        {
            Keyword keyword = (Keyword)m.GetCustomAttribute(typeof(Keyword));
            string keywordName = keyword.name;
            return keywordName != null && keywordName.Length > 0 ? keywordName : m.Name;
        }

        public MethodInfo GetFunctionMethodByName(string name)
        {
            return GetFunctionMethods()
                      .First(m =>
                      {
                          Keyword keyword = (Keyword)m.GetCustomAttribute(typeof(Keyword));
                          return GetFunctionName(m) == name;
                      });
        }

        public void Loadkeyword(string path)
        {
            if (!keywordsDLLs.ContainsKey(path))
            {
                Logger.Debug("Loading new keyword assembly " + path + " to the AppDomain " + AppDomain.CurrentDomain.FriendlyName);

                string pdbName = dllPath + "\\" + Path.GetFileNameWithoutExtension(path) + ".pdb";

                if (File.Exists(pdbName))
                {
                    keywordsDLLs[path] = Assembly.Load(File.ReadAllBytes(path), File.ReadAllBytes(pdbName));
                }
                else
                {
                    keywordsDLLs[path] = Assembly.Load(File.ReadAllBytes(path));
                }
            }
            keywordAssembly = keywordsDLLs[path];
        }

        public void LoadDependencies(string path)
        {
            if (!dependenciesDLLs.ContainsKey(path))
            {
                Logger.Debug("Loading new dependencies " + path + " to the AppDomain " + AppDomain.CurrentDomain.FriendlyName);

                ZipArchive z = ZipFile.OpenRead(path);
                foreach (ZipArchiveEntry file in z.Entries)
                {
                    string newFile = dllPath + "\\" + Path.GetFileName(file.FullName);
                    if (file.Name == "") continue;
                    if (!File.Exists(newFile))
                    {
                        file.ExtractToFile(newFile);
                    }
                }
                dependenciesDLLs[path] = true;
            }
        }

        public void LoadDependency(string path)
        {
            if (!dependenciesDLLs.ContainsKey(path))
            {
                Logger.Debug("Loading new dependency " + path + " to the AppDomain " + AppDomain.CurrentDomain.FriendlyName);

                if (!File.Exists(dllPath + "\\" + Path.GetFileName(path)))
                {
                    File.Copy(path, dllPath + "\\" + Path.GetFileName(path), true);
                }
                dependenciesDLLs[path] = true;
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

        public SerializableOutput Handle(string methodName, string keywordInput, Dictionary<string, string> properties, 
            TokenSession tokenReservationSession, TokenSession tokenSession, bool alwaysThrowException = false)
        {
            OutputBuilder outputBuilder = new OutputBuilder();
            InputObject inputObject = JsonConvert.DeserializeObject<InputObject>(keywordInput);

            MethodInfo method = GetFunctionMethodByName(methodName);

            Type type = method.DeclaringType;

            var c = Activator.CreateInstance(type);

            Keyword keyword = method.GetCustomAttribute(typeof(Keyword)) as Keyword;

            List<string> missingProperties = new List<string>();
            if (keyword.properties != null)
            {
                foreach (string val in keyword.properties)
                {
                    if (!properties.ContainsKey(val))
                    {
                        missingProperties.Add(val);
                    }
                }
            }
            if (missingProperties.Count>0)
            {
                outputBuilder.SetBusinessError("The Keyword is missing the following properties '"+
                    string.Join(", ",missingProperties)+"'");
                SerializableOutput outputMsg = new SerializableOutput
                {
                    output = JsonConvert.SerializeObject(outputBuilder.Output),
                    error = outputBuilder.Error,
                    attachments = outputBuilder.Attachments,
                    measures = outputBuilder.MeasureHelper.GetMeasures()
                };

                return outputMsg;
            }

            if (type.IsSubclassOf(typeof(AbstractKeyword)))
            {
                AbstractKeyword script = (AbstractKeyword)c;
                script.input = inputObject.payload.payload;
                script.session = tokenReservationSession;
                script.tokenSession = tokenSession;
                script.output = outputBuilder;
                script.properties = properties;
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
                timeouted = !Monitor.Wait(monitor, TimeSpan.FromMilliseconds(inputObject.callTimeout));
            }

            if (timeouted)
            {
                outputBuilder.Attachments.Add(TakeThreadDump(invokerThread));
                invokerThread.Abort();
                // Wait max 1 sec:
                invokerThread.Join(1000);

                if (aborted)
                {
                    outputBuilder.SetError("Timeout after " + inputObject.callTimeout + " milliseconds. " +
                        "The keyword execution could be interrupted on the agent side. You can increase the call timeout in the configuraiton screen of the keyword");
                }
                else
                {
                    outputBuilder.SetError("Timeout after " + inputObject.callTimeout + " milliseconds. " +
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
                            script.output.Attachments.Add(AttachmentHelper.GenerateAttachmentForException(kw_exception));
                            script.output.SetError("Error while executing " + methodName + " in .NET agent: " +
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
                output = JsonConvert.SerializeObject(outputBuilder.Output),
                error = outputBuilder.Error,
                attachments = outputBuilder.Attachments,
                measures = outputBuilder.MeasureHelper.GetMeasures()
            };

            return outputMessage;
        }
        
        public void LoadAssembly(Assembly assembly)
        {
            this.keywordAssembly = assembly;
        }
    }
}
