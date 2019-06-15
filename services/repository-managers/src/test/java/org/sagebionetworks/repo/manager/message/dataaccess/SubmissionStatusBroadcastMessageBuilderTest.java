package org.sagebionetworks.repo.manager.message.dataaccess;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.sagebionetworks.markdown.MarkdownClientException;
import org.sagebionetworks.markdown.MarkdownDao;
import org.sagebionetworks.repo.model.RestrictableObjectType;
import org.sagebionetworks.repo.model.dao.subscription.Subscriber;
import org.sagebionetworks.repo.model.subscription.SubscriptionObjectType;
import org.sagebionetworks.repo.model.subscription.Topic;

import com.amazonaws.services.simpleemail.model.SendRawEmailRequest;

@RunWith(MockitoJUnitRunner.class)
public class SubmissionStatusBroadcastMessageBuilderTest {
	@Mock
	MarkdownDao mockMarkdownDao;

	private Subscriber subscriber;
	private String objectId;
	private String requirementId;
	private String rejectedReason;
	private String resourceId;
	
	SubmissionStatusBroadcastMessageBuilder builder;

	@Before
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
		assertTrue(body.contains("A member of the Synapse Access and Compliance Team has reviewed and approved your request."));
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
		assertTrue(body.contains("A member of the Synapse Access and Compliance Team has reviewed and approved your request."));
		assertTrue(body.contains("https://www.synapse.org/#!Team:2"));
		assertTrue(body.contains("https://www.synapse.org/#!AccessRequirements:ID=2&TYPE=TEAM"));
	}

	@Test (expected = IllegalArgumentException.class)
	public void testBuildRawBodyForEvaluation(){
		builder = new SubmissionStatusBroadcastMessageBuilder(
				objectId, null, requirementId, resourceId,
				RestrictableObjectType.EVALUATION, mockMarkdownDao, false);
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
		assertTrue(body.contains("A member of the Synapse Access and Compliance Team has reviewed your request and left a comment:"));
		assertTrue(body.contains(rejectedReason));
		assertTrue(body.contains("https://www.synapse.org/#!Synapse:2"));
		assertTrue(body.contains("https://www.synapse.org/#!AccessRequirements:ID=2&TYPE=ENTITY"));
	}
	
	@Test (expected = MarkdownClientException.class)
	public void testBuildEmailFailure() throws Exception{
		when(mockMarkdownDao.convertMarkdown(anyString(), isNull())).thenThrow(new MarkdownClientException(500, ""));
		builder.buildEmailForSubscriber(subscriber);
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
