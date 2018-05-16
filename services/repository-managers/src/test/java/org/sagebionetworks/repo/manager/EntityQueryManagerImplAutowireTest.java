package org.sagebionetworks.repo.manager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang.math.RandomUtils;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.Folder;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.NodeQueryResults;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.entity.query.Condition;
import org.sagebionetworks.repo.model.entity.query.EntityFieldCondition;
import org.sagebionetworks.repo.model.entity.query.EntityFieldName;
import org.sagebionetworks.repo.model.entity.query.EntityQuery;
import org.sagebionetworks.repo.model.entity.query.EntityQueryResult;
import org.sagebionetworks.repo.model.entity.query.EntityQueryResults;
import org.sagebionetworks.repo.model.entity.query.EntityQueryUtils;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.entity.query.Operator;
import org.sagebionetworks.repo.model.entity.query.Sort;
import org.sagebionetworks.repo.model.entity.query.SortDirection;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.query.BasicQuery;
import org.sagebionetworks.repo.model.query.Comparator;
import org.sagebionetworks.repo.model.query.CompoundId;
import org.sagebionetworks.repo.model.query.Expression;
import org.sagebionetworks.repo.model.query.jdo.NodeField;
import org.sagebionetworks.repo.model.table.EntityDTO;
import org.sagebionetworks.repo.model.table.TableEntity;
import org.sagebionetworks.table.cluster.ConnectionFactory;
import org.sagebionetworks.table.cluster.TableIndexDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.google.common.collect.Lists;

/**
 * Integration test for EntityQueryManagerImpl.
 * 
 * @author John
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class EntityQueryManagerImplAutowireTest {
	@Autowired
	StackConfiguration config;
	
	@Autowired
	private EntityManager entityManager;
	
	@Autowired
	private UserManager userManager;
	
	@Autowired
	private EntityQueryManager entityQueryManger;
	
	@Autowired
	private NodeDAO nodeDao;
	
	@Autowired
	ConnectionFactory connectionFactory;
	
	@Mock
	ProgressCallback mockProgressCallback;
	
	private List<String> nodesToDelete;
	private UserInfo adminUserInfo;
	
	Project project;
	Folder folder;
	TableEntity table;
	private String alias1 = "alias" + RandomUtils.nextInt();
	
	EntityFieldCondition parentIdCondition;
	EntityQuery query;
	
	List<String> ids;
	TableIndexDAO indexDAO;
	
	@Before
	public void before() throws Exception {
				
		MockitoAnnotations.initMocks(this);
		assertNotNull(entityManager);
		nodesToDelete = new ArrayList<String>();
		
		// Make sure we have a valid user.
		adminUserInfo = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		UserInfo.validateUserInfo(adminUserInfo);
		// project
		project = new Project();
		project.setName(UUID.randomUUID().toString());
		project.setAlias(alias1);
		String id = entityManager.createEntity(adminUserInfo, project, null);
		nodesToDelete.add(id);
		project = entityManager.getEntity(adminUserInfo, id, Project.class);
		// folder
		folder = new Folder();
		folder.setName(UUID.randomUUID().toString());
		folder.setParentId(project.getId());
		id = entityManager.createEntity(adminUserInfo, folder, null);
		folder = entityManager.getEntity(adminUserInfo, id, Folder.class);
		// Table
		table = new TableEntity();
		table.setName(UUID.randomUUID().toString());
		table.setParentId(project.getId());
		id = entityManager.createEntity(adminUserInfo, table, null);
		table = entityManager.getEntity(adminUserInfo, id, TableEntity.class);
		
		// add annotations to the table
		Annotations annotations = entityManager.getAnnotations(adminUserInfo, table.getId());
		annotations.addAnnotation("aDate", new Date(100));
		annotations.addAnnotation("aDouble", 1.1234);
		annotations.addAnnotation("aLong", 123L);
		annotations.addAnnotation("aString", "foo bar");
		entityManager.updateAnnotations(adminUserInfo, table.getId(), annotations);
		table = entityManager.getEntity(adminUserInfo, table.getId(), TableEntity.class);
		
		ids = Lists.newArrayList(project.getId(), folder.getId(), table.getId());
		int maxAnnotationChars = 500;
		List<EntityDTO> dtos = nodeDao.getEntityDTOs(ids, maxAnnotationChars);
		indexDAO = connectionFactory.getFirstConnection();
		indexDAO.addEntityData(mockProgressCallback, dtos);
		
		
		// ParentId condition
		parentIdCondition = EntityQueryUtils.buildCondition(EntityFieldName.parentId, Operator.EQUALS, project.getId());
		// query
		query = new EntityQuery();
		List<Condition> conditions = new ArrayList<Condition>(1);
		conditions.add(parentIdCondition);
		query.setConditions(conditions);
		// add a sort for consistent results
		Sort sort = new Sort();
		sort.setColumnName(EntityFieldName.id.name());
		sort.setDirection(SortDirection.DESC);
		query.setSort(sort);
	}
	
	@After
	public void after() throws Exception {
		if(entityManager != null && nodesToDelete != null){
			for(String id: nodesToDelete){
				try {
					entityManager.deleteEntity(adminUserInfo, id);
				} catch (Exception e) {
					e.printStackTrace();
				} 				
			}
		}
		if(indexDAO != null){
			indexDAO.deleteEntityData(mockProgressCallback, KeyFactory.stringToKey(ids));
		}
	}
	
	@Test
	public void testBasicQuery() throws Exception {
		// run a query with zero conditions.
		EntityQuery query = new EntityQuery();
		query.setLimit(1L);
		EntityQueryResults results = entityQueryManger.executeQuery(query, adminUserInfo);
		assertNotNull(results);
		assertNotNull(results.getEntities());
		assertEquals(1, results.getEntities().size());
		// There should be more than the three entities added for this test.
		assertTrue(results.getTotalEntityCount() >= 3);
	}
	
	@Test
	public void testQueryChildren(){
		// the test query has a parentId condition.
		EntityQueryResults results = entityQueryManger.executeQuery(query, adminUserInfo);
		assertNotNull(results);
		assertNotNull(results.getEntities());
		assertEquals(2, results.getEntities().size());
		// there are two two children in this project.
		assertTrue(results.getTotalEntityCount() == 2);
		// The first should be the table
		Long userId = adminUserInfo.getId();
		// Table validate
		EntityQueryResult tableResult = results.getEntities().get(0);
		assertEquals(table.getId(), tableResult.getId());
		assertEquals(table.getName(), tableResult.getName());
		assertEquals(table.getEtag(), tableResult.getEtag());
		assertEquals(EntityType.table.name(), tableResult.getEntityType());
		assertEquals(userId, tableResult.getCreatedByPrincipalId());
		assertEquals(table.getCreatedOn(), tableResult.getCreatedOn());
		assertEquals(userId, tableResult.getModifiedByPrincipalId());
		assertEquals(table.getModifiedOn(), tableResult.getModifiedOn());
		assertEquals(table.getParentId(), tableResult.getParentId());
		assertEquals(table.getParentId(), tableResult.getParentId());
		assertEquals(KeyFactory.stringToKey(project.getId()), tableResult.getProjectId());
		
		EntityQueryResult folderResult = results.getEntities().get(1);
		assertEquals(folder.getId(), folderResult.getId());
		assertEquals(folder.getName(), folderResult.getName());
		assertEquals(folder.getEtag(), folderResult.getEtag());
	}
	
	@Test
	public void testQueryChildrenByType(){
		// add type to the query
		query.setFilterByType(EntityType.table);
		EntityQueryResults results = entityQueryManger.executeQuery(query, adminUserInfo);
		assertNotNull(results);
		assertNotNull(results.getEntities());
		assertEquals(1, results.getEntities().size());
		// there is only one table in this project.
		assertTrue(results.getTotalEntityCount() == 1);
		// Table validate
		EntityQueryResult tableResult = results.getEntities().get(0);
		assertEquals(table.getId(), tableResult.getId());
	}
	
	@Test
	public void testQueryChildrenOffset(){
		// the test query has a parentId condition.
		// offset of one should return the folder
		query.setOffset(1L);
		EntityQueryResults results = entityQueryManger.executeQuery(query, adminUserInfo);
		assertNotNull(results);
		assertNotNull(results.getEntities());
		assertEquals(1, results.getEntities().size());
		// there are two two children in this project.
		assertTrue(results.getTotalEntityCount() == 2);
		// should only have the table.		
		EntityQueryResult folderResult = results.getEntities().get(0);
		assertEquals(folder.getId(), folderResult.getId());
		assertEquals(folder.getName(), folderResult.getName());
		assertEquals(folder.getEtag(), folderResult.getEtag());
	}
	
	@Test
	public void testQueryInClause(){
		// Test the in clause
		EntityFieldCondition inCondition = EntityQueryUtils.buildCondition(EntityFieldName.name, Operator.IN, folder.getName(), table.getName());
		// clear the other condition
		query.getConditions().clear();
		query.getConditions().add(inCondition);
		EntityQueryResults results = entityQueryManger.executeQuery(query, adminUserInfo);
		assertNotNull(results);
		assertNotNull(results.getEntities());
		assertEquals(2, results.getEntities().size());
		// there should be only two.
		assertTrue(results.getTotalEntityCount() == 2);
		// Table validate
		EntityQueryResult tableResult = results.getEntities().get(0);
		assertEquals(table.getId(), tableResult.getId());
		EntityQueryResult folderResult = results.getEntities().get(1);
		assertEquals(folder.getId(), folderResult.getId());
	}
	
	@Test
	public void testQueryDateConditionValue(){
		// The folder and table should have a createdOn greater than or equal to folder.createOn.
		EntityFieldCondition condition = EntityQueryUtils.buildCondition(EntityFieldName.createdOn, Operator.GREATER_THAN_OR_EQUALS, folder.getCreatedOn());
		// add this condition
		query.getConditions().add(condition);
		EntityQueryResults results = entityQueryManger.executeQuery(query, adminUserInfo);
		assertNotNull(results);
		assertNotNull(results.getEntities());
		assertEquals(2, results.getEntities().size());
		// there should be only two.
		assertTrue(results.getTotalEntityCount() == 2);
		// Table validate
		EntityQueryResult tableResult = results.getEntities().get(0);
		assertEquals(table.getId(), tableResult.getId());
		EntityQueryResult folderResult = results.getEntities().get(1);
		assertEquals(folder.getId(), folderResult.getId());
	}
	
	@Test
	public void testQueryIntegerConditionValue(){
		// The folder and table should have an id greater than or equal to folder.id
		EntityFieldCondition condition = EntityQueryUtils.buildCondition(EntityFieldName.id, Operator.GREATER_THAN_OR_EQUALS, KeyFactory.stringToKey(folder.getId()));
		// add this condition
		query.getConditions().add(condition);
		EntityQueryResults results = entityQueryManger.executeQuery(query, adminUserInfo);
		assertNotNull(results);
		assertNotNull(results.getEntities());
		assertEquals(2, results.getEntities().size());
		// there should be only two.
		assertTrue(results.getTotalEntityCount() == 2);
		// Table validate
		EntityQueryResult tableResult = results.getEntities().get(0);
		assertEquals(table.getId(), tableResult.getId());
		EntityQueryResult folderResult = results.getEntities().get(1);
		assertEquals(folder.getId(), folderResult.getId());
	}
	
	@Test
	public void testQueryProjectId(){
		// Find everything in the project.
		EntityFieldCondition condition = EntityQueryUtils.buildCondition(EntityFieldName.projectId, Operator.EQUALS, project.getId());
		// add this condition
		query.getConditions().clear();
		query.getConditions().add(condition);
		EntityQueryResults results = entityQueryManger.executeQuery(query, adminUserInfo);
		assertNotNull(results);
		assertNotNull(results.getEntities());
		assertEquals(3, results.getEntities().size());
		// there should be only two.
		assertTrue(results.getTotalEntityCount() == 3);
	}
	
	@Test
	public void testQueryFromEntityNotProjects() {
		EntityFieldCondition condition = EntityQueryUtils.buildCondition(EntityFieldName.nodeType, Operator.NOT_EQUALS, "folder");
		// add this condition
		query.getConditions().clear();
		query.getConditions().add(parentIdCondition);
		query.getConditions().add(condition);
		EntityQueryResults results = entityQueryManger.executeQuery(query, adminUserInfo);
		assertNotNull(results);
		List<EntityQueryResult> entityQueryResults = results.getEntities();
		assertNotNull(entityQueryResults);
		Set<String> queryResultTypes = entityQueryResultToEntityTypes(entityQueryResults);
		assertTrue(queryResultTypes.contains("table"));
		assertTrue(queryResultTypes.size() == 1);
		// there should be only 1.
		assertTrue(results.getTotalEntityCount() == 1);
	}
	
	@Test
	public void testQueryFromEntityNotProjectsTables() {
		EntityFieldCondition condition = EntityQueryUtils.buildCondition(EntityFieldName.nodeType, Operator.IN, new String[]{"table","folder"});
		// add this condition
		query.getConditions().clear();
		query.getConditions().add(parentIdCondition);
		query.getConditions().add(condition);
		EntityQueryResults results = entityQueryManger.executeQuery(query, adminUserInfo);
		assertNotNull(results);
		List<EntityQueryResult> entityQueryResults = results.getEntities();
		assertNotNull(entityQueryResults);
		Set<String> queryResultTypes = entityQueryResultToEntityTypes(entityQueryResults);
		assertTrue(queryResultTypes.contains("table"));
		assertTrue(queryResultTypes.contains("folder"));
		assertTrue(queryResultTypes.size() == 2);
		// there should be two.
		assertTrue(results.getTotalEntityCount() == 2);
	}
	
	@Ignore // query by alias no longer works
	@Test
	public void testQueryByAlias() {
		EntityFieldCondition condition = EntityQueryUtils.buildCondition(EntityFieldName.alias, Operator.EQUALS, alias1);
		// add this condition
		query.getConditions().clear();
		query.getConditions().add(condition);
		EntityQueryResults results = entityQueryManger.executeQuery(query, adminUserInfo);
		assertNotNull(results);
		assertNotNull(results.getEntities());
		assertEquals(1, results.getEntities().size());
		// there should be exactly 1
		assertTrue(results.getTotalEntityCount() == 1);
	}
	
	@Test
	public void testQuerySelectStarWithAnnotations(){
		BasicQuery query = new BasicQuery();
		query.setFrom("table");
		// select * from the table.
		query.addExpression(
				new Expression(
						new CompoundId(null, NodeField.ID.getFieldName())
						, Comparator.EQUALS
						, table.getId()));
		NodeQueryResults results = entityQueryManger.executeQuery(query, adminUserInfo);
		assertNotNull(results);
		assertEquals(1L, results.getTotalNumberOfResults());
		Map<String, Object> row = results.getAllSelectedData().get(0);
		// validate the annotation.
		Object value = row.get("aString");
		validateAnnotationValue(value, "foo bar");
		value = row.get("aDate");
		validateAnnotationValue(value, 100L);
		value = row.get("aDouble");
		validateAnnotationValue(value, 1.1234);
		value = row.get("aLong");
		validateAnnotationValue(value, 123L);
	}
	
	@Test
	public void testQuerySelectAnnotations(){
		BasicQuery query = new BasicQuery();
		query.setSelect(Lists.newArrayList("aDouble", "doesNotExist"));
		query.setFrom("table");
		// select * from the table.
		query.addExpression(
				new Expression(
						new CompoundId(null, NodeField.ID.getFieldName())
						, Comparator.EQUALS
						, table.getId()));
		NodeQueryResults results = entityQueryManger.executeQuery(query, adminUserInfo);
		assertNotNull(results);
		assertEquals(1L, results.getTotalNumberOfResults());
		Map<String, Object> row = results.getAllSelectedData().get(0);
		// validate the annotation.
		Object value = row.get("aDouble");
		validateAnnotationValue(value, 1.1234);
		value = row.get("doesNotExist");
		validateAnnotationValue(value, null);
	}
	
	/**
	 * Annotation values must be lists of 
	 * @param object
	 * @param expected
	 */
	public <T> void validateAnnotationValue(Object object, T expected){
		assertTrue("Expected annotation values to be a list", object instanceof List);
		List list = (List) object;
		assertEquals(1, list.size());
		Object single = list.get(0);
		assertEquals(expected, single);
	}
	
	

	private Set<String> entityQueryResultToEntityTypes(List<EntityQueryResult> entityQueryResults) {
		Set<String> typeSet = new HashSet<String>();
		for (EntityQueryResult result: entityQueryResults) {
			typeSet.add(result.getEntityType());
		}
		return typeSet;
	}
}
