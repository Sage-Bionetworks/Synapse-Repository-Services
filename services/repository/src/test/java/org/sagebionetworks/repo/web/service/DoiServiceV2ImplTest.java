package org.sagebionetworks.repo.web.service;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sagebionetworks.repo.manager.doi.DoiManager;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(MockitoJUnitRunner.class)
public class DoiServiceV2ImplTest {

	DoiServiceV2 service = new DoiServiceV2Impl();

	@Mock
	DoiManager mockManager;

	private Long userId = 89734L;
	private String objectId = KeyFactory.keyToString(2L);
	private ObjectType objectType = ObjectType.ENTITY;
	private Long versionNumber = 3L;

	@Before
	public void before(){
		ReflectionTestUtils.setField(service, "doiManager", mockManager);
	}

	@Test
	public void testGetDoi() throws Exception {
		service.getDoi(userId, objectId, objectType, versionNumber);
		verify(mockManager).getDoi(userId, objectId, objectType, versionNumber);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testGetDoiNullObjectId() throws Exception {
		service.getDoi(userId, null, objectType, versionNumber);
		verify(mockManager, never()).getDoi(userId, null, objectType, versionNumber);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testGetDoiNullType() throws Exception {
		service.getDoi(userId, objectId, null, versionNumber);
		verify(mockManager, never()).getDoi(userId, objectId, null, versionNumber);
	}

	@Test
	public void testGetDoiNullVersion() throws Exception {
		service.getDoi(userId, objectId, objectType, null);
		verify(mockManager).getDoi(userId, objectId, objectType, null);
	}

	@Test
	public void testGetDoiAssociation() throws Exception {
		service.getDoiAssociation(userId, objectId, objectType, versionNumber);
		verify(mockManager).getDoiAssociation(userId, objectId, objectType, versionNumber);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testGetDoiAssociationNullObjectId() throws Exception {
		service.getDoiAssociation(userId, null, objectType, versionNumber);
		verify(mockManager, never()).getDoiAssociation(userId, null, objectType, versionNumber);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testGetDoiAssociationNullType() throws Exception {
		service.getDoiAssociation(userId, objectId, null, versionNumber);
		verify(mockManager, never()).getDoiAssociation(userId, objectId, null, versionNumber);
	}

	@Test
	public void testGetDoiAssociationNullVersion() throws Exception {
		service.getDoiAssociation(userId, objectId, objectType, null);
		verify(mockManager).getDoiAssociation(userId, objectId, objectType, null);
	}

	@Test
	public void testLocate() {
		service.locate(userId, objectId, objectType, versionNumber);
		verify(mockManager).getLocation(objectId, objectType, versionNumber);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testLocateNullObjectId() {
		service.locate(userId, null, objectType, versionNumber);
		verify(mockManager, never()).getLocation(null, objectType, versionNumber);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testLocateNullType() {
		service.locate(userId, objectId, null, versionNumber);
		verify(mockManager, never()).getLocation(objectId, null, versionNumber);
	}

	@Test
	public void testLocateNullVersion() {
		service.locate(userId, objectId, objectType, null);
		verify(mockManager).getLocation(objectId, objectType, null);
	}
}
