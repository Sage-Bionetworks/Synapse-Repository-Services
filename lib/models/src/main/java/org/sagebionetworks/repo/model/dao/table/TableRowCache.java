package org.sagebionetworks.repo.model.dao.table;

import java.io.IOException;
import java.util.ConcurrentModificationException;
import java.util.Map;
import java.util.Set;

import org.sagebionetworks.repo.model.table.CurrentRowCacheStatus;
import org.sagebionetworks.repo.model.table.Row;

/**
 * This is an interface to the row cache.
 */
public interface TableRowCache {
	
	public boolean isEnabled();

	public CurrentRowCacheStatus getLatestCurrentVersionNumber(String tableId);

	public void setLatestCurrentVersionNumber(CurrentRowCacheStatus oldStatus, Long newLastCurrentVersion2)
			throws ConcurrentModificationException;

	public Map<Long, Long> getCurrentVersionNumbers(String tableId, Iterable<Long> rowIds);

	public void updateCurrentVersionNumbers(String tableId, Map<Long, Long> rowIdVersionNumbers);

	public Row getRowFromCache(String tableId, Long rowId, Long versionNumber) throws IOException;

	public Map<Long, Row> getRowsFromCache(String tableId, Map<Long, Long> rowIdVersionNumbers) throws IOException;

	public Map<Long, Row> getRowsFromCache(String tableId, Long version, Iterable<Long> rowsToGet) throws IOException;

	public void putRowInCache(String tableId, Row row) throws IOException;

	public void putRowsInCache(String tableId, Iterable<Row> rows) throws IOException;

	public void truncateAllData();
}
