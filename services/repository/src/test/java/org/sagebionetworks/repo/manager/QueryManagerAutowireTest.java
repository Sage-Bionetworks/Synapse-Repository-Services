package org.sagebionetworks.repo.manager;

import static org.junit.Assert.*;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.sagebionetworks.authutil.AuthUtilConstants;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.AuthorizationManager;
import org.sagebionetworks.repo.model.Dataset;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.query.BasicQuery;
import org.sagebionetworks.repo.model.query.Compartor;
import org.sagebionetworks.repo.model.query.CompoundId;
import org.sagebionetworks.repo.model.query.Expression;
import org.sagebionetworks.repo.model.query.ObjectType;
import org.sagebionetworks.repo.web.ConflictingUpdateException;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:manager-test-context.xml" })
public class QueryManagerAutowireTest {
	
	@Autowired
	QueryManager queryManager;
	@Autowired
	EntityManager entityManager;
	
	private AuthorizationManager mockAuth;
	
	List<String> toDelete = null;
	
	private long totalEntities = 10;
	
	@Before
	public void before() throws DatastoreException, InvalidModelException, NotFoundException, UnauthorizedException, ConflictingUpdateException{
		assertNotNull(queryManager);
		assertNotNull(queryManager);
		mockAuth = Mockito.mock(AuthorizationManager.class);
		entityManager.overrideAuthDaoForTest(mockAuth);
		when(mockAuth.canAccess(anyString(), anyString(), any(AuthorizationConstants.ACCESS_TYPE.class))).thenReturn(true);
		when(mockAuth.canCreate(anyString(), anyString())).thenReturn(true);
		
		toDelete = new ArrayList<String>();
		// Create some datasetst.
		for(int i=0; i<totalEntities; i++){
			Dataset ds = createForTest(i);
			String id = entityManager.createEntity(null, ds);
			toDelete.add(id);
			Annotations annos = entityManager.getAnnoations(null, id);
			assertNotNull(annos);
			// Add some annotations
			annos.addAnnotation("stringKey", "string"+i);
			annos.addAnnotation("stringListKey", "one");
			annos.addAnnotation("stringListKey", "two");
			annos.addAnnotation("stringListKey", "three");
			annos.addAnnotation("longKey", new Long(i));
			annos.addAnnotation("dateKey", new Date(10000+i));
			annos.addAnnotation("doubleKey", new Double(42*i));
			entityManager.updateAnnotations(null,id, annos);
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
	
	@Test
	public void testExecuteQuery() throws DatastoreException{
		// Build up the query.
		BasicQuery query = new BasicQuery();
		query.setFrom(ObjectType.dataset);
		query.setOffset(0);
		query.setLimit(totalEntities-2);
		query.setSort("longKey");
		query.setAscending(false);
		query.addExpression(new Expression(new CompoundId("dataset", "doubleKey"), Compartor.GREATER_THAN, "0.0"));
		// Execute it.
		long start = System.currentTimeMillis();
		QueryResults results = queryManager.executeQuery(null, query, Dataset.class);
		long end = System.currentTimeMillis();
		System.out.println("Executed the query in: "+(end-start)+" ms");
		assertNotNull(results);
		assertEquals(totalEntities-1, results.getTotalNumberOfResults());
		// Spot check the results
		assertNotNull(results);
		System.out.println(results);
		assertNotNull(results.getResults());
		List<Map<String, Object>> list = results.getResults();
		assertEquals(8, list.size());
		Map<String, Object> row = list.get(0);
		assertNotNull(row);
		Object ob = row.get("stringListKey");
		assertTrue(ob instanceof Collection);
		Collection collect = (Collection) ob;
		assertTrue(collect.contains("three"));
	}

}
