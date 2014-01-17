package org.sagebionetworks.repo.manager.principal;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.repo.model.principal.AliasType;
import org.sagebionetworks.repo.model.principal.PrincipalAliasDAO;
import org.springframework.test.util.ReflectionTestUtils;

public class PrincipalManagerImplUnitTest {

	PrincipalAliasDAO mockPrincipalAliasDAO;
	
	PrincipalManagerImpl manager;
	
	@Before
	public void before(){
		mockPrincipalAliasDAO = Mockito.mock(PrincipalAliasDAO.class);
		manager = new PrincipalManagerImpl();
		ReflectionTestUtils.setField(manager, "principalAliasDAO", mockPrincipalAliasDAO);
	}
	@Test
	public void testValid(){
		// Valid
		assertTrue(manager.isAliasValid("one@gmail.org", AliasType.USER_EMAIL));
		assertTrue(manager.isAliasValid("one", AliasType.USER_NAME));
		assertTrue(manager.isAliasValid("Team Name", AliasType.TEAM_NAME));
		assertTrue(manager.isAliasValid("https://gmail.com/myId", AliasType.USER_OPEN_ID));
		// Invalid
		assertFalse(manager.isAliasValid("bad", AliasType.USER_EMAIL));
		assertFalse(manager.isAliasValid("Has Space", AliasType.USER_NAME));
		assertFalse(manager.isAliasValid("@#$%", AliasType.TEAM_NAME));
		assertFalse(manager.isAliasValid("notAURL", AliasType.USER_OPEN_ID));
	}
	
	@Test
	public void testNotAvailable(){
		String toTest = "007";
		when(mockPrincipalAliasDAO.isAliasAvailable(toTest)).thenReturn(false);
		assertFalse(manager.isAliasAvailable(toTest));
	}
	
	@Test
	public void testAvailable(){
		String toTest = "007";
		when(mockPrincipalAliasDAO.isAliasAvailable(toTest)).thenReturn(true);
		assertTrue(manager.isAliasAvailable(toTest));
	}
}
