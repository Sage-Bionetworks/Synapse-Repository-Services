package org.sagebionetworks.repo.manager.table.metadata.providers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.NodeManager;
import org.sagebionetworks.repo.manager.table.metadata.DefaultColumnModel;
import org.sagebionetworks.repo.model.IdAndEtag;
import org.sagebionetworks.repo.model.LimitExceededException;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.annotation.v2.Annotations;
import org.sagebionetworks.repo.model.dbo.dao.table.ViewScopeDao;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.ObjectDataDTO;
import org.sagebionetworks.repo.model.table.ReplicationType;
import org.sagebionetworks.repo.model.table.SubType;
import org.sagebionetworks.repo.model.table.TableConstants;
import org.sagebionetworks.repo.model.table.ViewObjectType;
import org.sagebionetworks.repo.model.table.ViewScopeType;
import org.sagebionetworks.repo.model.table.ViewTypeMask;
import org.sagebionetworks.table.cluster.metadata.ObjectFieldTypeMapper;
import org.sagebionetworks.table.cluster.view.filter.FlatIdsFilter;
import org.sagebionetworks.table.cluster.view.filter.HierarchicaFilter;
import org.sagebionetworks.table.cluster.view.filter.ViewFilter;

import com.google.common.collect.Sets;

@ExtendWith(MockitoExtension.class)
public class EntityMetadataIndexProviderUnitTest {

	@Mock
	private NodeManager mockNodeManager;

	@Mock
	private NodeDAO mockNodeDao;
	
	@Mock
	private ViewScopeDao mockViewScopDao;

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
	public void testGetSubTypesForMaskEmpty() {
		Long viewTypeMask = 0L;

		// Call under test
		Set<SubType> result = provider.getSubTypesForMask(viewTypeMask);

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

		Set<SubType> expected = Sets.newHashSet(SubType.file);

		// Call under test
		Set<SubType> result = provider.getSubTypesForMask(viewTypeMask);

		assertEquals(expected, result);
	}

	@Test
	public void testGetSubTypesForMaskWithMixed() {
		Long viewTypeMask = ViewTypeMask.File.getMask() | ViewTypeMask.Project.getMask();

		Set<SubType> expected = Sets.newHashSet(SubType.file, SubType.project);

		// Call under test
		Set<SubType> result = provider.getSubTypesForMask(viewTypeMask);

		assertEquals(expected, result);
	}
	
	@Test
	public void testGetSubTypesForMaskWithAllTypes() {
		// one of each
		long viewTypeMask = (long) 0xffffffff;

		// Call under test
		Set<SubType> result = provider.getSubTypesForMask(viewTypeMask);
		
		Set<SubType> expected = Arrays.stream(ViewTypeMask.values()).map(t -> SubType.valueOf(t.getEntityType().name()))
				.collect(Collectors.toSet());

		assertEquals(expected, result);
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
	public void testDefaultColumnModelWithDatasetCollectionMask() {
		Long viewTypeMask = ViewTypeMask.DatasetCollection.getMask();

		DefaultColumnModel expected = EntityMetadataIndexProvider.DATASET_COLLECTION_DEFAULT_COLUMNS;
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
	public void testDefaultColumnModelExcludingFileMaskAndDatasetCollection() {
		Long viewTypeMask = 0L;

		for (ViewTypeMask type : ViewTypeMask.values()) {
			if (type != ViewTypeMask.File && type != ViewTypeMask.DatasetCollection) {
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
	public void testGetViewFilter() {
		long viewId = 123L;
		long viewTypeMask = ViewTypeMask.Project.getMask();
		Set<Long> scope = Sets.newHashSet(1L,2L);
		when(mockViewScopDao.getViewScope(viewId)).thenReturn(scope);
		when(mockViewScopDao.getViewScopeType(viewId)).thenReturn(new ViewScopeType(ViewObjectType.ENTITY, viewTypeMask));
		// call under test
		ViewFilter filter= provider.getViewFilter(viewId);
		ViewFilter expected = new FlatIdsFilter(ReplicationType.ENTITY, Sets.newHashSet(SubType.project), scope);
		assertEquals(expected, filter);
		verify(mockViewScopDao).getViewScope(viewId);
		verify(mockViewScopDao).getViewScopeType(viewId);
	}
	
	@Test
	public void testGetViewFilterWithProject() {
		long viewTypeMask = ViewTypeMask.Project.getMask();
		Set<Long> scope = Sets.newHashSet(1L,2L);
		// call under test
		ViewFilter filter = provider.getViewFilter(viewTypeMask, scope);
		ViewFilter expected = new FlatIdsFilter(ReplicationType.ENTITY, Sets.newHashSet(SubType.project), scope);
		assertEquals(expected, filter);
	}
	
	@Test
	public void testGetViewFilterWithFile() throws LimitExceededException {
		long viewTypeMask = ViewTypeMask.File.getMask();
		Set<Long> scope = Sets.newHashSet(1L,2L);
		Set<Long> fullScope = Sets.newHashSet(1L,2L,3L);
		when(mockNodeDao.getAllContainerIds((Collection<Long>)any(), anyInt())).thenReturn(fullScope);
		// call under test
		ViewFilter filter = provider.getViewFilter(viewTypeMask, scope);
		ViewFilter expected = new HierarchicaFilter(ReplicationType.ENTITY, Sets.newHashSet(SubType.file), fullScope);
		assertEquals(expected, filter);
	}
	
	@Test
	public void testGetViewFilterWithFileOverLimit() throws LimitExceededException {
		long viewTypeMask = ViewTypeMask.File.getMask();
		Set<Long> scope = Sets.newHashSet(1L,2L);
		when(mockNodeDao.getAllContainerIds((Collection<Long>)any(), anyInt())).thenThrow(new LimitExceededException("over"));
		String message = assertThrows(IllegalStateException.class, ()->{
			// call under test
			provider.getViewFilter(viewTypeMask, scope);
		}).getMessage();
		assertEquals("org.sagebionetworks.repo.model.LimitExceededException: over", message);
		verify(mockNodeDao).getAllContainerIds(scope, TableConstants.MAX_CONTAINERS_PER_VIEW);
	}
	
	@Test
	public void testValidateScopeAndTypeWithProjectPlusFile() throws LimitExceededException {
		int maxContainersPerView = 4;
		long viewTypeMask = ViewTypeMask.Project.getMask() | ViewTypeMask.File.getMask();
		Set<Long> scope = Sets.newHashSet(1L,2L);
		String message = assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			provider.validateScopeAndType(viewTypeMask, scope, maxContainersPerView);
		}).getMessage();
		
		assertEquals(EntityMetadataIndexProvider.PROJECT_TYPE_CANNOT_BE_COMBINED_WITH_ANY_OTHER_TYPE, message);
		verify(mockNodeDao, never()).getAllContainerIds((Collection<Long>)any(), anyInt());
	}
	
	@Test
	public void testValidateScopeAndTypeWithProjectUnderLimit() throws LimitExceededException {
		int maxContainersPerView = 4;
		long viewTypeMask = ViewTypeMask.Project.getMask();
		Set<Long> scope = Sets.newHashSet(1L,2L);
		// call under test
		provider.validateScopeAndType(viewTypeMask, scope, maxContainersPerView);
		verify(mockNodeDao, never()).getAllContainerIds((Collection<Long>)any(), anyInt());
	}
	
	@Test
	public void testValidateScopeAndTypeWithProjecNullScope() throws LimitExceededException {
		int maxContainersPerView = 4;
		long viewTypeMask = ViewTypeMask.Project.getMask();
		Set<Long> scope = null;
		// call under test
		provider.validateScopeAndType(viewTypeMask, scope, maxContainersPerView);
		verify(mockNodeDao, never()).getAllContainerIds((Collection<Long>)any(), anyInt());
	}
	
	@Test
	public void testValidateScopeAndTypeWithProjectOverLimit() throws LimitExceededException {
		int maxContainersPerView = 1;
		long viewTypeMask = ViewTypeMask.Project.getMask();
		Set<Long> scope = Sets.newHashSet(1L,2L);
		String message = assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			provider.validateScopeAndType(viewTypeMask, scope, maxContainersPerView);
		}).getMessage();
		assertEquals(String.format(EntityMetadataIndexProvider.SCOPE_SIZE_LIMITED_EXCEEDED_PROJECT_VIEW, maxContainersPerView), message);
		verify(mockNodeDao, never()).getAllContainerIds((Collection<Long>)any(), anyInt());
	}
	
	@Test
	public void testValidateScopeAndTypeWithFileUnderLimit() throws LimitExceededException {
		int maxContainersPerView = 4;
		long viewTypeMask = ViewTypeMask.File.getMask();
		Set<Long> scope = Sets.newHashSet(1L,2L);
		// call under test
		provider.validateScopeAndType(viewTypeMask, scope, maxContainersPerView);
		verify(mockNodeDao).getAllContainerIds(scope, maxContainersPerView);
	}
	
	@Test
	public void testValidateScopeAndTypeWithFileNullScope() throws LimitExceededException {
		int maxContainersPerView = 4;
		long viewTypeMask = ViewTypeMask.File.getMask();
		Set<Long> scope = null;
		// call under test
		provider.validateScopeAndType(viewTypeMask, scope, maxContainersPerView);
		verify(mockNodeDao, never()).getAllContainerIds((Collection<Long>)any(), anyInt());
	}

	@Test
	public void testValidateScopeAndTypeWithFileOverLimit() throws LimitExceededException {
		int maxContainersPerView = 4;
		long viewTypeMask = ViewTypeMask.File.getMask();
		Set<Long> scope = Sets.newHashSet(1L,2L);
		when(mockNodeDao.getAllContainerIds((Collection<Long>)any(), anyInt())).thenThrow(new LimitExceededException("over"));
		String message = assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			provider.validateScopeAndType(viewTypeMask, scope, maxContainersPerView);
		}).getMessage();
		
		assertEquals(String.format(EntityMetadataIndexProvider.SCOPE_SIZE_LIMITED_EXCEEDED_FILE_VIEW, maxContainersPerView), message);
		verify(mockNodeDao).getAllContainerIds(scope, maxContainersPerView);
	}
}
