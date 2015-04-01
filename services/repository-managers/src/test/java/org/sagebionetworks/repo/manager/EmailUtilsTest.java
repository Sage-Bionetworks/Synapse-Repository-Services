package org.sagebionetworks.repo.manager;

import static org.junit.Assert.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.amazonaws.services.simpleemail.model.SendEmailRequest;

public class EmailUtilsTest {

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testCreateEmailRequest() {
		SendEmailRequest request = EmailUtils.
				createEmailRequest("foo@bar.com", "foo", "bar", false, "foobar");
		assertEquals(Collections.singletonList("foo@bar.com"), request.getDestination().getToAddresses());
		assertEquals("foo", request.getMessage().getSubject().getData());
		assertEquals("bar", request.getMessage().getBody().getText().getData());
		assertNull(request.getMessage().getBody().getHtml());
		assertEquals("foobar <notifications@sagebase.org>", request.getSource());

		request = EmailUtils.
				createEmailRequest("foo@bar.com", "foo", "<html>bar</html>", true, "foobar");
		assertEquals(Collections.singletonList("foo@bar.com"), request.getDestination().getToAddresses());
		assertEquals("foo", request.getMessage().getSubject().getData());
		assertEquals("<html>bar</html>", request.getMessage().getBody().getHtml().getData());
		assertNull(request.getMessage().getBody().getText());
		assertEquals("foobar <notifications@sagebase.org>", request.getSource());
}
	
	@Test
	public void testReadMailTemplate() {
		Map<String,String> fieldValues = new HashMap<String,String>();
		fieldValues.put("#displayname#", "Foo Bar");
		fieldValues.put("#domain#", "Synapse");
		fieldValues.put("#username#", "foobar");
		String message = EmailUtils.readMailTemplate("message/WelcomeTemplate.txt", fieldValues);
		assertTrue(message.indexOf("#")<0); // all fields have been replaced
		assertTrue(message.indexOf("Foo Bar")>=0);
		assertTrue(message.indexOf("Synapse") >= 0);
		assertTrue(message.indexOf("foobar")>=0);
	}

}
