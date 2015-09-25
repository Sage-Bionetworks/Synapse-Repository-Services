package org.sagebionetworks.repo.manager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Collections;

import org.junit.Test;
import org.sagebionetworks.StackConfiguration;

import com.amazonaws.services.simpleemail.model.SendEmailRequest;

public class SendEmailRequestBuilderTest {
	
	private static final String UNSUB_ENDPOINT = "https://www.synapse.org/#unsub:";

	@Test
	public void testCreateEmailRequest() {
		SendEmailRequest request = (new SendEmailRequestBuilder())
				.withRecipientEmail("foo@bar.com")
				.withSubject("subject")
				.withBody("bar")
				.withIsHtml(false)
				.withSenderUserName("foobar")
				.withSenderDisplayName("Foo Bar")
				.withNotificationUnsubscribeEndpoint(UNSUB_ENDPOINT)
				.withUserId("101")
				.build();
		assertEquals(Collections.singletonList("foo@bar.com"), request.getDestination().getToAddresses());
		assertEquals("subject", request.getMessage().getSubject().getData());
		String body = request.getMessage().getBody().getText().getData();
		assertTrue(body.startsWith("bar"));
		assertTrue(body.indexOf(UNSUB_ENDPOINT)>0);
		assertNull(request.getMessage().getBody().getHtml());
		assertEquals("Foo Bar <foobar@synapse.org>", request.getSource());

		request = (new SendEmailRequestBuilder())
				.withRecipientEmail("foo@bar.com")
				.withSubject("subject")
				.withBody("<div>bar</div>")
				.withIsHtml(true)
				.withSenderUserName("foobar")
				.withSenderDisplayName("Foo Bar")
				.withNotificationUnsubscribeEndpoint(UNSUB_ENDPOINT)
				.withUserId("101")
				.build();
		assertEquals(Collections.singletonList("foo@bar.com"), request.getDestination().getToAddresses());
		assertEquals("subject", request.getMessage().getSubject().getData());
		body = request.getMessage().getBody().getHtml().getData();
		assertTrue(body.startsWith("<div>bar</div>"));
		assertTrue(body.indexOf(UNSUB_ENDPOINT)>0);
		assertNull(request.getMessage().getBody().getText());
		assertEquals("Foo Bar <foobar@synapse.org>", request.getSource());
	}
	
	@Test
	public void testCreateEmailRequestNoUnsubEndpoint() {
		SendEmailRequest request = (new SendEmailRequestBuilder())
				.withRecipientEmail("foo@bar.com")
				.withSubject("subject")
				.withBody("bar")
				.withIsHtml(false)
				.withSenderUserName("foobar")
				.withSenderDisplayName("Foo Bar")
				.withUserId("101")
				.build();
		assertEquals(Collections.singletonList("foo@bar.com"), request.getDestination().getToAddresses());
		assertEquals("subject", request.getMessage().getSubject().getData());
		String body = request.getMessage().getBody().getText().getData();
		assertTrue(body.startsWith("bar"));
		assertTrue(body.indexOf(StackConfiguration.getDefaultPortalNotificationEndpoint())>0);
		assertNull(request.getMessage().getBody().getHtml());
		assertEquals("Foo Bar <foobar@synapse.org>", request.getSource());

		request = (new SendEmailRequestBuilder())
				.withRecipientEmail("foo@bar.com")
				.withSubject("subject")
				.withBody("<div>bar</div>")
				.withIsHtml(true)
				.withSenderUserName("foobar")
				.withSenderDisplayName("Foo Bar")
				.withUserId("101")
				.build();
		assertEquals(Collections.singletonList("foo@bar.com"), request.getDestination().getToAddresses());
		assertEquals("subject", request.getMessage().getSubject().getData());
		body = request.getMessage().getBody().getHtml().getData();
		assertTrue(body.startsWith("<div>bar</div>"));
		assertTrue(body.indexOf(StackConfiguration.getDefaultPortalNotificationEndpoint())>0);
		assertNull(request.getMessage().getBody().getText());
		assertEquals("Foo Bar <foobar@synapse.org>", request.getSource());
	}

	
}
