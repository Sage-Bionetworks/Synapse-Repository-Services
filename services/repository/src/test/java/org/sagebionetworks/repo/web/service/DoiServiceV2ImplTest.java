package org.sagebionetworks.repo.web.service;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.doi.DoiManager;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
public class DoiServiceV2ImplTest {

	DoiServiceV2 service = new DoiServiceV2Impl();

	@Mock
	DoiManager mockDoiManager;

	private String objectId = KeyFactory.keyToString(2L);
	private ObjectType objectType = ObjectType.ENTITY;
	private Long versionNumber = 3L;

	@BeforeEach
	public void before(){
		ReflectionTestUtils.setField(service, "doiManager", mockDoiManager);
	}

	@Test
	public void testGetDoi() throws Exception {
		service.getDoi(objectId, objectType, versionNumber);
		verify(mockDoiManager).getDoi(objectId, objectType, versionNumber);
	}

	@Test
	public void testGetDoiNullVersion() throws Exception {
		service.getDoi(objectId, objectType, null);
		verify(mockDoiManager).getDoi(objectId, objectType, null);
	}

	@Test
	public void testGetDoiAssociation() throws Exception {
		service.getDoiAssociation(objectId, objectType, versionNumber);
		verify(mockDoiManager).getDoiAssociation(objectId, objectType, versionNumber);
	}

	@Test
	public void testGetDoiAssociationNullVersion() throws Exception {
		service.getDoiAssociation(objectId, objectType, null);
		verify(mockDoiManager).getDoiAssociation(objectId, objectType, null);
	}

	@Test
	public void testLocate() {
		service.locate(objectId, objectType, versionNumber);
		verify(mockDoiManager).getLocation(objectId, objectType, versionNumber);
	}

	@Test
	public void testLocateNullObjectId() {
		Assertions.assertThrows(IllegalArgumentException.class, ()->{
			service.locate(null, objectType, versionNumber);
		});
		verify(mockDoiManager, never()).getLocation(null, objectType, versionNumber);
	}

	@Test
	public void testLocateNullType() {
		Assertions.assertThrows(IllegalArgumentException.class, ()->{
			service.locate(objectId, null, versionNumber);
		});
		verify(mockDoiManager, never()).getLocation(objectId, null, versionNumber);
	}

	@Test
	public void testLocateNullVersion() {
		service.locate(objectId, objectType, null);
		verify(mockDoiManager).getLocation(objectId, objectType, null);
	}
}
