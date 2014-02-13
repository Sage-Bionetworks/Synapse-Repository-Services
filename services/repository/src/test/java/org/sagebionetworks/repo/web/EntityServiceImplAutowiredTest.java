package org.sagebionetworks.repo.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.AsynchronousDAO;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.Data;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.LayerTypeNames;
import org.sagebionetworks.repo.model.LocationData;
import org.sagebionetworks.repo.model.LocationTypeNames;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.Reference;
import org.sagebionetworks.repo.model.Step;
import org.sagebionetworks.repo.model.Study;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.web.service.EntityService;
import org.sagebionetworks.repo.web.service.TrashService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
@Deprecated // This test builds up old objects types in the before methods so it will be deleted when the old object types are removed.
public class EntityServiceImplAutowiredTest {
	
	@Autowired
	private EntityService entityController;
	
	@Autowired
	private TrashService trashService;
	
	@Autowired
	public UserManager userManager;
	
	@Autowired
	private AsynchronousDAO asynchronousDAO;
	
	private List<String> toDelete = null;
	
	private int totalEntities = 10;
	private int layers = 5;
	private int locations = 2;
	
	private Long adminUserId;
	private UserInfo adminUserInfo;
	private String activityId;
	
	HttpServletRequest mockRequest;

	@Before
	public void before() throws Exception {
		// Map test objects to their urls
		// Make sure we have a valid user.
		adminUserId = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();
		adminUserInfo = userManager.getUserInfo(adminUserId);
		UserInfo.validateUserInfo(adminUserInfo);
		
		mockRequest = Mockito.mock(HttpServletRequest.class);
		activityId = null;
		when(mockRequest.getServletPath()).thenReturn("/repo/v1");
		when(mockRequest.getParameter(AuthorizationConstants.USER_ID_PARAM)).thenReturn(adminUserId.toString());
		
		toDelete = new ArrayList<String>();
		// Create a project to hold the datasets
		Project project = new Project();
		project.setName("projectRoot");
		project = entityController.createEntity(adminUserId, project, activityId, mockRequest);
		assertNotNull(project);

		
		// Create some datasetst.
		for(int i=0; i<totalEntities; i++){
			Study ds = createForTest(i);
			ds.setParentId(project.getId());
			ds = entityController.createEntity(adminUserId, ds, activityId, mockRequest);
			for(int layer=0; layer<layers; layer++){
				Data inLayer = createLayerForTest(i*10+layer);
				inLayer.setParentId(ds.getId());
				inLayer.setMd5("b960413cf33e1333b2b709319c29870d");
				List<LocationData> locationDatas = new ArrayList<LocationData>();
				inLayer.setLocations(locationDatas);
				for(int loc=0; loc<locations; loc++){
					LocationData loca = createLayerLocatoinsForTest(i*10+layer*10+loc);
					locationDatas.add(loca);
				}
				inLayer = entityController.createEntity(adminUserId, inLayer, activityId, mockRequest);
			}
			toDelete.add(ds.getId());
		}
		toDelete.add(project.getId());
	}
	
	private Study createForTest(int i){
		Study ds = new Study();
		ds.setName("someName"+i);
		ds.setDescription("someDesc"+i);
		ds.setCreatedBy("magic"+i);
		ds.setCreatedOn(new Date(1001));
		ds.setAnnotations("someAnnoUrl"+1);
		ds.setUri("someUri"+i);
		return ds;
	}
	
	private Data createLayerForTest(int i) throws InvalidModelException{
		Data layer = new Data();
		layer.setName("layerName"+i);
		layer.setDescription("layerDesc"+i);
		layer.setCreatedOn(new Date(1001));
		layer.setType(LayerTypeNames.G);
		return layer;
	}
	
	private LocationData createLayerLocatoinsForTest(int i) throws InvalidModelException{
		LocationData locationData = new LocationData();
		locationData.setPath("a/very/long/path/"+i);
		locationData.setType(LocationTypeNames.awsebs);
		return locationData;
	}
	
	@After
	public void after() throws Exception {
		for (String id: toDelete) {
			try {
				entityController.deleteEntity(adminUserId, id);
			} catch (Exception e) {}
		}
		trashService.purgeTrashForUser(adminUserId);
	}
	
	@Test
	public void testQuery() throws DatastoreException, NotFoundException, UnauthorizedException{
		// Basic query
		PaginatedResults<Study> paginated = entityController.getEntities(adminUserId, new PaginatedParameters(1,100, null, true), mockRequest, Study.class);
		assertNotNull(paginated);
		assertNotNull(paginated.getPaging());
		List<Study> results = paginated.getResults();
		assertNotNull(results);
		assertEquals(totalEntities, results.size());
		// Check the urls for each object
		for(Study ds: results){
			UrlHelpers.validateAllUrls(ds);
		}
		// Sorted
		paginated = entityController.getEntities(adminUserId, new PaginatedParameters(1, 3, "name", true), mockRequest, Study.class);
		results = paginated.getResults();
		assertNotNull(results);
		assertEquals(3, results.size());
		assertNotNull(results.get(2));
		assertEquals("someName2", results.get(2).getName());
	}
	
	@Test 
	public void testGetChildrenOfType() throws DatastoreException, NotFoundException, UnauthorizedException{
		String datasetOneId = toDelete.get(0);
		List<Data> list = entityController.getEntityChildrenOfType(adminUserId, datasetOneId, Data.class, mockRequest);
		assertNotNull(list);
		assertEquals(layers, list.size());
		Data lastLayer = list.get(layers -1);
		assertNotNull(lastLayer);
		// Check the urls for each object
		for(Data layer: list){
			// Check all of the urls
			UrlHelpers.validateAllUrls(layer);
			// Now get the locations.
			assertNotNull(layer.getLocations());
			assertEquals(locations, layer.getLocations().size());
		}
	}
	
	@Test 
	public void testGetChildrenOfTypePaginated() throws DatastoreException, NotFoundException, UnauthorizedException{
		String datasetOneId = toDelete.get(0);
		PaginatedResults<Data> resutls = entityController.getEntityChildrenOfTypePaginated(adminUserId, datasetOneId, Data.class, new PaginatedParameters(), mockRequest);
		assertNotNull(resutls);
		assertEquals(layers, resutls.getTotalNumberOfResults());
		List<Data> list = resutls.getResults();
		assertNotNull(list);
		assertEquals(layers, list.size());
		Data lastLayer = list.get(layers -1);
		assertNotNull(lastLayer);
		// Now get the locations.
		assertNotNull(lastLayer.getLocations());
		assertEquals(locations, lastLayer.getLocations().size());
	}
	
	@Test
	public void testGetReferences() throws Exception {
		// get an entity
		String id1 = toDelete.get(0);		
		// verify that nothing refers to it
		PaginatedResults<EntityHeader> ehs = entityController.getEntityReferences(adminUserId, id1, null, null, null, mockRequest);
		assertEquals(0, ehs.getTotalNumberOfResults());
		// make another entity refer to the first one
		Step step = new Step();
		Reference ref = new Reference();
		ref.setTargetId(id1);
		Set<Reference> refs = new HashSet<Reference>();
		refs.add(ref);
		step.setInput(refs);
		step = entityController.createEntity(adminUserId, step, null, mockRequest);
		toDelete.add(step.getId());
		// Manually update
		updateAnnotationsAndReferences();
		// verify that the Step can be retrieved via its reference
		ehs = entityController.getEntityReferences(adminUserId, id1, null, null, null, mockRequest);
		assertEquals(1, ehs.getTotalNumberOfResults());
		assertEquals(step.getId(), ehs.getResults().iterator().next().getId());
	}

	/**
	 * Since we have moved the annotation updates to an asynchronous process we need to manually
	 * update the annotations of all nodes for this test. See PLFM-1548
	 * 
	 * @throws NotFoundException
	 */
	public void updateAnnotationsAndReferences() throws NotFoundException {
		for(String id: toDelete){
			asynchronousDAO.createEntity(id);
		}
	}

}
