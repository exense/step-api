package step.handlers.javahandler;

import org.junit.Test;

import javax.json.Json;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static step.handlers.javahandler.JsonObjectMapperTest.*;

public class AbstractKeywordTest {

    @Test
    public void getInputOrProperty() {
        AbstractKeyword keyword = new AbstractKeyword();
        // Define the keyword input
        keyword.setInput(pojoAsJson().build());
        // Define the properties
        Map<String, String> properties = new HashMap<>();
        // MY_STRING is also present in the input. We add the same key to the properties in order to test the precedence
        properties.put(MY_STRING, "otherValue");
        // Following properties are not present in the input
        properties.put("myStringProperty", "my value");
        properties.put("myIntegerProperty", "1");
        properties.put("myLongProperty", Long.toString(Long.MAX_VALUE));
        properties.put("myBooleanProperty", "true");
        properties.put("myPojoProperty", pojoAsJson().build().toString());
        properties.put("myArrayProperty", Json.createArrayBuilder().add(pojoAsJson()).build().toString());
        keyword.setProperties(properties);

        // Ensure the correct values are returned
        assertEquals(STRING, keyword.getInputOrProperty(MY_STRING));
        assertEquals("my value", keyword.getInputOrProperty("myStringProperty"));
        assertEquals(INT, (int) keyword.getInputOrPropertyAsInteger(MY_INTEGER));
        assertEquals(1, (int) keyword.getInputOrPropertyAsInteger("myIntegerProperty"));
        assertEquals(LONG, (long) keyword.getInputOrPropertyAsLong(MY_LONG));
        assertEquals(1, (long) keyword.getInputOrPropertyAsLong("myIntegerProperty"));
        assertEquals(BOOLEAN, keyword.getInputOrPropertyAsBoolean(MY_BOOLEAN));
        assertEquals(true, keyword.getInputOrPropertyAsBoolean("myBooleanProperty"));
        assertEquals(new Pojo2(), keyword.getInputOrPropertyAsObject(MY_POJO, Pojo2.class));
        assertEquals(new Pojo2(), keyword.getInputOrPropertyAsObject("myPojoProperty", Pojo2.class));
        assertEquals(Arrays.asList(new Pojo2()), keyword.getInputOrPropertyAsList(MY_LIST, Pojo2.class));
        assertEquals(Arrays.asList(new Pojo2()), keyword.getInputOrPropertyAsList("myArrayProperty", Pojo2.class));
        assertNull(keyword.getInputOrPropertyAsBoolean("myNotExistingProperty"));
    }
}