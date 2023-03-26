package step.handlers.javahandler;

import java.lang.annotation.*;

@Target({ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface JsonSchemaPropertyRef {
	String id();
	JsonSchemaProperty property();
}
