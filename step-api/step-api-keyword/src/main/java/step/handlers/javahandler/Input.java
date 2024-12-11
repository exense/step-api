/*******************************************************************************
 * Copyright (C) 2020, exense GmbH
 *
 * This file is part of STEP
 *
 * STEP is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * STEP is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with STEP.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package step.handlers.javahandler;

import java.lang.annotation.*;

// In the past, it was possible to use this annotation to define default values in the members of nested classes
// This feature has been removed to simplify the implementation and @Input annotations can only be used to annotate
// method parameters now
@Target({ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface Input {
	String name() default "";
	String defaultValue() default "";
	boolean required() default false;
}
