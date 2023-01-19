using Step.Handlers.NetHandler;

namespace KeywordsForTesting
{
    public class TestMultipleKeywords : AbstractKeyword
    {
        [Keyword(name = "My Other Keyword")]
        public void MyKeyword()
        {
            // We need a separate Assembly for testing "TestScriptRunnerMultipleKeywords"
        }
        public override void BeforeKeyword(Keyword annotation)
        {
            output.Add("BeforeKeyword", annotation.name);
        }

        public override void AfterKeyword(Keyword annotation)
        {
            output.Add("AfterKeyword", annotation.name);
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
