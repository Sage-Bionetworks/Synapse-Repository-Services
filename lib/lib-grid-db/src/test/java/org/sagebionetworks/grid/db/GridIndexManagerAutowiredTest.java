package org.sagebionetworks.grid.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.sagebionetworks.repo.model.grid.ClockTable;
import org.sagebionetworks.repo.model.grid.GridUtils;
import org.sagebionetworks.repo.model.grid.encoding.IndexedModelEncoder;
import org.sagebionetworks.repo.model.grid.node.ArrayNode;
import org.sagebionetworks.repo.model.grid.node.ConstantNode;
import org.sagebionetworks.repo.model.grid.node.ObjectNode;
import org.sagebionetworks.repo.model.grid.node.RGANode;
import org.sagebionetworks.repo.model.grid.node.VectorNode;
import org.sagebionetworks.repo.model.grid.patch.ConType;
import org.sagebionetworks.repo.model.grid.patch.ConValue;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.repo.model.grid.patch.Patch;
import org.sagebionetworks.repo.model.grid.patch.operation.builder.Operations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {"classpath:grid-db-test-context.xml"})
public class GridIndexManagerAutowiredTest {

    private static final Logger log = LogManager.getLogger(GridIndexManagerAutowiredTest.class);

    @Autowired
    private GridIndexManager gridIndexManager;

    @Autowired
    private GridIndexDao gridIndexDao;

    private Patch patch;
    private final Random random = new Random(System.currentTimeMillis());
    private String sessionId = GridUtils.gridSessionIdAsString(Math.abs(random.nextLong()));
    private final Long replicaId = Math.abs(random.nextLong());

    private final List<Patch> patches = new ArrayList<>();

    @AfterEach
    public void after() {
        gridIndexDao.truncateAll();
    }


    private void savePatch() {
        patches.add(patch);
        LogicalTimestamp prevPatchId = patch.getPatchId();
        long prevPatchSpan = patch.getSpan();
        patch = new Patch().setPatchId(LogicalTimestamp.newIncrement(prevPatchId, prevPatchSpan + 1));
    }

    private void applyPatches() {
        log.info("Applying patches: {}", patches.size());
        for (int i = 0; i < patches.size(); i++) {
            Patch p = patches.get(i);
            if (i % 5 == 0) {
                log.info("Applying patch {} of {}: {}", i + 1, patches.size(), p.getPatchId());
            }
            gridIndexManager.applyPatch(sessionId, replicaId, p);
        }
    }


    @Test
    @Timeout(value = 5, unit = TimeUnit.MINUTES)
    @Disabled
    public void testScalabilityRequirements() {
        // PLFM-9032 - We aim to validate that we can handle 10M cells in under 5 minutes.
        long nCol = 100; // target 100
        long nRow = 100_000; // target 100_000
        long rowsPerPatch = 100; // More rows per patch results in fewer database calls, but requires more memory.

        // Create the patches
        LogicalTimestamp lastRowRef;
        patch = new Patch().setPatchId(new LogicalTimestamp().setReplicaId(replicaId).setSequenceNumber(0L));
        LogicalTimestamp objRef = patch.addNewOperation(Operations.newObject());
        LogicalTimestamp rowsArrayRef = patch.addNewOperation(Operations.newArray());
        lastRowRef = rowsArrayRef;
        patch.addNewOperation(
                Operations.insertObject()
                        .setObjectId(objRef)
                        .setMap(Collections.singletonMap("rows", rowsArrayRef))
        );

        savePatch();
        for (long j = 0; j < nRow; j++) {
            LogicalTimestamp rowDataRef = patch.addNewOperation(Operations.newVector());
            Map<Integer, LogicalTimestamp> cellValues = new LinkedHashMap<>();
            for (int i = 0; i < nCol; i++) {
                LogicalTimestamp newConstantRef = patch.addNewOperation(Operations.newConstant().setValue(new ConValue(ConType.STRING, i + "-" + j)));
                cellValues.put(i, newConstantRef);
            }
            patch.addNewOperation(Operations.insertVector()
                    .setVectorId(rowDataRef)
                    .setMap(cellValues)
            );

            LogicalTimestamp insertArrayOperationRef = patch.addNewOperation(Operations.insertArray()
                    .setArrayId(rowsArrayRef)
                    .setReferenceId(lastRowRef)
                    .setElementIds(Collections.singletonList(rowDataRef))
            );

            if (j % rowsPerPatch == 0) {
                log.info("Saving row {} out of {}", j, nRow);
                savePatch();
            }

            lastRowRef = insertArrayOperationRef;
        }
        savePatch();

        // Once all patches have been created, apply them.
        applyPatches();
    }

    /**
     * Round-trip test: apply patches to replica A, export a snapshot,
     * then apply the exported snapshot to replica B and verify state matches.
     */
    @Test
    @Timeout(value = 1, unit = TimeUnit.MINUTES)
    public void testExportSnapshotRoundTrip(@TempDir Path tempDir) {
        // Replica IDs must fit within 57 bits for CBOR encoding
        Long replicaA = Math.abs(random.nextLong()) % (1L << 57);
        Long replicaB = Math.abs(random.nextLong()) % (1L << 57);

        // Build and apply patches to replica A
        patch = new Patch().setPatchId(new LogicalTimestamp().setReplicaId(replicaA).setSequenceNumber(0L));
        LogicalTimestamp objRef = patch.addNewOperation(Operations.newObject());
        LogicalTimestamp rowsArrayRef = patch.addNewOperation(Operations.newArray());
        LogicalTimestamp lastRowRef = rowsArrayRef;
        patch.addNewOperation(
                Operations.insertObject()
                        .setObjectId(objRef)
                        .setMap(Collections.singletonMap("rows", rowsArrayRef))
        );
        // Wire the root value node (0,0) to point to the root object
        patch.addNewOperation(
                Operations.insertValue()
                        .setValueId(new LogicalTimestamp().setReplicaId(0L).setSequenceNumber(0L))
                        .setReferenceId(objRef)
        );
        savePatch();

        // Add a few rows with cells
        int nRows = 5;
        int nCols = 3;
        for (int j = 0; j < nRows; j++) {
            LogicalTimestamp rowDataRef = patch.addNewOperation(Operations.newVector());
            Map<Integer, LogicalTimestamp> cellValues = new LinkedHashMap<>();
            for (int i = 0; i < nCols; i++) {
                LogicalTimestamp constRef = patch.addNewOperation(
                        Operations.newConstant().setValue(new ConValue(ConType.STRING, "cell-" + i + "-" + j)));
                cellValues.put(i, constRef);
            }
            patch.addNewOperation(Operations.insertVector()
                    .setVectorId(rowDataRef)
                    .setMap(cellValues)
            );
            LogicalTimestamp insertRef = patch.addNewOperation(Operations.insertArray()
                    .setArrayId(rowsArrayRef)
                    .setReferenceId(lastRowRef)
                    .setElementIds(Collections.singletonList(rowDataRef))
            );
            lastRowRef = insertRef;
            savePatch();
        }

        // Apply all patches to replica A
        for (Patch p : patches) {
            gridIndexManager.applyPatch(sessionId, replicaA, p);
        }

        // Export replica A's state
        Path exportedFile = tempDir.resolve("exported-snapshot.cbor");
        ClockTable exportedClock = gridIndexManager.exportSnapshot(sessionId, replicaA, exportedFile);
        assertNotNull(exportedClock);

        // Create replica B
        gridIndexManager.startMessageChain(sessionId, replicaB, "testExportSnapshotRoundTrip");
        // Apply the exported snapshot to replica B
        gridIndexManager.applySnapshot(sessionId, replicaB, exportedFile);

        // Compare clocks
        List<LogicalTimestamp> clockA = gridIndexManager.getClock(sessionId, replicaA);
        List<LogicalTimestamp> clockB = gridIndexManager.getClock(sessionId, replicaB);
        assertEquals(clockA, clockB, "Replica A and B should have identical clocks after round-trip");
    }

    /**
     * Scale benchmark for {@link GridIndexManager#exportSnapshot}.
     * <p>
     * Instead of building the DB state by applying patches (which is slow and unrelated to
     * what we want to measure), this test:
     * <ol>
     *   <li>Constructs a snapshot file of the desired size directly with
     *       {@link IndexedModelEncoder} — O(rows*cols) but no DB calls.</li>
     *   <li>Imports the snapshot via {@link GridIndexManager#applySnapshot}.</li>
     *   <li>Benchmarks {@link GridIndexManager#exportSnapshot} over {@code maxRuns} runs.</li>
     * </ol>
     */
    @Test
    @Disabled("This is a manual benchmark, not a unit test")
    public void benchmarkApplyAndExportSnapshot(@TempDir Path tempDir) throws Exception {
        int maxRuns = 2;
        gridIndexDao.truncateAll();

        for (int nCols = 10; nCols <= 50; nCols += 10) {
            for (int nRows = 10; nRows <= 10_000; nRows *= 2) {
                sessionId = GridUtils.gridSessionIdAsString(Math.abs(random.nextLong()));
                // Replica IDs must fit within 57 bits for CBOR encoding
                Long replicaA = Math.abs(random.nextLong()) % (1L << 57);

                // Build the snapshot file directly — no patch application needed
                String label = nRows + "-" + nCols;
                log.info("Building snapshot file: rows={}, cols={}", nRows, nCols);
                long buildStart = System.nanoTime();
                Path snapshotInput = buildSnapshotFile(tempDir, label, replicaA, nRows, nCols);
                long buildMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - buildStart);
                log.info("Snapshot file built in {} ms, size={} bytes", buildMs, Files.size(snapshotInput));

                // Populate the replica by importing the snapshot
                log.info("Importing snapshot into replica A");
                long importStart = System.nanoTime();
                gridIndexManager.applySnapshot(sessionId, replicaA, snapshotInput);
                long importMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - importStart);
                log.info("applySnapshot (setup) duration={} ms, rows={}, cols={}", importMs, nRows, nCols);

                // Benchmark exportSnapshot
                for (int run = 0; run < maxRuns; run++) {
                    Path exportedFile = tempDir.resolve(label + "-" + run + "-exported-snapshot.cbor");
                    long startNano = System.nanoTime();
                    ClockTable exportedClock = gridIndexManager.exportSnapshot(sessionId, replicaA, exportedFile);
                    long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNano);
                    assertNotNull(exportedClock);
                    long fileSize = Files.exists(exportedFile) ? Files.size(exportedFile) : 0L;
                    log.info("exportSnapshot duration={} ms, exported file='{}', size={} bytes, rows={}, cols={}",
                            durationMs, exportedFile.toAbsolutePath(), fileSize, nRows, nCols);
                }

                gridIndexDao.truncateAll();
            }
        }
    }

    /**
     * Builds a snapshot file directly with {@link IndexedModelEncoder} — no patch application —
     * and uses {@link GridIndexManager#applySnapshot} to populate the replica.
     * <p>
     * The document structure written is:
     * <pre>
     *   root ObjectNode { "rows" → rowsArrayNode }
     *   rowsArrayNode: ArrayNode[ row0VecNode, row1VecNode, … ]
     *   rowNVecNode: VectorNode{ 0→con_0_N, 1→con_1_N, … }
     * </pre>
     * Timestamp allocation (all from {@code replicaId}):
     * <ul>
     *   <li>seq 0 — root ObjectNode</li>
     *   <li>seq 1 — rows ArrayNode</li>
     *   <li>per row (seq base = 2 + row * (nCols + 2)):
     *     <ul>
     *       <li>+0 — RGA insert node (the array slot)</li>
     *       <li>+1 … +nCols — ConstantNodes for columns 0…nCols-1</li>
     *       <li>+nCols+1 — VectorNode for the row</li>
     *     </ul>
     *   </li>
     * </ul>
     */
    private Path buildSnapshotFile(Path dir, String label, long replicaId, int nRows, int nCols) throws IOException {
        // Monotonically increasing sequence counter; all timestamps share replicaId.
        long[] seq = {0L};

        LogicalTimestamp objId = new LogicalTimestamp().setReplicaId(replicaId).setSequenceNumber(seq[0]++);
        LogicalTimestamp arrId = new LogicalTimestamp().setReplicaId(replicaId).setSequenceNumber(seq[0]++);

        Path snapshotFile = dir.resolve(label + "-snapshot.cbor");
        try (OutputStream out = Files.newOutputStream(snapshotFile);
             IndexedModelEncoder encoder = new IndexedModelEncoder(out, objId)) {

            // Root ObjectNode: { "rows" → arrId }
            encoder.writeNode(new ObjectNode()
                    .setId(objId)
                    .setValue(Collections.singletonMap("rows", arrId)));

            // Collect RGA elements; the ArrayNode is written last once all are known.
            List<RGANode> rgaElements = new ArrayList<>(nRows);
            LogicalTimestamp prevRgaRef = arrId; // first element's predecessor is the array head

            for (int row = 0; row < nRows; row++) {
                // RGA insert node for this row (the array slot itself)
                LogicalTimestamp rgaNodeId = new LogicalTimestamp().setReplicaId(replicaId).setSequenceNumber(seq[0]++);

                // ConstantNodes for each column
                Map<Integer, ConstantNode> vecSlots = new LinkedHashMap<>();
                for (int col = 0; col < nCols; col++) {
                    LogicalTimestamp conId = new LogicalTimestamp().setReplicaId(replicaId).setSequenceNumber(seq[0]++);
                    ConstantNode con = new ConstantNode()
                            .setId(conId)
                            .setValue(new ConValue(ConType.STRING, col + "-" + row));
                    encoder.writeNode(con);
                    vecSlots.put(col, new ConstantNode().setId(conId)); // stub for VectorNode
                }

                // VectorNode for this row
                LogicalTimestamp vecId = new LogicalTimestamp().setReplicaId(replicaId).setSequenceNumber(seq[0]++);
                encoder.writeNode(new VectorNode()
                        .setId(vecId)
                        .setValues(vecSlots));

                // RGA element: slot in the rows array, data = vecId
                rgaElements.add(new RGANode()
                        .setContainerId(arrId)
                        .setNodeId(rgaNodeId)
                        .setDataId(vecId)
                        .setReferenceNodeId(prevRgaRef)
                        .setIsDeleted(false));
                prevRgaRef = rgaNodeId;
            }

            // Write the rows ArrayNode last (all elements collected)
            encoder.writeNode(new ArrayNode()
                    .setId(arrId)
                    .setElements(rgaElements));
        }
        return snapshotFile;
    }
}
