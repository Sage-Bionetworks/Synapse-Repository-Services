package org.sagebionetworks.repo.model.query.entity;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ColumnReferenceTest {

	@Test
	public void testLeftHandSideNodeField(){
		ColumnReference lhs = new ColumnReference(NodeToEntity.createdOn.name(), 0);
		assertEquals("E.CREATED_ON", lhs.toSql());
		assertEquals(null, lhs.getAnnotationAlias());
		assertEquals(new Integer(0), lhs.getColumnIndex());
	}
	
	@Test
	public void testLeftHandSideAnnotation(){
		String annotationName = "foo";
		int annotationIndex = 3;
		ColumnReference lhs = new ColumnReference(annotationName, annotationIndex);
		assertEquals("A3.STRING_VALUE", lhs.toSql());
		assertEquals("A3", lhs.getAnnotationAlias());
		assertEquals(new Integer(annotationIndex), lhs.getColumnIndex());
	}
}
