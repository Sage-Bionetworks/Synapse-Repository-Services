package org.sagebionetworks.repo.manager.table.metadata.providers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.LimitExceededException;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.ViewObjectType;
import org.sagebionetworks.repo.model.table.ViewTypeMask;
import org.sagebionetworks.table.cluster.metadata.ObjectFieldTypeMapper;
import org.sagebionetworks.util.EnumUtils;

import com.google.common.collect.ImmutableSet;

@ExtendWith(MockitoExtension.class)
public class EntityMetadataIndexProviderUnitTest {

	@Mock
	private NodeDAO mockNodeDao;

	@InjectMocks
	private EntityMetadataIndexProvider provider;

	@Test
	public void testGetObjectType() {
		// Call under test
		assertEquals(ViewObjectType.ENTITY, provider.getObjectType());
	}

	@Test
	public void testObjectFieldTypeMapper() {
		ObjectFieldTypeMapper mapper = provider;

		// Call under test
		assertEquals(ColumnType.ENTITYID, mapper.getIdColumnType());
		assertEquals(ColumnType.ENTITYID, mapper.getParentIdColumnType());
		assertEquals(ColumnType.ENTITYID, mapper.getBenefactorIdColumnType());
	}

	@Test
	public void testSupportsSubTypeFiltering() {
		// Call under test
		assertTrue(provider.supportsSubtypeFiltering());
	}

	@Test
	public void testIsFilterScopeByObjectIdWithFileMask() {
		Long viewTypeMask = ViewTypeMask.File.getMask();

		// Call under test
		assertFalse(provider.isFilterScopeByObjectId(viewTypeMask));
	}

	@Test
	public void testIsFilterScopeByObjectIdWithProjectMask() {
		Long viewTypeMask = ViewTypeMask.Project.getMask();

		// Call under test
		assertTrue(provider.isFilterScopeByObjectId(viewTypeMask));
	}

	@Test
	public void testIsFilterScopeByObjectIdWithMixedMask() {
		Long viewTypeMask = ViewTypeMask.File.getMask() | ViewTypeMask.Project.getMask();

		// Call under test
		assertFalse(provider.isFilterScopeByObjectId(viewTypeMask));
	}

	@Test
	public void testGetSubTypesForMaskEmpty() {
		Long viewTypeMask = 0L;

		// Call under test
		List<String> result = provider.getSubTypesForMask(viewTypeMask);

		assertTrue(result.isEmpty());
	}

	@Test
	public void testGetSubTypesForMaskWithFile() {
		Long viewTypeMask = ViewTypeMask.File.getMask();

		List<String> expected = EnumUtils.names(EntityType.file);

		// Call under test
		List<String> result = provider.getSubTypesForMask(viewTypeMask);

		assertEquals(expected, result);
	}

	@Test
	public void testGetSubTypesForMaskWithMixed() {
		Long viewTypeMask = ViewTypeMask.File.getMask() | ViewTypeMask.Project.getMask();

		List<String> expected = EnumUtils.names(EntityType.file, EntityType.project);

		// Call under test
		List<String> result = provider.getSubTypesForMask(viewTypeMask);

		assertEquals(expected, result);
	}

	@Test
	public void testGetAllContainerIdsForScope() throws Exception {

		Set<Long> scope = ImmutableSet.of(1L, 2L);
		Long viewTypeMask = ViewTypeMask.File.getMask();
		int containerLimit = 10;

		Set<Long> expected = ImmutableSet.of(1L, 2L, 3L, 4L);

		when(mockNodeDao.getAllContainerIds(anySet(), anyInt())).thenReturn(expected);

		// Call under test
		Set<Long> result = provider.getAllContainerIdsForScope(scope, viewTypeMask, containerLimit);

		assertEquals(expected, result);
	}

	@Test
	public void testGetAllContainerIdsForScopeForProject() throws Exception {

		Set<Long> scope = ImmutableSet.of(1L, 2L);
		Long viewTypeMask = ViewTypeMask.Project.getMask();
		int containerLimit = 10;

		// Call under test
		Set<Long> result = provider.getAllContainerIdsForScope(scope, viewTypeMask, containerLimit);

		assertEquals(scope, result);
		verifyZeroInteractions(mockNodeDao);
	}

	@Test
	public void testGetAllContainerIdsForScopeExceedLimit() throws Exception {

		Set<Long> scope = ImmutableSet.of(1L, 2L);
		Long viewTypeMask = ViewTypeMask.File.getMask();
		int containerLimit = 10;

		LimitExceededException limitEx = new LimitExceededException("Error");

		doThrow(limitEx).when(mockNodeDao).getAllContainerIds(anySet(), anyInt());

		LimitExceededException result = assertThrows(LimitExceededException.class, () -> {
			// Call under test
			provider.getAllContainerIdsForScope(scope, viewTypeMask, containerLimit);
		});

		assertEquals(limitEx, result);

	}

}
