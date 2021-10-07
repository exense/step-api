using Newtonsoft.Json.Linq;
using Step.Functions.IO;
using System;
using System.Collections.Generic;
using System.Linq;

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
            contextProperties.ToList().ForEach(x => properties[x.Key] = x.Value);

            KeywordExecutor executor = new KeywordExecutor();

            foreach (Type type in keywordClasses)
            {
                executor.AddKeywordAssembly(type.Assembly);
            }
            var input = new Input
            {
                function = function,
                payload = JObject.Parse(inputJson)
            };
            return executor.CallKeyword(input, session, session, properties, false);
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
