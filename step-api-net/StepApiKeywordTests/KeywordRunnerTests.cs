﻿using Microsoft.VisualStudio.TestTools.UnitTesting;
using System;
using System.Collections.Generic;

namespace Step.Handlers.NetHandler.Tests
{
    [TestClass]
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

        }
        [TestMethod]
        public void TestScriptRunnerOnError()
        {
            ExecutionContext runner = KeywordRunner.GetExecutionContext(typeof(TestKeywords));
            var output = runner.Run("My Error Keyword", @"{}");

            Assert.AreEqual("true", output.Payload["onError"]);
        }

        [TestMethod]
        public void TestScriptRunnerRun()
        {
            ExecutionContext runner = KeywordRunner.GetExecutionContext(typeof(TestKeywords));
            var output = runner.Run("My Keyword", @"{}");

            Assert.AreEqual(null, output.Error);
            Assert.AreEqual("value", output.Payload["key"]);
        }

        [TestMethod]
        public void TestScriptRunnerWithInputsAndPropertiesRun()
        {
            Dictionary<string, string> properties = new Dictionary<string, string>
            {
                ["myProp1"] = "myValue1"
            };

            ExecutionContext runner = KeywordRunner.GetExecutionContext(typeof(TestKeywords));
            var output = runner.Run("My Keyword", @"{'myInput1':'myInputValue1'}", properties);

            Assert.AreEqual(null, output.Error);
            Assert.AreEqual("value", output.Payload["key"]);
            Assert.AreEqual("myValue1", output.Payload["myProp1"]);
            Assert.AreEqual("myInputValue1", output.Payload["myInput1"]);
        }
    }
}