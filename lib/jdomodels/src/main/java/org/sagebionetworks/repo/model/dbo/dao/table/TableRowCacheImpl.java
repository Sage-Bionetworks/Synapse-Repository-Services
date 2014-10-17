package org.sagebionetworks.repo.model.dbo.dao.table;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.dynamo.dao.rowcache.RowCacheDao;
import org.sagebionetworks.repo.model.dao.table.CurrentRowCacheDao;
import org.sagebionetworks.repo.model.dao.table.TableRowCache;
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.table.cluster.ConnectionFactory;
import org.sagebionetworks.util.ProgressCallback;
import org.springframework.beans.factory.annotation.Autowired;

public class TableRowCacheImpl implements TableRowCache {
	@Autowired
	ConnectionFactory connectionFactory;

	@Autowired
	RowCacheDao rowCacheDao;

	@Autowired
	StackConfiguration stackConfiguration;

	@Override
	public boolean isEnabled() {
		return stackConfiguration.getTableEnabled() && rowCacheDao.isEnabled();
	}

	@Override
	public long getLatestCurrentVersionNumber(Long tableId) {
		CurrentRowCacheDao currentRowCacheDao = connectionFactory.getCurrentRowCacheConnection(tableId);
		if (!currentRowCacheDao.isEnabled()) {
			return -1L;
		}
		return currentRowCacheDao.getLatestCurrentVersionNumber(tableId);
	}

	@Override
	public void updateCurrentVersionNumbers(Long tableId, Map<Long, Long> rowIdVersionNumbers, ProgressCallback<Long> progressCallback) {
		CurrentRowCacheDao currentRowCacheDao = connectionFactory.getCurrentRowCacheConnection(tableId);
		if (!currentRowCacheDao.isEnabled()) {
			throw new IllegalStateException("the current row cache was asked to update versions, but it is disabled");
		}
		currentRowCacheDao.putCurrentVersions(tableId, rowIdVersionNumbers, progressCallback);
	}

	@Override
	public Map<Long, Long> getCurrentVersionNumbers(Long tableId, Iterable<Long> rowIds) {
		CurrentRowCacheDao currentRowCacheDao = connectionFactory.getCurrentRowCacheConnection(tableId);
		if (!currentRowCacheDao.isEnabled()) {
			throw new IllegalStateException("the current row cache was asked for versions, but it is disabled");
		}
		return currentRowCacheDao.getCurrentVersions(tableId, rowIds);
	}

	@Override
	public Map<Long, Long> getCurrentVersionNumbers(Long tableId, long rowIdOffset, long limit) {
		CurrentRowCacheDao currentRowCacheDao = connectionFactory.getCurrentRowCacheConnection(tableId);
		if (!currentRowCacheDao.isEnabled()) {
			throw new IllegalStateException("the current row cache was asked for versions, but it is disabled");
		}
		return currentRowCacheDao.getCurrentVersions(tableId, rowIdOffset, limit);
	}

	@Override
	public void removeFromCache(Long tableId) {
		if (rowCacheDao.isEnabled()) {
			rowCacheDao.deleteEntriesForTable(tableId);
		}
		CurrentRowCacheDao currentRowCacheDao = connectionFactory.getCurrentRowCacheConnection(tableId);
		if (currentRowCacheDao.isEnabled()) {
			currentRowCacheDao.deleteCurrentTable(tableId);
		}
	}

	@Override
	public Row getRowFromCache(Long tableId, Long rowId, Long versionNumber) throws IOException {
		if (!rowCacheDao.isEnabled()) {
			return null;
		}
		return rowCacheDao.getRow(tableId, rowId, versionNumber);
	}

	@Override
	public Map<Long, Row> getRowsFromCache(Long tableId, Map<Long, Long> rowIdVersionNumbers) throws IOException {
		if (!rowCacheDao.isEnabled()) {
			return Collections.emptyMap();
		}
		return rowCacheDao.getRows(tableId, rowIdVersionNumbers);
	}

	@Override
	public Map<Long, Row> getRowsFromCache(Long tableId, Long version, Iterable<Long> rowsToGet) throws IOException {
		if (!rowCacheDao.isEnabled()) {
			return Collections.emptyMap();
		}
		return rowCacheDao.getRows(tableId, version, rowsToGet);
	}

	@Override
	public void putRowInCache(Long tableId, Row row) throws IOException {
		if (!rowCacheDao.isEnabled()) {
			return;
		}
		rowCacheDao.putRow(tableId, row);
	}

	@Override
	public void putRowsInCache(Long tableId, Iterable<Row> rows) throws IOException {
		if (!rowCacheDao.isEnabled()) {
			return;
		}
		rowCacheDao.putRows(tableId, rows);
	}

	@Override
	public void truncateAllData() {
		for (CurrentRowCacheDao currentRowCacheDao : connectionFactory.getCurrentRowCacheConnections()) {
			if (currentRowCacheDao.isEnabled()) {
				currentRowCacheDao.truncateAllData();
			}
		}
		if (rowCacheDao.isEnabled()) {
			rowCacheDao.truncateAllData();
		}
	}
}
