package org.sagebionetworks.table.cluster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.model.dbo.dao.table.TableModelTestUtils;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.table.cluster.columntranslation.RowMetadataColumnTranslationReference;
import org.sagebionetworks.table.cluster.columntranslation.SchemaColumnTranslationReference;
import org.sagebionetworks.table.query.ParseException;
import org.sagebionetworks.table.query.TableQueryParser;
import org.sagebionetworks.table.query.model.ColumnReference;
import org.sagebionetworks.table.query.model.QuerySpecification;
import org.sagebionetworks.table.query.model.SelectList;
import org.sagebionetworks.table.query.model.TableNameCorrelation;

@ExtendWith(MockitoExtension.class)
public class TableAndColumnMapperTest {

	private List<ColumnModel> allColumns;
	private Map<String, ColumnModel> columnMap;
	
	@Mock
	SchemaProvider mockSchemaProvider;

	@BeforeEach
	public void before() {
		allColumns = Arrays.asList(
				TableModelTestUtils.createColumn(111L, "foo", ColumnType.STRING),
				TableModelTestUtils.createColumn(222L, "has space", ColumnType.STRING),
				TableModelTestUtils.createColumn(333L, "bar", ColumnType.INTEGER),
				TableModelTestUtils.createColumn(444L, "foo_bar", ColumnType.STRING),
				TableModelTestUtils.createColumn(555L, "Foo", ColumnType.STRING),
				TableModelTestUtils.createColumn(666L, "datetype", ColumnType.DATE),
				TableModelTestUtils.createColumn(777L, "has\"quote", ColumnType.STRING),
				TableModelTestUtils.createColumn(888L, "aDouble", ColumnType.DOUBLE),
				TableModelTestUtils.createColumn(999L, "year", ColumnType.DATE));
		columnMap = allColumns.stream()
			      .collect(Collectors.toMap(ColumnModel::getName, Function.identity()));
	}

	@Test
	public void testConstructorWithSingleTable() throws ParseException {
		QuerySpecification model = new TableQueryParser("select * from syn123").querySpecification();

		when(mockSchemaProvider.getTableSchema(any())).thenReturn(allColumns);

		// call under test
		TableAndColumnMapper mapper = new TableAndColumnMapper(model, mockSchemaProvider);
		assertEquals(Arrays.asList(IdAndVersion.parse("syn123")), mapper.getTableIds());
		assertEquals(allColumns, mapper.getUnionOfAllTableSchemas());
	}

	@Test
	public void testConstructorWithMultipleTables() throws ParseException {
		QuerySpecification model = new TableQueryParser("select * from syn123 join syn456").querySpecification();
		when(mockSchemaProvider.getTableSchema(IdAndVersion.parse("syn123")))
		.thenReturn(List.of(columnMap.get("foo"), columnMap.get("has space"), columnMap.get("bar")));
		
		when(mockSchemaProvider.getTableSchema(IdAndVersion.parse("syn456")))
		.thenReturn(List.of(columnMap.get("foo_bar"), allColumns.get(4), allColumns.get(5)));
		
		// call under test
		TableAndColumnMapper mapper = new TableAndColumnMapper(model, mockSchemaProvider);
		assertEquals(Arrays.asList(IdAndVersion.parse("syn123"), IdAndVersion.parse("syn456")), mapper.getTableIds());
		assertEquals(allColumns.subList(0, 6), mapper.getUnionOfAllTableSchemas());
	}

	@Test
	public void testConstructorWithNullModel() throws ParseException {
		QuerySpecification model = null;

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			new TableAndColumnMapper(model, mockSchemaProvider);
		}).getMessage();
		assertEquals("QuerySpecification is required.", message);
	}

	@Test
	public void testConstructorWithNullProvider() throws ParseException {
		QuerySpecification model = new TableQueryParser("select * from syn123").querySpecification();
		SchemaProvider provider = null;

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			new TableAndColumnMapper(model, provider);
		}).getMessage();
		assertEquals("SchemaProvider is required.", message);
	}

	@Test
	public void testConstructorWithEmptySchema() throws ParseException {
		QuerySpecification model = new TableQueryParser("select * from syn123").querySpecification();

		when(mockSchemaProvider.getTableSchema(any())).thenReturn(Collections.emptyList());
		
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			new TableAndColumnMapper(model, mockSchemaProvider);
		}).getMessage();
		assertEquals("Schema for syn123 is empty.", message);
	}

	@Test
	public void testBuildSelectAllColumns() throws ParseException {
		QuerySpecification model = new TableQueryParser("select * from syn123").querySpecification();
		
		when(mockSchemaProvider.getTableSchema(any())).thenReturn(allColumns);
		
		TableAndColumnMapper mapper = new TableAndColumnMapper(model, mockSchemaProvider);

		// call under test
		SelectList selectList = mapper.buildSelectAllColumns();
		assertNotNull(selectList);
		assertEquals("\"foo\", \"has space\", \"bar\", \"foo_bar\", \"Foo\", \"datetype\", \"has\"\"quote\", \"aDouble\", \"year\"",
				selectList.toSql());
	}

	@Test
	public void testBuildSelectAllColumnsWithJoinWithoutAlias() throws ParseException {
		QuerySpecification model = new TableQueryParser("select * from syn123 join syn456").querySpecification();
		
		when(mockSchemaProvider.getTableSchema(IdAndVersion.parse("syn123")))
				.thenReturn(List.of(columnMap.get("foo"), columnMap.get("has space")));
		when(mockSchemaProvider.getTableSchema(IdAndVersion.parse("syn456")))
				.thenReturn(List.of(columnMap.get("bar"), columnMap.get("foo_bar")));
		
		TableAndColumnMapper mapper = new TableAndColumnMapper(model, mockSchemaProvider);

		// call under test
		SelectList selectList = mapper.buildSelectAllColumns();
		assertNotNull(selectList);
		assertEquals("syn123.\"foo\", syn123.\"has space\", syn456.\"bar\", syn456.\"foo_bar\"", selectList.toSql());
	}

	@Test
	public void testBuildSelectAllColumnsWithJoinWithAlias() throws ParseException {
		QuerySpecification model = new TableQueryParser("select * from syn123 t join syn456 r").querySpecification();
		
		when(mockSchemaProvider.getTableSchema(IdAndVersion.parse("syn123")))
				.thenReturn(List.of(columnMap.get("foo"), columnMap.get("has space")));
		when(mockSchemaProvider.getTableSchema(IdAndVersion.parse("syn456")))
				.thenReturn(List.of(columnMap.get("bar"), columnMap.get("foo_bar")));
		
		TableAndColumnMapper mapper = new TableAndColumnMapper(model, mockSchemaProvider);

		// call under test
		SelectList selectList = mapper.buildSelectAllColumns();
		assertNotNull(selectList);
		assertEquals("t.\"foo\", t.\"has space\", r.\"bar\", r.\"foo_bar\"", selectList.toSql());
	}

	@Test
	public void testBuildSelectAllColumnsWithJoinWithMixedAlias() throws ParseException {
		QuerySpecification model = new TableQueryParser("select * from syn123 t join syn456").querySpecification();
		
		when(mockSchemaProvider.getTableSchema(IdAndVersion.parse("syn123")))
				.thenReturn(List.of(columnMap.get("foo"), columnMap.get("has space")));
		when(mockSchemaProvider.getTableSchema(IdAndVersion.parse("syn456")))
				.thenReturn(List.of(columnMap.get("bar"), columnMap.get("foo_bar")));
		
		TableAndColumnMapper mapper = new TableAndColumnMapper(model, mockSchemaProvider);

		// call under test
		SelectList selectList = mapper.buildSelectAllColumns();
		assertNotNull(selectList);
		assertEquals("t.\"foo\", t.\"has space\", syn456.\"bar\", syn456.\"foo_bar\"", selectList.toSql());
	}

	@Test
	public void testLookupColumnReferenceWithNullRef() throws ParseException {
		QuerySpecification model = new TableQueryParser("select * from syn123 t join syn456").querySpecification();

		when(mockSchemaProvider.getTableSchema(IdAndVersion.parse("syn123")))
				.thenReturn(List.of(columnMap.get("foo"), columnMap.get("has space")));
		when(mockSchemaProvider.getTableSchema(IdAndVersion.parse("syn456")))
				.thenReturn(List.of(columnMap.get("bar"), columnMap.get("foo_bar")));
		
		TableAndColumnMapper mapper = new TableAndColumnMapper(model, mockSchemaProvider);
		// call under test
		assertEquals(Optional.empty(), mapper.lookupColumnReference((ColumnReference)null));
	}

	@Test
	public void testLookupColumnReferenceWithMultipleTables() throws ParseException {
		QuerySpecification model = new TableQueryParser("select * from syn123 t join syn456").querySpecification();
		
		when(mockSchemaProvider.getTableSchema(IdAndVersion.parse("syn123")))
				.thenReturn(List.of(columnMap.get("foo"), columnMap.get("has space")));
		when(mockSchemaProvider.getTableSchema(IdAndVersion.parse("syn456")))
				.thenReturn(List.of(columnMap.get("bar"), columnMap.get("foo_bar")));
		
		TableAndColumnMapper mapper = new TableAndColumnMapper(model, mockSchemaProvider);

		ColumnReference columnReference = new TableQueryParser("syn456.foo_bar").columnReference();
		// call under test
		assertEquals(Optional.of(new SchemaColumnTranslationReference(columnMap.get("foo_bar"))),
				mapper.lookupColumnReference(columnReference));
	}
	
	@Test
	public void testLookupColumnReferenceWithString() throws ParseException {
		QuerySpecification model = new TableQueryParser("select * from syn123").querySpecification();
		
		when(mockSchemaProvider.getTableSchema(IdAndVersion.parse("syn123")))
				.thenReturn(List.of(columnMap.get("foo"), columnMap.get("has space")));
		
		TableAndColumnMapper mapper = new TableAndColumnMapper(model, mockSchemaProvider);

		// call under test
		assertEquals(Optional.of(new SchemaColumnTranslationReference(columnMap.get("foo"))),
				mapper.lookupColumnReferenceByName("foo"));
	}
	
	@Test
	public void testLookupColumnReferenceWithNullString() throws ParseException {
		QuerySpecification model = new TableQueryParser("select * from syn123").querySpecification();
		
		when(mockSchemaProvider.getTableSchema(IdAndVersion.parse("syn123")))
				.thenReturn(List.of(columnMap.get("foo"), columnMap.get("has space")));
		
		TableAndColumnMapper mapper = new TableAndColumnMapper(model, mockSchemaProvider);

		// call under test
		assertEquals(Optional.empty(),	mapper.lookupColumnReferenceByName((String)null));
	}
	
	@Test
	public void testLookupColumnReferenceWithStringSpace() throws ParseException {
		QuerySpecification model = new TableQueryParser("select * from syn123").querySpecification();

		when(mockSchemaProvider.getTableSchema(IdAndVersion.parse("syn123")))
				.thenReturn(List.of(columnMap.get("foo"), columnMap.get("has space")));
		
		TableAndColumnMapper mapper = new TableAndColumnMapper(model, mockSchemaProvider);

		// call under test
		assertEquals(Optional.of(new SchemaColumnTranslationReference(columnMap.get("has space"))),
				mapper.lookupColumnReferenceByName("has space"));
	}
	
	@Test
	public void testLookupColumnReferenceWithStringQuote() throws ParseException {
		QuerySpecification model = new TableQueryParser("select * from syn123").querySpecification();

		when(mockSchemaProvider.getTableSchema(IdAndVersion.parse("syn123")))
				.thenReturn(List.of(columnMap.get("has\"quote"), columnMap.get("has space")));
		
		TableAndColumnMapper mapper = new TableAndColumnMapper(model, mockSchemaProvider);

		// call under test
		assertEquals(Optional.of(new SchemaColumnTranslationReference(columnMap.get("has\"quote"))),
				mapper.lookupColumnReferenceByName("has\"quote"));
	}
	
	@Test
	public void testLookupColumnReferenceWithStringKeyword() throws ParseException {
		QuerySpecification model = new TableQueryParser("select * from syn123").querySpecification();
		
		when(mockSchemaProvider.getTableSchema(IdAndVersion.parse("syn123")))
				.thenReturn(List.of(columnMap.get("year"), columnMap.get("has space")));
		
		TableAndColumnMapper mapper = new TableAndColumnMapper(model, mockSchemaProvider);

		// call under test
		assertEquals(Optional.of(new SchemaColumnTranslationReference(columnMap.get("year"))),
				mapper.lookupColumnReferenceByName("year"));
	}
	
	@Test
	public void testLookupColumnReferenceWithMultipleTablesTranslated() throws ParseException {
		QuerySpecification model = new TableQueryParser("select * from syn123 t join syn456").querySpecification();
		
		when(mockSchemaProvider.getTableSchema(IdAndVersion.parse("syn123")))
				.thenReturn(List.of(columnMap.get("foo"), columnMap.get("has space")));
		when(mockSchemaProvider.getTableSchema(IdAndVersion.parse("syn456")))
				.thenReturn(List.of(columnMap.get("bar"), columnMap.get("foo_bar")));
		
		TableAndColumnMapper mapper = new TableAndColumnMapper(model, mockSchemaProvider);
		
		ColumnReference columnReference = new TableQueryParser("_A1._C444_").columnReference();
		// call under test
		assertEquals(Optional.of(new SchemaColumnTranslationReference(columnMap.get("foo_bar"))),
				mapper.lookupColumnReference(columnReference));
	}
	
	@Test
	public void testLookupColumnReferenceWithMultipleTablesNoMatch() throws ParseException {
		QuerySpecification model = new TableQueryParser("select * from syn123 t join syn456").querySpecification();
		
		when(mockSchemaProvider.getTableSchema(IdAndVersion.parse("syn123")))
				.thenReturn(List.of(columnMap.get("foo"), columnMap.get("has space")));
		when(mockSchemaProvider.getTableSchema(IdAndVersion.parse("syn456")))
				.thenReturn(List.of(columnMap.get("bar"), columnMap.get("foo_bar")));
		
		TableAndColumnMapper mapper = new TableAndColumnMapper(model, mockSchemaProvider);

		ColumnReference columnReference = new TableQueryParser("syn456.nothere").columnReference();
		// call under test
		assertEquals(Optional.empty(),	mapper.lookupColumnReference(columnReference));
	}
	
	@Test
	public void testLookupColumnReferenceWithNullLHSAndMultipleTables() throws ParseException {
		QuerySpecification model = new TableQueryParser("select * from syn123 t join syn456").querySpecification();
		
		when(mockSchemaProvider.getTableSchema(IdAndVersion.parse("syn123")))
				.thenReturn(List.of(columnMap.get("foo"), columnMap.get("has space")));
		when(mockSchemaProvider.getTableSchema(IdAndVersion.parse("syn456")))
				.thenReturn(List.of(columnMap.get("bar"), columnMap.get("foo_bar")));
		
		TableAndColumnMapper mapper = new TableAndColumnMapper(model, mockSchemaProvider);

		ColumnReference columnReference = new TableQueryParser("foo").columnReference();
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			mapper.lookupColumnReference(columnReference);
		}).getMessage();
		assertEquals("Expected a table name or table alias for column: foo", message);
	}

	@Test
	public void testLookupColumnReferenceWithNullLHSAndSingle() throws ParseException {
		QuerySpecification model = new TableQueryParser("select * from syn123").querySpecification();
		
		when(mockSchemaProvider.getTableSchema(IdAndVersion.parse("syn123")))
				.thenReturn(List.of(columnMap.get("foo"), columnMap.get("has space")));
		
		TableAndColumnMapper mapper = new TableAndColumnMapper(model, mockSchemaProvider);

		ColumnReference columnReference = new TableQueryParser("foo").columnReference();
		// call under test
		assertEquals(Optional.of(new SchemaColumnTranslationReference(columnMap.get("foo"))),
				mapper.lookupColumnReference(columnReference));
	}
	
	
	@Test
	public void testLookupColumnReferenceMatchWithNullRef() throws ParseException {
		QuerySpecification model = new TableQueryParser("select * from syn123 t join syn456").querySpecification();
		
		when(mockSchemaProvider.getTableSchema(IdAndVersion.parse("syn123")))
				.thenReturn(List.of(columnMap.get("foo"), columnMap.get("has space")));
		when(mockSchemaProvider.getTableSchema(IdAndVersion.parse("syn456")))
				.thenReturn(List.of(columnMap.get("bar"), columnMap.get("foo_bar")));
		
		TableAndColumnMapper mapper = new TableAndColumnMapper(model, mockSchemaProvider);
		// call under test
		assertEquals(Optional.empty(), mapper.lookupColumnReferenceMatch(null));
	}

	@Test
	public void testLookupColumnReferenceMatchWithMultipleTables() throws ParseException {
		QuerySpecification model = new TableQueryParser("select * from syn123 t join syn456").querySpecification();
		
		when(mockSchemaProvider.getTableSchema(IdAndVersion.parse("syn123")))
				.thenReturn(List.of(columnMap.get("foo"), columnMap.get("has space")));
		when(mockSchemaProvider.getTableSchema(IdAndVersion.parse("syn456")))
				.thenReturn(List.of(columnMap.get("bar"), columnMap.get("foo_bar")));
		
		TableAndColumnMapper mapper = new TableAndColumnMapper(model, mockSchemaProvider);

		ColumnReference columnReference = new TableQueryParser("syn456.foo_bar").columnReference();
		// call under test
		Optional<ColumnReferenceMatch> optionalMatch = mapper.lookupColumnReferenceMatch(columnReference);
		assertTrue(optionalMatch.isPresent());
		assertEquals(new SchemaColumnTranslationReference(columnMap.get("foo_bar")), optionalMatch.get().getColumnTranslationReference());
		TableInfo tableInfo  = optionalMatch.get().getTableInfo();
		assertEquals("syn456", tableInfo.getOriginalTableName());
		assertEquals(1, tableInfo.getTableIndex());
	}
	
	@Test
	public void testLookupColumnReferenceMatchWithBenefactor() throws ParseException {
		QuerySpecification model = new TableQueryParser("select * from syn123 t join syn456").querySpecification();
		
		when(mockSchemaProvider.getTableSchema(IdAndVersion.parse("syn123")))
				.thenReturn(List.of(columnMap.get("foo"), columnMap.get("has space")));
		when(mockSchemaProvider.getTableSchema(IdAndVersion.parse("syn456")))
				.thenReturn(List.of(columnMap.get("bar"), columnMap.get("foo_bar")));
		
		TableAndColumnMapper mapper = new TableAndColumnMapper(model, mockSchemaProvider);

		ColumnReference columnReference = new TableQueryParser("T123.ROW_BENEFACTOR").columnReference();
		// call under test
		Optional<ColumnReferenceMatch> optionalMatch = mapper.lookupColumnReferenceMatch(columnReference);
		assertTrue(optionalMatch.isPresent());
		assertEquals(RowMetadataColumnTranslationReference.ROW_BENEFACTOR.getColumnTranslationReference(), optionalMatch.get().getColumnTranslationReference());
		TableInfo tableInfo  = optionalMatch.get().getTableInfo();
		assertEquals("syn123", tableInfo.getOriginalTableName());
		assertEquals(0, tableInfo.getTableIndex());
	}
	
	@Test
	public void testLookupColumnReferenceMatchWithBenefactorAndVersion() throws ParseException {
		QuerySpecification model = new TableQueryParser("select * from syn123 t join syn456.3").querySpecification();
		
		when(mockSchemaProvider.getTableSchema(IdAndVersion.parse("syn123")))
				.thenReturn(List.of(columnMap.get("foo"), columnMap.get("has space")));
		when(mockSchemaProvider.getTableSchema(IdAndVersion.parse("syn456.3")))
				.thenReturn(List.of(columnMap.get("bar"), columnMap.get("foo_bar")));
		
		TableAndColumnMapper mapper = new TableAndColumnMapper(model, mockSchemaProvider);

		ColumnReference columnReference = new TableQueryParser("T456_3.ROW_BENEFACTOR").columnReference();
		// call under test
		Optional<ColumnReferenceMatch> optionalMatch = mapper.lookupColumnReferenceMatch(columnReference);
		assertTrue(optionalMatch.isPresent());
		assertEquals(RowMetadataColumnTranslationReference.ROW_BENEFACTOR.getColumnTranslationReference(), optionalMatch.get().getColumnTranslationReference());
		TableInfo tableInfo  = optionalMatch.get().getTableInfo();
		assertEquals("syn456.3", tableInfo.getOriginalTableName());
		assertEquals(1, tableInfo.getTableIndex());
	}
	
	@Test
	public void testLookupColumnReferenceMatchWithMultipleTablesFirstTable() throws ParseException {
		QuerySpecification model = new TableQueryParser("select * from syn123 t join syn456").querySpecification();
		
		when(mockSchemaProvider.getTableSchema(IdAndVersion.parse("syn123")))
				.thenReturn(List.of(columnMap.get("foo"), columnMap.get("has space")));
		when(mockSchemaProvider.getTableSchema(IdAndVersion.parse("syn456")))
				.thenReturn(List.of(columnMap.get("bar"), columnMap.get("foo_bar")));
		
		TableAndColumnMapper mapper = new TableAndColumnMapper(model, mockSchemaProvider);

		ColumnReference columnReference = new TableQueryParser("syn123.foo").columnReference();
		// call under test
		Optional<ColumnReferenceMatch> optionalMatch = mapper.lookupColumnReferenceMatch(columnReference);
		assertTrue(optionalMatch.isPresent());
		assertEquals(new SchemaColumnTranslationReference(columnMap.get("foo")), optionalMatch.get().getColumnTranslationReference());
		TableInfo tableInfo  = optionalMatch.get().getTableInfo();
		assertEquals("syn123", tableInfo.getOriginalTableName());
		assertEquals(0, tableInfo.getTableIndex());
	}
	
	@Test
	public void testLookupColumnReferenceWithROW_ID() throws ParseException {
		QuerySpecification model = new TableQueryParser("select * from syn123 t join syn456").querySpecification();
		
		when(mockSchemaProvider.getTableSchema(IdAndVersion.parse("syn123")))
				.thenReturn(List.of(columnMap.get("foo"), columnMap.get("has space")));
		when(mockSchemaProvider.getTableSchema(IdAndVersion.parse("syn456")))
				.thenReturn(List.of(columnMap.get("bar"), columnMap.get("foo_bar")));
		
		TableAndColumnMapper mapper = new TableAndColumnMapper(model, mockSchemaProvider);

		ColumnReference columnReference = new TableQueryParser("syn123.ROW_ID").columnReference();
		// call under test
		Optional<ColumnReferenceMatch> optionalMatch = mapper.lookupColumnReferenceMatch(columnReference);
		assertTrue(optionalMatch.isPresent());
		assertEquals(RowMetadataColumnTranslationReference.ROW_ID.getColumnTranslationReference(), optionalMatch.get().getColumnTranslationReference());
		TableInfo tableInfo  = optionalMatch.get().getTableInfo();
		assertEquals("syn123", tableInfo.getOriginalTableName());
		assertEquals(0, tableInfo.getTableIndex());
	}
	
	@Test
	public void testLookupColumnReferenceMatchWithMultipleTablesNoMatch() throws ParseException {
		QuerySpecification model = new TableQueryParser("select * from syn123 t join syn456").querySpecification();
		
		when(mockSchemaProvider.getTableSchema(IdAndVersion.parse("syn123")))
				.thenReturn(List.of(columnMap.get("foo"), columnMap.get("has space")));
		when(mockSchemaProvider.getTableSchema(IdAndVersion.parse("syn456")))
				.thenReturn(List.of(columnMap.get("bar"), columnMap.get("foo_bar")));
		
		TableAndColumnMapper mapper = new TableAndColumnMapper(model, mockSchemaProvider);

		ColumnReference columnReference = new TableQueryParser("syn456.nothere").columnReference();
		// call under test
		assertEquals(Optional.empty(),	mapper.lookupColumnReferenceMatch(columnReference));
	}
	
	@Test
	public void testLookupColumnReferenceMatchWithMultipleAliasOfSameTable() throws ParseException {
		QuerySpecification model = new TableQueryParser("select * from syn123 t1 join syn456 join syn123 t2")
				.querySpecification();
		
		when(mockSchemaProvider.getTableSchema(IdAndVersion.parse("syn123")))
				.thenReturn(List.of(columnMap.get("foo"), columnMap.get("has space")));
		when(mockSchemaProvider.getTableSchema(IdAndVersion.parse("syn456")))
				.thenReturn(List.of(columnMap.get("bar"), columnMap.get("foo_bar")));
		
		TableAndColumnMapper mapper = new TableAndColumnMapper(model, mockSchemaProvider);

		// call under test
		Optional<ColumnReferenceMatch> optionalMatch = mapper
				.lookupColumnReferenceMatch(new TableQueryParser("t2.`has space`").columnReference());
		assertTrue(optionalMatch.isPresent());
		assertEquals(new SchemaColumnTranslationReference(columnMap.get("has space")),
				optionalMatch.get().getColumnTranslationReference());
		TableInfo tableInfo = optionalMatch.get().getTableInfo();
		assertEquals("syn123", tableInfo.getOriginalTableName());
		assertEquals(2, tableInfo.getTableIndex());
	}

	@Test
	public void testLookupColumnReferenceMatchWithNullLHSAndMultipleTables() throws ParseException {
		QuerySpecification model = new TableQueryParser("select * from syn123 t join syn456").querySpecification();
		
		when(mockSchemaProvider.getTableSchema(IdAndVersion.parse("syn123")))
				.thenReturn(List.of(columnMap.get("foo"), columnMap.get("has space")));
		when(mockSchemaProvider.getTableSchema(IdAndVersion.parse("syn456")))
				.thenReturn(List.of(columnMap.get("bar"), columnMap.get("foo_bar")));
		
		TableAndColumnMapper mapper = new TableAndColumnMapper(model, mockSchemaProvider);

		ColumnReference columnReference = new TableQueryParser("foo").columnReference();
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			mapper.lookupColumnReferenceMatch(columnReference);
		}).getMessage();
		assertEquals("Expected a table name or table alias for column: foo", message);
	}

	@Test
	public void testLookupColumnReferenceMatchWithNullLHSAndSingle() throws ParseException {
		QuerySpecification model = new TableQueryParser("select * from syn123").querySpecification();
		
		when(mockSchemaProvider.getTableSchema(IdAndVersion.parse("syn123")))
				.thenReturn(List.of(columnMap.get("foo"), columnMap.get("has space")));
		
		TableAndColumnMapper mapper = new TableAndColumnMapper(model, mockSchemaProvider);

		ColumnReference columnReference = new TableQueryParser("foo").columnReference();
		// call under test
		Optional<ColumnReferenceMatch> optionalMatch = mapper.lookupColumnReferenceMatch(columnReference);
		assertTrue(optionalMatch.isPresent());
		assertEquals(new SchemaColumnTranslationReference(columnMap.get("foo")), optionalMatch.get().getColumnTranslationReference());
		TableInfo tableInfo  = optionalMatch.get().getTableInfo();
		assertEquals("syn123", tableInfo.getOriginalTableName());
		assertEquals(0, tableInfo.getTableIndex());
	}
	
	@Test
	public void testLookupTableNameCorrelationWithSingleTable() throws ParseException {
		QuerySpecification model = new TableQueryParser("select * from syn123").querySpecification();
	
		when(mockSchemaProvider.getTableSchema(IdAndVersion.parse("syn123")))
				.thenReturn(List.of(columnMap.get("foo"), columnMap.get("has space")));
		
		TableAndColumnMapper mapper = new TableAndColumnMapper(model, mockSchemaProvider);

		TableNameCorrelation tableNameCorrelation = new TableQueryParser("syn123").tableNameCorrelation();
		// call under test
		Optional<TableInfo> optionalMatch = mapper.lookupTableNameCorrelation(tableNameCorrelation);
		assertTrue(optionalMatch.isPresent());
		TableInfo tableInfo  = optionalMatch.get();
		assertEquals("syn123", tableInfo.getOriginalTableName());
		assertEquals(0, tableInfo.getTableIndex());
	}
	
	@Test
	public void testLookupTableNameCorrelationWithSingleTableWithMatchingAlias() throws ParseException {
		QuerySpecification model = new TableQueryParser("select * from syn123 a").querySpecification();
		
		when(mockSchemaProvider.getTableSchema(IdAndVersion.parse("syn123")))
				.thenReturn(List.of(columnMap.get("foo"), columnMap.get("has space")));
		
		TableAndColumnMapper mapper = new TableAndColumnMapper(model, mockSchemaProvider);

		TableNameCorrelation tableNameCorrelation = new TableQueryParser("syn123 a").tableNameCorrelation();
		// call under test
		Optional<TableInfo> optionalMatch = mapper.lookupTableNameCorrelation(tableNameCorrelation);
		assertTrue(optionalMatch.isPresent());
		TableInfo tableInfo  = optionalMatch.get();
		assertEquals("syn123", tableInfo.getOriginalTableName());
		assertEquals(0, tableInfo.getTableIndex());
	}
	
	// alias is not used for lookup
	@Test
	public void testLookupTableNameCorrelationWithSingleTableWithNonmatchingAlias() throws ParseException {
		QuerySpecification model = new TableQueryParser("select * from syn123 r").querySpecification();
		
		when(mockSchemaProvider.getTableSchema(IdAndVersion.parse("syn123")))
				.thenReturn(List.of(columnMap.get("foo"), columnMap.get("has space")));
		
		TableAndColumnMapper mapper = new TableAndColumnMapper(model, mockSchemaProvider);

		TableNameCorrelation tableNameCorrelation = new TableQueryParser("syn123 _A0").tableNameCorrelation();
		// call under test
		Optional<TableInfo> optionalMatch = mapper.lookupTableNameCorrelation(tableNameCorrelation);
		assertTrue(optionalMatch.isPresent());
		TableInfo tableInfo  = optionalMatch.get();
		assertEquals("syn123", tableInfo.getOriginalTableName());
		assertEquals(0, tableInfo.getTableIndex());
	}
	
	@Test
	public void testLookupTableNameCorrelationWithSingleTableWithTranslated() throws ParseException {
		QuerySpecification model = new TableQueryParser("select * from syn123").querySpecification();
		
		when(mockSchemaProvider.getTableSchema(IdAndVersion.parse("syn123")))
				.thenReturn(List.of(columnMap.get("foo"), columnMap.get("has space")));
		
		TableAndColumnMapper mapper = new TableAndColumnMapper(model, mockSchemaProvider);

		TableNameCorrelation tableNameCorrelation = new TableQueryParser("T123").tableNameCorrelation();
		// call under test
		Optional<TableInfo> optionalMatch = mapper.lookupTableNameCorrelation(tableNameCorrelation);
		assertTrue(optionalMatch.isPresent());
		TableInfo tableInfo  = optionalMatch.get();
		assertEquals("syn123", tableInfo.getOriginalTableName());
		assertEquals(0, tableInfo.getTableIndex());
	}
	
	@Test
	public void testLookupTableNameCorrelationWithSingleTableWithNoMatch() throws ParseException {
		QuerySpecification model = new TableQueryParser("select * from syn123").querySpecification();
		
		when(mockSchemaProvider.getTableSchema(IdAndVersion.parse("syn123")))
				.thenReturn(List.of(columnMap.get("foo"), columnMap.get("has space")));
		
		TableAndColumnMapper mapper = new TableAndColumnMapper(model, mockSchemaProvider);

		TableNameCorrelation tableNameCorrelation = new TableQueryParser("syn456").tableNameCorrelation();
		// call under test
		Optional<TableInfo> optionalMatch = mapper.lookupTableNameCorrelation(tableNameCorrelation);
		assertEquals(Optional.empty(), optionalMatch);
	}
	
	@Test
	public void testLookupTableNameCorrelationWithMultipleTablesMatchFrist() throws ParseException {
		QuerySpecification model = new TableQueryParser("select * from syn123 join syn456").querySpecification();

		when(mockSchemaProvider.getTableSchema(IdAndVersion.parse("syn123")))
				.thenReturn(List.of(columnMap.get("foo"), columnMap.get("has space")));
		when(mockSchemaProvider.getTableSchema(IdAndVersion.parse("syn456")))
				.thenReturn(List.of(columnMap.get("bar"), columnMap.get("foo_bar")));
		
		TableAndColumnMapper mapper = new TableAndColumnMapper(model, mockSchemaProvider);

		TableNameCorrelation tableNameCorrelation = new TableQueryParser("syn123").tableNameCorrelation();
		// call under test
		Optional<TableInfo> optionalMatch = mapper.lookupTableNameCorrelation(tableNameCorrelation);
		assertTrue(optionalMatch.isPresent());
		TableInfo tableInfo  = optionalMatch.get();
		assertEquals("syn123", tableInfo.getOriginalTableName());
		assertEquals(0, tableInfo.getTableIndex());
	}

	@Test
	public void testLookupTableNameCorrelationWithMultipleTablesMatchLast() throws ParseException {
		QuerySpecification model = new TableQueryParser("select * from syn123 join syn456").querySpecification();
		
		when(mockSchemaProvider.getTableSchema(IdAndVersion.parse("syn123")))
				.thenReturn(List.of(columnMap.get("foo"), columnMap.get("has space")));
		when(mockSchemaProvider.getTableSchema(IdAndVersion.parse("syn456")))
				.thenReturn(List.of(columnMap.get("bar"), columnMap.get("foo_bar")));
		
		TableAndColumnMapper mapper = new TableAndColumnMapper(model, mockSchemaProvider);

		TableNameCorrelation tableNameCorrelation = new TableQueryParser("syn456").tableNameCorrelation();
		// call under test
		Optional<TableInfo> optionalMatch = mapper.lookupTableNameCorrelation(tableNameCorrelation);
		assertTrue(optionalMatch.isPresent());
		TableInfo tableInfo  = optionalMatch.get();
		assertEquals("syn456", tableInfo.getOriginalTableName());
		assertEquals(1, tableInfo.getTableIndex());
	}
	
	@Test
	public void testLookupTableNameCorrelationWithMultipleTablesNoMatch() throws ParseException {
		QuerySpecification model = new TableQueryParser("select * from syn123 join syn456").querySpecification();
		
		when(mockSchemaProvider.getTableSchema(IdAndVersion.parse("syn123")))
				.thenReturn(List.of(columnMap.get("foo"), columnMap.get("has space")));
		when(mockSchemaProvider.getTableSchema(IdAndVersion.parse("syn456")))
				.thenReturn(List.of(columnMap.get("bar"), columnMap.get("foo_bar")));
		
		TableAndColumnMapper mapper = new TableAndColumnMapper(model, mockSchemaProvider);

		TableNameCorrelation tableNameCorrelation = new TableQueryParser("syn789").tableNameCorrelation();
		// call under test
		Optional<TableInfo> optionalMatch = mapper.lookupTableNameCorrelation(tableNameCorrelation);
		assertEquals(Optional.empty(), optionalMatch);
	}
	
	@Test
	public void testLookupTableNameCorrelationWithMultipleTablesSameName() throws ParseException {
		QuerySpecification model = new TableQueryParser("select * from syn123 r1 join syn456 join syn123 r2").querySpecification();
		
		when(mockSchemaProvider.getTableSchema(IdAndVersion.parse("syn123")))
				.thenReturn(List.of(columnMap.get("foo"), columnMap.get("has space")));
		when(mockSchemaProvider.getTableSchema(IdAndVersion.parse("syn456")))
				.thenReturn(List.of(columnMap.get("bar"), columnMap.get("foo_bar")));
		
		TableAndColumnMapper mapper = new TableAndColumnMapper(model, mockSchemaProvider);

		// call under test
		Optional<TableInfo> optionalMatch = mapper.lookupTableNameCorrelation(new TableQueryParser("syn123 r2").tableNameCorrelation());
		assertTrue(optionalMatch.isPresent());
		TableInfo tableInfo  = optionalMatch.get();
		assertEquals("syn123", tableInfo.getOriginalTableName());
		assertEquals("r2", tableInfo.getTableAlias().get());
		assertEquals(2, tableInfo.getTableIndex());
	}
	
	@Test
	public void testLookupTableNameCorrelationWithMultipleTablesMissingAlias() throws ParseException {
		QuerySpecification model = new TableQueryParser("select * from syn123 r1 join syn456 join syn123 r2").querySpecification();
	
		when(mockSchemaProvider.getTableSchema(IdAndVersion.parse("syn123")))
				.thenReturn(List.of(columnMap.get("foo"), columnMap.get("has space")));
		when(mockSchemaProvider.getTableSchema(IdAndVersion.parse("syn456")))
				.thenReturn(List.of(columnMap.get("bar"), columnMap.get("foo_bar")));
		
		TableAndColumnMapper mapper = new TableAndColumnMapper(model, mockSchemaProvider);

		// call under test
		Optional<TableInfo> optionalMatch = mapper.lookupTableNameCorrelation(new TableQueryParser("syn123").tableNameCorrelation());
		assertEquals(Optional.empty(), optionalMatch);
	}
	
	@Test
	public void testgetSingleTableIdWithSingleTable() throws ParseException {
		QuerySpecification model = new TableQueryParser("select * from syn123").querySpecification();
		
		when(mockSchemaProvider.getTableSchema(IdAndVersion.parse("syn123")))
				.thenReturn(List.of(columnMap.get("foo"), columnMap.get("has space")));
		
		TableAndColumnMapper mapper = new TableAndColumnMapper(model, mockSchemaProvider);
		// call under test
		assertEquals(Optional.of(IdAndVersion.parse("syn123")), mapper.getSingleTableId());
	}
	
	@Test
	public void testgetSingleTableIdWithMultipleTables() throws ParseException {
		QuerySpecification model = new TableQueryParser("select * from syn123 join syn456").querySpecification();

		when(mockSchemaProvider.getTableSchema(IdAndVersion.parse("syn123")))
				.thenReturn(List.of(columnMap.get("foo"), columnMap.get("has space")));
		when(mockSchemaProvider.getTableSchema(IdAndVersion.parse("syn456")))
				.thenReturn(List.of(columnMap.get("bar"), columnMap.get("foo_bar")));
		
		TableAndColumnMapper mapper = new TableAndColumnMapper(model, mockSchemaProvider);
		// call under test
		assertEquals(Optional.empty(), mapper.getSingleTableId());
	}
	

}
