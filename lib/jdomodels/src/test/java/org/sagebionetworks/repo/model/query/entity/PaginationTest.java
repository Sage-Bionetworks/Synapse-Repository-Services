package org.sagebionetworks.repo.model.query.entity;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

public class PaginationTest {

	long limit;
	long offset;
	
	@Before
	public void before(){
		limit = 101;
		offset = 12;
	}
	
	@Test
	public void testToSql(){
		Pagination pagination = new Pagination(limit, offset);
		assertEquals(" LIMIT :bLimit OFFSET :bOffset", pagination.toSql());
	}

}
