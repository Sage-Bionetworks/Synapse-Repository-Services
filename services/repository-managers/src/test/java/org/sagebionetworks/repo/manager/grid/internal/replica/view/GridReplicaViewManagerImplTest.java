package org.sagebionetworks.repo.manager.grid.internal.replica.view;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.grid.db.GridIndexDao;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.GridHeader;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.RowView;
import org.sagebionetworks.repo.manager.grid.internal.replica.view.query.QueryElement;
import org.sagebionetworks.repo.model.grid.node.ConstantNode;
import org.sagebionetworks.repo.model.grid.patch.ConType;
import org.sagebionetworks.repo.model.grid.patch.ConValue;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;

@ExtendWith(MockitoExtension.class)
public class GridReplicaViewManagerImplTest {

	@Mock
	private GridIndexDao mockGridIndexDao;
	@Mock
	private GridHeader mockHeader;

	private GridReplicaViewManagerImpl manager;

	@BeforeEach
	public void setUp() {
		manager = spy(new GridReplicaViewManagerImpl(mockGridIndexDao));
	}

	@Test
	public void testGetQueryIteratorWithMultiplePages() {
		List<Long> capturedLimits = new ArrayList<>();
		List<Long> capturedOffsets = new ArrayList<>();

		doAnswer(invocation -> {
			QueryElement q = invocation.getArgument(1);
			capturedLimits.add(q.getLimit());
			capturedOffsets.add(q.getOffset());
			long offset = q.getOffset();
			if (offset == 0) {
				return createRows(0, 1000);
			} else if (offset == 1000) {
				return createRows(1000, 500);
			} else {
				return Collections.emptyList();
			}
		}).when(manager).querySinglePage(any(GridHeader.class), any(QueryElement.class));

		Iterator<RowView> iterator = manager.getQueryIterator(mockHeader, new QueryElement());
		List<RowView> results = new ArrayList<>();
		while (iterator.hasNext()) {
			results.add(iterator.next());
		}

		assertEquals(1500, results.size());
		assertEquals(List.of(1000L, 1000L), capturedLimits);
		assertEquals(List.of(0L, 1000L), capturedOffsets);
	}

	@Test
	public void testGetQueryIteratorWithEmptyResult() {
		doAnswer(invocation -> {
			return Collections.emptyList();
		}).when(manager).querySinglePage(any(GridHeader.class), any(QueryElement.class));

		Iterator<RowView> iterator = manager.getQueryIterator(mockHeader, new QueryElement());
		assertFalse(iterator.hasNext());
	}

	@Test
	public void testGetQueryIteratorWithCallerLimitSmallerThanPageSize() {
		List<Long> capturedLimits = new ArrayList<>();
		List<Long> capturedOffsets = new ArrayList<>();

		doAnswer(invocation -> {
			QueryElement q = invocation.getArgument(1);
			capturedLimits.add(q.getLimit());
			capturedOffsets.add(q.getOffset());
			return createRows(0, q.getLimit().intValue());
		}).when(manager).querySinglePage(any(GridHeader.class), any(QueryElement.class));

		Iterator<RowView> iterator = manager.getQueryIterator(mockHeader,
				new QueryElement().setLimit(123L));
		List<RowView> results = new ArrayList<>();
		while (iterator.hasNext()) {
			results.add(iterator.next());
		}

		assertEquals(123, results.size());
		// effectiveLimit = min(1000, 123 - 0) = 123
		assertEquals(List.of(123L), capturedLimits);
		assertEquals(List.of(0L), capturedOffsets);
	}

	@Test
	public void testGetQueryIteratorWithCallerLimitSpanningMultiplePages() {
		List<Long> capturedLimits = new ArrayList<>();
		List<Long> capturedOffsets = new ArrayList<>();

		doAnswer(invocation -> {
			QueryElement q = invocation.getArgument(1);
			capturedLimits.add(q.getLimit());
			capturedOffsets.add(q.getOffset());
			return createRows(0, q.getLimit().intValue());
		}).when(manager).querySinglePage(any(GridHeader.class), any(QueryElement.class));

		Iterator<RowView> iterator = manager.getQueryIterator(mockHeader,
				new QueryElement().setLimit(2500L));
		List<RowView> results = new ArrayList<>();
		while (iterator.hasNext()) {
			results.add(iterator.next());
		}

		assertEquals(2500, results.size());
		// Page 0: min(1000, 2500-0) = 1000
		// Page 1: min(1000, 2500-1000) = 1000
		// Page 2: min(1000, 2500-2000) = 500
		assertEquals(List.of(1000L, 1000L, 500L), capturedLimits);
		assertEquals(List.of(0L, 1000L, 2000L), capturedOffsets);
	}

	@Test
	public void testGetQueryIteratorWithCallerOffset() {
		List<Long> capturedLimits = new ArrayList<>();
		List<Long> capturedOffsets = new ArrayList<>();

		doAnswer(invocation -> {
			QueryElement q = invocation.getArgument(1);
			capturedLimits.add(q.getLimit());
			capturedOffsets.add(q.getOffset());
			return createRows(0, 500);
		}).when(manager).querySinglePage(any(GridHeader.class), any(QueryElement.class));

		Iterator<RowView> iterator = manager.getQueryIterator(mockHeader,
				new QueryElement().setOffset(200L));
		List<RowView> results = new ArrayList<>();
		while (iterator.hasNext()) {
			results.add(iterator.next());
		}

		assertEquals(500, results.size());
		assertEquals(List.of(1000L), capturedLimits);
		// callerOffset (200) + paginationOffset (0) = 200
		assertEquals(List.of(200L), capturedOffsets);
	}

	@Test
	public void testGetQueryIteratorWithCallerLimitAndOffset() {
		List<Long> capturedLimits = new ArrayList<>();
		List<Long> capturedOffsets = new ArrayList<>();

		doAnswer(invocation -> {
			QueryElement q = invocation.getArgument(1);
			capturedLimits.add(q.getLimit());
			capturedOffsets.add(q.getOffset());
			return createRows(0, q.getLimit().intValue());
		}).when(manager).querySinglePage(any(GridHeader.class), any(QueryElement.class));

		Iterator<RowView> iterator = manager.getQueryIterator(mockHeader,
				new QueryElement().setLimit(1500L).setOffset(200L));
		List<RowView> results = new ArrayList<>();
		while (iterator.hasNext()) {
			results.add(iterator.next());
		}

		assertEquals(1500, results.size());
		// Page 0: limit=min(1000, 1500-0)=1000, offset=200+0=200
		// Page 1: limit=min(1000, 1500-1000)=500, offset=200+1000=1200
		assertEquals(List.of(1000L, 500L), capturedLimits);
		assertEquals(List.of(200L, 1200L), capturedOffsets);
	}

	@Test
	public void testGetQueryIteratorWithFilters() {
		doAnswer(invocation -> {
			return createRows(0, 100);
		}).when(manager).querySinglePage(any(GridHeader.class), any(QueryElement.class));

		Iterator<RowView> iterator = manager.getQueryIterator(mockHeader, Collections.emptyList());
		List<RowView> results = new ArrayList<>();
		while (iterator.hasNext()) {
			results.add(iterator.next());
		}

		assertEquals(100, results.size());
	}

	private List<RowView> createRows(int startIndex, int count) {
		return IntStream.range(startIndex, startIndex + count)
				.mapToObj(i -> new RowView().setRowIndex((long) i))
				.collect(Collectors.toList());
	}

	@Test
	public void testReadSelectedValuesWithMissingVectorEntry() {
		String selectedVals = "[null,{\"i\":[100,101],\"v\":[\"a\"]},null]";

		// call under test
		ConstantNode[] nodes = GridReplicaViewManagerImpl.readSelectedValues(selectedVals);

		assertEquals(3, nodes.length);
		assertNull(nodes[0]);
		assertEquals(new ConstantNode().setId(new LogicalTimestamp().setReplicaId(100L).setSequenceNumber(101L))
				.setValue(new ConValue(ConType.STRING, "a")), nodes[1]);
		assertNull(nodes[2]);
	}

	@Test
	public void testReadSelectedValuesWithNoColumns() {
		// call under test
		ConstantNode[] nodes = GridReplicaViewManagerImpl.readSelectedValues("[]");

		assertArrayEquals(new ConstantNode[0], nodes);
	}
}
