package org.sagebionetworks.repo.manager.table;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.model.dbo.dao.table.MaterializedViewDao;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.table.MaterializedView;
import org.sagebionetworks.table.query.ParseException;

import com.google.common.collect.ImmutableSet;

@ExtendWith(MockitoExtension.class)
public class MaterializedViewManagerImplTest {
	
	@Mock
	private MaterializedViewDao mockDao;
		
	@InjectMocks
	private MaterializedViewManagerImpl manager;
	
	@Mock
	private MaterializedView mockView;
	
	private String viewId = "syn123";
	private Long viewVersion = 1L;	
	private IdAndVersion idAndVersion = KeyFactory.idAndVersion(viewId, viewVersion);

	@Test
	public void testValidate() {
		String sql = "SELECT * FROM syn123";
		
		when(mockView.getDefiningSQL()).thenReturn(sql);
		
		// Call under test
		manager.validate(mockView);
		
		verify(mockView, atLeastOnce()).getDefiningSQL();
	}
	
	@Test
	public void testValidateWithNullSQL() {
		String sql = null;
		
		when(mockView.getDefiningSQL()).thenReturn(sql);
		
		String message = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			manager.validate(mockView);
		}).getMessage();
		
		assertEquals("The materialized view definingSQL is required and must not be the empty string.", message);
		verify(mockView, atLeastOnce()).getDefiningSQL();
	}
	
	@Test
	public void testValidateWithEmptySQL() {
		String sql = "";
		
		when(mockView.getDefiningSQL()).thenReturn(sql);
		
		String message = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			manager.validate(mockView);
		}).getMessage();
		
		assertEquals("The materialized view definingSQL is required and must not be the empty string.", message);
		verify(mockView, atLeastOnce()).getDefiningSQL();
	}
	
	@Test
	public void testValidateWithBlankSQL() {
		String sql = "   ";
		
		when(mockView.getDefiningSQL()).thenReturn(sql);
		
		String message = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			manager.validate(mockView);
		}).getMessage();
		
		assertEquals("The materialized view definingSQL is required and must not be a blank string.", message);
		verify(mockView, atLeastOnce()).getDefiningSQL();
	}
	
	@Test
	public void testValidateWithInvalidSQL() {
		String sql = "invalid SQL";
		
		when(mockView.getDefiningSQL()).thenReturn(sql);
		
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			manager.validate(mockView);
		});
		
		assertTrue(ex.getCause() instanceof ParseException);
		
		assertTrue(ex.getMessage().startsWith("Encountered \" <regular_identifier> \"invalid"));
		verify(mockView, atLeastOnce()).getDefiningSQL();
	}
	
	@Test
	public void testValidateWithWithNoTable() {
		String sql = "SELECT foo";
		
		when(mockView.getDefiningSQL()).thenReturn(sql);
		
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			manager.validate(mockView);
		});
		
		assertTrue(ex.getCause() instanceof ParseException);
		
		assertTrue(ex.getMessage().startsWith("Encountered \"<EOF>\" at line 1, column 10."));
		verify(mockView, atLeastOnce()).getDefiningSQL();
	}
	
	@Test
	public void testValidateWithWithUnsupportedJoin() {
		String sql = "SELECT * FROM table1 JOIN table2";
		
		when(mockView.getDefiningSQL()).thenReturn(sql);
		
		String message = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			manager.validate(mockView);
		}).getMessage();
		
		assertEquals("The JOIN keyword is not supported in this context", message);
		verify(mockView, atLeastOnce()).getDefiningSQL();
	}
	
	@Test
	public void testRegisterSourceTables() {
		
		Set<IdAndVersion> currentSourceTables = Collections.emptySet();
		String sql = "SELECT * FROM syn123";
		
		Set<IdAndVersion> expectedDeletes = Collections.emptySet();
		Set<IdAndVersion> expectedSources = ImmutableSet.of(
			IdAndVersion.parse("syn123")
		);
		
		when(mockView.getId()).thenReturn(viewId);
		when(mockView.getVersionNumber()).thenReturn(viewVersion);
		when(mockView.getDefiningSQL()).thenReturn(sql);
		
		when(mockDao.getSourceTables(any())).thenReturn(currentSourceTables);
		
		
	
		// Call under test
		manager.registerSourceTables(mockView);
		
		verify(mockDao).getSourceTables(idAndVersion);
		verify(mockDao).deleteSourceTables(idAndVersion, expectedDeletes);
		verify(mockDao).addSourceTables(idAndVersion, expectedSources);
		
	}
	
	@Test
	public void testRegisterSourceTablesWithNonOverlappingAssociations() {
		
		Set<IdAndVersion> currentSourceTables = ImmutableSet.of(
			IdAndVersion.parse("syn456")
		);
		
		String sql = "SELECT * FROM syn123";
		
		Set<IdAndVersion> expectedDeletes = currentSourceTables;
		Set<IdAndVersion> expectedSources = ImmutableSet.of(
			IdAndVersion.parse("syn123")
		);
		
		when(mockView.getId()).thenReturn(viewId);
		when(mockView.getVersionNumber()).thenReturn(viewVersion);
		when(mockView.getDefiningSQL()).thenReturn(sql);
		
		when(mockDao.getSourceTables(any())).thenReturn(currentSourceTables);
		
		
	
		// Call under test
		manager.registerSourceTables(mockView);
		
		verify(mockDao).getSourceTables(idAndVersion);
		verify(mockDao).deleteSourceTables(idAndVersion, expectedDeletes);
		verify(mockDao).addSourceTables(idAndVersion, expectedSources);
		
	}
	
	@Test
	public void testRegisterSourceTablesWithOverlappingAssociations() {
		
		Set<IdAndVersion> currentSourceTables = ImmutableSet.of(
			IdAndVersion.parse("syn123"), IdAndVersion.parse("syn456")
		);
		
		String sql = "SELECT * FROM syn123";
		
		Set<IdAndVersion> expectedDeletes = ImmutableSet.of(
			IdAndVersion.parse("syn456")
		);
		Set<IdAndVersion> expectedSources = ImmutableSet.of(
			IdAndVersion.parse("syn123")
		);
		
		when(mockView.getId()).thenReturn(viewId);
		when(mockView.getVersionNumber()).thenReturn(viewVersion);
		when(mockView.getDefiningSQL()).thenReturn(sql);
		
		when(mockDao.getSourceTables(any())).thenReturn(currentSourceTables);
		
		
	
		// Call under test
		manager.registerSourceTables(mockView);
		
		verify(mockDao).getSourceTables(idAndVersion);
		verify(mockDao).deleteSourceTables(idAndVersion, expectedDeletes);
		verify(mockDao).addSourceTables(idAndVersion, expectedSources);
		
	}
	
	@Test
	public void testRegisterSourceTablesWithNoChanges() {
		
		Set<IdAndVersion> currentSourceTables = ImmutableSet.of(
			IdAndVersion.parse("syn123")
		);
		
		String sql = "SELECT * FROM syn123";
		
		when(mockView.getId()).thenReturn(viewId);
		when(mockView.getVersionNumber()).thenReturn(viewVersion);
		when(mockView.getDefiningSQL()).thenReturn(sql);
		
		when(mockDao.getSourceTables(any())).thenReturn(currentSourceTables);
		
		
	
		// Call under test
		manager.registerSourceTables(mockView);
		
		verify(mockDao).getSourceTables(idAndVersion);
		verifyNoMoreInteractions(mockDao);
		
	}
	
	@Test
	public void testRegisterSourceTablesWithMultipleTables() {
		
		Set<IdAndVersion> currentSourceTables = Collections.emptySet();
		String sql = "SELECT * FROM syn123 JOIN syn456";
		
		Set<IdAndVersion> expectedDeletes = Collections.emptySet();
		Set<IdAndVersion> expectedSources = ImmutableSet.of(
			IdAndVersion.parse("syn123"), IdAndVersion.parse("syn456")
		);
		
		when(mockView.getId()).thenReturn(viewId);
		when(mockView.getVersionNumber()).thenReturn(viewVersion);
		when(mockView.getDefiningSQL()).thenReturn(sql);
		
		when(mockDao.getSourceTables(any())).thenReturn(currentSourceTables);
		
		
	
		// Call under test
		manager.registerSourceTables(mockView);
		
		verify(mockDao).getSourceTables(idAndVersion);
		verify(mockDao).deleteSourceTables(idAndVersion, expectedDeletes);
		verify(mockDao).addSourceTables(idAndVersion, expectedSources);
		
	}
	
	@Test
	public void testRegisterSourceTablesWithMultipleTablesWithNonOverlappingAssociations() {
		
		Set<IdAndVersion> currentSourceTables = ImmutableSet.of(
			IdAndVersion.parse("syn789"), IdAndVersion.parse("syn101112")
		);
		String sql = "SELECT * FROM syn123 JOIN syn456";
		
		Set<IdAndVersion> expectedDeletes = currentSourceTables;
		Set<IdAndVersion> expectedSources = ImmutableSet.of(
			IdAndVersion.parse("syn123"), IdAndVersion.parse("syn456")
		);
		
		when(mockView.getId()).thenReturn(viewId);
		when(mockView.getVersionNumber()).thenReturn(viewVersion);
		when(mockView.getDefiningSQL()).thenReturn(sql);
		
		when(mockDao.getSourceTables(any())).thenReturn(currentSourceTables);
		
		
	
		// Call under test
		manager.registerSourceTables(mockView);
		
		verify(mockDao).getSourceTables(idAndVersion);
		verify(mockDao).deleteSourceTables(idAndVersion, expectedDeletes);
		verify(mockDao).addSourceTables(idAndVersion, expectedSources);
		
	}
	
	@Test
	public void testRegisterSourceTablesWithMultipleTablesWithOverlappingAssociations() {
		
		Set<IdAndVersion> currentSourceTables = ImmutableSet.of(
			IdAndVersion.parse("syn456"), IdAndVersion.parse("syn789"), IdAndVersion.parse("syn101112")
		);
		String sql = "SELECT * FROM syn123 JOIN syn456";
		
		Set<IdAndVersion> expectedDeletes = ImmutableSet.of(
			IdAndVersion.parse("syn789"), IdAndVersion.parse("syn101112")
		);
		Set<IdAndVersion> expectedSources = ImmutableSet.of(
			IdAndVersion.parse("syn123"), IdAndVersion.parse("syn456")
		);
		
		when(mockView.getId()).thenReturn(viewId);
		when(mockView.getVersionNumber()).thenReturn(viewVersion);
		when(mockView.getDefiningSQL()).thenReturn(sql);
		
		when(mockDao.getSourceTables(any())).thenReturn(currentSourceTables);
		
		
	
		// Call under test
		manager.registerSourceTables(mockView);
		
		verify(mockDao).getSourceTables(idAndVersion);
		verify(mockDao).deleteSourceTables(idAndVersion, expectedDeletes);
		verify(mockDao).addSourceTables(idAndVersion, expectedSources);
		
	}
	
	@Test
	public void testRegisterSourceTablesWithMultipleTablesAndNoChanges() {
		
		Set<IdAndVersion> currentSourceTables = ImmutableSet.of(
			IdAndVersion.parse("syn456"), IdAndVersion.parse("syn123")
		);
		
		String sql = "SELECT * FROM syn123 JOIN syn456";
		
		when(mockView.getId()).thenReturn(viewId);
		when(mockView.getVersionNumber()).thenReturn(viewVersion);
		when(mockView.getDefiningSQL()).thenReturn(sql);
		
		when(mockDao.getSourceTables(any())).thenReturn(currentSourceTables);
		
		
	
		// Call under test
		manager.registerSourceTables(mockView);
		
		verify(mockDao).getSourceTables(idAndVersion);
		verifyNoMoreInteractions(mockDao);
		
	}
	
	@Test
	public void testRegisterSourceTablesWithVersions() {
		
		Set<IdAndVersion> currentSourceTables = ImmutableSet.of(
			IdAndVersion.parse("syn456"), IdAndVersion.parse("syn123.2")
		);
		
		String sql = "SELECT * FROM syn123.3 JOIN syn456";
		
		Set<IdAndVersion> expectedDeletes = ImmutableSet.of(
				IdAndVersion.parse("syn123.2")
		);
		
		Set<IdAndVersion> expectedSources = ImmutableSet.of(
			IdAndVersion.parse("syn123.3"), IdAndVersion.parse("syn456")
		);
		
		when(mockView.getId()).thenReturn(viewId);
		when(mockView.getVersionNumber()).thenReturn(viewVersion);
		when(mockView.getDefiningSQL()).thenReturn(sql);
		
		when(mockDao.getSourceTables(any())).thenReturn(currentSourceTables);
	
		// Call under test
		manager.registerSourceTables(mockView);
		
		verify(mockDao).getSourceTables(idAndVersion);
		verify(mockDao).deleteSourceTables(idAndVersion, expectedDeletes);
		verify(mockDao).addSourceTables(idAndVersion, expectedSources);
		
	}

}
