package org.sagebionetworks.repo.model.query.jdo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.sagebionetworks.repo.model.query.Comparator;

public class ComparatorTest {
	
	
	@Test
	public void testGetSqlForAllComparator(){
		// Make sure we support all types
		Comparator[] all = Comparator.values();
		for(Comparator comp: all){
			String sql = comp.getSql();
			assertNotNull(sql);
		}
	}
	
	@Test
	public void testEquals(){
		assertEquals("=", Comparator.EQUALS.getSql());
	}

	@Test
	public void testGreater(){
		assertEquals(">", Comparator.GREATER_THAN.getSql());
	}
	
	@Test
	public void testLesss(){
		assertEquals("<", Comparator.LESS_THAN.getSql());
	}
	
	@Test
	public void testGreaterThanOrEquals(){
		assertEquals(">=", Comparator.GREATER_THAN_OR_EQUALS.getSql());
	}
	
	@Test
	public void testLessThanOrEquals(){
		assertEquals("<=", Comparator.LESS_THAN_OR_EQUALS.getSql());
	}
	
	@Test
	public void testIn(){
		assertEquals("IN", Comparator.IN.getSql());
	}
	
}
