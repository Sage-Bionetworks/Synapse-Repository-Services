package org.sagebionetworks.repo.model.query.jdo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.query.BasicQuery;
import org.sagebionetworks.repo.model.query.Comparator;
import org.sagebionetworks.repo.model.query.CompoundId;
import org.sagebionetworks.repo.model.query.Expression;
import org.sagebionetworks.repo.model.query.entity.NodeQueryDaoFactory;
import org.sagebionetworks.repo.model.query.entity.NodeQueryDaoV2;
import org.sagebionetworks.repo.model.query.entity.QueryModel;
import org.sagebionetworks.repo.model.table.AnnotationDTO;
import org.sagebionetworks.repo.model.table.AnnotationType;
import org.sagebionetworks.repo.model.table.EntityDTO;
import org.sagebionetworks.table.cluster.ConnectionFactory;
import org.sagebionetworks.table.cluster.TableIndexDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.google.common.collect.Lists;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class NodeQueryDaoV2ImplTest {
	@Autowired
	StackConfiguration config;

	@Autowired
	ConnectionFactory connectionFactory;
	@Autowired
	NodeQueryDaoFactory nodeQueryDaoFactory;
	
	@Mock
	ProgressCallback mockProgressCallback;
	
	TableIndexDAO tableIndexDao;
	NodeQueryDaoV2 nodeQueryDaoV2;
	
	BasicQuery query;
	
	List<EntityDTO> entities;
	
	Expression projectIdExpression;
	
	@Before
	public void before(){
		
		// Only the ProgressCallback is mocked for this test.  All other dependencies are autowired.
		MockitoAnnotations.initMocks(this);
		// create the dao from the connection.
		nodeQueryDaoV2 = nodeQueryDaoFactory.createConnection();
		tableIndexDao = connectionFactory.getFirstConnection();
		// delete all data
		tableIndexDao.deleteEntityData(mockProgressCallback, Lists.newArrayList(1L,2L,3L,4L,5L));
		
		// setup some hierarchy.
		// project
		entities = new LinkedList<EntityDTO>();
		EntityDTO project = createEntityDTO(1L, EntityType.project, 0);
		project.setParentId(null);
		project.setBenefactorId(project.getId());
		project.setProjectId(project.getId());
		entities.add(project);
		// folder
		EntityDTO folder = createEntityDTO(2L, EntityType.folder, 0);
		folder.setParentId(project.getId());
		folder.setBenefactorId(project.getId());
		folder.setProjectId(project.getId());
		entities.add(folder);
		
		// add some files
		for(long id=3; id<6; id++){
			EntityDTO file = createEntityDTO(id, EntityType.file, 3);
			file.setParentId(folder.getId());
			file.setBenefactorId(project.getId());
			file.setProjectId(project.getId());
			entities.add(file);
		}

		// populate the tables that will be queried.
		tableIndexDao.addEntityData(mockProgressCallback, entities);
		projectIdExpression = new Expression(
				new CompoundId(null, NodeField.PROJECT_ID.getFieldName())
				, Comparator.EQUALS, "syn"+project.getId());
	}
	
	@Test
	public void testSelectStar(){
		// select * query with no filters or sort.
		BasicQuery query = new BasicQuery();
		query.setSelect(null);
		query.setFilters(null);
		query.setSort(null);
		QueryModel model = new QueryModel(query);
		List<Map<String, Object>> results = nodeQueryDaoV2.executeQuery(model);
		assertNotNull(results);
		assertTrue(results.size() >= 5);
		long count = nodeQueryDaoV2.executeCountQuery(model);
		assertTrue(count >= 5);
	}
	
	@Test
	public void testExecuteQuerySelectStarSingleRow(){
		EntityDTO project = entities.get(0);
		// select * query with no filters or sort.
		BasicQuery query = new BasicQuery();
		query.setSelect(null);
		// id=projectId
		Expression equalsId = new Expression(
				new CompoundId(null, NodeField.ID.getFieldName())
				, Comparator.EQUALS
				, project.getId());
		query.setFilters(Lists.newArrayList(equalsId));
		query.setSort(null);
		QueryModel model = new QueryModel(query);
		List<Map<String, Object>> results = nodeQueryDaoV2.executeQuery(model);
		assertNotNull(results);
		assertEquals(1, results.size());

		// Validate all of the fields are mapped
		Map<String, Object> row = results.get(0);
		assertEquals(project.getId(), row.get(NodeField.ID.getFieldName()));
		assertEquals(project.getCurrentVersion(), row.get(NodeField.VERSION_NUMBER.getFieldName()));
		// version label is mapped to current version
		assertEquals(project.getCurrentVersion(), row.get(NodeField.VERSION_LABEL.getFieldName()));
		// version comment is mapped to current version.
		assertEquals(project.getCurrentVersion(), row.get(NodeField.VERSION_COMMENT.getFieldName()));
		assertEquals(project.getName(), row.get(NodeField.NAME.getFieldName()));
		assertEquals(project.getBenefactorId(), row.get(NodeField.BENEFACTOR_ID.getFieldName()));
		assertEquals(project.getParentId(), row.get(NodeField.PARENT_ID.getFieldName()));
		assertEquals(project.getProjectId(), row.get(NodeField.PROJECT_ID.getFieldName()));
		assertEquals(project.getCreatedBy(), row.get(NodeField.CREATED_BY.getFieldName()));
		assertEquals(project.getCreatedOn().getTime(), row.get(NodeField.CREATED_ON.getFieldName()));
		assertEquals(project.getModifiedBy(), row.get(NodeField.MODIFIED_BY.getFieldName()));
		assertEquals(project.getModifiedOn().getTime(), row.get(NodeField.MODIFIED_ON.getFieldName()));
		assertEquals(project.getType().name(), row.get(NodeField.NODE_TYPE.getFieldName()));
		// alias is always null
		assertEquals(null, row.get(NodeField.ALIAS_ID.getFieldName()));
	}
	
	@Test
	public void testAnnotationsToResults(){
		// select * query with no filters or sort.
		BasicQuery query = new BasicQuery();
		query.setSelect(null);
		// type = file
		Expression equalsId = new Expression(
				new CompoundId(null, NodeField.NODE_TYPE.getFieldName())
				, Comparator.EQUALS
				, EntityType.file.name());
		query.setFilters(Lists.newArrayList(equalsId, projectIdExpression));
		query.setSort(NodeField.ID.getFieldName());
		query.setAscending(true);
		QueryModel model = new QueryModel(query);
		List<Map<String, Object>> results = nodeQueryDaoV2.executeQuery(model);
		assertNotNull(results);
		assertEquals(3, results.size());
		nodeQueryDaoV2.addAnnotationsToResults(results);
		// are the annotations added?
		Map<String, Object> row = results.get(0);
		assertEquals("0", row.get("key0"));
		assertEquals("1", row.get("key1"));
		assertEquals("2", row.get("key2"));
		
	}
	
	/**
	 * Test for PLFM-4367
	 */
	@Test
	public void testAnnotationsToResultsEmpty(){
		// call under test
		nodeQueryDaoV2.addAnnotationsToResults(new LinkedList<Map<String,Object>>());	
	}
	
	@Test
	public void testExecuteQuerySortOnAnnotation(){
		BasicQuery query = new BasicQuery();
		query.setSelect(Lists.newArrayList(NodeField.ID.getFieldName()));
		query.setSort("key0");
		query.addExpression(projectIdExpression);
		QueryModel model = new QueryModel(query);
		List<Map<String, Object>> results = nodeQueryDaoV2.executeQuery(model);
		assertNotNull(results);
		assertEquals(5, results.size());
	}
	
	@Test
	public void testExecuteQuerySelectAnnotation(){
		BasicQuery query = new BasicQuery();
		query.setSelect(Lists.newArrayList("key0"));
		query.setSort(null);
		query.addExpression(projectIdExpression);
		QueryModel model = new QueryModel(query);
		List<Map<String, Object>> results = nodeQueryDaoV2.executeQuery(model);
		assertNotNull(results);
		assertEquals(5, results.size());
		int countNulls = 0;
		int countNonNulls = 0;;
		for(Map<String, Object> row: results){
			Object value = row.get("key0");
			if(value == null){
				countNulls++;
			}else{
				countNonNulls ++;
				assertEquals("0", value);
			}
		}
		assertEquals("Two rwos should have null annotation values",2, countNulls);
		assertEquals("Three rows should have non-null annotation values",3, countNonNulls);
	}
	
	@Test
	public void testGetDistinctBenefactors(){
		BasicQuery query = new BasicQuery();
		query.setSelect(Lists.newArrayList("key0"));
		query.setSort(null);
		query.addExpression(projectIdExpression);
		QueryModel model = new QueryModel(query);
		long limit = 100;
		Set<Long> benefactors = nodeQueryDaoV2.getDistinctBenefactors(model, limit);
		assertNotNull(benefactors);
		assertEquals(1,  benefactors.size());
		Long projectId = 1L;
		assertTrue(benefactors.contains(projectId));
	}
	
	/**
	 * Helper to create populated EntityDTO.
	 * @param id
	 * @param type
	 * @param annotationCount
	 * @return
	 */
	private EntityDTO createEntityDTO(long id, EntityType type, int annotationCount){
		EntityDTO entityDto = new EntityDTO();
		entityDto.setId(id);
		entityDto.setCurrentVersion(2L);
		entityDto.setCreatedBy(222L);
		entityDto.setCreatedOn(new Date());
		entityDto.setEtag("etag"+id);
		entityDto.setName("name"+id);
		entityDto.setType(type);
		entityDto.setParentId(1L);
		entityDto.setBenefactorId(2L);
		entityDto.setProjectId(3L);
		entityDto.setModifiedBy(333L);
		entityDto.setModifiedOn(new Date());
		if(EntityType.file.equals(type)){
			entityDto.setFileHandleId(888L);
		}
		List<AnnotationDTO> annos = new LinkedList<AnnotationDTO>();
		for(int i=0; i<annotationCount; i++){
			AnnotationDTO annoDto = new AnnotationDTO();
			annoDto.setEntityId(id);
			annoDto.setKey("key"+i);
			annoDto.setType(AnnotationType.values()[i%AnnotationType.values().length]);
			annoDto.setValue(""+i);
			annos.add(annoDto);
		}
		if(!annos.isEmpty()){
			entityDto.setAnnotations(annos);
		}
		return entityDto;
	}

}
