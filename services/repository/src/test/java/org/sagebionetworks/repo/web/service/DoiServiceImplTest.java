package org.sagebionetworks.repo.web.service;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.Test;
import org.sagebionetworks.repo.manager.doi.EntityDoiManager;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.springframework.test.util.ReflectionTestUtils;

public class DoiServiceImplTest {
	
	private Long userId = 89734L;

	@Test
	public void testCreateEntityDoi() throws Exception {
		DoiServiceImpl srv = new DoiServiceImpl();
		EntityDoiManager mockManager = mock(EntityDoiManager.class);
		ReflectionTestUtils.setField(srv, "entityDoiManager", mockManager);
		final String objectId = KeyFactory.keyToString(2L);
		final ObjectType objectType = ObjectType.ENTITY;
		final Long versionNumber = 3L;
		srv.createDoi(userId, objectId, objectType, versionNumber);
		verify(mockManager, times(1)).createDoi(userId, objectId, versionNumber);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testCreateEntityDoiWithException1() throws Exception {
		DoiServiceImpl srv = new DoiServiceImpl();
		EntityDoiManager mockManager = mock(EntityDoiManager.class);
		ReflectionTestUtils.setField(srv, "entityDoiManager", mockManager);
		final String objectId = KeyFactory.keyToString(2L);
		final Long versionNumber = 3L;
		srv.createDoi(userId, objectId, null, versionNumber);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testCreateEntityDoiWithException2() throws Exception {
		DoiServiceImpl srv = new DoiServiceImpl();
		EntityDoiManager mockManager = mock(EntityDoiManager.class);
		ReflectionTestUtils.setField(srv, "entityDoiManager", mockManager);
		final String objectId = KeyFactory.keyToString(2L);
		final Long versionNumber = 3L;
		srv.createDoi(userId, objectId, ObjectType.EVALUATION, versionNumber);
	}

	@Test
	public void testGetEntityDoi() throws Exception {
		DoiServiceImpl srv = new DoiServiceImpl();
		EntityDoiManager mockManager = mock(EntityDoiManager.class);
		ReflectionTestUtils.setField(srv, "entityDoiManager", mockManager);
		final String objectId = KeyFactory.keyToString(2L);
		final ObjectType objectType = ObjectType.ENTITY;
		final Long versionNumber = 3L;
		srv.getDoiForVersion(userId, objectId, objectType, versionNumber);
		verify(mockManager, times(1)).getDoiForVersion(userId, objectId, versionNumber);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testGetEntityDoiWithException1() throws Exception {
		DoiServiceImpl srv = new DoiServiceImpl();
		EntityDoiManager mockManager = mock(EntityDoiManager.class);
		ReflectionTestUtils.setField(srv, "entityDoiManager", mockManager);
		final String objectId = KeyFactory.keyToString(2L);
		final Long versionNumber = 3L;
		srv.getDoiForVersion(userId, objectId, null, versionNumber);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testGetEntityDoiWithException2() throws Exception {
		DoiServiceImpl srv = new DoiServiceImpl();
		EntityDoiManager mockManager = mock(EntityDoiManager.class);
		ReflectionTestUtils.setField(srv, "entityDoiManager", mockManager);
		final String objectId = KeyFactory.keyToString(2L);
		final Long versionNumber = 3L;
		srv.getDoiForVersion(userId, objectId, ObjectType.EVALUATION, versionNumber);
	}
}
