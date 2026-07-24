package org.sagebionetworks.repo.manager.grid.synch;

import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.grid.GridManager;
import org.sagebionetworks.repo.manager.grid.internal.replica.change.IntendedChangePublisher;
import org.sagebionetworks.repo.manager.grid.internal.replica.change.PatchBuilderPublisher;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.Column;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.GridHeader;
import org.sagebionetworks.repo.manager.grid.synch.core.SynchronizationLogic;
import org.sagebionetworks.repo.manager.grid.synch.handler.CopyHandler;
import org.sagebionetworks.repo.manager.grid.synch.handler.CopyHandlerProvider;
import org.sagebionetworks.repo.manager.grid.synch.handler.SourceHandler;
import org.sagebionetworks.repo.manager.grid.synch.handler.SourceHandlerProvider;
import org.sagebionetworks.repo.manager.grid.synch.handler.SourceWriter;
import org.sagebionetworks.repo.manager.grid.synch.io.RowSourceItemReader;
import org.sagebionetworks.repo.manager.grid.synch.row.RowSourceReader;
import org.sagebionetworks.repo.manager.grid.synch.row.RowSyncOutcomeHandler;
import org.sagebionetworks.repo.manager.grid.synch.row.RowSyncRules;
import org.sagebionetworks.repo.manager.grid.synch.schema.SchemaSourceReader;
import org.sagebionetworks.repo.manager.grid.synch.schema.SchemaSyncOutcomeHandler;
import org.sagebionetworks.repo.manager.grid.synch.schema.SchemaSyncRules;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.asynch.AsyncJobProgressCallback;
import org.sagebionetworks.repo.model.dbo.grid.GridSource;
import org.sagebionetworks.repo.model.grid.GridConnectionInfo;
import org.sagebionetworks.repo.model.grid.GridSession;
import org.sagebionetworks.repo.model.grid.SyncType;
import org.sagebionetworks.repo.model.grid.SynchronizeGridRequest;
import org.sagebionetworks.repo.model.grid.SynchronizeGridResponse;

@ExtendWith(MockitoExtension.class)
public class GridSynchronizationManagerImplTest {

	@Mock
	private GridManager mockGridManager;
	@Mock
	private PatchBuilderPublisher mockPatchBuilderPublisher;
	@Mock
	private SourceHandlerProvider mockSourceHandlerProvdier;
	@Mock
	private CopyHandlerProvider mockCopyHandlerProvider;
	@Mock
	private SynchronizationLogic mockLogic;
	@Mock
	private SynchronizeProvider mockSynchronizeProvider;
	@Mock
	private CopyHandler mockCopyHandler;
	@Mock
	private SourceHandler mockSourceHandler;
	@Mock
	private SourceWriter mockSourceWriter;
	@Mock
	private RowSourceItemReader mockSourceReader;
	@Mock
	private SchemaSyncOutcomeHandler mockSchemaHandler;
	@Mock
	private SchemaSourceReader mockSchemaReader;
	@Mock
	private SchemaSyncRules mockSchemaRules;
	@Mock
	private RowSourceReader mockRowReader;
	@Mock
	private RowSyncRules mockRowRules;
	@Mock
	private RowSyncOutcomeHandler mockRowHandler;
	@Mock
	private IntendedChangePublisher mockIntendedChangePublisher;
	@Mock
	private AsyncJobProgressCallback mockCallback;
	@Mock
	private UserInfo mockUser;
	@Mock
	private GridHeader mockHeader;
	@Mock
	private GridConnectionInfo mockConnection;

	@Spy
	@InjectMocks
	private GridSynchronizationManagerImpl manager;

	private String gridSessionId;
	private GridSession gridSession;
	private GridSource gridSource;
	private List<Column> finalSchema;
	private SynchronizeGridRequest request;

	@BeforeEach
	public void before() {
		gridSessionId = "123";
		request = new SynchronizeGridRequest().setGridSessionId(gridSessionId);
		gridSession = new GridSession().setSessionId(gridSessionId);
		gridSource = new GridSource(333L, EntityType.entityview);
		finalSchema = List.of(new Column().setName("foo"));
	}

	private void createStubs(GridSource source, boolean preserveUserAttribution) throws Exception {
		when(mockGridManager.getGridSession(mockUser, gridSessionId)).thenReturn(gridSession);
		when(mockCopyHandlerProvider.createCopyHandler(gridSession)).thenReturn(mockCopyHandler);
		when(mockCopyHandler.getGridSource()).thenReturn(source);
		when(mockSourceHandlerProvdier.createNewHandler(mockCallback, mockUser, gridSession, source))
				.thenReturn(mockSourceHandler);
		when(mockSourceHandler.createSourceWriter(any(SyncType.class))).thenReturn(mockSourceWriter);
		when(mockSourceHandler.getSourceRowReader()).thenReturn(mockSourceReader);

		when(mockSynchronizeProvider.getSchemaSyncOutcomeHandler(mockIntendedChangePublisher, mockCopyHandler,
				mockSourceWriter)).thenReturn(mockSchemaHandler);
		when(mockSchemaHandler.streamCopyItems()).thenReturn(Stream.of());
		when(mockSchemaHandler.getFinalSchema()).thenReturn(finalSchema);
		when(mockSynchronizeProvider.getSchemaSourceReader(mockSourceHandler)).thenReturn(mockSchemaReader);
		when(mockSynchronizeProvider.getSchemaSyncRules(mockSourceHandler)).thenReturn(mockSchemaRules);

		when(mockSynchronizeProvider.getRowSourceReader(mockSourceReader)).thenReturn(mockRowReader);
		when(mockSynchronizeProvider.getRowSyncRules(mockSourceHandler)).thenReturn(mockRowRules);
		when(mockSynchronizeProvider.getRowSyncOutcomeHandler(mockLogic, mockIntendedChangePublisher, finalSchema, mockCopyHandler,
				mockSourceWriter, preserveUserAttribution)).thenReturn(mockRowHandler);
		when(mockRowHandler.streamCopyItems()).thenReturn(Stream.of());

		doReturn(mockIntendedChangePublisher).when(manager).newIntendedChangePublisher(mockCopyHandler);
	}

	@Test
	public void testSynchronizeCopyWithSource() throws Exception {
		createStubs(gridSource, false);

		Set<Long> benefactorIds = Set.of(111L, 222L);
		when(mockSourceWriter.getErrorMessages()).thenReturn(List.of("errorOne", "errorTwo"));
		when(mockSourceHandler.getBenefactorIds()).thenReturn(benefactorIds);
		when(mockSourceHandler.getSourceVersion()).thenReturn(Optional.of(5L));
		when(mockSourceHandler.getSourceSchema$Id()).thenReturn(Optional.of("my.org-Schema-1.0.0"));
		when(mockSourceWriter.completePush()).thenReturn(Optional.empty());

		// call under test
		SynchronizeGridResponse response = manager.synchronizeCopyWithSource(mockCallback, mockUser, request);
		assertEquals(new SynchronizeGridResponse().setGridSessionId(request.getGridSessionId())
				.setErrorMessages(List.of("errorOne", "errorTwo")), response);

		// null syncType defaults to PULL_PUSH and is validated against the source
		verify(mockSourceHandler).validateSyncType(SyncType.PULL_PUSH);
		// the writer prepares any push artifact keyed to the final schema
		verify(mockSourceWriter).beginPush(mockCallback, finalSchema);
		verify(mockLogic).synchronize(any(), eq(mockSchemaReader), eq(mockSchemaRules), eq(mockSchemaHandler));
		verify(mockLogic).synchronize(any(), eq(mockRowReader), eq(mockRowRules), eq(mockRowHandler));
		verify(mockGridManager).updateSessionBenefactorIds(gridSessionId, benefactorIds);
		verify(mockGridManager).updateSourceEntityVersion(gridSessionId, 5L);
		verify(mockGridManager).updateSessionSchemaId(gridSessionId, "my.org-Schema-1.0.0");

		verify(mockCopyHandler).close();
		verify(mockSourceHandler).close();
		verify(mockSourceWriter).close();
		verify(mockSourceReader).close();
		verify(mockIntendedChangePublisher).close();
		verify(mockSchemaHandler).close();
	}

	@Test
	public void testSynchronizeCopyWithSourcePullIsRejected() throws Exception {
		request.setSyncType(SyncType.PULL);
		String expectedError = "PULL is not supported";
		when(mockGridManager.getGridSession(mockUser, gridSessionId)).thenReturn(gridSession);
		when(mockCopyHandlerProvider.createCopyHandler(gridSession)).thenReturn(mockCopyHandler);
		when(mockCopyHandler.getGridSource()).thenReturn(gridSource);
		when(mockSourceHandlerProvdier.createNewHandler(mockCallback, mockUser, gridSession, gridSource))
				.thenReturn(mockSourceHandler);
		when(mockSourceHandler.createSourceWriter(SyncType.PULL)).thenReturn(mockSourceWriter);
		when(mockSourceHandler.getSourceRowReader()).thenReturn(mockSourceReader);
		doThrow(new IllegalArgumentException(expectedError)).when(mockSourceHandler).validateSyncType(SyncType.PULL);
		doReturn(mockIntendedChangePublisher).when(manager).newIntendedChangePublisher(mockCopyHandler);

		String message = Assertions.assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.synchronizeCopyWithSource(mockCallback, mockUser, request);
		}).getMessage();
		assertEquals(expectedError, message);

		// No merge should occur and resources are still closed.
		verify(mockCopyHandler).close();
		verify(mockSourceHandler).close();
		verify(mockSourceWriter).close();
		verify(mockSourceReader).close();
		verify(mockIntendedChangePublisher).close();
	}

	@Test
	public void testSynchronizeCopyWithSourcePullPushBuildsNewVersion() throws Exception {
		GridSource recordSetSource = new GridSource(444L, EntityType.recordset);
		request.setSyncType(SyncType.PULL_PUSH);
		createStubs(recordSetSource, false);

		when(mockSourceHandler.getSourceVersion()).thenReturn(Optional.of(7L));
		when(mockSourceWriter.completePush()).thenReturn(Optional.of(8L));

		// call under test
		manager.synchronizeCopyWithSource(mockCallback, mockUser, request);

		// the writer prepares the push artifact, then flushes it to a new version
		verify(mockSourceWriter).beginPush(mockCallback, finalSchema);
		verify(mockSourceWriter).completePush();
		// the new pushed version becomes the synced baseline (overrides the 7L from getSourceVersion)
		verify(mockGridManager).updateSourceEntityVersion(gridSessionId, 8L);
		verify(mockSourceHandler).close();
		verify(mockSourceWriter).close();
	}

	@Test
	public void testSynchronizeCopyWithSourceRecordSetPullPreservesUserAttribution() throws Exception {
		GridSource recordSetSource = new GridSource(444L, EntityType.recordset);
		request.setSyncType(SyncType.PULL);
		// PULL: the row outcome handler is built with preserveUserAttribution=true
		createStubs(recordSetSource, true);

		when(mockSourceHandler.getSourceVersion()).thenReturn(Optional.of(5L));
		when(mockSourceWriter.completePush()).thenReturn(Optional.empty());

		// call under test
		manager.synchronizeCopyWithSource(mockCallback, mockUser, request);

		// PULL builds the row handler with preserveUserAttribution=true
		verify(mockSynchronizeProvider).getRowSyncOutcomeHandler(mockLogic, mockIntendedChangePublisher, finalSchema, mockCopyHandler,
				mockSourceWriter, true);
		verify(mockSourceWriter).completePush();
		// PULL: no new version is pushed; the synced source revision is still recorded
		verify(mockGridManager).updateSourceEntityVersion(gridSessionId, 5L);
		verify(mockSourceHandler).close();
	}

	@Test
	public void testNewIntendedChangePublisher() {
		when(mockCopyHandler.getHeader()).thenReturn(mockHeader);
		when(mockCopyHandler.getConnectionInfo()).thenReturn(mockConnection);
		// call under test
		IntendedChangePublisher icp = manager.newIntendedChangePublisher(mockCopyHandler);
		assertNotNull(icp);
	}

}
