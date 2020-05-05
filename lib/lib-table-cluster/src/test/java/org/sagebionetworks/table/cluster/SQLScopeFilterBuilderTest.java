package org.sagebionetworks.table.cluster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.table.TableConstants;
import org.sagebionetworks.repo.model.table.ViewType;
import org.sagebionetworks.repo.model.table.ViewTypeMask;
import org.sagebionetworks.table.cluster.metadata.ScopeFilterProvider;

import com.google.common.collect.ImmutableList;

@ExtendWith(MockitoExtension.class)
public class SQLScopeFilterBuilderTest {
	
	@Mock
	private ScopeFilterProvider scopeFilterProvider;
	
	@Test
	public void testBuildWithNullProvider() {
		scopeFilterProvider = null;
		Long viewTypeMask = ViewTypeMask.File.getMask();
		
		SQLScopeFilterBuilder builder = new SQLScopeFilterBuilder(scopeFilterProvider, viewTypeMask);
		
		String errorMessage = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			builder.build();
		}).getMessage();
		
		assertEquals("scopeFilterProvider is required.", errorMessage);
	}
	
	@Test
	public void testBuildWithNullMask() {
		Long viewTypeMask = null;
		
		SQLScopeFilterBuilder builder = new SQLScopeFilterBuilder(scopeFilterProvider, viewTypeMask);
		
		String errorMessage = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			builder.build();
		}).getMessage();
		
		assertEquals("viewTypeMask is required.", errorMessage);
	}

	@Test
	public void testBuildWithFileFilter(){
		boolean filterByObjectId = false;
		Long viewTypeMask = ViewTypeMask.File.getMask();
	
		when(scopeFilterProvider.isFilterScopeByObjectId(viewTypeMask)).thenReturn(filterByObjectId);
		when(scopeFilterProvider.getSubTypesForMask(viewTypeMask)).thenReturn(ImmutableList.of(EntityType.file));
		
		SQLScopeFilterBuilder builder = new SQLScopeFilterBuilder(scopeFilterProvider, viewTypeMask);
		
		SQLScopeFilter scopeFilter = builder.build();
		
		assertEquals(TableConstants.OBJECT_REPLICATION_COL_PARENT_ID, scopeFilter.getViewScopeFilterColumn());
		assertEquals("R.SUBTYPE IN ('file')", scopeFilter.getViewTypeFilter());
	}

	@Test
	public void testBuildWithObjectIdFilter(){
		boolean filterByObjectId = true;
		Long viewTypeMask = ViewTypeMask.File.getMask();
	
		when(scopeFilterProvider.isFilterScopeByObjectId(viewTypeMask)).thenReturn(filterByObjectId);
		when(scopeFilterProvider.getSubTypesForMask(viewTypeMask)).thenReturn(ImmutableList.of(EntityType.project));
		
		SQLScopeFilterBuilder builder = new SQLScopeFilterBuilder(scopeFilterProvider, viewTypeMask);
		
		SQLScopeFilter scopeFilter = builder.build();
		
		assertEquals(TableConstants.OBJECT_REPLICATION_COL_OBJECT_ID, scopeFilter.getViewScopeFilterColumn());
		assertEquals("R.SUBTYPE IN ('project')", scopeFilter.getViewTypeFilter());
	}
	
	@Test
	public void testBuildWithMultipleTypes(){
		boolean filterByObjectId = false;
		Long viewTypeMask = ViewTypeMask.getMaskForDepricatedType(ViewType.file_and_table);
	
		when(scopeFilterProvider.isFilterScopeByObjectId(viewTypeMask)).thenReturn(filterByObjectId);
		when(scopeFilterProvider.getSubTypesForMask(viewTypeMask)).thenReturn(ImmutableList.of(EntityType.file, EntityType.table));
		
		SQLScopeFilterBuilder builder = new SQLScopeFilterBuilder(scopeFilterProvider, viewTypeMask);
		
		SQLScopeFilter scopeFilter = builder.build();
		
		assertEquals(TableConstants.OBJECT_REPLICATION_COL_PARENT_ID, scopeFilter.getViewScopeFilterColumn());
		assertEquals("R.SUBTYPE IN ('file','table')", scopeFilter.getViewTypeFilter());
	}
	
}
