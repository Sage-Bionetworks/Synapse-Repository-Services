package org.sagebionetworks.table.cluster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.dbo.dao.table.TableModelTestUtils;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.table.cluster.columntranslation.ColumnTranslationReference;
import org.sagebionetworks.table.cluster.columntranslation.RowMetadataColumnTranslationReference;
import org.sagebionetworks.table.cluster.columntranslation.SchemaColumnTranslationReference;
import org.sagebionetworks.table.query.ParseException;
import org.sagebionetworks.table.query.TableQueryParser;
import org.sagebionetworks.table.query.model.ColumnReference;
import org.sagebionetworks.table.query.model.TableNameCorrelation;

public class TableInfoTest {

	private ColumnModel columnFoo;
	private ColumnModel columnBar;
	private ColumnModel columnWithSpace;

	private int tableIndex;
	private List<ColumnModel> schema;

	@BeforeEach
	public void before() {
		columnFoo = TableModelTestUtils.createColumn(111L, "foo", ColumnType.STRING);
		columnBar = TableModelTestUtils.createColumn(222L, "bar", ColumnType.INTEGER);
		columnWithSpace = TableModelTestUtils.createColumn(333L, "has space", ColumnType.DOUBLE);
		schema = Arrays.asList(columnFoo, columnBar, columnWithSpace);
		tableIndex = 1;
	}

	@Test
	public void tesConstructorWithNoVersionNoAlias() throws ParseException {
		TableNameCorrelation tableNameCorrelation = new TableQueryParser("syn123").tableNameCorrelation();
		// call under test
		TableInfo info = new TableInfo(tableNameCorrelation, tableIndex, schema);
		assertEquals("syn123", info.getOriginalTableName());
		assertEquals(IdAndVersion.parse("syn123"), info.getTableIdAndVersion());
		assertEquals("T123", info.getTranslatedTableName());
		assertEquals(null, info.getTableAlias().orElse(null));
		assertEquals(schema, info.getTableSchema());
		assertEquals(tableIndex, info.getTableIndex());
		assertEquals("_A1", info.getTranslatedTableAlias());
	}

	@Test
	public void tesConstructorWithNoVersionWithAlias() throws ParseException {
		TableNameCorrelation tableNameCorrelation = new TableQueryParser("syn123 t").tableNameCorrelation();
		// call under test
		TableInfo info = new TableInfo(tableNameCorrelation, tableIndex, schema);
		assertEquals("syn123", info.getOriginalTableName());
		assertEquals(IdAndVersion.parse("syn123"), info.getTableIdAndVersion());
		assertEquals("T123", info.getTranslatedTableName());
		assertEquals("t", info.getTableAlias().orElse(null));
		assertEquals(schema, info.getTableSchema());
		assertEquals(tableIndex, info.getTableIndex());
		assertEquals("_A1", info.getTranslatedTableAlias());
	}

	@Test
	public void tesConstructorWithVersionNoAlias() throws ParseException {
		TableNameCorrelation tableNameCorrelation = new TableQueryParser("syn123.4").tableNameCorrelation();
		// call under test
		TableInfo info = new TableInfo(tableNameCorrelation, tableIndex, schema);
		assertEquals("syn123.4", info.getOriginalTableName());
		assertEquals(IdAndVersion.parse("syn123.4"), info.getTableIdAndVersion());
		assertEquals("T123_4", info.getTranslatedTableName());
		assertEquals(null, info.getTableAlias().orElse(null));
		assertEquals(schema, info.getTableSchema());
		assertEquals("_A1", info.getTranslatedTableAlias());
	}

	@Test
	public void tesConstructorWithVersionWithAlias() throws ParseException {
		TableNameCorrelation tableNameCorrelation = new TableQueryParser("syn123.4 as t").tableNameCorrelation();
		// call under test
		TableInfo info = new TableInfo(tableNameCorrelation, tableIndex, schema);
		assertEquals("syn123.4", info.getOriginalTableName());
		assertEquals(IdAndVersion.parse("syn123.4"), info.getTableIdAndVersion());
		assertEquals("T123_4", info.getTranslatedTableName());
		assertEquals("t", info.getTableAlias().orElse(null));
		assertEquals(schema, info.getTableSchema());
		assertEquals("_A1", info.getTranslatedTableAlias());
	}

	@Test
	public void testConstructorWithNull() {
		TableNameCorrelation tableNameCorrelation = null;
		String message = assertThrows(IllegalArgumentException.class, () -> {
			new TableInfo(tableNameCorrelation, tableIndex, schema);
		}).getMessage();
		assertEquals("TableNameCorrelation is required.", message);
	}

	@Test
	public void testLookupColumnReferenceWithoutLHS() throws ParseException {
		TableNameCorrelation tableNameCorrelation = new TableQueryParser("syn123").tableNameCorrelation();
		ColumnReference reference = new TableQueryParser("foo").columnReference();
		TableInfo info = new TableInfo(tableNameCorrelation, tableIndex, schema);
		// call under test
		Optional<ColumnTranslationReference> transRef = info.lookupColumnReference(reference);
		assertNotNull(transRef);
		assertEquals(new SchemaColumnTranslationReference(columnFoo), transRef.orElse(null));
	}

	@Test
	public void testLookupColumnReferenceWithNullRef() throws ParseException {
		TableNameCorrelation tableNameCorrelation = new TableQueryParser("syn123").tableNameCorrelation();
		TableInfo info = new TableInfo(tableNameCorrelation, tableIndex, schema);
		// call under test
		assertEquals(Optional.empty(), info.lookupColumnReference(null));
	}

	@Test
	public void testLookupColumnReferenceWithoutLHSWithBackticks() throws ParseException {
		TableNameCorrelation tableNameCorrelation = new TableQueryParser("syn123").tableNameCorrelation();
		ColumnReference reference = new TableQueryParser("`has space`").columnReference();
		TableInfo info = new TableInfo(tableNameCorrelation, tableIndex, schema);
		// call under test
		Optional<ColumnTranslationReference> transRef = info.lookupColumnReference(reference);
		assertNotNull(transRef);
		assertEquals(new SchemaColumnTranslationReference(columnWithSpace), transRef.orElse(null));
	}

	@Test
	public void testLookupColumnReferenceWithoutLHSWithDoubleQuotes() throws ParseException {
		TableNameCorrelation tableNameCorrelation = new TableQueryParser("syn123").tableNameCorrelation();
		ColumnReference reference = new TableQueryParser("\"has space\"").columnReference();
		TableInfo info = new TableInfo(tableNameCorrelation, tableIndex, schema);
		// call under test
		Optional<ColumnTranslationReference> transRef = info.lookupColumnReference(reference);
		assertNotNull(transRef);
		assertEquals(new SchemaColumnTranslationReference(columnWithSpace), transRef.orElse(null));
	}

	@Test
	public void testLookupColumnReferenceWithRHSNoMatch() throws ParseException {
		TableNameCorrelation tableNameCorrelation = new TableQueryParser("syn123").tableNameCorrelation();
		ColumnReference reference = new TableQueryParser("nope").columnReference();
		TableInfo info = new TableInfo(tableNameCorrelation, tableIndex, schema);
		// call under test
		Optional<ColumnTranslationReference> transRef = info.lookupColumnReference(reference);
		assertNotNull(transRef);
		assertFalse(transRef.isPresent());
	}

	@Test
	public void testLookupColumnReferenceWithLHSMatchesTableName() throws ParseException {
		TableNameCorrelation tableNameCorrelation = new TableQueryParser("syn1").tableNameCorrelation();
		ColumnReference reference = new TableQueryParser("syn1.foo").columnReference();
		TableInfo info = new TableInfo(tableNameCorrelation, tableIndex, schema);
		// call under test
		Optional<ColumnTranslationReference> transRef = info.lookupColumnReference(reference);
		assertNotNull(transRef);
		assertEquals(new SchemaColumnTranslationReference(columnFoo), transRef.orElse(null));
	}

	@Test
	public void testLookupColumnReferenceWithLHSMatchesTableNameWithBackticks() throws ParseException {
		TableNameCorrelation tableNameCorrelation = new TableQueryParser("syn1").tableNameCorrelation();
		ColumnReference reference = new TableQueryParser("`syn1`.`has space`").columnReference();
		TableInfo info = new TableInfo(tableNameCorrelation, tableIndex, schema);
		// call under test
		Optional<ColumnTranslationReference> transRef = info.lookupColumnReference(reference);
		assertNotNull(transRef);
		assertEquals(new SchemaColumnTranslationReference(columnWithSpace), transRef.orElse(null));
	}

	@Test
	public void testLookupColumnReferenceWithLHSDoesNotMatchesTableName() throws ParseException {
		TableNameCorrelation tableNameCorrelation = new TableQueryParser("syn1").tableNameCorrelation();
		ColumnReference reference = new TableQueryParser("syn2.foo").columnReference();
		TableInfo info = new TableInfo(tableNameCorrelation, tableIndex, schema);
		// call under test
		Optional<ColumnTranslationReference> transRef = info.lookupColumnReference(reference);
		assertNotNull(transRef);
		assertFalse(transRef.isPresent());
	}

	@Test
	public void testLookupColumnReferenceWithLHSMatchesTableAlias() throws ParseException {
		TableNameCorrelation tableNameCorrelation = new TableQueryParser("syn1 as t").tableNameCorrelation();
		ColumnReference reference = new TableQueryParser("t.foo").columnReference();
		TableInfo info = new TableInfo(tableNameCorrelation, tableIndex, schema);
		// call under test
		Optional<ColumnTranslationReference> transRef = info.lookupColumnReference(reference);
		assertNotNull(transRef);
		assertEquals(new SchemaColumnTranslationReference(columnFoo), transRef.orElse(null));
	}

	@Test
	public void testLookupColumnReferenceWithLHSMatchesTableAliasWithBackticks() throws ParseException {
		TableNameCorrelation tableNameCorrelation = new TableQueryParser("syn1 as t").tableNameCorrelation();
		ColumnReference reference = new TableQueryParser("`t`.`has space`").columnReference();
		TableInfo info = new TableInfo(tableNameCorrelation, tableIndex, schema);
		// call under test
		Optional<ColumnTranslationReference> transRef = info.lookupColumnReference(reference);
		assertNotNull(transRef);
		assertEquals(new SchemaColumnTranslationReference(columnWithSpace), transRef.orElse(null));
	}

	@Test
	public void testLookupColumnReferenceWithLHSDoesNotMatchTableAlias() throws ParseException {
		TableNameCorrelation tableNameCorrelation = new TableQueryParser("syn1 as t").tableNameCorrelation();
		ColumnReference reference = new TableQueryParser("r.foo").columnReference();
		TableInfo info = new TableInfo(tableNameCorrelation, tableIndex, schema);
		// call under test
		Optional<ColumnTranslationReference> transRef = info.lookupColumnReference(reference);
		assertNotNull(transRef);
		assertFalse(transRef.isPresent());
	}

	@Test
	public void testLookupColumnReferenceWithReservedColumn() throws ParseException {
		TableNameCorrelation tableNameCorrelation = new TableQueryParser("syn1").tableNameCorrelation();
		ColumnReference reference = new TableQueryParser("ROW_ID").columnReference();
		TableInfo info = new TableInfo(tableNameCorrelation, tableIndex, schema);
		// call under test
		Optional<ColumnTranslationReference> transRef = info.lookupColumnReference(reference);
		assertNotNull(transRef);
		assertEquals(RowMetadataColumnTranslationReference.ROW_ID.getColumnTranslationReference(), transRef.orElse(null));
	}

	@Test
	public void testLookupColumnReferenceWithReservedColumnWithTableName() throws ParseException {
		TableNameCorrelation tableNameCorrelation = new TableQueryParser("syn1").tableNameCorrelation();
		ColumnReference reference = new TableQueryParser("syn1.ROW_VERSION").columnReference();
		TableInfo info = new TableInfo(tableNameCorrelation, tableIndex, schema);
		// call under test
		Optional<ColumnTranslationReference> transRef = info.lookupColumnReference(reference);
		assertNotNull(transRef);
		assertEquals(RowMetadataColumnTranslationReference.ROW_VERSION.getColumnTranslationReference(), transRef.orElse(null));
	}

	@Test
	public void testLookupColumnReferenceWithReservedColumnWithTableAlias() throws ParseException {
		TableNameCorrelation tableNameCorrelation = new TableQueryParser("syn1 as q").tableNameCorrelation();
		ColumnReference reference = new TableQueryParser("q.row_benefactor").columnReference();
		TableInfo info = new TableInfo(tableNameCorrelation, tableIndex, schema);
		// call under test
		Optional<ColumnTranslationReference> transRef = info.lookupColumnReference(reference);
		assertNotNull(transRef);
		assertEquals(RowMetadataColumnTranslationReference.ROW_BENEFACTOR.getColumnTranslationReference(), transRef.orElse(null));
	}
	
	@Test
	public void testLookupColumnReferenceWithReservedColumnWithTranslatedTableName() throws ParseException {
		TableNameCorrelation tableNameCorrelation = new TableQueryParser("syn1 as q").tableNameCorrelation();
		ColumnReference reference = new TableQueryParser("T1.row_benefactor").columnReference();
		TableInfo info = new TableInfo(tableNameCorrelation, tableIndex, schema);
		// call under test
		Optional<ColumnTranslationReference> transRef = info.lookupColumnReference(reference);
		assertNotNull(transRef);
		assertEquals(RowMetadataColumnTranslationReference.ROW_BENEFACTOR.getColumnTranslationReference(), transRef.orElse(null));
	}
	
	@Test
	public void testLookupColumnReferenceWithReservedColumnWithTranslatedTableNameWithVersion() throws ParseException {
		TableNameCorrelation tableNameCorrelation = new TableQueryParser("syn1.5 as q").tableNameCorrelation();
		ColumnReference reference = new TableQueryParser("T1_5.row_benefactor").columnReference();
		TableInfo info = new TableInfo(tableNameCorrelation, tableIndex, schema);
		// call under test
		Optional<ColumnTranslationReference> transRef = info.lookupColumnReference(reference);
		assertNotNull(transRef);
		assertEquals(RowMetadataColumnTranslationReference.ROW_BENEFACTOR.getColumnTranslationReference(), transRef.orElse(null));
	}

	@Test
	public void testIsMatchWithBothAlias() throws ParseException {
		TableNameCorrelation tableNameCorrelation = new TableQueryParser("syn1 as q").tableNameCorrelation();
		TableInfo info = new TableInfo(tableNameCorrelation, tableIndex, schema);
		assertTrue(info.isMatch(new TableQueryParser("syn1 as q").tableNameCorrelation()));
	}

	@Test
	public void testIsMatchWithBothAliasTranslated() throws ParseException {
		tableIndex = 3;
		TableNameCorrelation tableNameCorrelation = new TableQueryParser("syn1 as q").tableNameCorrelation();
		TableInfo info = new TableInfo(tableNameCorrelation, tableIndex, schema);
		assertTrue(info.isMatch(new TableQueryParser("T1 _A3").tableNameCorrelation()));
	}
	
	@Test
	public void testIsMatchWithBothAliasWithTableTranslatedButNotAlais() throws ParseException {
		tableIndex = 3;
		TableNameCorrelation tableNameCorrelation = new TableQueryParser("syn1 as q").tableNameCorrelation();
		TableInfo info = new TableInfo(tableNameCorrelation, tableIndex, schema);
		assertTrue(info.isMatch(new TableQueryParser("T1 q").tableNameCorrelation()));
	}
	
	@Test
	public void testIsMatchWithBothAliasWithTableNotTransatedWithAliasTransalted() throws ParseException {
		tableIndex = 3;
		TableNameCorrelation tableNameCorrelation = new TableQueryParser("syn1 as q").tableNameCorrelation();
		TableInfo info = new TableInfo(tableNameCorrelation, tableIndex, schema);
		assertTrue(info.isMatch(new TableQueryParser("syn1 _A3").tableNameCorrelation()));
	}

	@Test
	public void testIsMatchWithoutAlias() throws ParseException {
		TableNameCorrelation tableNameCorrelation = new TableQueryParser("syn1").tableNameCorrelation();
		TableInfo info = new TableInfo(tableNameCorrelation, tableIndex, schema);
		assertTrue(info.isMatch(new TableQueryParser("syn1").tableNameCorrelation()));
	}

	@Test
	public void testIsMatchWithoutAliasAndTranslated() throws ParseException {
		TableNameCorrelation tableNameCorrelation = new TableQueryParser("syn1").tableNameCorrelation();
		TableInfo info = new TableInfo(tableNameCorrelation, tableIndex, schema);
		assertTrue(info.isMatch(new TableQueryParser("T1").tableNameCorrelation()));
	}

	@Test
	public void testIsMatchWithRightAliasWithoutLeftAlias() throws ParseException {
		TableNameCorrelation tableNameCorrelation = new TableQueryParser("syn1 as q").tableNameCorrelation();
		TableInfo info = new TableInfo(tableNameCorrelation, tableIndex, schema);
		assertFalse(info.isMatch(new TableQueryParser("syn1").tableNameCorrelation()));
	}
	
	@Test
	public void testIsMatchWithoutRightAliasWithLeftAlias() throws ParseException {
		TableNameCorrelation tableNameCorrelation = new TableQueryParser("syn1").tableNameCorrelation();
		TableInfo info = new TableInfo(tableNameCorrelation, tableIndex, schema);
		assertFalse(info.isMatch(new TableQueryParser("syn1 as q").tableNameCorrelation()));
	}
	
	@Test
	public void testIsMatchWithVersionsMatching() throws ParseException {
		TableNameCorrelation tableNameCorrelation = new TableQueryParser("syn1.2").tableNameCorrelation();
		TableInfo info = new TableInfo(tableNameCorrelation, tableIndex, schema);
		assertTrue(info.isMatch(new TableQueryParser("syn1.2").tableNameCorrelation()));
	}
	
	@Test
	public void testIsMatchWithVersionsMatchingTranslated() throws ParseException {
		TableNameCorrelation tableNameCorrelation = new TableQueryParser("syn1.2").tableNameCorrelation();
		TableInfo info = new TableInfo(tableNameCorrelation, tableIndex, schema);
		assertTrue(info.isMatch(new TableQueryParser("T1_2").tableNameCorrelation()));
	}
	
	@Test
	public void testIsMatchWithVersionMissing() throws ParseException {
		TableNameCorrelation tableNameCorrelation = new TableQueryParser("syn1.2").tableNameCorrelation();
		TableInfo info = new TableInfo(tableNameCorrelation, tableIndex, schema);
		assertFalse(info.isMatch(new TableQueryParser("syn1").tableNameCorrelation()));
	}
}
