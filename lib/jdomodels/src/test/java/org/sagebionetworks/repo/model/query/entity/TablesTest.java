package org.sagebionetworks.repo.model.query.entity;

import static org.junit.Assert.assertEquals;

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
	boolean sortIsAscending;
	SortList sortList;
	IndexProvider indexProvider;
	SelectList select;
	
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
		indexProvider = new IndexProvider();
		// default with no sort
		sortColumnName = null;
		sortIsAscending = false;
		sortList = new SortList(sortColumnName, sortIsAscending, indexProvider);
		select = new SelectList(Lists.newArrayList(NodeField.ID.getFieldName()), indexProvider);
	}
	
	@Test
	public void testNodeFieldOnly(){
		// create with node fields only.
		Tables tables = new Tables(
				select,
				new ExpressionList(Lists.newArrayList(nodeExpression), indexProvider),
				sortList);
		assertEquals(" FROM ENTITY_REPLICATION "+Constants.ENTITY_REPLICATION_ALIAS,tables.toSql());
	}
	
	@Test
	public void testSelectAnnotation(){
		select = new SelectList(Lists.newArrayList("annotationName"), indexProvider);
		Tables tables = new Tables(
				select,
				new ExpressionList(new LinkedList<Expression>(), indexProvider),
				sortList);
		String sql = tables.toSql();
		assertEquals(" FROM ENTITY_REPLICATION E LEFT JOIN ANNOTATION_REPLICATION A1"
				+ " ON (E.ID = A1.ENTITY_ID AND A1.ANNO_KEY = :bJoinName1)", sql);
	}
	
	@Test
	public void testAnnotationOnly(){
		// create with node fields only.
		Tables tables = new Tables(
				select,
				new ExpressionList(Lists.newArrayList(annotationExpression), indexProvider),
				sortList);
		String sql = tables.toSql();
		assertEquals(" FROM ENTITY_REPLICATION E JOIN ANNOTATION_REPLICATION A1"
				+ " ON (E.ID = A1.ENTITY_ID AND A1.ANNO_KEY = :bJoinName1)", sql);
	}
	
	@Test
	public void testNodeFieldsAndAnnotationOnly(){
		// create with node fields only.
		Tables tables = new Tables(select,
				new ExpressionList(Lists.newArrayList(annotationExpression, nodeExpression), indexProvider),
				sortList);
		String sql = tables.toSql();
		assertEquals(" FROM ENTITY_REPLICATION E JOIN ANNOTATION_REPLICATION A1"
				+ " ON (E.ID = A1.ENTITY_ID AND A1.ANNO_KEY = :bJoinName1)", sql);
	}
	
	@Test
	public void testSortOnAnnotation(){
		sortColumnName = "foo";
		sortList = new SortList(sortColumnName, sortIsAscending, indexProvider);
		Tables tables = new Tables(select,
				new ExpressionList(new LinkedList<Expression>(), indexProvider),
				sortList);
		String sql = tables.toSql();
		assertEquals(" FROM ENTITY_REPLICATION E LEFT JOIN ANNOTATION_REPLICATION A1"
				+ " ON (E.ID = A1.ENTITY_ID AND A1.ANNO_KEY = :bJoinName1)", sql);
	}
	
	@Test
	public void testSortOnNode(){
		sortColumnName = NodeField.NAME.getFieldName();
		sortList = new SortList(sortColumnName, sortIsAscending, indexProvider);
		Tables tables = new Tables(select,
				new ExpressionList(new LinkedList<Expression>(), indexProvider),
				sortList);
		String sql = tables.toSql();
		// do not need an annotation join when sorting on a node field
		assertEquals(" FROM ENTITY_REPLICATION "+Constants.ENTITY_REPLICATION_ALIAS, sql);
	}
	
	@Test
	public void testSortOnAnnotationWithAnnotationExpresion(){
		sortColumnName = "foo";
		sortList = new SortList(sortColumnName, sortIsAscending, indexProvider);
		Tables tables = new Tables(select,
				new ExpressionList(Lists.newArrayList(annotationExpression), indexProvider),
				sortList);
		String sql = tables.toSql();
		assertEquals(" FROM ENTITY_REPLICATION E"
				+ " JOIN ANNOTATION_REPLICATION A2"
				+ " ON (E.ID = A2.ENTITY_ID AND A2.ANNO_KEY = :bJoinName2)"
				+ " LEFT JOIN ANNOTATION_REPLICATION A1"
				+ " ON (E.ID = A1.ENTITY_ID AND A1.ANNO_KEY = :bJoinName1)", sql);
	}

}
