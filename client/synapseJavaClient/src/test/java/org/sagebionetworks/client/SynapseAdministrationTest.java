package org.sagebionetworks.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.entity.StringEntity;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.repo.model.MigratableObjectCount;
import org.sagebionetworks.repo.model.MigratableObjectData;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.Study;
import org.sagebionetworks.repo.model.daemon.BackupRestoreStatus;
import org.sagebionetworks.repo.model.daemon.DaemonStatus;
import org.sagebionetworks.repo.model.daemon.DaemonType;
import org.sagebionetworks.repo.model.daemon.RestoreSubmission;
import org.sagebionetworks.repo.model.message.FireMessagesResult;
import org.sagebionetworks.repo.model.message.ObjectType;
import org.sagebionetworks.repo.model.migration.IdList;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.sagebionetworks.repo.model.migration.MigrationTypeCount;
import org.sagebionetworks.repo.model.migration.MigrationTypeCounts;
import org.sagebionetworks.repo.model.migration.MigrationTypeList;
import org.sagebionetworks.repo.model.migration.RowMetadataResult;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;


public class SynapseAdministrationTest {
	HttpClientProvider mockProvider = null;
	DataUploader mockUploader = null;
	HttpResponse mockResponse;
	
	SynapseAdministration synapse;
	
	@Before
	public void before() throws Exception{
		// The mock provider
		mockProvider = Mockito.mock(HttpClientProvider.class);
		mockUploader = Mockito.mock(DataUploaderMultipartImpl.class);
		mockResponse = Mockito.mock(HttpResponse.class);
		when(mockProvider.performRequest(any(String.class),any(String.class),any(String.class),(Map<String,String>)anyObject())).thenReturn(mockResponse);
		synapse = new SynapseAdministration(mockProvider, mockUploader);
	}
	
	@Test
	public void testGetAllMigratableObjectsPaginated() throws Exception {
		PaginatedResults<MigratableObjectData> p = new PaginatedResults<MigratableObjectData>();
		// This is what we want returned.
		String jsonString = EntityFactory.createJSONStringForEntity(p);

		StringEntity responseEntity = new StringEntity(jsonString);
		// We want the mock response to return JSON for this entity.
		when(mockResponse.getEntity()).thenReturn(responseEntity);
		PaginatedResults<MigratableObjectData> clone = synapse.getAllMigratableObjectsPaginated(0, 100, true);
		// For this test we want return 
		assertNotNull(clone);
		// The clone should equal the original ds
		assertEquals(p, clone);
	}
	
	@Test
	public void testGetMigratableObjectCounts() throws Exception {
		PaginatedResults<MigratableObjectCount> p = new PaginatedResults<MigratableObjectCount>();
		String expectedJSONResult = EntityFactory.createJSONStringForEntity(p);
		StringEntity responseEntity = new StringEntity(expectedJSONResult);
		when(mockResponse.getEntity()).thenReturn(responseEntity);
		PaginatedResults<MigratableObjectCount> oc = synapse.getMigratableObjectCounts();
		assertNotNull(oc);
		assertEquals(p, oc);
	}
	
	@Test
	public void testGetCurrentChangeNumber() throws Exception {
		FireMessagesResult expectedRes = new FireMessagesResult();
		expectedRes.setNextChangeNumber(-1L);
		String expectedJSONResult = EntityFactory.createJSONStringForEntity(expectedRes);
		StringEntity responseEntity = new StringEntity(expectedJSONResult);
		when(mockResponse.getEntity()).thenReturn(responseEntity);
		FireMessagesResult res = synapse.getCurrentChangeNumber();
		assertNotNull(res);
		assertEquals(expectedRes, res);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testBuildListMessagesURLNullStartNumber(){
		SynapseAdministration.buildListMessagesURL(null, ObjectType.EVALUATION, new Long(1));
	}
	@Test
	public void testBuildListMessagesURL(){
		String expected = "/admin/messages?startChangeNumber=345&type=EVALUATION&limit=987";
		String url = SynapseAdministration.buildListMessagesURL(new Long(345), ObjectType.EVALUATION, new Long(987));
		assertEquals(expected, url);
	}
	
	@Test
	public void testBuildListMessagesURLNullType(){
		String expected = "/admin/messages?startChangeNumber=345&limit=987";
		String url = SynapseAdministration.buildListMessagesURL(new Long(345),null, new Long(987));
		assertEquals(expected, url);
	}
	
	@Test
	public void testBuildListMessagesURLNullLimit(){
		String expected = "/admin/messages?startChangeNumber=345&type=EVALUATION";
		String url = SynapseAdministration.buildListMessagesURL(new Long(345), ObjectType.EVALUATION, null);
		assertEquals(expected, url);
	}
	
	@Test
	public void testBuildListMessagesURLAllNonRequiredNull(){
		String expected = "/admin/messages?startChangeNumber=345";
		String url = SynapseAdministration.buildListMessagesURL(new Long(345), null, null);
		assertEquals(expected, url);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testBuildPublishMessagesURLQueueNameNull(){
		SynapseAdministration.buildPublishMessagesURL(null, new Long(345), ObjectType.ACTIVITY, new Long(888));
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testBuildPublishMessagesURLStartNumberNull(){
		SynapseAdministration.buildPublishMessagesURL("some-queue", null, ObjectType.ACTIVITY, new Long(888));
	}
	
	@Test
	public void testBuildPublishMessagesURL(){
		String expected = "/admin/messages/rebroadcast?queueName=some-queue&startChangeNumber=345&type=ACTIVITY&limit=888";
		String url = SynapseAdministration.buildPublishMessagesURL("some-queue", new Long(345), ObjectType.ACTIVITY, new Long(888));
		assertEquals(expected, url);
	}
	
	@Test
	public void testBuildPublishMessagesURLTypeNull(){
		String expected = "/admin/messages/rebroadcast?queueName=some-queue&startChangeNumber=345&limit=888";
		String url = SynapseAdministration.buildPublishMessagesURL("some-queue", new Long(345), null, new Long(888));
		assertEquals(expected, url);
	}
	
	@Test
	public void testBuildPublishMessagesURLLimitNull(){
		String expected = "/admin/messages/rebroadcast?queueName=some-queue&startChangeNumber=345&type=ACTIVITY";
		String url = SynapseAdministration.buildPublishMessagesURL("some-queue", new Long(345), ObjectType.ACTIVITY, null);
		assertEquals(expected, url);
	}
	
}
