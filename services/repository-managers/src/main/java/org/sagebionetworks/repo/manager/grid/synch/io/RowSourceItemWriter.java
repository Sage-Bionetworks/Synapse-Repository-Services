package org.sagebionetworks.repo.manager.grid.synch.io;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Writes source rows to disk during Phase 2 (row synchronization) and creates
 * pointers for later retrieval. This enables O(n) memory usage by keeping row
 * data on disk instead of loading all source rows into memory.
 *
 * <p>
 * During synchronization, as source rows are read from the source handler, they
 * are serialized to disk using this writer. Each write operation returns a
 * {@link DiskPointer} that can later be used by {@link RowSourceItemReader} to:
 * <ul>
 * <li>Quickly locate the row on disk using offset and length</li>
 * <li>Verify data integrity using the hash</li>
 * <li>Match rows between copy and source using the key</li>
 * <li>Load the row data only when needed for comparison or merging</li>
 * </ul>
 */
public class RowSourceItemWriter implements AutoCloseable {

	private final OutputStream out;
	private long position;

	/**
	 * Creates a new writer that serializes rows to disk.
	 *
	 * @param out the output stream to write serialized row data to
	 */
	public RowSourceItemWriter(OutputStream out) {
		this.out = out;
	}

	/**
	 * Writes a row to disk and returns a pointer for later retrieval. The pointer
	 * contains all metadata needed to locate and verify the row data on disk.
	 *
	 * <p>
	 * As rows are written sequentially, this method tracks the current file
	 * position to calculate the offset for each row. The returned
	 * {@link DiskPointer} can be used by {@link RowSourceItemReader} to retrieve the row
	 * later during synchronization.
	 *
	 * @param row the source row to write to disk
	 * @return pointer containing the row's key, hash, file offset, and data length
	 * @throws RuntimeException if writing to disk fails
	 */
	public DiskPointer nextRow(RowSourceItem row) {
		try {
			long startOffset = position;
			byte[] bytes = row.getBytes();
			out.write(bytes);
			position += bytes.length;
			return new DiskPointer(row.getKey(), row.getHash(), startOffset, bytes.length);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void close() throws IOException {
		out.close();
	}

}
