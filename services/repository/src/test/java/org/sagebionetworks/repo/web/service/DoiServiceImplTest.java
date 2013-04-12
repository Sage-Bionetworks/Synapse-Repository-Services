package org.sagebionetworks.repo.web.service;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.Test;
import org.sagebionetworks.repo.manager.doi.EntityDoiManager;
import org.sagebionetworks.repo.model.doi.DoiObjectType;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.springframework.test.util.ReflectionTestUtils;

public class DoiServiceImplTest {

	@Test
	public void testCreateEntityDoi() throws Exception {
		DoiServiceImpl srv = new DoiServiceImpl();
		EntityDoiManager mockManager = mock(EntityDoiManager.class);
		ReflectionTestUtils.setField(srv, "entityDoiManager", mockManager);
		final String userId = Long.toString(1L);
		final String objectId = KeyFactory.keyToString(2L);
		final DoiObjectType objectType = DoiObjectType.ENTITY;
		final Long versionNumber = 3L;
		srv.createDoi(userId, objectId, objectType, versionNumber);
		verify(mockManager, times(1)).createDoi(userId, objectId, versionNumber);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testCreateEntityDoiWithException1() throws Exception {
		DoiServiceImpl srv = new DoiServiceImpl();
		EntityDoiManager mockManager = mock(EntityDoiManager.class);
		ReflectionTestUtils.setField(srv, "entityDoiManager", mockManager);
		final String userId = Long.toString(1L);
		final String objectId = KeyFactory.keyToString(2L);
		final Long versionNumber = 3L;
		srv.createDoi(userId, objectId, null, versionNumber);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testCreateEntityDoiWithException2() throws Exception {
		DoiServiceImpl srv = new DoiServiceImpl();
		EntityDoiManager mockManager = mock(EntityDoiManager.class);
		ReflectionTestUtils.setField(srv, "entityDoiManager", mockManager);
		final String userId = Long.toString(1L);
		final String objectId = KeyFactory.keyToString(2L);
		final Long versionNumber = 3L;
		srv.createDoi(userId, objectId, DoiObjectType.EVALUATION, versionNumber);
	}

	@Test
	public void testGetEntityDoi() throws Exception {
		DoiServiceImpl srv = new DoiServiceImpl();
		EntityDoiManager mockManager = mock(EntityDoiManager.class);
		ReflectionTestUtils.setField(srv, "entityDoiManager", mockManager);
		final String userId = Long.toString(1L);
		final String objectId = KeyFactory.keyToString(2L);
		final DoiObjectType objectType = DoiObjectType.ENTITY;
		final Long versionNumber = 3L;
		srv.getDoi(userId, objectId, objectType, versionNumber);
		verify(mockManager, times(1)).getDoi(userId, objectId, versionNumber);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testGetEntityDoiWithException1() throws Exception {
		DoiServiceImpl srv = new DoiServiceImpl();
		EntityDoiManager mockManager = mock(EntityDoiManager.class);
		ReflectionTestUtils.setField(srv, "entityDoiManager", mockManager);
		final String userId = Long.toString(1L);
		final String objectId = KeyFactory.keyToString(2L);
		final Long versionNumber = 3L;
		srv.getDoi(userId, objectId, null, versionNumber);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testGetEntityDoiWithException2() throws Exception {
		DoiServiceImpl srv = new DoiServiceImpl();
		EntityDoiManager mockManager = mock(EntityDoiManager.class);
		ReflectionTestUtils.setField(srv, "entityDoiManager", mockManager);
		final String userId = Long.toString(1L);
		final String objectId = KeyFactory.keyToString(2L);
		final Long versionNumber = 3L;
		srv.getDoi(userId, objectId, DoiObjectType.EVALUATION, versionNumber);
	}
}
