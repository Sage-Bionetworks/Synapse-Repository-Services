package org.sagebionetworks.repo.model.dbo.dao.table;

import java.io.IOException;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.Map;

import org.sagebionetworks.dynamo.dao.rowcache.CurrentRowCacheDao;
import org.sagebionetworks.dynamo.dao.rowcache.RowCacheDao;
import org.sagebionetworks.repo.model.dao.table.TableRowCache;
import org.sagebionetworks.repo.model.table.CurrentRowCacheStatus;
import org.sagebionetworks.repo.model.table.Row;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.services.dynamodb.model.ConditionalCheckFailedException;

public class TableRowCacheImpl implements TableRowCache {
	@Autowired
	CurrentRowCacheDao currentRowCacheDao;

	@Autowired
	RowCacheDao rowCacheDao;

	@Override
	public boolean isEnabled() {
		return currentRowCacheDao.isEnabled() && rowCacheDao.isEnabled();
	}

	@Override
	public CurrentRowCacheStatus getLatestCurrentVersionNumber(Long tableId) {
		if (!currentRowCacheDao.isEnabled()) {
			return new CurrentRowCacheStatus(tableId, null, null);
		}
		return currentRowCacheDao.getLatestCurrentVersionNumber(tableId);
	}

	@Override
	public void setLatestCurrentVersionNumber(CurrentRowCacheStatus oldStatus, Long newLastCurrentVersion)
			throws ConcurrentModificationException {
		if (!currentRowCacheDao.isEnabled()) {
			throw new IllegalStateException("the current row cache was asked to set latest version, but it is disabled");
		}
		try {
			currentRowCacheDao.setLatestCurrentVersionNumber(oldStatus, newLastCurrentVersion);
		} catch (ConditionalCheckFailedException e) {
			throw new ConcurrentModificationException("Latest current version number was updated by someone else: " + e.getMessage(), e);
		}
	}

	@Override
	public void updateCurrentVersionNumbers(Long tableId, Map<Long, Long> rowIdVersionNumbers) {
		if (!currentRowCacheDao.isEnabled()) {
			throw new IllegalStateException("the current row cache was asked to update versions, but it is disabled");
		}
		currentRowCacheDao.putCurrentVersions(tableId, rowIdVersionNumbers);
	}

	@Override
	public Map<Long, Long> getCurrentVersionNumbers(Long tableId, Iterable<Long> rowIds) {
		if (!currentRowCacheDao.isEnabled()) {
			throw new IllegalStateException("the current row cache was asked for versions, but it is disabled");
		}
		return currentRowCacheDao.getCurrentVersions(tableId, rowIds);
	}

	@Override
	public Map<Long, Long> getCurrentVersionNumbers(Long tableId, long rowIdOffset, long limit) {
		if (!currentRowCacheDao.isEnabled()) {
			throw new IllegalStateException("the current row cache was asked for versions, but it is disabled");
		}
		return currentRowCacheDao.getCurrentVersions(tableId, rowIdOffset, limit);
	}

	@Override
	public void removeFromCache(Long tableId) {
		if (!rowCacheDao.isEnabled()) {
			return;
		}
		currentRowCacheDao.deleteCurrentTable(tableId);
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
		if (currentRowCacheDao.isEnabled()) {
			currentRowCacheDao.truncateAllData();
		}
		if (rowCacheDao.isEnabled()) {
			rowCacheDao.truncateAllData();
		}
	}
}
