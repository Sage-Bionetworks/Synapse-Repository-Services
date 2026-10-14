package org.sagebionetworks.repo.manager.grid.synch.schema;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.grid.synch.handler.SourceHandler;

@ExtendWith(MockitoExtension.class)
public class SchemaSyncRulesTest {

	@Mock
	private SourceHandler mockHandler;

	@InjectMocks
	private SchemaSyncRules rules;

	@Test
	public void testGetKey() {
		assertEquals("one", rules.getKey(new ColumnCopyItem().setColumnName("one")));
		verifyNoMoreInteractions(mockHandler);
	}

	@Test
	public void testMatches() {
		// call under test
		assertTrue(rules.matches(new ColumnCopyItem().setColumnName("a"), new ColumnSourceItem().setColumnName("a")));
		assertFalse(rules.matches(new ColumnCopyItem().setColumnName("a"), new ColumnSourceItem().setColumnName("b")));
	}

	@Test
	public void testIsExcludedFromMatching() {
		when(mockHandler.isColumnExcludedFromMatching("gridOnly")).thenReturn(true);

		// call under test — delegates the grid column name to the handler; the
		// precomputed key is ignored (schema exclusion has no dedup concern)
		assertTrue(rules.isExcludedFromMatching(new ColumnCopyItem().setColumnName("gridOnly"), "gridOnly"));

		verify(mockHandler).isColumnExcludedFromMatching("gridOnly");
		verifyNoMoreInteractions(mockHandler);
	}

	@Test
	public void testWasDeletedByUser() {
		when(mockHandler.isColumnDeletedByUser("d")).thenReturn(true);

		// call under test — delegates the source column name to the handler
		assertTrue(rules.wasDeletedByUser(new ColumnSourceItem().setColumnName("d")));

		verify(mockHandler).isColumnDeletedByUser("d");
		verifyNoMoreInteractions(mockHandler);
	}
}
