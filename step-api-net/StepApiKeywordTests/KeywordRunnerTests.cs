﻿using KeywordsForTesting;
using Microsoft.VisualStudio.TestTools.UnitTesting;
using Step.Functions.IO;
using System;
using System.Collections.Generic;

namespace Step.Handlers.NetHandler.Tests
{
    public class TestKeywords : AbstractKeyword
    {
        public static string THROW_EXCEPTION_IN_BEFORE = "THROW_BEFORE";
        public static string THROW_EXCEPTION_IN_AFTER = "THROW_AFTER";

        public override bool OnError(Exception e)
        {
            output.Add("onError", "true");
            return input["onError_return"]!=null && (bool) input["onError_return"];
        }

        public override void BeforeKeyword(String KeywordName, Keyword Annotation)
        {
            output.Add("BeforeKeyword", KeywordName);
            if (input.ContainsKey(THROW_EXCEPTION_IN_BEFORE))
            {
                throw new Exception(input[THROW_EXCEPTION_IN_BEFORE].ToString());
            }
        }

        public override void AfterKeyword(String KeywordName, Keyword Annotation)
        {
            output.Add("AfterKeyword", KeywordName);
            if (input.ContainsKey(THROW_EXCEPTION_IN_AFTER))
            {
                throw new Exception(input[THROW_EXCEPTION_IN_AFTER].ToString());
            }
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

        [Keyword(name = "My Set Error Keyword")]
        public void MySetErrorKeyword()
        {
            output.SetError("Testing normal errors");
        }

        [Keyword(name = "My Business Error Keyword")]
        public void MyBusinessErrorKeyword()
        {
            output.SetBusinessError("Testing business errors");
        }

        [Keyword()]
        public void MyKeywordUsingProperties()
        {
            EchoProperties();
        }

        [Keyword(properties = new string[] { "prop1" })]
        public void MyKeywordWithPropertyAnnotation()
        {
            EchoProperties();
        }

        [Keyword(properties = new string[] {"prop.{myPlaceHolder}"}, optionalProperties = new string[] { "myOptionalProperty" })]
        public void MyKeywordWithPlaceHoldersInProperties()
        {
            EchoProperties();
        }

        protected void EchoProperties()
        {
            foreach(var key in properties.Keys)
            {
                output.Add(key, properties[key]);
            }
        }
    }

    [TestClass]
    public class ScriptRunnerTest
    {
        Output output;

        [TestMethod]
        public void TestNotAbstractClass()
        {
            ExecutionContext runner = KeywordRunner.GetExecutionContext(typeof(TestNotAbstractKeywords));

            output = runner.Run("Not Abstract Keyword");
            Assert.IsNull(output.error);
            Assert.IsTrue(output.payload.Count==0);
        }

        [TestMethod]
        public void TestScriptRunnerMultipleKeywords()
        {
            ExecutionContext runner = KeywordRunner.GetExecutionContext(typeof(TestKeywords), 
                typeof(TestMultipleKeywords));

            output = runner.Run("My Other Keyword", @"{}");
            Assert.IsNull(output.error);
            Assert.AreEqual(output.payload["BeforeKeyword"].ToString(), "My Other Keyword");
            Assert.AreEqual(output.payload["AfterKeyword"].ToString(), "My Other Keyword");

            output = runner.Run("My Keyword", @"{}");
            Assert.IsNull(output.error);
            Assert.AreEqual(output.payload["BeforeKeyword"].ToString(), "My Keyword");
            Assert.AreEqual(output.payload["AfterKeyword"].ToString(), "My Keyword");
            output = runner.Run("My Other Keyword", @"{}");
            Assert.IsNull(output.error);

            output = runner.Run("My Other non existing Keyword", @"{}");
            Assert.AreEqual("Unable to find method annoted by 'Keyword' with name == 'My Other non existing Keyword'", 
                output.error.msg);
        }

        [TestMethod]
        public void TestScriptRunnerOnError()
        {
            ExecutionContext runner = KeywordRunner.GetExecutionContext(typeof(TestKeywords));

            output = runner.Run("My Error Keyword", @"{onError_return:'false'}");
            Assert.IsNull(output.error);
            Assert.AreEqual("true", output.payload["onError"].ToString());
            
            output = runner.Run("My Error Keyword", @"{onError_return:'true'}");
            Assert.AreEqual("This is a test", output.error.msg);
            Assert.AreEqual("true", output.payload["onError"].ToString());
        }

        [TestMethod]
        public void TestPropertiesWithoutValidation()
        {
            Dictionary<string, string> properties = new Dictionary<string, string>
            {
                ["prop1"] = "val1"
            };

            ExecutionContext runner = KeywordRunner.GetExecutionContext(
                new Dictionary<string, string>() {}, 
                typeof(TestKeywords));

            var output = runner.Run("MyKeywordUsingProperties", @"{}", properties);
            Assert.AreEqual("val1", output.payload["prop1"].ToString());
            Assert.IsNull(output.error);
        }

        [TestMethod]
        public void TestPropertyValidation()
        {
            Dictionary<string, string> properties = new Dictionary<string, string>
            {
                ["prop1"] = "val1"
            };

            ExecutionContext runner = KeywordRunner.GetExecutionContext(
                new Dictionary<string, string>() { { "$validateProperties", "true" } },
                typeof(TestKeywords));

            var output = runner.Run("MyKeywordWithPropertyAnnotation", @"{}", properties);
            Assert.AreEqual("val1", output.payload["prop1"].ToString());
            Assert.AreEqual(3,output.payload.Count);
            Assert.IsNull(output.error);
        }

        [TestMethod]
        public void TestPropertyValidationPropertyMissing()
        {
            Dictionary<string, string> properties = new Dictionary<string, string>
            {
                ["prop2"] = "My Property 2"
            };

            ExecutionContext runner = KeywordRunner.GetExecutionContext(
                new Dictionary<string, string>() { { "$validateProperties", "true" } },
                typeof(TestKeywords));

            var output = runner.Run("MyKeywordWithPropertyAnnotation", "{}", properties);
            Assert.AreEqual("The Keyword is missing the following properties 'prop1'", output.error.msg);
        }

        [TestMethod]
        public void TestPropertyValidationWithPlaceHolder()
        {
            Dictionary<string, string> properties = new Dictionary<string, string>
            {
                ["prop.placeHolderValue"] = "My Property with Place holder",
                ["myPlaceHolder"] = "placeHolderValue"
            };

            ExecutionContext runner = KeywordRunner.GetExecutionContext(
                new Dictionary<string, string>() { { "$validateProperties", "true" } },
                typeof(TestKeywords));

            var output = runner.Run("MyKeywordWithPlaceHoldersInProperties", @"{}", properties);
            Assert.AreEqual("My Property with Place holder", output.payload["prop.placeHolderValue"].ToString());
            Assert.AreEqual(3, output.payload.Count);
            Assert.IsNull(output.error);
        }

        [TestMethod]
        public void TestPropertyValidationWithPlaceHolderInInput()
        {
            Dictionary<string, string> properties = new Dictionary<string, string>
            {
                ["prop.placeHolderValue"] = "My Property with Place holder",
                 // The placeholder value from the input should be taken
                ["myPlaceHolder"] = "placeHolderValue from properties",
            };

            ExecutionContext runner = KeywordRunner.GetExecutionContext(
                new Dictionary<string, string>() { { "$validateProperties", "true" } },
                typeof(TestKeywords));

            var output = runner.Run("MyKeywordWithPlaceHoldersInProperties", "{\"myPlaceHolder\": \"placeHolderValue\"}", properties);
            Assert.AreEqual("My Property with Place holder", output.payload["prop.placeHolderValue"].ToString());
            Assert.AreEqual(3, output.payload.Count);
            Assert.IsNull(output.error);
        }

        [TestMethod]
        public void TestPropertyValidationWithOptionalProperties()
        {
            Dictionary<string, string> properties = new Dictionary<string, string>
            {
                ["prop.placeHolderValue"] = "My Property with Place holder",
                ["myOptionalProperty"] = "My optional Property"
            };

            ExecutionContext runner = KeywordRunner.GetExecutionContext(
                new Dictionary<string, string>() { { "$validateProperties", "true" } },
                typeof(TestKeywords));

            var output = runner.Run("MyKeywordWithPlaceHoldersInProperties", "{\"myPlaceHolder\": \"placeHolderValue\"}", properties);
            Assert.AreEqual("My Property with Place holder", output.payload["prop.placeHolderValue"].ToString());
            Assert.AreEqual("My optional Property", output.payload["myOptionalProperty"].ToString());
            Assert.AreEqual(4, output.payload.Count);
            Assert.IsNull(output.error);
        }

        [TestMethod]
        public void TestPropertyValidationWithPlaceHolderInInputWhereTheResolvedPropertyIsMissing()
        {
            Dictionary<string, string> properties = new Dictionary<string, string>
            {
                ["other.placeHolderValue"] = "My Property with Place holder"
            };

            ExecutionContext runner = KeywordRunner.GetExecutionContext(
                new Dictionary<string, string>() { { "$validateProperties", "true" } },
                typeof(TestKeywords));

            var output = runner.Run("MyKeywordWithPlaceHoldersInProperties", "{\"myPlaceHolder\": \"placeHolderValue\"}", properties);
            Assert.AreEqual("The Keyword is missing the following properties 'prop.placeHolderValue'", output.error.msg);
        }

        [TestMethod]
        public void TestPropertyValidationWithPlaceHolderInInputWhereThePlaceholderIsMissing()
        {
            Dictionary<string, string> properties = new Dictionary<string, string>
            {
                ["other.placeHolderValue"] = "My Property with Place holder"
            };

            ExecutionContext runner = KeywordRunner.GetExecutionContext(
                new Dictionary<string, string>() { { "$validateProperties", "true" } },
                typeof(TestKeywords));

            var output = runner.Run("MyKeywordWithPlaceHoldersInProperties", "{}", properties);
            Assert.AreEqual("The Keyword is missing the following property or input 'myPlaceHolder'", output.error.msg);
        }

        [TestMethod]
        public void TestBeforeAfterException()
        {
            ExecutionContext runner = KeywordRunner.GetExecutionContext(typeof(TestKeywords));
            output = runner.Run("My Keyword", @"{'"+ TestKeywords.THROW_EXCEPTION_IN_BEFORE + "':'Error Before'}");
            Assert.IsNull(output.error);
            Assert.AreEqual(output.payload["BeforeKeyword"].ToString(), "My Keyword");
            Assert.AreEqual(output.payload["AfterKeyword"].ToString(), "My Keyword");
            Assert.IsNull(output.payload["key"]);
        }

        [TestMethod]
        public void TestScriptRunnerRun()
        {
            ExecutionContext runner = KeywordRunner.GetExecutionContext(typeof(TestKeywords));
            output = runner.Run("My Keyword", @"{}");

            Assert.IsNull(output.error);
            Assert.AreEqual("value", output.payload["key"].ToString());
        }

        [TestMethod]
        public void TestScriptRunnerWithInputsAndPropertiesRun()
        {
            Dictionary<string, string> properties = new Dictionary<string, string>
            {
                ["myProp1"] = "myValue1"
            };

            ExecutionContext runner = KeywordRunner.GetExecutionContext(typeof(TestKeywords));
            output = runner.Run("My Keyword", @"{'myInput1':'myInputValue1'}", properties);

            Assert.IsNull(output.error);
            Assert.AreEqual("value", output.payload["key"].ToString());
            Assert.AreEqual("myValue1", output.payload["myProp1"].ToString());
            Assert.AreEqual("myInputValue1", output.payload["myInput1"].ToString());
        }
    }
}