package org.sagebionetworks.repo.web;

import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.commons.logging.Log;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.web.controller.AbstractAutowiredControllerJunit5TestBase;
import org.sagebionetworks.repo.web.service.EntityService;
import org.sagebionetworks.repo.web.service.EntityServiceImpl;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DeadlockLoserDataAccessException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Autowired version of the DeadlockWatcherTest
 * 
 * @author jmhill
 * 
 */
public class DeadlockWatcherTestAutoWire extends AbstractAutowiredControllerJunit5TestBase {

	@Autowired
	DeadlockWatcher deadlockWatcher;

	@Autowired
	EntityService entityService;

	private EntityManager originalEntityManager;
	private UserInfo userInfo;
	
	@Autowired
	private UserManager userManager;

	@BeforeEach
	public void before() throws Exception {
		originalEntityManager = (EntityManager) ReflectionTestUtils.getField(((EntityServiceImpl) getTargetObject(entityService)),
				"entityManager");
		userInfo = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
	}

	@AfterEach
	public void after() throws Exception {
		ReflectionTestUtils.setField(((EntityServiceImpl) getTargetObject(entityService)), "entityManager", originalEntityManager);
	}

	@Test
	public void testDeadlock() throws Exception {
		Log mockLog = Mockito.mock(Log.class);
		deadlockWatcher.setLog(mockLog);

		EntityManager mockEntityManager = mock(EntityManager.class);
		when(mockEntityManager.getEntityHeader(any(UserInfo.class), anyString())).thenThrow(
				new DeadlockLoserDataAccessException("fake", null));

		ReflectionTestUtils.setField(((EntityServiceImpl) getTargetObject(entityService)), "entityManager", mockEntityManager);

		try {
			entityService.getEntityHeader(userInfo, (String)null);
			fail("Should have thrown a DeadlockLoserDataAccessException");
		} catch (DeadlockLoserDataAccessException e2) {
		}
		// The log should have been hit at least 4 times
		verify(mockLog, atLeast(4)).debug(any(String.class));
	}

	@Test
	public void testTransientException() throws Exception {
		Log mockLog = Mockito.mock(Log.class);
		deadlockWatcher.setLog(mockLog);

		EntityManager mockEntityManager = mock(EntityManager.class);
		when(mockEntityManager.getEntityHeader(any(UserInfo.class), anyString())).thenThrow(
				new TransientDataAccessException("fake", null) {
					private static final long serialVersionUID = 1L;
				});

		ReflectionTestUtils.setField(((EntityServiceImpl) getTargetObject(entityService)), "entityManager", mockEntityManager);

		try {
			entityService.getEntityHeader(userInfo, (String)null);
			fail("Should have thrown a DeadlockLoserDataAccessException");
		} catch (TransientDataAccessException e2) {

		}
		// The log should have been hit at least 4 times
		verify(mockLog, atLeast(4)).debug(any(String.class));
	}

	@SuppressWarnings("unchecked")
	protected <T> T getTargetObject(T proxy) throws Exception {
		if (AopUtils.isJdkDynamicProxy(proxy)) {
			return (T) ((Advised) proxy).getTargetSource().getTarget();
		} else {
			return proxy;
		}
	}
}
