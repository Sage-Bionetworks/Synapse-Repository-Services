package org.sagebionetworks.repo.model;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.junit.Test;
import org.sagebionetworks.repo.model.NextPageToken;

public class NextPageTokenTest {

	@Test
	public void testNextPage(){
		long limit = 10;
		long offset = 2;
		NextPageToken token = new NextPageToken(limit, offset);
		assertEquals(limit, token.getLimit());
		assertEquals(offset, token.getOffset());
		String tokenString = token.toToken();
		NextPageToken clone = new NextPageToken(tokenString);
		assertEquals(limit, clone.getLimit());
		assertEquals(offset, clone.getOffset());
	}
	
	@Test
	public void testNextPageNull(){
		String tokenString = null;
		NextPageToken token = new NextPageToken(tokenString);
		assertNotNull(token);
		assertEquals(NextPageToken.DEFAULT_LIMIT, token.getLimit());
		assertEquals(NextPageToken.DEFAULT_OFFSET, token.getOffset());
	}
	
	@Test (expected = IllegalArgumentException.class)
	public void testNextPageBadToken(){
		String tokenString = "notatoken";
		new NextPageToken(tokenString);
	}
	
	@Test (expected = IllegalArgumentException.class)
	public void testNextPageBadTokenTwo(){
		String tokenString = "1a";
		new NextPageToken(tokenString);
	}
	
	@Test (expected = IllegalArgumentException.class)
	public void testNextPageBadTokenThree(){
		String tokenString = "a2";
		new NextPageToken(tokenString);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testConstructorWithDefaultLimitGreaterThanMaxLimit() {
		new NextPageToken(null, 11L, 10L);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testConstructorWithLimitTokenGreaterThanConstantMaxLimit() {
		new NextPageToken("51a0");
	}

	@Test (expected = IllegalArgumentException.class)
	public void testConstructorWithLimitTokenGreaterThanProvidedMaxLimit() {
		new NextPageToken("11a0", 10L, 10L);
	}

	@Test
	public void testConstructorWithProvidedDefaultLimitAndMaxLimit() {
		NextPageToken token = new NextPageToken("10a5", 100L, 100L);
		assertNotNull(token);
		assertEquals(10L, token.getLimit());
		assertEquals(5L, token.getOffset());
	}

	@Test
	public void testGetLimitForQuery() {
		NextPageToken token = new NextPageToken(10L, 0L);
		assertEquals(11L, token.getLimitForQuery());
	}

	@Test
	public void testGetNextPageTokenForCurrentListCaseNoNextPage() {
		NextPageToken token = new NextPageToken(10L, 0L);
		assertNull(token.getNextPageTokenForCurrentResults(Arrays.asList("1")));
	}

	@Test
	public void testGetNextPageTokenForCurrentListCaseNextPageExists() {
		NextPageToken token = new NextPageToken(1L, 0L);
		List<String> currentResults = new LinkedList<String>();
		currentResults.addAll(Arrays.asList("1", "2"));
		String nextPageToken = token.getNextPageTokenForCurrentResults(currentResults);
		assertNotNull(nextPageToken);
		assertEquals(new NextPageToken(1L, 1L), new NextPageToken(nextPageToken));
		assertEquals(1, currentResults.size());
		assertEquals("1", currentResults.get(0));
	}
}
