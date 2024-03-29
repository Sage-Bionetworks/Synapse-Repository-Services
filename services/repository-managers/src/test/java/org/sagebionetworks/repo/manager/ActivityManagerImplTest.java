package org.sagebionetworks.repo.manager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.manager.entity.EntityAuthorizationManager;
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

@ExtendWith(MockitoExtension.class)
public class ActivityManagerImplTest {
	
	@Mock
	IdGenerator mockIdGenerator;
	@Mock
	ActivityDAO mockActivityDAO;
	@Mock
	EntityAuthorizationManager mockAuthorizationManager;
	
	@Spy
	@InjectMocks
	ActivityManagerImpl activityManager;
	
	UserInfo normalUserInfo;
	UserInfo adminUserInfo;
	
	AdapterFactory adapterFactory = new AdapterFactoryImpl();
	

	@BeforeEach
	public void before() throws Exception{
		normalUserInfo = new UserInfo(false);
		adminUserInfo = new UserInfo(true);
		configureUser(adminUserInfo, "1");
		configureUser(normalUserInfo, "2");
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
		doReturn(AuthorizationStatus.authorized()).when(activityManager).canAccessActivity(any(), any());

		activityManager.getEntitiesGeneratedBy(normalUserInfo, id, Integer.MAX_VALUE, 0);
		
		verify(mockActivityDAO).getEntitiesGeneratedBy(id, Integer.MAX_VALUE, 0);
	}
	
	@Test
	public void testGetEntitiesGeneratedByNotFound() throws Exception {
		String id = "123";
		when(mockActivityDAO.get(anyString())).thenThrow(new NotFoundException(""));

		assertThrows(NotFoundException.class, () -> {
			activityManager.getEntitiesGeneratedBy(normalUserInfo, id, Integer.MAX_VALUE, 0);
		});
	}
	
	private PaginatedResults<Reference> generateQueryResults(int numResults, int total) {
		PaginatedResults<Reference> results = new PaginatedResults<Reference>();
		List<Reference> resultList = new ArrayList<Reference>();		
		for(int i=0; i<numResults; i++) {
			Reference ref = new Reference();
			ref.setTargetId("nodeId");
			resultList.add(ref);
		}
		results.setResults(resultList);
		results.setTotalNumberOfResults(total);
		return results;
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
		doReturn(AuthorizationStatus.accessDenied("")).when(activityManager).canAccessActivity(any(), any());

		assertThrows(UnauthorizedException.class, () -> {
			activityManager.getEntitiesGeneratedBy(normalUserInfo, id, Integer.MAX_VALUE, 0);
		});
	}
	
	@Test
	public void testCanAccessActivityPagination() throws Exception {		 
		Activity act = new Activity();
		String actId = "1";
		int limit = 1000;
		int total = 2001;
		int offset = 0;
		// create as admin, try to access as user so fails access and tests pagination
		act.setId(actId);
		act.setCreatedBy(adminUserInfo.getId().toString());
		when(mockActivityDAO.get(actId)).thenReturn(act);
		PaginatedResults<Reference> results1 = generateQueryResults(limit, total);
		PaginatedResults<Reference> results2 = generateQueryResults(total-limit, total);		
		PaginatedResults<Reference> results3 = generateQueryResults(total-(2*limit), total);
		when(mockActivityDAO.getEntitiesGeneratedBy(actId, limit, offset)).thenReturn(results1);
		when(mockActivityDAO.getEntitiesGeneratedBy(actId, limit, offset+limit)).thenReturn(results2);		
		when(mockActivityDAO.getEntitiesGeneratedBy(actId, limit, offset+(2*limit))).thenReturn(results3);

		boolean canAccess = activityManager.canAccessActivity(normalUserInfo, actId).isAuthorized();
		verify(mockActivityDAO).getEntitiesGeneratedBy(actId, limit, offset);
		verify(mockActivityDAO).getEntitiesGeneratedBy(actId, limit, offset+limit);
		verify(mockActivityDAO).getEntitiesGeneratedBy(actId, limit, offset+(2*limit));
		assertFalse(canAccess);
	}

	@Test
	public void testCanAccessActivityPaginationSmallResultSet() throws Exception {		 
		Activity act = new Activity();
		String actId = "1";
		int limit = 1000;
		int offset = 0;
		// create as admin, try to access as user so fails access and tests pagination
		act.setId(actId);
		act.setCreatedBy(adminUserInfo.getId().toString());
		when(mockActivityDAO.get(actId)).thenReturn(act);
		PaginatedResults<Reference> results1 = generateQueryResults(1, 1);		
		when(mockActivityDAO.getEntitiesGeneratedBy(actId, limit, offset)).thenReturn(results1);		

		boolean canAccess = activityManager.canAccessActivity(normalUserInfo, actId).isAuthorized();
		verify(mockActivityDAO).getEntitiesGeneratedBy(actId, limit, offset);
		assertFalse(canAccess);
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
