package org.sagebionetworks.repo.model.query.entity;

import static org.junit.Assert.*;

import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.repo.model.query.Comparator;

import com.google.common.collect.Lists;

public class SqlExpressionTest {
	
	int index;
	ColumnReference nameReference;
	String annotationName;
	ColumnReference annotationRefrence;
	Parameters parameters;
	String bindKey;
	
	@Before
	public void before(){
		index = 5;
		nameReference = new ColumnReference(NodeToEntity.name.name(), index);
		annotationName = "foo";
		annotationRefrence =new ColumnReference(annotationName, index);
		parameters = new Parameters();
		bindKey = Constants.BIND_PREFIX_EXPRESSION+index;
	}

	@Test
	public void testNodeField(){
		Comparator comparator = Comparator.EQUALS;
		String rhs = "someName";
		SqlExpression expression = new SqlExpression(nameReference, comparator, rhs);
		assertEquals("E.NAME = :"+bindKey, expression.toSql());
	}
	
	@Test
	public void testAnnotation(){
		Comparator comparator = Comparator.GREATER_THAN_OR_EQUALS;
		Integer rhs = 123;
		SqlExpression expression = new SqlExpression(annotationRefrence, comparator, rhs);
		assertEquals("A5.ANNO_VALUE >= :"+bindKey, expression.toSql());
	}
	
	@Test
	public void testInClause(){
		ColumnReference lfs = new ColumnReference(NodeToEntity.benefactorId.name(), index);
		Comparator comparator = Comparator.IN;
		List<Long> rhs = Lists.newArrayList(123L, 456L);

		SqlExpression expression = new SqlExpression(lfs, comparator, rhs);
		assertEquals("E.BENEFACTOR_ID IN (:"+bindKey+")", expression.toSql());
	}
	
	@Test
	public void getBindParameters(){
		Comparator comparator = Comparator.GREATER_THAN_OR_EQUALS;
		Integer rhs = 123;
		SqlExpression expression = new SqlExpression(annotationRefrence, comparator, rhs);
		expression.bindParameters(parameters);
		Map<String, Object> map = parameters.getParameters();
		assertEquals(rhs, map.get(bindKey));
	}

}
