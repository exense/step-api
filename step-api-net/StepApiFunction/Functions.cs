
using Newtonsoft.Json.Linq;
using Step.Core.Reports;
using Step.Grid.IO;
using System;
using System.Collections.Generic;

namespace Step.Functions.IO
{
    public class Input
    {
        public string Function;

        public long FunctionCallTimeout;

        public JObject Payload;

        public Dictionary<string, string> Properties;

        public List<Attachment> Attachments;
    }

    public class Output
    {
        public JObject Payload;

        public Error Error;

        public List<Attachment> Attachments;

        public List<Measure> Measures;
    }

    public class OutputBuilder
    {
        public JObject Output { get; } = new JObject();

        public MeasurementsBuilder MeasureHelper { get; } = new MeasurementsBuilder();

        public List<Attachment> Attachments { get; } = new List<Attachment>();

        public Error Error { get; private set; }

        public OutputBuilder Add(string Key, string Val)
        {
            Output.Add(Key, Val);
            return this;
        }

        public OutputBuilder SetError(string TechnicalError)
        {
            Error = new Error(ErrorType.TECHNICAL, "keyword", TechnicalError, 0, true);
            return this;
        }

        public OutputBuilder SetError(string TechnicalError, Exception e)
        {
            SetError(TechnicalError);
            AddAttachment(AttachmentHelper.GenerateAttachmentForException(e));
            return this;
        }

        public OutputBuilder SetBusinessError(string BusinessError)
        {
            Error = new Error(ErrorType.BUSINESS, "keyword", BusinessError, 0, true);
            return this;
        }

        public void StartMeasure(string id)
        {
            MeasureHelper.StartMeasure(id);
        }

        public void StartMeasure(string id, long begin)
        {
            MeasureHelper.StartMeasure(id, begin);
        }

        public void StopMeasure(long end, Dictionary<string, Object> data)
        {
            MeasureHelper.StopMeasure(end, data);
        }

        public void AddMeasure(string measureName, long durationMillis)
        {
            MeasureHelper.AddMeasure(measureName, durationMillis);
        }

        public void AddMeasure(string measureName, long aDurationMillis, Dictionary<string, Object> data)
        {
            MeasureHelper.AddMeasure(measureName, aDurationMillis, data);
        }

        public void StopMeasure()
        {
            MeasureHelper.StopMeasure();
        }

        public void StopMeasure(Dictionary<string, Object> data)
        {
            MeasureHelper.StopMeasure(data);
        }

        public void AddAttachments(List<Attachment> attachments)
        {
            Attachments.AddRange(attachments);
        }

        public void AddAttachment(Attachment attachment)
        {
            Attachments.Add(attachment);
        }

        public Output Build()
        {
            Output output = new Output
            {
                Payload = this.Output,
                Error = this.Error,
                Measures = this.MeasureHelper.GetMeasures(),
                Attachments = this.Attachments
            };
            return output;
        }
    }
}
