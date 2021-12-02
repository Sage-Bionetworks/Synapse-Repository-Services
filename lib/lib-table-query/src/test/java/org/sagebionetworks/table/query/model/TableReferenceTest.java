package org.sagebionetworks.table.query.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.table.query.ParseException;
import org.sagebionetworks.table.query.TableQueryParser;

class TableReferenceTest {

	@Test
	public void testTableName() throws ParseException {
		TableReference reference = new TableQueryParser("tableA").tableReference();
		assertEquals("tableA", reference.toSql());
		assertFalse(reference.hasJoin());
	}
	
	@Test
	public void testTableNameWithAs() throws ParseException {
		TableReference reference = new TableQueryParser("tableA as A").tableReference();
		assertEquals("tableA AS A", reference.toSql());
		assertFalse(reference.hasJoin());
	}

	@Test
	public void testJoinedTable() throws ParseException {
		TableReference reference = new TableQueryParser("tableA join tableB on tableA.id = tableB.id").tableReference();
		assertEquals("tableA JOIN tableB ON tableA.id = tableB.id", reference.toSql());
		assertTrue(reference.hasJoin());
	}

	@Test
	public void testJoinRecursive() throws ParseException {
		TableReference reference = new TableQueryParser(
				"a join b on a.i = b.i inner join c on a.e = c.e left join d on a.i = d.i").tableReference();
		assertEquals("a JOIN b ON a.i = b.i INNER JOIN c ON a.e = c.e LEFT JOIN d ON a.i = d.i", reference.toSql());
		assertTrue(reference.hasJoin());
	}
	
	@Test
	public void testJoinRecursiveWithAlias() throws ParseException {
		TableReference reference = new TableQueryParser(
				"syn123 as a join syn456 b on a.i = b.i inner join syn789 c on a.e = c.e left join  syn222 d on a.i = d.i").tableReference();
		assertEquals("syn123 AS a JOIN syn456 b ON a.i = b.i INNER JOIN syn789 c ON a.e = c.e LEFT JOIN syn222 d ON a.i = d.i", reference.toSql());
		assertTrue(reference.hasJoin());
	}

	@Test
	public void testMultipleJoins() throws ParseException {
		TableReference tableA = new TableQueryParser("tableA").tableReference();
		TableReference tableB = new TableQueryParser("tableB").tableReference();
		TableReference tableC = new TableQueryParser("tableC").tableReference();
		JoinCondition condition = new JoinCondition(new TableQueryParser("tableB.id = tableC.id").searchCondition());
		TableReference joinedTables = new TableReference(

				new QualifiedJoin(
						new TableReference(new QualifiedJoin(tableA, tableB,
								new JoinCondition(new TableQueryParser("tableA.id = tableB.id").searchCondition()))),
						tableC, new JoinCondition(new TableQueryParser("tableA.id = tableC.id").searchCondition())));

		assertEquals("tableA " + "JOIN tableB ON tableA.id = tableB.id " + "JOIN tableC ON tableA.id = tableC.id",
				joinedTables.toSql());
	}
	
	@Test
	public void testGetTableName() throws ParseException {
		TableReference reference = new TableQueryParser("tableA").tableReference();
		assertEquals("tableA", reference.getSingleTableName().get());
		assertFalse(reference.hasJoin());
	}
	
	@Test
	public void testGetTableNameWithJoin() throws ParseException {
		TableReference reference = new TableQueryParser("tableA join tableB").tableReference();
		assertEquals(Optional.empty(), reference.getSingleTableName());
	}
	
	@Test
	public void testReplaceElement() throws ParseException {
		QuerySpecification model = new TableQueryParser("select * from syn123 group by bar, a").querySpecification();
		TableReference old = model.getFirstElementOfType(TableReference.class);
		Element oldChild = old.getChild();
		TableReference replacement = new TableQueryParser("T123").tableReference();
		// call under test
		old.replaceElement(replacement);
		assertEquals("SELECT * FROM T123 GROUP BY bar, a", model.toSql());
		assertNotNull(replacement.getParent());
		assertNull(old.getParent());
		assertNull(oldChild.getParent());
	}
}