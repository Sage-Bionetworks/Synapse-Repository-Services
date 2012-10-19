package org.sagebionetworks.repo.manager;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.repo.model.ActivityDAO;
import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.Reference;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.User;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.provenance.Activity;
import org.sagebionetworks.repo.model.provenance.ActivityType;
import org.sagebionetworks.repo.web.util.UserProvider;
import org.sagebionetworks.schema.adapter.AdapterFactory;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.org.json.AdapterFactoryImpl;
import org.springframework.beans.factory.annotation.Autowired;

public class ActivityManagerImplTest {
	
	IdGenerator mockIdGenerator;
	ActivityDAO mockActivityDAO;
	AuthorizationManager mockAuthorizationManager;	
	ActivityManager activityManager;
	UserInfo adminUserInfo;
	
	AdapterFactory adapterFactory = new AdapterFactoryImpl();
	

	@Before
	public void before() throws Exception{
		mockIdGenerator = mock(IdGenerator.class);
		mockActivityDAO = mock(ActivityDAO.class);
		mockAuthorizationManager = mock(AuthorizationManager.class);
		adminUserInfo = new UserInfo(true);
		configureAdminUser();
		 
		activityManager = new ActivityManagerImpl(mockIdGenerator, mockActivityDAO, mockAuthorizationManager);
	}

	@Test
	public void testCreateActivity() throws Exception {
		Long id = new Long(123);
		when(mockIdGenerator.generateNewId()).thenReturn(id);
		
		activityManager.createActivity(adminUserInfo, new Activity());
		
		ArgumentCaptor<Activity> captor = ArgumentCaptor.forClass(Activity.class);
		verify(mockActivityDAO).create(captor.capture());
		Activity createdAct = captor.getValue();
		assertNotNull(createdAct);
		assertEquals(id, createdAct.getId());
		assertNotNull(createdAct.getCreatedBy());
		assertNotNull(createdAct.getCreatedOn());
		assertNotNull(createdAct.getModifiedBy());
		assertNotNull(createdAct.getModifiedOn());
		assertEquals(ActivityType.UNDEFINED, createdAct.getActivityType());
	}
	
	@Test
	public void testUpdateActivity() throws Exception {
		String id = "123";
		Activity act = newTestActivity(id);
		act.setCreatedBy(adminUserInfo.getUser().getId());
		act.setModifiedBy(adminUserInfo.getUser().getId());
		when(mockActivityDAO.get(anyString())).thenReturn(act);
		
		// deep copy so we modify a different object that what is returned by the DAO
		Activity actToUpdate = new Activity(act.writeToJSONObject(adapterFactory.createNew()));
		assertTrue(actToUpdate.getActivityType() != ActivityType.MANUAL); // just to be sure

		Thread.sleep(100L); // ensure that the 'modifiedOn' date is later
		long actModifiedOn = actToUpdate.getModifiedOn().getTime();
		actToUpdate.setActivityType(ActivityType.MANUAL);
		activityManager.updateActivity(adminUserInfo, actToUpdate);
		
		verify(mockActivityDAO).lockActivityAndIncrementEtag(id.toString(), actToUpdate.getEtag(), ChangeType.UPDATE);
		ArgumentCaptor<Activity> captor = ArgumentCaptor.forClass(Activity.class);
		verify(mockActivityDAO).update(captor.capture());
		Activity actUpdated = captor.getValue();

		assertTrue(actUpdated.getModifiedOn().getTime()-actModifiedOn>0);
		assertTrue(actUpdated.getActivityType() == ActivityType.MANUAL);
	}
	
	@Test(expected=UnauthorizedException.class)
	public void testUpdateActivityAccessDenied() throws Exception {
		String id = "123";
		Activity act = newTestActivity(id);
		act.setCreatedBy("someOtherUser");
		act.setModifiedBy("someOtherUser");
		when(mockActivityDAO.get(anyString())).thenReturn(act);
		
		activityManager.updateActivity(adminUserInfo, act);
		fail("Should throw UnathorizedException due to invalid user for update");
	}
	
	@Test
	public void testDeleteActivity() throws Exception { 
		String id = "123";
		Activity act = newTestActivity(id);
		act.setCreatedBy(adminUserInfo.getUser().getId());
		act.setModifiedBy(adminUserInfo.getUser().getId());
		when(mockActivityDAO.get(anyString())).thenReturn(act);

		activityManager.deleteActivity(adminUserInfo, id.toString());
		
		verify(mockActivityDAO).lockActivityAndIncrementEtag(id.toString(), act.getEtag(), ChangeType.DELETE);		
		verify(mockActivityDAO).delete(id.toString());
	}
	
	@Test(expected=UnauthorizedException.class)
	public void testDeleteActivityAccessDenied() throws Exception { 
		String id = "123";
		Activity act = newTestActivity(id);
		act.setCreatedBy("someOtherUser");
		act.setModifiedBy("someOtherUser");
		when(mockActivityDAO.get(anyString())).thenReturn(act);

		activityManager.deleteActivity(adminUserInfo, id.toString());
		verify(mockActivityDAO).delete(id.toString());
		fail("Should throw UnathorizedException due to invalid user for delete");
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
		act.setActivityType(ActivityType.CODE_EXECUTION);
		Reference ref = new Reference();
		ref.setTargetId("syn123");
		ref.setTargetVersionNumber((long)1);
		Set<Reference> used = new HashSet<Reference>();
		used.add(ref);
		act.setUsed(used);
		Reference executedEntity = new Reference();
		executedEntity.setTargetId("syn456");
		executedEntity.setTargetVersionNumber((long)1);
		act.setExecutedEntity(executedEntity);
		return act;
	}

	private void configureAdminUser() {
		UserGroup userGroup = new UserGroup();
		userGroup.setId("1");
		userGroup.setIsIndividual(true);
		userGroup.setName("Admin@sagebase.org");
		userGroup.setCreationDate(new Date());
		adminUserInfo.setIndividualGroup(userGroup);
		User user = new User();
		user.setId("userId");
		user.setUserId("userId");
		user.setAgreesToTermsOfUse(true);
		user.setEtag("0");
		user.setDisplayName("Admin User");
		user.setFname("Admin");
		user.setLname("User");
		user.setCreationDate(new Date());
		adminUserInfo.setUser(user);
	}
		

}
