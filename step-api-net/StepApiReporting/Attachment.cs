using System;

namespace Step.Grid.IO
{
    public class Attachment
    {
        public string name { get; set; }

        public string description { get; set; }

        public string hexContent { get; set; }

        public Boolean isDirectory { get; set; }
    }

    public class AttachmentHelper
    {
        public static string Base64Encode(byte[] bytes)
        {
            return Convert.ToBase64String(bytes);
        }

        public static byte[] Base64Decode(string base64EncodedData)
        {
            return Convert.FromBase64String(base64EncodedData);
        }

        public static Attachment GenerateAttachmentForException(Exception e)
        {
            return GenerateAttachmentFromByteArray(System.Text.Encoding.UTF8.GetBytes(e.ToString()), "exception.log");
        }

        public static Attachment GenerateAttachmentFromByteArray(byte[] bytes, string attachmentName)
        {
            Attachment attachment = new()
            {
                name = attachmentName,
                hexContent = Base64Encode(bytes)
            };
            return attachment;
        }
    }

}
