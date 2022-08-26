package org.sagebionetworks.repo.manager.message.dataaccess;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.markdown.MarkdownClientException;
import org.sagebionetworks.markdown.MarkdownDao;
import org.sagebionetworks.repo.model.RestrictableObjectType;
import org.sagebionetworks.repo.model.dao.subscription.Subscriber;
import org.sagebionetworks.repo.model.subscription.SubscriptionObjectType;
import org.sagebionetworks.repo.model.subscription.Topic;

import com.amazonaws.services.simpleemail.model.SendRawEmailRequest;

@ExtendWith(MockitoExtension.class)
public class SubmissionStatusBroadcastMessageBuilderTest {
	@Mock
	MarkdownDao mockMarkdownDao;

	private Subscriber subscriber;
	private String objectId;
	private String requirementId;
	private String rejectedReason;
	private String resourceId;
	
	SubmissionStatusBroadcastMessageBuilder builder;

	@BeforeEach
	public void before(){
		requirementId = "1";

		subscriber = new Subscriber();
		subscriber.setFirstName("subscriberFirstName");
		subscriber.setLastName("subscriberLastName");
		subscriber.setNotificationEmail("subsciber@domain.org");
		subscriber.setSubscriberId("123");
		subscriber.setUsername("subscriberUsername");
		subscriber.setSubscriptionId("999");

		resourceId = "2";
	
		builder = new SubmissionStatusBroadcastMessageBuilder(
				objectId, null, requirementId, resourceId,
				RestrictableObjectType.ENTITY, mockMarkdownDao, false);
	}

	@Test
	public void testBuildRawBodyForApprovedMessage(){
		String body = builder.buildRawBody(subscriber);
		assertNotNull(body);
		assertTrue(body.contains("subscriberFirstName subscriberLastName (subscriberUsername)"));
		assertTrue(body.contains("Your request for access was reviewed and approved."));
		assertTrue(body.contains("https://www.synapse.org/#!Synapse:2"));
		assertTrue(body.contains("https://www.synapse.org/#!AccessRequirements:ID=2&TYPE=ENTITY"));
	}

	@Test
	public void testBuildRawBodyForTeam(){
		builder = new SubmissionStatusBroadcastMessageBuilder(
				objectId, null, requirementId, resourceId,
				RestrictableObjectType.TEAM, mockMarkdownDao, false);
		String body = builder.buildRawBody(subscriber);
		assertNotNull(body);
		assertTrue(body.contains("subscriberFirstName subscriberLastName (subscriberUsername)"));
		assertTrue(body.contains("Your request for access was reviewed and approved."));
		assertTrue(body.contains("https://www.synapse.org/#!Team:2"));
		assertTrue(body.contains("https://www.synapse.org/#!AccessRequirements:ID=2&TYPE=TEAM"));
	}

	@Test
	public void testBuildRawBodyForEvaluation(){
		
		assertThrows(IllegalArgumentException.class, () -> {			
			builder = new SubmissionStatusBroadcastMessageBuilder(
					objectId, null, requirementId, resourceId,
					RestrictableObjectType.EVALUATION, mockMarkdownDao, false);
		});
	}

	@Test
	public void testBuildRawBodyForRejectedMessage(){
		rejectedReason = "some reason";
		builder = new SubmissionStatusBroadcastMessageBuilder(
				objectId, rejectedReason, requirementId, resourceId,
				RestrictableObjectType.ENTITY, mockMarkdownDao, true);
		String body = builder.buildRawBody(subscriber);
		assertNotNull(body);
		assertTrue(body.contains("subscriberFirstName subscriberLastName (subscriberUsername)"));
		assertTrue(body.contains("A reviewer left a comment in your request. Your request cannot be approved until you respond:"));
		assertTrue(body.contains("\n>" + rejectedReason + "\n"));
		assertTrue(body.contains("https://www.synapse.org/#!Synapse:2"));
		assertTrue(body.contains("https://www.synapse.org/#!AccessRequirements:ID=2&TYPE=ENTITY"));
	}
	
	@Test
	public void testBuildRawBodyForRejectedMessageMultiLine(){
		rejectedReason = "some reason\nsome other reason";
		builder = new SubmissionStatusBroadcastMessageBuilder(
				objectId, rejectedReason, requirementId, resourceId,
				RestrictableObjectType.ENTITY, mockMarkdownDao, true);
		String body = builder.buildRawBody(subscriber);
		assertNotNull(body);
		assertTrue(body.contains("subscriberFirstName subscriberLastName (subscriberUsername)"));
		assertTrue(body.contains("A reviewer left a comment in your request. Your request cannot be approved until you respond:"));
		assertTrue(body.contains("\n>some reason\n>some other reason\n"));
		assertTrue(body.contains("https://www.synapse.org/#!Synapse:2"));
		assertTrue(body.contains("https://www.synapse.org/#!AccessRequirements:ID=2&TYPE=ENTITY"));
	}
	
	@Test
	public void testGetIndentedRejectReason(){
		rejectedReason = "Some reason.";
		builder = new SubmissionStatusBroadcastMessageBuilder(
				objectId, rejectedReason, requirementId, resourceId,
				RestrictableObjectType.ENTITY, mockMarkdownDao, true);
		String result = builder.getIndentedRejectReason();
		assertEquals(">Some reason.", result);
	}
	
	@Test
	public void testGetIndentedRejectReasonMultiLine(){
		rejectedReason = "Some reason.\nSome other reason.\n   Another reason.";
		builder = new SubmissionStatusBroadcastMessageBuilder(
				objectId, rejectedReason, requirementId, resourceId,
				RestrictableObjectType.ENTITY, mockMarkdownDao, true);
		String result = builder.getIndentedRejectReason();
		assertEquals(">Some reason.\n>Some other reason.\n>   Another reason.", result);
	}
	
	@Test
	public void testGetIndentedRejectReasonEmptyLine(){
		rejectedReason = "";
		builder = new SubmissionStatusBroadcastMessageBuilder(
				objectId, rejectedReason, requirementId, resourceId,
				RestrictableObjectType.ENTITY, mockMarkdownDao, true);
		String result = builder.getIndentedRejectReason();
		assertEquals(">", result);
	}
	
	@Test
	public void testGetIndentedRejectReasonNullLine(){
		rejectedReason = null;
		builder = new SubmissionStatusBroadcastMessageBuilder(
				objectId, rejectedReason, requirementId, resourceId,
				RestrictableObjectType.ENTITY, mockMarkdownDao, true);
		String result = builder.getIndentedRejectReason();
		assertEquals(">", result);
	}
	
	@Test
	public void testBuildEmailFailure() throws Exception{
		MarkdownClientException ex = new MarkdownClientException(500, "");
		when(mockMarkdownDao.convertMarkdown(anyString(), isNull())).thenThrow(ex);
		
		MarkdownClientException result = assertThrows(MarkdownClientException.class, () -> {			
			builder.buildEmailForSubscriber(subscriber);
		});
		
		assertEquals(ex, result);
	}
	
	@Test
	public void testBuildEmailSuccess() throws Exception{
		when(mockMarkdownDao.convertMarkdown(anyString(), isNull())).thenReturn("content");
		SendRawEmailRequest request = builder.buildEmailForSubscriber(subscriber);
		assertNotNull(request);
	}

	@Test
	public void testGetTopic() {
		Topic topic = builder.getBroadcastTopic();
		assertNotNull(topic);
		assertEquals(SubscriptionObjectType.DATA_ACCESS_SUBMISSION_STATUS, topic.getObjectType());
		assertEquals(objectId, topic.getObjectId());
	}

	@Test
	public void testGetRelatedUsersWithoutMentionedUsers() {
		assertNull(builder.getRelatedUsers());
	}
}
