package org.sagebionetworks.repo.model.query.entity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

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
	IndexProvider indexProvider;
	Parameters parameters;
	String bindKey0;
	String bindKey1;

	
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
		parameters = new Parameters();
		bindKey0 = Constants.BIND_PREFIX_EXPRESSION+0;
		bindKey1 = Constants.BIND_PREFIX_EXPRESSION+1;
		indexProvider = new IndexProvider();
	}
	
	@Test
	public void testNodeFieldAndAnnotation(){
		ExpressionList list = new ExpressionList(Lists.newArrayList(nodeExpression, annotationExpression), indexProvider);
		assertEquals(" WHERE E.CREATED_BY = :"+bindKey0+" AND A1.STRING_VALUE > :"+bindKey1,list.toSql());
	}
	
	@Test
	public void testGetAnnotationAnnotationReferences(){
		ExpressionList list = new ExpressionList(Lists.newArrayList(nodeExpression), indexProvider);
		List<ColumnReference> annos = list.getAnnotationReferences();
		assertNotNull(annos);
		assertEquals(0, annos.size());
	}
	
	@Test
	public void testGetAnnotationAnnotationOnly(){
		ExpressionList list = new ExpressionList(Lists.newArrayList(annotationExpression), indexProvider);
		List<ColumnReference> annos = list.getAnnotationReferences();
		assertNotNull(annos);
		assertEquals(1, annos.size());
		ColumnReference ref = annos.get(0);
		assertEquals("A0", ref.getAnnotationAlias());
	}
	
	@Test
	public void testGetAnnotationAnnotationMixed(){
		ExpressionList list = new ExpressionList(Lists.newArrayList(nodeExpression, annotationExpression), indexProvider);
		List<ColumnReference> annos = list.getAnnotationReferences();
		assertNotNull(annos);
		assertEquals(1, annos.size());
		ColumnReference ref = annos.get(0);
		assertEquals("A1", ref.getAnnotationAlias());
	}
	
	@Test
	public void testEmptyList(){
		ExpressionList list = new ExpressionList(new LinkedList<Expression>(), indexProvider);
		assertEquals("",list.toSql());
		List<ColumnReference> annos = list.getAnnotationReferences();
		assertNotNull(annos);
	}
	
	@Test
	public void testNullList(){
		ExpressionList list = new ExpressionList(null, indexProvider);
		assertEquals("",list.toSql());
	}
	
	@Test
	public void testBindParameters(){
		ExpressionList list = new ExpressionList(Lists.newArrayList(annotationExpression, nodeExpression), indexProvider);
		list.bindParameters(parameters);
		Map<String, Object> map = parameters.getParameters();
		assertEquals(456L, map.get(bindKey0));
		assertEquals(123L, map.get(bindKey1));
	}

}
