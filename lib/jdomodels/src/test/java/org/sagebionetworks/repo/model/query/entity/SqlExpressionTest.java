package org.sagebionetworks.repo.model.query.entity;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;
import org.sagebionetworks.repo.model.query.Comparator;

import com.google.common.collect.Lists;

public class SqlExpressionTest {

	@Test
	public void testNodeField(){
		int index = 5;
		ColumnReference lfs = new ColumnReference(NodeToEntity.name.name(), index);
		Comparator comparator = Comparator.EQUALS;
		String rhs = "someName";
		SqlExpression expression = new SqlExpression(lfs, comparator, rhs);
		assertEquals("E.NAME = :b5", expression.toSql());
	}
	
	@Test
	public void testAnnotation(){
		int index = 5;
		String annotationName = "foo";
		ColumnReference lfs = new ColumnReference(annotationName, index);
		Comparator comparator = Comparator.GREATER_THAN_OR_EQUALS;
		Integer rhs = 123;
		SqlExpression expression = new SqlExpression(lfs, comparator, rhs);
		assertEquals("A5.ANNO_VALUE >= :b5", expression.toSql());
	}
	
	@Test
	public void testInClause(){
		int index = 0;
		ColumnReference lfs = new ColumnReference(NodeToEntity.benefactorId.name(), index);
		Comparator comparator = Comparator.IN;
		List<Long> rhs = Lists.newArrayList(123L, 456L);

		SqlExpression expression = new SqlExpression(lfs, comparator, rhs);
		assertEquals("E.BENEFACTOR_ID IN (:b0)", expression.toSql());
	}

}
