package org.sagebionetworks.repo.manager.grid.synch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
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
import org.sagebionetworks.repo.model.RecordSet;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.grid.GridSession;
import org.sagebionetworks.repo.model.grid.GridSourceChangeType;
import org.sagebionetworks.repo.model.grid.GridSourceStatus;
import org.sagebionetworks.repo.model.schema.JsonSchemaObjectBinding;
import org.sagebionetworks.repo.model.schema.JsonSchemaVersionInfo;
import org.sagebionetworks.repo.model.table.CsvTableDescriptor;
import org.sagebionetworks.repo.model.table.EntityView;
import org.sagebionetworks.repo.web.NotFoundException;

@ExtendWith(MockitoExtension.class)
public class GridSourceStatusManagerTest {

	@Mock
	private GridManager mockGridManager;
	@Mock
	private EntityManager mockEntityManager;
	@Mock
	private FileHandleManager mockFileHandleManager;
	@Mock
	private UserInfo mockUser;

	@InjectMocks
	private GridSourceStatusManager manager;

	private String sessionId;
	private String recordSetId;
	private String schemaId;
	private GridSession session;
	private RecordSet baseline;
	private RecordSet current;

	@BeforeEach
	public void before() {
		sessionId = "123";
		recordSetId = "syn456";
		schemaId = "my.org-schema-1.0.0";
		session = new GridSession().setSessionId(sessionId).setSourceEntityId(recordSetId)
				.setSourceEntityVersionNumber(1L).setGridJsonSchema$Id(schemaId);
		baseline = recordSet(1L);
		current = recordSet(2L);
	}

	private RecordSet recordSet(Long versionNumber) {
		RecordSet recordSet = new RecordSet().setUpsertKey(List.of("a", "b"))
				.setCsvDescriptor(new CsvTableDescriptor().setIsFirstLineHeader(true).setSeparator(","));
		recordSet.setId(recordSetId);
		recordSet.setVersionNumber(versionNumber);
		recordSet.setDataFileHandleId("789");
		return recordSet;
	}

	private void stubSessionAndCurrent() {
		when(mockGridManager.getGridSession(mockUser, sessionId)).thenReturn(session);
		when(mockEntityManager.getEntity(mockUser, recordSetId)).thenReturn(current);
	}

	private void stubBoundSchema(String boundSchemaId) {
		when(mockEntityManager.findBoundSchema(recordSetId)).thenReturn(Optional.ofNullable(boundSchemaId).map(
				id -> new JsonSchemaObjectBinding().setJsonSchemaVersionInfo(new JsonSchemaVersionInfo().set$id(id))));
	}

	private void stubBaseline() {
		when(mockEntityManager.getEntityForVersion(mockUser, recordSetId, 1L, RecordSet.class)).thenReturn(baseline);
	}

	private GridSourceStatus expectedStatus(Long baselineVersionNumber, Long currentVersionNumber,
			List<GridSourceChangeType> changes) {
		return new GridSourceStatus().setGridSessionId(sessionId).setSourceEntityId(recordSetId)
				.setBaselineVersionNumber(baselineVersionNumber).setCurrentVersionNumber(currentVersionNumber)
				.setHasChanges(!changes.isEmpty()).setChanges(changes);
	}

	@Test
	public void testGetGridSourceStatusWithSameVersion() {
		current = recordSet(1L);
		stubSessionAndCurrent();
		stubBoundSchema(schemaId);
		stubBaseline();

		// call under test
		GridSourceStatus status = manager.getGridSourceStatus(mockUser, sessionId);

		assertEquals(expectedStatus(1L, 1L, List.of()), status);
		verifyNoInteractions(mockFileHandleManager);
	}

	@Test
	public void testGetGridSourceStatusWithNewVersionAndSameFileHandle() {
		stubSessionAndCurrent();
		stubBoundSchema(schemaId);
		stubBaseline();

		// call under test
		GridSourceStatus status = manager.getGridSourceStatus(mockUser, sessionId);

		assertEquals(expectedStatus(1L, 2L, List.of()), status);
		verifyNoInteractions(mockFileHandleManager);
	}

	@Test
	public void testGetGridSourceStatusWithNewFileHandleAndMatchingMD5() {
		current.setDataFileHandleId("999");
		stubSessionAndCurrent();
		stubBoundSchema(schemaId);
		stubBaseline();
		when(mockFileHandleManager.isMatchingMD5("789", "999")).thenReturn(true);

		// call under test
		GridSourceStatus status = manager.getGridSourceStatus(mockUser, sessionId);

		assertEquals(expectedStatus(1L, 2L, List.of()), status);
	}

	@Test
	public void testGetGridSourceStatusWithData() {
		current.setDataFileHandleId("999");
		stubSessionAndCurrent();
		stubBoundSchema(schemaId);
		stubBaseline();
		when(mockFileHandleManager.isMatchingMD5("789", "999")).thenReturn(false);

		// call under test
		GridSourceStatus status = manager.getGridSourceStatus(mockUser, sessionId);

		assertEquals(expectedStatus(1L, 2L, List.of(GridSourceChangeType.DATA)), status);
	}

	@Test
	public void testGetGridSourceStatusWithCsvDescriptor() {
		current.setCsvDescriptor(new CsvTableDescriptor().setIsFirstLineHeader(true).setSeparator("\t"));
		stubSessionAndCurrent();
		stubBoundSchema(schemaId);
		stubBaseline();

		// call under test
		GridSourceStatus status = manager.getGridSourceStatus(mockUser, sessionId);

		assertEquals(expectedStatus(1L, 2L, List.of(GridSourceChangeType.CSV_DESCRIPTOR)), status);
	}

	@Test
	public void testGetGridSourceStatusWithNullCsvDescriptorAndDefaultCsvDescriptor() {
		baseline.setCsvDescriptor(null);
		current.setCsvDescriptor(new CsvTableDescriptor().setIsFirstLineHeader(true));
		stubSessionAndCurrent();
		stubBoundSchema(schemaId);
		stubBaseline();

		// call under test
		GridSourceStatus status = manager.getGridSourceStatus(mockUser, sessionId);

		assertEquals(expectedStatus(1L, 2L, List.of()), status);
	}

	@Test
	public void testGetGridSourceStatusWithNullCsvDescriptorAndNonDefaultCsvDescriptor() {
		baseline.setCsvDescriptor(null);
		current.setCsvDescriptor(new CsvTableDescriptor().setIsFirstLineHeader(false));
		stubSessionAndCurrent();
		stubBoundSchema(schemaId);
		stubBaseline();

		// call under test
		GridSourceStatus status = manager.getGridSourceStatus(mockUser, sessionId);

		assertEquals(expectedStatus(1L, 2L, List.of(GridSourceChangeType.CSV_DESCRIPTOR)), status);
	}

	@Test
	public void testGetGridSourceStatusWithUpsertKey() {
		current.setUpsertKey(List.of("a"));
		stubSessionAndCurrent();
		stubBoundSchema(schemaId);
		stubBaseline();

		// call under test
		GridSourceStatus status = manager.getGridSourceStatus(mockUser, sessionId);

		assertEquals(expectedStatus(1L, 2L, List.of(GridSourceChangeType.UPSERT_KEY)), status);
	}

	@Test
	public void testGetGridSourceStatusWithJsonSchemaAndSameVersion() {
		current = recordSet(1L);
		stubSessionAndCurrent();
		stubBoundSchema("my.org-schema-2.0.0");
		stubBaseline();

		// call under test
		GridSourceStatus status = manager.getGridSourceStatus(mockUser, sessionId);

		assertEquals(expectedStatus(1L, 1L, List.of(GridSourceChangeType.JSON_SCHEMA)), status);
	}

	@Test
	public void testGetGridSourceStatusWithJsonSchemaUnbound() {
		stubSessionAndCurrent();
		stubBoundSchema(null);
		stubBaseline();

		// call under test
		GridSourceStatus status = manager.getGridSourceStatus(mockUser, sessionId);

		assertEquals(expectedStatus(1L, 2L, List.of(GridSourceChangeType.JSON_SCHEMA)), status);
	}

	@Test
	public void testGetGridSourceStatusWithNoSchemaOnSessionOrSource() {
		session.setGridJsonSchema$Id(null);
		stubSessionAndCurrent();
		stubBoundSchema(null);
		stubBaseline();

		// call under test
		GridSourceStatus status = manager.getGridSourceStatus(mockUser, sessionId);

		assertEquals(expectedStatus(1L, 2L, List.of()), status);
	}

	@Test
	public void testGetGridSourceStatusWithNoBaseline() {
		session.setSourceEntityVersionNumber(null);
		stubSessionAndCurrent();
		stubBoundSchema(schemaId);

		// call under test
		GridSourceStatus status = manager.getGridSourceStatus(mockUser, sessionId);

		assertEquals(expectedStatus(null, 2L, List.of(GridSourceChangeType.NO_BASELINE)), status);
		verify(mockEntityManager, never()).getEntityForVersion(any(), any(), any(), any());
		verifyNoInteractions(mockFileHandleManager);
	}

	@Test
	public void testGetGridSourceStatusWithBaselineMissing() {
		// the baseline version was deleted, leaving the current version lower than it
		session.setSourceEntityVersionNumber(3L);
		stubSessionAndCurrent();
		stubBoundSchema(schemaId);
		when(mockEntityManager.getEntityForVersion(mockUser, recordSetId, 3L, RecordSet.class))
				.thenThrow(new NotFoundException("missing"));

		// call under test
		GridSourceStatus status = manager.getGridSourceStatus(mockUser, sessionId);

		assertEquals(expectedStatus(3L, 2L, List.of(GridSourceChangeType.BASELINE_MISSING)), status);
		verifyNoInteractions(mockFileHandleManager);
	}

	@Test
	public void testGetGridSourceStatusWithMultipleChanges() {
		current.setDataFileHandleId("999");
		current.setUpsertKey(List.of("a"));
		current.setCsvDescriptor(new CsvTableDescriptor().setIsFirstLineHeader(false));
		stubSessionAndCurrent();
		stubBoundSchema("my.org-schema-2.0.0");
		stubBaseline();
		when(mockFileHandleManager.isMatchingMD5("789", "999")).thenReturn(false);

		// call under test
		GridSourceStatus status = manager.getGridSourceStatus(mockUser, sessionId);

		assertEquals(expectedStatus(1L, 2L, List.of(GridSourceChangeType.DATA, GridSourceChangeType.CSV_DESCRIPTOR,
				GridSourceChangeType.UPSERT_KEY, GridSourceChangeType.JSON_SCHEMA)), status);
	}

	@Test
	public void testGetGridSourceStatusWithEntityView() {
		when(mockGridManager.getGridSession(mockUser, sessionId)).thenReturn(session);
		when(mockEntityManager.getEntity(mockUser, recordSetId)).thenReturn(new EntityView());

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.getGridSourceStatus(mockUser, sessionId);
		}).getMessage();

		assertEquals("Grid source status is only available for a grid session with a RecordSet source", message);
		verify(mockEntityManager, never()).findBoundSchema(any());
		verify(mockEntityManager, never()).getEntityForVersion(any(), any(), any(), any());
		verifyNoInteractions(mockFileHandleManager);
	}

	@Test
	public void testGetGridSourceStatusWithUnauthorizedSession() {
		when(mockGridManager.getGridSession(mockUser, sessionId)).thenThrow(new UnauthorizedException("nope"));

		assertThrows(UnauthorizedException.class, () -> {
			// call under test
			manager.getGridSourceStatus(mockUser, sessionId);
		});

		verifyNoInteractions(mockEntityManager, mockFileHandleManager);
	}

	@Test
	public void testGetGridSourceStatusWithUnauthorizedSource() {
		when(mockGridManager.getGridSession(mockUser, sessionId)).thenReturn(session);
		when(mockEntityManager.getEntity(mockUser, recordSetId)).thenThrow(new UnauthorizedException("nope"));

		assertThrows(UnauthorizedException.class, () -> {
			// call under test
			manager.getGridSourceStatus(mockUser, sessionId);
		});

		verify(mockEntityManager, never()).findBoundSchema(any());
		verifyNoInteractions(mockFileHandleManager);
	}

	@Test
	public void testGetGridSourceStatusWithNullUser() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.getGridSourceStatus(null, sessionId);
		}).getMessage();

		assertEquals("user is required.", message);
		verifyNoInteractions(mockGridManager, mockEntityManager, mockFileHandleManager);
	}

	@Test
	public void testGetGridSourceStatusWithNullSessionId() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.getGridSourceStatus(mockUser, null);
		}).getMessage();

		assertEquals("sessionId is required.", message);
		verifyNoInteractions(mockGridManager, mockEntityManager, mockFileHandleManager);
	}
}
