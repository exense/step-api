using Newtonsoft.Json.Linq;
using Step.Functions.IO;
using System;
using System.Collections.Generic;

namespace Step.Handlers.NetHandler
{
    [AttributeUsage(AttributeTargets.Method)]
    public class Keyword : Attribute
    {
        public string name;
        public string description;
        public string schema;
        public string[] properties;
        public string[] optionalProperties;
    }

    public class AbstractKeyword
    {
        public JObject input;

        public Dictionary<string, string> properties;

        public OutputBuilder output;

        public TokenSession session;

        public TokenSession tokenSession;
        
        public virtual bool OnError(Exception e)
        {
            return true;
        }
    }
}
