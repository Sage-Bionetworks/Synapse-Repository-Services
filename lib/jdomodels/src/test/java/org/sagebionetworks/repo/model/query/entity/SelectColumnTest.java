package org.sagebionetworks.repo.model.query.entity;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class SelectColumnTest {

	@Test
	public void testToSqlNodeField(){
		int index = 1;
		ColumnReference ref = new ColumnReference(NodeToEntity.benefactorId.name(), index);
		SelectColumn select = new SelectColumn(ref);
		assertEquals("E.BENEFACTOR_ID AS 'benefactorId'", select.toSql());
	}
	
	@Test
	public void testToSqlNodeFieldAlis(){
		int index = 1;
		// Alias is part of the node fields but does not exist in the entity table
		ColumnReference ref = new ColumnReference(NodeToEntity.alias.name(), index);
		SelectColumn select = new SelectColumn(ref);
		assertEquals("NULL AS 'alias'", select.toSql());
	}
	
	@Test
	public void testToSqlNodeAnnotationName(){
		int index = 1;
		ColumnReference ref = new ColumnReference("annotationName", index);
		SelectColumn select = new SelectColumn(ref);
		assertEquals("A1.STRING_VALUE AS 'annotationName'", select.toSql());
	}

}
