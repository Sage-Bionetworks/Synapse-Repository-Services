package org.sagebionetworks.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.entity.StringEntity;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.message.FireMessagesResult;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;


public class SynapseAdministrationTest {
	HttpClientProvider mockProvider = null;
	DataUploader mockUploader = null;
	HttpResponse mockResponse;
	
	SynapseAdminClientImpl synapse;
	
	@Before
	public void before() throws Exception{
		// The mock provider
		mockProvider = Mockito.mock(HttpClientProvider.class);
		mockUploader = Mockito.mock(DataUploaderMultipartImpl.class);
		mockResponse = Mockito.mock(HttpResponse.class);
		when(mockProvider.performRequest(any(String.class),any(String.class),any(String.class),(Map<String,String>)anyObject())).thenReturn(mockResponse);
		synapse = new SynapseAdminClientImpl(mockProvider, mockUploader);
	}
	
	@Test
	public void testGetCurrentChangeNumber() throws Exception {
		FireMessagesResult expectedRes = new FireMessagesResult();
		expectedRes.setNextChangeNumber(-1L);
		String expectedJSONResult = EntityFactory.createJSONStringForEntity(expectedRes);
		StringEntity responseEntity = new StringEntity(expectedJSONResult);
		when(mockResponse.getEntity()).thenReturn(responseEntity);
		StatusLine statusLine = Mockito.mock(StatusLine.class);
		when(statusLine.getStatusCode()).thenReturn(200);
		when(mockResponse.getStatusLine()).thenReturn(statusLine);
		FireMessagesResult res = synapse.getCurrentChangeNumber();
		assertNotNull(res);
		assertEquals(expectedRes, res);
	}
	
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
