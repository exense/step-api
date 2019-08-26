using NUnit.Framework;
using System;
using System.Collections.Generic;

namespace Step.Handlers.NetHandler.Tests
{
    public class ScriptRunnerTest
    {
        private class TestKeywords : AbstractKeyword
        {
            public override bool OnError(Exception e)
            {
                Output.Add("onError", "true");
                return false;
            }

            [Keyword(Name = "My Keyword")]
            public void MyKeyword()
            {
                Output.Add("key", "value");
                foreach (string key in Properties.Keys)
                {
                    Output.Add(key, Properties[key]);
                }
                Output.Add("input", (Input == null).ToString());
                var inputMap = Input.ToObject<Dictionary<string, string>>();
                foreach (string key in inputMap.Keys)
                {
                    Output.Add(key, inputMap[key]);
                }
            }

            [Keyword(Name = "My Error Keyword")]
            public void MyErrorKeyword()
            {
                throw new Exception("Test");
            }

            [Keyword(Name = "My Prop Keyword", Properties = new string[]{"prop1", "prop2"})]
            public void MyKeywordWithProperties()
            {
                Output.Add("executed", "My Prop Keyword");
            }
        }

        [TestCase]
        public void TestScriptRunnerOnError()
        {
            ExecutionContext runner = KeywordRunner.GetExecutionContext(typeof(TestKeywords));
            var output = runner.Run("My Error Keyword", @"{}");

            Assert.AreEqual("true", output.Payload["onError"].ToString());
        }

        [TestCase]
        public void TestScriptRunnerProperties()
        {
            ExecutionContext runner = KeywordRunner.GetExecutionContext(typeof(TestKeywords));
            var output = runner.Run("My Prop Keyword", @"{}");
            Assert.AreEqual("The Keyword is missing the following properties 'prop1, prop2'", output.Error.Msg);

            output = runner.Run("My Prop Keyword", @"{}", new Dictionary<string, string>() { { "prop1", "val1" } } );
            Assert.AreEqual("The Keyword is missing the following properties 'prop2'", output.Error.Msg);

            output = runner.Run("My Prop Keyword", @"{}", new Dictionary<string, string>() { { "prop1", "val1" },
                { "prop2", "val2" } });
            Assert.IsNull(output.Error);
        }

        [TestCase]
        public void TestScriptRunnerRun()
        {
            ExecutionContext runner = KeywordRunner.GetExecutionContext(typeof(TestKeywords));
            var output = runner.Run("My Keyword", @"{}");

            Assert.AreEqual(null, output.Error);
            Assert.AreEqual("value", output.Payload["key"].ToString());
        }

        [TestCase]
        public void TestScriptRunnerWithInputsAndPropertiesRun()
        {
            Dictionary<string, string> properties = new Dictionary<string, string>
            {
                ["myProp1"] = "myValue1"
            };

            ExecutionContext runner = KeywordRunner.GetExecutionContext(typeof(TestKeywords));
            var output = runner.Run("My Keyword", @"{'myInput1':'myInputValue1'}", properties);

            Assert.AreEqual(null, output.Error);
            Assert.AreEqual("value", output.Payload["key"].ToString());
            Assert.AreEqual("myValue1", output.Payload["myProp1"].ToString());
            Assert.AreEqual("myInputValue1", output.Payload["myInput1"].ToString());
        }
    }
}