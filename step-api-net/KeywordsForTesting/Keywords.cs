using Step.Handlers.NetHandler;
using System;

namespace KeywordsForTesting
{
    public class TestMultipleKeywords : AbstractKeyword
    {
        [Keyword(name = "My Other Keyword")]
        public void MyKeyword()
        {
            // We need a separate Assembly for testing "TestScriptRunnerMultipleKeywords"
        }
        public override void BeforeKeyword(String KeywordName, Keyword Annotation)
        {
            output.Add("BeforeKeyword", KeywordName);
        }

        public override void AfterKeyword(String KeywordName, Keyword Annotation)
        {
            output.Add("AfterKeyword", KeywordName);
        }
    }
    public class TestNotAbstractKeywords
    {
        [Keyword(name = "Not Abstract Keyword")]
        public void NotAbstractKeyword()
        {
            // We need a separate Assembly for testing "TestScriptRunnerMultipleKeywords"
        }
    }
}
