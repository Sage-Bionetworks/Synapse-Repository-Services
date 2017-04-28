package org.sagebionetworks.repo.model.query.entity;

import static org.junit.Assert.assertEquals;

import java.util.Map;

import org.junit.Before;
import org.junit.Test;

public class AnnotationJoinTest {
	
	ColumnReference annotationReference;
	
	Parameters parameters;
	int index = 5;
	String annotationName = "foo";
	
	@Before
	public void before(){
		index = 5;
		annotationName = "foo";
		annotationReference = new ColumnReference(annotationName, index);
		parameters = new Parameters();
	}

	@Test
	public void testToSqlLeftJoinTrue(){
		boolean leftJoin = true;
		AnnotationJoin join = new AnnotationJoin(annotationReference, leftJoin);
		assertEquals(" LEFT JOIN ANNOTATION_REPLICATION A5 ON (E.ID = A5.ENTITY_ID AND A5.ANNO_KEY = :bJoinName5)", join.toSql());
	}
	
	@Test
	public void testToSqlLeftJoinFalse(){
		boolean leftJoin = false;
		AnnotationJoin join = new AnnotationJoin(annotationReference, leftJoin);
		assertEquals(" JOIN ANNOTATION_REPLICATION A5 ON (E.ID = A5.ENTITY_ID AND A5.ANNO_KEY = :bJoinName5)", join.toSql());
	}

	@Test
	public void testBindParameters(){
		boolean leftJoin = false;
		AnnotationJoin join = new AnnotationJoin(annotationReference, leftJoin);
		join.bindParameters(parameters);
		Map<String, Object> map = parameters.getParameters();
		String bindName = Constants.BIND_PREFIX_ANNOTATION_JOIN+index;
		assertEquals(annotationName, map.get(bindName));
	}
}
