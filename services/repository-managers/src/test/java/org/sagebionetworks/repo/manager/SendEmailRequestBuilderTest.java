package org.sagebionetworks.repo.manager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Collections;

import org.junit.Test;

import com.amazonaws.services.simpleemail.model.SendEmailRequest;

public class SendEmailRequestBuilderTest {

	@Test
	public void testCreateEmailRequest() {
		SendEmailRequest request = (new SendEmailRequestBuilder())
				.withRecipientEmail("foo@bar.com")
				.withSubject("foo")
				.withBody("bar")
				.withIsHtml(false)
				.withSenderUserName("foobar")
				.withSenderDisplayName("foobar")
				.build();
		assertEquals(Collections.singletonList("foo@bar.com"), request.getDestination().getToAddresses());
		assertEquals("foo", request.getMessage().getSubject().getData());
		assertEquals("bar", request.getMessage().getBody().getText().getData());
		assertNull(request.getMessage().getBody().getHtml());
		assertEquals("foobar <foobar@synapse.org>", request.getSource());

		request = (new SendEmailRequestBuilder())
				.withRecipientEmail("foo@bar.com")
				.withSubject("foo")
				.withBody("<div>bar</div>")
				.withIsHtml(true)
				.withSenderUserName("foobar")
				.withSenderDisplayName("foobar")
				.build();
		assertEquals(Collections.singletonList("foo@bar.com"), request.getDestination().getToAddresses());
		assertEquals("foo", request.getMessage().getSubject().getData());
		assertEquals("<div>bar</div>", request.getMessage().getBody().getHtml().getData());
		assertNull(request.getMessage().getBody().getText());
		assertEquals("foobar <foobar@synapse.org>", request.getSource());
	}
	
}
