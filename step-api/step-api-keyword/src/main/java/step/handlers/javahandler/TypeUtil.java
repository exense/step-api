package step.handlers.javahandler;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Map;

public class TypeUtil {

    public static ParameterizedType mapOf(final Type keyType, final Type valueType) {
        return new ParameterizedType() {
            @Override
            public Type[] getActualTypeArguments() {
                return new Type[]{keyType, valueType};
            }

            @Override
            public Type getRawType() {
                return Map.class;
            }

            @Override
            public Type getOwnerType() {
                return null;
            }

            @Override
            public String toString() {
                return "Map<" + keyType.getTypeName() + ", " + valueType.getTypeName() + ">";
            }
        };
    }
}
