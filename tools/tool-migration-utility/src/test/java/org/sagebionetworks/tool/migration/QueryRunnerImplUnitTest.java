package org.sagebionetworks.tool.migration;

import static org.junit.Assert.*;

import org.junit.Test;
import org.sagebionetworks.tool.migration.dao.QueryRunnerImpl;

/**
 * Unit test for the query runner.
 * @author John
 *
 */
public class QueryRunnerImplUnitTest {

	@Test
	public void testGetNextOffset(){
		long offset = 1;
		long limit = 3;
		long total = 100;
		long next = QueryRunnerImpl.getNextOffset(offset, limit, total);
		assertEquals(4, next);
	}
	
	@Test
	public void testGetNextOffset2(){
		long offset = 4;
		long limit = 3;
		long total = 4;
		long next = QueryRunnerImpl.getNextOffset(offset, limit, total);
		assertEquals(-1, next);
	}
	@Test
	public void testGetNextOffset3(){
		long offset = 99;
		long limit = 10;
		long total = 101;
		long next = QueryRunnerImpl.getNextOffset(offset, limit, total);
		assertEquals(-1, next);
	}
	
	@Test
	public void testGetNextOffset4(){
		long offset = 1;
		long limit = 10;
		long total = 11;
		long next = QueryRunnerImpl.getNextOffset(offset, limit, total);
		assertEquals(11, next);
	}
}
