using Newtonsoft.Json.Linq;
using Step.Functions.IO;
using System;
using System.Collections.Generic;

namespace Step.Handlers.NetHandler
{
    [AttributeUsage(AttributeTargets.Method)]
    public class Keyword : Attribute
    {
        public string Name { get; set; }
        public string Schema { get; set; }
    }

    public class AbstractKeyword
    {
        public JObject Input { get; set; }

        public Dictionary<string, string> Properties { get; set; }

        public OutputBuilder Output { get; set; }

        public TokenSession Session { get; set; }

        public TokenSession TokenSession { get; set; }

        public virtual bool OnError(Exception e)
        {
            return true;
        }
    }

    /*
    public class Function
    {
        public string Type = "step.plugins.dotnet.DotNetFunction";

        public Dictionary<string, string> Attributes { get; set; }

        public JObject Schema { get; set; }
    }
    */
}
