package org.sagebionetworks.repo.manager;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.Data;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.Folder;
import org.sagebionetworks.repo.model.GenotypeData;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.PhenotypeData;
import org.sagebionetworks.repo.model.Study;
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
	
	@Before
	public void before() throws Exception{
		assertNotNull(entityManager);
		assertNotNull(testUserProvider);
		userInfo = testUserProvider.getTestAdminUserInfo();
		
		toDelete = new ArrayList<String>();
		mockAuth = Mockito.mock(AuthorizationManager.class);
		entityManager.overrideAuthDaoForTest(mockAuth);
		when(mockAuth.canAccess((UserInfo)any(), anyString(), any(ACCESS_TYPE.class))).thenReturn(true);
		when(mockAuth.canCreate((UserInfo)any(), (Node)any())).thenReturn(true);

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
	public void testChangeEntityType1() throws Exception {
		// Data to PhenotypeData
		Data d = createLayerForTest(1);
		String id = entityManager.createEntity(userInfo, d);
		toDelete.add(id);
		Annotations annos = entityManager.getAnnotations(userInfo, id);
		annos.addAnnotation("someAnnotKey", "someAnnotValue");
		entityManager.updateAnnotations(userInfo, id, annos);
		d = entityManager.getEntity(userInfo, id, Data.class);
		entityManager.changeEntityType(userInfo, id, "phenotypedata", d.getEtag());
		PhenotypeData pd = entityManager.getEntity(userInfo, id, PhenotypeData.class);
		assertNotNull(pd);
		annos = entityManager.getAnnotations(userInfo, id);
		assertEquals("someAnnotValue", annos.getSingleValue("someAnnotKey"));
	}
	
	@Test
	public void testChangeEntityType2() throws Exception {
		// Version 1
		Data d = new Data();
		d.setName("dataEntityName");
		d.setDescription("dataEntityDescription");
		d.setDisease("dataEntityDiseaseV1");
		d.setVersionLabel("1.0.0");
		String id = entityManager.createEntity(userInfo, d);
		toDelete.add(id);
		Annotations annots = entityManager.getAnnotations(userInfo, id);
		annots.addAnnotation("v1StringAnnotKey", "v1StringAnnotValue");
		annots.addAnnotation("v1LongAnnotKey", new Long(1));
		entityManager.updateAnnotations(userInfo, id, annots);
		// Version 2
		d = entityManager.getEntity(userInfo, id, Data.class);
		d.setName("dataEntityNameV2");
		d.setDescription("dataEntityDescriptionV2");
		d.setVersionLabel("2.0.0");
		entityManager.updateEntity(userInfo, d, true);
		annots = entityManager.getAnnotations(userInfo, id);
		annots.addAnnotation("v2StringAnnotKey", "v2StringAnnotValue");
		annots.addAnnotation("v2DoubleAnnotKey", new Double(2.0));
		entityManager.updateAnnotations(userInfo, id, annots);
		d = entityManager.getEntity(userInfo, id, Data.class);
		String beforeETag = d.getEtag();
		
		// Change type
		entityManager.changeEntityType(userInfo, id, "phenotypedata", beforeETag);
		
		// Check
		PhenotypeData pd = entityManager.getEntity(userInfo, id, PhenotypeData.class);
		assertNotNull(pd);
		assertEquals("dataEntityNameV2", pd.getName());
		assertEquals("dataEntityDiseaseV1", pd.getDisease());
		assertFalse(beforeETag.equals(pd.getEtag()));
		annots = entityManager.getAnnotations(userInfo, id);
		assertTrue(annots.getStringAnnotations().containsKey("v2StringAnnotKey"));
		assertEquals("v2StringAnnotValue", annots.getSingleValue("v2StringAnnotKey"));
		assertEquals(new Double(2.0), annots.getSingleValue("v2DoubleAnnotKey"));
		assertEquals(new Long(1), annots.getSingleValue("v1LongAnnotKey"));
		Annotations annots2 = entityManager.getAnnotationsForVersion(userInfo, id, 2L);
		assertEquals(annots, annots2);
		Annotations annots1 = entityManager.getAnnotationsForVersion(userInfo, id, 1L);
		assertEquals(annots.getSingleValue("v1LongANnotsKey"), annots1.getSingleValue("v1LongAnnotsKey"));
		assertFalse(annots1.getDoubleAnnotations().containsKey("v2DoubleAnnotKey"));
		d = entityManager.getEntityForVersion(userInfo, id, 1L, Data.class);
		assertNotNull(d);
		assertEquals("1.0.0", d.getVersionLabel());
		assertEquals("dataEntityDiseaseV1", d.getDisease());
	}
	
	
	@Test
	public void testAllInOne() throws DatastoreException, InvalidModelException, NotFoundException, UnauthorizedException, ConflictingUpdateException{
		// Create a datset
		Study ds = createDataset();
		String id = entityManager.createEntity(userInfo, ds);
		assertNotNull(id);
		toDelete.add(id);
		// Get another copy
		EntityWithAnnotations<Study> ewa = entityManager.getEntityWithAnnotations(userInfo, id, Study.class);
		assertNotNull(ewa);
		assertNotNull(ewa.getAnnotations());
		assertNotNull(ewa.getEntity());
		Study fetched = entityManager.getEntity(userInfo, id, Study.class);
		assertNotNull(fetched);
		assertEquals(ewa.getEntity(), fetched);
		System.out.println("Original: "+ds.toString());
		System.out.println("Fetched: "+fetched.toString());
		assertEquals(ds.getName(), fetched.getName());
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
		fetched = entityManager.getEntity(userInfo, id, Study.class);
		fetched.setName("myNewName");
		entityManager.updateEntity(userInfo, fetched, false);
		fetched = entityManager.getEntity(userInfo, id, Study.class);
		assertNotNull(fetched);
		assertEquals("myNewName", fetched.getName());
	}
	
	
	@Test
	public void testAggregateUpdate() throws ConflictingUpdateException, NotFoundException, DatastoreException, UnauthorizedException, InvalidModelException{
		Study ds = createDataset();
		String parentId = entityManager.createEntity(userInfo, ds);
		assertNotNull(parentId);
		toDelete.add(parentId);
		List<Data> layerList = new ArrayList<Data>();
		int layers = 3;
		for(int i=0; i<layers; i++){
			Data layer = createLayerForTest(i);
			layerList.add(layer);
		}
		List<String> childrenIds = entityManager.aggregateEntityUpdate(userInfo, parentId, layerList);
		assertNotNull(childrenIds);
		assertEquals(layers, childrenIds.size());
		
		List<Data> children = entityManager.getEntityChildren(userInfo, parentId, Data.class);
		assertNotNull(children);
		assertEquals(layers, children.size());
		Data toUpdate = children.get(0);
		String udpatedId = toUpdate.getId();
		assertNotNull(udpatedId);
		toUpdate.setName("updatedName");
		// Do it again
		entityManager.aggregateEntityUpdate(userInfo, parentId, children);
		children = entityManager.getEntityChildren(userInfo, parentId, Data.class);
		assertNotNull(children);
		assertEquals(layers, children.size());
		// find the one with the updated name
		Data updatedLayer = entityManager.getEntity(userInfo, udpatedId, Data.class);
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
		Study ds = createDataset();
		// This primary field is stored as an annotation.
		ds.setDisease("disease");
		String id = entityManager.createEntity(userInfo, ds);
		assertNotNull(id);
		toDelete.add(id);
		// Ge the annotations of the datasets
		Annotations annos = entityManager.getAnnotations(userInfo, id);
		assertNotNull(annos);
		// None of the primary field annotations should be in this set, in fact it should be emtpy
		assertEquals(0, annos.getStringAnnotations().size());
		assertEquals(0, annos.getDateAnnotations().size());
		Study clone = entityManager.getEntity(userInfo, id, Study.class);
		assertNotNull(clone);
		assertEquals(ds.getDisease(), clone.getDisease());
		// Now add an annotation
		annos.addAnnotation("stringKey", "some string value");
		entityManager.updateAnnotations(userInfo, id, annos);
		annos = entityManager.getAnnotations(userInfo, id);
		assertNotNull(annos);
		assertEquals("some string value", annos.getSingleValue("stringKey"));
		// Make sure we did not lose any primary annotations.
		clone = entityManager.getEntity(userInfo, id, Study.class);
		assertNotNull(clone);
		assertEquals(ds.getDisease(), clone.getDisease());
		// Now change the primary field
		clone.setDisease("disease2");
		entityManager.updateEntity(userInfo, clone, false);
		clone = entityManager.getEntity(userInfo, id, Study.class);
		assertNotNull(clone);
		assertEquals("disease2", clone.getDisease());
		// We should not have lost any of the additional annotations
		annos = entityManager.getAnnotations(userInfo, id);
		assertNotNull(annos);
		assertEquals("some string value", annos.getSingleValue("stringKey"));
		
	}
	
	
	@Test
	public void testPLFM_1283() throws DatastoreException, InvalidModelException, UnauthorizedException, NotFoundException{
		Data study = new Data();
		study.setName("test PLFM-1283");
		String id = entityManager.createEntity(userInfo, study);
		assertNotNull(id);
		toDelete.add(id);
		try{
			entityManager.getEntityWithAnnotations(userInfo, id, GenotypeData.class);
			fail("The requested entity type does not match the actaul entity type so this should fail.");
		}catch(IllegalArgumentException e){
			// This is expected.
			System.out.println(e.getMessage());
			assertTrue(e.getMessage().indexOf(id) > 0);
			assertTrue(e.getMessage().indexOf(Data.class.getName()) > 0);
			assertTrue(e.getMessage().indexOf(GenotypeData.class.getName()) > 0);
		}
		
	}
	
	private Data createLayerForTest(int i){
		Data layer = new Data();
		layer.setName("layerName"+i);
		layer.setDescription("layerDesc"+i);
		layer.setCreatedOn(new Date(1001));
		return layer;
	}
	
	/**
	 * Create a dataset with all of its fields filled in.
	 * @return
	 */
	public Study createDataset(){
		// First we create a dataset with all fields filled in.
		Study ds = new Study();
		ds.setName("someName");
		ds.setDescription("someDesc");
		ds.setCreatedBy("magic");
		ds.setCreatedOn(new Date(1001));
		ds.setAnnotations("someAnnoUrl");
		ds.setEtag("110");
		ds.setId("12");
		ds.setUri("someUri");
		return ds;
	}

}
