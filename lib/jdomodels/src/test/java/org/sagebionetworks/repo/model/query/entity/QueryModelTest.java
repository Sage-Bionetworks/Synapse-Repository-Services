package org.sagebionetworks.repo.model.query.entity;

import static org.junit.Assert.*;

import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.repo.model.query.BasicQuery;
import org.sagebionetworks.repo.model.query.Comparator;
import org.sagebionetworks.repo.model.query.CompoundId;
import org.sagebionetworks.repo.model.query.Expression;
import org.sagebionetworks.repo.model.query.jdo.NodeField;

import com.google.common.collect.Lists;

public class QueryModelTest {
	
	BasicQuery query;
	
	Expression nodeExpression;
	Expression annotationExpression;
	Parameters parameters;
	
	@Before
	public void before(){
		nodeExpression = new Expression(
				new CompoundId(null, NodeField.CREATED_BY.getFieldName())
				, Comparator.EQUALS
				, 123L);
		annotationExpression = new Expression(
				new CompoundId(null, "foo")
				, Comparator.GREATER_THAN
				, 456L);
		
		query = new BasicQuery();
		query.setSelect(Lists.newArrayList(NodeField.ID.getFieldName()));
		query.setFilters(Lists.newArrayList(nodeExpression, annotationExpression));
		query.setSort("bar");
		parameters = new Parameters();
	}
	
	@Test
	public void testAllParts(){
		QueryModel model = new QueryModel(query);
		String sql = model.toSql();
		assertTrue(sql.contains("SELECT"));
		assertTrue(sql.contains("FROM ENTITY_REPLICATION"));
		assertTrue(sql.contains("JOIN ANNOTATION_REPLICATION A1"));
		assertTrue(sql.contains("WHERE E.CREATED_BY = :"));
		assertTrue(sql.contains(" ORDER BY A2.ANNO_VALUE ASC"));
		assertTrue(sql.contains("LIMIT :bLimit OFFSET :bOffset"));
	}
	

	@Test
	public void testNoExpressionNoSort(){
		query.setFilters(null);
		query.setSort(null);
		QueryModel model = new QueryModel(query);
		assertEquals("SELECT E.ID AS 'id' FROM ENTITY_REPLICATION R LIMIT :bLimit OFFSET :bOffset", model.toSql());
	}
	
	@Test
	public void testBindParameters(){
		QueryModel model = new QueryModel(query);
		model.bindParameters(parameters);
		Map<String, Object> map = parameters.getParameters();
		// 2 per expression * 2 expression + 1 limit + 1 offset
		assertEquals(6, map.size());
	}
	
	@Test
	public void testCountQuery(){
		QueryModel model = new QueryModel(query);
		String count = model.toCountSql();
		assertEquals("SELECT COUNT(*)"
				+ " FROM ENTITY_REPLICATION R"
				+ " JOIN ANNOTATION_REPLICATION A1"
				+ " ON (R.ID = A1.ENTITY_ID AND A1.ANNO_KEY = :bJoinName1)"
				+ " LEFT JOIN ANNOTATION_REPLICATION A2"
				+ " ON (R.ID = A2.ENTITY_ID AND A2.ANNO_KEY = :bJoinName2)"
				+ " WHERE E.CREATED_BY = :bExpressionValue0 AND A1.ANNO_VALUE > :bExpressionValue1", count);
	}

}
