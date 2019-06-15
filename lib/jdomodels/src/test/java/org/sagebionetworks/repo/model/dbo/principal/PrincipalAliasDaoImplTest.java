package org.sagebionetworks.repo.model.dbo.principal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
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
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.NameConflictException;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.UserGroupHeader;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.UserProfileDAO;
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

import com.google.common.collect.Lists;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class PrincipalAliasDaoImplTest {

	@Autowired
	private PrincipalAliasDAO principalAliasDao;
	
	@Autowired
	private UserProfileDAO userProfileDao;
	
	@Autowired
	private UserGroupDAO userGroupDao;
	
	@Mock
	ResultSet mockResultSet;
	
	Long principalId;
	Long principalId2;
	List<Long> toDelete;
	
	@Before
	public void before() throws DatastoreException, NotFoundException{
		MockitoAnnotations.initMocks(this);
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
	public void testFindPrincipalForAlias(){
		PrincipalAlias alias = new PrincipalAlias();
		alias.setAlias(UUID.randomUUID().toString()+"@test.com");
		alias.setType(AliasType.USER_EMAIL);
		alias.setPrincipalId(principalId);
		
		PrincipalAlias expected = principalAliasDao.bindAliasToPrincipal(alias);
		
		PrincipalAlias actual = principalAliasDao.findPrincipalWithAlias(alias.getAlias());
		assertEquals(expected, actual);
		
		// this alias does not exist
		assertNull(principalAliasDao.findPrincipalWithAlias(UUID.randomUUID().toString()));
	}

	@Test
	public void testFindPrincipalForAliasWithTypeFilter(){
		PrincipalAlias alias = new PrincipalAlias();
		alias.setAlias(UUID.randomUUID().toString()+"@test.com");
		alias.setType(AliasType.USER_EMAIL);
		alias.setPrincipalId(principalId);

		PrincipalAlias expected = principalAliasDao.bindAliasToPrincipal(alias);

		PrincipalAlias actual = principalAliasDao.findPrincipalWithAlias(alias.getAlias(), AliasType.USER_EMAIL, AliasType.USER_NAME);
		assertEquals(expected, actual);

		// this alias does not exist
		assertNull(principalAliasDao.findPrincipalWithAlias(alias.getAlias(), AliasType.TEAM_NAME));
	}
	
	@Test (expected=NotFoundException.class)
	public void testPrincipalNotFound() throws NotFoundException{
		PrincipalAlias alias = new PrincipalAlias();
		alias.setAlias("Fake principals Id");
		alias.setType(AliasType.TEAM_NAME);
		// No principal should exist with this ID.
		alias.setPrincipalId(-1L);
		principalAliasDao.bindAliasToPrincipal(alias);
	}
	
	@Test
	public void testBindEmail() throws NotFoundException{
		// Test binding an alias to a principal
		PrincipalAlias alias = new PrincipalAlias();
		// Use to upper as the alias
		alias.setAlias("james.bond@Spy.org");
		alias.setType(AliasType.USER_EMAIL);
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
		alias.setPrincipalId(principalId);
		PrincipalAlias result = principalAliasDao.bindAliasToPrincipal(alias);
		assertNotNull(result);
		// Now try to bind this to another user
		alias = new PrincipalAlias();
		// Use to upper as the alias
		alias.setAlias("james.bond@Spy.org");
		alias.setType(AliasType.USER_EMAIL);
		alias.setPrincipalId(principalId2);
		principalAliasDao.bindAliasToPrincipal(alias);
	}
    
	@Test
	public void testList() throws NotFoundException{
		int startingCount = principalAliasDao.listPrincipalAliases(AliasType.USER_EMAIL).size();
		PrincipalAlias alias = new PrincipalAlias();
		// Use to upper as the alias
		alias.setAlias("james.bond@Spy.org");
		alias.setType(AliasType.USER_EMAIL);
		alias.setPrincipalId(principalId);
		
		// Save the alias and fetch is back.
		PrincipalAlias emailOne = principalAliasDao.bindAliasToPrincipal(alias);
		// Create another email
		alias = new PrincipalAlias();
		// Use to upper as the alias
		alias.setAlias("james.bond@gmail.com");
		alias.setType(AliasType.USER_EMAIL);
		alias.setPrincipalId(principalId);
		PrincipalAlias emailTwo = principalAliasDao.bindAliasToPrincipal(alias);
		// Add a username
		alias = new PrincipalAlias();
		// Use to upper as the alias
		alias.setAlias("007");
		alias.setType(AliasType.USER_NAME);
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
		
		// Test listing all by type
		list = principalAliasDao.listPrincipalAliases(AliasType.USER_EMAIL);
		assertTrue(startingCount < list.size());
		
		list = principalAliasDao.listPrincipalAliases(principalId, AliasType.USER_EMAIL, "james.bond@Spy.org");
		assertNotNull(list);
		assertEquals(1, list.size());
		assertEquals(emailOne, list.get(0));
		list = principalAliasDao.listPrincipalAliases(principalId, AliasType.USER_EMAIL, "foo");
		assertNotNull(list);
		assertTrue(list.isEmpty());
	}
	
	@Test
	public void listPrincipalAliasesSet() throws NotFoundException{
		// Test that we can get the aliases for two separate users in one call.
		PrincipalAlias alias = new PrincipalAlias();
		alias.setAlias("foo@bar.org");
		alias.setType(AliasType.USER_EMAIL);
		alias.setPrincipalId(principalId);
		PrincipalAlias emailOne = principalAliasDao.bindAliasToPrincipal(alias);
		// Now do a second binding for another user
		alias = new PrincipalAlias();
		alias.setAlias("bar@bar.org");
		alias.setType(AliasType.USER_EMAIL);
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
	public void testEmptyPrincipalIdList() throws Exception {
		assertTrue(principalAliasDao.listPrincipalAliases(Collections.EMPTY_SET).isEmpty());
	}
	
	@Test
	public void testRemoveAllAliasFromPrincipal() throws NotFoundException{
		PrincipalAlias alias = new PrincipalAlias();
		alias.setAlias("foo@bar.org");
		alias.setType(AliasType.USER_EMAIL);
		alias.setPrincipalId(principalId);
		PrincipalAlias emailOne = principalAliasDao.bindAliasToPrincipal(alias);
		
		alias = new PrincipalAlias();
		alias.setAlias("userName");
		alias.setType(AliasType.USER_NAME);
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
		alias.setPrincipalId(principalId);
		PrincipalAlias emailOne = principalAliasDao.bindAliasToPrincipal(alias);
		
		alias = new PrincipalAlias();
		alias.setAlias("userName");
		alias.setType(AliasType.USER_NAME);
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
	public void testOneUsernamePerPrincipal() throws NotFoundException{
		// Users are only allowed to have one username.
		PrincipalAlias alias = new PrincipalAlias();
		alias.setAlias("userName");
		alias.setType(AliasType.USER_NAME);
		alias.setPrincipalId(principalId);
		PrincipalAlias one = principalAliasDao.bindAliasToPrincipal(alias);
		assertNotNull(one);
		// Now try to bind another name
		alias = new PrincipalAlias();
		alias.setAlias("newUserName");
		alias.setType(AliasType.USER_NAME);
		alias.setPrincipalId(principalId);
		PrincipalAlias two = principalAliasDao.bindAliasToPrincipal(alias);
		assertNotNull(two);
		assertEquals("Binding two different usernames to the same users should have updated the first one and not created a second one.",one.getAliasId(), two.getAliasId());
		// The etag should have changed.
		assertFalse("The etag of the changed alias should have changed.",one.getEtag().equals(two.getEtag()));
		// Listing the aliases
		List<PrincipalAlias> list = principalAliasDao.listPrincipalAliases(principalId);
		assertNotNull(list);
		assertEquals(1, list.size());
		assertEquals("There should only be one alias and it should match the second.",two, list.get(0));
	}
	
	@Test
	public void testOneTeamNamePerPrincipal() throws NotFoundException{
		// Users are only allowed to have one username.
		PrincipalAlias alias = new PrincipalAlias();
		alias.setAlias("team name one");
		alias.setType(AliasType.TEAM_NAME);
		alias.setPrincipalId(principalId);
		PrincipalAlias one = principalAliasDao.bindAliasToPrincipal(alias);
		assertNotNull(one);
		// Now try to bind another name
		alias = new PrincipalAlias();
		alias.setAlias("team name two");
		alias.setType(AliasType.TEAM_NAME);
		alias.setPrincipalId(principalId);
		PrincipalAlias two = principalAliasDao.bindAliasToPrincipal(alias);
		assertNotNull(two);
		assertEquals("Binding two different team names to the same team should have updated the first one and not created a second one.",one.getAliasId(), two.getAliasId());
		// The etag should have changed.
		assertFalse("The etag of the changed alias should have changed.",one.getEtag().equals(two.getEtag()));
		// Listing the aliases
		List<PrincipalAlias> list = principalAliasDao.listPrincipalAliases(principalId);
		assertNotNull(list);
		assertEquals(1, list.size());
		assertEquals("There should only be one alias and it should match the second.",two, list.get(0));
	}
	
	@Test
	public void testBootStrap(){
		assertNotNull(this.userGroupDao.getBootstrapPrincipals());
		// Validate each
		for(BootstrapPrincipal bp: this.userGroupDao.getBootstrapPrincipals()){
			if(bp instanceof BootstrapUser){
				BootstrapUser user= (BootstrapUser) bp;
				PrincipalAlias alias = this.principalAliasDao.findPrincipalWithAlias(user.getEmail().getAliasName());
				assertNotNull("Bootstrap users must have an email",alias);
				assertEquals("We must keep the ID of bootstrapped aliases",user.getEmail().getAliasId(), alias.getAliasId());
				alias = this.principalAliasDao.findPrincipalWithAlias(user.getUserName().getAliasName());
				assertEquals("We must keep the ID of bootstrapped aliases",user.getUserName().getAliasId(), alias.getAliasId());
			}else{
				BootstrapGroup group = (BootstrapGroup) bp;
				PrincipalAlias alias = this.principalAliasDao.findPrincipalWithAlias(group.getGroupAlias().getAliasName());
				assertNotNull("Bootstrap groups must have team names", alias);
				assertEquals("We must keep the ID of bootstrapped aliases",group.getGroupAlias().getAliasId(), alias.getAliasId());
			}
		}
	}

	@Test(expected = IllegalArgumentException.class)
	public void testGetPrincipalIdWithNullAlias() {
		principalAliasDao.lookupPrincipalID(null, AliasType.USER_NAME);
	}

	@Test(expected = NotFoundException.class)
	public void testGetPrincipalIdWithEmptyAlias() {
		principalAliasDao.lookupPrincipalID("", AliasType.USER_NAME);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testGetPrincipalIdWithNullType() {
		principalAliasDao.lookupPrincipalID("anonymous", null);
	}

	@Test
	public void testGetPrincipalId() {
		String username = UUID.randomUUID().toString();
		PrincipalAlias alias = new PrincipalAlias();
		alias.setAlias(username);
		alias.setType(AliasType.USER_NAME);
		alias.setPrincipalId(principalId);
		String toLookup = principalAliasDao.bindAliasToPrincipal(alias).getAlias()+" ";
		assertEquals(principalId, (Long)principalAliasDao.lookupPrincipalID(toLookup, AliasType.USER_NAME));
	}

	@Test (expected = NotFoundException.class)
	public void testGetUserNameNotFound() {
		principalAliasDao.getUserName(principalId+1);
	}

	@Test
	public void testGetUserName() {
		String username = UUID.randomUUID().toString();
		PrincipalAlias alias = new PrincipalAlias();
		alias.setAlias(username);
		alias.setType(AliasType.USER_NAME);
		alias.setPrincipalId(principalId);
		principalAliasDao.bindAliasToPrincipal(alias);
		assertEquals(username, principalAliasDao.getUserName(principalId));
	}

	@Test (expected = NotFoundException.class)
	public void testGetTeamNameNotFound() {
		principalAliasDao.getTeamName(principalId+1);
	}

	@Test
	public void testGetTeamName() {
		String teamName = UUID.randomUUID().toString();
		PrincipalAlias alias = new PrincipalAlias();
		alias.setAlias(teamName);
		alias.setType(AliasType.TEAM_NAME);
		alias.setPrincipalId(principalId);
		principalAliasDao.bindAliasToPrincipal(alias);
		assertEquals(teamName, principalAliasDao.getTeamName(principalId));
	}
	
	@Test
	public void testListPrincipalHeaders(){
		// setup a user
		PrincipalAlias alias = new PrincipalAlias();
		alias.setAlias("User_One");
		alias.setType(AliasType.USER_NAME);
		alias.setPrincipalId(principalId);
		PrincipalAlias one = principalAliasDao.bindAliasToPrincipal(alias);
		assertNotNull(one);
		// Give the user a first and last name
		UserProfile profile = new UserProfile();
		profile.setOwnerId(""+principalId);
		profile.setFirstName("James");
		profile.setLastName("Bond");
		userProfileDao.create(profile);
		
		// setup a team
		alias = new PrincipalAlias();
		alias.setAlias("Team One");
		alias.setType(AliasType.TEAM_NAME);
		alias.setPrincipalId(principalId2);
		PrincipalAlias two = principalAliasDao.bindAliasToPrincipal(alias);
		assertNotNull(two);
		
		// call under test
		List<UserGroupHeader> headers = principalAliasDao.listPrincipalHeaders(Lists.newArrayList(principalId2, principalId));
		assertNotNull(headers);
		assertEquals(2, headers.size());
		// the first header is for a team
		UserGroupHeader header = headers.get(0);
		assertEquals(""+principalId2, header.getOwnerId());
		assertFalse(header.getIsIndividual());
		assertEquals("Team One", header.getUserName());
		assertEquals(null, header.getFirstName());
		assertEquals(null, header.getLastName());
		// the second header is for a user
		header = headers.get(1);
		assertEquals(""+principalId, header.getOwnerId());
		assertTrue(header.getIsIndividual());
		assertEquals("User_One", header.getUserName());
		assertEquals("James", header.getFirstName());
		assertEquals("Bond", header.getLastName());
	}
	
	@Test
	public void testListPrincipalHeadersNullNames(){
		// setup a user
		PrincipalAlias alias = new PrincipalAlias();
		alias.setAlias("User_One");
		alias.setType(AliasType.USER_NAME);
		alias.setPrincipalId(principalId);
		PrincipalAlias one = principalAliasDao.bindAliasToPrincipal(alias);
		assertNotNull(one);
		// Give the user a first and last name
		UserProfile profile = new UserProfile();
		profile.setOwnerId(""+principalId);
		profile.setFirstName(null);
		profile.setLastName(null);
		userProfileDao.create(profile);
		
		// call under test
		List<UserGroupHeader> headers = principalAliasDao.listPrincipalHeaders(Lists.newArrayList(principalId));
		assertNotNull(headers);
		assertEquals(1, headers.size());
		// the first header is for a team
		UserGroupHeader header = headers.get(0);
		assertEquals(""+principalId, header.getOwnerId());
		assertTrue(header.getIsIndividual());
		assertEquals("User_One", header.getUserName());
		assertEquals(null, header.getFirstName());
		assertEquals(null, header.getLastName());
	}
	
	/**
	 * First and Last names are stored as tiny blobs of UTF-8 bytes,
	 * so they must be read correctly in the headers.
	 */
	@Test
	public void testListPrincipalHeadersUTF8Names(){
		// setup a user
		PrincipalAlias alias = new PrincipalAlias();
		alias.setAlias("User_One");
		alias.setType(AliasType.USER_NAME);
		alias.setPrincipalId(principalId);
		PrincipalAlias one = principalAliasDao.bindAliasToPrincipal(alias);
		assertNotNull(one);
		String firstName = "Älp√";
		String lastName = "£♥◙";
		// Give the user a first and last name
		UserProfile profile = new UserProfile();
		profile.setOwnerId(""+principalId);
		profile.setFirstName(firstName);
		profile.setLastName(lastName);
		userProfileDao.create(profile);
		
		// call under test
		List<UserGroupHeader> headers = principalAliasDao.listPrincipalHeaders(Lists.newArrayList(principalId));
		assertNotNull(headers);
		assertEquals(1, headers.size());
		UserGroupHeader header = headers.get(0);
		assertEquals(""+principalId, header.getOwnerId());
		assertTrue(header.getIsIndividual());
		assertEquals("User_One", header.getUserName());
		assertEquals(firstName, header.getFirstName());
		assertEquals(lastName, header.getLastName());
	}
	
	/**
	 * If the ID does not exist it should not be in the results.
	 */
	@Test
	public void testListPrincipalHeadersDoesNotExist(){
		// setup a user
		PrincipalAlias alias = new PrincipalAlias();
		alias.setAlias("User_One");
		alias.setType(AliasType.USER_NAME);
		alias.setPrincipalId(principalId);
		PrincipalAlias one = principalAliasDao.bindAliasToPrincipal(alias);
		
		long idDoesNotExist = -1L;
		
		// call under test
		List<UserGroupHeader> headers = principalAliasDao.listPrincipalHeaders(Lists.newArrayList(principalId, idDoesNotExist));
		assertNotNull(headers);
		assertEquals(1, headers.size());
		UserGroupHeader header = headers.get(0);
		assertEquals(""+principalId, header.getOwnerId());
	}
	
	@Test
	public void testListPrincipalHeadersEmpty(){
		// empty list should not fail.
		// call under test
		List<UserGroupHeader> headers = principalAliasDao.listPrincipalHeaders(new LinkedList<Long>());
		assertNotNull(headers);
		assertEquals(0, headers.size());
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testListPrincipalHeadersNull(){
		// call under test
		List<UserGroupHeader> headers = principalAliasDao.listPrincipalHeaders(null);
		assertNotNull(headers);
		assertEquals(0, headers.size());
	}
	
	@Test
	public void testGetStringUTF8Null() throws SQLException{
		String name = "aName";
		when(mockResultSet.getBytes(name)).thenReturn(null);
		// call under test
		String result = PrincipalAliasDaoImpl.getStringUTF8(mockResultSet, name);
		assertEquals(null, result);
	}
	
	@Test
	public void testGetStringUTF8() throws Exception {
		String name = "aName";
		String value = "someValue";
		when(mockResultSet.getBytes(name)).thenReturn(value.getBytes("UTF-8"));
		// call under test
		String result = PrincipalAliasDaoImpl.getStringUTF8(mockResultSet, name);
		assertEquals(value, result);
	}
	
	@Test	
	public void testFindPrincipalsWithAliases(){
		// Test that we can get the aliases for two separate users in one call.
		PrincipalAlias alias = new PrincipalAlias();
		alias.setAlias("fooUser");
		alias.setType(AliasType.USER_NAME);
		alias.setPrincipalId(principalId);
		PrincipalAlias userName = principalAliasDao.bindAliasToPrincipal(alias);
		// Now do a second binding for another user
		alias = new PrincipalAlias();
		alias.setAlias("foo Team");
		alias.setType(AliasType.TEAM_NAME);
		alias.setPrincipalId(principalId2);
		PrincipalAlias teamName = principalAliasDao.bindAliasToPrincipal(alias);
		
		List<String> aliasList = Lists.newArrayList("fooUser", "foo Team");
		List<AliasType> types = Lists.newArrayList(AliasType.USER_EMAIL);
		// type filter does not match aliases
		List<Long> results = principalAliasDao.findPrincipalsWithAliases(aliasList, types);
		assertNotNull(results);
		assertEquals(0,  results.size());
		// type by user_name;
		types = Lists.newArrayList(AliasType.USER_NAME);
		results = principalAliasDao.findPrincipalsWithAliases(aliasList, types);
		assertNotNull(results);
		assertEquals(1,  results.size());
		assertEquals(Lists.newArrayList(userName.getPrincipalId()), results);
		// type by team
		types = Lists.newArrayList(AliasType.TEAM_NAME);
		results = principalAliasDao.findPrincipalsWithAliases(aliasList, types);
		assertNotNull(results);
		assertEquals(1,  results.size());
		assertEquals(Lists.newArrayList(teamName.getPrincipalId()), results);
		// both
		types = Lists.newArrayList(AliasType.TEAM_NAME, AliasType.USER_NAME);
		results = principalAliasDao.findPrincipalsWithAliases(aliasList, types);
		assertNotNull(results);
		assertEquals(2,  results.size());
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testFindPrincipalsWithAliasesNullAliases(){
		List<String> aliasList = null;
		List<AliasType> types = Lists.newArrayList(AliasType.USER_EMAIL);
		// call under test
		principalAliasDao.findPrincipalsWithAliases(aliasList, types);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testFindPrincipalsWithAliasesNullTypes(){
		List<String> aliasList = Lists.newArrayList("fooUser", "foo Team");
		List<AliasType> types = null;
		// call under test
		principalAliasDao.findPrincipalsWithAliases(aliasList, types);
	}
	
	@Test
	public void testFindPrincipalsWithAliasesEmptyAlliases(){
		List<String> aliasList = new LinkedList<>();
		List<AliasType> types = Lists.newArrayList(AliasType.USER_EMAIL);
		// call under test
		List<Long> results = principalAliasDao.findPrincipalsWithAliases(aliasList, types);
		assertNotNull(results);
		assertTrue(results.isEmpty());
	}
	
	@Test
	public void testFindPrincipalsWithAliasesEmptyTypes(){
		List<String> aliasList = Lists.newArrayList("fooUser", "foo Team");
		List<AliasType> types = new LinkedList<>();
		// call under test
		List<Long> results = principalAliasDao.findPrincipalsWithAliases(aliasList, types);
		assertNotNull(results);
		assertTrue(results.isEmpty());
	}

	@Test
	public void testAliasIsBoundToPrincipal() {
		String boundEmail = "bound@test.com";
		PrincipalAlias alias = new PrincipalAlias();
		alias.setAlias(boundEmail);
		alias.setPrincipalId(principalId);
		alias.setType(AliasType.USER_EMAIL);
		PrincipalAlias result = principalAliasDao.bindAliasToPrincipal(alias);
		String principalIdAsString = Long.toString(principalId);

		assertTrue(principalAliasDao.aliasIsBoundToPrincipal(boundEmail, principalIdAsString));
		assertTrue(principalAliasDao.aliasIsBoundToPrincipal("bOunD@TesT.COM", principalIdAsString));
		assertFalse(principalAliasDao.aliasIsBoundToPrincipal("not-bound@test.com", principalIdAsString));
		assertFalse(principalAliasDao.aliasIsBoundToPrincipal(boundEmail, "not-the-right-id"));
	}
}
