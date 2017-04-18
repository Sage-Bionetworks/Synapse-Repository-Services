package org.sagebionetworks.repo.model.query.entity;

import static org.junit.Assert.*;

import java.util.LinkedList;

import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.repo.model.query.Comparator;
import org.sagebionetworks.repo.model.query.CompoundId;
import org.sagebionetworks.repo.model.query.Expression;
import org.sagebionetworks.repo.model.query.jdo.NodeField;

import com.google.common.collect.Lists;

public class TablesTest {
	
	Expression nodeExpression;
	Expression annotationExpression;
	String sortColumnName;
	int sortIndex;
	boolean sortIsAscending;
	SortList sortList;
	
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
		// default with no sort
		sortColumnName = null;
		sortIndex = 0;
		sortIsAscending = false;
		sortList = new SortList(sortIndex, sortColumnName, sortIsAscending);
	}
	
	@Test
	public void testNodeFieldOnly(){
		// create with node fields only.
		Tables tables = new Tables(new ExpressionList(Lists.newArrayList(nodeExpression)), sortList);
		assertEquals(" FROM ENTITY_REPLICATION R",tables.toSql());
	}
	
	@Test
	public void testAnnotationOnly(){
		// create with node fields only.
		Tables tables = new Tables(new ExpressionList(Lists.newArrayList(annotationExpression)), sortList);
		String sql = tables.toSql();
		assertEquals(" FROM ENTITY_REPLICATION R JOIN ANNOTATION_REPLICATION A0"
				+ " ON (R.ID = A0.ENTITY_ID AND A0.ANNO_KEY = :bJoinName0)", sql);
	}
	
	@Test
	public void testNodeFieldsAndAnnotationOnly(){
		// create with node fields only.
		Tables tables = new Tables(new ExpressionList(Lists.newArrayList(annotationExpression, nodeExpression)), sortList);
		String sql = tables.toSql();
		assertEquals(" FROM ENTITY_REPLICATION R JOIN ANNOTATION_REPLICATION A0"
				+ " ON (R.ID = A0.ENTITY_ID AND A0.ANNO_KEY = :bJoinName0)", sql);
	}
	
	@Test
	public void testSortOnAnnotation(){
		sortIndex = 0;
		sortColumnName = "foo";
		sortList = new SortList(sortIndex, sortColumnName, sortIsAscending);
		Tables tables = new Tables(new ExpressionList(new LinkedList<Expression>()), sortList);
		String sql = tables.toSql();
		assertEquals(" FROM ENTITY_REPLICATION R LEFT JOIN ANNOTATION_REPLICATION A0"
				+ " ON (R.ID = A0.ENTITY_ID AND A0.ANNO_KEY = :bJoinName0)", sql);
	}
	
	@Test
	public void testSortOnNode(){
		sortIndex = 0;
		sortColumnName = NodeField.NAME.getFieldName();
		sortList = new SortList(sortIndex, sortColumnName, sortIsAscending);
		Tables tables = new Tables(new ExpressionList(new LinkedList<Expression>()), sortList);
		String sql = tables.toSql();
		// do not need an annotation join when sorting on a node field
		assertEquals(" FROM ENTITY_REPLICATION R", sql);
	}
	
	@Test
	public void testSortOnAnnotationWithAnnotationExpresion(){
		sortIndex = 1;
		sortColumnName = "foo";
		sortList = new SortList(sortIndex, sortColumnName, sortIsAscending);
		Tables tables = new Tables(new ExpressionList(Lists.newArrayList(annotationExpression)), sortList);
		String sql = tables.toSql();
		assertEquals(" FROM ENTITY_REPLICATION R"
				+ " JOIN ANNOTATION_REPLICATION A0"
				+ " ON (R.ID = A0.ENTITY_ID AND A0.ANNO_KEY = :bJoinName0)"
				+ " LEFT JOIN ANNOTATION_REPLICATION A1"
				+ " ON (R.ID = A1.ENTITY_ID AND A1.ANNO_KEY = :bJoinName1)", sql);
	}

}
