package org.sagebionetworks.repo.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.sagebionetworks.authutil.AuthUtilConstants;
import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.Dataset;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InputDataLayer;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.LayerLocation;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.User;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.web.util.UserProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class EntitiesAccessorImplAutoWiredTest {
	
	@Autowired
	EntitiesAccessor entitiesAccessor;
	
	@Autowired
	EntityManager entityManager;
	
	@Autowired
	public UserProvider testUserProvider;
	
	private AuthorizationManager mockAuth;
	
	List<String> toDelete = null;
	
	private int totalEntities = 10;
	private int layers = 5;
	private int locations = 2;
	
	private UserInfo userInfo = null;
//	final UserInfo anonUserInfo = new UserInfo(false);
	
	@Before
	public void before() throws DatastoreException, InvalidModelException, NotFoundException, UnauthorizedException{
		assertNotNull(entitiesAccessor);
		assertNotNull(entityManager);
		assertNotNull(testUserProvider);
		userInfo = testUserProvider.getTestAdiminUserInfo();
		mockAuth = Mockito.mock(AuthorizationManager.class);
		entityManager.overrideAuthDaoForTest(mockAuth);
		entitiesAccessor.overrideAuthDaoForTest(mockAuth);
		when(mockAuth.canAccess((UserInfo)any(), anyString(), any(AuthorizationConstants.ACCESS_TYPE.class))).thenReturn(true);
		when(mockAuth.canCreate((UserInfo)any(), (Node)any())).thenReturn(true);

//		User anonUser = new User();
//		anonUser.setUserId(AuthUtilConstants.ANONYMOUS_USER_ID);
//		anonUserInfo.setUser(anonUser);
//		when(mockAuth.getUserInfo(AuthUtilConstants.ANONYMOUS_USER_ID)).thenReturn(anonUserInfo);

		toDelete = new ArrayList<String>();
		// Create some datasetst.
		for(int i=0; i<totalEntities; i++){
			Dataset ds = createForTest(i);
			String id = entityManager.createEntity(userInfo, ds);
			for(int layer=0; layer<layers; layer++){
				InputDataLayer inLayer = createLayerForTest(i*10+layer);
				inLayer.setParentId(id);
				String layerId = entityManager.createEntity(userInfo, inLayer);
				for(int loc=0; loc<locations; loc++){
					LayerLocation loca = createLayerLocatoinsForTest(i*10+layer*10+loc);
					loca.setParentId(layerId);
					entityManager.createEntity(userInfo, loca);
				}
			}
			toDelete.add(id);
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
		ds.setLayer("someLayerUrl"+i);
		ds.setReleaseDate(new Date(15689));
		ds.setStatus("someStatus"+i);
		ds.setVersion("someVersion"+i);
		ds.setUri("someUri"+i);
		return ds;
	}
	
	private InputDataLayer createLayerForTest(int i){
		InputDataLayer layer = new InputDataLayer();
		layer.setName("layerName"+i);
		layer.setDescription("layerDesc"+i);
		layer.setCreationDate(new Date(1001));
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
		if(entityManager != null && toDelete != null){
			for(String id: toDelete){
				try{
					entityManager.deleteEntity(userInfo, id);
				}catch(Exception e){}
			}
		}
	}
	
	@Test
	public void testQuery() throws DatastoreException, NotFoundException, UnauthorizedException{
		// Basic query
		PaginatedResults<Dataset> paginated = entitiesAccessor.getInRange(userInfo, 0, totalEntities, Dataset.class);
		assertNotNull(paginated);
		List<Dataset> results = paginated.getResults();
		assertNotNull(results);
		assertEquals(totalEntities, results.size());
		// Sorted
		paginated = entitiesAccessor.getInRangeSortedBy(userInfo, 0, 3, "name", true, Dataset.class);
		results = paginated.getResults();
		assertNotNull(results);
		assertEquals(3, results.size());
		assertNotNull(results.get(2));
		assertEquals("someName2", results.get(2).getName());
	}
	
	@Test 
	public void testGetChildrenOfType() throws DatastoreException, NotFoundException, UnauthorizedException{
		String datasetOneId = toDelete.get(0);
		List<InputDataLayer> list = entitiesAccessor.getChildrenOfType(userInfo, datasetOneId, InputDataLayer.class);
		assertNotNull(list);
		assertEquals(layers, list.size());
		InputDataLayer lastLayer = list.get(layers -1);
		assertNotNull(lastLayer);
		// Now get the locations.
		List<LayerLocation> locationList = entitiesAccessor.getChildrenOfType(userInfo, lastLayer.getId(), LayerLocation.class);
		assertNotNull(locationList);
		assertEquals(locations, locationList.size());
	}

}
