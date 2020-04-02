/*******************************************************************************
 * (C) Copyright 2018 Jerome Comte and Dorian Cransac
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
 *******************************************************************************/
package step.functions.io;


import org.junit.Assert;
import org.junit.Test;

public class AbstractSessionTest {

	@Test
	public void test() throws Exception {
		try (AbstractSession session = new AbstractSession()) {
			session.put("test", "value1");
			
			String res = (String) session.get("test");
			Assert.assertEquals("value1",res);
			
			res = (String) session.get("test2");
			Assert.assertNull(res);
			
			res = (String) session.getOrDefault("test","value2");
			Assert.assertEquals("value1",res);
			
			res = (String) session.getOrDefault("test2","value2");
			Assert.assertEquals("value2",res);
			res = (String) session.get("test2");
			Assert.assertEquals("value2",res);
		}
	}
}
