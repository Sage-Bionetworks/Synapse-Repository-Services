package org.sagebionetworks.repo.model.dao.table;

import java.util.List;

import org.sagebionetworks.repo.model.table.Row;
/**
 * Handler for a batches of rows.
 *
 */
public interface RowBatchHandler {
	
	/**
	 * Called for each batch of rows.
	 * @param batch The batch of rows.
	 * @param currentProgress  Progress data. Typically the index of the last row in this batch.
	 * @param totalProgress Progress data.  Typically the total number of rows to be processed.
	 */
	public void nextBatch(List<Row> batch, long currentProgress, long totalProgress);

}
