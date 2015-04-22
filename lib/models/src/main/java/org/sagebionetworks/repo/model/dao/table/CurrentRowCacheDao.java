package org.sagebionetworks.repo.model.dao.table;

import java.util.Map;

import org.sagebionetworks.repo.model.table.ColumnMapper;

public interface CurrentRowCacheDao {
	boolean isEnabled();

	long getLatestCurrentRowVersionNumber(Long tableId);

	Map<Long, RowAccessor> getCurrentRows(Long tableId, Iterable<Long> rowIds, ColumnMapper mapper);
}
