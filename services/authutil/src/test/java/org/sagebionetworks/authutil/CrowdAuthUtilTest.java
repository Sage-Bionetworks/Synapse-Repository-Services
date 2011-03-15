package org.sagebionetworks.authutil;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.junit.Assert.assertEquals;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:authutil-test-context.xml" })
public class CrowdAuthUtilTest {
	@Autowired
	CrowdAuthUtil crowdAuthUtil = null;


	@Test
	public void testGetFromXML() throws Exception {
		String s = CrowdAuthUtil.getFromXML("/root/name/@attr", new String("<root><name attr='value'/></root>").getBytes());
		assertEquals("value", s);
	}

}
