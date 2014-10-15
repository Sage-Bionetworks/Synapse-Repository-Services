package org.sagebionetworks.dynamo.dao.rowcache;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.sagebionetworks.dynamo.KeyValueSplitter;
import org.sagebionetworks.repo.model.table.Row;

import com.google.common.collect.Maps;

public class RowCacheDaoStub implements RowCacheDao {

	public boolean isEnabled = false;

	public Map<String, Row> rows = Maps.newHashMap();

	@Override
	public boolean isEnabled() {
		return isEnabled;
	}

	private String createKey(Long tableId, Long rowId, Long versionNumber) {
		return KeyValueSplitter.createKey(tableId, rowId, versionNumber);
	}

	@Override
	public Row getRow(Long tableId, Long rowId, Long versionNumber) throws IOException {
		return rows.get(createKey(tableId, rowId, versionNumber));
	}

	@Override
	public Map<Long, Row> getRows(Long tableId, Map<Long, Long> rowsToGet) throws IOException {
		Map<Long, Row> result = Maps.newHashMap();
		for (Map.Entry<Long, Long> row : rowsToGet.entrySet()) {
			result.put(row.getKey(), rows.get(createKey(tableId, row.getKey(), row.getValue())));
		}
		return result;
	}

	@Override
	public Map<Long, Row> getRows(Long tableId, Long version, Iterable<Long> rowsToGet) throws IOException {
		Map<Long, Row> result = Maps.newHashMap();
		for (Long rowId : rowsToGet) {
			Row row = rows.get(createKey(tableId, rowId, version));
			if (row != null) {
				result.put(rowId, row);
			}
		}
		return result;
	}

	@Override
	public void putRow(Long tableId, Row row) throws IOException {
		rows.put(createKey(tableId, row.getRowId(), row.getVersionNumber()), row);
	}

	@Override
	public void putRows(Long tableId, Iterable<Row> rowsToPut) throws IOException {
		for (Row row : rowsToPut) {
			rows.put(createKey(tableId, row.getRowId(), row.getVersionNumber()), row);
		}
	}

	@Override
	public void deleteEntriesForTable(Long tableId) {
		String prefix = tableId.toString() + KeyValueSplitter.SEPARATOR;
		for (Iterator<Entry<String, Row>> iterator = rows.entrySet().iterator(); iterator.hasNext();) {
			Entry<String, Row> entry = iterator.next();
			if (entry.getKey().startsWith(prefix)) {
				iterator.remove();
			}
		}
	}

	@Override
	public void truncateAllData() {
		rows.clear();
	}
}
