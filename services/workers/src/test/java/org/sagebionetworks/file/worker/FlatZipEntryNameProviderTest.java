package org.sagebionetworks.file.worker;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

public class FlatZipEntryNameProviderTest {

	Long fileHandleId;
	FlatZipEntryNameProvider provider;

	@Before
	public void before() {
		provider = new FlatZipEntryNameProvider();
		fileHandleId = 123L;
	}

	@Test
	public void testDuplicatePrefix() {
		String name = "foo.txt";
		// call under test
		assertEquals("foo.txt", provider.createZipEntryName(name, fileHandleId));
		assertEquals("foo(1).txt", provider.createZipEntryName(name, fileHandleId));
		assertEquals("foo(2).txt", provider.createZipEntryName(name, fileHandleId));
	}

	@Test
	public void testDuplicatePrefixDifferentSuffix() {
		String name = "foo.txt";
		String name2 = "foo.bar";
		
		assertEquals("foo.txt", provider.createZipEntryName(name, fileHandleId));
		assertEquals("foo.bar", provider.createZipEntryName(name2, fileHandleId));
	}
	
	@Test
	public void testExistingGeneratedName() {
		String name = "foo(1).txt";
		String name2 = "foo.txt";
	
		assertEquals("foo(1).txt", provider.createZipEntryName(name, fileHandleId));
		assertEquals("foo.txt", provider.createZipEntryName(name2, fileHandleId));
	}
	
	@Test
	public void testDuplicateExistingGeneratedName() {
		String name = "foo(1).txt";
	
		assertEquals("foo(1).txt", provider.createZipEntryName(name, fileHandleId));
		assertEquals("foo(1)(1).txt", provider.createZipEntryName(name, fileHandleId));
		assertEquals("foo(1)(2).txt", provider.createZipEntryName(name, fileHandleId));
	}
	
	@Test
	public void testExistingGeneratedNameWithRepetition() {
		String name = "foo(1).txt";
		String name2 = "foo.txt";
	
		assertEquals("foo(1).txt", provider.createZipEntryName(name, fileHandleId));
		assertEquals("foo.txt", provider.createZipEntryName(name2, fileHandleId));
		assertEquals("foo(2).txt", provider.createZipEntryName(name2, fileHandleId));
	}
	
	@Test
	public void testExistingGeneratedNameWithGap() {
		String name = "foo(2).txt";
		String name2 = "foo.txt";
	
		assertEquals("foo(2).txt", provider.createZipEntryName(name, fileHandleId));
		assertEquals("foo.txt", provider.createZipEntryName(name2, fileHandleId));
		assertEquals("foo(1).txt", provider.createZipEntryName(name2, fileHandleId));
		assertEquals("foo(3).txt", provider.createZipEntryName(name2, fileHandleId));
		assertEquals("foo(2)(1).txt", provider.createZipEntryName(name, fileHandleId));
	}
	
	@Test
	public void testExistingGeneratedNameAndDifferentSuffix() {
		String name = "foo(1).txt";
		String name2 = "foo.bar";
	
		assertEquals("foo(1).txt", provider.createZipEntryName(name, fileHandleId));
		assertEquals("foo.bar", provider.createZipEntryName(name2, fileHandleId));
		assertEquals("foo(1).bar", provider.createZipEntryName(name2, fileHandleId));
	}

	@Test
	public void testNoSufix() {
		String name = "foo";
		assertEquals("foo", provider.createZipEntryName(name, fileHandleId));
		assertEquals("foo(1)", provider.createZipEntryName(name, fileHandleId));
		assertEquals("foo(2)", provider.createZipEntryName(name, fileHandleId));
	}

	@Test
	public void testMultipleDots() {
		String name = "foo.xml.zip";
		// call under test
		assertEquals("foo.xml.zip", provider.createZipEntryName(name, fileHandleId));
		assertEquals("foo(1).xml.zip", provider.createZipEntryName(name, fileHandleId));
		assertEquals("foo(2).xml.zip", provider.createZipEntryName(name, fileHandleId));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testNullName() {
		String name = null;
		// call under test
		provider.createZipEntryName(name, fileHandleId);
	}

	@Test
	public void testNullFileHandle() {
		String name = "foo.txt";
		fileHandleId = null;
		// call under test
		assertEquals("foo.txt", provider.createZipEntryName(name, fileHandleId));
	}
}
