package org.sagebionetworks.repo.web.service;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.sagebionetworks.repo.manager.doi.DoiManager;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(MockitoJUnitRunner.class)
public class DoiServiceV2ImplTest {

	DoiServiceV2 service = new DoiServiceV2Impl();

	@Mock
	DoiManager mockDoiManager;

	private Long userId = 89734L;
	private UserInfo user = new UserInfo(true);
	private String objectId = KeyFactory.keyToString(2L);
	private ObjectType objectType = ObjectType.ENTITY;
	private Long versionNumber = 3L;

	@Before
	public void before(){
		ReflectionTestUtils.setField(service, "doiManager", mockDoiManager);
	}

	@Test
	public void testGetDoi() throws Exception {
		service.getDoi(userId, objectId, objectType, versionNumber);
		verify(mockDoiManager).getDoi(objectId, objectType, versionNumber);
	}

	@Test
	public void testGetDoiNullVersion() throws Exception {
		service.getDoi(userId, objectId, objectType, null);
		verify(mockDoiManager).getDoi(objectId, objectType, null);
	}

	@Test
	public void testGetDoiAssociation() throws Exception {
		service.getDoiAssociation(userId, objectId, objectType, versionNumber);
		verify(mockDoiManager).getDoiAssociation(objectId, objectType, versionNumber);
	}

	@Test
	public void testGetDoiAssociationNullVersion() throws Exception {
		service.getDoiAssociation(userId, objectId, objectType, null);
		verify(mockDoiManager).getDoiAssociation(objectId, objectType, null);
	}

	@Test
	public void testLocate() {
		service.locate(userId, objectId, objectType, versionNumber);
		verify(mockDoiManager).getLocation(objectId, objectType, versionNumber);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testLocateNullObjectId() {
		service.locate(userId, null, objectType, versionNumber);
		verify(mockDoiManager, never()).getLocation(null, objectType, versionNumber);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testLocateNullType() {
		service.locate(userId, objectId, null, versionNumber);
		verify(mockDoiManager, never()).getLocation(objectId, null, versionNumber);
	}

	@Test
	public void testLocateNullVersion() {
		service.locate(userId, objectId, objectType, null);
		verify(mockDoiManager).getLocation(objectId, objectType, null);
	}
}
