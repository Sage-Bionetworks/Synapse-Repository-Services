package org.sagebionetworks.table.cluster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.dbo.dao.table.TableModelTestUtils;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.table.query.ParseException;
import org.sagebionetworks.table.query.TableQueryParser;
import org.sagebionetworks.table.query.model.QuerySpecification;

public class TableAndColumnMapperTest {

	private List<ColumnModel> allColumns;

	@BeforeEach
	public void before() {
		allColumns = Arrays.asList(TableModelTestUtils.createColumn(111L, "foo", ColumnType.STRING),
				TableModelTestUtils.createColumn(222L, "has space", ColumnType.STRING),
				TableModelTestUtils.createColumn(333L, "bar", ColumnType.STRING),
				TableModelTestUtils.createColumn(444L, "foo_bar", ColumnType.STRING),
				TableModelTestUtils.createColumn(555L, "Foo", ColumnType.STRING),
				TableModelTestUtils.createColumn(666L, "datetype", ColumnType.DATE));
	}

	@Test
	public void testConstructorWithSingleTable() throws ParseException {
		QuerySpecification model = new TableQueryParser("select * from syn123").querySpecification();
		Map<IdAndVersion, List<ColumnModel>> map = new LinkedHashMap<>();
		map.put(IdAndVersion.parse("syn123"), allColumns);

		// call under test
		TableAndColumnMapper mapper = new TableAndColumnMapper(model, createSchemaProvider(map));
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
		TableAndColumnMapper mapper = new TableAndColumnMapper(model, createSchemaProvider(map));
		assertEquals(Arrays.asList(IdAndVersion.parse("syn123"), IdAndVersion.parse("syn456")), mapper.getTableIds());
		assertEquals(allColumns.subList(0, 6), mapper.getUnionOfAllTableSchemas());
	}
	
	@Test
	public void testConstructorWithNullModel() throws ParseException {
		QuerySpecification model = null;
		Map<IdAndVersion, List<ColumnModel>> map = new LinkedHashMap<>();
		map.put(IdAndVersion.parse("syn123"), allColumns);
		
		String message = assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			new TableAndColumnMapper(model, createSchemaProvider(map));
		}).getMessage();
		assertEquals("QuerySpecification is required.", message);
	}
	
	@Test
	public void testConstructorWithNullProvider() throws ParseException {
		QuerySpecification model = new TableQueryParser("select * from syn123").querySpecification();
		SchemaProvider provider = null;
		
		String message = assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			new TableAndColumnMapper(model, provider);
		}).getMessage();
		assertEquals("SchemaProvider is required.", message);
	}

	/**
	 * Helper to create a schema provider from the given map.
	 * 
	 * @param map
	 * @return
	 */
	public SchemaProvider createSchemaProvider(Map<IdAndVersion, List<ColumnModel>> map) {
		return (IdAndVersion i) -> {
			return map.get(i);
		};
	}
}
