package step.handlers.javahandler;

import java.lang.annotation.*;

@Target({ElementType.PARAMETER, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface Input {
	String name() default "";
	String defaultValue() default "";
	boolean required() default false;
}
