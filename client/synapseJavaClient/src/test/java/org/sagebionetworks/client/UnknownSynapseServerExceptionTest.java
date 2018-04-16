package org.sagebionetworks.client;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.sagebionetworks.client.exceptions.UnknownSynapseServerException;

public class UnknownSynapseServerExceptionTest {
	
	@Test
	public void testStatusCode(){
		UnknownSynapseServerException e = new UnknownSynapseServerException(503);
		assertEquals("Status Code: 503", e.getMessage());
		assertEquals(null, e.getCause());
	}
	
	@Test
	public void testStatusCodeInNullMessage(){
		UnknownSynapseServerException e = new UnknownSynapseServerException(503, (String)null);
		assertEquals("Status Code: 503", e.getMessage());
		assertEquals(null, e.getCause());
	}
	
	@Test
	public void testStatusCodeInMessageTwo(){
		UnknownSynapseServerException e = new UnknownSynapseServerException(503, "the message");
		assertEquals("Status Code: 503 message: the message", e.getMessage());
		assertEquals(null, e.getCause());
	}
	
	@Test
	public void testStatusCodeMessageCause(){
		Exception cause = new Exception("the cause");
		UnknownSynapseServerException e = new UnknownSynapseServerException(503, "the message", cause);
		assertEquals("Status Code: 503 message: the message", e.getMessage());
		assertEquals(cause, e.getCause());
	}

	
	@Test
	public void testStatusCodeNullMessageCause(){
		Exception cause = new Exception("the cause");
		UnknownSynapseServerException e = new UnknownSynapseServerException(503, null, cause);
		assertEquals("Status Code: 503", e.getMessage());
		assertEquals(cause, e.getCause());
	}
}
