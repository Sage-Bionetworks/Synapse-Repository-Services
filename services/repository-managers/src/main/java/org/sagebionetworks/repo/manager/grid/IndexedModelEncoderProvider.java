package org.sagebionetworks.repo.manager.grid;

import java.io.OutputStream;

import org.sagebionetworks.repo.model.grid.encoding.IndexedModelEncoder;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;

public interface IndexedModelEncoderProvider {

    IndexedModelEncoder getEncoder(OutputStream out, LogicalTimestamp rootNodeId);

}
