package org.sagebionetworks.repo.manager.table;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.FacetType;
import org.sagebionetworks.repo.model.table.VirtualTable;
import org.sagebionetworks.table.cluster.QueryTranslator;
import org.sagebionetworks.table.cluster.description.IndexDescription;
import org.sagebionetworks.table.cluster.description.TableIndexDescription;
import org.sagebionetworks.table.query.ParseException;

@ExtendWith(MockitoExtension.class)
public class VirtualTableManagerImplTest {

	@Spy
	@InjectMocks
	private VirtualTableManagerImpl manager;

	@Mock
	private VirtualTable mockTable;
	@Mock
	private ColumnModelManager mockColumnModelManager;
	@Mock
	private TableManagerSupport mockTableManagerSupport;
	@Mock
	private IndexDescription mockIndexDescription;


	@Test
	public void testValidate() {
		String sql = "SELECT * FROM syn123";

		when(mockTable.getDefiningSQL()).thenReturn(sql);
		when(mockTable.getId()).thenReturn("syn456");
		when(mockTableManagerSupport.getIndexDescription(any())).thenReturn(mockIndexDescription);

		// Call under test
		manager.validate(mockTable);

		verify(mockTable).getDefiningSQL();
		verify(mockTableManagerSupport).getIndexDescription(IdAndVersion.parse("syn123"));
	}
	
	@Test
	public void testValidateWithNullId() {
		String sql = "SELECT * FROM syn123";

		when(mockTable.getDefiningSQL()).thenReturn(sql);
		// a new VT will not have an ID
		when(mockTable.getId()).thenReturn(null);
		when(mockTableManagerSupport.getIndexDescription(any())).thenReturn(mockIndexDescription);

		// Call under test
		manager.validate(mockTable);

		verify(mockTable).getDefiningSQL();
		verify(mockTableManagerSupport).getIndexDescription(IdAndVersion.parse("syn123"));
	}

	@Test
	public void testValidateWithNullSQL() {
		String sql = null;

		when(mockTable.getDefiningSQL()).thenReturn(sql);

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			manager.validate(mockTable);
		}).getMessage();

		assertEquals("The definingSQL of the virtual table is required and must not be the empty string.", message);
		verify(mockTable).getDefiningSQL();
	}

	@Test
	public void testValidateWithEmptySQL() {
		String sql = "";

		when(mockTable.getDefiningSQL()).thenReturn(sql);

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			manager.validate(mockTable);
		}).getMessage();

		assertEquals("The definingSQL of the virtual table is required and must not be the empty string.", message);
		verify(mockTable).getDefiningSQL();
	}

	@Test
	public void testValidateWithBlankSQL() {
		String sql = "   ";

		when(mockTable.getDefiningSQL()).thenReturn(sql);

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			manager.validate(mockTable);
		}).getMessage();

		assertEquals("The definingSQL of the virtual table is required and must not be a blank string.", message);
		verify(mockTable).getDefiningSQL();
	}

	@Test
	public void testValidateWithInvalidSQL() {
		String sql = "invalid SQL";

		when(mockTable.getDefiningSQL()).thenReturn(sql);
		when(mockTable.getId()).thenReturn("syn456");

		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			manager.validate(mockTable);
		});

		assertTrue(ex.getCause() instanceof ParseException);
		verify(mockTable).getDefiningSQL();
	}

	@Test
	public void testValidateWithWithNoTable() {
		String sql = "SELECT foo";

		when(mockTable.getDefiningSQL()).thenReturn(sql);
		when(mockTable.getId()).thenReturn("syn456");

		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			manager.validate(mockTable);
		});

		assertTrue(ex.getCause() instanceof ParseException);
		verify(mockTable).getDefiningSQL();
	}

	@Test
	public void testValidateWithWithUnsupportedJoin() {
		String sql = "SELECT * FROM syn1 JOIN syn2";

		when(mockTable.getDefiningSQL()).thenReturn(sql);
		when(mockTable.getId()).thenReturn("syn456");

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			manager.validate(mockTable);
		}).getMessage();

		assertEquals("The JOIN keyword is not supported in this context", message);
		verify(mockTable).getDefiningSQL();
	}

	@Test
	public void testValidateDefiningSql() {
		String sql = "select * from syn123";

		doReturn(null).when(manager).validateSqlAndGetSchema(sql);

		// Call under test
		manager.validateDefiningSql(sql);
		verify(manager).validateSqlAndGetSchema(sql);
	}

	@Test
	public void testValidateDefiningSqlWithNullSql() {
		String sql = null;

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			manager.validateDefiningSql(sql);
		}).getMessage();

		assertEquals("The definingSQL of the virtual table is required.", message);
	}

	@Test
	public void testValidateSqlAndGetSchema() {
		String sql = "select * from syn456";
		ColumnModel cm = new ColumnModel().setName("foo").setId("99").setColumnType(ColumnType.INTEGER);
		IdAndVersion id = IdAndVersion.parse("syn123");
		List<ColumnModel> expected = List.of(new ColumnModel().setName("foo").setColumnType(ColumnType.INTEGER));

		when(mockTableManagerSupport.getTableSchema(any())).thenReturn(List.of(cm));
		when(mockTableManagerSupport.getIndexDescription(any())).thenReturn(new TableIndexDescription(id));
		when(mockTableManagerSupport.getColumnModel(any())).thenReturn(cm);

		// call under test
		List<ColumnModel> schema = manager.validateSqlAndGetSchema(sql);

		verify(mockTableManagerSupport).getTableSchema(IdAndVersion.parse("syn456"));
		verify(mockTableManagerSupport).getIndexDescription(IdAndVersion.parse("syn456"));

		assertEquals(expected, schema);
	}

	@Test
	public void testValidateSqlAndGetSchemaWithCast() {
		String sql = "select cast(foo as 88) from syn456";
		ColumnModel foo = new ColumnModel().setName("foo").setId("99").setColumnType(ColumnType.INTEGER);
		ColumnModel bar = new ColumnModel().setName("bar").setId("88").setColumnType(ColumnType.INTEGER);
		IdAndVersion id = IdAndVersion.parse("syn123");
		List<ColumnModel> expected = List.of(new ColumnModel().setName("bar").setColumnType(ColumnType.INTEGER));

		when(mockTableManagerSupport.getTableSchema(any())).thenReturn(List.of(foo));
		when(mockTableManagerSupport.getColumnModel(any())).thenReturn(bar);
		when(mockTableManagerSupport.getIndexDescription(any())).thenReturn(new TableIndexDescription(id));

		// call under test
		List<ColumnModel> schema = manager.validateSqlAndGetSchema(sql);

		verify(mockTableManagerSupport).getTableSchema(IdAndVersion.parse("syn456"));
		verify(mockTableManagerSupport, times(4)).getColumnModel("88");
		verify(mockTableManagerSupport).getIndexDescription(IdAndVersion.parse("syn456"));

		assertEquals(expected, schema);
	}

	@Test
	public void testValidateSqlAndGetSchemaCastWithFacet() {
		String sql = "select cast(foo as 88) from syn456";
		ColumnModel foo = new ColumnModel().setName("foo").setId("99").setColumnType(ColumnType.INTEGER);
		ColumnModel bar = new ColumnModel().setName("bar").setId("88").setColumnType(ColumnType.INTEGER).setFacetType(FacetType.range);
		IdAndVersion id = IdAndVersion.parse("syn123");
		List<ColumnModel> expected = List.of(new ColumnModel().setName("bar").setColumnType(ColumnType.INTEGER).setFacetType(FacetType.range));

		when(mockTableManagerSupport.getTableSchema(any())).thenReturn(List.of(foo));
		when(mockTableManagerSupport.getColumnModel(any())).thenReturn(bar);
		when(mockTableManagerSupport.getIndexDescription(any())).thenReturn(new TableIndexDescription(id));

		// call under test
		List<ColumnModel> schema = manager.validateSqlAndGetSchema(sql);

		verify(mockTableManagerSupport).getTableSchema(IdAndVersion.parse("syn456"));
		verify(mockTableManagerSupport, times(4)).getColumnModel("88");
		verify(mockTableManagerSupport).getIndexDescription(IdAndVersion.parse("syn456"));

		assertEquals(expected, schema);
	}

	@Test
	public void testValidateSqlAndGetSchemaWithNullSql() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.validateSqlAndGetSchema(null);
		}).getMessage();

		assertEquals("The definingSQL of the virtual table is required.", message);
	}

	@Test
	public void testValidateSqlAndGetSchemaWithBadSql() {
		String sql = "select foo from syn456 a wrong";

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.validateSqlAndGetSchema(sql);
		}).getMessage();

		assertTrue(message.contains("Encountered \" <regular_identifier> \"wrong \""));
		verifyZeroInteractions(mockColumnModelManager, mockTableManagerSupport);
	}

	@Test
	public void testValidateSqlAndGetSchemaWithCTE() {
		String sql = "with cte as (select foo from syn456) select * from cte";

		String message = assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			manager.validateSqlAndGetSchema(sql);
		}).getMessage();

		assertTrue(message.contains("Encountered \" \"WITH\""));
		verifyZeroInteractions(mockColumnModelManager, mockTableManagerSupport);
	}

	@Test
	public void testValidateSqlAndGetSchemaWithJoin() {
		String sql = "select * from syn1 join syn2 on (syn1.id = syn2.id)";

		String message = assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			manager.validateSqlAndGetSchema(sql);
		}).getMessage();

		assertEquals("The defining SQL can only reference one table/view", message);
		verifyZeroInteractions(mockColumnModelManager, mockTableManagerSupport);
	}

	@Test
	public void testValidateSqlAndGetSchemaWithUnion() {
		String sql = "select * from syn1 union select *from syn2";

		String message = assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			manager.validateSqlAndGetSchema(sql);
		}).getMessage();

		assertTrue(message.contains("Encountered \" \"UNION\""));
		verifyZeroInteractions(mockColumnModelManager, mockTableManagerSupport);
	}

	@Test
	public void testGetSchemaIds() {
		List<String> columnIds = List.of("1", "2");
		when(mockColumnModelManager.getColumnIdsForTable(any())).thenReturn(columnIds);
		// call under test
		List<String> result = manager.getSchemaIds(IdAndVersion.parse("syn123"));
		assertEquals(columnIds, result);
		verify(mockColumnModelManager).getColumnIdsForTable(IdAndVersion.parse("syn123"));
	}

	@Test
	public void testRegisterDefiningSql() {
		IdAndVersion id = IdAndVersion.parse("syn123");
		String sql = "select * from syn456";
		ColumnModel cm = new ColumnModel().setName("foo").setId("99").setColumnType(ColumnType.INTEGER);

		when(mockTableManagerSupport.getTableSchema(any())).thenReturn(List.of(cm));
		when(mockColumnModelManager.createColumnModel(any())).thenReturn(cm);
		when(mockTableManagerSupport.getIndexDescription(any())).thenReturn(new TableIndexDescription(id));
		when(mockTableManagerSupport.getColumnModel(any())).thenReturn(cm);

		// call under test
		manager.registerDefiningSql(id, sql);

		verify(mockColumnModelManager).bindColumnsToVersionOfObject(List.of("99"), id);
		verify(mockColumnModelManager)
				.createColumnModel(new ColumnModel().setName("foo").setId(null).setColumnType(ColumnType.INTEGER));
	}
	
	@Test
	public void testRegisterDefiningSqlWithNullId() {
		IdAndVersion id = null;
		String sql = "select foo from syn456";
		
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.registerDefiningSql(id, sql);
		}).getMessage();

		assertEquals("table Id is required.", message);
		verifyZeroInteractions(mockColumnModelManager, mockTableManagerSupport);
	}

	@Test
	public void testRegisterDefiningSqlWithNullSql() {
		IdAndVersion id = IdAndVersion.parse("syn123");
		String sql = null;

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.registerDefiningSql(id, sql);
		}).getMessage();

		assertEquals("definingSQL is required.", message);
		verifyZeroInteractions(mockColumnModelManager, mockTableManagerSupport);
	}

}
