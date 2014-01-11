package org.sagebionetworks.repo.model.dbo.principal;

import static org.junit.Assert.*;

import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.NameConflictException;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.principal.AliasType;
import org.sagebionetworks.repo.model.principal.BootstrapGroup;
import org.sagebionetworks.repo.model.principal.BootstrapPrincipal;
import org.sagebionetworks.repo.model.principal.BootstrapUser;
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
	Long principalId;
	Long principalId2;
	List<Long> toDelete;
	
	@Before
	public void before() throws DatastoreException, NotFoundException{
		toDelete = new LinkedList<Long>();
		// Create a test user
		UserGroup ug = new UserGroup();
		ug.setCreationDate(new Date());
		ug.setIsIndividual(true);
		principalId = userGroupDao.create(ug);
		toDelete.add(principalId);
		
		ug = new UserGroup();
		ug.setCreationDate(new Date());
		ug.setIsIndividual(true);
		principalId2 = userGroupDao.create(ug);
		toDelete.add(principalId2);
	}
	
	@After
	public void after(){
		if(toDelete != null){
			for(Long id: toDelete){
				try {
					userGroupDao.delete(id.toString());
				} catch (Exception e) {} 
			}
		}
	}
	
	@Test
	public void testCRUD() throws NotFoundException{
		// Test binding an alias to a principal
		PrincipalAlias alias = new PrincipalAlias();
		// Use to upper as the alias
		alias.setAlias(UUID.randomUUID().toString()+"@test.com");
		alias.setType(AliasType.USER_EMAIL);
		alias.setIsValidated(true);
		alias.setPrincipalId(principalId);
		
		// Before we start the alias should be available
//		assertTrue("Alias should be available before bound", principalAliasDao.isAliasAvailable(alias.getAlias()));
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
	
	@Test
	public void testBindEmail() throws NotFoundException{
		// Test binding an alias to a principal
		PrincipalAlias alias = new PrincipalAlias();
		// Use to upper as the alias
		alias.setAlias("james.bond@Spy.org");
		alias.setType(AliasType.USER_EMAIL);
		alias.setIsValidated(true);
		alias.setPrincipalId(principalId);
		
		// Before we start the alias should be available
		assertTrue("Alias should be available before bound", principalAliasDao.isAliasAvailable(alias.getAlias()));
		// Save the alias and fetch is back.
		PrincipalAlias result = principalAliasDao.bindAliasToPrincipal(alias);
		assertNotNull(result);
		assertNotNull(result.getAliasId());
		assertNotNull(result.getEtag());
		assertEquals(alias.getAlias(), result.getAlias());
	}
	
	@Test
	public void testBindTeam() throws NotFoundException{
		// Test binding an alias to a principal
		PrincipalAlias alias = new PrincipalAlias();
		// Use to upper as the alias
		alias.setAlias("Best team Ever");
		alias.setType(AliasType.TEAM_NAME);
		alias.setIsValidated(true);
		alias.setPrincipalId(principalId);
		
		// Before we start the alias should be available
		assertTrue("Alias should be available before bound", principalAliasDao.isAliasAvailable(alias.getAlias()));
		// Save the alias and fetch is back.
		PrincipalAlias result = principalAliasDao.bindAliasToPrincipal(alias);
		assertNotNull(result);
		assertNotNull(result.getAliasId());
		assertNotNull(result.getEtag());
		assertEquals(alias.getAlias(), result.getAlias());
	}
	
	@Test(expected = NameConflictException.class)
	public void testPLFM_2482() throws NotFoundException {
		// Test binding an alias to a principal
		PrincipalAlias alias = new PrincipalAlias();
		// Use to upper as the alias
		alias.setAlias("james.bond@Spy.org");
		alias.setType(AliasType.USER_EMAIL);
		alias.setIsValidated(true);
		alias.setPrincipalId(principalId);
		PrincipalAlias result = principalAliasDao.bindAliasToPrincipal(alias);
		assertNotNull(result);
		// Now try to bind this to another user
		alias = new PrincipalAlias();
		// Use to upper as the alias
		alias.setAlias("james.bond@Spy.org");
		alias.setType(AliasType.USER_EMAIL);
		alias.setIsValidated(true);
		alias.setPrincipalId(principalId2);
		principalAliasDao.bindAliasToPrincipal(alias);
	}
    
	@Test
	public void testList() throws NotFoundException{
		
		PrincipalAlias alias = new PrincipalAlias();
		// Use to upper as the alias
		alias.setAlias("james.bond@Spy.org");
		alias.setType(AliasType.USER_EMAIL);
		alias.setIsValidated(true);
		alias.setPrincipalId(principalId);
		
		// Save the alias and fetch is back.
		PrincipalAlias emailOne = principalAliasDao.bindAliasToPrincipal(alias);
		// Create another email
		alias = new PrincipalAlias();
		// Use to upper as the alias
		alias.setAlias("james.bond@gmail.com");
		alias.setType(AliasType.USER_EMAIL);
		alias.setIsValidated(true);
		alias.setPrincipalId(principalId);
		PrincipalAlias emailTwo = principalAliasDao.bindAliasToPrincipal(alias);
		// Add a username
		alias = new PrincipalAlias();
		// Use to upper as the alias
		alias.setAlias("007");
		alias.setType(AliasType.USER_NAME);
		alias.setIsValidated(true);
		alias.setPrincipalId(principalId);
		PrincipalAlias userName = principalAliasDao.bindAliasToPrincipal(alias);
		// Now list all of them
		List<PrincipalAlias> list = principalAliasDao.listPrincipalAliases(principalId);
		assertNotNull(list);
		assertEquals(3, list.size());
		assertEquals(emailOne, list.get(0));
		assertEquals(emailTwo, list.get(1));
		assertEquals(userName, list.get(2));
		// Now filter by type
		list = principalAliasDao.listPrincipalAliases(principalId, AliasType.USER_EMAIL);
		assertNotNull(list);
		assertEquals(2, list.size());
		assertEquals(emailOne, list.get(0));
		assertEquals(emailTwo, list.get(1));
		// username only
		list = principalAliasDao.listPrincipalAliases(principalId, AliasType.USER_NAME);
		assertNotNull(list);
		assertEquals(1, list.size());
		assertEquals(userName, list.get(0));

	}
	
	@Test
	public void listPrincipalAliasesSet() throws NotFoundException{
		// Test that we can get the aliases for two separate users in one call.
		PrincipalAlias alias = new PrincipalAlias();
		alias.setAlias("foo@bar.org");
		alias.setType(AliasType.USER_EMAIL);
		alias.setIsValidated(true);
		alias.setPrincipalId(principalId);
		PrincipalAlias emailOne = principalAliasDao.bindAliasToPrincipal(alias);
		// Now do a second binding for another user
		alias = new PrincipalAlias();
		alias.setAlias("bar@bar.org");
		alias.setType(AliasType.USER_EMAIL);
		alias.setIsValidated(true);
		alias.setPrincipalId(principalId2);
		PrincipalAlias emailTwo = principalAliasDao.bindAliasToPrincipal(alias);
		
		// Validate that we can find both in one call
		Set<Long> idSet = new HashSet<Long>();
		idSet.add(principalId);
		idSet.add(principalId2);
		// List both
		List<PrincipalAlias> list = principalAliasDao.listPrincipalAliases(idSet);
		assertNotNull(list);
		assertEquals(2, list.size());
		assertEquals(emailOne, list.get(0));
		assertEquals(emailTwo, list.get(1));
	}
	
	@Test
	public void testRemoveAllAliasFromPrincipal() throws NotFoundException{
		PrincipalAlias alias = new PrincipalAlias();
		alias.setAlias("foo@bar.org");
		alias.setType(AliasType.USER_EMAIL);
		alias.setIsValidated(true);
		alias.setPrincipalId(principalId);
		PrincipalAlias emailOne = principalAliasDao.bindAliasToPrincipal(alias);
		
		alias = new PrincipalAlias();
		alias.setAlias("userName");
		alias.setType(AliasType.USER_NAME);
		alias.setIsValidated(true);
		alias.setPrincipalId(principalId);
		PrincipalAlias two = principalAliasDao.bindAliasToPrincipal(alias);
		// There should currently be 2
		List<PrincipalAlias> list = principalAliasDao.listPrincipalAliases(principalId);
		assertNotNull(list);
		assertEquals(2, list.size());
		// This should delete both
		principalAliasDao.removeAllAliasFromPrincipal(principalId);
		// List again
		list = principalAliasDao.listPrincipalAliases(principalId);
		assertNotNull(list);
		assertEquals(0, list.size());
	}
	
	@Test
	public void testRemoveAliasFromPrincipal() throws NotFoundException{
		PrincipalAlias alias = new PrincipalAlias();
		alias.setAlias("foo@bar.org");
		alias.setType(AliasType.USER_EMAIL);
		alias.setIsValidated(true);
		alias.setPrincipalId(principalId);
		PrincipalAlias emailOne = principalAliasDao.bindAliasToPrincipal(alias);
		
		alias = new PrincipalAlias();
		alias.setAlias("userName");
		alias.setType(AliasType.USER_NAME);
		alias.setIsValidated(true);
		alias.setPrincipalId(principalId);
		PrincipalAlias two = principalAliasDao.bindAliasToPrincipal(alias);
		// There should currently be 2
		List<PrincipalAlias> list = principalAliasDao.listPrincipalAliases(principalId);
		assertNotNull(list);
		assertEquals(2, list.size());
		// Remove the second only
		principalAliasDao.removeAliasFromPrincipal(principalId, two.getAliasId());
		// List again
		list = principalAliasDao.listPrincipalAliases(principalId);
		assertNotNull(list);
		assertEquals(1, list.size());
		assertEquals("This alias should not still exist", emailOne, list.get(0));
		// Now delete the other
		principalAliasDao.removeAliasFromPrincipal(principalId, emailOne.getAliasId());
		// List again
		list = principalAliasDao.listPrincipalAliases(principalId);
		assertNotNull(list);
		assertEquals(0, list.size());
	}
	
	@Test
	public void testBootStrap(){
		assertNotNull(this.userGroupDao.getBootstrapPrincipals());
		// Validate each
		for(BootstrapPrincipal bp: this.userGroupDao.getBootstrapPrincipals()){
			if(bp instanceof BootstrapUser){
				BootstrapUser user= (BootstrapUser) bp;
				assertNotNull("Bootstrap users must have an email",this.principalAliasDao.findPrincipalWithAlias(user.getEmail()));
				assertNotNull("Bootstrap users must have a username",this.principalAliasDao.findPrincipalWithAlias(user.getUserName()));
			}else{
				BootstrapGroup group = (BootstrapGroup) bp;
				assertNotNull("Bootstrap groups must have team names", this.principalAliasDao.findPrincipalWithAlias(group.getGroupName()));
			}
		}
	}
	
}
