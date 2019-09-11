using Step.Handlers.NetHandler;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace KeywordsForTesting
{
    public class TestMultipleKeywords : AbstractKeyword
    {
        [Keyword(name = "My Other Keyword")]
        public void MyKeyword()
        {
            // We need a separate Assembly for testing "TestScriptRunnerMultipleKeywords"
        }
    }
}
