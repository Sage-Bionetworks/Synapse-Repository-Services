package org.sagebionetworks.repo.web;

import javax.servlet.http.HttpServletRequest;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.repo.manager.EntityManager;

public class GenericEntityControllerImplUnitTest {
	
	GenericEntityController controller;
	EntityManager mockEntityManager = null;
	HttpServletRequest mockRequest = null;
	
	@Before
	public void before(){
		mockEntityManager = Mockito.mock(EntityManager.class);
		mockRequest = Mockito.mock(HttpServletRequest.class);
		controller = new GenericEntityControllerImpl(mockEntityManager);
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
	
}
