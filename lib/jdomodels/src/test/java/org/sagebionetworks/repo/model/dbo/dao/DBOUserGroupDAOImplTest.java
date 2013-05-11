package org.sagebionetworks.repo.model.dbo.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.MigratableObjectData;
import org.sagebionetworks.repo.model.MigratableObjectDescriptor;
import org.sagebionetworks.repo.model.MigratableObjectType;
import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.UserGroupInt;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.UserProfileDAO;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.schema.ObjectSchema;
import org.sagebionetworks.schema.adapter.JSONEntity;
import org.sagebionetworks.schema.adapter.org.json.JSONObjectAdapterImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })

public class DBOUserGroupDAOImplTest {
	
	@Autowired
	private UserGroupDAO userGroupDAO;
		
	@Autowired
	private UserProfileDAO userProfileDAO;
		
	List<String> groupsToDelete;
	
	private static final String GROUP_NAME = "test-group";

	@Before
	public void setUp() throws Exception {
		groupsToDelete = new ArrayList<String>();
		UserGroup ug = userGroupDAO.findGroup(GROUP_NAME, false);
		if(ug != null){
			userGroupDAO.delete(ug.getId());
		}
	}

	@After
	public void tearDown() throws Exception {
		if(groupsToDelete != null && userGroupDAO != null){
			for(String todelte: groupsToDelete){
				userGroupDAO.delete(todelte);
			}
		}
	}
	
	@Test
	public void testRoundTrip() throws Exception {
		UserGroup group = new UserGroup();
		group.setName(GROUP_NAME);
		group.setIsIndividual(false);
		long initialCount = userGroupDAO.getCount();
		String groupId = userGroupDAO.create(group);
		assertNotNull(groupId);
		groupsToDelete.add(groupId);
		UserGroup clone = userGroupDAO.get(groupId);
		assertEquals(groupId, clone.getId());
		assertEquals(GROUP_NAME, clone.getName());
		assertEquals(group.getIsIndividual(), clone.getIsIndividual());
		assertEquals(1+initialCount, userGroupDAO.getCount());
	}
	
	@Test
	public void testGetMigrationObjectData() throws Exception {
		boolean foundPublic = false;
		UserGroup ug = userGroupDAO.findGroup("PUBLIC", false);
		assertNotNull(ug);
		
		boolean foundUser = false;
		UserGroup oner = new UserGroup();
		oner.setName("oner");
		oner.setIsIndividual(true);
		String onerId = userGroupDAO.create(oner);
		groupsToDelete.add(onerId);
		
		UserProfile up = new UserProfile();
		up.setOwnerId(onerId);
		up.setEtag("some tag");
		String jsonString = (String) UserProfile.class.getField(JSONEntity.EFFECTIVE_SCHEMA).get(null);
		ObjectSchema schema = new ObjectSchema(new JSONObjectAdapterImpl(jsonString));
		String upId = userProfileDAO.create(up, schema); // this will be deleted via cascade when the user-group is deleted
		
		QueryResults<MigratableObjectData> migrationData = userGroupDAO.getMigrationObjectData(0, 10000, true);
		assert(migrationData.getTotalNumberOfResults()>1);
		assertEquals(migrationData.getTotalNumberOfResults(), migrationData.getResults().size());
		
		for (MigratableObjectData od : migrationData.getResults()) {
			MigratableObjectDescriptor obj = od.getId();
			assertNotNull(obj.getId());
			assertEquals(MigratableObjectType.PRINCIPAL, obj.getType());
			assertNotNull(od.getEtag());
			assertTrue(od.getDependencies().isEmpty()); // Groups are not dependent on any other migratable object
			if (obj.getId().equals(ug.getId())) {
				foundPublic = true;
				assertEquals(DBOUserGroupDAOImpl.DEFAULT_ETAG, od.getEtag()); // multiuser groups have no real etags
			}
			
			if (obj.getId().equals(onerId)) {
				foundUser=true;
			}
		}
		
		assertTrue(foundPublic);
		assertTrue(foundUser);
		
		// make sure pagination works
		migrationData = userGroupDAO.getMigrationObjectData(0, 1, true);
		assertEquals(1, migrationData.getResults().size());
	}
	
	
	@Test
	public void findAnonymousUser() throws Exception {
		assertNotNull(userGroupDAO.findGroup(AuthorizationConstants.ANONYMOUS_USER_ID, true));
	}
	@Test
	public void testDoesPrincipalExist() throws Exception {
		UserGroup group = new UserGroup();
		group.setName(GROUP_NAME);
		String groupId = userGroupDAO.create(group);
		assertNotNull(groupId);
		groupsToDelete.add(groupId);
		
		assertTrue(userGroupDAO.doesPrincipalExist(GROUP_NAME));
		
		assertFalse(userGroupDAO.doesPrincipalExist(""+(new Random()).nextLong()));
	}
	@Test
	public void testGetGroupsByNamesEmptySet()  throws Exception {

		Collection<String> groupNames = new HashSet<String>();
		Map<String,UserGroup> map =  userGroupDAO.getGroupsByNames(groupNames);
		assertTrue(map.isEmpty());
	}

	@Test
	public void testGetGroupsByNames() throws Exception {
		Collection<UserGroup> allGroups = null; 
		allGroups = userGroupDAO.getAll();
		int startingCount =  allGroups.size();
	
		Collection<String> groupNames = new HashSet<String>();
		groupNames.add(GROUP_NAME);
		Map<String,UserGroup> map = null;
		map = userGroupDAO.getGroupsByNames(groupNames);
		assertFalse("initial groups: "+allGroups+"  getGroupsByNames("+GROUP_NAME+") returned "+map.keySet(), map.containsKey(GROUP_NAME));
//		assertFalse(map.containsKey(GROUP_NAME));
			
		UserGroup group = new UserGroup();
		group.setName(GROUP_NAME);
		String groupId = userGroupDAO.create(group);
		assertNotNull(groupId);
		groupsToDelete.add(groupId);
		allGroups = userGroupDAO.getAll();
		assertEquals(allGroups.toString(), (startingCount+1), allGroups.size()); // now the new group should be there
			
		groupNames.clear();
		groupNames.add(GROUP_NAME);	
		map = userGroupDAO.getGroupsByNames(groupNames);
		assertTrue(groupNames.toString()+" -> "+map.toString(), map.containsKey(GROUP_NAME));
		
		
		groupNames.clear(); 
		// Add one of the default groups
		groupNames.add(AuthorizationConstants.DEFAULT_GROUPS.AUTHENTICATED_USERS.name());
		map = userGroupDAO.getGroupsByNames(groupNames);
		assertTrue(map.toString(), map.containsKey(AuthorizationConstants.DEFAULT_GROUPS.AUTHENTICATED_USERS.name()));

		// try the paginated call
		List<UserGroup> groups = userGroupDAO.getInRange(0, startingCount+100, false);
		List<String> omit = new ArrayList<String>();
		omit.add(AuthorizationConstants.DEFAULT_GROUPS.AUTHENTICATED_USERS.name());
		List<UserGroup> groupsButOne = userGroupDAO.getInRangeExcept(0, startingCount+100, false, omit);
		assertEquals(groups.size(), groupsButOne.size()+1);
	}
	
	@Test
	public void testBootstrapUsers() throws DatastoreException, NotFoundException{
		List<UserGroupInt> boots = this.userGroupDAO.getBootstrapUsers();
		assertNotNull(boots);
		assertTrue(boots.size() >0);
		// Each should exist
		for(UserGroupInt bootUg: boots){
			UserGroup ug = userGroupDAO.get(bootUg.getId());
			assertEquals(bootUg.getId(), ug.getId());
			assertEquals(bootUg.getName(), ug.getName());
		}
	}

}
