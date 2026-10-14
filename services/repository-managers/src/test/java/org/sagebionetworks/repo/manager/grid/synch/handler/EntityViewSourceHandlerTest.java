package org.sagebionetworks.repo.manager.grid.synch.handler;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.grid.GridAuthorizationManager;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.SynapseRow;
import org.sagebionetworks.repo.manager.grid.synch.io.RowSourceItem;
import org.sagebionetworks.repo.manager.grid.synch.io.RowSourceItemReader;
import org.sagebionetworks.repo.manager.grid.synch.io.RowSourceItemReference;
import org.sagebionetworks.repo.manager.grid.synch.row.RowCopyItemImpl;
import org.sagebionetworks.repo.manager.schema.AnnotationsTranslator;
import org.sagebionetworks.repo.manager.schema.JsonSchemaManager;
import org.sagebionetworks.repo.manager.table.RowHandlerProvider;
import org.sagebionetworks.repo.manager.table.TableQueryManager;
import org.sagebionetworks.repo.manager.table.query.MainQuery;
import org.sagebionetworks.repo.manager.table.query.QueryTranslations;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsValue;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsValueType;
import org.sagebionetworks.repo.model.dao.asynch.AsyncJobProgressCallback;
import org.sagebionetworks.repo.model.dao.table.RowHandler;
import org.sagebionetworks.repo.model.grid.GridSession;
import org.sagebionetworks.repo.model.grid.SyncType;
import org.sagebionetworks.repo.model.grid.patch.ConType;
import org.sagebionetworks.repo.model.grid.patch.ConValue;
import org.sagebionetworks.repo.model.schema.JsonSchema;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.Query;
import org.sagebionetworks.repo.model.table.QueryResultBundle;
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.table.cluster.QueryTranslator;
import org.sagebionetworks.util.FileProvider;

@ExtendWith(MockitoExtension.class)
public class EntityViewSourceHandlerTest {

	@Mock
	private AsyncJobProgressCallback mockCallback;
	@Mock
	private UserInfo mockUser;
	@Mock
	private UserInfo mockSessionOwner;
	@Mock
	private TableQueryManager mockTableQueryManager;
	@Mock
	private GridAuthorizationManager mockGridAuthorizationManager;
	@Mock
	private FileProvider mockFileProvider;
	@Mock
	private AnnotationWriter mockAnnotationWriter;
	@Mock
	private JsonSchemaManager mockJsonSchemaManager;
	@Mock
	private AnnotationsTranslator mockAnnotationsTranslator;
	@Mock
	private JsonSchema mockJsonSchema;
	@Mock
	private QueryTranslations mockQueryTranslations;
	@Mock
	private MainQuery mockMainQuery;
	@Mock
	private QueryTranslator mockQueryTranslator;

	private GridSession session;

	@BeforeEach
	public void before() {
		session = new GridSession().setGridJsonSchema$Id("schema.org").setSourceEntityId("syn123").setSessionId("111");
	}

	private EntityViewSourceHandler setupHandler(GridSession session, List<String> requiredColumns,
			List<ColumnModel> schemaOfSelect, List<Row> rows) throws Exception {
		if (session.getGridJsonSchema$Id() != null) {
			when(mockJsonSchema.getRequired()).thenReturn(requiredColumns);
			when(mockJsonSchemaManager.getValidationSchema(session.getGridJsonSchema$Id())).thenReturn(mockJsonSchema);
		}
		doAnswer((InvocationOnMock invocation) -> {
			return File.createTempFile((String) invocation.getArgument(0), (String) invocation.getArgument(1));
		}).when(mockFileProvider).createTempFile("Source-" + session.getSourceEntityId(), ".bin");

		doAnswer((InvocationOnMock invocation) -> {
			return new BufferedOutputStream(new FileOutputStream((File) invocation.getArgument(0)));
		}).when(mockFileProvider).createFileOutputStream(any());

		doAnswer((InvocationOnMock invocation) -> {
			return new RandomAccessFile((File) invocation.getArgument(0), (String) invocation.getArgument(1));
		}).when(mockFileProvider).createRandomAccessFile(any(), any());

		when(mockGridAuthorizationManager.getRowLevelFilterUserInfo(mockUser, session.getSessionId()))
				.thenReturn(mockSessionOwner);

		doAnswer((InvocationOnMock invocation) -> {
			RowHandlerProvider rhp = invocation.getArgument(3);
			when(mockQueryTranslations.getMainQuery()).thenReturn(mockMainQuery);
			when(mockMainQuery.getTranslator()).thenReturn(mockQueryTranslator);
			when(mockQueryTranslator.getSchemaOfSelect()).thenReturn(schemaOfSelect);
			try (RowHandler rowHandler = rhp.getHandler(mockQueryTranslations);) {
				rows.forEach(r -> rowHandler.nextRow(r));
			}

			return new QueryResultBundle();
		}).when(mockTableQueryManager).runQueryAsStream(eq(mockCallback), eq(mockSessionOwner),
				eq(new Query().setSql("select * from " + session.getSourceEntityId())), any(), eq(ACCESS_TYPE.READ),
				eq(ACCESS_TYPE.UPDATE));

		return new EntityViewSourceHandler(mockCallback, mockUser, session, mockTableQueryManager,
				mockGridAuthorizationManager, mockFileProvider, mockAnnotationWriter, mockJsonSchemaManager,
				mockAnnotationsTranslator);

	}

	@Test
	public void testGetRowReader() throws Exception {
		List<String> requiredColumns = List.of("anInt");

		List<ColumnModel> schema = List.of(
				// aString
				new ColumnModel().setColumnType(ColumnType.STRING).setName("aString"),
				// anInt
				new ColumnModel().setColumnType(ColumnType.INTEGER).setName("anInt"));

		List<Row> rows = List.of(
				// 1
				new Row().setRowId(1L).setVersionNumber(8L).setEtag("e1").setValues(Arrays.asList("a", "111")),
				// 2
				new Row().setRowId(2L).setVersionNumber(3L).setEtag("e2").setValues(Arrays.asList("b", "222")),
				// 3
				new Row().setRowId(3L).setValues(Arrays.asList("c", null)),
				// 4
				new Row().setRowId(4L).setValues(Arrays.asList(null, "333")),
				// 5
				new Row().setRowId(5L).setValues(Arrays.asList(null, null)));

		try (EntityViewSourceHandler handler = setupHandler(session, requiredColumns, schema, rows);
				// call under test
				RowSourceItemReader rowReader = handler.getSourceRowReader()) {

			List<RowSourceItem> results = StreamSupport
					.stream(Spliterators.spliteratorUnknownSize(rowReader.remainingRows(), Spliterator.ORDERED), false)
					.map(RowSourceItemReference::fetchRow).collect(Collectors.toList());

			assertEquals(List.of(
					// 1
					new RowSourceItem(
							new TreeMap<>(Map.of("aString", new ConValue(ConType.STRING, "a"), "anInt",
									new ConValue(ConType.LONG, 111L))),
							"syn1", new SynapseRow().setRowId(1L).setVersionNumber(8L).setEtag("e1")),
					// 2
					new RowSourceItem(
							new TreeMap<>(Map.of("aString", new ConValue(ConType.STRING, "b"), "anInt",
									new ConValue(ConType.LONG, 222L))),
							"syn2", new SynapseRow().setRowId(2L).setVersionNumber(3L).setEtag("e2")),
					// 3
					new RowSourceItem(new TreeMap<>(Map.of("aString", new ConValue(ConType.STRING, "c"), "anInt",
							new ConValue(ConType.NULL, null))), "syn3", new SynapseRow().setRowId(3L)),
					// 4
					new RowSourceItem(new TreeMap<>(Map.of("aString", new ConValue(ConType.UNDEFINED, null), "anInt",
							new ConValue(ConType.LONG, 333L))), "syn4", new SynapseRow().setRowId(4L)),
					// 5
					new RowSourceItem(new TreeMap<>(Map.of("aString", new ConValue(ConType.UNDEFINED, null), "anInt",
							new ConValue(ConType.NULL, null))), "syn5", new SynapseRow().setRowId(5L))

			), results);
		}
		verifyNoMoreInteractionsOnAllMocks();
	}

	private void verifyNoMoreInteractionsOnAllMocks() {
		verifyNoMoreInteractions(mockAnnotationsTranslator, mockAnnotationsTranslator, mockCallback, mockFileProvider,
				mockGridAuthorizationManager, mockJsonSchema, mockJsonSchemaManager, mockMainQuery,
				mockQueryTranslations, mockSessionOwner, mockTableQueryManager, mockUser);
	}

	@Test
	public void testGetRowReaderWithNullJsonSchema() throws Exception {
		List<String> requiredColumns = null;

		session.setGridJsonSchema$Id(null);

		List<ColumnModel> schema = List.of(
				// aString
				new ColumnModel().setColumnType(ColumnType.STRING).setName("aString"),
				// anInt
				new ColumnModel().setColumnType(ColumnType.INTEGER).setName("anInt"));

		List<Row> rows = List.of(
				// 3
				new Row().setRowId(3L).setVersionNumber(0L).setEtag("e1").setValues(Arrays.asList("c", null)),
				// 5
				new Row().setRowId(5L).setValues(Arrays.asList(null, null)));

		try (EntityViewSourceHandler handler = setupHandler(session, requiredColumns, schema, rows);
				// call under test
				RowSourceItemReader rowReader = handler.getSourceRowReader()) {

			List<RowSourceItem> results = StreamSupport
					.stream(Spliterators.spliteratorUnknownSize(rowReader.remainingRows(), Spliterator.ORDERED), false)
					.map(RowSourceItemReference::fetchRow).collect(Collectors.toList());

			assertEquals(List.of(
					// 3
					new RowSourceItem(
							new TreeMap<>(Map.of("aString", new ConValue(ConType.STRING, "c"), "anInt",
									new ConValue(ConType.UNDEFINED, null))),
							"syn3", new SynapseRow().setRowId(3L).setVersionNumber(0L).setEtag("e1")),
					// 5
					new RowSourceItem(new TreeMap<>(Map.of("aString", new ConValue(ConType.UNDEFINED, null), "anInt",
							new ConValue(ConType.UNDEFINED, null))), "syn5", new SynapseRow().setRowId(5L))

			), results);
		}
		verifyNoMoreInteractionsOnAllMocks();
	}

	@Test
	public void testGetRowReaderWithNullJsonSchemaRequired() throws Exception {
		List<String> requiredColumns = null;
		when(mockJsonSchema.getRequired()).thenReturn(null);

		List<ColumnModel> schema = List.of(
				// aString
				new ColumnModel().setColumnType(ColumnType.STRING).setName("aString"),
				// anInt
				new ColumnModel().setColumnType(ColumnType.INTEGER).setName("anInt"));

		List<Row> rows = List.of(
				// 3
				new Row().setRowId(3L).setVersionNumber(0L).setEtag("e1").setValues(Arrays.asList("c", null)),
				// 5
				new Row().setRowId(5L).setValues(Arrays.asList(null, null)));

		try (EntityViewSourceHandler handler = setupHandler(session, requiredColumns, schema, rows);
			 // call under test
			 RowSourceItemReader rowReader = handler.getSourceRowReader()) {

			List<RowSourceItem> results = StreamSupport
					.stream(Spliterators.spliteratorUnknownSize(rowReader.remainingRows(), Spliterator.ORDERED), false)
					.map(RowSourceItemReference::fetchRow).collect(Collectors.toList());

			assertEquals(List.of(
					// 3
					new RowSourceItem(
							new TreeMap<>(Map.of("aString", new ConValue(ConType.STRING, "c"), "anInt",
									new ConValue(ConType.UNDEFINED, null))),
							"syn3", new SynapseRow().setRowId(3L).setVersionNumber(0L).setEtag("e1")),
					// 5
					new RowSourceItem(new TreeMap<>(Map.of("aString", new ConValue(ConType.UNDEFINED, null), "anInt",
							new ConValue(ConType.UNDEFINED, null))), "syn5", new SynapseRow().setRowId(5L))

			), results);
		}
		verifyNoMoreInteractionsOnAllMocks();
	}

	@Test
	public void testGetRowReaderWithEmptyJsonSchemaRequired() throws Exception {
		List<String> requiredColumns = null;
		when(mockJsonSchema.getRequired()).thenReturn(Collections.emptyList());

		List<ColumnModel> schema = List.of(
				// aString
				new ColumnModel().setColumnType(ColumnType.STRING).setName("aString"),
				// anInt
				new ColumnModel().setColumnType(ColumnType.INTEGER).setName("anInt"));

		List<Row> rows = List.of(
				// 3
				new Row().setRowId(3L).setVersionNumber(0L).setEtag("e1").setValues(Arrays.asList("c", null)),
				// 5
				new Row().setRowId(5L).setValues(Arrays.asList(null, null)));

		try (EntityViewSourceHandler handler = setupHandler(session, requiredColumns, schema, rows);
			 // call under test
			 RowSourceItemReader rowReader = handler.getSourceRowReader()) {

			List<RowSourceItem> results = StreamSupport
					.stream(Spliterators.spliteratorUnknownSize(rowReader.remainingRows(), Spliterator.ORDERED), false)
					.map(RowSourceItemReference::fetchRow).collect(Collectors.toList());

			assertEquals(List.of(
					// 3
					new RowSourceItem(
							new TreeMap<>(Map.of("aString", new ConValue(ConType.STRING, "c"), "anInt",
									new ConValue(ConType.UNDEFINED, null))),
							"syn3", new SynapseRow().setRowId(3L).setVersionNumber(0L).setEtag("e1")),
					// 5
					new RowSourceItem(new TreeMap<>(Map.of("aString", new ConValue(ConType.UNDEFINED, null), "anInt",
							new ConValue(ConType.UNDEFINED, null))), "syn5", new SynapseRow().setRowId(5L))

			), results);
		}
		verifyNoMoreInteractionsOnAllMocks();
	}

	@Test
	public void testGetCurrentSchema() throws Exception {
		List<ColumnModel> schema = List.of(
				// aString
				new ColumnModel().setColumnType(ColumnType.STRING).setName("aString"),
				// anInt
				new ColumnModel().setColumnType(ColumnType.INTEGER).setName("anInt"));
		try (EntityViewSourceHandler handler = setupHandler(session, Collections.emptyList(), schema,
				Collections.emptyList()); RowSourceItemReader rowReader = handler.getSourceRowReader()) {

			// call under test
			assertEquals(List.of("aString", "anInt"), handler.getCurrentSourceSchema());
		}
		verifyNoMoreInteractionsOnAllMocks();
	}

	@Test
	public void testGetRowKey() throws Exception {
		try (EntityViewSourceHandler handler = setupHandler(session, Collections.emptyList(), Collections.emptyList(),
				Collections.emptyList()); RowSourceItemReader rowReader = handler.getSourceRowReader()) {

			// call under test
			assertEquals("syn111", handler.getRowKey(
					new RowCopyItemImpl().setSynapseRow(new SynapseRow().setRowId(111L).setVersionNumber(2L))));
		}
		verifyNoMoreInteractionsOnAllMocks();
	}

	@Test
	public void testGetRowKeyWithMissingSynapseRow() throws Exception {
		try (EntityViewSourceHandler handler = setupHandler(session, Collections.emptyList(), Collections.emptyList(),
				Collections.emptyList()); RowSourceItemReader rowReader = handler.getSourceRowReader()) {

			String message = assertThrows(IllegalArgumentException.class, () -> {
				// call under test
				handler.getRowKey(new RowCopyItemImpl().setSynapseRow(null));
			}).getMessage();
			assertEquals("Expected Synapse rows", message);
		}
		verifyNoMoreInteractionsOnAllMocks();
	}

	@Test
	public void testGetBenefactorIds() throws Exception {
		List<String> requiredColumns = Collections.emptyList();
		List<ColumnModel> schema = List.of(
				new ColumnModel().setColumnType(ColumnType.STRING).setName("aString"));
		Long benefactorOne = 111L;
		Long benefactorTwo = 222L;
		// Two rows with distinct benefactor IDs, one row with a null benefactor (should be ignored)
		List<Row> rows = List.of(
				new Row().setRowId(1L).setVersionNumber(1L).setEtag("e1").setBenefactorId(benefactorOne).setValues(Arrays.asList("a")),
				new Row().setRowId(2L).setVersionNumber(1L).setEtag("e2").setBenefactorId(benefactorTwo).setValues(Arrays.asList("b")),
				new Row().setRowId(3L).setVersionNumber(1L).setEtag("e3").setBenefactorId(benefactorOne).setValues(Arrays.asList("c")),
				new Row().setRowId(4L).setVersionNumber(1L).setEtag("e4").setValues(Arrays.asList("d")));

		try (EntityViewSourceHandler handler = setupHandler(session, requiredColumns, schema, rows);
				RowSourceItemReader reader = handler.getSourceRowReader()) {
			// call under test
			Set<Long> result = handler.getBenefactorIds();
			assertEquals(Set.of(benefactorOne, benefactorTwo), result);
		}
	}

	@Test
	public void testResolveSyncTypeEntityViewNullRejected() throws Exception {
		EntityViewSourceHandler handler = buildHandlerForSyncTypeTests();
		// call under test — null causes IllegalArgumentException
		assertThrows(IllegalArgumentException.class, () -> handler.validateSyncType(null));
	}

	@Test
	public void testResolveSyncTypeEntityViewPullPushAccepted() throws Exception {
		EntityViewSourceHandler handler = buildHandlerForSyncTypeTests();
		// call under test — PULL_PUSH is the only supported type
		assertDoesNotThrow(() -> handler.validateSyncType(SyncType.PULL_PUSH));
	}

	@Test
	public void testResolveSyncTypeEntityViewPullRejected() throws Exception {
		EntityViewSourceHandler handler = buildHandlerForSyncTypeTests();
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			handler.validateSyncType(SyncType.PULL);
		}).getMessage();
		assertEquals("PULL synchronization is not supported for EntityView-based grid sessions.", message);
	}

	/**
	 * Minimal setup for tests that only need a constructed handler and do not call
	 * {@link EntityViewSourceHandler#getSourceRowReader()}. Avoids the
	 * {@code createRandomAccessFile} stub that {@link #setupHandler} includes,
	 * which would be flagged as an unnecessary stubbing.
	 */
	private EntityViewSourceHandler buildHandlerForSyncTypeTests() throws Exception {
		GridSession noSchemaSession = new GridSession().setSourceEntityId("syn123").setSessionId("111");
		List<ColumnModel> schema = List.of(new ColumnModel().setColumnType(ColumnType.STRING).setName("aString"));
		List<Row> rows = List.of();

		doAnswer((InvocationOnMock invocation) -> {
			return File.createTempFile((String) invocation.getArgument(0), (String) invocation.getArgument(1));
		}).when(mockFileProvider).createTempFile("Source-" + noSchemaSession.getSourceEntityId(), ".bin");
		doAnswer((InvocationOnMock invocation) -> {
			return new BufferedOutputStream(new FileOutputStream((File) invocation.getArgument(0)));
		}).when(mockFileProvider).createFileOutputStream(any());
		when(mockGridAuthorizationManager.getRowLevelFilterUserInfo(mockUser, noSchemaSession.getSessionId()))
				.thenReturn(mockSessionOwner);
		doAnswer((InvocationOnMock invocation) -> {
			RowHandlerProvider rhp = invocation.getArgument(3);
			when(mockQueryTranslations.getMainQuery()).thenReturn(mockMainQuery);
			when(mockMainQuery.getTranslator()).thenReturn(mockQueryTranslator);
			when(mockQueryTranslator.getSchemaOfSelect()).thenReturn(schema);
			try (RowHandler rowHandler = rhp.getHandler(mockQueryTranslations)) {
				rows.forEach(r -> rowHandler.nextRow(r));
			}
			return new QueryResultBundle();
		}).when(mockTableQueryManager).runQueryAsStream(eq(mockCallback), eq(mockSessionOwner),
				eq(new Query().setSql("select * from " + noSchemaSession.getSourceEntityId())), any(),
				eq(ACCESS_TYPE.READ), eq(ACCESS_TYPE.UPDATE));

		return new EntityViewSourceHandler(mockCallback, mockUser, noSchemaSession, mockTableQueryManager,
				mockGridAuthorizationManager, mockFileProvider, mockAnnotationWriter, mockJsonSchemaManager,
				mockAnnotationsTranslator);
	}
}
