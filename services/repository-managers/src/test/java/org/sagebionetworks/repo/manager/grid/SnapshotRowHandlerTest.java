package org.sagebionetworks.repo.manager.grid;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.schema.JsonSchemaValidationManager;
import org.sagebionetworks.repo.manager.schema.JsonSubject;
import org.sagebionetworks.repo.model.dbo.dao.table.TableModelTestUtils;
import org.sagebionetworks.repo.model.grid.ClockTable;
import org.sagebionetworks.repo.model.grid.encoding.IndexedModelEncoder;
import org.sagebionetworks.repo.model.grid.node.ArrayNode;
import org.sagebionetworks.repo.model.grid.node.ConstantNode;
import org.sagebionetworks.repo.model.grid.node.Node;
import org.sagebionetworks.repo.model.grid.node.ObjectNode;
import org.sagebionetworks.repo.model.grid.node.RGANode;
import org.sagebionetworks.repo.model.grid.node.VectorNode;
import org.sagebionetworks.repo.model.grid.patch.ConType;
import org.sagebionetworks.repo.model.grid.patch.ConValue;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.repo.model.schema.JsonSchema;
import org.sagebionetworks.repo.model.schema.ValidationException;
import org.sagebionetworks.repo.model.schema.ValidationResults;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.util.FileProvider;

@ExtendWith(MockitoExtension.class)
public class SnapshotRowHandlerTest {

	@Mock
	private SnapshotStore mockSnapshotStore;

	@Mock
	private FileProvider mockFileProvider;

	@Mock
	private JsonSchemaValidationManager mockValidationManager;

	@Mock
	private IndexedModelEncoderProvider mockEncoderProvider;

	@Mock
	private IndexedModelEncoder mockEncoder;

	@Mock
	private File mockFile;

	@Mock
	private OutputStream mockFileOutputStream;

	private String sessionId;
	private Long replicaId;
	private List<ColumnModel> schema;
	private List<Integer> requiredColumnIndices;
	private Long createdByUserId;

	@BeforeEach
	public void before() throws IOException {
		sessionId = "s123";
		replicaId = 19L;
		schema = List.of(new ColumnModel().setColumnType(ColumnType.STRING).setName("aString"),
				new ColumnModel().setColumnType(ColumnType.INTEGER).setName("anInt"));
		requiredColumnIndices = Collections.emptyList();
		createdByUserId = 999L;

		lenient().when(mockFileProvider.createTempFile("snapshot", ".cbor")).thenReturn(mockFile);
		lenient().when(mockFileProvider.createFileOutputStream(mockFile)).thenReturn(mockFileOutputStream);
		lenient().when(mockEncoderProvider.getEncoder(any(OutputStream.class), any(LogicalTimestamp.class)))
				.thenReturn(mockEncoder);
		lenient().when(mockEncoder.getClockTable()).thenReturn(new ClockTable(Collections.emptyList()));
	}

	@Test
	public void testBuildDocumentStructure() throws IOException {
		SnapshotRowHandler handler = new SnapshotRowHandler(mockSnapshotStore, sessionId, replicaId, schema,
				requiredColumnIndices, mockFileProvider, mockEncoderProvider, createdByUserId, mockValidationManager, null);

		// call under test
		SnapshotRowHandler.DocumentStructure result = handler.buildDocumentStructure();

		assertNotNull(result);
		assertNotNull(result.rootObjectNode);
		assertNotNull(result.documentVersionNode);
		assertNotNull(result.columnNamesNode);
		assertNotNull(result.columnOrderNode);
		assertNotNull(result.rowsNode);

		// Verify root object contains all required fields
		Map<String, LogicalTimestamp> rootMap = result.rootObjectNode.getValue();
		assertEquals(4, rootMap.size());
		assertTrue(rootMap.containsKey(DocumentConstants.DOC_VERSION));
		assertTrue(rootMap.containsKey(DocumentConstants.COLUMN_NAMES));
		assertTrue(rootMap.containsKey(DocumentConstants.COLUMN_ORDER));
		assertTrue(rootMap.containsKey(DocumentConstants.ROWS));

		// Verify document version
		assertEquals(ConType.STRING, result.documentVersionNode.getConValue().getType());
		assertEquals("0.1.0", result.documentVersionNode.getConValue().getValue());

		// Verify nodes have IDs
		assertNotNull(result.rootObjectNode.getId());
		assertNotNull(result.documentVersionNode.getId());
		assertNotNull(result.columnNamesNode.getId());
		assertNotNull(result.columnOrderNode.getId());
		assertNotNull(result.rowsNode.getId());

		// Verify replica IDs match
		assertEquals(replicaId, result.rootObjectNode.getId().getReplicaId());
		assertEquals(replicaId, result.documentVersionNode.getId().getReplicaId());

		handler.close();
	}

	@Test
	public void testBuildColumnSchemaWithEmptySchema() throws IOException {
		SnapshotRowHandler handler = new SnapshotRowHandler(mockSnapshotStore, sessionId, replicaId,
				Collections.emptyList(), requiredColumnIndices, mockFileProvider, mockEncoderProvider, createdByUserId,
				mockValidationManager, null);

		VectorNode columnNamesNode = new VectorNode().setId(new LogicalTimestamp().setReplicaId(replicaId)
				.setSequenceNumber(1L));
		ArrayNode columnOrderNode = new ArrayNode().setId(new LogicalTimestamp().setReplicaId(replicaId)
				.setSequenceNumber(2L));

		// call under test
		SnapshotRowHandler.ColumnSchemaResult result = handler.buildColumnSchema(Collections.emptyList(),
				columnNamesNode, columnOrderNode);

		assertNotNull(result);
		assertEquals(0, result.translators.length);
		assertTrue(result.nodesToWrite.isEmpty());

		handler.close();
	}

	@Test
	public void testBuildColumnSchemaWithMultipleColumns() throws IOException {
		List<ColumnModel> testSchema = List.of(
				new ColumnModel().setColumnType(ColumnType.STRING).setName("name"),
				new ColumnModel().setColumnType(ColumnType.INTEGER).setName("age"),
				new ColumnModel().setColumnType(ColumnType.BOOLEAN).setName("active"));

		SnapshotRowHandler handler = new SnapshotRowHandler(mockSnapshotStore, sessionId, replicaId, testSchema,
				requiredColumnIndices, mockFileProvider, mockEncoderProvider, createdByUserId, mockValidationManager, null);

		VectorNode columnNamesNode = new VectorNode().setId(new LogicalTimestamp().setReplicaId(replicaId)
				.setSequenceNumber(1L)).setValues(new LinkedHashMap<>());
		ArrayNode columnOrderNode = new ArrayNode().setId(new LogicalTimestamp().setReplicaId(replicaId)
				.setSequenceNumber(2L)).setElements(new ArrayList<>());

		// call under test
		SnapshotRowHandler.ColumnSchemaResult result = handler.buildColumnSchema(testSchema, columnNamesNode,
				columnOrderNode);

		assertNotNull(result);
		assertEquals(3, result.translators.length);
		assertNotNull(result.translators[0]);
		assertNotNull(result.translators[1]);
		assertNotNull(result.translators[2]);

		// Should create 2 nodes per column (name constant + index constant)
		assertEquals(6, result.nodesToWrite.size());

		// Verify column names are set
		assertEquals(3, columnNamesNode.getValues().size());
		assertTrue(columnNamesNode.getValues().containsKey(0));
		assertTrue(columnNamesNode.getValues().containsKey(1));
		assertTrue(columnNamesNode.getValues().containsKey(2));

		// Verify column order is set
		assertEquals(3, columnOrderNode.getElements().size());

		// Verify column name values
		ConstantNode name0 = columnNamesNode.getValues().get(0);
		assertEquals("name", name0.getConValue().getValue());
		ConstantNode name1 = columnNamesNode.getValues().get(1);
		assertEquals("age", name1.getConValue().getValue());
		ConstantNode name2 = columnNamesNode.getValues().get(2);
		assertEquals("active", name2.getConValue().getValue());

		handler.close();
	}

	@Test
	public void testBuildColumnSchemaRGALinking() throws IOException {
		List<ColumnModel> testSchema = List.of(
				new ColumnModel().setColumnType(ColumnType.STRING).setName("col1"),
				new ColumnModel().setColumnType(ColumnType.STRING).setName("col2"));

		SnapshotRowHandler handler = new SnapshotRowHandler(mockSnapshotStore, sessionId, replicaId, testSchema,
				requiredColumnIndices, mockFileProvider, mockEncoderProvider, createdByUserId, mockValidationManager, null);

		VectorNode columnNamesNode = new VectorNode().setId(new LogicalTimestamp().setReplicaId(replicaId)
				.setSequenceNumber(100L)).setValues(new LinkedHashMap<>());
		ArrayNode columnOrderNode = new ArrayNode().setId(new LogicalTimestamp().setReplicaId(replicaId)
				.setSequenceNumber(101L)).setElements(new ArrayList<>());

		handler.buildColumnSchema(testSchema, columnNamesNode, columnOrderNode);

		// Verify RGA linking - first element references the array node
		List<RGANode> elements = columnOrderNode.getElements();
		assertEquals(2, elements.size());
		assertEquals(columnOrderNode.getId(), elements.get(0).getReferenceNodeId());

		// Second element references the first element's data
		assertEquals(elements.get(0).getDataId(), elements.get(1).getReferenceNodeId());

		handler.close();
	}

	@Test
	public void testNextTimestampIncrementsSequence() throws IOException {
		SnapshotRowHandler handler = new SnapshotRowHandler(mockSnapshotStore, sessionId, replicaId, schema,
				requiredColumnIndices, mockFileProvider, mockEncoderProvider, createdByUserId, mockValidationManager, null);

		// call under test
		LogicalTimestamp ts1 = handler.nextTimestamp();
		LogicalTimestamp ts2 = handler.nextTimestamp();
		LogicalTimestamp ts3 = handler.nextTimestamp();

		assertEquals(replicaId, ts1.getReplicaId());
		assertEquals(replicaId, ts2.getReplicaId());
		assertEquals(replicaId, ts3.getReplicaId());

		// Sequence numbers should increment
		assertTrue(ts1.getSequenceNumber() < ts2.getSequenceNumber());
		assertTrue(ts2.getSequenceNumber() < ts3.getSequenceNumber());

		handler.close();
	}

	@Test
	public void testGetRowData() throws IOException {
		SnapshotRowHandler handler = new SnapshotRowHandler(mockSnapshotStore, sessionId, replicaId, schema,
				requiredColumnIndices, mockFileProvider, mockEncoderProvider, createdByUserId, mockValidationManager, null);

		Row row = new Row().setValues(Arrays.asList("test", "123"));
		List<Node> capturedNodes = new ArrayList<>();

		// call under test
		VectorNode result = handler.getRowData(row, capturedNodes::add);

		assertNotNull(result);
		assertNotNull(result.getId());
		assertEquals(replicaId, result.getId().getReplicaId());

		// Should capture vector node + 2 constant nodes (one per column)
		assertEquals(3, capturedNodes.size());
		assertTrue(capturedNodes.get(0) instanceof VectorNode);
		assertTrue(capturedNodes.get(1) instanceof ConstantNode);
		assertTrue(capturedNodes.get(2) instanceof ConstantNode);

		// Verify values
		Map<Integer, ConstantNode> values = result.getValues();
		assertEquals(2, values.size());
		assertTrue(values.containsKey(0));
		assertTrue(values.containsKey(1));

		handler.close();
	}

	@Test
	public void testGetRowDataWithNullValues() throws IOException {
		SnapshotRowHandler handler = new SnapshotRowHandler(mockSnapshotStore, sessionId, replicaId, schema,
				requiredColumnIndices, mockFileProvider, mockEncoderProvider, createdByUserId, mockValidationManager, null);

		Row row = new Row().setValues(Arrays.asList(null, null));
		List<Node> capturedNodes = new ArrayList<>();

		// call under test
		VectorNode result = handler.getRowData(row, capturedNodes::add);

		assertNotNull(result);
		assertEquals(2, result.getValues().size());

		// All values should still be present (as undefined or null constants)
		assertTrue(result.getValues().containsKey(0));
		assertTrue(result.getValues().containsKey(1));

		handler.close();
	}

	@Test
	public void testGetRowDataWithEmptyValues() throws IOException {
		List<ColumnModel> emptySchema = Collections.emptyList();
		SnapshotRowHandler handler = new SnapshotRowHandler(mockSnapshotStore, sessionId, replicaId, emptySchema,
				requiredColumnIndices, mockFileProvider, mockEncoderProvider, createdByUserId, mockValidationManager, null);

		Row row = new Row().setValues(Collections.emptyList());
		List<Node> capturedNodes = new ArrayList<>();

		// call under test
		VectorNode result = handler.getRowData(row, capturedNodes::add);

		assertNotNull(result);
		// Should only capture the vector node itself
		assertEquals(1, capturedNodes.size());
		assertTrue(capturedNodes.get(0) instanceof VectorNode);

		handler.close();
	}

	@Test
	public void testGetRowMetadataWithNoMetadata() throws IOException {
		SnapshotRowHandler handler = new SnapshotRowHandler(mockSnapshotStore, sessionId, replicaId, schema,
				requiredColumnIndices, mockFileProvider, mockEncoderProvider, createdByUserId, mockValidationManager, null);

		Row row = new Row().setValues(Arrays.asList("test", "123"));
		VectorNode rowDataNode = new VectorNode().setId(new LogicalTimestamp().setReplicaId(replicaId)
				.setSequenceNumber(1L));
		List<Node> capturedNodes = new ArrayList<>();

		// call under test - no metadata (no rowId, no version, no etag, no validation)
		Optional<ObjectNode> result = handler.getRowMetadata(row, rowDataNode, capturedNodes::add);

		assertFalse(result.isPresent());
		assertTrue(capturedNodes.isEmpty());

		handler.close();
	}

	@Test
	public void testGetRowMetadataWithSynapseRowOnly() throws IOException {
		SnapshotRowHandler handler = new SnapshotRowHandler(mockSnapshotStore, sessionId, replicaId, schema,
				requiredColumnIndices, mockFileProvider, mockEncoderProvider, createdByUserId, mockValidationManager, null);

		Row row = new Row().setValues(Arrays.asList("test", "123")).setRowId(1L).setVersionNumber(2L)
				.setEtag("etag-123");
		VectorNode rowDataNode = new VectorNode().setId(new LogicalTimestamp().setReplicaId(replicaId)
				.setSequenceNumber(1L));
		List<Node> capturedNodes = new ArrayList<>();

		// call under test
		Optional<ObjectNode> result = handler.getRowMetadata(row, rowDataNode, capturedNodes::add);

		assertTrue(result.isPresent());
		ObjectNode metadata = result.get();

		// Should have 2 nodes: ObjectNode + ConstantNode for synapseRow
		assertEquals(2, capturedNodes.size());

		// Verify metadata structure
		Map<String, LogicalTimestamp> metadataMap = metadata.getValue();
		assertEquals(1, metadataMap.size());
		assertTrue(metadataMap.containsKey(DocumentConstants.SYNAPSE_ROW));

		handler.close();
	}

	@Test
	public void testGetRowMetadataWithValidationOnly() throws IOException {
		JsonSchema validationSchema = new JsonSchema();
		ValidationResults validResults = new ValidationResults();
		validResults.setIsValid(true);

		when(mockValidationManager.validate(eq(validationSchema), any(JsonSubject.class)))
				.thenReturn(validResults);

		SnapshotRowHandler handler = new SnapshotRowHandler(mockSnapshotStore, sessionId, replicaId, schema,
				requiredColumnIndices, mockFileProvider, mockEncoderProvider, createdByUserId, mockValidationManager,
				validationSchema);

		Row row = new Row().setValues(Arrays.asList("test", "123"));

		// Create a proper rowDataNode with values
		Map<Integer, ConstantNode> cellValues = new LinkedHashMap<>();
		cellValues.put(0, new ConstantNode().setId(new LogicalTimestamp().setReplicaId(replicaId)
				.setSequenceNumber(10L)).setValue(new ConValue(ConType.STRING, "test")));
		cellValues.put(1, new ConstantNode().setId(new LogicalTimestamp().setReplicaId(replicaId)
				.setSequenceNumber(11L)).setValue(new ConValue(ConType.LONG, 123L)));

		VectorNode rowDataNode = new VectorNode().setId(new LogicalTimestamp().setReplicaId(replicaId)
				.setSequenceNumber(1L)).setValues(cellValues);

		List<Node> capturedNodes = new ArrayList<>();

		// call under test
		Optional<ObjectNode> result = handler.getRowMetadata(row, rowDataNode, capturedNodes::add);

		assertTrue(result.isPresent());
		ObjectNode metadata = result.get();

		// Should have 2 nodes: ObjectNode + ConstantNode for validation
		assertEquals(2, capturedNodes.size());

		// Verify metadata structure
		Map<String, LogicalTimestamp> metadataMap = metadata.getValue();
		assertEquals(1, metadataMap.size());
		assertTrue(metadataMap.containsKey(DocumentConstants.ROW_VALIDATION));

		verify(mockValidationManager, times(1)).validate(eq(validationSchema), any(JsonSubject.class));

		handler.close();
	}

	@Test
	public void testGetRowMetadataWithBothSynapseRowAndValidation() throws IOException {
		JsonSchema validationSchema = new JsonSchema();
		ValidationResults validResults = new ValidationResults();
		validResults.setIsValid(true);

		when(mockValidationManager.validate(eq(validationSchema), any(JsonSubject.class)))
				.thenReturn(validResults);

		SnapshotRowHandler handler = new SnapshotRowHandler(mockSnapshotStore, sessionId, replicaId, schema,
				requiredColumnIndices, mockFileProvider, mockEncoderProvider, createdByUserId, mockValidationManager,
				validationSchema);

		Row row = new Row().setValues(Arrays.asList("test", "123")).setRowId(1L).setVersionNumber(2L)
				.setEtag("etag-123");

		Map<Integer, ConstantNode> cellValues = new LinkedHashMap<>();
		cellValues.put(0, new ConstantNode().setId(new LogicalTimestamp().setReplicaId(replicaId)
				.setSequenceNumber(10L)).setValue(new ConValue(ConType.STRING, "test")));
		cellValues.put(1, new ConstantNode().setId(new LogicalTimestamp().setReplicaId(replicaId)
				.setSequenceNumber(11L)).setValue(new ConValue(ConType.LONG, 123L)));

		VectorNode rowDataNode = new VectorNode().setId(new LogicalTimestamp().setReplicaId(replicaId)
				.setSequenceNumber(1L)).setValues(cellValues);

		List<Node> capturedNodes = new ArrayList<>();

		// call under test
		Optional<ObjectNode> result = handler.getRowMetadata(row, rowDataNode, capturedNodes::add);

		assertTrue(result.isPresent());
		ObjectNode metadata = result.get();

		// Should have 3 nodes: ObjectNode + ConstantNode for synapseRow + ConstantNode for validation
		assertEquals(3, capturedNodes.size());

		// Verify metadata structure
		Map<String, LogicalTimestamp> metadataMap = metadata.getValue();
		assertEquals(2, metadataMap.size());
		assertTrue(metadataMap.containsKey(DocumentConstants.SYNAPSE_ROW));
		assertTrue(metadataMap.containsKey(DocumentConstants.ROW_VALIDATION));

		handler.close();
	}


	@Test
	public void testCreateValidationConstant() throws IOException {
		JsonSchema validationSchema = new JsonSchema();
		ValidationResults validResults = new ValidationResults();
		validResults.setIsValid(true);
		validResults.setObjectId("test-id");
		validResults.setObjectEtag("test-etag");

		when(mockValidationManager.validate(eq(validationSchema), any(JsonSubject.class)))
				.thenReturn(validResults);

		SnapshotRowHandler handler = new SnapshotRowHandler(mockSnapshotStore, sessionId, replicaId, schema,
				requiredColumnIndices, mockFileProvider, mockEncoderProvider, createdByUserId, mockValidationManager,
				validationSchema);

		Map<Integer, ConstantNode> cellValues = new LinkedHashMap<>();
		cellValues.put(0, new ConstantNode().setId(new LogicalTimestamp().setReplicaId(replicaId)
				.setSequenceNumber(10L)).setValue(new ConValue(ConType.STRING, "test")));
		cellValues.put(1, new ConstantNode().setId(new LogicalTimestamp().setReplicaId(replicaId)
				.setSequenceNumber(11L)).setValue(new ConValue(ConType.LONG, 123L)));

		// call under test
		ConstantNode result = handler.createValidationConstant(cellValues);

		assertNotNull(result);
		assertNotNull(result.getId());
		assertEquals(ConType.JSON_OBJECT, result.getConValue().getType());

		// Timestamp should be > 11 (highest cell value timestamp)
		assertTrue(result.getId().getSequenceNumber() > 11L);

		// Verify validation was called
		verify(mockValidationManager, times(1)).validate(eq(validationSchema), any(JsonSubject.class));

		// Verify validation results are in the constant
		JSONObject validationJson = (JSONObject) result.getConValue().getValue();
		assertNotNull(validationJson);
		assertTrue(validationJson.getBoolean("isValid"));

		handler.close();
	}

	@Test
	public void testCreateValidationConstantCleansUpResults() throws IOException {
		JsonSchema validationSchema = new JsonSchema();
		ValidationResults validResults = new ValidationResults();
		validResults.setIsValid(false);
		validResults.setObjectId("test-id");
		validResults.setObjectEtag("test-etag");
		validResults.setValidatedOn(new java.util.Date());
		validResults.setSchema$id("schema-id");
		validResults.setValidationException(new ValidationException().setMessage("Some error"));

		when(mockValidationManager.validate(eq(validationSchema), any(JsonSubject.class)))
				.thenReturn(validResults);

		SnapshotRowHandler handler = new SnapshotRowHandler(mockSnapshotStore, sessionId, replicaId, schema,
				requiredColumnIndices, mockFileProvider, mockEncoderProvider, createdByUserId, mockValidationManager,
				validationSchema);

		Map<Integer, ConstantNode> cellValues = new LinkedHashMap<>();
		cellValues.put(0, new ConstantNode().setId(new LogicalTimestamp().setReplicaId(replicaId)
				.setSequenceNumber(10L)).setValue(new ConValue(ConType.STRING, "test")));

		ConstantNode result = handler.createValidationConstant(cellValues);

		// Verify cleanup happened
		JSONObject validationJson = (JSONObject) result.getConValue().getValue();
		assertFalse(validationJson.has("validatedOn"));
		assertFalse(validationJson.has("schema$id"));
		assertFalse(validationJson.has("validationException"));

		handler.close();
	}

	@Test
	public void testFinalizeEncodingWithNoRows() throws IOException {
		SnapshotRowHandler handler = new SnapshotRowHandler(mockSnapshotStore, sessionId, replicaId, schema,
				requiredColumnIndices, mockFileProvider, mockEncoderProvider, createdByUserId, mockValidationManager, null);

		// call under test - should not throw
		handler.finalizeEncoding();

		// Verify encoder was used (columnOrderNode in init + rowsNode in finalize = 2 ArrayNode writes)
		verify(mockEncoder, times(2)).writeNode(any(ArrayNode.class));
		verify(mockEncoder, times(1)).close();

		handler.close();
	}

	@Test
	public void testFinalizeEncodingWithRows() throws IOException {
		SnapshotRowHandler handler = new SnapshotRowHandler(mockSnapshotStore, sessionId, replicaId, schema,
				requiredColumnIndices, mockFileProvider, mockEncoderProvider, createdByUserId, mockValidationManager, null);

		handler.nextRow(new Row().setValues(Arrays.asList("one", "1")));
		handler.nextRow(new Row().setValues(Arrays.asList("two", "2")));

		// call under test - should not throw
		handler.finalizeEncoding();

		// Verify encoder was used to write nodes (columnOrderNode in init + rowsNode in finalize = 2 ArrayNode writes)
		verify(mockEncoder, times(2)).writeNode(any(ArrayNode.class));
		verify(mockEncoder, times(1)).close();

		handler.close();
	}


	@Test
	public void testNoColumnsNoRows() throws IOException {
		schema = Collections.emptyList();

		SnapshotRowHandler handler = new SnapshotRowHandler(mockSnapshotStore, sessionId, replicaId, schema,
				requiredColumnIndices, mockFileProvider, mockEncoderProvider, createdByUserId, mockValidationManager, null);
		SnapshotRowHandler spyHandler = spy(handler);

		// call under test
		try (SnapshotRowHandler h = spyHandler) {
			// no row to add
		}

		// Verify internal methods were called (only methods called after construction)
		verify(spyHandler, times(1)).finalizeEncoding();
		// getRowData and getRowMetadata should not be called when no rows
		verify(spyHandler, never()).getRowData(any(Row.class), any());
		verify(spyHandler, never()).getRowMetadata(any(Row.class), any(VectorNode.class), any());

		verifyFileCreatedUploadedAndDeleted();
		verifySnapshotSaved();
	}

	@Test
	public void testWithColumnNoRows() throws IOException {
		SnapshotRowHandler handler = new SnapshotRowHandler(mockSnapshotStore, sessionId, replicaId, schema,
				requiredColumnIndices, mockFileProvider, mockEncoderProvider, createdByUserId, mockValidationManager, null);
		SnapshotRowHandler spyHandler = spy(handler);

		// call under test
		try (SnapshotRowHandler h = spyHandler) {
			// no row to add
		}

		// Verify internal methods were called (only methods called after construction)
		verify(spyHandler, times(1)).finalizeEncoding();
		// getRowData and getRowMetadata should not be called when no rows
		verify(spyHandler, never()).getRowData(any(Row.class), any());
		verify(spyHandler, never()).getRowMetadata(any(Row.class), any(VectorNode.class), any());

		verifyFileCreatedUploadedAndDeleted();
		verifySnapshotSaved();
	}

	@Test
	public void testWithRows() throws IOException {
		SnapshotRowHandler handler = new SnapshotRowHandler(mockSnapshotStore, sessionId, replicaId, schema,
				requiredColumnIndices, mockFileProvider, mockEncoderProvider, createdByUserId, mockValidationManager, null);
		SnapshotRowHandler spyHandler = spy(handler);

		Row row1 = new Row().setValues(Arrays.asList("one", "101")).setRowId(1L).setVersionNumber(4L)
				.setEtag("fake-etag-1");
		Row row2 = new Row().setValues(Arrays.asList("two", "202")).setRowId(2L).setVersionNumber(5L)
				.setEtag("fake-etag-2");
		Row row3 = new Row().setValues(Arrays.asList("three", "303")).setRowId(3L).setVersionNumber(6L)
				.setEtag("fake-etag-3");

		// call under test
		try (SnapshotRowHandler h = spyHandler) {
			h.nextRow(row1);
			h.nextRow(row2);
			h.nextRow(row3);
		}

		// Verify internal methods were called (only methods called after construction)
		verify(spyHandler, times(3)).getRowData(any(Row.class), any());
		verify(spyHandler, times(3)).getRowMetadata(any(Row.class), any(VectorNode.class), any());
		verify(spyHandler, times(1)).finalizeEncoding();

		verifyFileCreatedUploadedAndDeleted();
		verifySnapshotSaved();
	}

	@Test
	public void testEachType() throws IOException {
		boolean hasDefault = false;
		schema = TableModelTestUtils.createOneOfEachType(hasDefault);
		List<Row> rows = TableModelTestUtils.createRows(schema, 3,
				new TableModelTestUtils.ValueOptions().includeSpace(false));

		SnapshotRowHandler handler = new SnapshotRowHandler(mockSnapshotStore, sessionId, replicaId, schema,
				requiredColumnIndices, mockFileProvider, mockEncoderProvider, createdByUserId, mockValidationManager, null);
		SnapshotRowHandler spyHandler = spy(handler);

		// call under test
		try (SnapshotRowHandler h = spyHandler) {
			rows.forEach(h::nextRow);
		}

		// Verify internal methods were called correct number of times
		verify(spyHandler, times(3)).getRowData(any(Row.class), any());
		verify(spyHandler, times(3)).getRowMetadata(any(Row.class), any(VectorNode.class), any());

		verifyFileCreatedUploadedAndDeleted();
		verifySnapshotSaved();
	}

	@Test
	public void testWriteNullOrUndefinedUsingRequiredColumnIndices() throws Exception {
		requiredColumnIndices = List.of(1); // only the second column is required

		SnapshotRowHandler handler = new SnapshotRowHandler(mockSnapshotStore, sessionId, replicaId, schema,
				requiredColumnIndices, mockFileProvider, mockEncoderProvider, createdByUserId, mockValidationManager, null);
		SnapshotRowHandler spyHandler = spy(handler);

		Row row = new Row().setValues(Arrays.asList(null, null)).setRowId(1L).setVersionNumber(4L)
				.setEtag("fake-etag-1");

		// call under test
		try (SnapshotRowHandler h = spyHandler) {
			h.nextRow(row);
		}

		// Verify internal methods were called
		verify(spyHandler, times(1)).getRowData(eq(row), any());
		verify(spyHandler, times(1)).getRowMetadata(eq(row), any(VectorNode.class), any());

		verifyFileCreatedUploadedAndDeleted();
		verifySnapshotSaved();
	}

	@Test
	public void testConstructorWithNullSnapshotStore() {
		// call under test
		assertThrows(IllegalArgumentException.class, () -> {
			new SnapshotRowHandler(null, sessionId, replicaId, schema, requiredColumnIndices, mockFileProvider,
					mockEncoderProvider, createdByUserId, mockValidationManager, null);
		});
	}

	@Test
	public void testConstructorFailsToCreateTempFile() throws IOException {
		when(mockFileProvider.createTempFile("snapshot", ".cbor"))
				.thenThrow(new IOException("Failed to create temp file"));

		// call under test
		RuntimeException ex = assertThrows(RuntimeException.class, () -> {
			new SnapshotRowHandler(mockSnapshotStore, sessionId, replicaId, schema, requiredColumnIndices,
					mockFileProvider, mockEncoderProvider, createdByUserId, mockValidationManager, null);
		});

		assertEquals("Failed to create temporary file for snapshot", ex.getMessage());
	}

	@Test
	public void testWithValidationSchema() throws IOException {
		// Setup validation schema and manager
		JsonSchema validationSchema = new JsonSchema();
		ValidationResults validResults = new ValidationResults();
		validResults.setIsValid(true);
		validResults.setObjectId("test-id");
		validResults.setObjectType(null);
		validResults.setObjectEtag("test-etag");

		when(mockValidationManager.validate(eq(validationSchema), any(JsonSubject.class)))
				.thenReturn(validResults);

		SnapshotRowHandler handler = new SnapshotRowHandler(mockSnapshotStore, sessionId, replicaId, schema,
				requiredColumnIndices, mockFileProvider, mockEncoderProvider, createdByUserId, mockValidationManager,
				validationSchema);
		SnapshotRowHandler spyHandler = spy(handler);

		Row row = new Row().setValues(Arrays.asList("one", "101")).setRowId(1L).setVersionNumber(4L)
				.setEtag("fake-etag-1");

		// call under test
		try (SnapshotRowHandler h = spyHandler) {
			h.nextRow(row);
		}

		// Verify internal methods were called
		verify(spyHandler, times(1)).getRowData(eq(row), any());
		verify(spyHandler, times(1)).getRowMetadata(eq(row), any(VectorNode.class), any());

		verifyFileCreatedUploadedAndDeleted();
		verifySnapshotSaved();

		// Verify validation was called once
		verify(mockValidationManager, times(1)).validate(eq(validationSchema), any(JsonSubject.class));
	}

	@Test
	public void testWithNullValidationSchema() throws IOException {
		SnapshotRowHandler handler = new SnapshotRowHandler(mockSnapshotStore, sessionId, replicaId, schema,
				requiredColumnIndices, mockFileProvider, mockEncoderProvider, createdByUserId, mockValidationManager, null);
		SnapshotRowHandler spyHandler = spy(handler);

		Row row = new Row().setValues(Arrays.asList("one", "101")).setRowId(1L).setVersionNumber(4L)
				.setEtag("fake-etag-1");

		// call under test - null validation schema should skip validation
		try (SnapshotRowHandler h = spyHandler) {
			h.nextRow(row);
		}

		// Verify internal methods were called
		verify(spyHandler, times(1)).getRowData(eq(row), any());
		verify(spyHandler, times(1)).getRowMetadata(eq(row), any(VectorNode.class), any());

		verifyFileCreatedUploadedAndDeleted();
		verifySnapshotSaved();

		// Verify validation was NOT called
		verify(mockValidationManager, never()).validate(any(), any());
	}


	private void verifyFileCreatedUploadedAndDeleted() throws IOException {
		verify(mockFileProvider).createTempFile("snapshot", ".cbor");
		verify(mockSnapshotStore).saveSnapshot(eq(sessionId), any(ClockTable.class), eq(createdByUserId),
				eq(mockFile));
		assertTrue(!mockFile.exists(), "Temp file should be deleted after close");
	}

	private void verifySnapshotSaved() {
		ArgumentCaptor<ClockTable> clockTableCaptor = ArgumentCaptor.forClass(ClockTable.class);
		verify(mockSnapshotStore).saveSnapshot(eq(sessionId), clockTableCaptor.capture(), eq(createdByUserId),
				eq(mockFile));

		ClockTable clockTable = clockTableCaptor.getValue();
		assertNotNull(clockTable, "Clock table should not be null");
	}

}
