package org.sagebionetworks.repo.manager.grid.synch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import org.sagebionetworks.repo.manager.grid.GridManager;
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
public class GridSourceStatusManagerTest {

	private static final String SESSION_ID = "321";
	private static final String RECORD_SET_ID = "syn123";
	private static final String SCHEMA_ID = "org.sagebionetworks.test-RecordSet-1.0.0";
	private static final String BASELINE_FILE_HANDLE_ID = "111";

	@Mock
	private GridManager mockGridManager;
	@Mock
	private EntityManager mockEntityManager;
	@Mock
	private FileHandleManager mockFileHandleManager;

	@InjectMocks
	private GridSourceStatusManager manager;

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
		RecordSet current = recordSet(1L, BASELINE_FILE_HANDLE_ID, null, List.of("a"));
		setupSession(current);
		setupBoundSchema(SCHEMA_ID);
		setupBaseline();

		// call under test
		GridSourceStatus result = manager.getGridSourceStatus(user, SESSION_ID);

		assertEquals(expectedStatus(1L, List.of()), result);
		// the two versions share a file handle, so no MD5 comparison is needed
		verifyNoInteractions(mockFileHandleManager);
	}

	@Test
	public void testGetGridSourceStatusWithNewVersionAndSameFileHandle() {
		RecordSet current = recordSet(2L, BASELINE_FILE_HANDLE_ID, null, List.of("a"));
		setupSession(current);
		setupBoundSchema(SCHEMA_ID);
		setupBaseline();

		// call under test
		GridSourceStatus result = manager.getGridSourceStatus(user, SESSION_ID);

		assertEquals(expectedStatus(2L, List.of()), result);
		verifyNoInteractions(mockFileHandleManager);
	}

	@Test
	public void testGetGridSourceStatusWithNewFileHandleAndMatchingMd5() {
		RecordSet current = recordSet(2L, "222", null, List.of("a"));
		setupSession(current);
		setupBoundSchema(SCHEMA_ID);
		setupBaseline();
		when(mockFileHandleManager.isMatchingMD5(BASELINE_FILE_HANDLE_ID, "222")).thenReturn(true);

		// call under test
		GridSourceStatus result = manager.getGridSourceStatus(user, SESSION_ID);

		assertEquals(expectedStatus(2L, List.of()), result);
	}

	@Test
	public void testGetGridSourceStatusWithDataChange() {
		RecordSet current = recordSet(2L, "222", null, List.of("a"));
		setupSession(current);
		setupBoundSchema(SCHEMA_ID);
		setupBaseline();
		when(mockFileHandleManager.isMatchingMD5(BASELINE_FILE_HANDLE_ID, "222")).thenReturn(false);

		// call under test
		GridSourceStatus result = manager.getGridSourceStatus(user, SESSION_ID);

		assertEquals(expectedStatus(2L, List.of(GridSourceChangeType.DATA)), result);
	}

	@Test
	public void testGetGridSourceStatusWithCsvDescriptorChange() {
		RecordSet current = recordSet(2L, BASELINE_FILE_HANDLE_ID,
				new CsvTableDescriptor().setIsFirstLineHeader(true).setSeparator("\t"), List.of("a"));
		setupSession(current);
		setupBoundSchema(SCHEMA_ID);
		setupBaseline();

		// call under test
		GridSourceStatus result = manager.getGridSourceStatus(user, SESSION_ID);

		assertEquals(expectedStatus(2L, List.of(GridSourceChangeType.CSV_DESCRIPTOR)), result);
	}

	/**
	 * A null descriptor is compared as the default descriptor, so explicitly
	 * declaring the defaults is not reported as a change.
	 */
	@Test
	public void testGetGridSourceStatusWithExplicitDefaultCsvDescriptor() {
		RecordSet current = recordSet(2L, BASELINE_FILE_HANDLE_ID,
				new CsvTableDescriptor().setIsFirstLineHeader(true), List.of("a"));
		setupSession(current);
		setupBoundSchema(SCHEMA_ID);
		setupBaseline();

		// call under test
		GridSourceStatus result = manager.getGridSourceStatus(user, SESSION_ID);

		assertEquals(expectedStatus(2L, List.of()), result);
	}

	@Test
	public void testGetGridSourceStatusWithUpsertKeyChange() {
		RecordSet current = recordSet(2L, BASELINE_FILE_HANDLE_ID, null, List.of("a", "b"));
		setupSession(current);
		setupBoundSchema(SCHEMA_ID);
		setupBaseline();

		// call under test
		GridSourceStatus result = manager.getGridSourceStatus(user, SESSION_ID);

		assertEquals(expectedStatus(2L, List.of(GridSourceChangeType.UPSERT_KEY)), result);
	}

	/**
	 * Re-binding a schema does not create a new RecordSet version, so a schema
	 * change must be reported even though the baseline is the current version.
	 */
	@Test
	public void testGetGridSourceStatusWithJsonSchemaChangeAndSameVersion() {
		RecordSet current = recordSet(1L, BASELINE_FILE_HANDLE_ID, null, List.of("a"));
		setupSession(current);
		setupBoundSchema("org.sagebionetworks.test-RecordSet-2.0.0");
		setupBaseline();

		// call under test
		GridSourceStatus result = manager.getGridSourceStatus(user, SESSION_ID);

		assertEquals(expectedStatus(1L, List.of(GridSourceChangeType.JSON_SCHEMA)), result);
	}

	@Test
	public void testGetGridSourceStatusWithJsonSchemaUnbound() {
		RecordSet current = recordSet(1L, BASELINE_FILE_HANDLE_ID, null, List.of("a"));
		setupSession(current);
		when(mockEntityManager.findBoundSchema(RECORD_SET_ID)).thenReturn(Optional.empty());
		setupBaseline();

		// call under test
		GridSourceStatus result = manager.getGridSourceStatus(user, SESSION_ID);

		assertEquals(expectedStatus(1L, List.of(GridSourceChangeType.JSON_SCHEMA)), result);
	}

	@Test
	public void testGetGridSourceStatusWithNoBaseline() {
		session.setSourceEntityVersionNumber(null);
		RecordSet current = recordSet(1L, BASELINE_FILE_HANDLE_ID, null, List.of("a"));
		setupSession(current);
		setupBoundSchema(SCHEMA_ID);

		// call under test
		GridSourceStatus result = manager.getGridSourceStatus(user, SESSION_ID);

		assertEquals(new GridSourceStatus().setGridSessionId(SESSION_ID).setSourceEntityId(RECORD_SET_ID)
				.setBaselineVersionNumber(null).setCurrentVersionNumber(1L).setHasChanges(true)
				.setChanges(List.of(GridSourceChangeType.NO_BASELINE)), result);
		verify(mockEntityManager, never()).getEntityForVersion(any(), anyString(), anyLong(), any());
		verifyNoInteractions(mockFileHandleManager);
	}

	@Test
	public void testGetGridSourceStatusWithBaselineMissing() {
		RecordSet current = recordSet(2L, "222", null, List.of("a"));
		setupSession(current);
		setupBoundSchema(SCHEMA_ID);
		when(mockEntityManager.getEntityForVersion(user, RECORD_SET_ID, 1L, RecordSet.class))
				.thenThrow(new NotFoundException("not found"));

		// call under test
		GridSourceStatus result = manager.getGridSourceStatus(user, SESSION_ID);

		assertEquals(expectedStatus(2L, List.of(GridSourceChangeType.BASELINE_MISSING)), result);
		verifyNoInteractions(mockFileHandleManager);
	}

	/**
	 * Deleting versions can leave the current version below the session's baseline.
	 * The baseline is then gone, so there is nothing to compare against.
	 */
	@Test
	public void testGetGridSourceStatusWithCurrentVersionBelowBaseline() {
		session.setSourceEntityVersionNumber(3L);
		RecordSet current = recordSet(2L, BASELINE_FILE_HANDLE_ID, null, List.of("a"));
		setupSession(current);
		setupBoundSchema(SCHEMA_ID);
		when(mockEntityManager.getEntityForVersion(user, RECORD_SET_ID, 3L, RecordSet.class))
				.thenThrow(new NotFoundException("not found"));

		// call under test
		GridSourceStatus result = manager.getGridSourceStatus(user, SESSION_ID);

		assertEquals(new GridSourceStatus().setGridSessionId(SESSION_ID).setSourceEntityId(RECORD_SET_ID)
				.setBaselineVersionNumber(3L).setCurrentVersionNumber(2L).setHasChanges(true)
				.setChanges(List.of(GridSourceChangeType.BASELINE_MISSING)), result);
	}

	/**
	 * All of the change types are reported together, in the declaration order of
	 * {@link GridSourceChangeType}.
	 */
	@Test
	public void testGetGridSourceStatusWithEveryVersionChange() {
		RecordSet current = recordSet(2L, "222", new CsvTableDescriptor().setIsFirstLineHeader(true).setSeparator("\t"),
				List.of("b"));
		setupSession(current);
		setupBoundSchema("org.sagebionetworks.test-RecordSet-2.0.0");
		setupBaseline();
		when(mockFileHandleManager.isMatchingMD5(BASELINE_FILE_HANDLE_ID, "222")).thenReturn(false);

		// call under test
		GridSourceStatus result = manager.getGridSourceStatus(user, SESSION_ID);

		assertEquals(expectedStatus(2L, List.of(GridSourceChangeType.DATA, GridSourceChangeType.CSV_DESCRIPTOR,
				GridSourceChangeType.UPSERT_KEY, GridSourceChangeType.JSON_SCHEMA)), result);
	}

	@Test
	public void testGetGridSourceStatusWithEntityViewSource() {
		when(mockGridManager.getGridSession(user, SESSION_ID)).thenReturn(session);
		when(mockEntityManager.getEntity(user, RECORD_SET_ID, RecordSet.class))
				.thenThrow(new IllegalArgumentException("The Entity: syn123 has an entityType=entityview"));

		// call under test
		assertThrows(IllegalArgumentException.class, () -> manager.getGridSourceStatus(user, SESSION_ID));

		verify(mockEntityManager, never()).findBoundSchema(anyString());
		verify(mockEntityManager, never()).getEntityForVersion(any(), anyString(), anyLong(), any());
		verifyNoInteractions(mockFileHandleManager);
	}

	@Test
	public void testGetGridSourceStatusWithNullUser() {
		// call under test
		assertThrows(IllegalArgumentException.class, () -> manager.getGridSourceStatus(null, SESSION_ID));

		verifyNoInteractions(mockGridManager, mockEntityManager, mockFileHandleManager);
	}

	@Test
	public void testGetGridSourceStatusWithNullSessionId() {
		// call under test
		assertThrows(IllegalArgumentException.class, () -> manager.getGridSourceStatus(user, null));

		verifyNoInteractions(mockGridManager, mockEntityManager, mockFileHandleManager);
	}

	private void setupSession(RecordSet current) {
		when(mockGridManager.getGridSession(user, SESSION_ID)).thenReturn(session);
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

	private GridSourceStatus expectedStatus(Long currentVersionNumber, List<GridSourceChangeType> changes) {
		return new GridSourceStatus().setGridSessionId(SESSION_ID).setSourceEntityId(RECORD_SET_ID)
				.setBaselineVersionNumber(session.getSourceEntityVersionNumber())
				.setCurrentVersionNumber(currentVersionNumber).setHasChanges(!changes.isEmpty()).setChanges(changes);
	}

	private RecordSet recordSet(Long versionNumber, String dataFileHandleId, CsvTableDescriptor csvDescriptor,
			List<String> upsertKey) {
		return new RecordSet().setDataFileHandleId(dataFileHandleId).setCsvDescriptor(csvDescriptor)
				.setUpsertKey(upsertKey).setVersionNumber(versionNumber).setId(RECORD_SET_ID);
	}
}
