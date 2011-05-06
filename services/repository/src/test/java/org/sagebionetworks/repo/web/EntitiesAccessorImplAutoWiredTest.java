package org.sagebionetworks.repo.web;

import static org.junit.Assert.*;
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
import org.sagebionetworks.authutil.AuthUtilConstants;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.AuthorizationDAO;
import org.sagebionetworks.repo.model.Dataset;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:manager-test-context.xml" })
public class EntitiesAccessorImplAutoWiredTest {
	
	@Autowired
	EntitiesAccessor2 entitiesAccessor;
	
	@Autowired
	EntityManager entityManager;
	
	private AuthorizationDAO mockAuth;
	
	List<String> toDelete = null;
	
	private int totalEntities = 10;
	
	@Before
	public void before() throws DatastoreException, InvalidModelException, NotFoundException, UnauthorizedException{
		assertNotNull(entitiesAccessor);
		assertNotNull(entityManager);
		mockAuth = Mockito.mock(AuthorizationDAO.class);
		entityManager.overrideAuthDaoForTest(mockAuth);
		entitiesAccessor.overrideAuthDaoForTest(mockAuth);
		when(mockAuth.canAccess(anyString(), anyString(), any(AuthorizationConstants.ACCESS_TYPE.class))).thenReturn(true);
		when(mockAuth.canCreate(anyString(), anyString())).thenReturn(true);
		
		toDelete = new ArrayList<String>();
		// Create some datasetst.
		for(int i=0; i<totalEntities; i++){
			Dataset ds = createForTest(i);
			String id = entityManager.createEntity(null, ds);
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
	
	@Ignore
	@Test
	public void testQuery() throws DatastoreException, NotFoundException, UnauthorizedException{
		// Basic query
		List<Dataset> results = entitiesAccessor.getInRange(null, 0, totalEntities, Dataset.class);
		assertNotNull(results);
		assertEquals(totalEntities, results.size());
		// Sorted
		results = entitiesAccessor.getInRangeSortedBy(null, 0, 3, "name", true, Dataset.class);
		assertNotNull(results);
		assertEquals(3, results.size());
		assertNotNull(results.get(2));
		assertEquals("someName2", results.get(2).getName());
	}

}
