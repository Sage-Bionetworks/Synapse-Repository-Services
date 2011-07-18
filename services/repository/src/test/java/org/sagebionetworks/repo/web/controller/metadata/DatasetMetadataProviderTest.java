package org.sagebionetworks.repo.web.controller.metadata;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.sagebionetworks.repo.manager.TestUserDAO;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.Dataset;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InputDataLayer;
import org.sagebionetworks.repo.model.InputDataLayer.LayerTypeNames;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.NodeConstants;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.query.BasicQuery;
import org.sagebionetworks.repo.model.query.Compartor;
import org.sagebionetworks.repo.model.query.Expression;
import org.sagebionetworks.repo.web.GenericEntityController;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.controller.ServletTestHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class DatasetMetadataProviderTest {
	
	// Used for cleanup
	@Autowired
	GenericEntityController entityController;
	
	@Autowired
	UserManager userManager;
	
	@Autowired
	DatasetMetadataProvider datasetMetadataProvider;

	static private Log log = LogFactory.getLog(DatasetMetadataProviderTest.class);

	private String userName = TestUserDAO.ADMIN_USER_NAME;
	private UserInfo testUser;
	HttpServletRequest mockRequest;

	private List<String> toDelete;

	@Before
	public void before() throws DatastoreException, NotFoundException {
		assertNotNull(entityController);
		assertNotNull(datasetMetadataProvider);
		toDelete = new ArrayList<String>();
		// Map test objects to their urls
		// Make sure we have a valid user.
		testUser = userManager.getUserInfo(userName);
		UserInfo.validateUserInfo(testUser);
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
					entityController.deleteEntity(userName, idToDelete);
				} catch (NotFoundException e) {
					// nothing to do here
				} catch (DatastoreException e) {
					// nothing to do here.
				}
			}
		}
	}
	
	
	@Test
	public void testValidate(){
		DatasetMetadataProvider provider = new DatasetMetadataProvider();
		// for now datasets must have a version.  If they do not then add it.
		// The provider should set the version on the dataset
		Dataset mockDs = Mockito.mock(Dataset.class);
		when(mockDs.getId()).thenReturn("101");
		when(mockDs.getVersion()).thenReturn(null);
		provider.validateEntity(mockDs, new EntityEvent(EventType.CREATE, null, null));
		verify(mockDs).setVersion("1.0.0");
	}
	
	@Test
	public void testCreateChildrenLayerQuery(){
		BasicQuery query = DatasetMetadataProvider.createChildrenLayerQuery("123");
		assertNotNull(query);
		assertEquals(ObjectType.layer, query.getFrom());
		assertNotNull(query.getFilters());
		assertEquals(1, query.getFilters().size());
		Expression expression = query.getFilters().get(0);
		assertNotNull(expression);
		assertNotNull(expression.getId());
		assertEquals(NodeConstants.COL_PARENT_ID, expression.getId().getFieldName());
		assertEquals(Compartor.EQUALS, expression.getCompare());
		assertEquals(123L, expression.getValue());
	}
	
	@Test
	public void testCreateHasClinicalQuery(){
		BasicQuery query = DatasetMetadataProvider.createHasClinicalQuery("123");
		assertNotNull(query);
		assertEquals(ObjectType.layer, query.getFrom());
		assertNotNull(query.getFilters());
		assertEquals(2, query.getFilters().size());
		// find the clinical
		for(Expression expression: query.getFilters()){
			assertNotNull(expression);
			assertNotNull(expression.getId());
			if(NodeConstants.COLUMN_LAYER_TYPE.equals(expression.getId().getFieldName())){
				assertEquals(Compartor.EQUALS, expression.getCompare());
				assertEquals(InputDataLayer.LayerTypeNames.C.name(), expression.getValue());
			}
		}
	}
	
	@Test
	public void testCreateHasExpressionQuery(){
		BasicQuery query = DatasetMetadataProvider.createHasExpressionQuery("123");
		assertNotNull(query);
		assertEquals(ObjectType.layer, query.getFrom());
		assertNotNull(query.getFilters());
		assertEquals(2, query.getFilters().size());
		// find the clinical
		for(Expression expression: query.getFilters()){
			assertNotNull(expression);
			assertNotNull(expression.getId());
			if(NodeConstants.COLUMN_LAYER_TYPE.equals(expression.getId().getFieldName())){
				assertEquals(Compartor.EQUALS, expression.getCompare());
				assertEquals(InputDataLayer.LayerTypeNames.E.name(), expression.getValue());
			}
		}
	}
	
	@Test
	public void testCreateHasGeneticQuery(){
		BasicQuery query = DatasetMetadataProvider.createHasGeneticQuery("123");
		assertNotNull(query);
		assertEquals(ObjectType.layer, query.getFrom());
		assertNotNull(query.getFilters());
		assertEquals(2, query.getFilters().size());
		// find the clinical
		for(Expression expression: query.getFilters()){
			assertNotNull(expression);
			assertNotNull(expression.getId());
			if(NodeConstants.COLUMN_LAYER_TYPE.equals(expression.getId().getFieldName())){
				assertEquals(Compartor.EQUALS, expression.getCompare());
				assertEquals(InputDataLayer.LayerTypeNames.G.name(), expression.getValue());
			}
		}
	}
	
	@Test
	public void testHasAllThreeLayerTypes() throws DatastoreException, InvalidModelException, UnauthorizedException, NotFoundException{
		// This first dataset exists to ensure there are layers of each type that do not belong to the
		// the dataset we are testing.
		Project project = new Project();
		project.setName("createAtLeastOneOfEachType");
		project = entityController.createEntity(userName, project, mockRequest);
		assertNotNull(project);
		toDelete.add(project.getId());
		
		Dataset ds = new Dataset();
		ds.setName("DatasetMetadataProviderTestDataset1");
		ds.setParentId(project.getId());
		ds = entityController.createEntity(userName, ds, mockRequest);
		assertNotNull(ds);
		toDelete.add(ds.getId());
		// Add a layer of each type to this dataset
		LayerTypeNames[] types = LayerTypeNames.values();
		for(LayerTypeNames type: types){
			InputDataLayer layer = new InputDataLayer();
			layer.setName("DatasetMetadataProviderTestLayerChildNotUsed");
			layer.setType(type.name());
			layer.setParentId(ds.getId());
			layer = entityController.createEntity(userName, layer, mockRequest);
			assertNotNull(layer);
			toDelete.add(layer.getId());
		}
		
		// This is the dataset that we are actually testing.
		ds = new Dataset();
		ds.setName("DatasetMetadataProviderTestDataset");
		ds.setParentId(project.getId());
		ds = entityController.createEntity(userName, ds, mockRequest);
		assertNotNull(ds);
		toDelete.add(ds.getId());
		// This dataset should not have any layers
		datasetMetadataProvider.addTypeSpecificMetadata(ds, mockRequest, testUser, EventType.GET);
		assertFalse(ds.getHasClinicalData());
		assertFalse(ds.getHasGeneticData());
		assertFalse(ds.getHasExpressionData());
		
		// Now add a clinical layer
		InputDataLayer layer = new InputDataLayer();
		layer.setName("DatasetMetadataProviderTestLayerC");
		layer.setType(LayerTypeNames.C.name());
		layer.setParentId(ds.getId());
		layer = entityController.createEntity(userName, layer, mockRequest);
		assertNotNull(layer);
		toDelete.add(layer.getId());
		String clinicalId = layer.getId();
		
		// We should now have a clinical
		datasetMetadataProvider.addTypeSpecificMetadata(ds, mockRequest, testUser, EventType.GET);
		assertTrue(ds.getHasClinicalData());
		assertFalse(ds.getHasGeneticData());
		assertFalse(ds.getHasExpressionData());
		
		// Add an expresion layer
		// Now add a clinical layer
		layer = new InputDataLayer();
		layer.setName("DatasetMetadataProviderTestLayerE");
		layer.setType(LayerTypeNames.E.name());
		layer.setParentId(ds.getId());
		layer = entityController.createEntity(userName, layer, mockRequest);
		assertNotNull(layer);
		toDelete.add(layer.getId());
		String expressionId = layer.getId();
		
		// We should now have a clinical and expression
		datasetMetadataProvider.addTypeSpecificMetadata(ds, mockRequest, testUser, EventType.GET);
		assertTrue(ds.getHasClinicalData());
		assertFalse(ds.getHasGeneticData());
		assertTrue(ds.getHasExpressionData());
		
		// Add an geneitc layer
		// Now add a clinical layer
		layer = new InputDataLayer();
		layer.setName("DatasetMetadataProviderTestLayerG");
		layer.setType(LayerTypeNames.G.name());
		layer.setParentId(ds.getId());
		layer = entityController.createEntity(userName, layer, mockRequest);
		assertNotNull(layer);
		toDelete.add(layer.getId());
		String geneticId = layer.getId();
		
		// We should now have a clinical and expression and genetic
		datasetMetadataProvider.addTypeSpecificMetadata(ds, mockRequest, testUser, EventType.GET);
		assertTrue(ds.getHasClinicalData());
		assertTrue(ds.getHasGeneticData());
		assertTrue(ds.getHasExpressionData());
		
		// now start deleting
		entityController.deleteEntity(userName, clinicalId, InputDataLayer.class);
		
		// We should now have a expression and genetic
		datasetMetadataProvider.addTypeSpecificMetadata(ds, mockRequest, testUser, EventType.GET);
		assertFalse(ds.getHasClinicalData());
		assertTrue(ds.getHasGeneticData());
		assertTrue(ds.getHasExpressionData());
		
		// Delete expression
		entityController.deleteEntity(userName, expressionId, InputDataLayer.class);
		
		// We should now have a genetic
		datasetMetadataProvider.addTypeSpecificMetadata(ds, mockRequest, testUser, EventType.GET);
		assertFalse(ds.getHasClinicalData());
		assertTrue(ds.getHasGeneticData());
		assertFalse(ds.getHasExpressionData());
		
		// Delete genetic
		entityController.deleteEntity(userName, geneticId, InputDataLayer.class);
		// Now all should be false
		datasetMetadataProvider.addTypeSpecificMetadata(ds, mockRequest, testUser, EventType.GET);
		assertFalse(ds.getHasClinicalData());
		assertFalse(ds.getHasGeneticData());
		assertFalse(ds.getHasExpressionData());
	}
	
}
