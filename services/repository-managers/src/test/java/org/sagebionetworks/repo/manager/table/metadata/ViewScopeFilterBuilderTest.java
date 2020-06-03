package org.sagebionetworks.repo.manager.table.metadata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.table.ViewObjectType;
import org.sagebionetworks.repo.model.table.ViewScopeFilter;
import org.sagebionetworks.repo.model.table.ViewTypeMask;
import org.sagebionetworks.util.EnumUtils;

import com.google.common.collect.ImmutableSet;

@ExtendWith(MockitoExtension.class)
public class ViewScopeFilterBuilderTest {

	@Mock
	private ViewScopeFilterProvider provider;
	
	@Test
	public void testBuild() {
		ViewObjectType objectType = ViewObjectType.ENTITY;
		Long viewTypeMask = ViewTypeMask.File.getMask();
		Set<Long> containerIds = ImmutableSet.of(1L, 2L);
		
		List<String> subTypes = EnumUtils.names(EntityType.file);
		boolean filterByObjectId = false;
		
		when(provider.getObjectType()).thenReturn(objectType);
		when(provider.getSubTypesForMask(viewTypeMask)).thenReturn(subTypes);
		when(provider.isFilterScopeByObjectId(viewTypeMask)).thenReturn(filterByObjectId);
		
		ViewScopeFilterBuilder builder = new ViewScopeFilterBuilder(provider, viewTypeMask)
				.withContainerIds(containerIds);
		
		// Call under test
		ViewScopeFilter filter = builder.build();
		
		assertEquals(objectType, filter.getObjectType());
		assertEquals(subTypes, filter.getSubTypes());
		assertEquals(filterByObjectId, filter.isFilterByObjectId());
		assertEquals(containerIds, filter.getContainerIds());
	}
	
	@Test
	public void testBuildWithNullMask() {
		
		IllegalArgumentException ex = new IllegalArgumentException("viewTypeMask is required.");
		
		doThrow(ex).when(provider).getSubTypesForMask(any());
		
		Long viewTypeMask = null;
		
		ViewScopeFilterBuilder builder = new ViewScopeFilterBuilder(provider, viewTypeMask);
	
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			builder.build();
		}).getMessage();
		
		assertEquals("viewTypeMask is required.", message);
	}
	
	@Test
	public void testBuildWithNullProvider() {
		Long viewTypeMask = ViewTypeMask.File.getMask();
		ViewScopeFilterProvider provider = null;
		
		ViewScopeFilterBuilder builder = new ViewScopeFilterBuilder(provider, viewTypeMask);
	
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			builder.build();
		}).getMessage();
		
		assertEquals("provider is required.", message);
	}
	
	@Test
	public void testBuildWithNullContainers() {
		Long viewTypeMask = ViewTypeMask.File.getMask();
		Set<Long> containerIds = null;
		
		ViewScopeFilterBuilder builder = new ViewScopeFilterBuilder(provider, viewTypeMask)
				.withContainerIds(containerIds);
		
		// Call under test
		ViewScopeFilter filter = builder.build();
		
		assertEquals(Collections.emptySet(), filter.getContainerIds());
	}
	
	@Test
	public void testBuildWithContainers() {
		Long viewTypeMask = ViewTypeMask.File.getMask();
		Set<Long> containerIds = ImmutableSet.of(1L, 2L);
		
		ViewScopeFilterBuilder builder = new ViewScopeFilterBuilder(provider, viewTypeMask)
				.withContainerIds(containerIds);
		
		// Call under test
		ViewScopeFilter filter = builder.build();
		
		assertEquals(containerIds, filter.getContainerIds());
	}
	
}
