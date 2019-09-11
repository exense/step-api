using Newtonsoft.Json;
using Newtonsoft.Json.Linq;
using Step.Core.Reports;
using Step.Functions.IO;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Reflection;

namespace Step.Handlers.NetHandler
{
    public class ExecutionContext
    {
        private TokenSession session = new TokenSession();
        private readonly Dictionary<string, string> contextProperties = new Dictionary<string, string>();
        private readonly Type[] keywordClasses;

        public ExecutionContext(Dictionary<string, string> contextProperties, bool throwExceptionOnError, params Type[] keywordClasses)
        {
            this.keywordClasses = keywordClasses;
            this.contextProperties = contextProperties;
        }

        public Output Run(string function)
        {
            return Run(function, "{}");
        }

        public Output Run(string function, string inputJson)
        {
            return Run(function, inputJson, new Dictionary<string, string>());
        }

        public Output Run(string function, string inputJson, Dictionary<string, string> properties)
        {
            properties.ToList().ForEach(x => contextProperties[x.Key] = x.Value);

            KeywordExecutor executor = new KeywordExecutor();

            foreach (Type type in keywordClasses)
            {
                executor.LoadAssembly(type.Assembly);
                try {
                    executor.GetFunctionMethodByName(function);

                    KeywordExecutor.SerializableOutput output = executor.CallFunction(function, "{payload: {payload:" + inputJson + "}, callTimeout:60000}",
                            contextProperties, session, session, false);

                    return new Output
                    {
                        payload = (JObject)JsonConvert.DeserializeObject(output.output),
                        attachments = output.attachments,
                        measures = output.measures,
                        error = output.error
                    };
                } catch (InvalidOperationException) { }
            }
            return new Output
            {
                error = new Error(ErrorType.TECHNICAL, "Could not find keyword named '"+function+"'")
            };
        }

        public void Close()
        {
            session.Close();
        }
    }

    public class KeywordRunner
    {
        public static ExecutionContext GetExecutionContext(params Type[] keywordClasses)
        {
            return GetExecutionContext(new Dictionary<string,string>(), keywordClasses);
        }

        public static ExecutionContext GetExecutionContext(Dictionary<string, string> properties, params Type[] keywordClasses)
        {
            return new ExecutionContext(properties, true, keywordClasses);
        }
    }
}
