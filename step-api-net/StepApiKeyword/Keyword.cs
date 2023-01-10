using Newtonsoft.Json.Linq;
using Step.Functions.IO;
using System;
using System.Collections.Generic;
using System.Reflection;

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
        public int timeout = 180000;
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

        /**
         * Hook called before each keyword call.
         *
         * @param keyword: the keyword to be called
         */
        public virtual void BeforeKeyword(MethodInfo keyword) { }

        /**
         * Hook called after each keyword call.
         *
         * @param keyword: the keyword to be called
         */
        public virtual void AfterKeyword(MethodInfo keyword) { }
    }
}
