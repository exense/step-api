package step.handlers.javahandler;

import org.junit.Test;
import step.functions.io.Output;

import javax.json.JsonObject;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class KeywordProxyTest {

    public static final String MY_SESSION_OBJECT = "mySessionObject";
    public static final String MY_OUTPUT_1 = "myOutput1";
    public static final String MY_OUTPUT_FROM_SESSION_OBJECT = "myOutputFromSessionObject";
    public static final String MY_OUTPUT_BEFORE_KEYWORD = "myOutputBeforeKeyword";
    public static final String MY_OUTPUT_AFTER_KEYWORD = "myOutputAfterKeyword";
    public static final String MY_DUMMY_OUTPUT = "test";
    public static final String MY_MEASURE = "myMeasure";

    @Test
    public void getProxy() {
        KeywordProxy keywordProxy = new KeywordProxy();
        TestKeywords proxy = keywordProxy.getProxy(TestKeywords.class);

        // Call myKeyword with an input
        String input1 = "blabla";
        proxy.myKeyword(input1);
        Output<JsonObject> lastOutput = keywordProxy.getLastOutput();
        assertEquals(MY_DUMMY_OUTPUT, lastOutput.getPayload().getString(MY_OUTPUT_BEFORE_KEYWORD));
        assertEquals(input1, lastOutput.getPayload().getString(MY_OUTPUT_1));
        assertEquals(MY_DUMMY_OUTPUT, lastOutput.getPayload().getString(MY_OUTPUT_AFTER_KEYWORD));
        assertFalse(lastOutput.getPayload().containsKey(MY_OUTPUT_FROM_SESSION_OBJECT));
        assertEquals(MY_MEASURE, lastOutput.getMeasures().get(0).getName());

        // Call myKeyword again with another input
        String input2 = "my other input";
        proxy.myKeyword(input2);
        assertEquals(input2, keywordProxy.getLastOutput().getPayload().getString(MY_OUTPUT_1));
        assertEquals(input1, keywordProxy.getLastOutput().getPayload().getString(MY_OUTPUT_FROM_SESSION_OBJECT));

        // Call another Keyword
        keywordProxy.getProxy(TestKeywords2.class).myKeyword2();
        // Ensure the session object created by the first keyword is available in this keyword too
        assertEquals(input2, keywordProxy.getLastOutput().getPayload().getString(MY_OUTPUT_1));
    }

    public static class TestKeywords extends AbstractKeyword {

        @Override
        public void beforeKeyword(String keywordName, Keyword annotation) {
            output.add(MY_OUTPUT_BEFORE_KEYWORD, MY_DUMMY_OUTPUT);
        }

        @Override
        public void afterKeyword(String keywordName, Keyword annotation) {
            output.add(MY_OUTPUT_AFTER_KEYWORD, MY_DUMMY_OUTPUT);
        }

        @Keyword
        public void myKeyword(@Input(name = "input1") String input1) {
            String mySessionObject = (String) session.get(MY_SESSION_OBJECT);
            if (mySessionObject != null) {
                output.add(MY_OUTPUT_FROM_SESSION_OBJECT, mySessionObject);
            }
            session.put(MY_SESSION_OBJECT, input1);
            output.add(MY_OUTPUT_1, input1);
            output.addMeasure(MY_MEASURE, 1);
        }
    }

    public static class TestKeywords2 extends AbstractKeyword {

        @Keyword
        public void myKeyword2() {
            output.add(MY_OUTPUT_1, (String) session.get(MY_SESSION_OBJECT));
        }
    }
}