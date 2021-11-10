package org.sagebionetworks.table.query.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.table.query.TableQueryParser;

class JoinTypeTest {
	
	@Test
	public void testInnerJoin() throws Exception {
		JoinType joinType = new TableQueryParser("inner").joinType();
		assertEquals("INNER",joinType.toSql());
	}
	
	@Test
	public void testLeftJoinWithoutExplicitOuter() throws Exception {
		JoinType joinType = new TableQueryParser("left").joinType();
		assertEquals("LEFT",joinType.toSql());
	}
	
	@Test
	public void testLeftJoinWithExplicitOuter() throws Exception {
		JoinType joinType = new TableQueryParser("left outer").joinType();
		assertEquals("LEFT OUTER",joinType.toSql());
	}
	
	@Test
	public void testRightJoinWithoutExplicitOuter() throws Exception {
		JoinType joinType = new TableQueryParser("right").joinType();
		assertEquals("RIGHT",joinType.toSql());
	}
	
	@Test
	public void testRightJoinWithExplicitOuter() throws Exception {
		JoinType joinType = new TableQueryParser("right outer").joinType();
		assertEquals("RIGHT OUTER",joinType.toSql());
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