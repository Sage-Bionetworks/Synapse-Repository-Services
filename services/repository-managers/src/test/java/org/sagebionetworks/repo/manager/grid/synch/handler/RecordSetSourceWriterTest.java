package org.sagebionetworks.repo.manager.grid.synch.handler;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.InOrder;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.grid.internal.replica.export.RecordSetArtifactBuilder;
import org.sagebionetworks.repo.manager.grid.internal.replica.export.GridRecordSetExporter;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.Column;
import org.sagebionetworks.repo.manager.grid.internal.replica.validation.GridRowValidator;
import org.sagebionetworks.repo.manager.grid.synch.io.RowSourceItem;
import org.sagebionetworks.repo.model.RecordSet;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.asynch.AsyncJobProgressCallback;
import org.sagebionetworks.repo.model.grid.SyncType;
import org.sagebionetworks.repo.model.grid.patch.ConType;
import org.sagebionetworks.repo.model.grid.patch.ConValue;
import org.sagebionetworks.repo.model.table.CsvTableDescriptor;

@ExtendWith(MockitoExtension.class)
public class RecordSetSourceWriterTest {

	@Mock
	private UserInfo mockUser;
	@Mock
	private GridRecordSetExporter mockRecordSetExporter;
	@Mock
	private GridRowValidator mockGridRowValidator;
	@Mock
	private AsyncJobProgressCallback mockCallback;

	private RecordSet recordSet;
	private CsvTableDescriptor csvDescriptor;

	@BeforeEach
	public void before() {
		recordSet = new RecordSet().setId("syn1").setVersionNumber(3L);
		csvDescriptor = new CsvTableDescriptor().setIsFirstLineHeader(true);
	}

	private RecordSetSourceWriter newPullWriter(String schemaId) {
		return new RecordSetSourceWriter(mockUser, mockRecordSetExporter, mockGridRowValidator, recordSet,
				csvDescriptor, schemaId, SyncType.PULL);
	}

	private RecordSetSourceWriter newPullPushWriter(String schemaId) {
		return new RecordSetSourceWriter(mockUser, mockRecordSetExporter, mockGridRowValidator, recordSet,
				csvDescriptor, schemaId, SyncType.PULL_PUSH);
	}

	@Test
	public void testCanAddRemove() {
		RecordSetSourceWriter writer = newPullWriter(null);
		assertTrue(writer.canAddRemoveRows());
		assertTrue(writer.canAddRemoveColumns());
	}

	@Test
	public void testMutatorsAreNoOps() {
		RecordSetSourceWriter writer = newPullWriter(null);
		// call under test — none of these mutate the source or record errors (no push builder yet)
		writer.removeRow(new RowSourceItem(new TreeMap<>(), "k"));
		writer.applyCellChangesFromCopyToSource("k", Map.of());
		writer.addColumnToSource("x");
		writer.removeColumn("x");
		assertTrue(writer.getErrorMessages().isEmpty());
		verifyNoInteractions(mockRecordSetExporter);
	}

	@Test
	public void testPushLifecycleNoOpForPull() throws Exception {
		RecordSetSourceWriter writer = newPullWriter(null);

		// call under test — PULL builds no artifact and pushes nothing
		writer.beginPush(mockCallback, List.of(new Column().setName("id")));
		writer.recordFinalRowState(Map.of("id", new ConValue(ConType.STRING, "x")));
		assertEquals(Optional.empty(), writer.completePush());
		writer.close();

		verifyNoInteractions(mockRecordSetExporter);
	}

	@Test
	public void testBeginPushCreatesBuilderAndRecordFinalRowStateFeedsItForPullPush() throws Exception {
		RecordSetSourceWriter writer = newPullPushWriter("my.org-1.0.0");
		RecordSetArtifactBuilder mockBuilder = mock(RecordSetArtifactBuilder.class);
		when(mockGridRowValidator.getValidationSchema("my.org-1.0.0")).thenReturn(null);
		when(mockCallback.getJobId()).thenReturn("42");
		when(mockRecordSetExporter.createArtifactBuilder(eq("42"), eq(List.of("id", "name")), any(), eq(csvDescriptor),
				eq("syn1"))).thenReturn(mockBuilder);

		// call under test — PULL_PUSH creates the builder; each surviving row is fed to it.
		writer.beginPush(mockCallback, List.of(new Column().setName("id"), new Column().setName("name")));
		Map<String, ConValue> cells = Map.of("id", new ConValue(ConType.STRING, "x"));
		writer.recordFinalRowState(cells);

		verify(mockBuilder).addRow(cells);
	}

	@Test
	public void testAddNewRowToSourceIsAlwaysNoOp() throws Exception {
		RecordSetSourceWriter writer = newPullPushWriter(null);
		RecordSetArtifactBuilder mockBuilder = mock(RecordSetArtifactBuilder.class);
		when(mockCallback.getJobId()).thenReturn("42");
		when(mockRecordSetExporter.createArtifactBuilder(any(), any(), any(), any(), any())).thenReturn(mockBuilder);
		writer.beginPush(mockCallback, List.of(new Column().setName("id"), new Column().setName("name")));
		TreeMap<String, ConValue> cells = new TreeMap<>(
				Map.of("id", new ConValue(ConType.LONG, 1L), "name", new ConValue(ConType.STRING, "Alice")));

		// call under test — addNewRowToSource never touches the push builder; recordFinalRowState
		// (called separately by RowSyncOutcomeHandler) is the sole path that feeds rows to the builder.
		writer.addNewRowToSource(new RowSourceItem(cells, "k"));

		verifyNoInteractions(mockBuilder);
	}

	@Test
	public void testCompletePushBuildsNewVersionForPullPush() throws Exception {
		RecordSetSourceWriter writer = newPullPushWriter(null);
		RecordSetArtifactBuilder mockBuilder = mock(RecordSetArtifactBuilder.class);
		when(mockCallback.getJobId()).thenReturn("1");
		when(mockRecordSetExporter.createArtifactBuilder(any(), any(), any(), any(), any())).thenReturn(mockBuilder);
		writer.beginPush(mockCallback, List.of(new Column().setName("id")));

		RecordSet pushed = new RecordSet().setId("syn1").setVersionNumber(9L);
		when(mockRecordSetExporter.pushFromArtifactBuilder(mockUser, recordSet, mockBuilder)).thenReturn(pushed);

		// call under test
		Optional<Long> result = writer.completePush();
		assertEquals(Optional.of(9L), result);
		// completePush() finishes the builder (flush + close writers) BEFORE the exporter reads
		// its files, then leaves the builder alive for the writer's close() to reap.
		InOrder order = inOrder(mockBuilder, mockRecordSetExporter);
		order.verify(mockBuilder).finish();
		order.verify(mockRecordSetExporter).pushFromArtifactBuilder(mockUser, recordSet, mockBuilder);
		verify(mockBuilder, never()).close();
	}

	@Test
	public void testCompletePushWithUploadFailurePropagatesExceptionAndCleansUpOnClose() throws Exception {
		RecordSetSourceWriter writer = newPullPushWriter(null);
		RecordSetArtifactBuilder mockBuilder = mock(RecordSetArtifactBuilder.class);
		when(mockCallback.getJobId()).thenReturn("1");
		when(mockRecordSetExporter.createArtifactBuilder(any(), any(), any(), any(), any())).thenReturn(mockBuilder);
		writer.beginPush(mockCallback, List.of(new Column().setName("id")));

		IllegalStateException uploadFailure = new IllegalStateException("upload failed");
		when(mockRecordSetExporter.pushFromArtifactBuilder(mockUser, recordSet, mockBuilder))
				.thenThrow(uploadFailure);

		// call under test — the upload exception propagates
		IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> writer.completePush());
		assertEquals(uploadFailure, thrown);
		// the builder was finished (flush + close writers) before the failing upload, but NOT
		// closed here — deletion is deferred to the writer's close().
		verify(mockBuilder).finish();
		verify(mockBuilder, never()).close();

		// The try-with-resources close() is the safety net that closes the builder
		// (which deletes the temp files) on this path.
		writer.close();
		verify(mockBuilder).close();
	}

	@Test
	public void testCloseWithPriorSuccessfulCompletePushIsIdempotent() throws Exception {
		RecordSetSourceWriter writer = newPullPushWriter(null);
		RecordSetArtifactBuilder mockBuilder = mock(RecordSetArtifactBuilder.class);
		when(mockCallback.getJobId()).thenReturn("1");
		when(mockRecordSetExporter.createArtifactBuilder(any(), any(), any(), any(), any())).thenReturn(mockBuilder);
		writer.beginPush(mockCallback, List.of(new Column().setName("id")));

		RecordSet pushed = new RecordSet().setId("syn1").setVersionNumber(9L);
		when(mockRecordSetExporter.pushFromArtifactBuilder(mockUser, recordSet, mockBuilder)).thenReturn(pushed);

		writer.completePush();

		// call under test — close() after a successful completePush() closes the builder
		// exactly once
		writer.close();

		verify(mockBuilder, times(1)).close();
	}

	@Test
	public void testCloseReleasesArtifactBuilderResources() throws Exception {
		RecordSetSourceWriter writer = newPullPushWriter(null);
		RecordSetArtifactBuilder mockBuilder = mock(RecordSetArtifactBuilder.class);
		when(mockCallback.getJobId()).thenReturn("1");
		when(mockRecordSetExporter.createArtifactBuilder(any(), any(), any(), any(), any())).thenReturn(mockBuilder);
		writer.beginPush(mockCallback, List.of(new Column().setName("id")));

		// call under test — close without completePush (simulates an exception path)
		writer.close();

		verify(mockBuilder).close();
	}

	@Test
	public void testCloseLogsAndDoesNotThrowWhenBuilderCloseFails() throws Exception {
		RecordSetSourceWriter writer = newPullPushWriter(null);
		RecordSetArtifactBuilder mockBuilder = mock(RecordSetArtifactBuilder.class);
		when(mockCallback.getJobId()).thenReturn("1");
		when(mockRecordSetExporter.createArtifactBuilder(any(), any(), any(), any(), any())).thenReturn(mockBuilder);
		writer.beginPush(mockCallback, List.of(new Column().setName("id")));
		doThrow(new IllegalStateException("close failed")).when(mockBuilder).close();

		// call under test — a failure closing the builder during cleanup is swallowed
		// (logged, best-effort) rather than propagated.
		assertDoesNotThrow(writer::close);

		verify(mockBuilder).close();
	}

}
