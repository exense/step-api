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
        /// Hook called when an exception is thrown by a keyword or by the BeforeKeyword 
        /// and AfterKeyword hooks
        /// </summary>
        /// <param name="e">the exception thrown</param>
        /// <returns>return true if the exception passed as argument has to be rethrown</returns>
        public virtual bool OnError(Exception e)
        {
            return true;
        }

        /// <summary>
        /// Hook called before each keyword call.
        /// If an error is thrown by this function, nor the keyword nor
        /// the afterKeyword hook will be called(but onError will be)
        /// </summary>
        /// <param name="KeywordName">the name of the keyword.  Will be the function name if annotation.name is empty </param>
        /// <param name="Annotation">the annotation of the called keyword</param>
        public virtual void BeforeKeyword(String KeywordName, Keyword Annotation) { }

        /// <summary>
        /// Hook called after each keyword call.
        /// If an error is thrown by the keyword or the beforeKeyword hook,
        /// the afterKeyword hook will be called with hadError to true
        /// </summary>
        /// <param name="KeywordName">the name of the keyword.  Will be the function name if annotation.name is empty </param>
        /// <param name="Annotation">the annotation of the called keyword</param>
        /// <param name="HadError">if an error occurred</param>
        public virtual void AfterKeyword(String KeywordName, Keyword Annotation, bool HadError) { }
    }
}
