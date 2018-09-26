package org.sagebionetworks.file.worker;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

public class CommandLineCacheZipEntryNameProviderTest {
	
	CommandLineCacheZipEntryNameProvider provider;
	
	@Before
	public void before() {
		provider = new CommandLineCacheZipEntryNameProvider();
	}

	@Test
	public void testCreateZipEntryName() {
		assertEquals("321/321321/foo.txt",
				provider.createZipEntryName("foo.txt", 321321L));
	}

	@Test
	public void testCreateZipEntrySmall() {
		assertEquals("1/1/foo.txt",
				provider.createZipEntryName("foo.txt", 1L));
	}
}
