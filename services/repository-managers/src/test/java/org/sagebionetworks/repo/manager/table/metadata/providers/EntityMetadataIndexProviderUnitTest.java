package org.sagebionetworks.repo.manager.table.metadata.providers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.NodeManager;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.LimitExceededException;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.annotation.v2.Annotations;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.ObjectDataDTO;
import org.sagebionetworks.repo.model.table.ViewObjectType;
import org.sagebionetworks.repo.model.table.ViewTypeMask;
import org.sagebionetworks.table.cluster.metadata.ObjectFieldTypeMapper;
import org.sagebionetworks.util.EnumUtils;

import com.google.common.collect.ImmutableList;
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
		Annotations result = provider.getAnnotations(mockUser, objectId);
		
		assertEquals(mockAnnotations, result);
		
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

}
