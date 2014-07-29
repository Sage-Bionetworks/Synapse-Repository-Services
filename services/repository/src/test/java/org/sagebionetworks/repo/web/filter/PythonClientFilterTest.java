package org.sagebionetworks.repo.web.filter;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

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
		assertEqualsArrays(new int[]{1,2,3}, PythonClientFilter.versionToTuple("1.2.3"));
		assertEqualsArrays(new int[]{1,2,0}, PythonClientFilter.versionToTuple("1.2"));
		assertEqualsArrays(new int[]{1,0,0}, PythonClientFilter.versionToTuple("1"));
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
	public void testIsAffectedPythonClient() {
		assertTrue(PythonClientFilter.isAffectedPythonClient("python-requests/1.2.3 cpython/2.7.4 linux/3.8.0-19-generic"));
		assertTrue(PythonClientFilter.isAffectedPythonClient("synapseclient/1.0.1 python-requests/2.1.0 cpython/2.7.3 linux/3.2.0-54-virtual"));
		assertTrue(PythonClientFilter.isAffectedPythonClient("synapseclient/0.5.2 python-requests/2.2.1 cpython/2.7.3 linux/3.2.0-57-virtual"));
		assertTrue(PythonClientFilter.isAffectedPythonClient("synapseclient/0.5.2 python-requests/2.2.1"));
		assertTrue(PythonClientFilter.isAffectedPythonClient("python-requests/2.2.1 synapseclient/0.5.2"));

		assertFalse(PythonClientFilter.isAffectedPythonClient("synapseclient/1.0.2 python-requests/2.1.0 cpython/2.7.3 linux/3.2.0-54-virtual"));
		assertFalse(PythonClientFilter.isAffectedPythonClient("synapseclient/1.0.1 cpython/2.7.3 linux/3.2.0-54-virtual"));

	}

}
