package org.sagebionetworks.repo.manager.grid.synch.io;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.sagebionetworks.util.ValidateArgument;

/**
 * Provides indexed, disk-based access to source rows during Phase 2 (row
 * synchronization). Enables O(n) memory usage by keeping row data on disk and
 * only loading rows into memory when needed for comparison or merging.
 *
 * <p>
 * During synchronization, source rows are serialized to disk and indexed by
 * key. This reader:
 * <ul>
 * <li>Indexes rows by key for fast lookup during copy-to-source matching</li>
 * <li>Supports consuming rows as they're matched (remove operation)</li>
 * <li>Tracks remaining unmatched rows for Phase 2 processing</li>
 * <li>Lazily loads row data from disk only when fetchRow() is called</li>
 * </ul>
 *
 * <p>
 * This approach allows synchronization to handle arbitrarily large datasets
 * without loading all source rows into memory at once.
 */
public class RowReader implements AutoCloseable {

	private final Map<String, DiskPointer> diskPointerMap;
	private final RandomAccessFile raf;

	/**
	 * Creates a new reader with indexed access to rows stored on disk.
	 *
	 * @param diskPointers list of pointers to serialized rows on disk (indexed by
	 *                     key)
	 * @param raf          the random access file containing the serialized row data
	 */
	public RowReader(List<DiskPointer> diskPointers, RandomAccessFile raf) {
		ValidateArgument.required(raf, "RandomAccessFile");
		ValidateArgument.required(diskPointers, "DiskPointers");
		this.diskPointerMap = diskPointers.stream()
				.collect(Collectors.toMap(DiskPointer::getKey, pointer -> pointer, (a, b) -> a, LinkedHashMap::new));
		;
		this.raf = raf;
	}

	/**
	 * Consumes and returns the row with the specified key. This is called during
	 * Phase 1 of synchronization when a copy row is matched with a source row,
	 * "consuming" the source row so it won't be processed again in Phase 2.
	 *
	 * <p>
	 * The returned RowHeader provides lazy access to the row data - the full row is
	 * only loaded from disk when fetchRow() is called.
	 *
	 * @param key the unique identifier for the row to consume
	 * @return RowHeader for the consumed row, or empty if no row exists with that
	 *         key
	 */
	public Optional<RowHeader> consumeRow(String key) {

		DiskPointer diskPointer = diskPointerMap.remove(key);
		if (diskPointer == null) {
			return Optional.empty();
		}
		return Optional.of(createRowHeader(diskPointer));
	}

	/**
	 * Returns an iterator over all rows that haven't been consumed yet. This is
	 * called during Phase 2 of synchronization to process source rows that don't
	 * exist in the copy.
	 *
	 * <p>
	 * After Phase 1, any rows remaining in the map are rows that exist in the
	 * source but not in the copy (or were deleted by the user in the copy).
	 *
	 * @return iterator over remaining unmatched rows
	 */
	public Iterator<RowHeader> remainingRows() {
		return diskPointerMap.values().stream().map(this::createRowHeader).iterator();
	}

	/**
	 * Creates a RowHeader that provides lazy access to a row on disk. The row data
	 * is only loaded from disk when fetchRow() is called, maintaining O(n) memory
	 * usage.
	 *
	 * @param diskPointer pointer to the row's location and metadata on disk
	 * @return RowHeader providing lazy access to the row
	 */
	private RowHeader createRowHeader(DiskPointer diskPointer) {
		return new RowHeader() {
			@Override
			public byte[] getHash() {
				return diskPointer.getHash();
			}

			@Override
			public SynchRow fetchRow() {
				try {
					raf.seek(diskPointer.getOffset());
					byte[] buffer = new byte[diskPointer.getLength()];
					raf.readFully(buffer);
					return new SynchRow(buffer, diskPointer.getKey());
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}

			@Override
			public String getKey() {
				return diskPointer.getKey();
			}
		};
	}

	@Override
	public void close() throws IOException {
		raf.close();
	}
}
