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
                catch (Exception)
                {
                }
            }
        }

        public void Close()
        {
            attributes.ToList().ForEach(p =>
            {
                CloseIfCloseable(p.Value);
            });
            attributes.Clear();
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
