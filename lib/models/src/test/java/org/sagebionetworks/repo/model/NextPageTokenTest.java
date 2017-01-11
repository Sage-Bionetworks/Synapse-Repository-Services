package org.sagebionetworks.repo.model;

import static org.junit.Assert.*;

import org.junit.Test;
import org.sagebionetworks.repo.model.NextPageToken;

public class NextPageTokenTest {

	@Test
	public void testNextPage(){
		long limit = 101;
		long offset = 51;
		NextPageToken token = new NextPageToken(limit, offset);
		assertEquals(limit, token.getLimit());
		assertEquals(offset, token.getOffset());
		String tokenString = token.toToken();
		NextPageToken clone = new NextPageToken(tokenString);
		assertEquals(limit, clone.getLimit());
		assertEquals(offset, clone.getOffset());
	}
	
	@Test (expected = IllegalArgumentException.class)
	public void testNextPageNull(){
		String tokenString = null;
		NextPageToken clone = new NextPageToken(tokenString);
	}
	
	@Test (expected = IllegalArgumentException.class)
	public void testNextPageBadToken(){
		String tokenString = "notatoken";
		NextPageToken clone = new NextPageToken(tokenString);
	}
	
	@Test (expected = IllegalArgumentException.class)
	public void testNextPageBadTokenTwo(){
		String tokenString = "1a";
		NextPageToken clone = new NextPageToken(tokenString);
	}
	
	@Test (expected = IllegalArgumentException.class)
	public void testNextPageBadTokenThree(){
		String tokenString = "a2";
		NextPageToken clone = new NextPageToken(tokenString);
	}
}
