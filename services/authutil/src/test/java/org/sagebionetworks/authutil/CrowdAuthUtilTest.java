package org.sagebionetworks.authutil;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Before;
import org.junit.Test;

public class CrowdAuthUtilTest {
	
	@Before
	public void setUp() {
		CrowdAuthUtil.acceptAllCertificates();
	}


	@Test
	public void testGetFromXML() throws Exception {
		String s = CrowdAuthUtil.getFromXML("/root/name/@attr", new String("<?xml version='1.0' encoding='UTF-8'?><root><name attr='value'/></root>").getBytes());
		assertEquals("value", s);
	}
	
	@Test
	public void testGetMultiFromXML() throws Exception {
		Collection<String> ss = CrowdAuthUtil.getMultiFromXML("/root/name/@attr", new String("<?xml version='1.0' encoding='UTF-8'?><root><name attr='value'/><name attr='value2'/></root>").getBytes());
		assertEquals(Arrays.asList(new String[]{"value","value2"}), ss);
	}

}
