package org.sagebionetworks.repo.service;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.doi.DoiManager;
import org.sagebionetworks.repo.model.doi.v2.DoiObjectType;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
public class DoiServiceV2ImplTest {

	DoiServiceV2 service = new DoiServiceV2Impl();

	@Mock
	DoiManager mockDoiManager;

	private String portalId = "123";
	private String objectId = KeyFactory.keyToString(2L);
	private DoiObjectType objectType = DoiObjectType.ENTITY;
	private Long versionNumber = 3L;

	@BeforeEach
	public void before(){
		ReflectionTestUtils.setField(service, "doiManager", mockDoiManager);
	}

	@Test
	public void testGetDoi() throws Exception {
		service.getDoi(portalId, objectId, objectType, versionNumber);
		verify(mockDoiManager).getDoi(portalId, objectId, objectType, versionNumber);
	}

	@Test
	public void testGetDoiNullVersion() throws Exception {
		service.getDoi(portalId, objectId, objectType, null);
		verify(mockDoiManager).getDoi(portalId, objectId, objectType, null);
	}

	@Test
	public void testGetDoiAssociation() throws Exception {
		service.getDoiAssociation(portalId, objectId, objectType, versionNumber);
		verify(mockDoiManager).getDoiAssociation(portalId, objectId, objectType, versionNumber);
	}

	@Test
	public void testGetDoiAssociationNullVersion() throws Exception {
		service.getDoiAssociation(portalId, objectId, objectType, null);
		verify(mockDoiManager).getDoiAssociation(portalId, objectId, objectType, null);
	}

	@Test
	public void testLocate() {
		service.locate(portalId, objectId, objectType, versionNumber);
		verify(mockDoiManager).getLocation(portalId, objectId, objectType, versionNumber);
	}

	@Test
	public void testLocateNullObjectId() {
		Assertions.assertThrows(IllegalArgumentException.class, ()->{
			service.locate(portalId, null, objectType, versionNumber);
		});
		verify(mockDoiManager, never()).getLocation(portalId, null, objectType, versionNumber);
	}

	@Test
	public void testLocateNullType() {
		Assertions.assertThrows(IllegalArgumentException.class, ()->{
			service.locate(portalId, objectId, null, versionNumber);
		});
		verify(mockDoiManager, never()).getLocation(portalId, objectId, null, versionNumber);
	}

	@Test
	public void testLocateNullVersion() {
		service.locate(portalId, objectId, objectType, null);
		verify(mockDoiManager).getLocation(portalId, objectId, objectType, null);
	}
}
