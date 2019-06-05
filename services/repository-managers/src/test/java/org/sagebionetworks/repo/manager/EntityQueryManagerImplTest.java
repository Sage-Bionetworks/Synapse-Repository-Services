package org.sagebionetworks.repo.manager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySetOf;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.NodeQueryResults;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.entity.query.DateValue;
import org.sagebionetworks.repo.model.entity.query.EntityFieldName;
import org.sagebionetworks.repo.model.entity.query.EntityQueryResult;
import org.sagebionetworks.repo.model.entity.query.IntegerValue;
import org.sagebionetworks.repo.model.entity.query.StringValue;
import org.sagebionetworks.repo.model.entity.query.Value;
import org.sagebionetworks.repo.model.query.BasicQuery;
import org.sagebionetworks.repo.model.query.Comparator;
import org.sagebionetworks.repo.model.query.entity.ExpressionList;
import org.sagebionetworks.repo.model.query.entity.NodeQueryDaoFactory;
import org.sagebionetworks.repo.model.query.entity.NodeQueryDaoV2;
import org.sagebionetworks.repo.model.query.entity.QueryModel;
import org.sagebionetworks.repo.model.query.entity.SqlExpression;
import org.sagebionetworks.repo.model.query.jdo.NodeField;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * The unit test for EntityQueryManagerImpl.
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class EntityQueryManagerImplTest {

	@Mock
	NodeQueryDaoV2 mockDao;
	@Mock
	NodeQueryDaoFactory nodeQueryDaoFactory;
	@Mock
	AuthorizationManager mockAuthorizationManager;
	@Mock
	UserInfo mockUser;
	@InjectMocks
	EntityQueryManagerImpl manager;
	
	EntityQueryResult result;
	NodeQueryResults sampleResutls;
	
	BasicQuery query;

	Set<Long> queryBenefactorIds;
	Set<Long> authroizedBenefactorIds;
	List<Map<String, Object>> pageResult;
	long count;
	
	@Before
	public void before(){
		when(nodeQueryDaoFactory.createConnection()).thenReturn(mockDao);
		// Sample
		result = new EntityQueryResult();
		result.setActivityId(null);
		result.setCreatedByPrincipalId(123L);
		result.setCreatedOn(new Date(1L));
		result.setModifiedByPrincipalId(456L);
		result.setModifiedOn(new Date(2));
		result.setEntityType(EntityType.table.name());
		result.setEtag("etag");
		result.setName("aName");
		result.setParentId("syn99");
		result.setVersionNumber(0L);
		result.setId("syn456");
		result.setProjectId(888L);
		result.setBenefactorId(111L);
		
		query = new BasicQuery();
		query.setFrom("project");
		// default to non-admin
		when(mockUser.isAdmin()).thenReturn(false);
		queryBenefactorIds = Sets.newHashSet(1L,2L,3L);
		when(mockDao.getDistinctBenefactors(any(QueryModel.class), anyLong())).thenReturn(queryBenefactorIds);
		authroizedBenefactorIds = Sets.newHashSet(2L,1L);
		when(mockAuthorizationManager.getAccessibleBenefactors(mockUser, queryBenefactorIds)).thenReturn(authroizedBenefactorIds);
		
		pageResult = new LinkedList<Map<String,Object>>();
		Map<String, Object> row = new HashMap<String, Object>();
		row.put(NodeField.NAME.getFieldName(), "name1");
		row.put(NodeField.ID.getFieldName(), 123L);
		row.put(NodeField.PARENT_ID.getFieldName(), 456L);
		pageResult.add(row);
		when(mockDao.executeQuery(any(QueryModel.class))).thenReturn(pageResult);
		count = 100;
		when(mockDao.executeCountQuery(any(QueryModel.class))).thenReturn(count);
	}
	
	@Test
	public void testTranslateEntityQueryResultRoundTrip(){
		Map<String, Object> entityMap = toMap(result);
		EntityQueryResult clone = manager.translate(entityMap);
		assertEquals(result, clone);
	}
	
	@Test
	public void testTranslateValueString(){
		StringValue sv = new StringValue();
		String in = "a string";
		sv.setValue(in);
		String out = (String) manager.translateValue(sv);
		assertEquals(in, out);
	}
	
	@Test
	public void testTranslateValueDate(){
		DateValue value = new DateValue();
		Date in = new Date(99);
		value.setValue(in);
		Long out = (Long) manager.translateValue(value);
		assertEquals(new Long(in.getTime()), out);
	}
	
	@Test
	public void testTranslateValueInteger(){
		IntegerValue value = new IntegerValue();
		Long in = 99L;
		value.setValue(in);
		Long out = (Long) manager.translateValue(value);
		assertEquals(in, out);
	}
	
	@Test
	public void testTranslateListSizeOne(){
		List<Value> list = new ArrayList<Value>(1);
		StringValue sv = new StringValue();
		String in = "a string";
		sv.setValue(in);
		list.add(sv);
		String out = (String) manager.translateValue(list);
		assertEquals(in, out);
	}
	
	@Test
	public void testTranslateListMoreThanOne(){
		List<Value> list = new ArrayList<Value>(2);
		//1
		StringValue sv = new StringValue();
		String in1 = "one";
		sv.setValue(in1);
		list.add(sv);
		//2
		StringValue sv2 = new StringValue();
		String in2 = "two";
		sv2.setValue(in2);
		list.add(sv2);
		// Should be list of stringss
		List<String> out = (List<String>) manager.translateValue(list);
		assertEquals(Arrays.asList(in1, in2), out);
	}
	
	/**
	 * Helper to create map for a result
	 * @param results
	 * @return
	 */
	private Map<String, Object> toMap(EntityQueryResult results){
		Map<String, Object> map = new HashMap<String, Object>();
		map.put(EntityFieldName.id.name(), results.getId());
		map.put(EntityFieldName.name.name(), result.getName());
		map.put(EntityFieldName.parentId.name(), results.getParentId());
		map.put(EntityFieldName.eTag.name(), results.getEtag());
		map.put(EntityFieldName.createdOn.name(), result.getCreatedOn().getTime());
		map.put(EntityFieldName.createdByPrincipalId.name(), results.getCreatedByPrincipalId());
		map.put(EntityFieldName.modifiedOn.name(), results.getModifiedOn().getTime());
		map.put(EntityFieldName.modifiedByPrincipalId.name(), results.getModifiedByPrincipalId());
		map.put(EntityFieldName.activityId.name(), results.getActivityId());
		map.put(EntityFieldName.versionNumber.name(), results.getVersionNumber());
		map.put(EntityFieldName.benefactorId.name(), results.getBenefactorId());
		map.put(EntityFieldName.projectId.name(), results.getProjectId());
		
		EntityType type = EntityType.valueOf(results.getEntityType());
		map.put("nodeType", type.name());
		return map;
	}
	
	@Test
	public void testExecuteQueryNonAdminSelectStar(){
		when(mockUser.isAdmin()).thenReturn(false);
		// call under test
		NodeQueryResults results = manager.executeQuery(query, mockUser);
		assertNotNull(results);
		assertEquals(count, results.getTotalNumberOfResults());
		assertNotNull(results.getResultIds());
		assertEquals(1, results.getResultIds().size());
		assertEquals("syn123", results.getResultIds().get(0));
		assertNotNull(results.getAllSelectedData());
		assertEquals(1, results.getAllSelectedData().size());
		Map<String, Object> row = results.getAllSelectedData().get(0);
		assertNotNull(row);
		assertEquals("name1",row.get(NodeField.NAME.getFieldName()));
		ArgumentCaptor<QueryModel> queryCapture = ArgumentCaptor.forClass(QueryModel.class);
		// call to lookup the benefactors for the query.
		verify(mockDao).getDistinctBenefactors(queryCapture.capture(), eq(EntityQueryManagerImpl.MAX_BENEFACTORS_PER_QUERY+1));
		QueryModel model = queryCapture.getValue();
		assertNotNull(model);
		// The from clause should have been changed to a condition.
		ExpressionList where = model.getWhereClause();
		assertNotNull(where);
		List<SqlExpression> expressions = where.getExpressions();
		assertNotNull(expressions);
		assertEquals(2, expressions.size());
		SqlExpression expression = expressions.get(0);
		assertEquals("E.TYPE",expression.getLeftHandSide().toSql());
		assertEquals("project",expression.getRightHandSide());
		// call to get the sub-set of benefactorIds that the user can see.
		verify(mockAuthorizationManager).getAccessibleBenefactors(mockUser, queryBenefactorIds);
		// Capture the final query
		queryCapture = ArgumentCaptor.forClass(QueryModel.class);
		verify(mockDao).executeQuery(queryCapture.capture());
		model = queryCapture.getValue();
		assertNotNull(model);
		// another expression should be added to the query
		where = model.getWhereClause();
		assertNotNull(where);
		expressions = where.getExpressions();
		assertNotNull(expressions);
		assertEquals(3, expressions.size());
		// the last expression should be a filter on the benefactors the user can see.
		expression = expressions.get(2);
		assertEquals("E.BENEFACTOR_ID",expression.getLeftHandSide().toSql());
		assertEquals(Comparator.IN, expression.getCompare());
		assertEquals(authroizedBenefactorIds, expression.getRightHandSide());
		// This is a select * query so the annotations should be added.
		verify(mockDao).addAnnotationsToResults(pageResult);
		verify(mockDao).executeCountQuery(model);
	}
	
	@Test
	public void testExecuteQueryAdmin(){
		// setup an admin
		when(mockUser.isAdmin()).thenReturn(true);
		// call under test
		NodeQueryResults results = manager.executeQuery(query, mockUser);
		assertNotNull(results);
		// benefactor lookup should not occur
		verify(mockDao, never()).getDistinctBenefactors(any(QueryModel.class), anyLong());
		// the accessible benefactors should not be called.
		verify(mockAuthorizationManager, never()).getAccessibleBenefactors(any(UserInfo.class), anySetOf(Long.class));
	}
	
	@Test
	public void testExecuteQueryNotSelectStar(){
		// not a select *
		query.setSelect(Lists.newArrayList("foo"));
		// call under test
		NodeQueryResults results = manager.executeQuery(query, mockUser);
		assertNotNull(results);
		// annotations should only be added for select *
		verify(mockDao, never()).addAnnotationsToResults(anyList());
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testExecuteQueryOverLimit(){
		query.setLimit(EntityQueryManagerImpl.MAX_LIMIT+1);
		// call under test
		 manager.executeQuery(query, mockUser);
	}
	
	@Test
	public void testExecuteQueryScopeTooBroad(){
		//setup too many benefactor ids
		queryBenefactorIds = new HashSet<>();
		for(long id =0; id<EntityQueryManagerImpl.MAX_BENEFACTORS_PER_QUERY+1; id++){
			queryBenefactorIds.add(id);
		}
		when(mockDao.getDistinctBenefactors(any(QueryModel.class), anyLong())).thenReturn(queryBenefactorIds);
		// call under test
		try{
			 manager.executeQuery(query, mockUser);
			 fail();
		}catch(IllegalArgumentException e){
			assertEquals(EntityQueryManagerImpl.SCOPE_IS_TOO_BROAD, e.getMessage());
		}
	}
	
	@Test
	public void testExecuteQueryNoBenefactors(){
		// return an empty set
		when(mockAuthorizationManager.getAccessibleBenefactors(mockUser, queryBenefactorIds)).thenReturn(new HashSet<Long>());
		NodeQueryResults results = manager.executeQuery(query, mockUser);
		assertNotNull(results);
		assertNotNull(results.getAllSelectedData());
		assertTrue(results.getAllSelectedData().isEmpty());
		assertNotNull(results.getResultIds());
		assertTrue(results.getResultIds().isEmpty());
		assertEquals(0L, results.getTotalNumberOfResults());
		// the query should not be executed.
		verify(mockDao, never()).executeQuery(any(QueryModel.class));
		verify(mockDao, never()).executeCountQuery(any(QueryModel.class));
		verify(mockDao, never()).addAnnotationsToResults(anyList());
	}
	
	/**
	 * Test for PLFM-4367
	 */
	@Test
	public void testExecuteQueryNoResults(){
		// return no results
		when(mockDao.executeQuery(any(QueryModel.class))).thenReturn(new LinkedList<Map<String,Object>>());
		// call under test
		NodeQueryResults results = manager.executeQuery(query, mockUser);
		assertNotNull(results);
		// annotations should not be added, since the list is empty.
		verify(mockDao, never()).addAnnotationsToResults(anyList());
	}
}
