package org.sagebionetworks.repo.manager.grid.synch.row;

import java.util.Optional;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.sagebionetworks.repo.manager.grid.synch.core.SourceReader;
import org.sagebionetworks.repo.manager.grid.synch.io.RowSourceItemReader;
import org.sagebionetworks.repo.manager.grid.synch.io.RowSourceItemReference;

/**
 * Read-only, disk-backed view of the source rows for Phase 2 synchronization.
 * Rows are consumed by key as they are matched to copy rows; after Phase 1 the
 * remaining rows are those that exist only in the source.
 */
public class RowSourceReader implements SourceReader<RowSourceItemReference> {

	private final RowSourceItemReader rowReader;

	public RowSourceReader(RowSourceItemReader rowReader) {
		this.rowReader = rowReader;
	}

	@Override
	public Optional<RowSourceItemReference> consume(String key) {
		return rowReader.consumeRow(key);
	}

	@Override
	public Stream<RowSourceItemReference> streamRemaining() {
		return StreamSupport.stream(Spliterators.spliteratorUnknownSize(rowReader.remainingRows(), Spliterator.ORDERED),
				false);
	}

}
