package org.sagebionetworks.repo.web;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.model.LayerLocation;
import org.sagebionetworks.repo.model.UserInfo;

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
	
	@Test
	public void testAggregateUpdate() throws Exception{
		List<String> idList = new ArrayList<String>();
		idList.add("201");
//		idList.add("301");
//		idList.add("401");
		String userId = "someUser";
		String parentId = "0";
		Collection<LayerLocation> toUpdate = new ArrayList<LayerLocation>();
		when(mockEntityManager.aggregateEntityUpdate((UserInfo)any(),eq(parentId), eq(toUpdate))).thenReturn(idList);
		LayerLocation existingLocation = new LayerLocation();
		existingLocation.setId("201");
		existingLocation.setMd5sum("someMD5");
		existingLocation.setPath("somePath");
		when(mockEntityManager.getEntity((UserInfo)any(), eq("201"), eq(LayerLocation.class))).thenReturn(existingLocation);
		// Now make the call
		controller.aggregateEntityUpdate(userId, parentId, toUpdate, mockRequest);
	}

}
