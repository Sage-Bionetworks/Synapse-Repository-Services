package org.sagebionetworks.repo.manager.grid;

import java.io.OutputStream;

import org.sagebionetworks.repo.model.grid.encoding.IndexedModelEncoder;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.springframework.stereotype.Component;

@Component
public class IndexedModelEncoderProviderImpl implements IndexedModelEncoderProvider {

    @Override
    public IndexedModelEncoder getEncoder(OutputStream out, LogicalTimestamp rootNodeId) {
        return new IndexedModelEncoder(out, rootNodeId);
    }

}
