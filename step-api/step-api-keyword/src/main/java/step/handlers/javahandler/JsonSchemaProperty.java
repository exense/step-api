package step.handlers.javahandler;

import java.lang.annotation.*;

@Target({ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface JsonSchemaProperty {
	String propertyRef() default "";
	String name() default "";
	String type() default "";
	boolean required() default false;
	String defaultV() default "";

	String[] nestedPropertiesRefs() default {};
}
