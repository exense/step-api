using Step.Handlers.NetHandler;

namespace KeywordsForTesting
{
    public class TestMultipleKeywords : AbstractKeyword
    {
        [Keyword(name = "My Other Keyword", htmlTemplate = "<label>KeywordsForTesting</label>")]
        public void MyKeyword()
        {
            // We need a separate Assembly for testing "TestScriptRunnerMultipleKeywords"
        }
    }
}
