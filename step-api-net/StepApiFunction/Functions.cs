using Step.Core.Reports;
using Step.Grid.IO;
using System;
using System.Collections.Generic;
using System.Text.Json;

namespace Step.Functions.IO
{
    public class Function
    {
        public string type = "step.plugins.dotnet.DotNetFunction";

        public Dictionary<string, string> attributes { get; set; }

        public JsonDocument schema { get; set; }
    }

    public class Input
    {
        public string function { get; set; }

        public long functionCallTimeout { get; set; }

        public Dictionary<string, object> payload { get; set; }

        public Dictionary<string, string> properties { get; set; }

        public List<Attachment> attachments { get; set; }
    }

    public class Output
    {
        public Dictionary<string, object> payload { get; set; }

        public Error error { get; set; }

        public List<Attachment> attachments { get; set; }

        public List<Measure> measures { get; set; }
    }

    public class OutputBuilder
    {
        public Dictionary<string, object> output { get;} = new();

        public MeasurementsBuilder measureHelper { get; } = new();

        public List<Attachment> attachments { get; } = new();

        public Error error { get; private set; }

        public OutputBuilder Add(string key, object value)
        {
            output.Add(key, value);
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
            Output output = new()
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
