package org.sagebionetworks.repo.manager.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.EnumSource.Mode;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.table.ColumnModelManager;
import org.sagebionetworks.repo.manager.table.RecordSetSchemaResolver;
import org.sagebionetworks.repo.manager.table.TableManagerSupport;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.RecordSet;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dbo.schema.EntitySchemaValidationResultDao;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.schema.JsonSchema;
import org.sagebionetworks.repo.model.schema.Type;
import org.sagebionetworks.repo.model.schema.ValidationSummaryStatistics;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.CsvTableDescriptor;
import org.sagebionetworks.repo.service.metadata.EntityEvent;
import org.sagebionetworks.repo.service.metadata.EventType;

@ExtendWith(MockitoExtension.class)
public class RecordSetManagerImplTest {

	@Mock
	private RecordSetSchemaResolver mockSchemaResolver;

	@Mock
	private ColumnModelManager mockColumnModelManager;

	@Mock
	private NodeDAO mockNodeDao;

	@Mock
	private TableManagerSupport mockTableManagerSupport;

	@Mock
	private EntitySchemaValidationResultDao mockValidationResultDao;

	@InjectMocks
	private RecordSetManagerImpl recordSetManager;

	@Mock
	private ValidationSummaryStatistics mockValidationStats;

	private RecordSet recordSet;
	private UserInfo userInfo;

	private JsonSchema boundSchema;
	private List<ColumnModel> schemaColumns;
	private List<ColumnModel> persistedColumns;
	private long newRevisionNumber = 3L;
	private IdAndVersion versionedKey;
	private IdAndVersion entityKey;

	@BeforeEach
	public void before() {
		recordSet = new RecordSet();
		recordSet.setId("syn123");
		recordSet.setVersionNumber(2L);
		recordSet.setDataFileHandleId("456");
		recordSet.setParentId("syn234567");
		recordSet.setUpsertKey(List.of("a", "b"));

		userInfo = new UserInfo(false, 55L, AuthorizationConstants.DEFAULT_REALM_ID);

		Map<String, JsonSchema> properties = new LinkedHashMap<>();
		properties.put("a", new JsonSchema().setType(Type.integer));
		properties.put("b", new JsonSchema().setType(Type._boolean));
		boundSchema = new JsonSchema().setProperties(properties);

		schemaColumns = List.of(
			new ColumnModel().setName("a").setColumnType(ColumnType.INTEGER),
			new ColumnModel().setName("b").setColumnType(ColumnType.BOOLEAN)
		);
		persistedColumns = List.of(
			new ColumnModel().setId("11").setName("a").setColumnType(ColumnType.INTEGER),
			new ColumnModel().setId("22").setName("b").setColumnType(ColumnType.BOOLEAN)
		);

		// The revision number may not have been bumped in the RecordSet DTO, so the
		// manager must look up the current revision number for the updated object.
		versionedKey = IdAndVersion.newBuilder().setId(123L).setVersion(newRevisionNumber).build();
		entityKey = IdAndVersion.newBuilder().setId(123L).build();
	}

	/**
	 * Stubs the schema-binding path exercised by inferSchemaAndBindToIndex.
	 */
	private void setupSchemaBinding() {
		when(mockNodeDao.getCurrentRevisionNumber("syn123")).thenReturn(newRevisionNumber);
		when(mockSchemaResolver.getBoundValidationSchema("syn123")).thenReturn(Optional.of(boundSchema));
		when(mockColumnModelManager.createColumnModels(userInfo, schemaColumns)).thenReturn(persistedColumns);
	}

	/**
	 * Verifies the schema was bound to both the versioned snapshot and the entity-level default.
	 */
	private void verifySchemaBound() {
		List<String> expectedIds = List.of("11", "22");
		verify(mockColumnModelManager).createColumnModels(userInfo, schemaColumns);
		verify(mockColumnModelManager).bindColumnsToVersionOfObject(expectedIds, versionedKey);
		verify(mockColumnModelManager).bindColumnsToDefaultVersionOfObject(expectedIds, "syn123");
	}

	@ParameterizedTest
	@EnumSource(value = EventType.class, mode = Mode.INCLUDE, names = {"CREATE", "UPDATE"})
	public void testValidateRecordSet(EventType eventType) {
		EntityEvent event = new EntityEvent(eventType, null, userInfo, false);

		// call under test
		recordSetManager.validateRecordSet(recordSet, event);
	}

	@ParameterizedTest
	@EnumSource(value = EventType.class, mode = Mode.INCLUDE, names = {"CREATE", "UPDATE"})
	public void testValidateRecordSetWithNoUpsertKey(EventType eventType) {
		recordSet.setUpsertKey(null);

		EntityEvent event = new EntityEvent(eventType, null, userInfo, false);

		assertEquals("The upsertKey is required and must not be empty.",
			assertThrows(IllegalArgumentException.class, () -> {
				// call under test
				recordSetManager.validateRecordSet(recordSet, event);
			}).getMessage()
		);

		recordSet.setUpsertKey(Collections.emptyList());

		assertEquals("The upsertKey is required and must not be empty.",
			assertThrows(IllegalArgumentException.class, () -> {
				// call under test
				recordSetManager.validateRecordSet(recordSet, event);
			}).getMessage()
		);
	}

	@ParameterizedTest
	@EnumSource(value = EventType.class, mode = Mode.INCLUDE, names = {"CREATE", "UPDATE"})
	public void testValidateRecordSetWithCsvDescriptorAndIsFirstLineHeaderMissingOrFalse(EventType eventType) {
		recordSet.setCsvDescriptor(new CsvTableDescriptor().setIsFirstLineHeader(false));

		EntityEvent event = new EntityEvent(eventType, null, userInfo, false);

		assertEquals("The csvDescriptor.isFirstLineHeader must be true.",
			assertThrows(IllegalArgumentException.class, () -> {
				// call under test
				recordSetManager.validateRecordSet(recordSet, event);
			}).getMessage()
		);

		recordSet.setCsvDescriptor(new CsvTableDescriptor().setIsFirstLineHeader(null));

		assertEquals("The csvDescriptor.isFirstLineHeader must be true.",
			assertThrows(IllegalArgumentException.class, () -> {
				// call under test
				recordSetManager.validateRecordSet(recordSet, event);
			}).getMessage()
		);
	}

	@ParameterizedTest
	@EnumSource(value = EventType.class, mode = Mode.INCLUDE, names = {"CREATE", "UPDATE"})
	public void testValidateRecordSetSanitizesWhenNotSkipped(EventType eventType) {
		// The server-controlled validation fields must be stripped from client input.
		recordSet.setValidationSummary(mockValidationStats);
		recordSet.setValidationFileHandleId("987");

		EntityEvent event = new EntityEvent(eventType, null, userInfo, false);

		// call under test
		recordSetManager.validateRecordSet(recordSet, event);

		assertNull(recordSet.getValidationSummary());
		assertNull(recordSet.getValidationFileHandleId());
	}

	@ParameterizedTest
	@EnumSource(value = EventType.class, mode = Mode.INCLUDE, names = {"CREATE", "UPDATE"})
	public void testValidateRecordSetWithSkipSanitization(EventType eventType) {
		// Trusted internal callers (the grid exporter) may set the validation fields directly.
		recordSet.setValidationSummary(mockValidationStats);
		recordSet.setValidationFileHandleId("987");

		EntityEvent event = new EntityEvent(eventType, null, userInfo, true);

		// call under test
		recordSetManager.validateRecordSet(recordSet, event);

		assertEquals(mockValidationStats, recordSet.getValidationSummary());
		assertEquals("987", recordSet.getValidationFileHandleId());
	}

	@ParameterizedTest
	@EnumSource(value = EventType.class, mode = Mode.EXCLUDE, names = {"CREATE", "UPDATE"})
	public void testValidateRecordSetWithOtherEventType(EventType eventType) {
		// Only CREATE/UPDATE validate and sanitize; other event types are a no-op.
		recordSet.setUpsertKey(null);
		recordSet.setValidationSummary(mockValidationStats);
		recordSet.setValidationFileHandleId("987");

		EntityEvent event = new EntityEvent(eventType, null, userInfo, false);

		// call under test
		recordSetManager.validateRecordSet(recordSet, event);

		assertEquals(mockValidationStats, recordSet.getValidationSummary());
		assertEquals("987", recordSet.getValidationFileHandleId());
	}

	@Test
	public void testInferSchemaAndBindToIndex() {
		setupSchemaBinding();

		// call under test
		recordSetManager.inferSchemaAndBindToIndex(userInfo, recordSet);

		verifySchemaBound();
		// Both triggers fire: the versioned one ensures this revision's snapshot
		// T{id}_{v} is built even under message-ordering races, and the versionless
		// one flips entity-level status to PROCESSING so unversioned queries wait
		// instead of returning stale data.
		verify(mockTableManagerSupport).setTableToProcessingAndTriggerUpdate(versionedKey);
		verify(mockTableManagerSupport).setTableToProcessingAndTriggerUpdate(entityKey);
	}

	@Test
	public void testInferSchemaAndBindToIndexWithEmptySchema() {
		// A bound schema is present but declares no properties, so no columns can be derived.
		when(mockSchemaResolver.getBoundValidationSchema("syn123")).thenReturn(Optional.of(new JsonSchema()));

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			recordSetManager.inferSchemaAndBindToIndex(userInfo, recordSet);
		}).getMessage();

		assertEquals("Cannot determine the column model schema from the JSON Schema. At least one property must be present.", message);

		verifyNoInteractions(mockColumnModelManager);
		verifyNoInteractions(mockNodeDao);
		verifyNoInteractions(mockTableManagerSupport);
	}

	@Test
	public void testInferSchemaAndBindToIndexWithNoBoundSchema() {
		// With no bound JSON Schema the RecordSet is not indexed: the whole binding path is skipped.
		when(mockSchemaResolver.getBoundValidationSchema("syn123")).thenReturn(Optional.empty());

		// call under test
		recordSetManager.inferSchemaAndBindToIndex(userInfo, recordSet);

		verifyNoInteractions(mockColumnModelManager);
		verifyNoInteractions(mockNodeDao);
		verifyNoInteractions(mockTableManagerSupport);
	}

	@Test
	public void testUpdateWithValidationResults() {
		when(mockValidationResultDao.getRecordSetValidationSummaryStatistics(KeyFactory.stringToKey(recordSet.getId()), recordSet.getVersionNumber())).thenReturn(
			Optional.of(mockValidationStats)
		);

		// call under test
		recordSetManager.updateWithValidationResults(recordSet);

		assertEquals(mockValidationStats, recordSet.getValidationSummary());
	}

	@Test
	public void testUpdateWithValidationResultsWithNoValidationStats() {
		when(mockValidationResultDao.getRecordSetValidationSummaryStatistics(KeyFactory.stringToKey(recordSet.getId()), recordSet.getVersionNumber())).thenReturn(
			Optional.empty()
		);

		// call under test
		recordSetManager.updateWithValidationResults(recordSet);

		assertNull(recordSet.getValidationSummary());
	}
}
