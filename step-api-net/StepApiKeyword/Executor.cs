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
    public class KeywordExecutor
    {
        protected static readonly ILog logger = LogManager.GetLogger(typeof(KeywordExecutor));

        protected List<Assembly> keywordAssemblies = new List<Assembly>();
        private readonly string VALIDATE_PROPERTIES = "$validateProperties";

        public List<Function> GetFunctions()
        {
            List<Function> functions = new List<Function>();
            List<MethodInfo> methods = GetFunctionMethods();

            foreach (MethodInfo m in methods)
            {
                Function f = new Function
                {
                    attributes = new Dictionary<string, string>()
                };

                Keyword annotation = (Keyword)m.GetCustomAttribute(typeof(Keyword));
                if (annotation.schema != null)
                {
                    try
                    {
                        f.schema = JObject.Parse(annotation.schema);
                    }
                    catch (Exception e)
                    {
                        throw new Exception("Error while parsing schema from function " + m.Name, e);
                    }
                }
                else
                {
                    f.schema = new JObject();
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
            keywordAssemblies.Add(assembly);
        }

        public MethodInfo GetFunctionMethodByName(string name)
        {
            return GetFunctionMethods()
                        .FirstOrDefault(m =>
                        {
                            Keyword keyword = (Keyword)m.GetCustomAttribute(typeof(Keyword));
                            return GetFunctionName(m) == name;
                        });
        }

        protected List<MethodInfo> GetFunctionMethods()
        {
            if (keywordAssemblies.Count == 0)
                throw new Exception("A DLL should be loaded with a call to 'Loadkeyword'");

            List<MethodInfo> result = new List<MethodInfo>();
            keywordAssemblies.ForEach(assembly=> result.AddRange(assembly.GetTypes()
                      .SelectMany(t => t.GetMethods())
                      .Where(m => m.GetCustomAttributes(typeof(Keyword), false).Length > 0)
                      .ToList()));
            return result;
        }

        protected string GetFunctionName(MethodInfo m)
        {
            Keyword keyword = (Keyword)m.GetCustomAttribute(typeof(Keyword));
            string keywordName = keyword.name;
            return keywordName != null && keywordName.Length > 0 ? keywordName : m.Name;
        }

        public Output CallFunction(Input input, TokenSession tokenReservationSession, TokenSession tokenSession, Dictionary<string, string> properties, bool alwaysThrowException = false)
        {
            // Create the merged property map containing the input properties and the additional properties
            Dictionary<string, string> mergedProperties = new Dictionary<string, string>();
            if(input.properties != null)
            {
                foreach (var prop in input.properties)
                {
                    mergedProperties.Add(prop.Key, prop.Value);
                }
            }
            foreach (var prop in properties)
            {
                mergedProperties.Add(prop.Key, prop.Value);
            }

            OutputBuilder outputBuilder = new OutputBuilder();
            var methodName = input.function;
            try
            {
                MethodInfo method = GetFunctionMethodByName(methodName);
                if(method == null)
                {
                    outputBuilder.SetError("Unable to find method annoted by 'Keyword' with name == '" + methodName + "'");
                    return outputBuilder.Build();
                }

                Type type = method.DeclaringType;

                Keyword keyword = method.GetCustomAttribute(typeof(Keyword)) as Keyword;

                List<string> missingProperties = new List<string>();
                Dictionary<string, string> keywordProperties = new Dictionary<string, string>();
                
                if (mergedProperties.ContainsKey(VALIDATE_PROPERTIES))
                {
                    if (keyword.properties != null)
                    {
                        foreach (string val in keyword.properties)
                        {
                            if (!mergedProperties.ContainsKey(val))
                            {
                                missingProperties.Add(val);
                            }
                            else
                            {
                                keywordProperties[val] = mergedProperties[val];
                            }
                        }
                    }
                    if (missingProperties.Count > 0)
                    {
                        outputBuilder.SetBusinessError("The Keyword is missing the following properties '" +
                                    string.Join(", ", missingProperties) + "'");
                        return outputBuilder.Build();
                    }
                }
                else
                {
                    keywordProperties = mergedProperties;
                }

                var c = Activator.CreateInstance(type);
                if (type.IsSubclassOf(typeof(AbstractKeyword)))
                {
                    AbstractKeyword script = (AbstractKeyword)c;
                    script.input = input.payload;
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
            }
            catch (Exception e)
            {
                outputBuilder.SetError(e.Message, e);
            }
            return outputBuilder.Build();
        }

        private void CallOnError(object c, Exception exception, string methodName, bool alwaysThrowException)
        {
            if (c.GetType().IsSubclassOf(typeof(AbstractKeyword)))
            {
                AbstractKeyword script = (AbstractKeyword)c;
                bool throwException = script.OnError(exception);
                if (throwException || alwaysThrowException)
                {
                    Exception cause = exception.InnerException ?? exception;

                    script.output.SetError(cause.Message, cause);
                }
            }
        }
    }
}
