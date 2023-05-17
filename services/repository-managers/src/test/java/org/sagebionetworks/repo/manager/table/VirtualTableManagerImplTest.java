package org.sagebionetworks.repo.manager.table;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.model.table.VirtualTable;
import org.sagebionetworks.table.query.ParseException;

@ExtendWith(MockitoExtension.class)
public class VirtualTableManagerImplTest {
	
	@InjectMocks
	private VirtualTableManagerImpl manager;

	@Mock
	private VirtualTable mockTable;
	
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

}
