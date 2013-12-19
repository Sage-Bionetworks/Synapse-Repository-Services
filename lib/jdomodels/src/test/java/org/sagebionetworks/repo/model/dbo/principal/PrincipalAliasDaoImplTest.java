package org.sagebionetworks.repo.model.dbo.principal;

import static org.junit.Assert.*;

import java.util.Date;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.principal.AliasType;
import org.sagebionetworks.repo.model.principal.PrincipalAlias;
import org.sagebionetworks.repo.model.principal.PrincipalAliasDAO;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class PrincipalAliasDaoImplTest {

	@Autowired
	private PrincipalAliasDAO principalAliasDao;
	
	@Autowired
	private UserGroupDAO userGroupDao;
	UserGroup principal;
	
	@Before
	public void before(){
		// Create a test user
		principal = new UserGroup();
		principal.setCreationDate(new Date());
		principal.setIsIndividual(true);
		principal.setName(UUID.randomUUID().toString());
		principal.setId(userGroupDao.create(principal));
	}
	
	@After
	public void after(){
		if(principal != null){
			try {
				userGroupDao.delete(principal.getId());
			} catch (Exception e) {} 
		}
	}
	
	@Test
	public void testCRUD() throws NotFoundException{
		// Test binding an alias to a principal
		PrincipalAlias alias = new PrincipalAlias();
		// Use to upper as the alias
		alias.setAlias(principal.getName().toUpperCase());
		alias.setType(AliasType.USER_NAME);
		alias.setIsValidated(true);
		alias.setPrincipalId(Long.parseLong(principal.getId()));
		
		// Before we start the alias should be available
		assertTrue("Alias should be available before bound", principalAliasDao.isAliasAvailable(alias.getAlias()));
		// Save the alias and fetch is back.
		PrincipalAlias result = principalAliasDao.bindAliasToPrincipal(alias);
		assertNotNull(result);
		assertNotNull(result.getAliasId());
		assertNotNull(result.getEtag());
		Long firstId = result.getAliasId();
		String firstEtag = result.getEtag();
		// Before we start the alias should be available
		assertFalse("Alias should be not available once bound", principalAliasDao.isAliasAvailable(alias.getAlias()));
		// Should be idempotent
		result = principalAliasDao.bindAliasToPrincipal(alias);
		// The ID should not have changed
		assertEquals("Binding the same alias to the same principal twice should not change the original alias id.",firstId, result.getAliasId());
		assertFalse("Binding the same alias to the same principal twice should have changed the etag.", firstEtag.equals(result.getEtag()));
	}
	
	
}
