package org.sagebionetworks.crowd.workers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.authutil.AuthenticationException;
import org.sagebionetworks.authutil.CrowdAuthUtil;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.GroupMembersDAO;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.auth.User;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })

public class CrowdGroupSynchronizerTest {

	@Autowired
	private GroupMembersDAO crowdGroupMembersDAO;
	
	@Autowired
	private GroupMembersDAO groupMembersDAO;
	
	@Autowired
	private UserGroupDAO userGroupDAO;
	
	@Autowired
	private CrowdGroupSynchronizer crowdGroupSynchronizer;
	
	private static final Log log = LogFactory.getLog(CrowdGroupSynchronizerTest.class);
	
	Set<String> originalGroupsInRDS;
	List<String> groupsToDeleteFromRDS;
	List<String> groupsToDeleteFromCrowd;
	List<String> usersToDeleteFromCrowd;
	
	@Before
	public void setUp() throws Exception {
		originalGroupsInRDS = new HashSet<String>();
		Collection<UserGroup> originals = userGroupDAO.getAll();
		for (UserGroup ug : originals) {
			originalGroupsInRDS.add(ug.getId());
		}
		groupsToDeleteFromRDS = new ArrayList<String>();
		groupsToDeleteFromCrowd = new ArrayList<String>();
		usersToDeleteFromCrowd = new ArrayList<String>();
	}

	@After
	public void tearDown() throws Exception {
		Collection<UserGroup> polluted = userGroupDAO.getAll();
		for (UserGroup ug : polluted) {
			if (!originalGroupsInRDS.contains(ug.getId())) {
				groupsToDeleteFromRDS.add(ug.getId());
			}
		}
		
		if(groupsToDeleteFromRDS != null) {
			for(String todelete: groupsToDeleteFromRDS){
				deleteTestGroupFromRDS(todelete);
			}
		}
		if(groupsToDeleteFromCrowd != null) {
			for(String todelete: groupsToDeleteFromCrowd){
				deleteTestGroupFromCrowd(todelete);
			}
		}
		if(usersToDeleteFromCrowd != null){
			for(String todelete: usersToDeleteFromCrowd) {
				CrowdAuthUtil.deleteUser(todelete);
			}
		}
	}
	
	public UserGroup createRDSTestGroup(String name) throws Exception {
		UserGroup group = new UserGroup();
		group.setName(name);
		group.setIsIndividual(false);
		String id = null;
		try {
			id = userGroupDAO.create(group);
		} catch (DatastoreException e) {
			// Already exists
			id = userGroupDAO.findGroup(name, false).getId();
		}
		assertNotNull(id);
		groupsToDeleteFromRDS.add(id);
		return userGroupDAO.get(id);
	}
	
	public void createCrowdTestGroup(String name) throws Exception {
		try {
			CrowdAuthUtil.createGroup(name);
		} catch (AuthenticationException e) {
			if (e.getRespStatus() == 400) {
				// Good, the group already exists
			} else {
				logException(e);
				throw e;
			}
		}
		groupsToDeleteFromCrowd.add(name);
	}
	
	private void deleteTestGroupFromRDS(String nameOrId) throws Exception {
		try {
			userGroupDAO.delete(nameOrId);
		} catch (NotFoundException e) {
			// Good, not in DB
		}
	}
	
	private void deleteTestGroupFromCrowd(String nameOrId) throws Exception {
		try {
			CrowdAuthUtil.deleteGroup(nameOrId);
		} catch (AuthenticationException e) {
			if (e.getRespStatus() == 404) {
				// Good, the group doesn't exist
			} else {
				logException(e);
				throw e;
			}
		}
	}
	
	private void initCrowdGroupMember(String username) throws Exception {
		// Add those users to Crowd
		User crowdUser = new User();
		crowdUser.setFirstName("bogus");
		crowdUser.setLastName("bogus");
		crowdUser.setDisplayName(username);
		crowdUser.setEmail(username);
		crowdUser.setPassword("super secure password");
		try {
			CrowdAuthUtil.createUser(crowdUser);
		} catch (AuthenticationException e) {
			if (e.getRespStatus() == 400) {
				// User already present
			} else {
				throw e;
			}
		}
		usersToDeleteFromCrowd.add(crowdUser.getEmail());
	}

	private UserGroup initRDSGroupMember(String username) throws Exception {
		// Add some users to the DB
		UserGroup user = new UserGroup();
		user.setName(username);
		user.setIsIndividual(true);
		try {
			user.setId(userGroupDAO.create(user)); 
		} catch (DatastoreException e) {
			user.setId(userGroupDAO.findGroup(username, true).getId());
		}
		groupsToDeleteFromRDS.add(user.getId());
		return user;
	}
	
	private void logException(Throwable e) {
		log.debug(e.getMessage());
		log.debug(e.getCause().getMessage());
		for (Throwable foo : e.getSuppressed()) {
			log.debug(foo.getMessage());
		}
	}

	
	@Test
	public void testNewGroupCreation() throws Exception {
		// Make a group that must be moved into RDS
		// Include a member and a non-member
		String VIPGroup = "testGroup-FancyLanguages";
		String elGenericVIP = "testerSpeaksSpanlish";
		String leGenericVIP = "testerSpeaksFranglais";
		createCrowdTestGroup(VIPGroup);
		initCrowdGroupMember(elGenericVIP);
		initCrowdGroupMember(leGenericVIP);
		CrowdAuthUtil.addUserToGroup(VIPGroup, elGenericVIP);

		crowdGroupSynchronizer.run();
		
		// Verify that everything was transferred
		assertTrue(userGroupDAO.doesPrincipalExist(VIPGroup));
		UserGroup notSoExclusiveAnymore = userGroupDAO.findGroup(VIPGroup, false);
		groupsToDeleteFromRDS.add(notSoExclusiveAnymore.getId());
		
		assertTrue(userGroupDAO.doesPrincipalExist(elGenericVIP));
		groupsToDeleteFromRDS.add(userGroupDAO.findGroup(elGenericVIP, true).getId());
		
		assertTrue(userGroupDAO.doesPrincipalExist(leGenericVIP));
		groupsToDeleteFromRDS.add(userGroupDAO.findGroup(leGenericVIP, true).getId());
		
		List<UserGroup> clubby = groupMembersDAO.getMembers(notSoExclusiveAnymore.getId());
		assertEquals("There should be one member", 1, clubby.size());
		assertEquals("The one member should be the Spanlish one", elGenericVIP, clubby.get(0).getName());
	}
	
	@Test
	public void testExistingGroupUntouched() throws Exception {
		UserGroup untouchable = createRDSTestGroup("Dalit test group");
		UserGroup untouchableMember = initRDSGroupMember("discriminated@gainst");
		
		List<String> adder = new ArrayList<String>();
		adder.add(untouchableMember.getId());
		groupMembersDAO.addMembers(untouchable.getId(), adder);
		
		// Etags have changed due to group membership
		untouchable = userGroupDAO.get(untouchable.getId());
		untouchableMember = userGroupDAO.get(untouchableMember.getId());
		
		crowdGroupSynchronizer.run();
		
		assertEquals("Etag should be the same", untouchable.getEtag(), userGroupDAO.get(untouchable.getId()).getEtag());
		assertEquals("Etag should be the same", untouchableMember.getEtag(), userGroupDAO.get(untouchableMember.getId()).getEtag());
		assertEquals("Member should still be there", 1, groupMembersDAO.getMembers(untouchable.getId()).size());
	}
	
	@Test
	public void testExistingGroupReplaced() throws Exception {
		String stompy = "testGroup OVERWRITER";
		
		UserGroup stompedUpon = createRDSTestGroup(stompy);
		UserGroup squishy = initRDSGroupMember("IgetSquishedSoon");
		List<String> adder = new ArrayList<String>();
		adder.add(squishy.getId());
		groupMembersDAO.addMembers(stompedUpon.getId(), adder);
		
		String evilMember = "MeStompOnOtherGroups";
		createCrowdTestGroup(stompy);
		initCrowdGroupMember(evilMember);
		CrowdAuthUtil.addUserToGroup(stompy, evilMember);

		crowdGroupSynchronizer.run();

		assertTrue(userGroupDAO.doesPrincipalExist(evilMember));
		groupsToDeleteFromRDS.add(userGroupDAO.findGroup(evilMember, true).getId());
		
		List<UserGroup> stomped = groupMembersDAO.getMembers(stompedUpon.getId());
		assertEquals("Stomped group should only have one member", 1, stomped.size());
		assertEquals("Stomped group should have the evil member", evilMember, stomped.get(0).getName());
		assertTrue("Squished user should still exist", userGroupDAO.get(squishy.getId()) != null);
	}
}
