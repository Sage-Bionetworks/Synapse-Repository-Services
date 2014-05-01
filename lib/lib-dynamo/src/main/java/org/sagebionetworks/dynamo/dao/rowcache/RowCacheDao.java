package org.sagebionetworks.dynamo.dao.rowcache;

import java.io.IOException;
import java.util.Map;

import org.sagebionetworks.repo.model.table.Row;

public interface RowCacheDao {
	boolean isEnabled();

	Row getRow(String tableId, Long rowId, Long versionNumber) throws IOException;

	void putRow(String tableId, Row row) throws IOException;

	void putRows(String tableId, Iterable<Row> rows) throws IOException;

	Map<Long, Row> getRows(String tableId, Map<Long, Long> rowsToGet) throws IOException;

	Map<Long, Row> getRows(String tableId, Long version, Iterable<Long> rowsToGet) throws IOException;

	void truncateAllData();
}
