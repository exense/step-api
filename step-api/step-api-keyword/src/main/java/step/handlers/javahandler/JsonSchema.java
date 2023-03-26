package step.handlers.javahandler;

import java.lang.annotation.*;

@Target({ElementType.METHOD, ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface JsonSchema {

	String title() default "";

	String description() default "";

	JsonSchemaProperty[] properties() default {};

	JsonSchemaPropertyRef[] propertiesRefs() default {};
}
