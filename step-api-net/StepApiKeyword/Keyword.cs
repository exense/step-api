using Step.Functions.IO;
using System;
using System.Collections.Generic;
using System.Text.Json;

namespace Step.Handlers.NetHandler
{
    [AttributeUsage(AttributeTargets.Method)]
    public class Keyword : Attribute
    {
        public string name;
        public string schema;
        public string[] properties;
        public string[] optionalProperties;
    }

    public class AbstractKeyword
    {
        public Dictionary<string, object> input;

        public Dictionary<string, string> properties;

        public OutputBuilder output;

        public TokenSession session;

        public virtual bool OnError(Exception e)
        {
            return true;
        }
    }
}
