package org.sagebionetworks.client;

import static org.junit.Assert.*;

import org.junit.Test;
import org.sagebionetworks.client.exceptions.SynapseServerException;

public class SynapseServerExceptionTest {
	
	@Test
	public void testStatusCodeInMessageOne(){
		SynapseServerException e = new SynapseServerException(503);
		e.printStackTrace();
		assertTrue(e.getMessage().contains("503"));
	}
	
	@Test
	public void testStatusCodeInMessageTwoNull(){
		SynapseServerException e = new SynapseServerException(503, (String)null);
		e.printStackTrace();
		assertTrue(e.getMessage().contains("503"));
	}
	
	@Test
	public void testStatusCodeInMessageTwo(){
		SynapseServerException e = new SynapseServerException(503, "the message");
		e.printStackTrace();
		assertTrue(e.getMessage().contains("503"));
		assertTrue(e.getMessage().contains("the message"));
	}

}
