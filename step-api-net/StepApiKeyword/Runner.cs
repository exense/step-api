using Newtonsoft.Json;
using Newtonsoft.Json.Linq;
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
        private readonly Assembly assembly;
        private readonly Type type;

        public ExecutionContext(Type type, Dictionary<string, string> contextProperties, bool throwExceptionOnError)
        {
            this.type = type;
            this.assembly = type.Assembly;
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

            KeywordExecutor invoker = new KeywordExecutor();
            invoker.LoadAssembly(this.assembly);
            KeywordExecutor.SerializableOutput serializedOutput = 
                invoker.Handle(function, "{Payload: {Payload:" + inputJson + "}, CallTimeout:60000}", 
                    properties, session, session, false);

            return new Output
            {
                payload = (JObject)JsonConvert.DeserializeObject(serializedOutput.output),
                attachments = serializedOutput.attachments,
                measures = serializedOutput.measures,
                error = serializedOutput.error
            };
        }

        public void Close()
        {
            session.Close();
        }
    }

    public class KeywordRunner
    {
        public static ExecutionContext GetExecutionContext(Type keywordClass)
        {
            return GetExecutionContext(new Dictionary<string,string>(), keywordClass);
        }

        public static ExecutionContext GetExecutionContext(Dictionary<string, string> properties, Type keywordClass)
        {
            return new ExecutionContext(keywordClass, properties, true);
        }
    }
}
