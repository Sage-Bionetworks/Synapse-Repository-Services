package org.sagebionetworks.repo.web.controller;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.repo.manager.principal.PrincipalManager;
import org.sagebionetworks.repo.model.principal.AliasCheckRequest;
import org.sagebionetworks.repo.model.principal.AliasCheckResponse;
import org.sagebionetworks.repo.model.principal.AliasType;
import org.sagebionetworks.repo.web.service.PrincipalServiceImpl;
import org.springframework.test.util.ReflectionTestUtils;

public class PrincipalServiceUnitTest {

	PrincipalManager mockManager;
	PrincipalServiceImpl service;
	
	@Before
	public void before(){
		mockManager = Mockito.mock(PrincipalManager.class);
		service = new PrincipalServiceImpl();
		ReflectionTestUtils.setField(service, "principalManager", mockManager);
	}
	
	@Test
	public void testcheckAliasNotValid(){
		AliasCheckRequest request = new AliasCheckRequest();
		request.setAlias("one");
		request.setType(AliasType.USER_EMAIL);
		
		when(mockManager.isAliasValid(request.getAlias(), request.getType())).thenReturn(false);
		AliasCheckResponse response = service.checkAlias(request);
		assertNotNull(response);
		assertFalse(response.getAvailable());
		assertFalse(response.getValid());
		// This should not even get called when it is not valid
		verify(mockManager, times(0)).isAliasAvailable(request.getAlias());
	}
	
	@Test
	public void testcheckAliasValidNotAvaiable(){
		AliasCheckRequest request = new AliasCheckRequest();
		request.setAlias("one@test.com");
		request.setType(AliasType.USER_EMAIL);
		
		when(mockManager.isAliasValid(request.getAlias(), request.getType())).thenReturn(true);
		when(mockManager.isAliasAvailable(request.getAlias())).thenReturn(false);
		AliasCheckResponse response = service.checkAlias(request);
		assertNotNull(response);
		assertFalse(response.getAvailable());
		assertTrue(response.getValid());
	}
	
	@Test
	public void testcheckAliasValid(){
		AliasCheckRequest request = new AliasCheckRequest();
		request.setAlias("one@test.com");
		request.setType(AliasType.USER_EMAIL);
		
		when(mockManager.isAliasValid(request.getAlias(), request.getType())).thenReturn(true);
		when(mockManager.isAliasAvailable(request.getAlias())).thenReturn(true);
		AliasCheckResponse response = service.checkAlias(request);
		assertNotNull(response);
		assertTrue(response.getAvailable());
		assertTrue(response.getValid());
	}
}
