package org.sagebionetworks.repo.manager.grid.synch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.RecordSet;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.grid.GridSession;
import org.sagebionetworks.repo.model.grid.GridSourceChangeType;
import org.sagebionetworks.repo.model.grid.GridSourceStatus;
import org.sagebionetworks.repo.model.schema.JsonSchemaObjectBinding;
import org.sagebionetworks.repo.model.schema.JsonSchemaVersionInfo;
import org.sagebionetworks.repo.model.table.CsvTableDescriptor;
import org.sagebionetworks.repo.web.NotFoundException;

@ExtendWith(MockitoExtension.class)
public class GridRecordSetSourceStatusManagerTest {

	private static final String SESSION_ID = "321";
	private static final String RECORD_SET_ID = "syn123";
	private static final String SCHEMA_ID = "org.sagebionetworks.test-RecordSet-1.0.0";
	private static final String BASELINE_FILE_HANDLE_ID = "111";

	@Mock
	private EntityManager mockEntityManager;
	@Mock
	private FileHandleManager mockFileHandleManager;

	@InjectMocks
	private GridRecordSetSourceStatusManager manager;

	private UserInfo user;
	private GridSession session;
	private RecordSet baseline;

	@BeforeEach
	public void before() {
		user = new UserInfo(false, 555L, AuthorizationConstants.DEFAULT_REALM_ID);
		session = new GridSession().setSessionId(SESSION_ID).setSourceEntityId(RECORD_SET_ID)
				.setSourceEntityVersionNumber(1L).setGridJsonSchema$Id(SCHEMA_ID);
		baseline = recordSet(1L, BASELINE_FILE_HANDLE_ID, null, List.of("a"));
	}

	@Test
	public void testGetGridSourceStatusWithNoChanges() {
		setupCurrent(recordSet(1L, BASELINE_FILE_HANDLE_ID, null, List.of("a")));
		setupBoundSchema(SCHEMA_ID);
		setupBaseline();

		// call under test
		GridSourceStatus result = manager.getGridSourceStatus(user, session);

		assertEquals(expectedStatus(List.of()), result);
		// the two versions share a file handle, so no MD5 comparison is needed
		verifyNoInteractions(mockFileHandleManager);
	}

	/**
	 * A revision that re-uses the previous revision's file handle carries the same
	 * data, so simply being newer than the baseline is not a change.
	 */
	@Test
	public void testGetGridSourceStatusWithNewVersionAndSameFileHandle() {
		setupCurrent(recordSet(2L, BASELINE_FILE_HANDLE_ID, null, List.of("a")));
		setupBoundSchema(SCHEMA_ID);
		setupBaseline();

		// call under test
		GridSourceStatus result = manager.getGridSourceStatus(user, session);

		assertEquals(expectedStatus(List.of()), result);
		verifyNoInteractions(mockFileHandleManager);
	}

	@Test
	public void testGetGridSourceStatusWithNewFileHandleAndMatchingMd5() {
		setupCurrent(recordSet(2L, "222", null, List.of("a")));
		setupBoundSchema(SCHEMA_ID);
		setupBaseline();
		when(mockFileHandleManager.isMatchingMD5(BASELINE_FILE_HANDLE_ID, "222")).thenReturn(true);

		// call under test
		GridSourceStatus result = manager.getGridSourceStatus(user, session);

		assertEquals(expectedStatus(List.of()), result);
	}

	@Test
	public void testGetGridSourceStatusWithDataChange() {
		setupCurrent(recordSet(2L, "222", null, List.of("a")));
		setupBoundSchema(SCHEMA_ID);
		setupBaseline();
		when(mockFileHandleManager.isMatchingMD5(BASELINE_FILE_HANDLE_ID, "222")).thenReturn(false);

		// call under test
		GridSourceStatus result = manager.getGridSourceStatus(user, session);

		assertEquals(expectedStatus(List.of(GridSourceChangeType.DATA)), result);
	}

	@Test
	public void testGetGridSourceStatusWithCsvDescriptorChange() {
		setupCurrent(recordSet(2L, BASELINE_FILE_HANDLE_ID,
				new CsvTableDescriptor().setIsFirstLineHeader(true).setSeparator("\t"), List.of("a")));
		setupBoundSchema(SCHEMA_ID);
		setupBaseline();

		// call under test
		GridSourceStatus result = manager.getGridSourceStatus(user, session);

		assertEquals(expectedStatus(List.of(GridSourceChangeType.CSV_DESCRIPTOR)), result);
	}

	/**
	 * A null descriptor is compared as the default descriptor, so explicitly
	 * declaring the defaults is not reported as a change.
	 */
	@Test
	public void testGetGridSourceStatusWithExplicitDefaultCsvDescriptor() {
		setupCurrent(recordSet(2L, BASELINE_FILE_HANDLE_ID, new CsvTableDescriptor().setIsFirstLineHeader(true),
				List.of("a")));
		setupBoundSchema(SCHEMA_ID);
		setupBaseline();

		// call under test
		GridSourceStatus result = manager.getGridSourceStatus(user, session);

		assertEquals(expectedStatus(List.of()), result);
	}

	@Test
	public void testGetGridSourceStatusWithUpsertKeyChange() {
		setupCurrent(recordSet(2L, BASELINE_FILE_HANDLE_ID, null, List.of("a", "b")));
		setupBoundSchema(SCHEMA_ID);
		setupBaseline();

		// call under test
		GridSourceStatus result = manager.getGridSourceStatus(user, session);

		assertEquals(expectedStatus(List.of(GridSourceChangeType.UPSERT_KEY)), result);
	}

	/**
	 * Re-binding a schema does not create a new RecordSet version, so a schema
	 * change must be reported even though the baseline is the current version.
	 */
	@Test
	public void testGetGridSourceStatusWithJsonSchemaChangeAndSameVersion() {
		setupCurrent(recordSet(1L, BASELINE_FILE_HANDLE_ID, null, List.of("a")));
		setupBoundSchema("org.sagebionetworks.test-RecordSet-2.0.0");
		setupBaseline();

		// call under test
		GridSourceStatus result = manager.getGridSourceStatus(user, session);

		assertEquals(expectedStatus(List.of(GridSourceChangeType.JSON_SCHEMA)), result);
	}

	/**
	 * Unbinding the source's schema is a change the grid has not picked up: it is
	 * still validating rows against the schema recorded on the session. A
	 * synchronization resolves it by clearing the session's recorded $id.
	 */
	@Test
	public void testGetGridSourceStatusWithJsonSchemaUnbound() {
		setupCurrent(recordSet(1L, BASELINE_FILE_HANDLE_ID, null, List.of("a")));
		when(mockEntityManager.findBoundSchema(RECORD_SET_ID)).thenReturn(Optional.empty());
		setupBaseline();

		// call under test
		GridSourceStatus result = manager.getGridSourceStatus(user, session);

		assertEquals(expectedStatus(List.of(GridSourceChangeType.JSON_SCHEMA)), result);
	}

	/**
	 * A session whose recorded $id is null has already taken in an unbound source,
	 * so there is nothing left to report.
	 */
	@Test
	public void testGetGridSourceStatusWithNoSchemaOnSessionOrSource() {
		session.setGridJsonSchema$Id(null);
		setupCurrent(recordSet(1L, BASELINE_FILE_HANDLE_ID, null, List.of("a")));
		when(mockEntityManager.findBoundSchema(RECORD_SET_ID)).thenReturn(Optional.empty());
		setupBaseline();

		// call under test
		GridSourceStatus result = manager.getGridSourceStatus(user, session);

		assertEquals(expectedStatus(List.of()), result);
	}

	/**
	 * Binding a schema to a source that had none when the grid was created is a
	 * change, even though the session's recorded $id is null.
	 */
	@Test
	public void testGetGridSourceStatusWithSchemaBoundAfterSessionCreated() {
		session.setGridJsonSchema$Id(null);
		setupCurrent(recordSet(1L, BASELINE_FILE_HANDLE_ID, null, List.of("a")));
		setupBoundSchema(SCHEMA_ID);
		setupBaseline();

		// call under test
		GridSourceStatus result = manager.getGridSourceStatus(user, session);

		assertEquals(expectedStatus(List.of(GridSourceChangeType.JSON_SCHEMA)), result);
	}

	@Test
	public void testGetGridSourceStatusWithNoBaseline() {
		session.setSourceEntityVersionNumber(null);
		setupCurrent(recordSet(1L, BASELINE_FILE_HANDLE_ID, null, List.of("a")));
		setupBoundSchema(SCHEMA_ID);

		// call under test
		GridSourceStatus result = manager.getGridSourceStatus(user, session);

		assertEquals(expectedStatus(List.of(GridSourceChangeType.NO_BASELINE)), result);
		verify(mockEntityManager, never()).getEntityForVersion(any(), anyString(), anyLong(), any());
		verifyNoInteractions(mockFileHandleManager);
	}

	@Test
	public void testGetGridSourceStatusWithBaselineMissing() {
		setupCurrent(recordSet(2L, "222", null, List.of("a")));
		setupBoundSchema(SCHEMA_ID);
		when(mockEntityManager.getEntityForVersion(user, RECORD_SET_ID, 1L, RecordSet.class))
				.thenThrow(new NotFoundException("not found"));

		// call under test
		GridSourceStatus result = manager.getGridSourceStatus(user, session);

		assertEquals(expectedStatus(List.of(GridSourceChangeType.BASELINE_MISSING)), result);
		verifyNoInteractions(mockFileHandleManager);
	}

	/**
	 * Deleting versions can leave the current version below the session's baseline.
	 * The baseline is then gone, so there is nothing to compare against.
	 */
	@Test
	public void testGetGridSourceStatusWithCurrentVersionBelowBaseline() {
		session.setSourceEntityVersionNumber(3L);
		setupCurrent(recordSet(2L, BASELINE_FILE_HANDLE_ID, null, List.of("a")));
		setupBoundSchema(SCHEMA_ID);
		when(mockEntityManager.getEntityForVersion(user, RECORD_SET_ID, 3L, RecordSet.class))
				.thenThrow(new NotFoundException("not found"));

		// call under test
		GridSourceStatus result = manager.getGridSourceStatus(user, session);

		assertEquals(expectedStatus(List.of(GridSourceChangeType.BASELINE_MISSING)), result);
	}

	/**
	 * All of the change types are reported together, in the declaration order of
	 * {@link GridSourceChangeType}.
	 */
	@Test
	public void testGetGridSourceStatusWithEveryVersionChange() {
		setupCurrent(recordSet(2L, "222", new CsvTableDescriptor().setIsFirstLineHeader(true).setSeparator("\t"),
				List.of("b")));
		setupBoundSchema("org.sagebionetworks.test-RecordSet-2.0.0");
		setupBaseline();
		when(mockFileHandleManager.isMatchingMD5(BASELINE_FILE_HANDLE_ID, "222")).thenReturn(false);

		// call under test
		GridSourceStatus result = manager.getGridSourceStatus(user, session);

		assertEquals(expectedStatus(List.of(GridSourceChangeType.DATA, GridSourceChangeType.CSV_DESCRIPTOR,
				GridSourceChangeType.UPSERT_KEY, GridSourceChangeType.JSON_SCHEMA)), result);
	}

	private void setupCurrent(RecordSet current) {
		when(mockEntityManager.getEntity(user, RECORD_SET_ID, RecordSet.class)).thenReturn(current);
	}

	private void setupBoundSchema(String schemaId) {
		when(mockEntityManager.findBoundSchema(RECORD_SET_ID)).thenReturn(Optional.of(new JsonSchemaObjectBinding()
				.setJsonSchemaVersionInfo(new JsonSchemaVersionInfo().set$id(schemaId))));
	}

	private void setupBaseline() {
		when(mockEntityManager.getEntityForVersion(user, RECORD_SET_ID, session.getSourceEntityVersionNumber(),
				RecordSet.class)).thenReturn(baseline);
	}

	private GridSourceStatus expectedStatus(List<GridSourceChangeType> changes) {
		return new GridSourceStatus().setGridSessionId(SESSION_ID).setSourceEntityId(RECORD_SET_ID)
				.setHasChanges(!changes.isEmpty()).setChanges(changes);
	}

	private RecordSet recordSet(Long versionNumber, String dataFileHandleId, CsvTableDescriptor csvDescriptor,
			List<String> upsertKey) {
		return new RecordSet().setDataFileHandleId(dataFileHandleId).setCsvDescriptor(csvDescriptor)
				.setUpsertKey(upsertKey).setVersionNumber(versionNumber).setId(RECORD_SET_ID);
	}
}
