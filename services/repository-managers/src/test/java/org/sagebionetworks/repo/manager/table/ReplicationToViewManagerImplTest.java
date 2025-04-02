package org.sagebionetworks.repo.manager.table;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.table.ReplicatedEvent;
import org.sagebionetworks.repo.model.table.ReplicationType;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.table.cluster.ViewUpdateHandler;

@ExtendWith(MockitoExtension.class)
public class ReplicationToViewManagerImplTest {

	@Mock
	private TableIndexConnectionFactory mockFactory;
	@Mock
	private TableIndexManager mockTableIndexManager;
	@Mock
	private TableManagerSupport mockTableManagerSupport;
	private int viewUpdateVisibilityTimeoutSeconds = 10;

	private ReplicationToViewManagerImpl manager;

	@BeforeEach
	public void before() {
		manager = new ReplicationToViewManagerImpl(mockFactory, mockTableManagerSupport,
				viewUpdateVisibilityTimeoutSeconds);
	}

	@Test
	public void testParseJSONArray() {
		// call under test
		assertEquals(List.of(12L, 34L, 56L), ReplicationToViewManagerImpl.parseJSONArray("[12,34,56]"));
		assertEquals(Collections.emptyList(), ReplicationToViewManagerImpl.parseJSONArray("[]"));
		assertEquals(Collections.emptyList(), ReplicationToViewManagerImpl.parseJSONArray(null));
	}

	@Test
	public void testobjectReplicated() {
		ReplicatedEvent event = new ReplicatedEvent()
				.setReplicatedObjectType(ObjectType.ENTITY).setPathIds(List.of(1L,2L,3L,4L,5L));
		when(mockFactory.connectToFirstIndex()).thenReturn(mockTableIndexManager);
		when(mockTableIndexManager.getViewsIntersectionForPath(List.of(1L, 2L, 3L, 4L, 5L), ReplicationType.ENTITY))
				.thenReturn(List.of(22L, 33L).iterator());

		// call under test
		manager.objectReplicated(event);
		verify(mockTableIndexManager).setViewAsNeedsUpdate(22L, viewUpdateVisibilityTimeoutSeconds);
		verify(mockTableIndexManager).setViewAsNeedsUpdate(33L, viewUpdateVisibilityTimeoutSeconds);

	}
	
	@Test
	public void testConsumeVisibleViewUpdates() {
		when(mockFactory.connectToFirstIndex()).thenReturn(mockTableIndexManager);
		doAnswer(i->{
			ViewUpdateHandler handler =  (ViewUpdateHandler)i.getArgument(0);
			handler.handleViewUpdate(123L);
			return false;
		}).when(mockTableIndexManager).consumeFirstVisibleViewUpdate(any());
		
		// call under test
		manager.consumeVisibleViewUpdates();
		
		verify(mockTableManagerSupport).triggerIndexUpdate(IdAndVersion.parse("syn123"));
	}
	
	@Test
	public void testConsumeVisibleViewUpdatesWithMultiple() {
		when(mockFactory.connectToFirstIndex()).thenReturn(mockTableIndexManager);
		when(mockTableIndexManager.consumeFirstVisibleViewUpdate(any())).thenReturn(true,  true, true, false);
		
		// call under test
		manager.consumeVisibleViewUpdates();
		
		verify(mockTableIndexManager, times(4)).consumeFirstVisibleViewUpdate(any());
	}
	
	@Test
	public void testConsumeVisibleViewUpdatesWithInfiniteLoop() {
		when(mockFactory.connectToFirstIndex()).thenReturn(mockTableIndexManager);
		when(mockTableIndexManager.consumeFirstVisibleViewUpdate(any())).thenReturn(true);

		// call under test
		manager.consumeVisibleViewUpdates();

		verify(mockTableIndexManager, times(ReplicationToViewManagerImpl.MAX_CALLS_PER_RUN))
				.consumeFirstVisibleViewUpdate(any());
	}
	
	@Test
	public void testConsumeVisibleViewUpdatesWithNotFoundException() {
		when(mockFactory.connectToFirstIndex()).thenReturn(mockTableIndexManager);
		doAnswer(i -> {
			ViewUpdateHandler handler = (ViewUpdateHandler) i.getArgument(0);
			handler.handleViewUpdate(123L);
			return false;
		}).when(mockTableIndexManager).consumeFirstVisibleViewUpdate(any());
		doThrow(new NotFoundException("not found")).when(mockTableManagerSupport)
				.triggerIndexUpdate(IdAndVersion.parse("syn123"));
		
		// call under test
		manager.consumeVisibleViewUpdates();
		verify(mockTableManagerSupport).triggerIndexUpdate(IdAndVersion.parse("syn123"));
	}
	
	@Test
	public void testConsumeVisibleViewUpdatesWithRuntime() {
		when(mockFactory.connectToFirstIndex()).thenReturn(mockTableIndexManager);
		doAnswer(i -> {
			ViewUpdateHandler handler = (ViewUpdateHandler) i.getArgument(0);
			handler.handleViewUpdate(123L);
			return false;
		}).when(mockTableIndexManager).consumeFirstVisibleViewUpdate(any());
		doThrow(new RuntimeException("not good")).when(mockTableManagerSupport)
				.triggerIndexUpdate(IdAndVersion.parse("syn123"));
		
		// call under test
		manager.consumeVisibleViewUpdates();
		verify(mockTableManagerSupport).triggerIndexUpdate(IdAndVersion.parse("syn123"));
	}
}
