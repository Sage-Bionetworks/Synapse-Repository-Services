package org.sagebionetworks.util.url;


import org.junit.jupiter.api.Test;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class UrlDataTest {

	@Test
	public void testRoundTrip() throws MalformedURLException {
		String urls[] = new String[] {
				"https://somehoset.org:8080/path/child?foo=bar&param2=value2#refs",
				"https://somehoset.org:8080/path/child?foo=",
				"http://foo.org",
				"http://foo.org#refs",
				"http://foo.org?foo=bar"
		};
		for (String startUlr : urls) {
			UrlData urlData = new UrlData(startUlr);
			URL url = urlData.toURL();
			assertNotNull(url);
			assertEquals(startUlr, url.toString());
		}
	}

	@Test
	public void testParseQueryStringEncoded() {
		String queryString = "key1=value%3D%3F%26&key2=value2%2F%3A%7B%7D%5B%5D%7C%40";
		LinkedHashMap<String, String> map = UrlData
				.parseQueryString(queryString);
		assertNotNull(map);
		assertEquals("value=?&", map.get("key1"));
		assertEquals("value2/:{}[]|@", map.get("key2"));
	}
	
	@Test
	public void testParseQueryStringWithEmptyValue() {
		String queryString = "key1=&key2=two";
		LinkedHashMap<String, String> map = UrlData
				.parseQueryString(queryString);
		assertNotNull(map);
		assertEquals("", map.get("key1"));
		assertEquals("two", map.get("key2"));
	}
	@Test
	public void testParseQueryStringNoKey() {
		String queryString = "=one&key2=two";
		LinkedHashMap<String, String> map = UrlData
				.parseQueryString(queryString);
		assertNotNull(map);
		assertEquals("two", map.get("key2"));
	}

	@Test
	public void testParseQueryStringNull() {
		String queryString = null;
		LinkedHashMap<String, String> map = UrlData
				.parseQueryString(queryString);
		assertNotNull(map);
		assertTrue(map.isEmpty());
	}

	@Test
	public void testParseQueryStringEmpty() {
		String queryString = "";
		LinkedHashMap<String, String> map = UrlData
				.parseQueryString(queryString);
		assertNotNull(map);
		assertTrue(map.isEmpty());
	}
	
	@Test
	public void testParseQueryStringEmptyValue() {
		String queryString = "key1=";
		LinkedHashMap<String, String> map = UrlData
				.parseQueryString(queryString);
		assertNotNull(map);
		assertEquals("", map.get("key1"));
	}
	
	@Test
	public void testToQueryString(){
		LinkedHashMap<String, String> queryParameters = new LinkedHashMap<String, String>();
		queryParameters.put("key1", "!@#$");
		queryParameters.put("key2", "two");
		queryParameters.put("key3", "%three");
		String queryString = UrlData.toQueryString(queryParameters);
		assertEquals("key1=%21%40%23%24&key2=two&key3=%25three", queryString);
	}
	
	@Test
	public void testToQueryStringEmpty(){
		LinkedHashMap<String, String> queryParameters = new LinkedHashMap<String, String>();
		String queryString = UrlData.toQueryString(queryParameters);
		assertEquals("", queryString);
	}

}
