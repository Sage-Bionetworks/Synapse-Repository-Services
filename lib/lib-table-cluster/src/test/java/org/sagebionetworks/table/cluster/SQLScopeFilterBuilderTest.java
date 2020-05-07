package org.sagebionetworks.table.cluster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.table.TableConstants;
import org.sagebionetworks.repo.model.table.ViewScopeFilter;

import com.google.common.collect.ImmutableList;

@ExtendWith(MockitoExtension.class)
public class SQLScopeFilterBuilderTest {
	
	@Mock
	private ViewScopeFilter mockViewScopeFilter;
	
	@Test
	public void testBuildWithNullFilter() {
		mockViewScopeFilter = null;
		
		SQLScopeFilterBuilder builder = new SQLScopeFilterBuilder(mockViewScopeFilter);
		
		String errorMessage = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			builder.build();
		}).getMessage();
		
		assertEquals("filter is required.", errorMessage);
	}
	
	@Test
	public void testBuildWithNullSubTypes() {
		List<Enum<?>> subTypes = null;
		
		when(mockViewScopeFilter.getSubTypes()).thenReturn(subTypes);
		
		SQLScopeFilterBuilder builder = new SQLScopeFilterBuilder(mockViewScopeFilter);
		
		String errorMessage = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			builder.build();
		}).getMessage();
		
		assertEquals("filter.subTypes is required.", errorMessage);
	}
	
	@Test
	public void testBuildWithFileFilter(){
		boolean filterByObjectId = false;
		List<Enum<?>> subTypes = ImmutableList.of(EntityType.file);
	
		when(mockViewScopeFilter.isFilterByObjectId()).thenReturn(filterByObjectId);
		when(mockViewScopeFilter.getSubTypes()).thenReturn(subTypes);
		
		SQLScopeFilterBuilder builder = new SQLScopeFilterBuilder(mockViewScopeFilter);
		
		SQLScopeFilter scopeFilter = builder.build();
		
		assertEquals(TableConstants.OBJECT_REPLICATION_COL_PARENT_ID, scopeFilter.getViewScopeFilterColumn());
		assertEquals("R.SUBTYPE IN ('file')", scopeFilter.getViewTypeFilter());
	}

	@Test
	public void testBuildWithObjectIdFilter(){
		boolean filterByObjectId = true;
		List<Enum<?>> subTypes = ImmutableList.of(EntityType.project);
	
		when(mockViewScopeFilter.isFilterByObjectId()).thenReturn(filterByObjectId);
		when(mockViewScopeFilter.getSubTypes()).thenReturn(subTypes);
		
		SQLScopeFilterBuilder builder = new SQLScopeFilterBuilder(mockViewScopeFilter);
		
		SQLScopeFilter scopeFilter = builder.build();
		
		assertEquals(TableConstants.OBJECT_REPLICATION_COL_OBJECT_ID, scopeFilter.getViewScopeFilterColumn());
		assertEquals("R.SUBTYPE IN ('project')", scopeFilter.getViewTypeFilter());
	}
	
	@Test
	public void testBuildWithMultipleTypes(){
		boolean filterByObjectId = false;
		List<Enum<?>> subTypes = ImmutableList.of(EntityType.file, EntityType.table);
	
		when(mockViewScopeFilter.isFilterByObjectId()).thenReturn(filterByObjectId);
		when(mockViewScopeFilter.getSubTypes()).thenReturn(subTypes);
		
		SQLScopeFilterBuilder builder = new SQLScopeFilterBuilder(mockViewScopeFilter);
		
		SQLScopeFilter scopeFilter = builder.build();
		
		assertEquals(TableConstants.OBJECT_REPLICATION_COL_PARENT_ID, scopeFilter.getViewScopeFilterColumn());
		assertEquals("R.SUBTYPE IN ('file','table')", scopeFilter.getViewTypeFilter());
	}
	
}
