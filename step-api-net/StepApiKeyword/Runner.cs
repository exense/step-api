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
        private TokenSession Session = new TokenSession();
        private readonly Dictionary<string, string> ContextProperties = new Dictionary<string, string>();
        private readonly Assembly Assembly;
        private readonly Type Type;

        public ExecutionContext(Type Type, Dictionary<string, string> ContextProperties, bool ThrowExceptionOnError)
        {
            this.Type = Type;
            this.Assembly = Type.Assembly;
            this.ContextProperties = ContextProperties;

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
            properties.ToList().ForEach(x => ContextProperties[x.Key] = x.Value);

            KeywordExecutor invoker = new KeywordExecutor();
            invoker.LoadAssembly(this.Assembly);
            KeywordExecutor.SerializableOutput serializedOutput = invoker.Handle(function, "{Payload: {Payload:" + inputJson + "}, CallTimeout:60000}", 
                properties, Session, Session, false);

            return new Output
            {
                Payload = (JObject)JsonConvert.DeserializeObject(serializedOutput.Output),
                Attachments = serializedOutput.Attachments,
                Measures = serializedOutput.Measures,
                Error = serializedOutput.Error
            };
        }

        public void Close()
        {
            Session.Close();
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
