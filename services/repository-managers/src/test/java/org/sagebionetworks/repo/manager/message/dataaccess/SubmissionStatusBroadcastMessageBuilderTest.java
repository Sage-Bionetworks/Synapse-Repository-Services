package org.sagebionetworks.repo.manager.message.dataaccess;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sagebionetworks.markdown.MarkdownClientException;
import org.sagebionetworks.markdown.MarkdownDao;
import org.sagebionetworks.repo.model.RestrictableObjectDescriptor;
import org.sagebionetworks.repo.model.RestrictableObjectType;
import org.sagebionetworks.repo.model.dao.subscription.Subscriber;
import org.sagebionetworks.repo.model.subscription.SubscriptionObjectType;
import org.sagebionetworks.repo.model.subscription.Topic;

import com.amazonaws.services.simpleemail.model.SendRawEmailRequest;

public class SubmissionStatusBroadcastMessageBuilderTest {
	@Mock
	MarkdownDao mockMarkdownDao;

	private Subscriber subscriber;
	private String objectId;
	private String requirementId;
	private String rejectedReason;
	private RestrictableObjectDescriptor rod;
	
	SubmissionStatusBroadcastMessageBuilder builder;

	@Before
	public void before(){
		MockitoAnnotations.initMocks(this);

		requirementId = "1";
	
		subscriber = new Subscriber();
		subscriber.setFirstName("subscriberFirstName");
		subscriber.setLastName("subscriberLastName");
		subscriber.setNotificationEmail("subsciber@domain.org");
		subscriber.setSubscriberId("123");
		subscriber.setUsername("subscriberUsername");
		subscriber.setSubscriptionId("999");

		rod = new RestrictableObjectDescriptor();
		rod.setId("2");
		rod.setType(RestrictableObjectType.ENTITY);
	
		builder = new SubmissionStatusBroadcastMessageBuilder(
				objectId, null, requirementId, rod, mockMarkdownDao, false);
	}

	@Test
	public void testBuildRawBodyForApprovedMessage(){
		String body = builder.buildRawBody(subscriber);
		assertNotNull(body);
		assertTrue(body.contains("subscriberFirstName subscriberLastName (subscriberUsername)"));
		assertTrue(body.contains("A member of the Synapse Access and Compliance Team has reviewed and approved your request."));
		assertTrue(body.contains("View your request"));
		assertTrue(body.contains("https://www.synapse.org/#!AccessRequirements:ID=2&AR_ID=1"));
	}

	@Test
	public void testBuildRawBodyForTeam(){
		rod.setType(RestrictableObjectType.TEAM);
		
		builder = new SubmissionStatusBroadcastMessageBuilder(
				objectId, null, requirementId, rod, mockMarkdownDao, false);
		String body = builder.buildRawBody(subscriber);
		assertNotNull(body);
		assertTrue(body.contains("subscriberFirstName subscriberLastName (subscriberUsername)"));
		assertTrue(body.contains("A member of the Synapse Access and Compliance Team has reviewed and approved your request."));
		assertTrue(body.contains("View your request"));
		assertTrue(body.contains("https://www.synapse.org/#!AccessRequirements:teamID=2&AR_ID=1"));
	}

	@Test (expected = IllegalArgumentException.class)
	public void testBuildRawBodyForEvaluation(){
		rod.setType(RestrictableObjectType.EVALUATION);
		
		builder = new SubmissionStatusBroadcastMessageBuilder(
				objectId, null, requirementId, rod, mockMarkdownDao, false);
	}

	@Test
	public void testBuildRawBodyForRejectedMessage(){
		rejectedReason = "some reason";
		builder = new SubmissionStatusBroadcastMessageBuilder(
				objectId, rejectedReason, requirementId, rod, mockMarkdownDao, true);
		String body = builder.buildRawBody(subscriber);
		assertNotNull(body);
		assertTrue(body.contains("subscriberFirstName subscriberLastName (subscriberUsername)"));
		assertTrue(body.contains("A member of the Synapse Access and Compliance Team has reviewed your request and left a comment:"));
		assertTrue(body.contains(rejectedReason));
		assertTrue(body.contains("your request"));
		assertTrue(body.contains("https://www.synapse.org/#!AccessRequirements:ID=2&AR_ID=1"));
	}
	
	@Test (expected = MarkdownClientException.class)
	public void testBuildEmailFailure() throws Exception{
		when(mockMarkdownDao.convertMarkdown(anyString(), anyString())).thenThrow(new MarkdownClientException(500, ""));
		builder.buildEmailForSubscriber(subscriber);
	}
	
	@Test
	public void testBuildEmailSuccess() throws Exception{
		when(mockMarkdownDao.convertMarkdown(anyString(), anyString())).thenReturn("content");
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
