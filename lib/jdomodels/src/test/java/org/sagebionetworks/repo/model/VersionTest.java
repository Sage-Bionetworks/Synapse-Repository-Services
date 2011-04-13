package org.sagebionetworks.repo.model;

import org.junit.Assert;
import org.junit.Test;
import org.sagebionetworks.repo.model.jdo.Version;

public class VersionTest {

	@Test
	public void testVersion() throws Exception {
		Version v = new Version("1.2.3");
		Assert.assertEquals("1.2.3", v.toString());
		Assert.assertEquals(".", v.getSeparator());
		Assert.assertEquals(3, v.getL().size());
	}

	@Test
	public void testNonInt() throws Exception {
		Version v = new Version("A.3");
		Assert.assertEquals("A.3", v.toString());
		Assert.assertEquals(".", v.getSeparator());
		Assert.assertEquals(2, v.getL().size());
	}

	@Test
	public void testIncrement() throws Exception {
		Version v = new Version("10");
		Assert.assertEquals("11", v.increment().toString());

		v = new Version("1.2.3");
		Assert.assertEquals("1.2.4", v.increment().toString());
		Assert.assertEquals("2.2.3", v.increment(0).toString());
		Assert.assertEquals(0, v.compareTo(v));
		Assert.assertEquals(-1, v.compareTo(v.increment()));
		Assert.assertEquals(1, v.increment().compareTo(v));
	}

	@Test
	public void testCompareTo() throws Exception {
		Version v = new Version("1.2.3");
		Assert.assertEquals(0, v.compareTo(v));
		Assert.assertEquals(-1, v.compareTo(v.increment()));
		Assert.assertEquals(1, v.increment().compareTo(v));
	}

}
