using Step.Functions.IO;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Reflection;
using System.Text.RegularExpressions;

namespace Step.Handlers.NetHandler
{
    public class KeywordExecutor
    {
        protected List<Assembly> keywordAssemblies = new();
        private readonly string VALIDATE_PROPERTIES = "$validateProperties";

        public void AddKeywordAssembly(Assembly assembly)
        {
            keywordAssemblies.Add(assembly);
        }

        public MethodInfo GetKeywordMethodByName(string name)
        {
            return GetKeywordMethods()
                .FirstOrDefault(m =>
                {
                    Keyword keyword = (Keyword)m.GetCustomAttribute(typeof(Keyword));
                    return GetKeywordName(m) == name;
                });
        }

        public List<MethodInfo> GetKeywordMethods()
        {
            if (keywordAssemblies.Count == 0)
                throw new Exception("No Keyword Assembly has been set. Please define the Keyword Assembly using the method AddKeywordAssembly()");

            List<MethodInfo> result = new();
            keywordAssemblies.ForEach(assembly => result.AddRange(assembly.GetTypes()
                      .SelectMany(t => t.GetMethods())
                      .Where(m => m.GetCustomAttributes(typeof(Keyword), false).Length > 0)
                      .ToList()));
            return result;
        }

        protected static string GetKeywordName(MethodInfo m)
        {
            Keyword keyword = (Keyword)m.GetCustomAttribute(typeof(Keyword));
            string keywordName = keyword.name;
            return keywordName != null && keywordName.Length > 0 ? keywordName : m.Name;
        }

        public Output CallKeyword(Input input, TokenSession tokenSession, Dictionary<string, string> properties, bool alwaysThrowException = false)
        {
            // Create the merged property map containing the input properties and the additional properties
            Dictionary<string, string> mergedProperties = new();
            if (input.properties != null)
            {
                foreach (var prop in input.properties)
                {
                    mergedProperties[prop.Key] = prop.Value;
                }
            }
            foreach (var prop in properties)
            {
                mergedProperties[prop.Key] = prop.Value;
            }

            OutputBuilder outputBuilder = new();
            var methodName = input.function;
            try
            {
                MethodInfo method = GetKeywordMethodByName(methodName);
                if (method == null)
                {
                    outputBuilder.SetError("Unable to find method annoted by 'Keyword' with name == '" + methodName + "'");
                    return outputBuilder.Build();
                }

                Type type = method.DeclaringType;

                Keyword keyword = method.GetCustomAttribute(typeof(Keyword)) as Keyword;

                Dictionary<string, string> keywordProperties;

                if (mergedProperties.ContainsKey(VALIDATE_PROPERTIES))
                {
                    List<string> missingProperties = new();
                    Dictionary<string, string> reducedProperties = new();
                    try
                    {
                        ProcessPropertyKeys(mergedProperties, input, keyword.properties, missingProperties, reducedProperties, true);
                        ProcessPropertyKeys(mergedProperties, input, keyword.optionalProperties, missingProperties, reducedProperties, false);
                        if (missingProperties.Count > 0)
                        {
                            outputBuilder.SetBusinessError("The Keyword is missing the following properties '" +
                                        string.Join(", ", missingProperties) + "'");
                            return outputBuilder.Build();
                        }
                        else
                        {
                            keywordProperties = reducedProperties;
                        }
                    }
                    catch (MissingPlaceholderException e)
                    {
                        outputBuilder.SetBusinessError("The Keyword is missing the following property or input '" + e.placeholder + "'");
                        return outputBuilder.Build();
                    }
                }
                else
                {
                    keywordProperties = mergedProperties;
                }

                dynamic c = Activator.CreateInstance(type);
                if (type.IsSubclassOf(typeof(AbstractKeyword)))
                {
                    AbstractKeyword script = (AbstractKeyword)c;
                    script.input = input.payload;
                    script.session = tokenSession;
                    script.output = outputBuilder;
                    script.properties = keywordProperties;
                }

                try
                {
                    method.Invoke(c, Array.Empty<object>());
                }
                catch (Exception e)
                {
                    CallOnError(c, e, alwaysThrowException);
                }
            }
            catch (Exception e)
            {
                outputBuilder.SetError(e.Message, e);
            }
            return outputBuilder.Build();
        }

        private static void ProcessPropertyKeys(Dictionary<string, string> properties, Input input, string[] requiredPropertyKeys, 
            List<string> missingProperties, Dictionary<string, string> reducedProperties, bool required)
        {
            if (requiredPropertyKeys != null)
            {
                // First try to resolve the place holders
                var resolvedPropertyKeys = new List<string>();
                foreach (string key in requiredPropertyKeys)
                {
                    resolvedPropertyKeys.Add(ReplacePlaceholders(key, properties, input));
                }

                // Then check if all properties exist
                foreach (string val in resolvedPropertyKeys)
                {
                    if (properties.ContainsKey(val))
                    {
                        reducedProperties[val] = properties[val];
                    }
                    else
                    {
                        if (required)
                        {
                            missingProperties.Add(val);
                        }
                    }
                }
            }
        }

        private static String ReplacePlaceholders(String property, Dictionary<String, String> properties, Input input)
        {
            return Regex.Replace(property, "\\{(.+?)\\}", delegate (Match match)
            {
                var matchStr = match.ToString();
                var key = matchStr[1..^1];
                string replacement;

                if (input.payload.ContainsKey(key))
                {
                    replacement = input.payload[key].ToString();
                }
                else
                {
                    if (properties.ContainsKey(key))
                    {
                        replacement = properties[key];
                    }
                    else
                    {
                        throw new MissingPlaceholderException(key);
                    }
                }
                return replacement;
            });

        }

        [Serializable]
        private class MissingPlaceholderException : Exception
        {
            public string placeholder { get; }

            public MissingPlaceholderException(string placeholder) : base()
            {
                this.placeholder = placeholder;
            }
        }

        private static void CallOnError(object c, Exception exception, bool alwaysThrowException)
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

        public static string Interrupt()
        {
            return "Not supported :(";
        }
    }
}
