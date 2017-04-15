package org.sagebionetworks.repo.model.query.entity;

import static org.junit.Assert.*;

import java.util.LinkedList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.repo.model.query.Comparator;
import org.sagebionetworks.repo.model.query.CompoundId;
import org.sagebionetworks.repo.model.query.Expression;
import org.sagebionetworks.repo.model.query.jdo.NodeField;

import com.google.common.collect.Lists;

public class ExpressionListTest {
	
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
	}
	
	@Test
	public void testNodeFieldAndAnnotation(){
		ExpressionList list = new ExpressionList(Lists.newArrayList(nodeExpression, annotationExpression));
		assertEquals(" WHERE E.CREATED_BY = :b0 AND A1.ANNO_VALUE > :b1",list.toSql());
		assertEquals(2, list.getSize());
	}
	
	@Test
	public void testGetAnnotationNodeOnly(){
		ExpressionList list = new ExpressionList(Lists.newArrayList(nodeExpression));
		List<SqlExpression> annos = list.getAnnotationExpressions();
		assertNotNull(annos);
		assertEquals(0, annos.size());
	}
	
	@Test
	public void testGetAnnotationAnnotationOnly(){
		ExpressionList list = new ExpressionList(Lists.newArrayList(annotationExpression));
		List<SqlExpression> annos = list.getAnnotationExpressions();
		assertNotNull(annos);
		assertEquals(1, annos.size());
		SqlExpression annoExpression = annos.get(0);
		assertEquals("A0", annoExpression.leftHandSide.getAnnotationAlias());
	}
	
	@Test
	public void testGetAnnotationAnnotationMixed(){
		ExpressionList list = new ExpressionList(Lists.newArrayList(nodeExpression, annotationExpression));
		List<SqlExpression> annos = list.getAnnotationExpressions();
		assertNotNull(annos);
		assertEquals(1, annos.size());
		SqlExpression annoExpression = annos.get(0);
		assertEquals("A1", annoExpression.leftHandSide.getAnnotationAlias());
	}
	
	@Test
	public void testEmptyList(){
		ExpressionList list = new ExpressionList(new LinkedList<Expression>());
		assertEquals("",list.toSql());
		List<SqlExpression> annos = list.getAnnotationExpressions();
		assertNotNull(annos);
		assertEquals(0, list.getSize());
	}
	
	@Test
	public void testNullList(){
		ExpressionList list = new ExpressionList(null);
		assertEquals("",list.toSql());
		assertEquals(0, list.getSize());
	}

}
