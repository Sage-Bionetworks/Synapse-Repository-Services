package org.sagebionetworks.repo.manager.table.metadata.providers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.NodeManager;
import org.sagebionetworks.repo.manager.table.metadata.DefaultColumnModel;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.IdAndEtag;
import org.sagebionetworks.repo.model.LimitExceededException;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.annotation.v2.Annotations;
import org.sagebionetworks.repo.model.dbo.dao.NodeUtils;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.ObjectDataDTO;
import org.sagebionetworks.repo.model.table.ViewObjectType;
import org.sagebionetworks.repo.model.table.ViewType;
import org.sagebionetworks.repo.model.table.ViewTypeMask;
import org.sagebionetworks.table.cluster.metadata.ObjectFieldTypeMapper;
import org.sagebionetworks.util.EnumUtils;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

@ExtendWith(MockitoExtension.class)
public class EntityMetadataIndexProviderUnitTest {

	@Mock
	private NodeManager mockNodeManager;

	@Mock
	private NodeDAO mockNodeDao;

	@InjectMocks
	private EntityMetadataIndexProvider provider;

	@Mock
	private ObjectDataDTO mockData;

	@Mock
	private UserInfo mockUser;

	@Mock
	private Annotations mockAnnotations;

	@Mock
	private ColumnModel mockModel;

	@Mock
	private IdAndEtag mockIdAndEtag;

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
	public void testIsFilterScopeByObjectIdWithNullMask() {
		Long viewTypeMask = null;

		String message = assertThrows(IllegalArgumentException.class, () -> { 
			// Call under test
			provider.isFilterScopeByObjectId(viewTypeMask);
		}).getMessage();
		
		assertEquals("viewTypeMask is required.", message);
	}

	@Test
	public void testGetSubTypesForMaskEmpty() {
		Long viewTypeMask = 0L;

		// Call under test
		List<String> result = provider.getSubTypesForMask(viewTypeMask);

		assertTrue(result.isEmpty());
	}
	
	@Test
	public void testGetSubTypesForNullMask() {
		Long viewTypeMask = null;

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			provider.getSubTypesForMask(viewTypeMask);
		}).getMessage();

		assertEquals("viewTypeMask is required.", message);
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
		Set<Long> result = provider.getContainerIdsForScope(scope, viewTypeMask, containerLimit);

		assertEquals(expected, result);
	}

	@Test
	public void testGetAllContainerIdsForScopeForProject() throws Exception {

		Set<Long> scope = ImmutableSet.of(1L, 2L);
		Long viewTypeMask = ViewTypeMask.Project.getMask();
		int containerLimit = 10;

		// Call under test
		Set<Long> result = provider.getContainerIdsForScope(scope, viewTypeMask, containerLimit);

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
			provider.getContainerIdsForScope(scope, viewTypeMask, containerLimit);
		});

		assertEquals(limitEx, result);

	}

	@Test
	public void testcreateViewOverLimitMessageFileView() {
		int limit = 10;
		// call under test
		String message = provider.createViewOverLimitMessage(ViewTypeMask.File.getMask(), limit);
		assertEquals(
				"The view's scope exceeds the maximum number of " + limit + " projects and/or folders. "
						+ "Note: The sub-folders of each project and folder in the scope count towards the limit.",
				message);
	}

	@Test
	public void testcreateViewOverLimitMessageFileAndTableView() {
		int limit = 10;
		// call under test
		String message = provider
				.createViewOverLimitMessage(ViewTypeMask.getMaskForDepricatedType(ViewType.file_and_table), limit);
		assertEquals(
				"The view's scope exceeds the maximum number of " + limit + " projects and/or folders. "
						+ "Note: The sub-folders of each project and folder in the scope count towards the limit.",
				message);
	}

	@Test
	public void testcreateViewOverLimitMessageProjectView() {
		int limit = 10;
		// call under test
		String message = provider.createViewOverLimitMessage(ViewTypeMask.Project.getMask(), limit);
		assertEquals("The view's scope exceeds the maximum number of " + limit + " projects.", message);
	}

	@Test
	public void testGetObjectData() {

		List<ObjectDataDTO> expected = Collections.singletonList(mockData);

		List<Long> objectIds = ImmutableList.of(1L, 2L, 3L);

		int maxAnnotationChars = 5;

		when(mockNodeDao.getEntityDTOs(any(), anyInt())).thenReturn(expected);

		// Call under test
		List<ObjectDataDTO> result = provider.getObjectData(objectIds, maxAnnotationChars);

		assertEquals(expected, result);
		verify(mockNodeDao).getEntityDTOs(objectIds, maxAnnotationChars);
	}

	@Test
	public void testGetAnnotations() {
		String objectId = "syn123";

		when(mockNodeManager.getUserAnnotations(any(), any())).thenReturn(mockAnnotations);

		// Call under test
		Optional<Annotations> result = provider.getAnnotations(mockUser, objectId);

		assertTrue(result.isPresent());
		assertEquals(mockAnnotations, result.get());

		verify(mockNodeManager).getUserAnnotations(mockUser, objectId);
	}

	@Test
	public void testUpdateAnnotations() {
		String objectId = "syn123";

		when(mockNodeManager.updateUserAnnotations(any(), any(), any())).thenReturn(mockAnnotations);

		// Call under test
		provider.updateAnnotations(mockUser, objectId, mockAnnotations);

		verify(mockNodeManager).updateUserAnnotations(mockUser, objectId, mockAnnotations);
	}

	@Test
	public void testCanUpdateAnnotation() {

		// Call under test
		boolean result = provider.canUpdateAnnotation(mockModel);

		assertTrue(result);
	}

	@Test
	public void testDefaultColumnModelWithNullMask() {
		Long viewTypeMask = null;

		assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			provider.getDefaultColumnModel(viewTypeMask);
		});
	}

	@Test
	public void testDefaultColumnModelWithFileMask() {
		Long viewTypeMask = ViewTypeMask.File.getMask();

		DefaultColumnModel expected = EntityMetadataIndexProvider.FILE_VIEW_DEFAULT_COLUMNS;
		// Call under test
		DefaultColumnModel model = provider.getDefaultColumnModel(viewTypeMask);

		assertEquals(expected, model);
	}

	@Test
	public void testDefaultColumnModelWithFileAndTableMask() {
		Long viewTypeMask = ViewTypeMask.File.getMask() | ViewTypeMask.Table.getMask();

		DefaultColumnModel expected = EntityMetadataIndexProvider.FILE_VIEW_DEFAULT_COLUMNS;
		// Call under test
		DefaultColumnModel model = provider.getDefaultColumnModel(viewTypeMask);

		assertEquals(expected, model);
	}

	@Test
	public void testDefaultColumnModelWithProjectMask() {
		Long viewTypeMask = ViewTypeMask.Project.getMask();

		DefaultColumnModel expected = EntityMetadataIndexProvider.BASIC_ENTITY_DEAFULT_COLUMNS;
		// Call under test
		DefaultColumnModel model = provider.getDefaultColumnModel(viewTypeMask);

		assertEquals(expected, model);
	}

	@Test
	public void testDefaultColumnModelExcludingFileMask() {
		Long viewTypeMask = 0L;

		for (ViewTypeMask type : ViewTypeMask.values()) {
			if (type != ViewTypeMask.File) {
				viewTypeMask |= type.getMask();
			}
		}

		DefaultColumnModel expected = EntityMetadataIndexProvider.BASIC_ENTITY_DEAFULT_COLUMNS;
		// Call under test
		DefaultColumnModel model = provider.getDefaultColumnModel(viewTypeMask);

		assertEquals(expected, model);
	}

	@Test
	public void testDefaultColumnModelincludingFileMask() {
		Long viewTypeMask = 0L;

		for (ViewTypeMask type : ViewTypeMask.values()) {
			viewTypeMask |= type.getMask();
		}

		DefaultColumnModel expected = EntityMetadataIndexProvider.FILE_VIEW_DEFAULT_COLUMNS;
		// Call under test
		DefaultColumnModel model = provider.getDefaultColumnModel(viewTypeMask);

		assertEquals(expected, model);
	}

	@Test
	public void testGetContainerIdsForReconciliation() throws LimitExceededException {

		Set<Long> scope = ImmutableSet.of(1L, 2L);
		Long viewTypeMask = ViewTypeMask.File.getMask();
		int containerLimit = 10;

		Set<Long> expected = ImmutableSet.of(1L, 2L, 3L, 4L);

		when(mockNodeDao.getAllContainerIds(anySet(), anyInt())).thenReturn(expected);

		// Call under test
		Set<Long> result = provider.getContainerIdsForReconciliation(scope, viewTypeMask, containerLimit);

		assertEquals(expected, result);
	}

	@Test
	public void testGetContainerIdsForReconciliationForProject() throws LimitExceededException {

		Set<Long> scope = ImmutableSet.of(1L, 2L);
		Long viewTypeMask = ViewTypeMask.Project.getMask();
		int containerLimit = 10;

		Set<Long> expected = ImmutableSet.of(KeyFactory.stringToKey(NodeUtils.ROOT_ENTITY_ID));

		// Call under test
		Set<Long> result = provider.getContainerIdsForReconciliation(scope, viewTypeMask, containerLimit);

		assertEquals(expected, result);
		verifyZeroInteractions(mockNodeDao);
	}

	@Test
	public void testGetAvaliableContainers() {

		List<Long> containerIds = ImmutableList.of(1L, 2L);
		Set<Long> expectedIds = ImmutableSet.of(1L);

		when(mockNodeDao.getAvailableNodes(any())).thenReturn(expectedIds);

		// Call under test
		Set<Long> result = provider.getAvailableContainers(containerIds);

		assertEquals(expectedIds, result);
		verify(mockNodeDao).getAvailableNodes(containerIds);
	}

	@Test
	public void testGetChildren() {

		Long containerId = 1L;
		List<IdAndEtag> expected = ImmutableList.of(mockIdAndEtag, mockIdAndEtag);

		when(mockNodeDao.getChildren(anyLong())).thenReturn(expected);

		// Call under test
		List<IdAndEtag> result = provider.getChildren(containerId);

		assertEquals(expected, result);
		verify(mockNodeDao).getChildren(containerId);
	}

	@Test
	public void testGetSumOfChildCRCsForEachContainer() {
		List<Long> containerIds = ImmutableList.of(1L, 2L);

		Map<Long, Long> expected = ImmutableMap.of(1L, 10L, 2L, 30L);

		when(mockNodeDao.getSumOfChildCRCsForEachParent(any())).thenReturn(expected);

		Map<Long, Long> result = provider.getSumOfChildCRCsForEachContainer(containerIds);

		assertEquals(expected, result);
		verify(mockNodeDao).getSumOfChildCRCsForEachParent(containerIds);
	}
	
	@Test
	public void testGetBenefactorObjectType() {
		
		// Call under test
		ObjectType objectType = provider.getBenefactorObjectType();
		
		assertEquals(ObjectType.ENTITY, objectType);
		
	}
	
	@Test
	public void testValidateScopeWithProjectCombinedWithOtherType() {
		
		long viewTypeMask = ViewTypeMask.Project.getMask() | ViewTypeMask.File.getMask();
		
		String message = assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			provider.validateTypeMask(viewTypeMask);
		}).getMessage();
		
		assertEquals(EntityMetadataIndexProvider.PROJECT_TYPE_CANNOT_BE_COMBINED_WITH_ANY_OTHER_TYPE, message);
	
	}

}
