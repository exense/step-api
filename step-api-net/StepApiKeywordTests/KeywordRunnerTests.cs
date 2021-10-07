using NUnit.Framework;
using KeywordsForTesting;
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
            return Boolean.Parse(input["onError_return"].ToString());
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
            
            foreach (string key in input.Keys)
            {
                output.Add(key, input[key]);
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

        [Keyword(properties = new string[] { "prop.{myPlaceHolder}" }, optionalProperties = new string[] { "myOptionalProperty" })]
        public void MyKeywordWithPlaceHoldersInProperties()
        {
            EchoProperties();
        }

        protected void EchoProperties()
        {
            foreach (var key in properties.Keys)
            {
                output.Add(key, properties[key]);
            }
        }
    }

    public class ScriptRunnerTest
    {
        Output output;

        [Test]
        public void TestScriptRunnerMultipleKeywords()
        {
            ExecutionContext runner = KeywordRunner.GetExecutionContext(typeof(TestKeywords),
                typeof(TestMultipleKeywords));

            output = runner.Run("My Other Keyword", @"{}");
            Assert.Null(output.error);
            output = runner.Run("My Keyword", @"{}");
            Assert.Null(output.error);
            output = runner.Run("My Other Keyword", @"{}");
            Assert.Null(output.error);

            output = runner.Run("My Other non existing Keyword", @"{}");
            Assert.AreEqual("Unable to find method annoted by 'Keyword' with name == 'My Other non existing Keyword'",
                output.error.msg);
        }

        [Test]
        public void TestScriptRunnerOnError()
        {
            ExecutionContext runner = KeywordRunner.GetExecutionContext(typeof(TestKeywords));

            output = runner.Run("My Error Keyword", @"{""onError_return"":""false""}");
            Assert.Null(output.error);
            Assert.AreEqual("true", output.payload["onError"].ToString());

            output = runner.Run("My Error Keyword", @"{""onError_return"":""true""}");
            Assert.AreEqual("This is a test", output.error.msg);
            Assert.AreEqual("true", output.payload["onError"].ToString());
        }

        [Test]
        public void TestPropertiesWithoutValidation()
        {
            Dictionary<string, string> properties = new()
            {
                ["prop1"] = "val1"
            };

            ExecutionContext runner = KeywordRunner.GetExecutionContext(
                new Dictionary<string, string>() { },
                typeof(TestKeywords));

            var output = runner.Run("MyKeywordUsingProperties", @"{}", properties);
            Assert.AreEqual("val1", output.payload["prop1"].ToString());
            Assert.Null(output.error);
        }

        [Test]
        public void TestPropertyValidation()
        {
            Dictionary<string, string> properties = new()
            {
                ["prop1"] = "val1"
            };

            ExecutionContext runner = KeywordRunner.GetExecutionContext(
                new Dictionary<string, string>() { { "$validateProperties", "true" } },
                typeof(TestKeywords));

            var output = runner.Run("MyKeywordWithPropertyAnnotation", @"{}", properties);
            Assert.AreEqual("val1", output.payload["prop1"].ToString());
            Assert.That(output.payload, Has.Exactly(1).Items);
            Assert.Null(output.error);
        }

        [Test]
        public void TestPropertyValidationPropertyMissing()
        {
            Dictionary<string, string> properties = new()
            {
                ["prop2"] = "My Property 2"
            };

            ExecutionContext runner = KeywordRunner.GetExecutionContext(
                new Dictionary<string, string>() { { "$validateProperties", "true" } },
                typeof(TestKeywords));

            var output = runner.Run("MyKeywordWithPropertyAnnotation", "{}", properties);
            Assert.AreEqual("The Keyword is missing the following properties 'prop1'", output.error.msg);
        }

        [Test]
        public void TestPropertyValidationWithPlaceHolder()
        {
            Dictionary<string, string> properties = new()
            {
                ["prop.placeHolderValue"] = "My Property with Place holder",
                ["myPlaceHolder"] = "placeHolderValue"
            };

            ExecutionContext runner = KeywordRunner.GetExecutionContext(
                new Dictionary<string, string>() { { "$validateProperties", "true" } },
                typeof(TestKeywords));

            var output = runner.Run("MyKeywordWithPlaceHoldersInProperties", @"{}", properties);
            Assert.AreEqual("My Property with Place holder", output.payload["prop.placeHolderValue"].ToString());
            Assert.That(output.payload, Has.Exactly(1).Items);
            Assert.Null(output.error);
        }

        [Test]
        public void TestPropertyValidationWithPlaceHolderInInput()
        {
            Dictionary<string, string> properties = new()
            {
                ["prop.placeHolderValue"] = "My Property with Place holder",
                ["myPlaceHolder"] = "placeHolderValue from properties"
            };

            ExecutionContext runner = KeywordRunner.GetExecutionContext(
                new Dictionary<string, string>() { { "$validateProperties", "true" } },
                typeof(TestKeywords));

            var output = runner.Run("MyKeywordWithPlaceHoldersInProperties", "{\"myPlaceHolder\": \"placeHolderValue\"}", properties);
            Assert.AreEqual("My Property with Place holder", output.payload["prop.placeHolderValue"].ToString());
            Assert.That(output.payload, Has.Exactly(1).Items);
            Assert.Null(output.error);
        }

        [Test]
        public void TestPropertyValidationWithOptionalProperties()
        {
            Dictionary<string, string> properties = new()
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
            Assert.AreEqual(2, output.payload.Count);
            Assert.Null(output.error);
        }

        [Test]
        public void TestPropertyValidationWithPlaceHolderInInputWhereTheResolvedPropertyIsMissing()
        {
            Dictionary<string, string> properties = new()
            {
                ["other.placeHolderValue"] = "My Property with Place holder"
            };

            ExecutionContext runner = KeywordRunner.GetExecutionContext(
                new Dictionary<string, string>() { { "$validateProperties", "true" } },
                typeof(TestKeywords));

            var output = runner.Run("MyKeywordWithPlaceHoldersInProperties", "{\"myPlaceHolder\": \"placeHolderValue\"}", properties);
            Assert.AreEqual("The Keyword is missing the following properties 'prop.placeHolderValue'", output.error.msg);
        }

        [Test]
        public void TestPropertyValidationWithPlaceHolderInInputWhereThePlaceholderIsMissing()
        {
            Dictionary<string, string> properties = new()
            {
                ["other.placeHolderValue"] = "My Property with Place holder"
            };

            ExecutionContext runner = KeywordRunner.GetExecutionContext(
                new Dictionary<string, string>() { { "$validateProperties", "true" } },
                typeof(TestKeywords));

            var output = runner.Run("MyKeywordWithPlaceHoldersInProperties", "{}", properties);
            Assert.AreEqual("The Keyword is missing the following property or input 'myPlaceHolder'", output.error.msg);
        }

        [Test]
        public void TestScriptRunnerRun()
        {
            ExecutionContext runner = KeywordRunner.GetExecutionContext(typeof(TestKeywords));
            output = runner.Run("My Keyword", @"{}");

            Assert.Null(output.error);
            Assert.AreEqual("value", output.payload["key"].ToString());
        }

        [Test]
        public void TestScriptRunnerWithInputsAndPropertiesRun()
        {
            Dictionary<string, string> properties = new()
            {
                ["myProp1"] = "myValue1"
            };

            ExecutionContext runner = KeywordRunner.GetExecutionContext(typeof(TestKeywords));
            output = runner.Run("My Keyword", @"{""myInput1"":""myInputValue1""}", properties);

            Assert.Null(output.error);
            Assert.AreEqual("value", output.payload["key"].ToString());
            Assert.AreEqual("myValue1", output.payload["myProp1"].ToString());
            Assert.AreEqual("myInputValue1", output.payload["myInput1"].ToString());
        }
    }
}