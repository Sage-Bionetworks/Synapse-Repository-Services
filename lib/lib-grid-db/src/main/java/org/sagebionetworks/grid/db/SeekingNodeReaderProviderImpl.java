package org.sagebionetworks.grid.db;

import java.io.IOException;
import java.nio.file.Path;

import org.sagebionetworks.repo.model.grid.encoding.SnapshotFileIndex;
import org.sagebionetworks.repo.model.grid.encoding.SeekingNodeReader;
import org.springframework.stereotype.Component;

@Component
public class SeekingNodeReaderProviderImpl implements SeekingNodeReaderProvider {

	@Override
	public SeekingNodeReader create(Path snapshotFile, SnapshotFileIndex index) throws IOException {
		return new SeekingNodeReader(snapshotFile, index);
	}
}
