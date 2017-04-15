package org.sagebionetworks.repo.model.query.entity;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

public class AnnotationJoinTest {
	
	ColumnReference annotationReference;
	
	@Before
	public void before(){
		int index = 5;
		String annotationName = "foo";
		annotationReference = new ColumnReference(annotationName, index);
	}

	@Test
	public void testToSqlJoin(){
		AnnotationJoin join = new AnnotationJoin(annotationReference);
		assertEquals(" JOIN ANNOTATION_REPLICATION A5 ON (R.ID = A5.ENTITY_ID AND A5.ANNO_KEY = :K5)", join.toSql());
	}

}
