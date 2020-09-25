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

import step.functions.io.Output;

@SuppressWarnings("serial")
public class KeywordException extends Exception {

	private final Output<?> output;
	
	public KeywordException(Output<?> output, Throwable cause) {
		super(getMessage(output), cause);
		this.output = output;
	}

	public KeywordException(Output<?> output) {
		super(getMessage(output));
		this.output = output;
	}
	
	private static String getMessage(Output<?> output) {
		return output.getError()!=null?output.getError().getMsg():"Undefined keyword error message";
	}

	public Output<?> getOutput() {
		return output;
	}

}
