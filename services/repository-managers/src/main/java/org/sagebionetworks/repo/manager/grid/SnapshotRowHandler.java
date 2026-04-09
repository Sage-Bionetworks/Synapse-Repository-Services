package org.sagebionetworks.repo.manager.grid;

import static org.sagebionetworks.repo.manager.grid.internal.replica.validation.GridReplicaValidationManagerImpl.cleanupValidationResults;
import static org.sagebionetworks.repo.manager.grid.internal.replica.view.GridReplicaViewManagerImpl.gridRowToJsonObject;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.sagebionetworks.repo.manager.grid.internal.replica.validation.JsonObjectSubject;
import org.sagebionetworks.repo.manager.grid.row.translator.ColumnTypeToConType;
import org.sagebionetworks.repo.manager.grid.row.translator.Translator;
import org.sagebionetworks.repo.manager.schema.JsonSchemaValidationManager;
import org.sagebionetworks.repo.manager.schema.JsonSubject;
import org.sagebionetworks.repo.model.dao.table.RowHandler;
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
import org.sagebionetworks.repo.model.schema.ValidationResults;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.sagebionetworks.util.FileProvider;
import org.sagebionetworks.util.ValidateArgument;

/**
 * A handler that can build and save a snapshot from a table row query.
 */
public class SnapshotRowHandler implements RowHandler {

    private final FileProvider fileProvider;
    private final IndexedModelEncoderProvider encoderProvider;

    private IndexedModelEncoder encoder;
    private OutputStream encoderOutputStream;
    private final String sessionId;
    private final Translator[] translators;
    private final ArrayNode rowsArray;
    private final List<RGANode> rowRgaNodes = new ArrayList<>();
    private LogicalTimestamp lastRowRef;
    private final List<Integer> requiredColumnIndices;
    private final File snapshotFile;
    private final SnapshotStore snapshotStore;
    private final Long replicaId;
    private final Long createdByUserId;

    // Optional validation fields
    private final JsonSchema validationSchema;
    private final JsonSchemaValidationManager jsonSchemaValidationManager;
    private final List<String> columnNames;

    private long nextNodeSequenceNumber = 1;
    private boolean closed = false;

    static private Logger log = LogManager.getLogger(SnapshotRowHandler.class);

    /**
     * Simple holder class for document structure nodes
     */
    static class DocumentStructure {
        final ObjectNode rootObjectNode;
        final ConstantNode documentVersionNode;
        final VectorNode columnNamesNode;
        final ArrayNode columnOrderNode;
        final ArrayNode rowsNode;

        DocumentStructure(ObjectNode rootObjectNode, ConstantNode documentVersionNode,
                          VectorNode columnNamesNode, ArrayNode columnOrderNode,
                          ArrayNode rowsNode) {
            this.rootObjectNode = rootObjectNode;
            this.documentVersionNode = documentVersionNode;
            this.columnNamesNode = columnNamesNode;
            this.columnOrderNode = columnOrderNode;
            this.rowsNode = rowsNode;
        }
    }

    /**
     * Result from building column schema
     */
    static class ColumnSchemaResult {
        final Translator[] translators;
        final List<Node> nodesToWrite;

        ColumnSchemaResult(Translator[] translators, List<Node> nodesToWrite) {
            this.translators = translators;
            this.nodesToWrite = nodesToWrite;
        }
    }

    public SnapshotRowHandler(SnapshotStore snapshotStore, String sessionId, Long replicaId, List<ColumnModel> schema,
                              List<Integer> requiredColumnIndices, FileProvider fileProvider, IndexedModelEncoderProvider encoderProvider,
                              Long createdByUserId, JsonSchemaValidationManager jsonSchemaValidationManager, JsonSchema validationSchema) {
        super();
        ValidateArgument.required(snapshotStore, "snapshotStore");

        // Initialize fields
        this.replicaId = replicaId;
        this.snapshotStore = snapshotStore;
        this.sessionId = sessionId;
        this.requiredColumnIndices = requiredColumnIndices;
        this.createdByUserId = createdByUserId;
        this.fileProvider = fileProvider;
        this.encoderProvider = encoderProvider;

        // Validation fields
        this.validationSchema = validationSchema;
        this.jsonSchemaValidationManager = jsonSchemaValidationManager;
        this.columnNames = schema.stream().map(ColumnModel::getName).collect(Collectors.toList());

        // Create temporary file
        try {
            snapshotFile = fileProvider.createTempFile("snapshot", ".cbor");
        } catch (IOException e) {
            throw new RuntimeException("Failed to create temporary file for snapshot", e);
        }

        try {
            // Build document structure
            DocumentStructure docStructure = buildDocumentStructure();

            // Set up rows array reference (needed for nextRow)
            this.rowsArray = docStructure.rowsNode;
            this.lastRowRef = docStructure.rowsNode.getId();

            // Build column schema
            ColumnSchemaResult columnSchema = buildColumnSchema(schema,
                                                                 docStructure.columnNamesNode,
                                                                 docStructure.columnOrderNode);
            this.translators = columnSchema.translators;

            // Initialize encoder and write initial nodes
            initializeEncoderAndWriteInitialNodes(docStructure, columnSchema.nodesToWrite);

        } catch (IOException e) {
            // Clean up on failure
            if (encoderOutputStream != null) {
                try { encoderOutputStream.close(); } catch (IOException ignored) {}
            }
            if (snapshotFile != null && snapshotFile.exists()) {
                snapshotFile.delete();
            }
            throw new RuntimeException("Failed to initialize encoder and write initial nodes", e);
        }
    }

    /**
     * Builds the initial document structure with root object, version, column names, and rows array.
     *
     * @return A DocumentStructure object containing the created nodes
     */
    DocumentStructure buildDocumentStructure() {
        ObjectNode rootObjectNode = new ObjectNode().setId(nextTimestamp());
        ConstantNode documentVersionNode = new ConstantNode()
            .setId(nextTimestamp())
            .setValue(new ConValue(ConType.STRING, "0.1.0"));
        VectorNode columnNamesNode = new VectorNode()
            .setId(nextTimestamp())
            .setValues(new LinkedHashMap<>());
        ArrayNode columnOrderNode = new ArrayNode()
            .setId(nextTimestamp())
            .setElements(new ArrayList<>());
        ArrayNode rowsNode = new ArrayNode()
            .setId(nextTimestamp())
            .setElements(new ArrayList<>());

        Map<String, LogicalTimestamp> objectMap = new LinkedHashMap<>();
        objectMap.put(DocumentConstants.DOC_VERSION, documentVersionNode.getId());
        objectMap.put(DocumentConstants.COLUMN_NAMES, columnNamesNode.getId());
        objectMap.put(DocumentConstants.COLUMN_ORDER, columnOrderNode.getId());
        objectMap.put(DocumentConstants.ROWS, rowsNode.getId());
        rootObjectNode.setValue(objectMap);

        return new DocumentStructure(
            rootObjectNode,
            documentVersionNode,
            columnNamesNode,
            columnOrderNode,
            rowsNode
        );
    }

    /**
     * Builds the column schema nodes from the provided column models.
     *
     * @param schema The list of column models
     * @param columnNamesNode The vector node that will hold column names
     * @param columnOrderNode The array node that will hold column order
     * @return A ColumnSchemaResult containing the translators and nodes to write
     */
    ColumnSchemaResult buildColumnSchema(List<ColumnModel> schema,
                                          VectorNode columnNamesNode,
                                          ArrayNode columnOrderNode) {
        if (schema.isEmpty()) {
            return new ColumnSchemaResult(new Translator[0], Collections.emptyList());
        }

        Translator[] translators = new Translator[schema.size()];
        List<Node> nodesToWrite = new ArrayList<>();
        Map<Integer, ConstantNode> columnNameMap = new LinkedHashMap<>();
        List<RGANode> indexArrayNodes = new ArrayList<>();
        LogicalTimestamp previousRgaNodeId = columnOrderNode.getId();

        for (int i = 0; i < schema.size(); i++) {
            ColumnModel cm = schema.get(i);

            // Column name
            ConstantNode nameConstNode = new ConstantNode()
                .setId(nextTimestamp())
                .setValue(new ConValue(ConType.STRING, cm.getName()));
            columnNameMap.put(i, nameConstNode);
            nodesToWrite.add(nameConstNode);

            // Column index
            ConstantNode columnIndexNode = new ConstantNode()
                .setId(nextTimestamp())
                .setValue(new ConValue(ConType.LONG, i));
            RGANode rgaNode = new RGANode()
                .setNodeId(nextTimestamp())
                .setDataId(columnIndexNode.getId())
                .setReferenceNodeId(previousRgaNodeId)
                .setIsDeleted(false);
            previousRgaNodeId = rgaNode.getDataId();
            indexArrayNodes.add(rgaNode);
            nodesToWrite.add(columnIndexNode);

            translators[i] = ColumnTypeToConType.lookUpType(cm.getColumnType()).getTranslator();
        }

        columnNamesNode.setValues(columnNameMap);
        columnOrderNode.setElements(indexArrayNodes);

        return new ColumnSchemaResult(translators, nodesToWrite);
    }

    /**
     * Initializes the encoder and writes the initial document structure to the snapshot file.
     *
     * @param documentStructure The document structure to write
     * @param columnSchemaNodes The column schema nodes to write
     * @throws IOException if writing fails
     */
    void initializeEncoderAndWriteInitialNodes(DocumentStructure documentStructure,
                                                List<Node> columnSchemaNodes) throws IOException {
        this.encoderOutputStream = fileProvider.createFileOutputStream(snapshotFile);
        this.encoder = encoderProvider.getEncoder(encoderOutputStream, documentStructure.rootObjectNode.getId());

        // Write initial document structure nodes (excluding rowsNode which is written at the end)
        encoder.writeNode(documentStructure.rootObjectNode);
        encoder.writeNode(documentStructure.documentVersionNode);
        encoder.writeNode(documentStructure.columnNamesNode);
        encoder.writeNode(documentStructure.columnOrderNode);

        // Write column schema nodes
        for (Node node : columnSchemaNodes) {
            encoder.writeNode(node);
        }
    }


    /**
     * Adds the RowMetadata object to the patch. The row metadata has the following pseudo-schema. Fields that can be
     * undefined are not guaranteed to be present.
     * <p>
     * ```
     * obj({
     * rowValidation: s.const(json_object) | undefined
     * synapseRow: s.const(json_array) | undefined
     * })
     * ```
     * The rowValidation metadata is included when a validation schema is provided.
     * The synapseRow metadata is a constant with a serialized JSON array that contains 3 values in order:
     * <p>
     * [<rowId>, <versionNumber>, <etag>]
     *
     * @param row the table query Row for which metadata should be extracted
     * @param rowDataNode the VectorNode containing the row data ConstantNodes (used for validation)
     * @param nodeConsumer consumer to collect created nodes
     * @return a reference to the object node containing the row metadata if metadata is present, an empty Optional otherwise.
     */
    Optional<ObjectNode> getRowMetadata(Row row, VectorNode rowDataNode, Consumer<Node> nodeConsumer) {
        boolean hasSynapseRow = row.getRowId() != null || row.getVersionNumber() != null || row.getEtag() != null;
        boolean hasValidation = validationSchema != null && jsonSchemaValidationManager != null;

        if (!hasSynapseRow && !hasValidation) {
            return Optional.empty();
        }

        ObjectNode metadataObject = new ObjectNode().setId(nextTimestamp());
        nodeConsumer.accept(metadataObject);

        Map<String, LogicalTimestamp> metadataMap = new LinkedHashMap<>();

        // Add synapseRow if present
        if (hasSynapseRow) {
            JSONArray synapseRowArray = new JSONArray();
            synapseRowArray.put(row.getRowId() != null ? row.getRowId() : JSONObject.NULL);
            synapseRowArray.put(row.getVersionNumber() != null ? row.getVersionNumber() : JSONObject.NULL);
            synapseRowArray.put(row.getEtag() != null ? row.getEtag() : JSONObject.NULL);
            ConstantNode synapseRowMetadata = new ConstantNode()
                    .setId(nextTimestamp())
                    .setValue(new ConValue(ConType.JSON_ARRAY, synapseRowArray));
            nodeConsumer.accept(synapseRowMetadata);
            metadataMap.put(DocumentConstants.SYNAPSE_ROW, synapseRowMetadata.getId());
        }

        // Add validation result - MUST be after all data constants to have a higher timestamp
        if (hasValidation) {
            ConstantNode validationConstant = createValidationConstant(rowDataNode.getValues());
            nodeConsumer.accept(validationConstant);
            metadataMap.put(DocumentConstants.ROW_VALIDATION, validationConstant.getId());
        }

        metadataObject.setValue(metadataMap);
        return Optional.of(metadataObject);
    }

    /**
     * Creates a validation constant node by validating the row data against the schema.
     * The timestamp of this constant will be greater than all data constant timestamps,
     * which prevents unnecessary re-validation by GridReplicaValidationWorker.
     *
     * @param cellValues the map of column index to ConstantNode values
     * @return a ConstantNode containing the validation results as a JSON object
     */
    ConstantNode createValidationConstant(Map<Integer, ConstantNode> cellValues) {
        // Convert Map<Integer, ConstantNode> to List<ConstantNode> ordered by index
        List<ConstantNode> orderedNodes = IntStream.range(0, columnNames.size())
                .mapToObj(i -> cellValues.get(i))
                .collect(Collectors.toList());

        // Build JSON from constants
        JSONObject rowJson = gridRowToJsonObject(columnNames, orderedNodes);

        // Create JsonSubject for validation
        JsonSubject subject = new JsonObjectSubject(rowJson);

        // Validate
        ValidationResults results = jsonSchemaValidationManager.validate(validationSchema, subject);

        cleanupValidationResults(results);

        // Serialize and create constant - timestamp will be > all data constants
        try {
            JSONObject validationJson = EntityFactory.createJSONObjectForEntity(results);
            return new ConstantNode()
                    .setId(nextTimestamp())
                    .setValue(new ConValue(ConType.JSON_OBJECT, validationJson));
        } catch (JSONObjectAdapterException e) {
            throw new RuntimeException("Failed to serialize validation results", e);
        }
    }

    /**
     * Creates and returns a NewVector containing the values for the row.
     *
     * @param row the table query Row for which Synapse Row metadata should be created
     * @return a reference to the vector node containing the row values.
     */
    VectorNode getRowData(Row row, Consumer<Node> nodeConsumer) {
        VectorNode rowVector = new VectorNode().setId(nextTimestamp());
        nodeConsumer.accept(rowVector);

        Map<Integer, ConstantNode> cellValues = new LinkedHashMap<>();
        for (int i = 0; i < row.getValues().size(); i++) {
            String cellValue = row.getValues().get(i);
            ConstantNode valueConstantNode = new ConstantNode().setId(nextTimestamp()).setValue(translators[i].translateNullable(cellValue, requiredColumnIndices.contains(i)));
            nodeConsumer.accept(valueConstantNode);
            cellValues.put(i, valueConstantNode);
        }
        if (!cellValues.isEmpty()) {
            rowVector.setValues(cellValues);
        }
        return rowVector;
    }

    @Override
    public void nextRow(Row row) {
        List<Node> newNodes = new ArrayList<>();
        ObjectNode rowObject = new ObjectNode().setId(nextTimestamp());
        newNodes.add(rowObject);

        VectorNode rowDataNode = getRowData(row, newNodes::add);

        Map<String, LogicalTimestamp> rowObjectMap = new LinkedHashMap<>();

        rowObjectMap.put(DocumentConstants.DATA, rowDataNode.getId());

        // Pass VectorNode for validation - it contains the ConstantNodes via getValues()
        getRowMetadata(row, rowDataNode, newNodes::add)
                .ifPresent(rowMetadata -> rowObjectMap.put(DocumentConstants.METADATA, rowMetadata.getId()));

        rowObject.setValue(rowObjectMap);

        // Create a new RGA node for the row object
        RGANode rowRgaNode = new RGANode()
                .setNodeId(nextTimestamp())
                .setDataId(rowObject.getId())
                .setReferenceNodeId(lastRowRef)
                .setIsDeleted(false);
        rowRgaNodes.add(rowRgaNode);

        lastRowRef = rowRgaNode.getDataId();

        // flush to the encoder
        try {
            for (Node node : newNodes) {
                encoder.writeNode(node);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to write node to snapshot file", e);
        }
    }

    void finalizeEncoding() {
        try {
            rowsArray.setElements(rowRgaNodes);
            encoder.writeNode(rowsArray);
        } catch (IOException e) {
            throw new RuntimeException("Failed to finalize encoding snapshot", e);
        } finally {
            try {
                encoder.close();
            } catch (IOException e) {
                throw new RuntimeException("Failed to close snapshot encoder", e);
            } finally {
                // Ensure stream is closed even if encoder.close() fails
                try {
                    if (encoderOutputStream != null) {
                        encoderOutputStream.close();
                    }
                } catch (IOException e) {
                    log.error("Failed to close encoder output stream", e);
                }
                closed = true;
            }
        }
    }


    @Override
    public void close() throws IOException {
        if (closed) {
            return;
        }
        try {
            finalizeEncoding();
            snapshotStore.saveSnapshot(this.sessionId, this.encoder.getClockTable(), createdByUserId, snapshotFile);
        } finally {
            // Delete the file on disk
            if (snapshotFile != null && snapshotFile.exists()) {
                if (!snapshotFile.delete()) {
                    log.error("Failed to delete temporary snapshot file: " + snapshotFile.getAbsolutePath());
                }
            }
        }
    }

    LogicalTimestamp nextTimestamp() {
        return new LogicalTimestamp()
                .setReplicaId(this.replicaId)
                .setSequenceNumber(this.nextNodeSequenceNumber++);
    }

}
