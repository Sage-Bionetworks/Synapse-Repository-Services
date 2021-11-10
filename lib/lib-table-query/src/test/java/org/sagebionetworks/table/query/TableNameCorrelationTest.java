package org.sagebionetworks.table.query;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.table.query.model.TableNameCorrelation;

public class TableNameCorrelationTest {
	
	@Test
	public void testSynNameWithoutCorrolation() throws ParseException {
		TableNameCorrelation tableName = new TableQueryParser("syn123").tableNameCorrelation();
		assertEquals("syn123", tableName.toSql());
	}
	
	@Test
	public void testSynNameWithVersionWithoutCorrolation() throws ParseException {
		TableNameCorrelation tableName = new TableQueryParser("syn123.2").tableNameCorrelation();
		assertEquals("syn123.2", tableName.toSql());
	}
	
	@Test
	public void testSynNameWihCorrolation() throws ParseException {
		TableNameCorrelation tableName = new TableQueryParser("syn123 as t").tableNameCorrelation();
		assertEquals("syn123 AS t", tableName.toSql());
	}
	
	@Test
	public void testSynNameWithVersionWithCorrolation() throws ParseException {
		TableNameCorrelation tableName = new TableQueryParser("syn123.2 as x").tableNameCorrelation();
		assertEquals("syn123.2 AS x", tableName.toSql());
	}
	
	@Test
	public void testSynNameWithVersionWithCorrolationWithNoAs() throws ParseException {
		TableNameCorrelation tableName = new TableQueryParser("syn123.2 x").tableNameCorrelation();
		assertEquals("syn123.2 x", tableName.toSql());
	}
	
	@Test
	public void testDerivedWithCorrolation() throws ParseException {
		TableNameCorrelation tableName = new TableQueryParser("T123 as `has space`").tableNameCorrelation();
		assertEquals("T123 AS `has space`", tableName.toSql());
	}

}
