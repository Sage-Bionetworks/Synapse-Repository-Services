package org.sagebionetworks.client;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

public class ClientVersionInfoTest {

	@Test
	public void testGetVersion(){
		String version = ClientVersionInfo.getClientVersionInfo();
		System.out.println(version);
		assertNotNull(version);
	}
}
