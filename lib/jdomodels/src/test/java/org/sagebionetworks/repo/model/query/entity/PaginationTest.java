package org.sagebionetworks.repo.model.query.entity;

import static org.junit.Assert.assertEquals;

import java.util.Map;

import org.junit.Before;
import org.junit.Test;

public class PaginationTest {

	long limit;
	long offset;
	
	Parameters parameters;
	
	@Before
	public void before(){
		limit = 101;
		offset = 12;
		parameters = new Parameters();
	}
	
	@Test
	public void testToSql(){
		Pagination pagination = new Pagination(limit, offset);
		assertEquals(" LIMIT :bLimit OFFSET :bOffset", pagination.toSql());
	}
	
	@Test
	public void testBindParameters(){
		Pagination pagination = new Pagination(limit, offset);
		pagination.bindParameters(parameters);
		Map<String, Object> results = parameters.getParameters();
		assertEquals(limit, results.get(Constants.BIND_LIMIT));
		assertEquals(offset, results.get(Constants.BIND_OFFSET));
	}

}
