package org.sagebionetworks.table.cluster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.dbo.dao.table.TableModelTestUtils;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.table.cluster.columntranslation.SchemaColumnTranslationReference;
import org.sagebionetworks.table.query.ParseException;
import org.sagebionetworks.table.query.TableQueryParser;
import org.sagebionetworks.table.query.model.ColumnReference;
import org.sagebionetworks.table.query.model.QuerySpecification;
import org.sagebionetworks.table.query.model.SelectList;

public class TableAndColumnMapperTest {

	private List<ColumnModel> allColumns;

	@BeforeEach
	public void before() {
		allColumns = Arrays.asList(
				TableModelTestUtils.createColumn(111L, "foo", ColumnType.STRING),
				TableModelTestUtils.createColumn(222L, "has space", ColumnType.STRING),
				TableModelTestUtils.createColumn(333L, "bar", ColumnType.INTEGER),
				TableModelTestUtils.createColumn(444L, "foo_bar", ColumnType.STRING),
				TableModelTestUtils.createColumn(555L, "Foo", ColumnType.STRING),
				TableModelTestUtils.createColumn(666L, "datetype", ColumnType.DATE),
				TableModelTestUtils.createColumn(777L, "has\"quote", ColumnType.STRING));
	}

	@Test
	public void testConstructorWithSingleTable() throws ParseException {
		QuerySpecification model = new TableQueryParser("select * from syn123").querySpecification();
		Map<IdAndVersion, List<ColumnModel>> map = new LinkedHashMap<>();
		map.put(IdAndVersion.parse("syn123"), allColumns);

		// call under test
		TableAndColumnMapper mapper = new TableAndColumnMapper(model, new TestSchemaProvider(map));
		assertEquals(Arrays.asList(IdAndVersion.parse("syn123")), mapper.getTableIds());
		assertEquals(allColumns, mapper.getUnionOfAllTableSchemas());
	}

	@Test
	public void testConstructorWithMultipleTables() throws ParseException {
		QuerySpecification model = new TableQueryParser("select * from syn123 join syn456").querySpecification();
		Map<IdAndVersion, List<ColumnModel>> map = new LinkedHashMap<>();
		map.put(IdAndVersion.parse("syn123"), Arrays.asList(allColumns.get(0), allColumns.get(1), allColumns.get(2)));
		map.put(IdAndVersion.parse("syn456"), Arrays.asList(allColumns.get(3), allColumns.get(4), allColumns.get(5)));
		// call under test
		TableAndColumnMapper mapper = new TableAndColumnMapper(model, new TestSchemaProvider(map));
		assertEquals(Arrays.asList(IdAndVersion.parse("syn123"), IdAndVersion.parse("syn456")), mapper.getTableIds());
		assertEquals(allColumns.subList(0, 6), mapper.getUnionOfAllTableSchemas());
	}

	@Test
	public void testConstructorWithNullModel() throws ParseException {
		QuerySpecification model = null;
		Map<IdAndVersion, List<ColumnModel>> map = new LinkedHashMap<>();
		map.put(IdAndVersion.parse("syn123"), allColumns);

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			new TableAndColumnMapper(model, new TestSchemaProvider(map));
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
		Map<IdAndVersion, List<ColumnModel>> map = new LinkedHashMap<>();
		map.put(IdAndVersion.parse("syn123"), Collections.emptyList());

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			new TableAndColumnMapper(model, new TestSchemaProvider(map));
		}).getMessage();
		assertEquals("Schema for syn123 is empty.", message);
	}

	@Test
	public void testBuildSelectAllColumns() throws ParseException {
		QuerySpecification model = new TableQueryParser("select * from syn123").querySpecification();
		Map<IdAndVersion, List<ColumnModel>> map = new LinkedHashMap<>();
		map.put(IdAndVersion.parse("syn123"), allColumns);
		TableAndColumnMapper mapper = new TableAndColumnMapper(model, new TestSchemaProvider(map));

		// call under test
		SelectList selectList = mapper.buildSelectAllColumns();
		assertNotNull(selectList);
		assertEquals("\"foo\", \"has space\", \"bar\", \"foo_bar\", \"Foo\", \"datetype\", \"has\"\"quote\"",
				selectList.toSql());
	}

	@Test
	public void testBuildSelectAllColumnsWithJoinWithoutAlias() throws ParseException {
		QuerySpecification model = new TableQueryParser("select * from syn123 join syn456").querySpecification();
		Map<IdAndVersion, List<ColumnModel>> map = new LinkedHashMap<>();
		map.put(IdAndVersion.parse("syn123"), Arrays.asList(allColumns.get(0), allColumns.get(1)));
		map.put(IdAndVersion.parse("syn456"), Arrays.asList(allColumns.get(2), allColumns.get(3)));
		TableAndColumnMapper mapper = new TableAndColumnMapper(model, new TestSchemaProvider(map));

		// call under test
		SelectList selectList = mapper.buildSelectAllColumns();
		assertNotNull(selectList);
		assertEquals("syn123.\"foo\", syn123.\"has space\", syn456.\"bar\", syn456.\"foo_bar\"", selectList.toSql());
	}

	@Test
	public void testBuildSelectAllColumnsWithJoinWithAlias() throws ParseException {
		QuerySpecification model = new TableQueryParser("select * from syn123 t join syn456 r").querySpecification();
		Map<IdAndVersion, List<ColumnModel>> map = new LinkedHashMap<>();
		map.put(IdAndVersion.parse("syn123"), Arrays.asList(allColumns.get(0), allColumns.get(1)));
		map.put(IdAndVersion.parse("syn456"), Arrays.asList(allColumns.get(2), allColumns.get(3)));
		TableAndColumnMapper mapper = new TableAndColumnMapper(model, new TestSchemaProvider(map));

		// call under test
		SelectList selectList = mapper.buildSelectAllColumns();
		assertNotNull(selectList);
		assertEquals("t.\"foo\", t.\"has space\", r.\"bar\", r.\"foo_bar\"", selectList.toSql());
	}

	@Test
	public void testBuildSelectAllColumnsWithJoinWithMixedAlias() throws ParseException {
		QuerySpecification model = new TableQueryParser("select * from syn123 t join syn456").querySpecification();
		Map<IdAndVersion, List<ColumnModel>> map = new LinkedHashMap<>();
		map.put(IdAndVersion.parse("syn123"), Arrays.asList(allColumns.get(0), allColumns.get(1)));
		map.put(IdAndVersion.parse("syn456"), Arrays.asList(allColumns.get(2), allColumns.get(3)));
		TableAndColumnMapper mapper = new TableAndColumnMapper(model, new TestSchemaProvider(map));

		// call under test
		SelectList selectList = mapper.buildSelectAllColumns();
		assertNotNull(selectList);
		assertEquals("t.\"foo\", t.\"has space\", syn456.\"bar\", syn456.\"foo_bar\"", selectList.toSql());
	}

	@Test
	public void testLookupColumnReferenceWithNullRef() throws ParseException {
		QuerySpecification model = new TableQueryParser("select * from syn123 t join syn456").querySpecification();
		Map<IdAndVersion, List<ColumnModel>> map = new LinkedHashMap<>();
		map.put(IdAndVersion.parse("syn123"), Arrays.asList(allColumns.get(0), allColumns.get(1)));
		map.put(IdAndVersion.parse("syn456"), Arrays.asList(allColumns.get(2), allColumns.get(3)));
		TableAndColumnMapper mapper = new TableAndColumnMapper(model, new TestSchemaProvider(map));
		// call under test
		assertEquals(Optional.empty(), mapper.lookupColumnReference(null));
	}

	@Test
	public void testLookupColumnReferenceWithMultipleTables() throws ParseException {
		QuerySpecification model = new TableQueryParser("select * from syn123 t join syn456").querySpecification();
		Map<IdAndVersion, List<ColumnModel>> map = new LinkedHashMap<>();
		map.put(IdAndVersion.parse("syn123"), Arrays.asList(allColumns.get(0), allColumns.get(1)));
		map.put(IdAndVersion.parse("syn456"), Arrays.asList(allColumns.get(2), allColumns.get(3)));
		TableAndColumnMapper mapper = new TableAndColumnMapper(model, new TestSchemaProvider(map));

		ColumnReference columnReference = new TableQueryParser("syn456.foo_bar").columnReference();
		// call under test
		assertEquals(Optional.of(new SchemaColumnTranslationReference(allColumns.get(3))),
				mapper.lookupColumnReference(columnReference));
	}

	@Test
	public void testLookupColumnReferenceWithNullLHSAndMultipleTables() throws ParseException {
		QuerySpecification model = new TableQueryParser("select * from syn123 t join syn456").querySpecification();
		Map<IdAndVersion, List<ColumnModel>> map = new LinkedHashMap<>();
		map.put(IdAndVersion.parse("syn123"), Arrays.asList(allColumns.get(0), allColumns.get(1)));
		map.put(IdAndVersion.parse("syn456"), Arrays.asList(allColumns.get(2), allColumns.get(3)));
		TableAndColumnMapper mapper = new TableAndColumnMapper(model, new TestSchemaProvider(map));

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
		Map<IdAndVersion, List<ColumnModel>> map = new LinkedHashMap<>();
		map.put(IdAndVersion.parse("syn123"), Arrays.asList(allColumns.get(0), allColumns.get(1)));
		TableAndColumnMapper mapper = new TableAndColumnMapper(model, new TestSchemaProvider(map));

		ColumnReference columnReference = new TableQueryParser("foo").columnReference();
		// call under test
		assertEquals(Optional.of(new SchemaColumnTranslationReference(allColumns.get(0))),
				mapper.lookupColumnReference(columnReference));
	}
}
