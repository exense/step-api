
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

        public Error(ErrorType Type, string Layer, string Msg, int Code, bool Root)
        {
            this.type = Type;
            this.layer = Layer;
            this.msg = Msg;
            this.code = Code;
            this.root = Root;
        }
    }

    [Serializable]
    public class Measure
    {
        public string name;

        public long duration;

        public long begin;

        public Dictionary<string, Object> data;

        public Measure(string Name, long Duration, long Begin, Dictionary<string, Object> Data)
        {
            this.name = Name;
            this.duration = Duration;
            this.begin = Begin;
            this.data = Data;
        }
    }

    [Serializable]
    public class MeasurementsBuilder
    {
        private Stack<Measure> Stack = new Stack<Measure>();

        private List<Measure> ClosedMeasures = new List<Measure>();

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
            lock (Stack)
            {
                Stack.Push(tr);
            }
        }

        public void StopMeasure(long end, Dictionary<string, Object> data)
        {
            Measure tr;
            lock (Stack)
            {
                tr = Stack.Pop();
            }

            if (tr != null)
            {
                tr.duration = (end - tr.begin);
                tr.data = data;
                lock (ClosedMeasures)
                {
                    ClosedMeasures.Add(tr);
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
            lock (ClosedMeasures)
            {
                ClosedMeasures.Add(new Measure(measureName, aDurationMillis, GetCurrentMillis(), data));
            }
        }

        public void AddMeasure(Measure measure)
        {
            lock (ClosedMeasures)
            {
                ClosedMeasures.Add(measure);
            }
        }

        public void AddMeasure(List<Measure> measures)
        {
            lock (ClosedMeasures)
            {
                ClosedMeasures.AddRange(measures);
            }
        }

        public List<Measure> GetMeasures()
        {
            lock (ClosedMeasures)
            {
                return new List<Measure>(ClosedMeasures);
            }
        }
    }
}
