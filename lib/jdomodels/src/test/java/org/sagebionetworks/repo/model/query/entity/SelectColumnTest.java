package org.sagebionetworks.repo.model.query.entity;

import static org.junit.Assert.*;

import org.junit.Test;

public class SelectColumnTest {

	@Test
	public void testToSqlNodeField(){
		SelectColumn select = new SelectColumn(NodeToEntity.benefactorId);
		assertEquals("E.BENEFACTOR_ID AS 'benefactorId'", select.toSql());
	}
	
	@Test
	public void testToSqlNodeFieldAlis(){
		// Alias is part of the node fields but does not exist in the entity table
		SelectColumn select = new SelectColumn(NodeToEntity.alias);
		assertEquals("NULL AS 'alias'", select.toSql());
	}
	
	@Test
	public void testToSqlNodeAnnotationName(){
		SelectColumn select = new SelectColumn("annotationName");
		assertEquals("NULL AS 'annotationName'", select.toSql());
	}

}
