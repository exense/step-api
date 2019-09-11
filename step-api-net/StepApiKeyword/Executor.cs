using log4net;
using Newtonsoft.Json;
using Newtonsoft.Json.Linq;
using Step.Core.Reports;
using Step.Functions.IO;
using Step.Grid;
using Step.Grid.IO;
using System;
using System.Collections.Generic;
using System.IO;
using System.IO.Compression;
using System.Linq;
using System.Reflection;

namespace Step.Handlers.NetHandler
{
    [Serializable]
    public class SerializableFunction
    {
        public string type = "step.plugins.dotnet.DotNetFunction";

        public Dictionary<string, string> attributes;

        public string schema;
    }

    public class Function
    {
        public string type = "step.plugins.dotnet.DotNetFunction";

        public Dictionary<string, string> attributes;

        public JObject schema;
    }

    public class KeywordExecutor : MarshalByRefObject
    {
        protected static readonly ILog logger = LogManager.GetLogger(typeof(KeywordExecutor));

        protected Assembly keywordAssembly;
        private readonly string VALIDATE_PROPERTIES = "$validateProperties";

        private Dictionary<string, Assembly> keywordsDLLs = new Dictionary<string, Assembly>();
        private Dictionary<string, bool> dependenciesDLLs = new Dictionary<string, bool>();

        public class InputObject
        {
            public string handler = "";
            public Input payload = new Input();
            public Dictionary<string, string> properties = new Dictionary<string, string>();
            public int callTimeout;
        }

        [Serializable]
        public class SerializableOutput
        {
            public string output = "{}";

            public List<Measure> measures = new List<Measure>();

            public List<Attachment> attachments = new List<Attachment>();

            public Error error;

            public AgentError agentError;
        }

        public List<SerializableFunction> GetFunctions()
        {
            List<SerializableFunction> functions = new List<SerializableFunction>();
            List<MethodInfo> methods = GetFunctionMethods();

            foreach (MethodInfo m in methods)
            {
                SerializableFunction f = new SerializableFunction
                {
                    attributes = new Dictionary<string, string>()
                };

                Keyword annotation = (Keyword)m.GetCustomAttribute(typeof(Keyword));
                if (annotation.schema != null)
                {
                    try
                    {
                        JObject.Parse(annotation.schema);
                        f.schema = annotation.schema;
                    }
                    catch (Exception e)
                    {
                        throw new Exception("Error while parsing schema from function " + m.Name, e);
                    }
                }
                else
                {
                    f.schema = "{}";
                }

                if (annotation.name != null)
                {
                    f.attributes["name"] = annotation.name;
                }
                else
                {
                    f.attributes["name"] = m.Name;
                }

                functions.Add(f);
            }

            return functions;
        }

        public void LoadAssembly(Assembly assembly)
        {
            keywordAssembly = assembly;
        }

        public void LoadKeyword(string path)
        {
            if (!keywordsDLLs.ContainsKey(path))
            {
                logger.Debug("Loading new keyword assembly " + path + " to the AppDomain " + AppDomain.CurrentDomain.FriendlyName);

                if (Path.GetExtension(path).ToLower() != ".dll")
                {
                    throw new Exception("Unknow extention. Should be a folder, a zip file, a dll or a pbd, was " +
                           Path.GetExtension(path).ToLower());
                }
                string pdbName = AppDomain.CurrentDomain.DynamicDirectory + "\\" + Path.GetFileNameWithoutExtension(path) + ".pdb";

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
                logger.Debug("Loading new dependencies " + path + " to the AppDomain " + AppDomain.CurrentDomain.FriendlyName);

                if (Directory.Exists(path))
                {
                    foreach (string fileName in Directory.EnumerateFiles(path))
                    {
                        string newFile = AppDomain.CurrentDomain.DynamicDirectory + "\\" + Path.GetFileName(fileName);
                        if (fileName == "") continue;
                        if (!File.Exists(newFile))
                        {
                            File.Copy(fileName, AppDomain.CurrentDomain.DynamicDirectory + "\\" + Path.GetFileName(fileName), true);
                        }
                    }
                }
                else
                {
                    switch (Path.GetExtension(path).ToLower())
                    {
                        case ".zip":
                            ZipArchive z = ZipFile.OpenRead(path);
                            foreach (ZipArchiveEntry file in z.Entries)
                            {
                                string newFile = AppDomain.CurrentDomain.DynamicDirectory + "\\" + Path.GetFileName(file.FullName);
                                if (file.Name == "") continue;
                                if (!File.Exists(newFile))
                                {
                                    file.ExtractToFile(newFile);
                                }
                            }
                            break;
                        case ".pdb":
                        case ".dll":
                            if (!File.Exists(AppDomain.CurrentDomain.DynamicDirectory + "\\" + Path.GetFileName(path)))
                            {
                                File.Copy(path, AppDomain.CurrentDomain.DynamicDirectory + "\\" + Path.GetFileName(path), true);
                            }
                            break;
                        default:
                            throw new Exception("Unknow extention. Should be a folder, a zip file, a dll or a pbd, was " +
                                Path.GetExtension(path).ToLower());
                    }
                }
                dependenciesDLLs[path] = true;
            }
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

        protected List<MethodInfo> GetFunctionMethods()
        {
            if (keywordAssembly == null)
                throw new Exception("A DLL should be loaded with a call to 'Loadkeyword'");

            return keywordAssembly.GetTypes()
                      .SelectMany(t => t.GetMethods())
                      .Where(m => m.GetCustomAttributes(typeof(Keyword), false).Length > 0)
                      .ToList();
        }

        protected string GetFunctionName(MethodInfo m)
        {
            Keyword keyword = (Keyword)m.GetCustomAttribute(typeof(Keyword));
            string keywordName = keyword.name;
            return keywordName != null && keywordName.Length > 0 ? keywordName : m.Name;
        }

        public SerializableOutput CallFunction(string methodName, string keywordInput, Dictionary<string, string> properties,
            TokenSession tokenReservationSession, TokenSession tokenSession, bool alwaysThrowException = false)
        {
            OutputBuilder outputBuilder = new OutputBuilder();
            try
            {
                InputObject inputObject = JsonConvert.DeserializeObject<InputObject>(keywordInput);

                MethodInfo method = GetFunctionMethodByName(methodName);

                Type type = method.DeclaringType;

                Keyword keyword = method.GetCustomAttribute(typeof(Keyword)) as Keyword;

                List<string> missingProperties = new List<string>();
                Dictionary<string, string> keywordProperties = new Dictionary<string, string>();
                if (properties.ContainsKey(VALIDATE_PROPERTIES))
                {
                    if (keyword.properties != null)
                    {
                        foreach (string val in keyword.properties)
                        {
                            if (!properties.ContainsKey(val))
                            {
                                missingProperties.Add(val);
                            }
                            else
                            {
                                keywordProperties[val] = properties[val];
                            }
                        }
                    }
                    if (missingProperties.Count > 0)
                    {
                        outputBuilder.SetBusinessError("The Keyword is missing the following properties '" +
                                    string.Join(", ", missingProperties) + "'");
                        return new SerializableOutput
                        {
                            output = JsonConvert.SerializeObject(outputBuilder.output),
                            error = outputBuilder.error,
                            attachments = outputBuilder.attachments,
                            measures = outputBuilder.measureHelper.GetMeasures()
                        };
                    }
                }
                else
                {
                    keywordProperties = properties;
                }

                var c = Activator.CreateInstance(type);
                if (type.IsSubclassOf(typeof(AbstractKeyword)))
                {
                    AbstractKeyword script = (AbstractKeyword)c;
                    script.input = inputObject.payload.payload;
                    script.session = tokenReservationSession;
                    script.tokenSession = tokenSession;
                    script.output = outputBuilder;
                    script.properties = keywordProperties;
                }

                try
                {
                    method.Invoke(c, new object[] { });
                }
                catch (Exception e)
                {
                    CallOnError(c, e, methodName, alwaysThrowException);
                }

                SerializableOutput outputMessage = new SerializableOutput
                {
                    output = JsonConvert.SerializeObject(outputBuilder.output),
                    error = outputBuilder.error,
                    attachments = outputBuilder.attachments,
                    measures = outputBuilder.measureHelper.GetMeasures()
                };
                return outputMessage;
            }
            catch (Exception e)
            {
                SerializableOutput outputMessage = new SerializableOutput();

                outputMessage.agentError = new AgentError(AgentErrorCode.UNEXPECTED);
                outputMessage.attachments.Add(AttachmentHelper.GenerateAttachmentForException(e));
                return outputMessage;
            }
        }

        private void CallOnError(object c, Exception exception, string methodName, bool alwaysThrowException)
        {
            if (c.GetType().IsSubclassOf(typeof(AbstractKeyword)))
            {
                AbstractKeyword script = (AbstractKeyword)c;
                bool throwException = script.OnError(exception);
                if (throwException || alwaysThrowException)
                {
                    Exception cause = (exception.InnerException != null) ? exception.InnerException : exception;

                    script.output.attachments.Add(AttachmentHelper.GenerateAttachmentForException(cause));
                    script.output.SetError(cause.Message, cause);
                }
            }
        }
    }
}
