package org.sagebionetworks.repo.web;

import javax.servlet.http.HttpServletRequest;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import org.mockito.Mockito;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.web.service.EntityService;
import org.sagebionetworks.repo.web.service.EntityServiceImpl;

import org.sagebionetworks.repo.model.User;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.manager.UserManager;

public class EntityServiceImplUnitTest {
	
	EntityService controller;
	EntityManager mockEntityManager = null;
	HttpServletRequest mockRequest = null;
	UserManager mockUserManager;
	User u;
	UserInfo userInfo;
	String userId;
	
	@Before
	public void before(){
		mockUserManager = Mockito.mock(UserManager.class);
		mockEntityManager = Mockito.mock(EntityManager.class);
		mockRequest = Mockito.mock(HttpServletRequest.class);
		controller = new EntityServiceImpl(mockUserManager, mockEntityManager);
		userId = "userId";
		u = new User();
		u.setUserId(userId);
		u.setLname("userLastName");
		userInfo = new UserInfo(true);
		userInfo.setUser(u);		
	}
	
	//TODO can this test be deleted or should it be replaced with an equivalent test?
	@Ignore
	@Test
	public void testAggregateUpdate() throws Exception{
	/*  
		List<String> idList = new ArrayList<String>();
		idList.add("201");
//		idList.add("301");
//		idList.add("401");
		String userId = "someUser";
		String parentId = "0";
		Collection<Location> toUpdate = new ArrayList<Location>();
		when(mockEntityManager.aggregateEntityUpdate((UserInfo)any(),eq(parentId), eq(toUpdate))).thenReturn(idList);
		Location existingLocation = new Location();
		existingLocation.setId("201");
		existingLocation.setMd5sum("9ca4d9623b655ba970e7b8173066b58f");
		existingLocation.setPath("somePath");
		when(mockEntityManager.getEntity((UserInfo)any(), eq("201"), eq(Location.class))).thenReturn(existingLocation);
		// Now make the call
		controller.aggregateEntityUpdate(userId, parentId, toUpdate, mockRequest);
    */
	}

	@Test(expected=IllegalArgumentException.class)
	public void testChangeEntityTypeNullUserId() throws Exception {
		controller.changeEntityType(null, "syn01", "newType");
	}
	
	@Test
	public void testChangeEntityType() throws Exception {
		when(mockUserManager.getUserInfo(userId)).thenReturn(userInfo);
		controller.changeEntityType(userId, "syn01", "newType");
		verify(mockEntityManager).changeEntityType(userInfo, "syn01", "newType");
	}	
}
