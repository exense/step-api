using Newtonsoft.Json.Linq;
using Step.Core.Reports;
using Step.Grid.IO;
using System;
using System.Collections.Generic;

namespace Step.Functions.IO
{

    public class DynamicValue<T>
    {

        public bool dynamic = false;
        public T value;
        public String expression = "";
        public String expressionType = "";

        public DynamicValue(T value)
        {
            this.value = value;
        }
    }

    public class Function
    {
        public string type = "step.plugins.dotnet.DotNetFunction";

        public string description = "";

        public Dictionary<string, string> attributes;

        public JObject schema;

        public DynamicValue<int> callTimeout = new DynamicValue<int>(180000);
    }

    public class Input
    {
        public string function;

        public long functionCallTimeout;

        public JObject payload;

        public Dictionary<string, string> properties;

        public List<Attachment> attachments;
    }

    public class Output
    {
        public JObject payload;

        public Error error;

        public List<Attachment> attachments;

        public List<Measure> measures;
    }

    public class OutputBuilder
    {
        public JObject output { get; } = new JObject();

        public MeasurementsBuilder measureHelper { get; } = new MeasurementsBuilder();

        public List<Attachment> attachments { get; } = new List<Attachment>();

        public Error error { get; private set; }

        public OutputBuilder Add(string key, string val)
        {
            output.Add(key, val);
            return this;
        }

        public OutputBuilder SetError(Error error)
        {
            this.error = error;
            return this;
        }

        public OutputBuilder SetError(string technicalError)
        {
            error = new Error(ErrorType.TECHNICAL, "keyword", technicalError, 0, true);
            return this;
        }

        public OutputBuilder SetError(string technicalError, Exception e)
        {
            SetError(technicalError);
            AddAttachment(AttachmentHelper.GenerateAttachmentForException(e));
            return this;
        }

        public OutputBuilder SetBusinessError(string businessError)
        {
            error = new Error(ErrorType.BUSINESS, "keyword", businessError, 0, true);
            return this;
        }

        public void StartMeasure(string id)
        {
            measureHelper.StartMeasure(id);
        }

        public void StartMeasure(string id, long begin)
        {
            measureHelper.StartMeasure(id, begin);
        }

        public void StopMeasure(long end, Dictionary<string, Object> data)
        {
            measureHelper.StopMeasure(end, data);
        }

        public void AddMeasure(string measureName, long durationMillis)
        {
            measureHelper.AddMeasure(measureName, durationMillis);
        }

        public void AddMeasure(string measureName, long aDurationMillis, Dictionary<string, Object> data)
        {
            measureHelper.AddMeasure(measureName, aDurationMillis, data);
        }

        public void StopMeasure()
        {
            measureHelper.StopMeasure();
        }

        public void StopMeasure(Dictionary<string, Object> data)
        {
            measureHelper.StopMeasure(data);
        }

        public void AddAttachments(List<Attachment> attachments)
        {
            this.attachments.AddRange(attachments);
        }

        public void AddAttachment(Attachment attachment)
        {
            attachments.Add(attachment);
        }

        public Output Build()
        {
            Output output = new Output
            {
                payload = this.output,
                error = this.error,
                measures = this.measureHelper.GetMeasures(),
                attachments = this.attachments
            };
            return output;
        }
    }
}
