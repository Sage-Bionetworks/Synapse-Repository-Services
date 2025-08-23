package org.sagebionetworks.grid.db;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.repo.model.grid.GridUtils;
import org.sagebionetworks.repo.model.grid.patch.ConType;
import org.sagebionetworks.repo.model.grid.patch.ConValue;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.repo.model.grid.patch.Patch;
import org.sagebionetworks.repo.model.grid.patch.operation.builder.Operations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

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

    private Patch patch;
    private final Random random = new Random(System.currentTimeMillis());
    private final String sessionId = GridUtils.gridSessionIdAsString(Math.abs(random.nextLong()));
    private final Long replicaId = Math.abs(random.nextLong());

    private final List<Patch> patches = new ArrayList<>();


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

}
