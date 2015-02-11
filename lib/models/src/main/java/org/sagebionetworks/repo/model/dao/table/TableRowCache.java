package org.sagebionetworks.repo.model.dao.table;

import java.util.Map;

import org.sagebionetworks.repo.model.table.ColumnMapper;
import org.sagebionetworks.util.ProgressCallback;

/**
 * This is an interface to the row cache.
 */
public interface TableRowCache {
	
	public boolean isCurrentVersionCacheEnabled();

	public boolean isCurrentRowCacheEnabled();

	public long getLatestCurrentVersionNumber(Long tableId);

	public Map<Long, Long> getCurrentVersionNumbers(Long tableId, Iterable<Long> rowIds);

	public Map<Long, Long> getCurrentVersionNumbers(Long tableId, long rowIdOffset, long limit);

	public void updateCurrentVersionNumbers(Long tableId, Map<Long, Long> rowIdVersionNumbers, ProgressCallback<Long> progressCallback);

	public void removeFromCache(Long tableId);

	public long getLatestCurrentRowVersionNumber(Long tableId);

	public Map<Long, RowAccessor> getCurrentRowsFromCache(Long tableId, Iterable<Long> rowsToGet, ColumnMapper mapper);

	public void truncateAllData();
}
