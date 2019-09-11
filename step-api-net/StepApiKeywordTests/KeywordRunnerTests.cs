using KeywordsForTesting;
using NUnit.Framework;
using Step.Functions.IO;
using System;
using System.Collections.Generic;

namespace Step.Handlers.NetHandler.Tests
{
    public class TestKeywords : AbstractKeyword
    {
        public override bool OnError(Exception e)
        {
            output.Add("onError", "true");
            return (bool) input.GetValue("onError_return");
        }

        [Keyword(name = "My Keyword")]
        public void MyKeyword()
        {
            output.Add("key", "value");
            foreach (string key in properties.Keys)
            {
                output.Add(key, properties[key]);
            }
            output.Add("input", (input == null).ToString());
            var inputMap = input.ToObject<Dictionary<string, string>>();
            foreach (string key in inputMap.Keys)
            {
                output.Add(key, inputMap[key]);
            }
        }

        [Keyword(name = "My Error Keyword")]
        public void MyErrorKeyword()
        {
            throw new Exception("This is a test");
        }

        [Keyword(name = "My Prop Keyword", properties = new string[] { "prop1", "prop2" })]
        public void MyKeywordWithProperties()
        {
            output.Add("executed", "My Prop Keyword");
        }
    }

    public class ScriptRunnerTest
    {
        Output output;

        [TestCase]
        public void TestScriptRunnerMultipleKeywords()
        {
            ExecutionContext runner = KeywordRunner.GetExecutionContext(typeof(TestKeywords), 
                typeof(TestMultipleKeywords));

            output = runner.Run("My Other Keyword", @"{}");
            Assert.AreEqual(null, output.error);
            output = runner.Run("My Keyword", @"{}");
            Assert.AreEqual(null, output.error);
            output = runner.Run("My Other Keyword", @"{}");
            Assert.AreEqual(null, output.error);

            output = runner.Run("My Other non existing Keyword", @"{}");
            Assert.AreEqual("Could not find keyword named 'My Other non existing Keyword'", 
                output.error.msg);
        }

        [TestCase]
        public void TestScriptRunnerOnError()
        {
            ExecutionContext runner = KeywordRunner.GetExecutionContext(typeof(TestKeywords));

            output = runner.Run("My Error Keyword", @"{onError_return:'false'}");
            Assert.AreEqual(null, output.error);
            Assert.AreEqual("true", output.payload["onError"].ToString());
            
            output = runner.Run("My Error Keyword", @"{onError_return:'true'}");
            Assert.AreEqual("This is a test", output.error.msg);
            Assert.AreEqual("true", output.payload["onError"].ToString());
        }

        [TestCase]
        public void TestScriptRunnerProperties()
        {
            ExecutionContext runner = KeywordRunner.GetExecutionContext(
                new Dictionary<string, string>() { { "$validateProperties", "true" } }, 
                typeof(TestKeywords));

            output = runner.Run("My Prop Keyword", @"{}");
            Assert.AreEqual("The Keyword is missing the following properties 'prop1, prop2'", output.error.msg);

            output = runner.Run("My Prop Keyword", @"{}", new Dictionary<string, string>() { { "prop1", "val1" } } );
            Assert.AreEqual("The Keyword is missing the following properties 'prop2'", output.error.msg);

            output = runner.Run("My Prop Keyword", @"{}", new Dictionary<string, string>() { { "prop1", "val1" },
                { "prop2", "val2" } });
            Assert.IsNull(output.error);
        }

        [TestCase]
        public void TestScriptRunnerRun()
        {
            ExecutionContext runner = KeywordRunner.GetExecutionContext(typeof(TestKeywords));
            output = runner.Run("My Keyword", @"{}");

            Assert.AreEqual(null, output.error);
            Assert.AreEqual("value", output.payload["key"].ToString());
        }

        [TestCase]
        public void TestScriptRunnerWithInputsAndPropertiesRun()
        {
            Dictionary<string, string> properties = new Dictionary<string, string>
            {
                ["myProp1"] = "myValue1"
            };

            ExecutionContext runner = KeywordRunner.GetExecutionContext(typeof(TestKeywords));
            output = runner.Run("My Keyword", @"{'myInput1':'myInputValue1'}", properties);

            Assert.AreEqual(null, output.error);
            Assert.AreEqual("value", output.payload["key"].ToString());
            Assert.AreEqual("myValue1", output.payload["myProp1"].ToString());
            Assert.AreEqual("myInputValue1", output.payload["myInput1"].ToString());
        }
    }
}