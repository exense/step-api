package step.handlers.javahandler;

import org.junit.Test;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

import static org.junit.Assert.assertEquals;

public class JsonObjectMapperTest {

    public static final String MY_STRING = "myString";
    public static final String MY_INTEGER = "myInteger";
    public static final String MY_LONG = "myLong";
    public static final String MY_DOUBLE = "myDouble";
    public static final String MY_BIG_DECIMAL = "myBigDecimal";
    public static final String MY_BIG_INTEGER = "myBigInteger";
    public static final String MY_BOOLEAN = "myBoolean";
    public static final String MY_POJO = "myPojo";
    public static final String MY_LIST = "myList";
    public static final String STRING = "test";
    public static final int INT = 1;
    public static final long LONG = 1234567891023L;
    public static final double DOUBLE = 0.1;
    public static final BigDecimal BIG_DECIMAL = new BigDecimal(10);
    public static final BigInteger BIG_INTEGER = new BigInteger("100");
    public static final boolean BOOLEAN = true;
    public static final Pojo POJO = new Pojo();
    public static final List<Pojo> LIST = Arrays.asList(POJO);
    public static final String MY_NULL_OBJECT = "myNullObject";
    public static final String MY_OBJECT = "myObject";
    public static final PojoPrivateFields POJO_WITH_PRIVATE_FIELDS = new PojoPrivateFields();

    @Test
    public void addValueToJsonObject() {
        JsonObject jsonObject = JsonObjectMapper.javaObjectToJsonObject(POJO);
        JsonObject pojoAsJson = pojoAsJson().build();
        assertEquals(pojoAsJson, jsonObject);
        JsonObject jsonObjectPrivateFields = JsonObjectMapper.javaObjectToJsonObject(POJO_WITH_PRIVATE_FIELDS);
        assertEquals(pojoAsJson, jsonObjectPrivateFields);
    }

    @Test
    public void jsonValueToObject() {
        Object pojo = JsonObjectMapper.jsonValueToJavaObject(pojoAsJson().build(), Pojo.class);
        assertEquals(POJO, pojo);

        Object pojoWithPrivateFields = JsonObjectMapper.jsonValueToJavaObject(pojoAsJson().build(), PojoPrivateFields.class);
        assertEquals(POJO_WITH_PRIVATE_FIELDS, pojoWithPrivateFields);
    }

    @Test
    public void testAddValueToJsonObject() {
        JsonObjectBuilder inputBuilder = Json.createObjectBuilder();
        JsonObjectMapper.addValueToJsonObject(inputBuilder, MY_STRING, STRING);
        JsonObjectMapper.addValueToJsonObject(inputBuilder, MY_INTEGER, INT);
        JsonObjectMapper.addValueToJsonObject(inputBuilder, MY_LONG, LONG);
        JsonObjectMapper.addValueToJsonObject(inputBuilder, MY_DOUBLE, DOUBLE);
        JsonObjectMapper.addValueToJsonObject(inputBuilder, MY_BIG_DECIMAL, BIG_DECIMAL);
        JsonObjectMapper.addValueToJsonObject(inputBuilder, MY_BIG_INTEGER, BIG_INTEGER);
        JsonObjectMapper.addValueToJsonObject(inputBuilder, MY_BOOLEAN, BOOLEAN);
        JsonObjectMapper.addValueToJsonObject(inputBuilder, MY_NULL_OBJECT, null);
        JsonObjectMapper.addValueToJsonObject(inputBuilder, MY_OBJECT, new myPojo());
        JsonObjectMapper.addValueToJsonObject(inputBuilder,"myStringList", List.of(STRING, "value2"));
        JsonObjectMapper.addValueToJsonObject(inputBuilder,"myIntegerList", List.of(INT, 2));
        JsonObjectMapper.addValueToJsonObject(inputBuilder,"myLongList", List.of(LONG, 2L));
        JsonObjectMapper.addValueToJsonObject(inputBuilder,"myDoubleList", List.of(DOUBLE, 2.03));
        JsonObjectMapper.addValueToJsonObject(inputBuilder,"myBigDecimalList", List.of(BIG_DECIMAL, new BigDecimal(2)));
        JsonObjectMapper.addValueToJsonObject(inputBuilder,"myBigIntegerList", List.of(BIG_INTEGER));
        JsonObjectMapper.addValueToJsonObject(inputBuilder,"myBooleanList", List.of(BOOLEAN, false));
        List<String> listOfNull = new ArrayList<>();
        listOfNull.add(null);
        JsonObjectMapper.addValueToJsonObject(inputBuilder,"myListWithNullValues", listOfNull);
        JsonObjectMapper.addValueToJsonObject(inputBuilder,"myListOfList", List.of( List.of(STRING, "value2"),  List.of(STRING, "value2")));

        JsonObjectMapper.addValueToJsonObject(inputBuilder,"myMapOfString", new HashMap<>(Map.of("key1", "value1", "key2", "value2" )));

        JsonObject jsonObject = inputBuilder.build();
        assertEquals("{\"myString\":\"test\",\"myInteger\":1,\"myLong\":" + LONG + ",\"myDouble\":0.1,\"myBigDecimal\":10,\"myBigInteger\":100,\"myBoolean\":true,\"myNullObject\":null,\"myObject\":{\"test\":\"test\"},\"myStringList\":[\"test\",\"value2\"],\"myIntegerList\":[1,2],\"myLongList\":[" + LONG + ",2],\"myDoubleList\":[0.1,2.03],\"myBigDecimalList\":[10,2],\"myBigIntegerList\":[100],\"myBooleanList\":[true,false],\"myListWithNullValues\":[null],\"myListOfList\":[[\"test\",\"value2\"],[\"test\",\"value2\"]],\"myMapOfString\":{\"key1\":\"value1\",\"key2\":\"value2\"}}",
                jsonObject.toString());

    }

    public static class myPojo {
        public String test = "test";
    }

    protected static JsonObjectBuilder pojoAsJson() {
        JsonObjectBuilder builder = pojo2AsJson();
        builder.add(MY_LIST, Json.createArrayBuilder().add(pojo2AsJson().build()));
        builder.add(MY_POJO, pojo2AsJson().build());
        builder.add("myStringList", Json.createArrayBuilder().add(STRING).build());
        builder.add("myIntegerList", Json.createArrayBuilder().add(INT).build());
        builder.add("myLongList", Json.createArrayBuilder().add(LONG).build());
        builder.add("myDoubleList", Json.createArrayBuilder().add(DOUBLE).build());
        builder.add("myBigDecimalList", Json.createArrayBuilder().add(BIG_DECIMAL).build());
        builder.add("myBigIntegerList", Json.createArrayBuilder().add(BIG_INTEGER).build());
        builder.add("myBooleanList", Json.createArrayBuilder().add(BOOLEAN).build());
        builder.add("myListWithNullValues", Json.createArrayBuilder().addNull().build());
        builder.add("myListWithObjectValues" ,Json.createArrayBuilder().add(STRING).add(BOOLEAN).add(INT).add(LONG).add(DOUBLE).build());
        builder.add("myListOfList", Json.createArrayBuilder().add(Json.createArrayBuilder().add(STRING).build()).build());
        builder.add("mapOfPojos", Json.createObjectBuilder().add("key1", pojo2AsJson()).add( "key2", pojo2AsJson()).build());
        builder.add("mapOfStrings", Json.createObjectBuilder().add("key1",  STRING).add( "key2", STRING).build());
        builder.add("mapOfIntegers", Json.createObjectBuilder().add("key1",  INT).add( "key2", INT).build());
        builder.add("mapOfLongs", Json.createObjectBuilder().add("key1",  LONG).add( "key2", LONG).build());
        builder.add("mapOfDoubles", Json.createObjectBuilder().add("key1",  DOUBLE).add( "key2", DOUBLE).build());
        builder.add("mapOfBigDecimals", Json.createObjectBuilder().add("key1",  BIG_DECIMAL).add( "key2", BIG_DECIMAL).build());
        builder.add("mapOfBigIntegers", Json.createObjectBuilder().add("key1",  BIG_INTEGER).add( "key2", BIG_INTEGER).build());
        builder.add("mapOfBooleans", Json.createObjectBuilder().add("key1",  BOOLEAN).add( "key2", BOOLEAN).build());
        builder.add("mapOfObjects", Json.createObjectBuilder().add("key1",  STRING).add( "key2", INT).build());
        return builder;
    }

    private static JsonObjectBuilder pojo2AsJson() {
        JsonObjectBuilder builder = Json.createObjectBuilder();
        builder.add(MY_STRING, STRING);
        builder.add(MY_INTEGER, INT);
        builder.add(MY_LONG, LONG);
        builder.add(MY_DOUBLE, DOUBLE);
        builder.add(MY_BIG_DECIMAL, BIG_DECIMAL);
        builder.add(MY_BIG_INTEGER, BIG_INTEGER);
        builder.add(MY_BOOLEAN, BOOLEAN);
        builder.addNull(MY_NULL_OBJECT);
        builder.add("stringArray", Json.createArrayBuilder().add(STRING).add(STRING).build());
        return builder;
    }

    public static class Pojo extends Pojo2 {
        List<Pojo2> myList = Arrays.asList(new Pojo2());
        List<String> myStringList = Arrays.asList(STRING);
        List<Integer> myIntegerList = Arrays.asList(INT);
        List<Long> myLongList = Arrays.asList(LONG);
        List<Double> myDoubleList = Arrays.asList(DOUBLE);
        List<BigDecimal> myBigDecimalList = Arrays.asList(BIG_DECIMAL);
        List<BigInteger> myBigIntegerList = Arrays.asList(BIG_INTEGER);
        List<Boolean> myBooleanList = Arrays.asList(BOOLEAN);
        List<Object> myListWithNullValues = new ArrayList<>();
        {
            myListWithNullValues.add(null);
        }
        List<Object> myListWithObjectValues = Arrays.asList(STRING, BOOLEAN, INT, LONG, DOUBLE);
        List<Collection<String>> myListOfList = Arrays.asList(Arrays.asList(STRING));
        Pojo2 myPojo = new Pojo2();
        Map<String, Pojo2> mapOfPojos = Map.of("key1", new Pojo2(), "key2", new Pojo2());
        Map<String, String> mapOfStrings = Map.of("key1", STRING, "key2", STRING);
        Map<String, Integer> mapOfIntegers = Map.of("key1", INT, "key2", INT);
        Map<String, Long> mapOfLongs = Map.of("key1", LONG, "key2", LONG);
        Map<String, Double> mapOfDoubles = Map.of("key1", DOUBLE, "key2", DOUBLE);
        Map<String, BigDecimal> mapOfBigDecimals =  Map.of("key1", BIG_DECIMAL, "key2", BIG_DECIMAL);
        Map<String, BigInteger> mapOfBigIntegers = Map.of("key1", BIG_INTEGER, "key2", BIG_INTEGER);
        Map<String, Boolean> mapOfBooleans = Map.of("key1", BOOLEAN, "key2", BOOLEAN);
        Map<String, Object> mapOfObjects = Map.of("key1", STRING, "key2", INT);

        @Override
        public boolean equals(Object object) {
            if (this == object) return true;
            if (!(object instanceof Pojo)) return false;
            if (!super.equals(object)) return false;
            Pojo pojo = (Pojo) object;
            return Objects.equals(myList, pojo.myList) && Objects.equals(myStringList, pojo.myStringList) && Objects.equals(myIntegerList, pojo.myIntegerList) && Objects.equals(myLongList, pojo.myLongList) && Objects.equals(myDoubleList, pojo.myDoubleList) && Objects.equals(myBigDecimalList, pojo.myBigDecimalList) && Objects.equals(myBigIntegerList, pojo.myBigIntegerList) && Objects.equals(myBooleanList, pojo.myBooleanList) && Objects.equals(myListWithNullValues, pojo.myListWithNullValues) && Objects.equals(myListWithObjectValues, pojo.myListWithObjectValues) && Objects.equals(myListOfList, pojo.myListOfList) && Objects.equals(myPojo, pojo.myPojo) && Objects.equals(mapOfPojos, pojo.mapOfPojos) && Objects.equals(mapOfStrings, pojo.mapOfStrings) && Objects.equals(mapOfIntegers, pojo.mapOfIntegers) && Objects.equals(mapOfLongs, pojo.mapOfLongs) && Objects.equals(mapOfDoubles, pojo.mapOfDoubles) && Objects.equals(mapOfBigDecimals, pojo.mapOfBigDecimals) && Objects.equals(mapOfBigIntegers, pojo.mapOfBigIntegers) && Objects.equals(mapOfBooleans, pojo.mapOfBooleans) && Objects.equals(mapOfObjects, pojo.mapOfObjects);
        }

        @Override
        public int hashCode() {
            return Objects.hash(myList, myStringList, myIntegerList, myLongList, myDoubleList, myBigDecimalList, myBigIntegerList, myBooleanList, myListWithNullValues, myListWithObjectValues, myListOfList, myPojo, mapOfPojos, mapOfStrings, mapOfIntegers, mapOfLongs, mapOfDoubles, mapOfBigDecimals, mapOfBigIntegers, mapOfBooleans, mapOfObjects);
        }
    }

    public static class Pojo2 {
        String myString = STRING;
        int myInteger = INT;
        long myLong = LONG;
        double myDouble = DOUBLE;
        BigDecimal myBigDecimal = BIG_DECIMAL;
        BigInteger myBigInteger = BIG_INTEGER;
        boolean myBoolean = BOOLEAN;
        Object myNullObject = null;
        String[] stringArray = {STRING, STRING};

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Pojo2 pojo2 = (Pojo2) o;
            return myInteger == pojo2.myInteger && myLong == pojo2.myLong && Double.compare(pojo2.myDouble, myDouble) == 0 && myBoolean == pojo2.myBoolean && Objects.equals(myString, pojo2.myString) && Objects.equals(myBigDecimal, pojo2.myBigDecimal) && Objects.equals(myBigInteger, pojo2.myBigInteger) && Objects.equals(myNullObject, pojo2.myNullObject);
        }
    }

    public static class PojoPrivateFields  {
        private String myString = STRING;
        private int myInteger = INT;
        private long myLong = LONG;
        private double myDouble = DOUBLE;
        private BigDecimal myBigDecimal = BIG_DECIMAL;
        private BigInteger myBigInteger = BIG_INTEGER;
        private boolean myBoolean = BOOLEAN;
        private Object myNullObject = null;
        private String[] stringArray = {STRING, STRING};
        private List<Pojo2> myList = Arrays.asList(new Pojo2());
        private List<String> myStringList = Arrays.asList(STRING);
        private List<Integer> myIntegerList = Arrays.asList(INT);
        private List<Long> myLongList = Arrays.asList(LONG);
        private List<Double> myDoubleList = Arrays.asList(DOUBLE);
        private List<BigDecimal> myBigDecimalList = Arrays.asList(BIG_DECIMAL);
        private List<BigInteger> myBigIntegerList = Arrays.asList(BIG_INTEGER);
        private List<Boolean> myBooleanList = Arrays.asList(BOOLEAN);
        private List<Object> myListWithNullValues = new ArrayList<>();
        {
            myListWithNullValues.add(null);
        }
        List<Object> myListWithObjectValues = Arrays.asList(STRING, BOOLEAN, INT, LONG, DOUBLE);
        private List<Collection<String>> myListOfList = Arrays.asList(Arrays.asList(STRING));
        private Pojo2 myPojo = new Pojo2();
        private Map<String, Pojo2> mapOfPojos = Map.of("key1", new Pojo2(), "key2", new Pojo2());
        private Map<String, String> mapOfStrings = Map.of("key1", STRING, "key2", STRING);
        private Map<String, Integer> mapOfIntegers = Map.of("key1", INT, "key2", INT);
        private Map<String, Long> mapOfLongs = Map.of("key1", LONG, "key2", LONG);
        private Map<String, Double> mapOfDoubles = Map.of("key1", DOUBLE, "key2", DOUBLE);
        private Map<String, BigDecimal> mapOfBigDecimals =  Map.of("key1", BIG_DECIMAL, "key2", BIG_DECIMAL);
        private Map<String, BigInteger> mapOfBigIntegers = Map.of("key1", BIG_INTEGER, "key2", BIG_INTEGER);
        private Map<String, Boolean> mapOfBooleans = Map.of("key1", BOOLEAN, "key2", BOOLEAN);
        private Map<String, Object> mapOfObjects = Map.of("key1", STRING, "key2", INT);


        @Override
        public boolean equals(Object object) {
            if (this == object) return true;
            if (!(object instanceof PojoPrivateFields)) return false;
            PojoPrivateFields that = (PojoPrivateFields) object;
            return myInteger == that.myInteger && myLong == that.myLong && Double.compare(myDouble, that.myDouble) == 0 && myBoolean == that.myBoolean && Objects.equals(myString, that.myString) && Objects.equals(myBigDecimal, that.myBigDecimal) && Objects.equals(myBigInteger, that.myBigInteger) && Objects.equals(myNullObject, that.myNullObject) && Objects.deepEquals(stringArray, that.stringArray) && Objects.equals(myList, that.myList) && Objects.equals(myStringList, that.myStringList) && Objects.equals(myIntegerList, that.myIntegerList) && Objects.equals(myLongList, that.myLongList) && Objects.equals(myDoubleList, that.myDoubleList) && Objects.equals(myBigDecimalList, that.myBigDecimalList) && Objects.equals(myBigIntegerList, that.myBigIntegerList) && Objects.equals(myBooleanList, that.myBooleanList) && Objects.equals(myListWithNullValues, that.myListWithNullValues) && Objects.equals(myListWithObjectValues, that.myListWithObjectValues) && Objects.equals(myListOfList, that.myListOfList) && Objects.equals(myPojo, that.myPojo) && Objects.equals(mapOfPojos, that.mapOfPojos) && Objects.equals(mapOfStrings, that.mapOfStrings) && Objects.equals(mapOfIntegers, that.mapOfIntegers) && Objects.equals(mapOfLongs, that.mapOfLongs) && Objects.equals(mapOfDoubles, that.mapOfDoubles) && Objects.equals(mapOfBigDecimals, that.mapOfBigDecimals) && Objects.equals(mapOfBigIntegers, that.mapOfBigIntegers) && Objects.equals(mapOfBooleans, that.mapOfBooleans) && Objects.equals(mapOfObjects, that.mapOfObjects);
        }

        @Override
        public int hashCode() {
            return Objects.hash(myString, myInteger, myLong, myDouble, myBigDecimal, myBigInteger, myBoolean, myNullObject, Arrays.hashCode(stringArray), myList, myStringList, myIntegerList, myLongList, myDoubleList, myBigDecimalList, myBigIntegerList, myBooleanList, myListWithNullValues, myListWithObjectValues, myListOfList, myPojo, mapOfPojos, mapOfStrings, mapOfIntegers, mapOfLongs, mapOfDoubles, mapOfBigDecimals, mapOfBigIntegers, mapOfBooleans, mapOfObjects);
        }
    }
}