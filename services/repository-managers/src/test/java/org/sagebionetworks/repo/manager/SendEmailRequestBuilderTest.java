package org.sagebionetworks.repo.manager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

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
				.withSenderDisplayName("Foo Bar")
				.withNotificationUnsubscribeEndpoint("https://www.synapse.org/#unsub:")
				.withUserId("101")
				.build();
		assertEquals(Collections.singletonList("foo@bar.com"), request.getDestination().getToAddresses());
		assertEquals("foo", request.getMessage().getSubject().getData());
		assertTrue(request.getMessage().getBody().getText().getData().startsWith("bar"));
		assertNull(request.getMessage().getBody().getHtml());
		assertEquals("Foo Bar <foobar@synapse.org>", request.getSource());

		request = (new SendEmailRequestBuilder())
				.withRecipientEmail("foo@bar.com")
				.withSubject("foo")
				.withBody("<div>bar</div>")
				.withIsHtml(true)
				.withSenderUserName("foobar")
				.withSenderDisplayName("Foo Bar")
				.withNotificationUnsubscribeEndpoint("https://www.synapse.org/#unsub:")
				.withUserId("101")
				.build();
		assertEquals(Collections.singletonList("foo@bar.com"), request.getDestination().getToAddresses());
		assertEquals("foo", request.getMessage().getSubject().getData());
		assertTrue(request.getMessage().getBody().getHtml().getData().startsWith("<div>bar</div>"));
		assertNull(request.getMessage().getBody().getText());
		assertEquals("Foo Bar <foobar@synapse.org>", request.getSource());
	}
	
}
