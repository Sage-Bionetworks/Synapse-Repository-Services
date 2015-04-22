package org.sagebionetworks.repo.web.filter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.entity.ContentType;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

public class PythonClientFilterTest {

	@Before
	public void setUp() throws Exception {
	}
	
	void assertEqualsArrays(int[] expected, int[] actual) {
		assertEquals(expected.length, actual.length);
		for (int i=0; i<expected.length; i++) {
			assertEquals(expected[i], actual[i]);
		}
	}


	@Test
	public void testVersionToTuple() {
		assertEqualsArrays(new int[]{1,2,3,0,0}, PythonClientFilter.versionToTuple("1.2.3"));
		assertEqualsArrays(new int[]{1,2,0,0,0}, PythonClientFilter.versionToTuple("1.2"));
		assertEqualsArrays(new int[]{1,0,0,0,0}, PythonClientFilter.versionToTuple("1"));
		assertEqualsArrays(new int[]{0,5,1,PythonClientFilter.DEV,1}, PythonClientFilter.versionToTuple("0.5.1.dev1"));
		assertEqualsArrays(new int[]{1,0,0,PythonClientFilter.DEV,3}, PythonClientFilter.versionToTuple("1.0.dev3"));
		assertEqualsArrays(new int[]{2,5,0,PythonClientFilter.DEV,4}, PythonClientFilter.versionToTuple("2.5.dev4"));
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testVersionToTupleIllegalNAN() {
		PythonClientFilter.versionToTuple("1.2FOO");
	}

	@Test(expected=IllegalArgumentException.class)
	public void testVersionToTupleIllegalTOOSHORT() {
		PythonClientFilter.versionToTuple("");
	}

	@Test(expected=IllegalArgumentException.class)
	public void testVersionToTupleIllegalTOOLONG() {
		PythonClientFilter.versionToTuple("1.2.3.4");
	}
	
	@Test
	public void testCompareVersions() {
		assertEquals(-1, PythonClientFilter.compareVersions("1.0.0", "1.0.1"));
		assertEquals(-1, PythonClientFilter.compareVersions("1.0", "1.0.1"));
		assertEquals( 1, PythonClientFilter.compareVersions("2.0.0", "1.0.1"));
		assertEquals( 1, PythonClientFilter.compareVersions("2", "1.0.1"));
		assertEquals(0, PythonClientFilter.compareVersions("1.2.3", "1.2.3"));
		assertEquals(0, PythonClientFilter.compareVersions("1.2", "1.2.0"));
		assertEquals(0, PythonClientFilter.compareVersions("1", "1.0.0"));
		assertEquals(0, PythonClientFilter.compareVersions("1", "1"));
		
	}

	@Test
	public void testCompareNonNumericVersionStrings() {
		assertEquals(-1, PythonClientFilter.compareVersions("1.2.3.dev1", "1.2.3.dev2"));
		assertEquals(-1, PythonClientFilter.compareVersions("1.2.3.dev1", "1.2.3"));
		assertEquals(0, PythonClientFilter.compareVersions("1.2.3.dev7", "1.2.3.dev7"));
		assertEquals(1, PythonClientFilter.compareVersions("1.2.3.dev123", "1.2.3.dev97"));
		assertEquals(1, PythonClientFilter.compareVersions("1.2.3.dev123", "1.2.2"));
	}


	@Test
	public void testIsAffectedPythonClient() {
		assertTrue(PythonClientFilter.isAffectedPythonClient("python-requests/1.2.3 cpython/2.7.4 linux/3.8.0-19-generic"));
		assertTrue(PythonClientFilter.isAffectedPythonClient("synapseclient/1.0.1 python-requests/2.1.0 cpython/2.7.3 linux/3.2.0-54-virtual"));
		assertTrue(PythonClientFilter.isAffectedPythonClient("synapseclient/0.5.2 python-requests/2.2.1 cpython/2.7.3 linux/3.2.0-57-virtual"));
		assertTrue(PythonClientFilter.isAffectedPythonClient("synapseclient/0.5.2 python-requests/2.2.1"));
		assertTrue(PythonClientFilter.isAffectedPythonClient("python-requests/2.2.1 synapseclient/0.5.2"));

		assertTrue(PythonClientFilter.isAffectedPythonClient("synapseclient/0.5.1.dev1 python-requests/2.2.1 cpython/2.7.6"));
		assertTrue(PythonClientFilter.isAffectedPythonClient("synapseclient/1.0.dev3 python-requests/2.2.1 cpython/2.7.6"));
		assertTrue(PythonClientFilter.isAffectedPythonClient("synapseclient/1.0.1.dev3 python-requests/2.2.1 cpython/2.7.6"));

		assertFalse(PythonClientFilter.isAffectedPythonClient("synapseclient/1.0.2 python-requests/2.1.0 cpython/2.7.3 linux/3.2.0-54-virtual"));
		assertFalse(PythonClientFilter.isAffectedPythonClient("synapseclient/1.0.1 cpython/2.7.3 linux/3.2.0-54-virtual"));
		assertFalse(PythonClientFilter.isAffectedPythonClient("synapseclient/7.8.unparsab1ejunk23 cpython/2.7.3 linux/3.2.0-54-virtual"));
	}

	@Test
	public void testFilter() throws Exception {
		PythonClientFilter filter = new PythonClientFilter();
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("User-Agent", "python-requests/foo");
		HttpServletResponse response = new MockHttpServletResponse();
		FilterChain filterChain = new FilterChain() {
			@Override
			public void doFilter(ServletRequest request,
					ServletResponse response) throws IOException,
					ServletException {
				response.setContentType("application/json; charset=ISO-8859-1");
				//response.addHeader("Content-Type", "application/json; charset=ISO-8859-1");
			}};
		filter.doFilter(request, response, filterChain);
		ContentType responseContentType = ContentType.parse(response.getContentType());
		assertNull(responseContentType.getCharset());

		String contentTypeHeader = response.getHeader("Content-Type");
		assertEquals("application/json", contentTypeHeader);
	}

}
