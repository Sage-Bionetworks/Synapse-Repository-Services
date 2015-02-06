package org.sagebionetworks.repo.manager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.AsynchronousDAO;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.Folder;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.NodeQueryDao;
import org.sagebionetworks.repo.model.NodeQueryResults;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.query.BasicQuery;
import org.sagebionetworks.repo.model.query.Comparator;
import org.sagebionetworks.repo.model.query.CompoundId;
import org.sagebionetworks.repo.model.query.Expression;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class QueryManagerAutowireTest {
	
	@Autowired
	private EntityManager entityManager;
	
	@Autowired
	public UserManager userManager;
	
	@Autowired
	private NodeQueryDao nodeQueryDao;
	
	@Autowired
	private AsynchronousDAO asynchronousDAO;
	
	private List<String> toDelete = null;
	
	private long totalEntities = 10;
	
	private UserInfo adminUserInfo;
	private HttpServletRequest mockRequest;
	
	@Before
	public void before() throws Exception {
		adminUserInfo = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		UserInfo.validateUserInfo(adminUserInfo);
		toDelete = new ArrayList<String>();
		mockRequest = Mockito.mock(HttpServletRequest.class);
		when(mockRequest.getServletPath()).thenReturn("/repo/v1");
		
		Project project = new Project();
		project.setName("QueryManagerAutowireTest.rootProject2");
		String id = entityManager.createEntity(adminUserInfo, project, null);
		project.setId(id);
		toDelete.add(project.getId());
		
		List<String> toUpdate = new LinkedList<String>();
		toUpdate.add(project.getId());
		// Create some datasets
		for(int i=0; i<totalEntities; i++){
			Folder ds = createForTest(i);
			ds.setParentId(project.getId());
			String dsId = entityManager.createEntity(adminUserInfo, ds, null);
			ds.setId(dsId);
			assertNotNull(ds);
			assertNotNull(ds.getId());
			toDelete.add(ds.getId());
			toUpdate.add(ds.getId());
			Annotations annos = entityManager.getAnnotations(adminUserInfo, ds.getId());
			assertNotNull(annos);
			// Add some annotations
			annos.addAnnotation("stringKey", "string"+i);
			annos.addAnnotation("stringListKey", "one");
			annos.addAnnotation("stringListKey", "two");
			annos.addAnnotation("stringListKey", "three");
			annos.addAnnotation("longKey", new Long(i));
			annos.addAnnotation("dateKey", new Date(10000+i));
			annos.addAnnotation("doubleKey", new Double(42*i));
			entityManager.updateAnnotations(adminUserInfo, ds.getId(), annos);
			// Add a layer to each dataset
			Folder inLayer = createLayerForTest(i);
			inLayer.setParentId(ds.getId());
			String lid = entityManager.createEntity(adminUserInfo, inLayer, null);
			inLayer.setId(id);
		}
		
		// since we have moved the annotation updates to an asynchronous process we need to manually
		// update the annotations of all nodes for this test. See PLFM-1548
		for(String entityId: toUpdate){
			asynchronousDAO.createEntity(entityId);
		}
	}
	
	private Folder createForTest(int i){
		Folder ds = new Folder();
		ds.setName("someName"+i);
		ds.setDescription("someDesc"+i);
		ds.setCreatedBy("magic"+i);
		ds.setCreatedOn(new Date(1001));
		ds.setAnnotations("someAnnoUrl"+1);
		ds.setUri("someUri"+i);
		return ds;
	}
	
	private Folder createLayerForTest(int i) throws InvalidModelException{
		Folder layer = new Folder();
		layer.setName("layerName"+i);
		layer.setDescription("layerDesc"+i);
		layer.setCreatedOn(new Date(1001));
		return layer;
	}
	
	@After
	public void after(){
		if(entityManager != null && toDelete != null){
			for(String id: toDelete){
				try{
					entityManager.deleteEntity(adminUserInfo, id);
				}catch(Exception e){}
			}
		}
	}
	
	@Test
	public void testExecuteQuery() throws DatastoreException, NotFoundException, UnauthorizedException {
		// Build up the query.
		BasicQuery query = new BasicQuery();
		query.setFrom(EntityType.folder.name());
		query.setOffset(0);
		query.setLimit(totalEntities-2);
		query.setSort("longKey");
		query.setAscending(false);
		query.addExpression(new Expression(new CompoundId("dataset", "doubleKey"), Comparator.GREATER_THAN, "0.0"));
		// Execute it.
		long start = System.currentTimeMillis();
		
		NodeQueryResults nodeResults = nodeQueryDao.executeQuery(query, adminUserInfo);
		QueryResults results = new QueryResults(nodeResults.getAllSelectedData(), nodeResults.getTotalNumberOfResults());
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
		Collection<String> collect = (Collection<String>) ob;
		assertTrue(collect.contains("three"));
		
		assertFalse("Test for bug PLFM-834", row.containsKey("jsonschema"));
	}
	
	/**
	 * This is a test for PLFM-1166
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws UnauthorizedException
	 */
	@Test
	public void testSelectAnnotation() throws DatastoreException, NotFoundException, UnauthorizedException {
		// Build up the query.
		BasicQuery query = new BasicQuery();
		query.setSelect(new ArrayList<String>());
		query.getSelect().add("id");
		query.getSelect().add("stringListKey");
		query.setFrom(EntityType.folder.name());
		query.setOffset(0);
		query.setLimit(totalEntities-2);
		query.setSort("longKey");
		query.setAscending(false);
		query.addExpression(new Expression(new CompoundId("folder", "doubleKey"), Comparator.GREATER_THAN, "0.0"));
		// Execute it.
		long start = System.currentTimeMillis();
		NodeQueryResults nodeResults = nodeQueryDao.executeQuery(query, adminUserInfo);
		QueryResults results = new QueryResults(nodeResults.getAllSelectedData(), nodeResults.getTotalNumberOfResults());
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
		System.out.println(row);
		Object ob = row.get("stringListKey");
		assertNotNull(ob);
		assertTrue(ob instanceof Collection);
		Collection<String> collect = (Collection<String>) ob;
		assertTrue(collect.contains("three"));
	}

}
