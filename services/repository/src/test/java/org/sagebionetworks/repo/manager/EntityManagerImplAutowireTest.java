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
import org.sagebionetworks.authutil.AuthUtilConstants;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.AuthorizationDAO;
import org.sagebionetworks.repo.model.Dataset;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.web.ConflictingUpdateException;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:manager-test-context.xml" })
public class EntityManagerImplAutowireTest {
	
	@Autowired
	private EntityManager entityManager;
	
	// We use a mock auth DAO for this test.
	private AuthorizationDAO mockAuth;

	private List<String> toDelete;
	
	@Before
	public void before() throws Exception{
		assertNotNull(entityManager);
		
		toDelete = new ArrayList<String>();
		mockAuth = Mockito.mock(AuthorizationDAO.class);
		entityManager.overrideAuthDaoForTest(mockAuth);
		when(mockAuth.canAccess(anyString(), anyString(), any(AuthorizationConstants.ACCESS_TYPE.class))).thenReturn(true);
		when(mockAuth.canCreate(anyString(), anyString())).thenReturn(true);
	}
	
	@After
	public void after(){
		if(entityManager != null && toDelete != null){
			for(String id: toDelete){
				try{
					entityManager.deleteEntity(AuthUtilConstants.ANONYMOUS_USER_ID, id);
				}catch(Exception e){}
			}
		}
	}
	
	@Test
	public void testCreate() throws DatastoreException, InvalidModelException, NotFoundException, UnauthorizedException, ConflictingUpdateException{
		// Create a datset
		Dataset ds = createDataset();
		String id = entityManager.createEntity(AuthUtilConstants.ANONYMOUS_USER_ID, ds);
		assertNotNull(id);
		toDelete.add(id);
		// Get another copy
		Dataset fetched = entityManager.getEntity(null, id, Dataset.class);
		assertNotNull(fetched);
		System.out.println("Original: "+ds.toString());
		System.out.println("Fetched: "+fetched.toString());
		assertEquals(ds.getName(), fetched.getName());
		assertEquals(ds.getStatus(), fetched.getStatus());
		// Now get the Annotations
		Annotations annos = entityManager.getAnnoations(null, id);
		assertNotNull(annos);
		annos.addAnnotation("someNewTestAnnotation", "someStringValue");
		// Update
		entityManager.updateAnnotations(null, annos);
		// Now make sure it changed
		annos = entityManager.getAnnoations(null, id);
		assertNotNull(annos);
		assertEquals("someStringValue", annos.getSingleValue("someNewTestAnnotation"));
		// Now update the dataset
		fetched = entityManager.getEntity(null, id, Dataset.class);
		fetched.setName("myNewName");
		entityManager.updateEntity(null, fetched);
		fetched = entityManager.getEntity(null, id, Dataset.class);
		assertNotNull(fetched);
		assertEquals("myNewName", fetched.getName());
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
		ds.setLayer("someLayerUrl");
		ds.setReleaseDate(new Date(15689));
		ds.setStatus("someStatus");
		ds.setVersion("someVersion");
		ds.setUri("someUri");
		return ds;
	}

}
