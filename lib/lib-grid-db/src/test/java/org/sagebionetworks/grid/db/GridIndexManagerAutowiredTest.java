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
import org.sagebionetworks.repo.model.grid.patch.operation.InsertArray;
import org.sagebionetworks.repo.model.grid.patch.operation.InsertObject;
import org.sagebionetworks.repo.model.grid.patch.operation.InsertVector;
import org.sagebionetworks.repo.model.grid.patch.operation.NewArray;
import org.sagebionetworks.repo.model.grid.patch.operation.NewConstant;
import org.sagebionetworks.repo.model.grid.patch.operation.NewObject;
import org.sagebionetworks.repo.model.grid.patch.operation.NewVector;
import org.sagebionetworks.repo.model.grid.patch.operation.immutable.ImmutableOperation;
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
    Random random = new Random(System.currentTimeMillis());
    String sessionId = GridUtils.gridSessionIdAsString(random.nextLong());
    Long replicaId = random.nextLong();

    private List<Patch> patches = new ArrayList<>();


    private void savePatch() {
        patches.add(patch);
        LogicalTimestamp prevPatchId = patch.getPatchId();
        long prevPatchSpan = patch.getSpan();
        patch = new Patch().setPatchId(LogicalTimestamp.newIncrement(prevPatchId, prevPatchSpan + 1));
    }

    private void applyPatches() {
        log.info("Applying patches", patches.size());
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
        ImmutableOperation<NewObject> obj = patch.addNewOperation(NewObject.class);
        ImmutableOperation<NewArray> rows = patch.addNewOperation(NewArray.class);
        lastRowRef = rows.getOperationId();
        patch.addNewOperation(new InsertObject().setObjectId(obj.getOperationId())
                .setMap(Collections.singletonMap("rows", rows.getOperationId())));

        savePatch();
        for (long j = 0; j < nRow; j++) {
            ImmutableOperation<NewVector> row = patch.addNewOperation(NewVector.class);
            Map<Integer, LogicalTimestamp> cellValues = new LinkedHashMap<>();
            for (int i = 0; i < nCol; i++) {
                ImmutableOperation<NewConstant> newConstant = patch.addNewOperation(new NewConstant().setValue(new ConValue(ConType.STRING, i + "-" + j)));
                cellValues.put(i, newConstant.getOperationId());
            }
            patch.addNewOperation(new InsertVector().setVectorId(row.getOperationId())
                    .setMap(cellValues))
            ;

            ImmutableOperation<InsertArray> insertArrayOperation = patch.addNewOperation(new InsertArray().setArrayId(rows.getOperationId())
                    .setReferenceId(lastRowRef)
                    .setElementIds(Collections.singletonList(row.getOperationId()))
            );

            if (j % rowsPerPatch == 0) {
                log.info("Saving row {} out of {}", j, nRow);
                savePatch();
            }

            lastRowRef = insertArrayOperation.getOperationId();
        }
        savePatch();

        // Once all patches have been created, apply them.
        applyPatches();
    }

}
