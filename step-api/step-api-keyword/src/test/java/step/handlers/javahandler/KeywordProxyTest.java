package step.handlers.javahandler;

import org.junit.Test;
import step.functions.io.Output;

import javax.json.Json;
import javax.json.JsonObject;

import java.util.HashMap;

import static org.junit.Assert.*;

public class KeywordProxyTest{

    public static final String MY_SESSION_OBJECT = "mySessionObject";
    public static final String MY_TOKEN_SESSION_OBJECT = "myTokenSessionObject";
    public static final String MY_OUTPUT_1 = "myOutput1";
    public static final String MY_OUTPUT_2 = "myOutput2";
    public static final String MY_OUTPUT_FROM_SESSION_OBJECT = "myOutputFromSessionObject";
    public static final String MY_OUTPUT_BEFORE_KEYWORD = "myOutputBeforeKeyword";
    public static final String MY_OUTPUT_AFTER_KEYWORD = "myOutputAfterKeyword";
    public static final String MY_DUMMY_OUTPUT = "test";
    public static final String MY_MEASURE = "myMeasure";

    @Test
    public void testInput() {
        KeywordProxy keywordProxy = new KeywordProxy();
        MyKeywordWithInputFields proxy = keywordProxy.getProxy(MyKeywordWithInputFields.class);
        proxy.MyKeywordWithInputAnnotation(10, 100, true, "str", "str2");
        JsonObject outputPayload = keywordProxy.getLastOutput().getPayload();
        JsonObject expectedJson = Json.createObjectBuilder().add("numberFieldOut", 10)
                .add("primitiveIntOut", 100).add("booleanFieldOut", true)
                .add("stringField1Out", "str").add("stringField2Out", "str2").build();
        assertEquals(expectedJson, outputPayload);
    }

    @Test
    public void testProxyWithoutParent() {
        getProxy(new KeywordProxy());
    }

    @Test
    public void testProxyWithParent() throws Exception {
        KeywordRunner.ExecutionContext runner = KeywordRunner.getExecutionContext(new HashMap<>(), ParentKeyword.class);
        Output<JsonObject> output = runner.run("testParent","{\"merge\":false}");
        assertNull(output.getError());
        assertEquals("{\"parent_output\":\"test\"}", output.getPayload().toString());
        assertEquals(3, output.getMeasures().size());
        assertNull(output.getAttachments());
    }

    @Test
    public void testProxyWithParentAndMerge() throws Exception {
        KeywordRunner.ExecutionContext runner = KeywordRunner.getExecutionContext(new HashMap<>(), ParentKeyword.class);
        Output<JsonObject> output = runner.run("testParent", "{\"merge\":true}");
        assertNull(output.getError());
        assertEquals("{\"myOutputBeforeKeyword\":\"test\",\"myOutput1\":\"my other input\",\"myOutputAfterKeyword\":\"test\",\"myOutputFromSessionObject\":\"blabla\",\"myOutput2\":\"my other input\",\"parent_output\":\"test\"}", output.getPayload().toString());
        assertEquals(5, output.getMeasures().size());
        assertNull(output.getAttachments());
    }

    public static void getProxy(KeywordProxy keywordProxy) {
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
        assertEquals(input2, keywordProxy.getLastOutput().getPayload().getString(MY_OUTPUT_2));
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
            tokenSession.put(MY_TOKEN_SESSION_OBJECT, input1);
            output.add(MY_OUTPUT_1, input1);
            output.addMeasure(MY_MEASURE, 1);
        }
    }

    public static class TestKeywords2 extends AbstractKeyword {

        @Keyword
        public void myKeyword2() {
            output.add(MY_OUTPUT_1, (String) session.get(MY_SESSION_OBJECT));
            output.add(MY_OUTPUT_2, (String) tokenSession.get(MY_TOKEN_SESSION_OBJECT));
            if (session.get(MY_TOKEN_SESSION_OBJECT) != null) {
                throw new RuntimeException("The token session object was added to the token reservation session instead of the token session.");
            }
            if (tokenSession.get(MY_SESSION_OBJECT) != null) {
                throw new RuntimeException("The session object was added to the token session instead of the token reservation session.");
            }
        }
    }

    public  static class ParentKeyword extends AbstractKeyword {

        @Keyword
        public void testParent() {
            getProxy(new KeywordProxy(this, input.getBoolean("merge")));
            output.add("parent_output", "test");
        }
    }
}