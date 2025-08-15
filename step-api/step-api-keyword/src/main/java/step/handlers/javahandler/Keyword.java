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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Keyword {

	/** Reserved keyword for the routing element; execution will occur on the controller rather than on agents */
    String EXECUTE_ON_CONTROLLER = "controller";
	/**
	 * @return the name of this keyword. If not specified the method name is 
	 * used as keyword name
	 */
	String name() default "";

	/**
	 * @return the description of this keyword.
	 */
	String description() default "";
	
	/**
	 * @return the list of properties required by this keyword
	 */
	String[] properties() default {};
	
	/**
	 * @return the list of optional properties which might be used by this keyword
	 */
	String[] optionalProperties() default {};
	
	/**
	 * @return the JSON schema of the input object. 
	 * @see <a href="https://json-schema.org/"> json-schema.org </a>
	 */
	String schema() default "";

	/**
	 * @return the call timeout for this keyword in milliseconds
	 */
	int timeout() default 180000;

	/**
	 * @return the reference to file with implementing plan (for composite keyword only)
	 */
	String planReference() default "";

	/**
	 *
	 * @return the routing criteria which can be either <ul>
	 *     <li>empty: use default routing on Step</li>
	 *     <li>{@link #EXECUTE_ON_CONTROLLER}: execute the keyword directly on the controller rather than on agents</li>
	 *     <li>"agent_attribute_key_1","agent_attribute_value_1",...: the agent token selection criteria provided as a list of key-value pairs</li>
	 * </ul>
	 */
	String[] routing() default {};
}
