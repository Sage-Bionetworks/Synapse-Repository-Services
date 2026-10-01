package org.sagebionetworks.table.cluster.description;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.dao.table.TableType;
import org.sagebionetworks.repo.model.entity.IdAndVersion;

public class IndexDescriptionStateTest {

	private final IdAndVersion idAndVersion = IdAndVersion.parse("syn123");

	@Test
	public void testConstructor() {
		// call under test
		IndexDescriptionState state = new IndexDescriptionState(idAndVersion, TableType.materializedview,
				"select * from syn1", 7L);

		assertEquals(idAndVersion, state.getIdAndVersion());
		assertEquals(TableType.materializedview, state.getTableType());
		assertEquals(Optional.of("select * from syn1"), state.getDefiningSql());
		assertEquals(Optional.of(7L), state.getIndexVersion());
	}

	@Test
	public void testConstructorWithNoDefiningSqlOrIndexVersion() {
		// Both are absent for a base index type that has not been built yet.
		// call under test
		IndexDescriptionState state = new IndexDescriptionState(idAndVersion, TableType.table, null, null);

		assertEquals(Optional.empty(), state.getDefiningSql());
		assertEquals(Optional.empty(), state.getIndexVersion());
	}

	@Test
	public void testConstructorWithNullIdAndVersion() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			new IndexDescriptionState(null, TableType.table, null, 7L);
		}).getMessage();
		assertEquals("idAndVersion is required.", message);
	}

	@Test
	public void testConstructorWithNullTableType() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			new IndexDescriptionState(idAndVersion, null, null, 7L);
		}).getMessage();
		assertEquals("tableType is required.", message);
	}

	@Test
	public void testEqualsWithSameState() {
		IndexDescriptionState state = new IndexDescriptionState(idAndVersion, TableType.virtualtable,
				"select * from syn1", 7L);

		// call under test
		assertEquals(new IndexDescriptionState(idAndVersion, TableType.virtualtable, "select * from syn1", 7L), state);
	}

	@Test
	public void testHashCodeWithSameState() {
		IndexDescriptionState state = new IndexDescriptionState(idAndVersion, TableType.virtualtable,
				"select * from syn1", 7L);

		// call under test
		assertEquals(new IndexDescriptionState(idAndVersion, TableType.virtualtable, "select * from syn1", 7L).hashCode(),
				state.hashCode());
	}

	@Test
	public void testEqualsWithDifferentDefiningSql() {
		IndexDescriptionState state = new IndexDescriptionState(idAndVersion, TableType.virtualtable,
				"select * from syn1", 7L);

		// call under test
		assertNotEquals(new IndexDescriptionState(idAndVersion, TableType.virtualtable, "select * from syn2", 7L),
				state);
	}

	@Test
	public void testEqualsWithDifferentIdAndVersion() {
		IndexDescriptionState state = new IndexDescriptionState(idAndVersion, TableType.table, null, 7L);

		// call under test
		assertNotEquals(new IndexDescriptionState(IdAndVersion.parse("syn123.1"), TableType.table, null, 7L), state);
	}

	@Test
	public void testEqualsWithDifferentTableType() {
		IndexDescriptionState state = new IndexDescriptionState(idAndVersion, TableType.table, null, 7L);

		// call under test
		assertNotEquals(new IndexDescriptionState(idAndVersion, TableType.recordset, null, 7L), state);
	}

	@Test
	public void testEqualsWithDifferentIndexVersion() {
		IndexDescriptionState state = new IndexDescriptionState(idAndVersion, TableType.table, null, 7L);

		// call under test
		assertNotEquals(new IndexDescriptionState(idAndVersion, TableType.table, null, 8L), state);
	}

	@Test
	public void testEqualsWithOtherType() {
		IndexDescriptionState state = new IndexDescriptionState(idAndVersion, TableType.table, null, 7L);

		// call under test
		assertNotEquals("not a state", state);
	}

	@Test
	public void testToStringWithLongDefiningSql() {
		// The defining SQL of a view can be arbitrarily long, so toString truncates it to keep a log line
		// readable.
		String definingSql = "select one, two, three, four, five, six, seven, eight, nine, ten from syn1";
		IndexDescriptionState state = new IndexDescriptionState(idAndVersion, TableType.materializedview, definingSql,
				null);

		// call under test
		String result = state.toString();

		assertTrue(result.contains("definingSql=" + definingSql.substring(0, 50) + "..."), result);
		assertFalse(result.contains(definingSql), result);
	}

	@Test
	public void testToStringWithShortDefiningSql() {
		IndexDescriptionState state = new IndexDescriptionState(idAndVersion, TableType.virtualtable,
				"select * from syn1", 7L);

		// call under test
		assertEquals("IndexDescriptionState [idAndVersion=syn123, tableType=virtualtable,"
				+ " definingSql=select * from syn1..., indexVersion=7]", state.toString());
	}

	@Test
	public void testToStringWithNoDefiningSql() {
		IndexDescriptionState state = new IndexDescriptionState(idAndVersion, TableType.table, null, 7L);

		// call under test
		assertTrue(state.toString().contains("definingSql=null"));
	}
}
