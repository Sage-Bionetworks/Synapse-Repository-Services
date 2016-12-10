package org.sagebionetworks.client;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.sagebionetworks.repo.model.ObjectType;


public class SynapseAdministrationTest {
	
	@Test (expected=IllegalArgumentException.class)
	public void testBuildListMessagesURLNullStartNumber(){
		SynapseAdminClientImpl.buildListMessagesURL(null, ObjectType.EVALUATION, new Long(1));
	}
	@Test
	public void testBuildListMessagesURL(){
		String expected = "/admin/messages?startChangeNumber=345&type=EVALUATION&limit=987";
		String url = SynapseAdminClientImpl.buildListMessagesURL(new Long(345), ObjectType.EVALUATION, new Long(987));
		assertEquals(expected, url);
	}
	
	@Test
	public void testBuildListMessagesURLNullType(){
		String expected = "/admin/messages?startChangeNumber=345&limit=987";
		String url = SynapseAdminClientImpl.buildListMessagesURL(new Long(345),null, new Long(987));
		assertEquals(expected, url);
	}
	
	@Test
	public void testBuildListMessagesURLNullLimit(){
		String expected = "/admin/messages?startChangeNumber=345&type=EVALUATION";
		String url = SynapseAdminClientImpl.buildListMessagesURL(new Long(345), ObjectType.EVALUATION, null);
		assertEquals(expected, url);
	}
	
	@Test
	public void testBuildListMessagesURLAllNonRequiredNull(){
		String expected = "/admin/messages?startChangeNumber=345";
		String url = SynapseAdminClientImpl.buildListMessagesURL(new Long(345), null, null);
		assertEquals(expected, url);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testBuildPublishMessagesURLQueueNameNull(){
		SynapseAdminClientImpl.buildPublishMessagesURL(null, new Long(345), ObjectType.ACTIVITY, new Long(888));
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testBuildPublishMessagesURLStartNumberNull(){
		SynapseAdminClientImpl.buildPublishMessagesURL("some-queue", null, ObjectType.ACTIVITY, new Long(888));
	}
	
	@Test
	public void testBuildPublishMessagesURL(){
		String expected = "/admin/messages/rebroadcast?queueName=some-queue&startChangeNumber=345&type=ACTIVITY&limit=888";
		String url = SynapseAdminClientImpl.buildPublishMessagesURL("some-queue", new Long(345), ObjectType.ACTIVITY, new Long(888));
		assertEquals(expected, url);
	}
	
	@Test
	public void testBuildPublishMessagesURLTypeNull(){
		String expected = "/admin/messages/rebroadcast?queueName=some-queue&startChangeNumber=345&limit=888";
		String url = SynapseAdminClientImpl.buildPublishMessagesURL("some-queue", new Long(345), null, new Long(888));
		assertEquals(expected, url);
	}
	
	@Test
	public void testBuildPublishMessagesURLLimitNull(){
		String expected = "/admin/messages/rebroadcast?queueName=some-queue&startChangeNumber=345&type=ACTIVITY";
		String url = SynapseAdminClientImpl.buildPublishMessagesURL("some-queue", new Long(345), ObjectType.ACTIVITY, null);
		assertEquals(expected, url);
	}
}
