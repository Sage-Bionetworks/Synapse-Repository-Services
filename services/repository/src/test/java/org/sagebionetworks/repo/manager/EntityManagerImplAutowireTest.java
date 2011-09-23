package org.sagebionetworks.repo.manager;

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
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.Dataset;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.Layer;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.util.UserProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class EntityManagerImplAutowireTest {
	
	@Autowired
	private EntityManager entityManager;
	
	@Autowired
	public UserProvider testUserProvider;
	
	// We use a mock auth DAO for this test.
	private AuthorizationManager mockAuth;

	private List<String> toDelete;
	
	private UserInfo userInfo;
	
//	private final UserInfo anonUserInfo = new UserInfo(false);

	
	@Before
	public void before() throws Exception{
		assertNotNull(entityManager);
		assertNotNull(testUserProvider);
		userInfo = testUserProvider.getTestAdminUserInfo();
		
		toDelete = new ArrayList<String>();
		mockAuth = Mockito.mock(AuthorizationManager.class);
		entityManager.overrideAuthDaoForTest(mockAuth);
		when(mockAuth.canAccess((UserInfo)any(), anyString(), any(AuthorizationConstants.ACCESS_TYPE.class))).thenReturn(true);
		when(mockAuth.canCreate((UserInfo)any(), (Node)any())).thenReturn(true);

//		User anonUser = new User();
//		anonUser.setUserId(AuthUtilConstants.ANONYMOUS_USER_ID);
//		anonUserInfo.setUser(anonUser);
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
	public void testAllInOne() throws DatastoreException, InvalidModelException, NotFoundException, UnauthorizedException, ConflictingUpdateException{
		// Create a datset
		Dataset ds = createDataset();
		String id = entityManager.createEntity(userInfo, ds);
		assertNotNull(id);
		toDelete.add(id);
		// Get another copy
		EntityWithAnnotations<Dataset> ewa = entityManager.getEntityWithAnnotations(userInfo, id, Dataset.class);
		assertNotNull(ewa);
		assertNotNull(ewa.getAnnotations());
		assertNotNull(ewa.getEntity());
		Dataset fetched = entityManager.getEntity(userInfo, id, Dataset.class);
		assertNotNull(fetched);
		assertEquals(ewa.getEntity(), fetched);
		System.out.println("Original: "+ds.toString());
		System.out.println("Fetched: "+fetched.toString());
		assertEquals(ds.getName(), fetched.getName());
		assertEquals(ds.getStatus(), fetched.getStatus());
		assertEquals(ds.getVersion(), fetched.getVersion());
		// Now get the Annotations
		Annotations annos = entityManager.getAnnotations(userInfo, id);
		assertNotNull(annos);
		assertEquals(ewa.getAnnotations(), annos);
		annos.addAnnotation("someNewTestAnnotation", "someStringValue");
		// Update
		entityManager.updateAnnotations(userInfo,id, annos);
		// Now make sure it changed
		annos = entityManager.getAnnotations(userInfo, id);
		assertNotNull(annos);
		assertEquals("someStringValue", annos.getSingleValue("someNewTestAnnotation"));
		// Now update the dataset
		fetched = entityManager.getEntity(userInfo, id, Dataset.class);
		fetched.setName("myNewName");
		entityManager.updateEntity(userInfo, fetched, false);
		fetched = entityManager.getEntity(userInfo, id, Dataset.class);
		assertNotNull(fetched);
		assertEquals("myNewName", fetched.getName());
	}
	
	@Test
	public void testAggregateUpdate() throws ConflictingUpdateException, NotFoundException, DatastoreException, UnauthorizedException, InvalidModelException{
		Dataset ds = createDataset();
		String parentId = entityManager.createEntity(userInfo, ds);
		assertNotNull(parentId);
		toDelete.add(parentId);
		List<Layer> layerList = new ArrayList<Layer>();
		int layers = 3;
		for(int i=0; i<layers; i++){
			Layer layer = createLayerForTest(i);
			layerList.add(layer);
		}
		List<String> childrenIds = entityManager.aggregateEntityUpdate(userInfo, parentId, layerList);
		assertNotNull(childrenIds);
		assertEquals(layers, childrenIds.size());
		
		List<Layer> children = entityManager.getEntityChildren(userInfo, parentId, Layer.class);
		assertNotNull(children);
		assertEquals(layers, children.size());
		Layer toUpdate = children.get(0);
		String udpatedId = toUpdate.getId();
		assertNotNull(udpatedId);
		toUpdate.setName("updatedName");
		// Do it again
		entityManager.aggregateEntityUpdate(userInfo, parentId, children);
		children = entityManager.getEntityChildren(userInfo, parentId, Layer.class);
		assertNotNull(children);
		assertEquals(layers, children.size());
		// find the one with the updated name
		Layer updatedLayer = entityManager.getEntity(userInfo, udpatedId, Layer.class);
		assertNotNull(updatedLayer);
		assertEquals("updatedName", updatedLayer.getName());
	}
	
	/**
	 * To resolve issue PLFM-203 we added a support for annotation name-spaces.  
	 * The primary field of a entity get their own name space.  Now when the annotations
	 * of an entity are fetched, we return entities from the "additional" name-spaces.
	 * @throws NotFoundException 
	 * @throws UnauthorizedException 
	 * @throws InvalidModelException 
	 * @throws DatastoreException 
	 * @throws ConflictingUpdateException 
	 */
	@Test
	public void testPLFM_203() throws DatastoreException, InvalidModelException, UnauthorizedException, NotFoundException, ConflictingUpdateException{
		Dataset ds = createDataset();
		// This primary field is stored as an annotation.
		ds.setEulaId("45");
		String id = entityManager.createEntity(userInfo, ds);
		assertNotNull(id);
		toDelete.add(id);
		// Ge the annotations of the datasets
		Annotations annos = entityManager.getAnnotations(userInfo, id);
		assertNotNull(annos);
		// None of the primary field annotations should be in this set, in fact it should be emtpy
		assertEquals(0, annos.getStringAnnotations().size());
		assertEquals(0, annos.getDateAnnotations().size());
		Dataset clone = entityManager.getEntity(userInfo, id, Dataset.class);
		assertNotNull(clone);
		assertEquals(ds.getEulaId(), clone.getEulaId());
		// Now add an annotation
		annos.addAnnotation("stringKey", "some string value");
		entityManager.updateAnnotations(userInfo, id, annos);
		annos = entityManager.getAnnotations(userInfo, id);
		assertNotNull(annos);
		assertEquals("some string value", annos.getSingleValue("stringKey"));
		// Make sure we did not lose any primary annotations.
		clone = entityManager.getEntity(userInfo, id, Dataset.class);
		assertNotNull(clone);
		assertEquals(ds.getEulaId(), clone.getEulaId());
		// Now change the primary field
		clone.setEulaId("101");
		entityManager.updateEntity(userInfo, clone, false);
		clone = entityManager.getEntity(userInfo, id, Dataset.class);
		assertNotNull(clone);
		assertEquals("101", clone.getEulaId());
		// We should not have lost any of the additional annotations
		annos = entityManager.getAnnotations(userInfo, id);
		assertNotNull(annos);
		assertEquals("some string value", annos.getSingleValue("stringKey"));
		
	}
	
	private Layer createLayerForTest(int i){
		Layer layer = new Layer();
		layer.setName("layerName"+i);
		layer.setDescription("layerDesc"+i);
		layer.setCreationDate(new Date(1001));
		return layer;
	}
	
	/**
	 * Create a dataset with all of its fields filled in.
	 * @return
	 */
	public Dataset createDataset(){
		// First we create a dataset with all fields filled in.
		Dataset ds = new Dataset();
		ds.setName("someName");
		ds.setDescription("someDesc");
		ds.setCreator("magic");
		ds.setCreationDate(new Date(1001));
		ds.setAnnotations("someAnnoUrl");
		ds.setEtag("110");
		ds.setId("12");
		ds.setHasClinicalData(false);
		ds.setHasExpressionData(true);
		ds.setHasGeneticData(true);
		ds.setLayers("someLayerUrl");
		ds.setReleaseDate(new Date(15689));
		ds.setStatus("someStatus");
		ds.setVersion("someVersion");
		ds.setUri("someUri");
		return ds;
	}

}
