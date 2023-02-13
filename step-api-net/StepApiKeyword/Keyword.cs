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
        // keyword timeout in milliseconds:
        public int timeout = 180000;
    }

    public class AbstractKeyword
    {
        public JObject input;

        public Dictionary<string, string> properties;

        public OutputBuilder output;

        public TokenSession session;

        public TokenSession tokenSession;

        /// <summary>
        /// Hook called when an exception is thrown by a keyword or by the BeforeKeyword hooks
        /// </summary>
        /// <param name="e">the exception thrown</param>
        /// <returns>return true if the exception passed as argument has to be rethrown</returns>
        public virtual bool OnError(Exception e)
        {
            return true;
        }

        /// <summary>
        /// Hook called before each keyword call.
        /// If an exception is thrown by this method, the keyword won't be executed 
        /// (but AfterKeyword and OnError will)
        /// </summary>
        /// <param name="KeywordName">the name of the keyword.  Will be the function name if annotation.name is empty </param>
        /// <param name="Annotation">the annotation of the called keyword</param>
        public virtual void BeforeKeyword(String KeywordName, Keyword Annotation) { }

        /// <summary>
        /// Hook called after each keyword call.
        /// This method is always called. If an exception is thrown by the keyword or the BeforeKeyword hook,
        /// this method is called after the OnError hook.
        /// </summary>
        /// <param name="KeywordName">the name of the keyword.  Will be the function name if annotation.name is empty </param>
        /// <param name="Annotation">the annotation of the called keyword</param>
        public virtual void AfterKeyword(String KeywordName, Keyword Annotation) { }
    }
}
