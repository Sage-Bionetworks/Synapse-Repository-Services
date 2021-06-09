package org.sagebionetworks.repo.manager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.model.ActivityDAO;
import org.sagebionetworks.repo.model.Reference;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.AuthorizationStatus;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.provenance.Activity;
import org.sagebionetworks.repo.model.provenance.Used;
import org.sagebionetworks.repo.model.provenance.UsedEntity;
import org.sagebionetworks.repo.model.provenance.UsedURL;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.schema.adapter.AdapterFactory;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.AdapterFactoryImpl;

public class ActivityManagerImplTest {
	
	IdGenerator mockIdGenerator;
	ActivityDAO mockActivityDAO;
	AuthorizationManager mockAuthorizationManager;	
	ActivityManager activityManager;
	UserInfo normalUserInfo;
	UserInfo adminUserInfo;
	
	AdapterFactory adapterFactory = new AdapterFactoryImpl();
	

	@Before
	public void before() throws Exception{
		mockIdGenerator = mock(IdGenerator.class);
		mockActivityDAO = mock(ActivityDAO.class);
		mockAuthorizationManager = mock(AuthorizationManager.class);
		normalUserInfo = new UserInfo(false);
		adminUserInfo = new UserInfo(true);
		configureUser(adminUserInfo, "1");
		configureUser(normalUserInfo, "2");
		 
		activityManager = new ActivityManagerImpl(mockIdGenerator, mockActivityDAO, mockAuthorizationManager);
	}

	@Test
	public void testCreateActivity() throws Exception {
		String id = "123";
		when(mockIdGenerator.generateNewId(IdType.ACTIVITY_ID)).thenReturn(new Long(id));
		
		activityManager.createActivity(normalUserInfo, new Activity());
		
		ArgumentCaptor<Activity> captor = ArgumentCaptor.forClass(Activity.class);
		verify(mockActivityDAO).create(captor.capture());
		Activity createdAct = captor.getValue();
		assertNotNull(createdAct);
		assertEquals(id, createdAct.getId());
		assertNotNull(createdAct.getCreatedBy());
		assertNotNull(createdAct.getCreatedOn());
		assertNotNull(createdAct.getModifiedBy());
		assertNotNull(createdAct.getModifiedOn());
	}
	
	@Test
	public void testCreateActivityWithAnonymousUser() throws Exception {
		UserInfo anonymousUser = UserInfoHelper.createAnonymousUserInfo();
		UnauthorizedException ex = assertThrows(UnauthorizedException.class, () -> { 
			activityManager.createActivity(anonymousUser, new Activity());
		});
		assertEquals(ex.getMessage(), "Cannot create activity with anonymous user.");
	}

	@Test
	public void testCreateActivityFakePeople() throws Exception {
		String id = "123";
		when(mockIdGenerator.generateNewId(IdType.ACTIVITY_ID)).thenReturn(new Long(id));
		Activity act = new Activity();
		String noOneUserId = "noone!";
		act.setCreatedBy(noOneUserId);
		act.setModifiedBy(noOneUserId);
		activityManager.createActivity(normalUserInfo, act);
		
		ArgumentCaptor<Activity> captor = ArgumentCaptor.forClass(Activity.class);
		verify(mockActivityDAO).create(captor.capture());
		Activity createdAct = captor.getValue();
		assertNotNull(createdAct);
		assertFalse(createdAct.getCreatedBy().equals(noOneUserId));
		assertFalse(createdAct.getModifiedBy().equals(noOneUserId));
	}
	
	@Test
	public void testUpdateActivity() throws Exception {
		updateTest(normalUserInfo, normalUserInfo);
	}

	@Test
	public void testUpdateActivityAdmin() throws Exception {
		updateTest(normalUserInfo, adminUserInfo);
	}

	private void updateTest(UserInfo createWith, UserInfo updateWith) throws NotFoundException,
			JSONObjectAdapterException, InterruptedException {
		String id = "123";
		String firstDesc = "firstDesc";
		String secondDesc = "secondDesc";
		Activity act = newTestActivity(id);
		act.setCreatedBy(createWith.getId().toString());
		act.setModifiedBy(createWith.getId().toString());
		act.setDescription(firstDesc);
		when(mockActivityDAO.get(anyString())).thenReturn(act);
		String originalEtag = act.getEtag();
		
		// deep copy so we modify a different object that what is returned by the DAO
		Activity actToUpdate = new Activity(act.writeToJSONObject(adapterFactory.createNew()));
		assertFalse(actToUpdate.getDescription().equals(secondDesc)); // just to be sure

		Thread.sleep(100L); // ensure that the 'modifiedOn' date is later
		long actModifiedOn = actToUpdate.getModifiedOn().getTime();
		actToUpdate.setDescription(secondDesc);
		when(mockActivityDAO.lockActivityAndGenerateEtag(id.toString(), originalEtag, ChangeType.UPDATE)).thenReturn("newETag");
		when(mockActivityDAO.update(actToUpdate)).thenReturn(actToUpdate);
		actToUpdate = activityManager.updateActivity(updateWith, actToUpdate);		
		
		verify(mockActivityDAO).lockActivityAndGenerateEtag(id.toString(), originalEtag, ChangeType.UPDATE);		
		verify(mockActivityDAO).update(actToUpdate);

		assertTrue(actToUpdate.getModifiedOn().getTime()-actModifiedOn>0);
		assertTrue(actToUpdate.getDescription().equals(secondDesc));
		assertFalse(act.getEtag().equals(actToUpdate.getEtag()));
	}

	@Test
	public void testUpdateActivityAccessDenied() throws Exception {
		String id = "123";
		Activity act = newTestActivity(id);
		act.setCreatedBy("someOtherUser");
		act.setModifiedBy("someOtherUser");
		when(mockActivityDAO.get(anyString())).thenReturn(act);
		
		assertThrows(UnauthorizedException.class, () -> {
			activityManager.updateActivity(normalUserInfo, act);
		});
	}


	@Test
	public void testUpdateActivityFakePeople() throws Exception {
		String id = "123";
		when(mockIdGenerator.generateNewId(IdType.ACTIVITY_ID)).thenReturn(new Long(id));
		
		// from database Activity
		Activity act = newTestActivity(id);
		act.setCreatedBy(normalUserInfo.getId().toString());
		act.setModifiedBy(normalUserInfo.getId().toString());
		String originalEtag = act.getEtag();
		
		// Activity to update with changed creator user id
		String noOneUserId = "noone!";
		Activity toUpdate = newTestActivity(id);		
		toUpdate.setCreatedBy(noOneUserId);
		toUpdate.setModifiedBy(noOneUserId);
		when(mockActivityDAO.get(anyString())).thenReturn(act);
		when(mockActivityDAO.lockActivityAndGenerateEtag(id.toString(), originalEtag, ChangeType.UPDATE)).thenReturn("newETag");
		when(mockActivityDAO.update(toUpdate)).thenReturn(toUpdate);

		Activity updatedActivity = activityManager.updateActivity(normalUserInfo, toUpdate); 					
		assertNull(updatedActivity.getCreatedBy());
		assertFalse(updatedActivity.getModifiedBy().equals(noOneUserId));
	}
	
	
	@Test
	public void testDeleteActivity() throws Exception { 
		String id = "123";
		Activity act = newTestActivity(id);
		act.setCreatedBy(normalUserInfo.getId().toString());
		act.setModifiedBy(normalUserInfo.getId().toString());
		when(mockActivityDAO.get(anyString())).thenReturn(act);

		activityManager.deleteActivity(normalUserInfo, id.toString());
				
		verify(mockActivityDAO).delete(id.toString());
	}
	
	@Test
	public void testDeleteActivityAdmin() throws Exception { 
		String id = "123";
		Activity act = newTestActivity(id);
		act.setCreatedBy(normalUserInfo.getId().toString());
		act.setModifiedBy(normalUserInfo.getId().toString());
		when(mockActivityDAO.get(anyString())).thenReturn(act);

		activityManager.deleteActivity(adminUserInfo, id.toString());
				
		verify(mockActivityDAO).delete(id.toString());
	}
	
	@Test
	public void testDeleteActivityAccessDenied() throws Exception { 
		String id = "123";
		Activity act = newTestActivity(id);
		act.setCreatedBy("someOtherUser");
		act.setModifiedBy("someOtherUser");
		when(mockActivityDAO.get(anyString())).thenReturn(act);

		assertThrows(UnauthorizedException.class, () -> {
			activityManager.deleteActivity(normalUserInfo, id.toString());
		});
	}

	@Test
	public void testActivityExists() {
		String id = "123";
		when(mockActivityDAO.doesActivityExist(id)).thenReturn(true);
		assertTrue(activityManager.doesActivityExist(id));
		reset(mockActivityDAO);
		when(mockActivityDAO.doesActivityExist(id)).thenReturn(false);
		assertFalse(activityManager.doesActivityExist(id));
	
	}

	@Test
	public void testPopulateCreationFields() throws Exception {
		Date beforePopulateDate = new Date();
		Thread.sleep(100L); // ensure that the 'modifiedOn' date is later
		Activity act = newTestActivity("123");
		ActivityManagerImpl.populateCreationFields(normalUserInfo, act);
		assertEquals(normalUserInfo.getId().toString(), act.getCreatedBy());
		assertEquals(normalUserInfo.getId().toString(), act.getModifiedBy());
		assertTrue(beforePopulateDate.before(act.getCreatedOn()));
		assertTrue(beforePopulateDate.before(act.getModifiedOn()));
	}
	
	@Test
	public void testPopulateModifiedFields() throws Exception {
		Date beforePopulateDate = new Date();
		Thread.sleep(100L); // ensure that the 'modifiedOn' date is later
		Activity act = newTestActivity("123");
		ActivityManagerImpl.populateModifiedFields(normalUserInfo, act);
		assertEquals(null, act.getCreatedBy());
		assertEquals(normalUserInfo.getId().toString(), act.getModifiedBy());
		assertNull(act.getCreatedOn());
		assertTrue(beforePopulateDate.before(act.getModifiedOn()));
	}
	
	@Test
	public void testGetEntitiesGeneratedBy() throws Exception {
		String id = "123";
		String firstDesc = "firstDesc";
		Activity act = newTestActivity(id);
		act.setCreatedBy(normalUserInfo.getId().toString());
		act.setModifiedBy(normalUserInfo.getId().toString());
		act.setDescription(firstDesc);
		when(mockActivityDAO.get(anyString())).thenReturn(act);
		when(mockAuthorizationManager.canAccessActivity(normalUserInfo, id)).thenReturn(AuthorizationStatus.authorized());

		activityManager.getEntitiesGeneratedBy(normalUserInfo, id, Integer.MAX_VALUE, 0);
		
		verify(mockActivityDAO).getEntitiesGeneratedBy(id, Integer.MAX_VALUE, 0);
	}
	
	@Test
	public void testGetEntitiesGeneratedByNotFound() throws Exception {
		String id = "123";
		when(mockActivityDAO.get(anyString())).thenThrow(new NotFoundException());

		assertThrows(NotFoundException.class, () -> {
			activityManager.getEntitiesGeneratedBy(normalUserInfo, id, Integer.MAX_VALUE, 0);
		});
	}

	@Test
	public void testGetEntitiesGeneratedByUnauthorized() throws Exception {
		String id = "123";
		String firstDesc = "firstDesc";
		Activity act = newTestActivity(id);
		act.setCreatedBy(normalUserInfo.getId().toString());
		act.setModifiedBy(normalUserInfo.getId().toString());
		act.setDescription(firstDesc);
		when(mockActivityDAO.get(anyString())).thenReturn(act);
		when(mockAuthorizationManager.canAccessActivity(normalUserInfo, id)).thenReturn(AuthorizationStatus.accessDenied(""));

		assertThrows(UnauthorizedException.class, () -> {
			activityManager.getEntitiesGeneratedBy(normalUserInfo, id, Integer.MAX_VALUE, 0);
		});
	}

	
	
	/*
	 * Private Methods
	 */
	private Activity newTestActivity(String id) {
		Activity act = new Activity();
		act.setId(id);
		act.setEtag("0");		
		act.setCreatedBy("555");
		act.setCreatedOn(new Date());
		act.setModifiedBy("666");
		act.setModifiedOn(new Date());
		UsedEntity usedEnt = new UsedEntity();
		Reference ref = new Reference();
		ref.setTargetId("syn123");
		ref.setTargetVersionNumber((long)1);
		usedEnt.setReference(ref);
		usedEnt.setWasExecuted(true);
		Set<Used> used = new HashSet<Used>();
		used.add(usedEnt);
		UsedURL ux = new UsedURL();
		ux.setUrl("http://url.com");
		used.add(ux);
		act.setUsed(used);
		return act;
	}

	private void configureUser(UserInfo userInfo, String userGroupId) {
		userInfo.setId(Long.parseLong(userGroupId));
		userInfo.setCreationDate(new Date());
	}
		

}
