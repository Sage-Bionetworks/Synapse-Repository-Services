package org.sagebionetworks.repo.manager.table.metadata.providers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.model.IdAndChecksum;
import org.sagebionetworks.repo.model.IdAndEtag;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.table.ObjectDataDTO;
import org.sagebionetworks.repo.model.table.ReplicationType;
import org.sagebionetworks.repo.model.table.SubType;
import org.sagebionetworks.table.cluster.view.filter.FlatIdAndVersionFilter;
import org.sagebionetworks.table.cluster.view.filter.FlatIdsFilter;
import org.sagebionetworks.table.cluster.view.filter.HierarchicaFilter;
import org.sagebionetworks.table.cluster.view.filter.IdVersionPair;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

@ExtendWith(MockitoExtension.class)
public class EntityObjectProviderTest {

	@Mock
	private NodeDAO mockNodeDao;
	@Mock
	private ObjectDataDTO mockData;

	@InjectMocks
	private EntityObjectProvider provider;

	@Mock
	private IdAndEtag mockIdAndEtag;

	@Test
	public void testGetObjectData() {

		List<ObjectDataDTO> expected = Collections.singletonList(mockData);
		List<ObjectDataDTO> empty = Collections.emptyList();

		List<Long> objectIds = ImmutableList.of(1L, 2L, 3L);

		int maxAnnotationChars = 5;

		when(mockNodeDao.getEntityDTOs(any(), anyInt(), anyLong(), anyLong())).thenReturn(expected, empty);

		// Call under test
		Iterator<ObjectDataDTO> iterator = provider.getObjectData(objectIds, maxAnnotationChars);
		List<ObjectDataDTO> result = new ArrayList<ObjectDataDTO>();
		iterator.forEachRemaining(result::add);

		assertEquals(expected, result);
		verify(mockNodeDao).getEntityDTOs(objectIds, maxAnnotationChars, EntityObjectProvider.PAGE_SIZE, 0L);
		verify(mockNodeDao).getEntityDTOs(objectIds, maxAnnotationChars, EntityObjectProvider.PAGE_SIZE,
				EntityObjectProvider.PAGE_SIZE);
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
	public void testStreamOverIdsAndChecksumsWithHierarchyFilter() {
		Long salt = 123L;
		Set<SubType> subTypes = Sets.newHashSet(SubType.file);
		Set<Long> parentId = Sets.newHashSet(1L, 2L, 3L);
		HierarchicaFilter filter = new HierarchicaFilter(ReplicationType.ENTITY, subTypes, parentId);

		List<IdAndChecksum> all = buildIdsAndChecksum(4);
		List<IdAndChecksum> pageOne = all.subList(0, 3);
		List<IdAndChecksum> pageTwo = all.subList(3, 4);

		when(mockNodeDao.getIdsAndChecksumsForChildren(any(), any(), any(), any(), any())).thenReturn(pageOne, pageTwo,
				Collections.emptyList());

		int pageSize = 3;
		// call under test
		Iterator<IdAndChecksum> resultsIt = provider.streamOverIdsAndChecksums(salt, filter, pageSize);
		List<IdAndChecksum> allResults = new ArrayList<IdAndChecksum>();
		resultsIt.forEachRemaining(i -> allResults.add(i));
		assertEquals(all, allResults);
		
		verify(mockNodeDao, times(3)).getIdsAndChecksumsForChildren(any(), any(), any(), any(), any());
		verify(mockNodeDao).getIdsAndChecksumsForChildren(eq(salt), eq(parentId), eq(subTypes), eq(3L), eq(0L));
		verify(mockNodeDao).getIdsAndChecksumsForChildren(eq(salt), eq(parentId), eq(subTypes), eq(3L), eq(3L));
		verify(mockNodeDao).getIdsAndChecksumsForChildren(eq(salt), eq(parentId), eq(subTypes), eq(3L), eq(6L));
	}
	
	@Test
	public void testStreamOverIdsAndChecksumsWithFlatFilter() {
		Long salt = 123L;
		Set<Long> scope = Sets.newHashSet(1L, 2L, 3L);
		Set<SubType> subTypes = Sets.newHashSet(SubType.file);
		FlatIdsFilter filter = new FlatIdsFilter(ReplicationType.ENTITY, subTypes, scope);

		List<IdAndChecksum> all = buildIdsAndChecksum(4);
		List<IdAndChecksum> pageOne = all.subList(0, 3);
		List<IdAndChecksum> pageTwo = all.subList(3, 4);

		when(mockNodeDao.getIdsAndChecksumsForObjects(any(), any(), any(), any())).thenReturn(pageOne, pageTwo,
				Collections.emptyList());

		int pageSize = 3;
		// call under test
		Iterator<IdAndChecksum> resultsIt = provider.streamOverIdsAndChecksums(salt, filter, pageSize);
		List<IdAndChecksum> allResults = new ArrayList<IdAndChecksum>();
		resultsIt.forEachRemaining(i -> allResults.add(i));
		assertEquals(all, allResults);
		
		verify(mockNodeDao, times(3)).getIdsAndChecksumsForObjects(any(), any(), any(), any());
		verify(mockNodeDao).getIdsAndChecksumsForObjects(eq(salt), eq(scope), eq(3L), eq(0L));
		verify(mockNodeDao).getIdsAndChecksumsForObjects(eq(salt), eq(scope), eq(3L), eq(3L));
		verify(mockNodeDao).getIdsAndChecksumsForObjects(eq(salt), eq(scope), eq(3L), eq(6L));
	}
	
	@Test
	public void testStreamOverIdsAndChecksumsWithFlatIdAndVersionFilter() {
		Long salt = 123L;
		Set<IdVersionPair> scope = Sets.newHashSet(
				new IdVersionPair().setId(1L).setVersion(3L),
				new IdVersionPair().setId(2L).setVersion(4L),
				new IdVersionPair().setId(3L).setVersion(5L)
		);
		Set<Long> objectIds = scope.stream().map(i->i.getId()).collect(Collectors.toSet());
		Set<SubType> subTypes = Sets.newHashSet(SubType.file);
		FlatIdAndVersionFilter filter = new FlatIdAndVersionFilter(ReplicationType.ENTITY, subTypes, scope);

		List<IdAndChecksum> all = buildIdsAndChecksum(4);
		List<IdAndChecksum> pageOne = all.subList(0, 3);
		List<IdAndChecksum> pageTwo = all.subList(3, 4);

		when(mockNodeDao.getIdsAndChecksumsForObjects(any(), any(), any(), any())).thenReturn(pageOne, pageTwo,
				Collections.emptyList());

		int pageSize = 3;
		// call under test
		Iterator<IdAndChecksum> resultsIt = provider.streamOverIdsAndChecksums(salt, filter, pageSize);
		List<IdAndChecksum> allResults = new ArrayList<IdAndChecksum>();
		resultsIt.forEachRemaining(i -> allResults.add(i));
		assertEquals(all, allResults);
		
		verify(mockNodeDao, times(3)).getIdsAndChecksumsForObjects(any(), any(), any(), any());
		verify(mockNodeDao).getIdsAndChecksumsForObjects(eq(salt), eq(objectIds), eq(3L), eq(0L));
		verify(mockNodeDao).getIdsAndChecksumsForObjects(eq(salt), eq(objectIds), eq(3L), eq(3L));
		verify(mockNodeDao).getIdsAndChecksumsForObjects(eq(salt), eq(objectIds), eq(3L), eq(6L));
	}

	/**
	 * Helper to create a list of List<IdAndChecksum> of the given size.
	 * 
	 * @param size
	 * @return
	 */
	public List<IdAndChecksum> buildIdsAndChecksum(int size) {
		List<IdAndChecksum> results = new ArrayList<>();
		for (int i = 0; i < size; i++) {
			results.add(new IdAndChecksum().withId(new Long(i)).withChecksum(new Long(i + 1)));
		}
		return results;
	}
}
