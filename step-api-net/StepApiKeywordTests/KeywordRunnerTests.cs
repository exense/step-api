using KeywordsForTesting;
using Step.Functions.IO;
using System;
using System.Collections.Generic;
using Xunit;

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

    public class ScriptRunnerTest
    {
        Output output;

        [Fact]
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
            Assert.Equal("Unable to find method annoted by 'Keyword' with name == 'My Other non existing Keyword'", 
                output.error.msg);
        }

        [Fact]
        public void TestScriptRunnerOnError()
        {
            ExecutionContext runner = KeywordRunner.GetExecutionContext(typeof(TestKeywords));

            output = runner.Run("My Error Keyword", @"{onError_return:'false'}");
            Assert.Null(output.error);
            Assert.Equal("true", output.payload["onError"].ToString());
            
            output = runner.Run("My Error Keyword", @"{onError_return:'true'}");
            Assert.Equal("This is a test", output.error.msg);
            Assert.Equal("true", output.payload["onError"].ToString());
        }

        [Fact]
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
            Assert.Equal("val1", output.payload["prop1"].ToString());
            Assert.Null(output.error);
        }

        [Fact]
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
            Assert.Equal("val1", output.payload["prop1"].ToString());
            Assert.Single(output.payload);
            Assert.Null(output.error);
        }

        [Fact]
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
            Assert.Equal("The Keyword is missing the following properties 'prop1'", output.error.msg);
        }

        [Fact]
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
            Assert.Equal("My Property with Place holder", output.payload["prop.placeHolderValue"].ToString());
            Assert.Single(output.payload);
            Assert.Null(output.error);
        }

        [Fact]
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
            Assert.Equal("My Property with Place holder", output.payload["prop.placeHolderValue"].ToString());
            Assert.Single(output.payload);
            Assert.Null(output.error);
        }

        [Fact]
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
            Assert.Equal("My Property with Place holder", output.payload["prop.placeHolderValue"].ToString());
            Assert.Equal("My optional Property", output.payload["myOptionalProperty"].ToString());
            Assert.Equal(2, output.payload.Count);
            Assert.Null(output.error);
        }

        [Fact]
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
            Assert.Equal("The Keyword is missing the following properties 'prop.placeHolderValue'", output.error.msg);
        }

        [Fact]
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
            Assert.Equal("The Keyword is missing the following property or input 'myPlaceHolder'", output.error.msg);
        }

        [Fact]
        public void TestScriptRunnerRun()
        {
            ExecutionContext runner = KeywordRunner.GetExecutionContext(typeof(TestKeywords));
            output = runner.Run("My Keyword", @"{}");

            Assert.Null(output.error);
            Assert.Equal("value", output.payload["key"].ToString());
        }

        [Fact]
        public void TestScriptRunnerWithInputsAndPropertiesRun()
        {
            Dictionary<string, string> properties = new Dictionary<string, string>
            {
                ["myProp1"] = "myValue1"
            };

            ExecutionContext runner = KeywordRunner.GetExecutionContext(typeof(TestKeywords));
            output = runner.Run("My Keyword", @"{'myInput1':'myInputValue1'}", properties);

            Assert.Null(output.error);
            Assert.Equal("value", output.payload["key"].ToString());
            Assert.Equal("myValue1", output.payload["myProp1"].ToString());
            Assert.Equal("myInputValue1", output.payload["myInput1"].ToString());
        }
    }
}