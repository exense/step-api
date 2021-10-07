
using System;
using System.Collections.Generic;

namespace Step.Core.Reports
{
    [Serializable]
    public enum ErrorType
    {
        TECHNICAL,
        BUSINESS
    }

    [Serializable]
    public class Error
    {
        public ErrorType type = ErrorType.TECHNICAL;

        public string layer;

        public string msg;

        public int code;

        public bool root;

        public Error(ErrorType type, string message) : this(type, null, message, 0, true)
        { }
        public Error(ErrorType type, string message, int code) : this(type, null, message, code, true)
        { }

        public Error(ErrorType type, string layer, string msg, int code, bool root)
        {
            this.type = type;
            this.layer = layer;
            this.msg = msg;
            this.code = code;
            this.root = root;
        }
    }

    [Serializable]
    public class Measure
    {
        public string name;

        public long duration;

        public long begin;

        public Dictionary<string, Object> data;

        public Measure(string name, long duration, long begin, Dictionary<string, Object> data)
        {
            this.name = name;
            this.duration = duration;
            this.begin = begin;
            this.data = data;
        }
    }

    [Serializable]
    public class MeasurementsBuilder
    {
        private Stack<Measure> stack = new Stack<Measure>();
        private List<Measure> closedMeasures = new List<Measure>();

        private long GetCurrentMillis()
        {
            DateTime Jan1970 = new DateTime(1970, 1, 1, 0, 0, 0, DateTimeKind.Utc);
            TimeSpan javaSpan = DateTime.UtcNow - Jan1970;
            return (long)javaSpan.TotalMilliseconds;
        }

        public void StartMeasure(string name)
        {
            PushMeasure(new Measure(name, 0, GetCurrentMillis(), null));
        }

        public void StartMeasure(string name, long begin)
        {
            PushMeasure(new Measure(name, 0, begin, null));
        }

        protected void PushMeasure(Measure tr)
        {
            lock (stack)
            {
                stack.Push(tr);
            }
        }

        public void StopMeasure(long end, Dictionary<string, Object> data)
        {
            Measure tr;
            lock (stack)
            {
                tr = stack.Pop();
            }

            if (tr != null)
            {
                tr.duration = (end - tr.begin);
                tr.data = data;
                lock (closedMeasures)
                {
                    closedMeasures.Add(tr);
                }
            }
            else
            {
                throw new Exception("No measure has been started. Please ensure to first call startMeasure before calling stopMeasure.");
            }
        }

        public void StopMeasure(Dictionary<string, Object> data)
        {
            StopMeasure(GetCurrentMillis(), data);
        }

        public void StopMeasure()
        {
            StopMeasure(null);
        }

        public void AddMeasure(string measureName, long aDurationMillis)
        {
            AddMeasure(measureName, aDurationMillis, null);
        }

        public void AddMeasure(string measureName, long aDurationMillis, Dictionary<string, Object> data)
        {
            lock (closedMeasures)
            {
                closedMeasures.Add(new Measure(measureName, aDurationMillis, GetCurrentMillis(), data));
            }
        }

        public void AddMeasure(Measure measure)
        {
            lock (closedMeasures)
            {
                closedMeasures.Add(measure);
            }
        }

        public void AddMeasure(List<Measure> measures)
        {
            lock (closedMeasures)
            {
                closedMeasures.AddRange(measures);
            }
        }

        public List<Measure> GetMeasures()
        {
            lock (closedMeasures)
            {
                return new List<Measure>(closedMeasures);
            }
        }
    }
}
