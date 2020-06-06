package org.sagebionetworks.table.query.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class JoinTypeTest {

	@Test
	public void testToSql_nullJoinType(){
		// should not add anything to String builder if constructed w/ null
		StringBuilder builder = new StringBuilder();
		JoinType joinType = new JoinType(null);
		joinType.toSql(builder, new ToSqlParameters(false));
		assertEquals(0, builder.length());
	}

	@Test
	public void testToSql_JoinTypeNotNull(){
		// should append outer join type if it is included
		StringBuilder builder = new StringBuilder();
		JoinType joinType = new JoinType(OuterJoinType.LEFT);
		joinType.toSql(builder, new ToSqlParameters(false));
		assertEquals("LEFT", builder.toString());
	}
}