package org.sagebionetworks.repo.manager.grid.synch.io;

import org.sagebionetworks.repo.manager.grid.synch.core.SourceItem;

/**
 * Represents the metadata for a source row stored on disk during Phase 2 (row
 * synchronization). Provides lazy access to row data to enable O(n) memory
 * usage during synchronization.
 *
 * <p>
 * During synchronization, source rows are written to disk and indexed by key.
 * The RowHeader stores the metadata needed to:
 * <ul>
 * <li>Match rows between copy and source using the key (inherited from
 * SourceItem)</li>
 * <li>Quickly compare rows using the hash without loading full row data</li>
 * <li>Fetch the full row data only when needed (lazy loading)</li>
 * </ul>
 *
 * <p>
 * This allows the synchronization logic to process large datasets efficiently
 * by only loading rows into memory when they need to be compared or merged.
 */
public interface RowHeader extends SourceItem {

 /**
  * Gets the hash of the serialized row data. Used for quick comparison to
  * determine if a row has changed without loading the full row data from disk.
  *
  * <p>
  * During synchronization, rows with matching hashes can be skipped (no merge
  * needed), while rows with different hashes require fetching the full data for
  * cell-level comparison.
  *
  * @return the hash bytes of the serialized row data
  */
 byte[] getHash();

 /**
  * Fetches the full row data from disk. Called only when needed during
  * synchronization (e.g., when hashes don't match and cell-level comparison or
  * merging is required).
  *
  * <p>
  * This lazy loading approach ensures that rows are only read from disk when
  * necessary, maintaining O(n) memory usage even for very large datasets.
  *
  * @return the complete row data loaded from disk
  */
 SynchRow fetchRow();

}
