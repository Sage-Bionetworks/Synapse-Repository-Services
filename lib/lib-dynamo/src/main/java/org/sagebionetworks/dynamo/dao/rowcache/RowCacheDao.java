package org.sagebionetworks.dynamo.dao.rowcache;

import java.io.IOException;
import java.util.Map;

import org.sagebionetworks.repo.model.table.Row;

public interface RowCacheDao {
	boolean isEnabled();

	Row getRow(Long tableId, Long rowId, Long versionNumber) throws IOException;

	void putRow(Long tableId, Row row) throws IOException;

	void putRows(Long tableId, Iterable<Row> rows) throws IOException;

	Map<Long, Row> getRows(Long tableId, Map<Long, Long> rowsToGet) throws IOException;

	Map<Long, Row> getRows(Long tableId, Long version, Iterable<Long> rowsToGet) throws IOException;

	void deleteEntriesForTable(Long tableId);

	void truncateAllData();
}
