package org.sagebionetworks.table.worker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.message.RepositoryMessagePublisher;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.dbo.dao.table.DefiningSqlDependencyDao;
import org.sagebionetworks.repo.model.dbo.dao.table.DefiningSqlDependencyDao.DependentObject;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.message.LocalStackChangeMesssage;
import org.sagebionetworks.repo.model.message.LocalStackMessage;
import org.sagebionetworks.repo.model.table.TableState;
import org.sagebionetworks.repo.model.table.TableStatusChangeEvent;
import org.sagebionetworks.util.progress.ProgressCallback;

import com.amazonaws.services.sqs.model.Message;

@ExtendWith(MockitoExtension.class)
public class DefiningSqlSourceUpdateWorkerTest {

	@Mock
	private DefiningSqlDependencyDao mockDefiningSqlDependencyDao;

	@Mock
	private RepositoryMessagePublisher mockRepositoryMessagePublisher;

	@InjectMocks
	private DefiningSqlSourceUpdateWorker worker;

	@Mock
	private ProgressCallback mockCallBack;

	@Mock
	private TableStatusChangeEvent mockEvent;

	@Mock
	private Message mockMessage;

	@Captor
	private ArgumentCaptor<LocalStackMessage> messageCaptor;

	@Test
	public void testRun() throws Exception {
		IdAndVersion sourceTableId = IdAndVersion.parse("123");

		when(mockEvent.getObjectType()).thenReturn(ObjectType.TABLE_STATUS_EVENT);
		when(mockEvent.getObjectId()).thenReturn("syn123");
		when(mockEvent.getObjectVersion()).thenReturn(null);
		when(mockEvent.getState()).thenReturn(TableState.AVAILABLE);
		// A materialized view and a search index both depend on the source. The second page is empty
		// because the PaginationIterator performs an additional call to check whether there are more results.
		when(mockDefiningSqlDependencyDao.getDependentsPage(eq(sourceTableId), anyLong(), anyLong())).thenReturn(
				Arrays.asList(
						new DependentObject(IdAndVersion.parse("456"), ObjectType.MATERIALIZED_VIEW.name()),
						new DependentObject(IdAndVersion.parse("789.2"), ObjectType.SEARCH_INDEX.name())),
				Collections.emptyList());

		// Call under test
		worker.run(mockCallBack, mockMessage, mockEvent);

		verify(mockRepositoryMessagePublisher, times(2)).publishLocalStackMessageToTopic(
				eq(ObjectType.SOURCE_DEPENDENCY_EVENT), messageCaptor.capture());
		List<LocalStackMessage> published = messageCaptor.getAllValues();
		// The worker stamps a non-null timestamp on each message.
		assertNotNull(published.get(0).getTimestamp());
		assertNotNull(published.get(1).getTimestamp());
		assertEquals(newMessage("syn456", null, ObjectType.MATERIALIZED_VIEW), clearTimestamp(published.get(0)));
		assertEquals(newMessage("syn789", 2L, ObjectType.SEARCH_INDEX), clearTimestamp(published.get(1)));
	}

	@Test
	public void testRunWithVersion() throws Exception {
		IdAndVersion sourceTableId = IdAndVersion.parse("123.2");

		when(mockEvent.getObjectType()).thenReturn(ObjectType.TABLE_STATUS_EVENT);
		when(mockEvent.getObjectId()).thenReturn("syn123");
		when(mockEvent.getObjectVersion()).thenReturn(2L);
		when(mockEvent.getState()).thenReturn(TableState.AVAILABLE);
		when(mockDefiningSqlDependencyDao.getDependentsPage(eq(sourceTableId), anyLong(), anyLong())).thenReturn(
				Arrays.asList(new DependentObject(IdAndVersion.parse("456"), ObjectType.MATERIALIZED_VIEW.name())),
				Collections.emptyList());

		// Call under test
		worker.run(mockCallBack, mockMessage, mockEvent);

		verify(mockRepositoryMessagePublisher).publishLocalStackMessageToTopic(
				eq(ObjectType.SOURCE_DEPENDENCY_EVENT), messageCaptor.capture());
		assertEquals(newMessage("syn456", null, ObjectType.MATERIALIZED_VIEW), clearTimestamp(messageCaptor.getValue()));
	}

	@Test
	public void testRunWithNoDependents() throws Exception {
		IdAndVersion sourceTableId = IdAndVersion.parse("123");

		when(mockEvent.getObjectType()).thenReturn(ObjectType.TABLE_STATUS_EVENT);
		when(mockEvent.getObjectId()).thenReturn("syn123");
		when(mockEvent.getObjectVersion()).thenReturn(null);
		when(mockEvent.getState()).thenReturn(TableState.AVAILABLE);
		when(mockDefiningSqlDependencyDao.getDependentsPage(eq(sourceTableId), anyLong(), anyLong()))
				.thenReturn(Collections.emptyList());

		// Call under test
		worker.run(mockCallBack, mockMessage, mockEvent);

		verifyNoInteractions(mockRepositoryMessagePublisher);
	}

	@Test
	public void testRunWithProcessingState() throws Exception {
		when(mockEvent.getObjectType()).thenReturn(ObjectType.TABLE_STATUS_EVENT);
		when(mockEvent.getState()).thenReturn(TableState.PROCESSING);

		// Call under test
		worker.run(mockCallBack, mockMessage, mockEvent);

		verifyNoInteractions(mockDefiningSqlDependencyDao);
		verifyNoInteractions(mockRepositoryMessagePublisher);
	}

	@Test
	public void testRunWithFailedState() throws Exception {
		when(mockEvent.getObjectType()).thenReturn(ObjectType.TABLE_STATUS_EVENT);
		when(mockEvent.getState()).thenReturn(TableState.PROCESSING_FAILED);

		// Call under test
		worker.run(mockCallBack, mockMessage, mockEvent);

		verifyNoInteractions(mockDefiningSqlDependencyDao);
		verifyNoInteractions(mockRepositoryMessagePublisher);
	}

	@Test
	public void testRunWithWrongObjectType() throws Exception {
		when(mockEvent.getObjectType()).thenReturn(ObjectType.ENTITY);

		String message = assertThrows(IllegalStateException.class, () -> {
			// Call under test
			worker.run(mockCallBack, mockMessage, mockEvent);
		}).getMessage();

		assertEquals("Unsupported object type: expected TABLE_STATUS_EVENT, got ENTITY", message);

		verifyNoInteractions(mockDefiningSqlDependencyDao);
		verifyNoInteractions(mockRepositoryMessagePublisher);
	}

	private static LocalStackChangeMesssage newMessage(String objectId, Long objectVersion, ObjectType objectType) {
		return new LocalStackChangeMesssage()
				.setObjectId(objectId)
				.setObjectVersion(objectVersion)
				.setObjectType(objectType)
				.setChangeType(ChangeType.UPDATE);
	}

	private static LocalStackMessage clearTimestamp(LocalStackMessage message) {
		return message.setTimestamp(null);
	}

}
