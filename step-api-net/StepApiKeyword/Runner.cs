using Step.Functions.IO;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text.Json;

namespace Step.Handlers.NetHandler
{
    public class ExecutionContext
    {
        private readonly TokenSession session = new();
        private readonly Dictionary<string, string> contextProperties = new();
        private readonly Type[] keywordClasses;

        public ExecutionContext(Dictionary<string, string> contextProperties, params Type[] keywordClasses)
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

            KeywordExecutor executor = new();

            foreach (Type type in keywordClasses)
            {
                executor.AddKeywordAssembly(type.Assembly);
            }
            var input = new Input
            {
                function = function,
                payload = JsonSerializer.Deserialize<Dictionary<string, object>>(inputJson.ToString())
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
            return GetExecutionContext(new Dictionary<string, string>(), keywordClasses);
        }

        public static ExecutionContext GetExecutionContext(Dictionary<string, string> properties, params Type[] keywordClasses)
        {
            return new ExecutionContext(properties, keywordClasses);
        }
    }
}
