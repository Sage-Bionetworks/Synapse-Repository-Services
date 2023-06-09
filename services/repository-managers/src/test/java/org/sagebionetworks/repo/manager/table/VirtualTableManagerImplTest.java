package org.sagebionetworks.repo.manager.table;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.VirtualTable;
import org.sagebionetworks.table.cluster.description.TableIndexDescription;
import org.sagebionetworks.table.query.ParseException;

@ExtendWith(MockitoExtension.class)
public class VirtualTableManagerImplTest {

	@InjectMocks
	private VirtualTableManagerImpl manager;

	@Mock
	private VirtualTable mockTable;
	@Mock
	private ColumnModelManager mockColumnModelManager;
	@Mock
	private TableManagerSupport mockTableManagerSupport;

	@Test
	public void testValidate() {
		String sql = "SELECT * FROM syn123";

		when(mockTable.getDefiningSQL()).thenReturn(sql);

		// Call under test
		manager.validate(mockTable);

		verify(mockTable).getDefiningSQL();
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

		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			manager.validate(mockTable);
		});

		assertTrue(ex.getCause() instanceof ParseException);

		assertTrue(ex.getMessage().startsWith("Encountered \" <regular_identifier> \"invalid"));
		verify(mockTable).getDefiningSQL();
	}

	@Test
	public void testValidateWithWithNoTable() {
		String sql = "SELECT foo";

		when(mockTable.getDefiningSQL()).thenReturn(sql);

		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			manager.validate(mockTable);
		});

		assertTrue(ex.getCause() instanceof ParseException);

		assertTrue(ex.getMessage().startsWith("Encountered \"<EOF>\" at line 1, column 10."));
		verify(mockTable).getDefiningSQL();
	}

	@Test
	public void testValidateWithWithUnsupportedJoin() {
		String sql = "SELECT * FROM table1 JOIN table2";

		when(mockTable.getDefiningSQL()).thenReturn(sql);

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			manager.validate(mockTable);
		}).getMessage();

		assertEquals("The JOIN keyword is not supported in this context", message);
		verify(mockTable).getDefiningSQL();
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
		List<ColumnModel> schema = List.of(cm);
		when(mockColumnModelManager.getTableSchema(any())).thenReturn(schema);
		when(mockColumnModelManager.createColumnModel(any())).thenReturn(cm);
		when(mockTableManagerSupport.getIndexDescription(any())).thenReturn(new TableIndexDescription(id));

		// call under test
		manager.registerDefiningSql(id, sql);

		verify(mockColumnModelManager).bindColumnsToVersionOfObject(List.of("99"), id);
		verify(mockColumnModelManager).getTableSchema(IdAndVersion.parse("syn456"));
		verify(mockColumnModelManager)
				.createColumnModel(new ColumnModel().setName("foo").setId(null).setColumnType(ColumnType.INTEGER));
		verify(mockTableManagerSupport).getIndexDescription(IdAndVersion.parse("syn456"));
	}
	
	@Test
	public void testRegisterDefiningSqlWithCast() {
		IdAndVersion id = IdAndVersion.parse("syn123");
		String sql = "select cast(foo as 88) from syn456";
		ColumnModel foo = new ColumnModel().setName("foo").setId("99").setColumnType(ColumnType.INTEGER);
		ColumnModel bar = new ColumnModel().setName("bar").setId("88").setColumnType(ColumnType.INTEGER);
		when(mockColumnModelManager.getTableSchema(any())).thenReturn(List.of(foo));
		when(mockColumnModelManager.getColumnModel(any())).thenReturn(bar);
		when(mockColumnModelManager.createColumnModel(any())).thenReturn(bar);
		when(mockTableManagerSupport.getIndexDescription(any())).thenReturn(new TableIndexDescription(id));

		// call under test
		manager.registerDefiningSql(id, sql);

		verify(mockColumnModelManager).bindColumnsToVersionOfObject(List.of("88"), id);
		verify(mockColumnModelManager).getTableSchema(IdAndVersion.parse("syn456"));
		verify(mockColumnModelManager, times(3)).getColumnModel("88");
		verify(mockColumnModelManager)
				.createColumnModel(new ColumnModel().setName("bar").setId(null).setColumnType(ColumnType.INTEGER));
		verify(mockTableManagerSupport).getIndexDescription(IdAndVersion.parse("syn456"));
	}
	
	@Test
	public void testRegisterDefiningSqlWithBadSql() {
		IdAndVersion id = IdAndVersion.parse("syn123");
		String sql = "select foo from syn456 a wrong";
		
		String message = assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			manager.registerDefiningSql(id, sql);
		}).getMessage();
		assertTrue(message.contains("Encountered \" <regular_identifier> \"wrong \""));

		verifyZeroInteractions(mockColumnModelManager, mockTableManagerSupport);
	}
	
	@Test
	public void testRegisterDefiningSqlWithCTE() {
		IdAndVersion id = IdAndVersion.parse("syn123");
		String sql = "with cte as (select foo from syn456) select * from cte";
		
		String message = assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			manager.registerDefiningSql(id, sql);
		}).getMessage();
		assertTrue(message.contains("Encountered \" \"WITH\""));

		verifyZeroInteractions(mockColumnModelManager, mockTableManagerSupport);
	}
	
	@Test
	public void testRegisterDefiningSqlWithJion() {
		IdAndVersion id = IdAndVersion.parse("syn123");
		String sql = "select * from syn1 join syn2 on (syn1.id = syn2.id)";
		
		String message = assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			manager.registerDefiningSql(id, sql);
		}).getMessage();
		assertEquals("The defining SQL can only reference one table/view", message);

		verifyZeroInteractions(mockColumnModelManager, mockTableManagerSupport);
	}
	
	@Test
	public void testRegisterDefiningSqlWithUnion() {
		IdAndVersion id = IdAndVersion.parse("syn123");
		String sql = "select * from syn1 union select * from syn2";
		
		String message = assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			manager.registerDefiningSql(id, sql);
		}).getMessage();
		assertTrue(message.contains("Encountered \" \"UNION\""));

		verifyZeroInteractions(mockColumnModelManager, mockTableManagerSupport);
	}
	
	@Test
	public void testRegisterDefiningSqlWithNullId() {
		IdAndVersion id = null;
		String sql = "select foo from syn456";
		
		String message = assertThrows(IllegalArgumentException.class, ()->{
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
		
		String message = assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			manager.registerDefiningSql(id, sql);
		}).getMessage();
		assertEquals("definingSQL is required.", message);

		verifyZeroInteractions(mockColumnModelManager, mockTableManagerSupport);
	}

}
