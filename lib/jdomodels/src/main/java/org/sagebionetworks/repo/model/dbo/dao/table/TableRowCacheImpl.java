package org.sagebionetworks.repo.model.dbo.dao.table;

import java.util.Map;

import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.model.dao.table.CurrentRowCacheDao;
import org.sagebionetworks.repo.model.dao.table.CurrentVersionCacheDao;
import org.sagebionetworks.repo.model.dao.table.RowAccessor;
import org.sagebionetworks.repo.model.dao.table.TableRowCache;
import org.sagebionetworks.repo.model.table.ColumnMapper;
import org.sagebionetworks.table.cluster.ConnectionFactory;
import org.sagebionetworks.util.ProgressCallback;
import org.springframework.beans.factory.annotation.Autowired;

public class TableRowCacheImpl implements TableRowCache {
	@Autowired
	ConnectionFactory connectionFactory;

	@Autowired
	StackConfiguration stackConfiguration;

	@Override
	public boolean isCurrentVersionCacheEnabled() {
		return stackConfiguration.getTableEnabled();
	}

	@Override
	public boolean isCurrentRowCacheEnabled() {
		return stackConfiguration.getTableEnabled();
	}

	@Override
	public long getLatestCurrentVersionNumber(Long tableId) {
		CurrentVersionCacheDao currentRowCacheDao = connectionFactory.getCurrentVersionCacheConnection(tableId);
		if (!isCurrentVersionCacheEnabled() || !currentRowCacheDao.isEnabled()) {
			return -1L;
		}
		return currentRowCacheDao.getLatestCurrentVersionNumber(tableId);
	}

	@Override
	public void updateCurrentVersionNumbers(Long tableId, Map<Long, Long> rowIdVersionNumbers, ProgressCallback<Long> progressCallback) {
		CurrentVersionCacheDao currentRowCacheDao = getCurrentVersionCacheConnection(tableId);
		currentRowCacheDao.putCurrentVersions(tableId, rowIdVersionNumbers, progressCallback);
	}

	@Override
	public Map<Long, Long> getCurrentVersionNumbers(Long tableId, Iterable<Long> rowIds) {
		CurrentVersionCacheDao currentRowCacheDao = getCurrentVersionCacheConnection(tableId);
		return currentRowCacheDao.getCurrentVersions(tableId, rowIds);
	}

	@Override
	public Map<Long, Long> getCurrentVersionNumbers(Long tableId, long rowIdOffset, long limit) {
		CurrentVersionCacheDao currentRowCacheDao = getCurrentVersionCacheConnection(tableId);
		return currentRowCacheDao.getCurrentVersions(tableId, rowIdOffset, limit);
	}

	@Override
	public void removeFromCache(Long tableId) {
		CurrentVersionCacheDao currentVersionCacheDao = connectionFactory.getCurrentVersionCacheConnection(tableId);
		if (isCurrentVersionCacheEnabled() && currentVersionCacheDao.isEnabled()) {
			currentVersionCacheDao.deleteCurrentVersionTable(tableId);
		}
	}

	@Override
	public long getLatestCurrentRowVersionNumber(Long tableId) {
		CurrentRowCacheDao currentRowCacheDao = getCurrentRowCacheConnection(tableId);
		return currentRowCacheDao.getLatestCurrentRowVersionNumber(tableId);
	}

	@Override
	public Map<Long, RowAccessor> getCurrentRowsFromCache(Long tableId, Iterable<Long> rowsToGet, ColumnMapper mapper) {
		CurrentRowCacheDao currentRowCacheDao = getCurrentRowCacheConnection(tableId);
		return currentRowCacheDao.getCurrentRows(tableId, rowsToGet, mapper);
	}

	@Override
	public void truncateAllData() {
		if (isCurrentVersionCacheEnabled()) {
			for (CurrentVersionCacheDao currentRowCacheDao : connectionFactory.getCurrentVersionCacheConnections()) {
				if (currentRowCacheDao.isEnabled()) {
					currentRowCacheDao.truncateAllData();
				}
			}
		}
	}

	private CurrentVersionCacheDao getCurrentVersionCacheConnection(Long tableId) {
		CurrentVersionCacheDao currentVersionCacheDao = connectionFactory.getCurrentVersionCacheConnection(tableId);
		if (!isCurrentVersionCacheEnabled() || !currentVersionCacheDao.isEnabled()) {
			throw new IllegalStateException("the current version cache was asked for versions, but it is disabled");
		}
		return currentVersionCacheDao;
	}

	private CurrentRowCacheDao getCurrentRowCacheConnection(Long tableId) {
		CurrentRowCacheDao currentRowCacheDao = connectionFactory.getCurrentRowCacheConnection(tableId);
		if (!isCurrentRowCacheEnabled() || !currentRowCacheDao.isEnabled()) {
			throw new IllegalStateException("the current row cache was asked for rows, but it is disabled");
		}
		return currentRowCacheDao;
	}
}
