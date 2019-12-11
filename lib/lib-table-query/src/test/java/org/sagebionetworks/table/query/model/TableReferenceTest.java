package org.sagebionetworks.table.query.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.table.query.ParseException;
import org.sagebionetworks.table.query.TableQueryParser;

class TableReferenceTest {

	@Test
	public void testTableName() throws ParseException {
		TableReference reference = new TableQueryParser("tableA").tableReference();
		assertEquals("tableA", reference.toSql());
	}

	@Test
	public void testMultipleJoins() throws ParseException {
		TableReference tableA = new TableQueryParser("tableA").tableReference();
		TableReference tableB = new TableQueryParser("tableB").tableReference();
		TableReference tableC = new TableQueryParser("tableC").tableReference();

		TableReference joinedTables = new TableReference(
			new QualifiedJoin(
				tableA,
				new TableReference(new QualifiedJoin(tableB, tableC))
			)
		);


		assertEquals("tableA JOIN tableB JOIN tableC", joinedTables.toSql());
	}
}