package org.sagebionetworks.repo.web.controller.metadata;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.sagebionetworks.repo.model.Dataset;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InputDataLayer;
import org.sagebionetworks.repo.model.InputDataLayer.LayerTypeNames;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.web.GenericEntityController;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.controller.ObjectTypeFactory;
import org.sagebionetworks.repo.web.util.UserProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * 
 * @author jmhill
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class LayerTypeCountCacheImplTest {
	
	@Autowired
	LayerTypeCountCache layerTypeCountCache;
	
	// Used for cleanup
	@Autowired
	GenericEntityController entityController;
	
	@Autowired
	public UserProvider testUserProvider;

	private String userId;
	private UserInfo testUser;
	HttpServletRequest mockRequest;

	private List<String> toDelete;

	@Before
	public void before() throws DatastoreException, NotFoundException {
		assertNotNull(entityController);
		toDelete = new ArrayList<String>();
		// Map test objects to their urls
		// Make sure we have a valid user.
		testUser = testUserProvider.getTestAdiminUserInfo();
		UserInfo.validateUserInfo(testUser);
		userId = testUser.getUser().getUserId();
		// Create the mock request
		mockRequest = Mockito.mock(HttpServletRequest.class);
		when(mockRequest.getServletPath()).thenReturn("/repo/v1");
		when(mockRequest.getRequestURI()).thenReturn("/dataset");
	}

	@After
	public void after() throws UnauthorizedException {
		if (entityController != null && toDelete != null) {
			for (String idToDelete : toDelete) {
				try {
					entityController.deleteEntity(userId, idToDelete);
				} catch (NotFoundException e) {
					// nothing to do here
				} catch (DatastoreException e) {
					// nothing to do here.
				}
			}
		}
	}
	
	@Test
	public void testCacheWarmup() throws Exception{
		// The cache should start off empty
		int expectedCount =0;
		assertEquals(0, layerTypeCountCache.getCacheSize());
		// Create a datset
		Dataset ds = (Dataset) ObjectTypeFactory.createObjectForTest("DatasetOne", ObjectType.dataset, null);
		ds = entityController.createEntity(userId, ds, mockRequest);
		assertNotNull(ds);
		assertNotNull(ds.getId());
		toDelete.add(ds.getId());
		// The cache haven an entry for each layer type * 1
		expectedCount = LayerTypeNames.values().length * 1;
		assertEquals(expectedCount, layerTypeCountCache.getCacheSize());
		// Now add a layer to the datsaet
		InputDataLayer layer = (InputDataLayer) ObjectTypeFactory.createObjectForTest("layerOne", ObjectType.layer, ds.getId());
		layer.setType(LayerTypeNames.G.name());
		layer = entityController.createEntity(userId, layer, mockRequest);
		assertNotNull(layer);
		toDelete.add(layer.getId());
		// Adding a layer should have cleared one entry from the cache.
		expectedCount--;
		assertEquals(expectedCount, layerTypeCountCache.getCacheSize());
		assertEquals(1, layerTypeCountCache.getCountFor(ds.getId(), LayerTypeNames.G, testUser));
		// Checking it should put it back in
		// Adding a layer should have cleared one entry from the cache.
		expectedCount++;
		assertEquals(expectedCount, layerTypeCountCache.getCacheSize());
		// Create another layer
		layer = (InputDataLayer) ObjectTypeFactory.createObjectForTest("layerOne", ObjectType.layer, ds.getId());
		layer.setType(LayerTypeNames.G.name());
		layer = entityController.createEntity(userId, layer, mockRequest);
		assertNotNull(layer);
		toDelete.add(layer.getId());
		// The cache size should be one less
		expectedCount--;
		assertEquals(expectedCount, layerTypeCountCache.getCacheSize());
		assertEquals(2, layerTypeCountCache.getCountFor(ds.getId(), LayerTypeNames.G, testUser));
		// This should add a value back
		expectedCount++;
		assertEquals(expectedCount, layerTypeCountCache.getCacheSize());
		
		// Now add another dataset without any layers
		ds = (Dataset) ObjectTypeFactory.createObjectForTest("DatasetTwo", ObjectType.dataset, null);
		ds = entityController.createEntity(userId, ds, mockRequest);
		assertNotNull(ds);
		assertNotNull(ds.getId());
		toDelete.add(ds.getId());
		// Now we have two datasets
		expectedCount = LayerTypeNames.values().length * 2;
		assertEquals(expectedCount, layerTypeCountCache.getCacheSize());
		
		// Finally, clear the cache and make sure the warm-up works to repopulate it.
		layerTypeCountCache.clearAll();
		expectedCount = 0;
		assertEquals(expectedCount, layerTypeCountCache.getCacheSize());
		// Now warm it up
		layerTypeCountCache.afterPropertiesSet();
		expectedCount = LayerTypeNames.values().length * 2;
		assertEquals("Failed to warm-up an empty cache with two existing datasets", expectedCount, layerTypeCountCache.getCacheSize());
	}

}
