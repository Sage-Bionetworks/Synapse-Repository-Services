package org.sagebionetworks.evaluation.dao.principal;

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
	public void testCreateAlais(){
		// Test binding an alias to a principal
		PrincipalAlias alias = new PrincipalAlias();
		// Use to upper as the alias
		alias.setAlias(principal.getName().toUpperCase());
		alias.setType(AliasType.USER_NAME);
		alias.setIsValidated(true);
		// Save the alias and fetch is back.
		PrincipalAlias result = principalAliasDao.bindAliasToPrincipal(alias);
		assertNotNull(result);
		assertNotNull(result.getAliasId());
		assertNotNull(result.getEtag());

	}
	
	
}
