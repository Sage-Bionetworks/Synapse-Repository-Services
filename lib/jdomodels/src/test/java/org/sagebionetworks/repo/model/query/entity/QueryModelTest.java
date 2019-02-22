package org.sagebionetworks.repo.model.query.entity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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
		assertTrue(sql.contains("JOIN ANNOTATION_REPLICATION A2"));
		assertTrue(sql.contains("WHERE E.CREATED_BY = :"));
		assertTrue(sql.contains(" ORDER BY A3.STRING_VALUE ASC"));
		assertTrue(sql.contains("LIMIT :bLimit OFFSET :bOffset"));
	}
	

	@Test
	public void testNoExpressionNoSort(){
		query.setFilters(null);
		query.setSort(null);
		QueryModel model = new QueryModel(query);
		assertEquals("SELECT E.ID AS 'id' FROM ENTITY_REPLICATION E LIMIT :bLimit OFFSET :bOffset", model.toSql());
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
				+ " FROM ENTITY_REPLICATION E"
				+ " JOIN ANNOTATION_REPLICATION A2"
				+ " ON (E.ID = A2.ENTITY_ID AND A2.ANNO_KEY = :bJoinName2)"
				+ " LEFT JOIN ANNOTATION_REPLICATION A3"
				+ " ON (E.ID = A3.ENTITY_ID AND A3.ANNO_KEY = :bJoinName3)"
				+ " WHERE E.CREATED_BY = :bExpressionValue1 AND A2.STRING_VALUE > :bExpressionValue2", count);
	}
	
	@Test
	public void testToDistinctBenefactorSql(){
		QueryModel model = new QueryModel(query);
		long limit = 101;
		String count = model.toDistinctBenefactorSql(limit);
		assertEquals("SELECT DISTINCT BENEFACTOR_ID"
				+ " FROM ENTITY_REPLICATION E"
				+ " JOIN ANNOTATION_REPLICATION A2"
				+ " ON (E.ID = A2.ENTITY_ID AND A2.ANNO_KEY = :bJoinName2)"
				+ " LEFT JOIN ANNOTATION_REPLICATION A3"
				+ " ON (E.ID = A3.ENTITY_ID AND A3.ANNO_KEY = :bJoinName3)"
				+ " WHERE E.CREATED_BY = :bExpressionValue1 AND A2.STRING_VALUE > :bExpressionValue2 LIMIT 101", count);
	}

}
