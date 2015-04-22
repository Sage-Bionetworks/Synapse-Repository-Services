package org.sagebionetworks.repo.model.dbo.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.NodeConstants;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.UserProfileDAO;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.file.ExternalFileHandle;
import org.sagebionetworks.repo.model.principal.BootstrapPrincipal;
import org.sagebionetworks.repo.model.principal.BootstrapUser;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class DBOUserProfileDAOImplTest {

	@Autowired 
	UserGroupDAO userGroupDAO;
	
	@Autowired
	UserProfileDAO userProfileDAO;
	
	@Autowired
	FileHandleDao fileHandleDao;
	
	private UserGroup individualGroup = null;
	
	private List<UserGroup> individualGroupsToDelete = null;
	private List<String> fileHandlesToDelete = null;
	
	private List<String> toDelete;
	
	@Before
	public void setUp() throws Exception {
		individualGroup = new UserGroup();
		individualGroup.setIsIndividual(true);
		individualGroup.setCreationDate(new Date());
		individualGroup.setId(userGroupDAO.create(individualGroup).toString());
		toDelete = new LinkedList<String>();
		individualGroupsToDelete = new ArrayList<UserGroup>();
		for (int i=0; i<2; i++) {
			UserGroup individualGroup = new UserGroup();
			individualGroup.setIsIndividual(true);
			individualGroup.setCreationDate(new Date());
			individualGroup.setId(userGroupDAO.create(individualGroup).toString());
			individualGroupsToDelete.add(individualGroup);
		}
		fileHandlesToDelete = new LinkedList<String>();
	}
		
	
	@After
	public void tearDown() throws Exception{
		for (UserGroup ug : individualGroupsToDelete) {
			// this will delete the user profile too
			userGroupDAO.delete(ug.getId());
		}
		if(toDelete != null){
			for(String id: toDelete){
				try {
					userProfileDAO.delete(id);
				} catch (Exception e) {}
			}
		}
		if(fileHandlesToDelete != null){
			for(String id:fileHandlesToDelete){
				try {
					fileHandleDao.delete(id);
				} catch (Exception e) {}
			}
		}
		individualGroupsToDelete.clear();
	}
	
	@Test
	public void testCRUD() throws Exception{
		List<UserProfile> userProfiles = new ArrayList<UserProfile>();
		long initialCount = userProfileDAO.getCount();
		// Create it
		for (UserGroup ug : individualGroupsToDelete) {
			// Create a new user profile
			UserProfile userProfile = new UserProfile();
			userProfile.setOwnerId(ug.getId());
			userProfile.setFirstName("foo");
			userProfile.setLastName("bar");
			userProfile.setRStudioUrl("http://rstudio.com");
			userProfile.setEtag(NodeConstants.ZERO_E_TAG);
			userProfiles.add(userProfile);
			// Create it
			String id = userProfileDAO.create(userProfile);
			assertNotNull(id);
			userProfile.setOwnerId(id);
		}
		
		assertEquals(userProfiles.size()+initialCount, userProfileDAO.getCount());
		
		// Fetch it
		UserProfile userProfile = userProfiles.get(0);
		String id = userProfile.getOwnerId();
		UserProfile clone = userProfileDAO.get(id);
		assertNotNull(clone);
		assertEquals(userProfile, clone);
		
		Long idLong1 = Long.parseLong(userProfiles.get(1).getOwnerId());
		Long idLong0 = Long.parseLong(userProfiles.get(0).getOwnerId());
		List<UserProfile> listed = userProfileDAO.list(Arrays.asList(new Long[]{idLong1, idLong0}));
		assertEquals(2, listed.size());
		assertEquals(Arrays.asList(new UserProfile[]{userProfiles.get(1), userProfiles.get(0)}), listed);
		try {
			userProfileDAO.list(Arrays.asList(new Long[]{idLong1, 87765443L+idLong0}));
			fail("NotFoundException expected");
		} catch (NotFoundException e) {
			//as expected
		}

		// Update it
		UserProfile updatedProfile = userProfileDAO.update(clone);
		assertTrue("etags should be different after an update", !clone.getEtag().equals(updatedProfile.getEtag()));

		try {
			clone.setFirstName("This Should Fail");
			userProfileDAO.update(clone);
			fail("conflicting update exception not thrown");
		}
		catch(ConflictingUpdateException e) {
			// We expected this exception
		}

		// Delete it
		for (UserProfile up: userProfiles) {
			userProfileDAO.delete(up.getOwnerId());
		}

		assertEquals(initialCount, userProfileDAO.getCount());
	}
	
	@Test
	public void testGetPictureFileHandleIdNotFound(){
		// Create a new type
		UserProfile userProfile = new UserProfile();
		userProfile.setOwnerId(individualGroup.getId());
		userProfile.setFirstName("foo");
		userProfile.setLastName("bar");
		userProfile.setRStudioUrl("http://rstudio.com");
		userProfile.setEtag(NodeConstants.ZERO_E_TAG);
		// Create it
		String id = userProfileDAO.create(userProfile);
		assertNotNull(id);
		toDelete.add(id);
		try {
			userProfileDAO.getPictureFileHandleId(id);
			fail("Should have failed");
		} catch (NotFoundException e) {
			assertTrue(e.getMessage().contains(id));
		}
	}
	
	@Test
	public void testGetPictureFileHandleId() throws NotFoundException{
		ExternalFileHandle ef = new ExternalFileHandle();
		ef.setExternalURL("http://google.com");
		ef.setCreatedBy(individualGroup.getId());
		ef.setCreatedOn(new Date());
		ef.setFileName("Some name");
		ef = fileHandleDao.createFile(ef);
		fileHandlesToDelete.add(ef.getId());
		// Create a new type
		UserProfile userProfile = new UserProfile();
		userProfile.setOwnerId(individualGroup.getId());
		userProfile.setFirstName("foo");
		userProfile.setLastName("bar");
		userProfile.setRStudioUrl("http://rstudio.com");
		userProfile.setEtag(NodeConstants.ZERO_E_TAG);
		userProfile.setProfilePicureFileHandleId(ef.getId());
		// Create it
		String id = userProfileDAO.create(userProfile);
		assertNotNull(id);
		toDelete.add(id);
		String fileId = userProfileDAO.getPictureFileHandleId(id);
		assertEquals(ef.getId(), fileId);
	}
	
	@Test
	public void testBootstrapUsers() throws DatastoreException, NotFoundException{
		List<BootstrapPrincipal> boots = this.userGroupDAO.getBootstrapPrincipals();
		assertNotNull(boots);
		assertTrue(boots.size() >0);
		// Each should exist
		for(BootstrapPrincipal bootUg: boots){
			if(bootUg instanceof BootstrapUser){
				UserProfile profile = userProfileDAO.get(bootUg.getId().toString());
				userGroupDAO.get(bootUg.getId());
				assertEquals(bootUg.getId().toString(), profile.getOwnerId());
			}
		}
	}
}
