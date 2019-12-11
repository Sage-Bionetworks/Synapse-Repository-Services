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
		JoinCondition condition = new JoinCondition(new TableQueryParser("tableB.id = tableC.id").searchCondition());
		TableReference joinedTables = new TableReference(
			new QualifiedJoin(
				new TableReference(new QualifiedJoin(tableA, tableB, new JoinCondition(new TableQueryParser("tableA.id = tableB.id").searchCondition()))),
				tableC,
				new JoinCondition(new TableQueryParser("tableA.id = tableC.id").searchCondition())
			)
		);


		assertEquals("tableA " +
				"JOIN tableB ON tableA.id = tableB.id " +
				"JOIN tableC ON tableA.id = tableC.id", joinedTables.toSql());
	}
}