using log4net;
using System;
using System.Collections.Generic;
using System.Linq;

namespace Step.Functions.IO
{
    public interface ICloseable
    {
        void Close();
    }

    public class TokenSession : MarshalByRefObject
    {
        // infinite lifetime
        public override object InitializeLifetimeService()
        {
            return null;
        }
        
        protected static readonly ILog logger = LogManager.GetLogger(typeof(TokenSession));

        private Dictionary<string, object> attributes = new Dictionary<string, object>();

        public virtual Object Get(string arg0)
        {
            attributes.TryGetValue(arg0, out Object o);
            return o;
        }

        public virtual Object Put(string arg0, Object arg1)
        {
            Object previous = Get(arg0);
            CloseIfCloseable(previous);
            return attributes[arg0] = arg1;
        }

        protected void CloseIfCloseable(Object o)
        {
            if (o != null && o is ICloseable)
            {
                try
                {
                    ((ICloseable)o).Close();
                }
                catch (Exception e)
                {
                    logger.Error("Unexpected error when closing a session object.",e);
                }
            }
        }

        public void Close()
        {
            try
            {
                attributes.ToList().ForEach(p =>
                {
                    CloseIfCloseable(p.Value);
                });
            }
            finally
            {
                attributes.Clear();
            }
        }
    }

    public class UnusableTokenSession : TokenSession
    {
        public override Object Get(string arg0)
        {
            throw UnusableSessionException();
        }

        public override Object Put(string arg0, Object arg1)
        {
            throw UnusableSessionException();
        }

        private Exception UnusableSessionException()
        {
            // TODO use error codes instead of error messages
            return new Exception("Session object unreachable. " +
                "Wrap your keywords with a Session node in your test plan in order to make the session object available.");
        }
    }
}
