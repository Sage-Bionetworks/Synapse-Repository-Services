package org.sagebionetworks.client;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

public class ClientVersionInfoTest {

	@Test
	public void testGetVersion(){
		String version = ClientVersionInfo.getClientVersionInfo();
		System.out.println(version);
		assertNotNull(version);
	}
}
