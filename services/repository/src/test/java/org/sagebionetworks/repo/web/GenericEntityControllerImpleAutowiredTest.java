package org.sagebionetworks.repo.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Date;
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
import org.sagebionetworks.repo.model.LayerLocation;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.web.util.UserProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class GenericEntityControllerImpleAutowiredTest {
	
	@Autowired
	GenericEntityController entityController;
	
	@Autowired
	public UserProvider testUserProvider;
	
	
	List<String> toDelete = null;
	
	private int totalEntities = 10;
	private int layers = 5;
	private int locations = 2;
	
	private String userName;
	private UserInfo userInfo;
	
	HttpServletRequest mockRequest;

	
	@Before
	public void before() throws DatastoreException, InvalidModelException, NotFoundException, UnauthorizedException{
		assertNotNull(entityController);
		assertNotNull(testUserProvider);
		// Map test objects to their urls
		// Make sure we have a valid user.
		userInfo = testUserProvider.getTestAdiminUserInfo();
		UserInfo.validateUserInfo(userInfo);
		userName = userInfo.getUser().getUserId();
		mockRequest = Mockito.mock(HttpServletRequest.class);
		when(mockRequest.getServletPath()).thenReturn("/repo/v1");


		toDelete = new ArrayList<String>();
		// Create some datasetst.
		for(int i=0; i<totalEntities; i++){
			Dataset ds = createForTest(i);
			ds = entityController.createEntity(userName, ds, mockRequest);
			for(int layer=0; layer<layers; layer++){
				InputDataLayer inLayer = createLayerForTest(i*10+layer);
				inLayer.setParentId(ds.getId());
				inLayer = entityController.createEntity(userName, inLayer, mockRequest);
				for(int loc=0; loc<locations; loc++){
					LayerLocation loca = createLayerLocatoinsForTest(i*10+layer*10+loc);
					loca.setParentId(inLayer.getId());
					entityController.createEntity(userName, loca, mockRequest);
				}
			}
			toDelete.add(ds.getId());
		}
	}
	
	private Dataset createForTest(int i){
		Dataset ds = new Dataset();
		ds.setName("someName"+i);
		ds.setDescription("someDesc"+i);
		ds.setCreator("magic"+i);
		ds.setCreationDate(new Date(1001));
		ds.setAnnotations("someAnnoUrl"+1);
		ds.setHasClinicalData(false);
		ds.setHasExpressionData(true);
		ds.setHasGeneticData(true);
		ds.setLayers("someLayerUrl"+i);
		ds.setReleaseDate(new Date(15689));
		ds.setStatus("someStatus"+i);
		ds.setVersion("someVersion"+i);
		ds.setUri("someUri"+i);
		return ds;
	}
	
	private InputDataLayer createLayerForTest(int i) throws InvalidModelException{
		InputDataLayer layer = new InputDataLayer();
		layer.setName("layerName"+i);
		layer.setDescription("layerDesc"+i);
		layer.setCreationDate(new Date(1001));
		layer.setType(LayerTypeNames.G.name());
		return layer;
	}
	
	private LayerLocation createLayerLocatoinsForTest(int i) throws InvalidModelException{
		LayerLocation location = new LayerLocation();
		location.setMd5sum("someMD%"+i);
		location.setPath("a/very/long/path/"+i);
		location.setType(LayerLocation.LocationTypeNames.awsebs.name());
		return location;
	}
	
	@After
	public void after(){
		if(entityController != null && toDelete != null){
			for(String id: toDelete){
				try{
					entityController.deleteEntity(userName, id);
				}catch(Exception e){}
			}
		}
	}
	
	@Test
	public void testQuery() throws DatastoreException, NotFoundException, UnauthorizedException{
		// Basic query
		PaginatedResults<Dataset> paginated = entityController.getEntities(userName, new PaginatedParameters(1,100, null, true), mockRequest, Dataset.class);
		assertNotNull(paginated);
		assertNotNull(paginated.getPaging());
		List<Dataset> results = paginated.getResults();
		assertNotNull(results);
		assertEquals(totalEntities, results.size());
		// Check the urls for each object
		for(Dataset ds: results){
			UrlHelpers.validateAllUrls(ds);
			// Each dataset should also have a genetic layer
			assertFalse(ds.getHasClinicalData());
			assertFalse(ds.getHasExpressionData());
			assertTrue(ds.getHasGeneticData());
		}
		// Sorted
		paginated = entityController.getEntities(userName, new PaginatedParameters(1, 3, "name", true), mockRequest, Dataset.class);
		results = paginated.getResults();
		assertNotNull(results);
		assertEquals(3, results.size());
		assertNotNull(results.get(2));
		assertEquals("someName2", results.get(2).getName());
	}
	
	@Test 
	public void testGetChildrenOfType() throws DatastoreException, NotFoundException, UnauthorizedException{
		String datasetOneId = toDelete.get(0);
		List<InputDataLayer> list = entityController.getEntityChildrenOfType(userName, datasetOneId, InputDataLayer.class, mockRequest);
		assertNotNull(list);
		assertEquals(layers, list.size());
		InputDataLayer lastLayer = list.get(layers -1);
		assertNotNull(lastLayer);
		// Check the urls for each object
		for(InputDataLayer layer: list){
			// Check all of the urls
			UrlHelpers.validateAllUrls(layer);
		}
		// Now get the locations.
		List<LayerLocation> locationList = entityController.getEntityChildrenOfType(userName, lastLayer.getId(), LayerLocation.class, mockRequest);
		assertNotNull(locationList);
		assertEquals(locations, locationList.size());
	}
	
	@Test 
	public void testGetChildrenOfTypePaginated() throws DatastoreException, NotFoundException, UnauthorizedException{
		String datasetOneId = toDelete.get(0);
		PaginatedResults<InputDataLayer> resutls = entityController.getEntityChildrenOfTypePaginated(userName, datasetOneId, InputDataLayer.class, new PaginatedParameters(), mockRequest);
		assertNotNull(resutls);
		assertEquals(layers, resutls.getTotalNumberOfResults());
		List<InputDataLayer> list = resutls.getResults();
		assertNotNull(list);
		assertEquals(layers, list.size());
		InputDataLayer lastLayer = list.get(layers -1);
		assertNotNull(lastLayer);
		// Now get the locations.
		PaginatedResults<LayerLocation> locationResults = entityController.getEntityChildrenOfTypePaginated(userName, lastLayer.getId(),  LayerLocation.class, new PaginatedParameters(0, 1000, null, true), mockRequest);
		assertNotNull(locationResults);
		List<LayerLocation> locationList = locationResults.getResults();
		assertNotNull(locationList);
		assertEquals(locations, locationList.size());
		for(LayerLocation location: locationList){
			// Check all of the urls
			UrlHelpers.validateAllUrls(location);
		}
	}

}
