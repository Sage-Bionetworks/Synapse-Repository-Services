package org.sagebionetworks.repo.manager.table.metadata.providers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.evaluation.dao.EvaluationDAO;
import org.sagebionetworks.evaluation.dao.SubmissionDAO;
import org.sagebionetworks.repo.model.IdAndChecksum;
import org.sagebionetworks.repo.model.IdAndEtag;
import org.sagebionetworks.repo.model.table.ObjectDataDTO;
import org.sagebionetworks.repo.model.table.SubType;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;

@ExtendWith(MockitoExtension.class)
public class SubmissionObjectProviderTest {

	
	@Mock
	private SubmissionDAO mockSubmissionDao;
	@Mock
	private EvaluationDAO mockEvaluationDao;
	@Mock
	private ObjectDataDTO mockData;

	@Mock
	private IdAndEtag mockIdAndEtag;
	
	@InjectMocks
	private SubmissionObjectProvider provider;
	
	@Test
	public void testGetObjectData() {

		List<ObjectDataDTO> expected = Collections.singletonList(mockData);

		List<Long> objectIds = ImmutableList.of(1L, 2L, 3L);

		int maxAnnotationChars = 5;

		when(mockSubmissionDao.getSubmissionData(any(), anyInt())).thenReturn(expected);

		// Call under test
		Iterator<ObjectDataDTO> iterator = provider.getObjectData(objectIds, maxAnnotationChars);
		List<ObjectDataDTO> result = new ArrayList<ObjectDataDTO>();
		iterator.forEachRemaining(result::add);

		assertEquals(expected, result);
		verify(mockSubmissionDao).getSubmissionData(objectIds, maxAnnotationChars);
	}
	
	@Test
	public void testStreamOverIdsAndChecksumsWithParentIds() {
		Long salt = 123L;
		Set<SubType> subTypes = Sets.newHashSet(SubType.file);
		Set<Long> parentId = Sets.newHashSet(1L, 2L, 3L);

		List<IdAndChecksum> all = buildIdsAndChecksum(4);

		when(mockSubmissionDao.getIdAndChecksumsPage(any(), any(), any(), any())).thenReturn(all,
				Collections.emptyList());

		// call under test
		Iterator<IdAndChecksum> resultsIt = provider.streamOverIdsAndChecksumsForChildren(salt, parentId, subTypes);
		List<IdAndChecksum> allResults = new ArrayList<IdAndChecksum>();
		resultsIt.forEachRemaining(i -> allResults.add(i));
		assertEquals(all, allResults);
		
		long pageSize = SubmissionObjectProvider.PAGE_SIZE;
		verify(mockSubmissionDao, times(2)).getIdAndChecksumsPage(any(), any(), any(), any());
		verify(mockSubmissionDao).getIdAndChecksumsPage(eq(salt), eq(parentId), eq(pageSize), eq(0L));
		verify(mockSubmissionDao).getIdAndChecksumsPage(eq(salt), eq(parentId), eq(pageSize), eq(pageSize));
	}
	
	@Test
	public void teststreamOverIdsAndChecksumsForObjects() {
		Long salt = 123L;
		Set<Long> ids = Sets.newHashSet(1L, 2L, 3L);
		String message = assertThrows(UnsupportedOperationException.class, ()->{
			provider.streamOverIdsAndChecksumsForObjects(salt, ids);
		}).getMessage();
		assertEquals("All submission views are hierarchical", message);
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
