package org.sagebionetworks.repo.web;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.sagebionetworks.repo.manager.ActivityManager;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.Reference;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.provenance.Activity;
import org.sagebionetworks.repo.model.provenance.Used;
import org.sagebionetworks.repo.model.provenance.UsedEntity;
import org.sagebionetworks.repo.model.provenance.UsedURL;
import org.sagebionetworks.repo.web.service.ActivityService;
import org.sagebionetworks.repo.web.service.ActivityServiceImpl;

public class ActivityServiceImplUnitTest {

	ActivityService activityService;
	ActivityManager mockActivityManager;
	EntityManager mockEntityManager;
	UserManager mockUserManager;
	
	UserInfo userInfo;
	Long userId = 84739L;
	Activity activity;
	Set<Used> used;
	String n1 = "syn123";
	String n2 = "syn456";
	String n3 = "syn789";
	String usedUrl = "http://url.com";
	
	
	@Before
	public void setup() throws Exception {
		mockActivityManager = mock(ActivityManager.class);
		mockEntityManager = mock(EntityManager.class);
		mockUserManager = mock(UserManager.class);
		activityService = new ActivityServiceImpl(mockActivityManager, mockEntityManager, mockUserManager);
		
		userInfo = new UserInfo(false);
		when(mockUserManager.getUserInfo(userId)).thenReturn(userInfo);
		
		activity = new Activity();
		activity.setName("name");
		used = new HashSet<Used>();
		UsedEntity ue;
		Reference ref;

		// used 1
		ue = new UsedEntity();
		ref = new Reference();
		ref.setTargetId(n1);
		ue.setReference(ref);
		used.add(ue);

		// used 2
		ue = new UsedEntity();
		ref = new Reference();
		ref.setTargetId(n2);
		ref.setTargetVersionNumber(2L);
		ue.setReference(ref);
		used.add(ue);
		
		// used 3
		ue = new UsedEntity();
		ref = new Reference();
		ref.setTargetId(n3);
		ue.setReference(ref);
		used.add(ue);		
		
		UsedURL ux = new UsedURL();
		ux.setUrl(usedUrl);
		used.add(ux);
		
		activity.setUsed(used);			
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testCreateActivity() throws Exception {		
		Long n1v = 2L;
		Long n3v = 1L;
		List<Reference> refs = createExpectedRefs(n1v, n3v);
		
		String activityId = "123";
		when(mockActivityManager.createActivity(userInfo, activity)).thenReturn(activityId);
		when(mockActivityManager.getActivity(userInfo, activityId)).thenReturn(activity);
		when(mockEntityManager.getCurrentRevisionNumbers(anyList())).thenReturn(refs);
		Activity act = activityService.createActivity(userId, activity);
		
		verifyFilledInVersions(n1v, n3v, act);
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testUpdateActivity() throws Exception {		
		Long n1v = 2L;
		Long n3v = 1L;
		List<Reference> refs = createExpectedRefs(n1v, n3v);			
		
		when(mockActivityManager.updateActivity(userInfo, activity)).thenReturn(activity);
		when(mockEntityManager.getCurrentRevisionNumbers(anyList())).thenReturn(refs);

		Activity act = activityService.updateActivity(userId, activity);
		
		verifyFilledInVersions(n1v, n3v, act);
	}

	@Test(expected=InvalidModelException.class)
	public void testMalformedUrl() throws Exception {
		UsedURL ux = new UsedURL();
		ux.setUrl("/usr/bin/bash");
		used.add(ux);
		
		Long n1v = 2L;
		Long n3v = 1L;
		List<Reference> refs = createExpectedRefs(n1v, n3v);
		
		String activityId = "123";
		when(mockActivityManager.createActivity(userInfo, activity)).thenReturn(activityId);
		when(mockActivityManager.getActivity(userInfo, activityId)).thenReturn(activity);
		when(mockEntityManager.getCurrentRevisionNumbers(anyList())).thenReturn(refs);
		Activity act = activityService.createActivity(userId, activity);		
		fail();
	}
	
	/*
	 * Private Methods
	 */
	private void verifyFilledInVersions(Long n1v, Long n3v, Activity act) {
		ArgumentCaptor<List> arg = ArgumentCaptor.forClass(List.class);
		verify(mockEntityManager).getCurrentRevisionNumbers(arg.capture());
		List<String> entityIds = arg.getValue();
		assertTrue(entityIds.contains(n1));
		assertTrue(entityIds.contains(n3));
		
		UsedEntity ue1 = null;
		UsedEntity ue2 = null;
		UsedEntity ue3 = null;
		UsedURL uUrl = null;
		for(Used used : act.getUsed()) {
			if(used instanceof UsedEntity) {
				UsedEntity ue = (UsedEntity) used;
				if(ue.getReference().getTargetId().equals(n1)) ue1 = ue;
				else if(ue.getReference().getTargetId().equals(n2)) ue2 = ue;
				else if(ue.getReference().getTargetId().equals(n3)) ue3 = ue;				
			} else if(used instanceof UsedURL) {
				UsedURL uu = (UsedURL) used;
				if(uu.getUrl().equals(usedUrl)) uUrl = uu;
				
			}
		}
		assertNotNull(ue2);
		assertNotNull(uUrl);
		
		assertTrue(n1v == ue1.getReference().getTargetVersionNumber());
		assertTrue(n3v == ue3.getReference().getTargetVersionNumber());
	}

	private List<Reference> createExpectedRefs(Long n1v, Long n3v) {
		List<Reference> refs = new ArrayList<Reference>();
		Reference ref;
		
		ref = new Reference();
		ref.setTargetId(n1);
		ref.setTargetVersionNumber(n1v);
		refs.add(ref);

		ref = new Reference();
		ref.setTargetId(n3);
		ref.setTargetVersionNumber(n3v);
		refs.add(ref);
		return refs;
	}
	
}








