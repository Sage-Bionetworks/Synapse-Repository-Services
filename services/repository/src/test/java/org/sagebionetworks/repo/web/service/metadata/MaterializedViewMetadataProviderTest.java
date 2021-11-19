package org.sagebionetworks.repo.web.service.metadata;

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
import org.sagebionetworks.repo.model.table.MaterializedView;
import org.sagebionetworks.table.query.ParseException;

@ExtendWith(MockitoExtension.class)
public class MaterializedViewMetadataProviderTest {

	@InjectMocks
	private MaterializedViewMetadataProvider provider;

	@Mock
	private MaterializedView mockView;
	
	@Mock
	private EntityEvent mockEvent;
	
	@Test
	public void testValidateEntity() {
		String sql = "SELECT * FROM syn123";
		
		when(mockView.getDefiningSQL()).thenReturn(sql);
		
		// Call under test
		provider.validateEntity(mockView, mockEvent);
		
		verify(mockView).getDefiningSQL();
	}
	
	@Test
	public void testValidateEntityWithNullSQL() {
		String sql = null;
		
		when(mockView.getDefiningSQL()).thenReturn(sql);
		
		String message = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			provider.validateEntity(mockView, mockEvent);
		}).getMessage();
		
		assertEquals("The materialized view definingSQL is required and must not be the empty string.", message);
		verify(mockView).getDefiningSQL();
	}
	
	@Test
	public void testValidateEntityWithEmptySQL() {
		String sql = "";
		
		when(mockView.getDefiningSQL()).thenReturn(sql);
		
		String message = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			provider.validateEntity(mockView, mockEvent);
		}).getMessage();
		
		assertEquals("The materialized view definingSQL is required and must not be the empty string.", message);
		verify(mockView).getDefiningSQL();
	}
	
	@Test
	public void testValidateEntityWithBlankSQL() {
		String sql = "   ";
		
		when(mockView.getDefiningSQL()).thenReturn(sql);
		
		String message = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			provider.validateEntity(mockView, mockEvent);
		}).getMessage();
		
		assertEquals("The materialized view definingSQL is required and must not be a blank string.", message);
		verify(mockView).getDefiningSQL();
	}
	
	@Test
	public void testValidateEntityWithInvalidSQL() {
		String sql = "invalid SQL";
		
		when(mockView.getDefiningSQL()).thenReturn(sql);
		
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			provider.validateEntity(mockView, mockEvent);
		});
		
		assertTrue(ex.getCause() instanceof ParseException);
		
		assertTrue(ex.getMessage().startsWith("Encountered \" <regular_identifier> \"invalid"));
		verify(mockView).getDefiningSQL();
	}
	
	@Test
	public void testValidateEntityWithWithNoTable() {
		String sql = "SELECT foo";
		
		when(mockView.getDefiningSQL()).thenReturn(sql);
		
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			provider.validateEntity(mockView, mockEvent);
		});
		
		assertTrue(ex.getCause() instanceof ParseException);
		
		assertTrue(ex.getMessage().startsWith("Encountered \"<EOF>\" at line 1, column 10."));
		verify(mockView).getDefiningSQL();
	}
	
	@Test
	public void testValidateEntityWithWithUnsupportedJoin() {
		String sql = "SELECT * FROM table1 JOIN table2";
		
		when(mockView.getDefiningSQL()).thenReturn(sql);
		
		String message = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			provider.validateEntity(mockView, mockEvent);
		}).getMessage();
		
		assertEquals("The JOIN keyword is not supported in this context", message);
		verify(mockView).getDefiningSQL();
	}

}
