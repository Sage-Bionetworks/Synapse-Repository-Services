package org.sagebionetworks.repo.model.query.entity;

import static org.junit.Assert.*;

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
	}
	
	@Test
	public void testAllParts(){
		QueryModel model = new QueryModel(query);
		String sql = model.toSql();
		assertTrue(sql.contains("SELECT"));
		assertTrue(sql.contains("FROM ENTITY_REPLICATION"));
		assertTrue(sql.contains("JOIN ANNOTATION_REPLICATION A1"));
		assertTrue(sql.contains("WHERE E.CREATED_BY = :b0"));
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

}
