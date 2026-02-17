package org.sagebionetworks.repo.manager.grid.synch;

import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

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
import org.sagebionetworks.repo.manager.grid.synch.io.RowReader;
import org.sagebionetworks.repo.manager.grid.synch.row.RowCopy;
import org.sagebionetworks.repo.manager.grid.synch.row.RowMerge;
import org.sagebionetworks.repo.manager.grid.synch.row.RowSource;
import org.sagebionetworks.repo.manager.grid.synch.schema.SchemaCopy;
import org.sagebionetworks.repo.manager.grid.synch.schema.SchemaSource;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.asynch.AsyncJobProgressCallback;
import org.sagebionetworks.repo.model.dbo.grid.GridSource;
import org.sagebionetworks.repo.model.grid.GridConnectionInfo;
import org.sagebionetworks.repo.model.grid.GridSession;
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
	private RowReader mockSourceReader;
	@Mock
	private SchemaCopy mockSchemaCopy;
	@Mock
	private SchemaSource mockSchemaSource;
	@Mock
	private RowCopy mockRowCopy;
	@Mock
	private RowSource mockRowSource;
	@Mock
	private RowMerge mockRowMerge;
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

	@Test
	public void testSynchronizeCopyWithSource() throws Exception {
		when(mockGridManager.getGridSession(mockUser, gridSessionId)).thenReturn(gridSession);
		when(mockCopyHandlerProvider.createCopyHandler(gridSession)).thenReturn(mockCopyHandler);
		when(mockCopyHandler.getGridSource()).thenReturn(gridSource);
		when(mockSourceHandlerProvdier.createNewProvider(mockCallback, mockUser, gridSession, gridSource))
				.thenReturn(mockSourceHandler);
		when(mockSourceHandler.getSourceRowReader()).thenReturn(mockSourceReader);
		when(mockSynchronizeProvider.getSchemaCopy(mockIntendedChangePublisher, mockCopyHandler))
				.thenReturn(mockSchemaCopy);
		when(mockSchemaCopy.getFinalSchema()).thenReturn(finalSchema);
		when(mockSynchronizeProvider.getSchemaSource(mockSourceHandler)).thenReturn(mockSchemaSource);
		when(mockSynchronizeProvider.getRowCopy(mockIntendedChangePublisher, finalSchema, mockCopyHandler))
				.thenReturn(mockRowCopy);
		when(mockSynchronizeProvider.getRowSource(mockSourceReader, mockSourceHandler)).thenReturn(mockRowSource);
		when(mockSynchronizeProvider.getRowMerge(mockLogic, mockIntendedChangePublisher, finalSchema, mockCopyHandler,
				mockSourceHandler)).thenReturn(mockRowMerge);

		when(mockSourceHandler.getErrorMessages()).thenReturn(List.of("errorOne", "errorTwo"));

		doReturn(mockIntendedChangePublisher).when(manager).newIntendedChangePublisher(mockCopyHandler);

		// call under test
		SynchronizeGridResponse response = manager.synchronizeCopyWithSource(mockCallback, mockUser, request);
		assertEquals(new SynchronizeGridResponse().setGridSessionId(request.getGridSessionId())
				.setErrorMessages(List.of("errorOne", "errorTwo")), response);

		verify(mockLogic).synchronize(eq(mockSchemaCopy), eq(mockSchemaSource), any());
		verify(mockLogic).synchronize(mockRowCopy, mockRowSource, mockRowMerge);

		verify(mockCopyHandler).close();
		verify(mockSourceHandler).close();
		verify(mockSourceReader).close();
		verify(mockIntendedChangePublisher).close();
		verify(mockSchemaCopy).close();
		verifyNoMoreInteractions(mockCopyHandlerProvider, mockCopyHandler, mockSourceHandler, mockSourceHandlerProvdier,
				mockSourceReader, mockPatchBuilderPublisher, mockSynchronizeProvider, mockSchemaCopy, mockSchemaSource,
				mockRowCopy, mockRowSource, mockRowMerge, mockIntendedChangePublisher, mockLogic);
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
