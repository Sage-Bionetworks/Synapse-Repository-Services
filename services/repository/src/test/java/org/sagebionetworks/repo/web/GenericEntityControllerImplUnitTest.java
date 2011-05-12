package org.sagebionetworks.repo.web;

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

public class GenericEntityControllerImplUnitTest {
	
	GenericEntityController controller;
	EntitiesAccessor mockEntityAccessor = null;
	EntityManager mockEntityManager = null;
	HttpServletRequest mockRequest = null;
	
	@Before
	public void before(){
		mockEntityAccessor = Mockito.mock(EntitiesAccessor.class);
		mockEntityManager = Mockito.mock(EntityManager.class);
		mockRequest = Mockito.mock(HttpServletRequest.class);
		controller = new GenericEntityControllerImpl(mockEntityAccessor, mockEntityManager);
	}
	
	@Test
	public void testAggregateUpdtae() throws Exception{
		List<String> idList = new ArrayList<String>();
		idList.add("201");
//		idList.add("301");
//		idList.add("401");
		String userId = "someUser";
		String parentId = "0";
		Collection<LayerLocation> toUpdate = new ArrayList<LayerLocation>();
		when(mockEntityManager.aggregateEntityUpdate(userId, parentId, toUpdate)).thenReturn(idList);
		LayerLocation existingLocation = new LayerLocation();
		existingLocation.setId("201");
		existingLocation.setMd5sum("someMD5");
		existingLocation.setPath("somePath");
		when(mockEntityManager.getEntity(userId, "201", LayerLocation.class)).thenReturn(existingLocation);
		// Now make the call
		controller.aggregateEntityUpdate(userId, parentId, toUpdate, mockRequest);
	}

}
