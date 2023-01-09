﻿using Newtonsoft.Json.Linq;
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


        /**
         * @param e
         * @return true if the exception passed as argument has to be rethrown.
         */
        public virtual bool OnError(Exception e)
        {
            return true;
        }

        /**
         * Hook called before each keyword call.
         *
         * @param keyword: the keyword to be called
         */
        public virtual void beforeKeyword(MethodInfo keyword) { }

        /**
         * Hook called after each keyword call.
         *
         * @param keyword: the keyword to be called
         */
        public virtual void afterKeyword(MethodInfo keyword) { }
    }
}
