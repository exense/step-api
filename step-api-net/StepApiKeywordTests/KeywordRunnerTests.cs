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

        [Keyword(name = "My Prop Keyword", properties = new string[] { "prop1", "prop2" })]
        public void MyKeywordWithProperties()
        {
            output.Add("executed", "My Prop Keyword");
        }

        [Keyword(name = "My Keyword With Complex Properties", properties = new string[] {
            "param_scope_global", "param_scope_App1", "param_scope_App2", "param_scope_KW",
                    "param_scope_KW2", "app.user.name","app.user.{app.user.name}.pwd" },
            optionalProperties = new string[] { "optionalProp", "optionalPropMissing"  })]
        public void MyKeywordWithComplexProperties()
        {
            output.Add("executed", "My Prop Keyword");
            foreach (string key in properties.Keys)
            {
                output.Add(key, properties[key]);
            }
        }

        [Keyword(name = "My Html Keyword", htmlTemplate = "<label>this is a test</label>")]
        public void MyKeywordWithCustomHtml()
        {
            output.Add("executed", "My Html Keyword");
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
        public void TestScriptRunnerProperties()
        {
            ExecutionContext runner = KeywordRunner.GetExecutionContext(
                new Dictionary<string, string>() { { "$validateProperties", "true" } }, 
                typeof(TestKeywords));

            output = runner.Run("My Prop Keyword", @"{}");
            Assert.Equal("The Keyword is missing the following properties 'prop1, prop2'", output.error.msg);

            output = runner.Run("My Prop Keyword", @"{}", new Dictionary<string, string>() { { "prop1", "val1" } } );
            Assert.Equal("The Keyword is missing the following properties 'prop2'", output.error.msg);

            output = runner.Run("My Prop Keyword", @"{}", new Dictionary<string, string>() { { "prop1", "val1" },
                { "prop2", "val2" } });
            Assert.Null(output.error);
        }

        [Fact]
        public void TestScriptRunnerComplexProperties()
        {
            ExecutionContext runner = KeywordRunner.GetExecutionContext(
                new Dictionary<string, string>() { { "$validateProperties", "true" } },
                typeof(TestKeywords));

            output = runner.Run("My Keyword With Complex Properties", @"{}");
            Assert.Equal("The Keyword is missing the following properties 'param_scope_global, param_scope_App1, param_scope_App2, " +
                "param_scope_KW, param_scope_KW2, app.user.name, app.user.{app.user.name}.pwd'", output.error.msg);

            output = runner.Run("My Keyword With Complex Properties", @"{}", new Dictionary<string, string>() {
                { "param_scope_global", "val1" }, { "app.user.name", "testUser" } });
            Assert.Equal("The Keyword is missing the following properties 'param_scope_App1, param_scope_App2, " +
                "param_scope_KW, param_scope_KW2, app.user.{app.user.name}.pwd or app.user.testUser.pwd'", output.error.msg);

            output = runner.Run("My Keyword With Complex Properties", @"{}", new Dictionary<string, string>() {
                { "param_scope_global", "val1" }, { "app.user.name", "testUser" },  { "app.user.testUser.pwd", "testUserPwd" } });
            Assert.Equal("The Keyword is missing the following properties 'param_scope_App1, param_scope_App2, " +
                "param_scope_KW, param_scope_KW2'", output.error.msg);

            output = runner.Run("My Keyword With Complex Properties", @"{}", new Dictionary<string, string>() {
                { "param_scope_global", "val1" }, { "param_scope_App1", "val1" }, { "param_scope_App2", "val1" },
                { "param_scope_KW", "val1" }, { "param_scope_KW2", "val1" }, { "app.user.name", "testUser" },
                { "app.user.testUser.pwd", "testUserPwd" }, { "optionalProp", "optionalPropVal" } });
            Assert.Null(output.error);
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

        [Fact]
        public void TestScriptRunnerCustomHtml()
        {
            ExecutionContext runner = KeywordRunner.GetExecutionContext(typeof(TestKeywords));
            output = runner.Run("My Html Keyword", @"{}");

            Assert.Null(output.error);
        }
    }
}