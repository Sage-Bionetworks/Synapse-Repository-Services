package org.sagebionetworks.repo.manager.table.metadata.providers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.model.IdAndChecksum;
import org.sagebionetworks.repo.model.IdAndEtag;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.table.ObjectDataDTO;
import org.sagebionetworks.repo.model.table.SubType;

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
	public void testStreamOverIdsAndChecksumsWithParentIds() {
		Long salt = 123L;
		Set<SubType> subTypes = Sets.newHashSet(SubType.file);
		Set<Long> parentId = Sets.newHashSet(1L, 2L, 3L);

		List<IdAndChecksum> all = buildIdsAndChecksum(4);

		when(mockNodeDao.getIdsAndChecksumsForChildren(any(), any(), any(), any(), any())).thenReturn(all,
				Collections.emptyList());

		// call under test
		Iterator<IdAndChecksum> resultsIt = provider.streamOverIdsAndChecksumsForChildren(salt, parentId, subTypes);
		List<IdAndChecksum> allResults = new ArrayList<IdAndChecksum>();
		resultsIt.forEachRemaining(i -> allResults.add(i));
		assertEquals(all, allResults);
		
		long pageSize = EntityObjectProvider.PAGE_SIZE;
		verify(mockNodeDao, times(2)).getIdsAndChecksumsForChildren(any(), any(), any(), any(), any());
		verify(mockNodeDao).getIdsAndChecksumsForChildren(eq(salt), eq(parentId), eq(subTypes), eq(pageSize), eq(0L));
		verify(mockNodeDao).getIdsAndChecksumsForChildren(eq(salt), eq(parentId), eq(subTypes), eq(pageSize), eq(pageSize));
	}
	
	@Test
	public void testStreamOverIdsAndChecksumsWithObjectIds() {
		Long salt = 123L;
		Set<Long> scope = Sets.newHashSet(1L, 2L, 3L);

		List<IdAndChecksum> all = buildIdsAndChecksum(4);

		when(mockNodeDao.getIdsAndChecksumsForObjects(any(), any(), any(), any())).thenReturn(all,
				Collections.emptyList());

		// call under test
		Iterator<IdAndChecksum> resultsIt = provider.streamOverIdsAndChecksumsForObjects(salt, scope);
		List<IdAndChecksum> allResults = new ArrayList<IdAndChecksum>();
		resultsIt.forEachRemaining(i -> allResults.add(i));
		assertEquals(all, allResults);
		
		long pageSize = EntityObjectProvider.PAGE_SIZE;
		
		verify(mockNodeDao, times(2)).getIdsAndChecksumsForObjects(any(), any(), any(), any());
		verify(mockNodeDao).getIdsAndChecksumsForObjects(eq(salt), eq(scope), eq(pageSize), eq(0L));
		verify(mockNodeDao).getIdsAndChecksumsForObjects(eq(salt), eq(scope), eq(pageSize), eq(pageSize));
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
