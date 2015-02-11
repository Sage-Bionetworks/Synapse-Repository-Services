package org.sagebionetworks.repo.model.dbo.dao.table;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.dao.table.RowAccessor;
import org.sagebionetworks.repo.model.dao.table.RowHandler;
import org.sagebionetworks.repo.model.dao.table.RowSetAccessor;
import org.sagebionetworks.repo.model.dao.table.TableRowCache;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.table.ColumnMapper;
import org.sagebionetworks.repo.model.table.RawRowSet;
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.repo.model.table.TableRowChange;
import org.sagebionetworks.repo.model.table.TableStatus;
import org.sagebionetworks.repo.model.table.TableUnavilableException;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.table.cluster.utils.TableModelUtils;
import org.sagebionetworks.util.ProgressCallback;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * Basic S3 & RDS implementation of the TableRowTruthDAO.
 * 
 * @author John
 * 
 */
public class CachingTableRowTruthDAOImpl extends TableRowTruthDAOImpl {
	private static final int MAX_CACHE_BEHIND = 2;

	private static Logger log = LogManager.getLogger(CachingTableRowTruthDAOImpl.class);

	@Autowired
	private TableRowCache tableRowCache;

	/**
	 * Check for a row level conflicts in the passed change sets, by scanning each row of each change set and looking
	 * for the intersection with the passed row Ids.
	 * 
	 * @param tableId
	 * @param delta
	 * @param coutToReserver
	 * @throws ConflictingUpdateException when a conflict is found
	 */
	@Override
	public void checkForRowLevelConflict(String tableIdString, RawRowSet delta, long minVersion) throws IOException {
		Iterable<Row> updatingRows = Iterables.filter(delta.getRows(), new Predicate<Row>() {
			@Override
			public boolean apply(Row row) {
				return !TableModelUtils.isNullOrInvalid(row.getRowId());
			}
		});

		Map<Long, Long> rowIds = TableModelUtils.getDistictValidRowIds(updatingRows);
		Map<Long, Long> rowIdLatestVersions = getLatestVersions(tableIdString, rowIds.keySet(), minVersion);

		if (delta.getEtag() != null) {
			long versionOfEtag = getVersionForEtag(tableIdString, delta.getEtag());

			for (Map.Entry<Long, Long> entry : rowIdLatestVersions.entrySet()) {
				if (entry.getValue().longValue() > versionOfEtag) {
					throwUpdateConflict(entry.getKey());
				}
			}
		} else {
			// we didn't get passed in an etag. That means we have to check each row and version individually to make
			// sure they are the latest version
			for (Map.Entry<Long, Long> entry : rowIdLatestVersions.entrySet()) {
				Long latestVersionOfRow = entry.getValue();
				Long lastVersionOfUpdateRow = rowIds.get(entry.getKey());
				if (latestVersionOfRow.longValue() > lastVersionOfUpdateRow.longValue()) {
					throwUpdateConflict(entry.getKey());
				}
			}
		}
	}

	@Override
	public void truncateAllRowData() {
		super.truncateAllRowData();
		tableRowCache.truncateAllData();
	}

	@Override
	public void updateLatestVersionCache(String tableIdString, ProgressCallback<Long> progressCallback) throws IOException {
		if (tableRowCache.isCurrentVersionCacheEnabled()) {
			Long tableId = KeyFactory.stringToKey(tableIdString);
			// Lookup the version number for this update.
			long latestCachedVersion = tableRowCache.getLatestCurrentVersionNumber(tableId);
			// Check each version greater than the latest cached version (must do this in ascending order)
			List<TableRowChange> changes = listRowSetsKeysForTableGreaterThanVersion(tableIdString, latestCachedVersion);

			for (TableRowChange change : changes) {
				final Map<Long, Long> rowIdVersionNumbers = Maps.newHashMap();
				final List<Row> rows = Lists.newArrayListWithCapacity(change.getRowCount().intValue());
				scanChange(new RowHandler() {
					@Override
					public void nextRow(Row row) {
						rowIdVersionNumbers.put(row.getRowId(), row.getVersionNumber());
						rows.add(row);
					}
				}, change);
				tableRowCache.updateCurrentVersionNumbers(tableId, rowIdVersionNumbers, progressCallback);
				if (progressCallback != null) {
					progressCallback.progressMade(change.getRowVersion());
				}
			}
		}
	}

	@Override
	public void removeCaches(Long tableId) throws IOException {
		tableRowCache.removeFromCache(tableId);
	}

	/**
	 * The current cache is uptodate enough if we are only a few row sets behind. Reasoning: too far behind, means we
	 * need to read too many S3 files and potentially run make it a very expensive operation. We don't want to wait
	 * until we are fully uptodate, because then we might never get there if the table is consistently being updated.
	 * This way, at most MAX_CACHE_BEHIND S3 files will be read twice, but we can use the table even when the cache is a
	 * bit behind
	 */
	private void verifyCurrentCacheUptodateEnough(String tableIdString) throws TableUnavilableException {
		if (tableRowCache.isCurrentVersionCacheEnabled()) {
			Long tableId = KeyFactory.stringToKey(tableIdString);
			long lastCachedVersion = tableRowCache.getLatestCurrentVersionNumber(tableId);
			// this number represents the number of rowsets that the cache is behind on the current stata
			int rowSetsBehind = super.countRowSetsForTableGreaterThanVersion(tableIdString, lastCachedVersion);
			if (rowSetsBehind > MAX_CACHE_BEHIND) {
				TableStatus status = new TableStatus();
				status.setProgressMessage("Still caching current versions for " + rowSetsBehind + " rows");
				status.setProgressCurrent((long) rowSetsBehind);
				status.setProgressTotal(0L);
				throw new TableUnavilableException(status);
			}
		}
	}

	@Override
	public RowSetAccessor getLatestVersionsWithRowData(String tableIdString, Set<Long> rowIds, long minVersion, ColumnMapper columnMapper)
			throws IOException {
		if (tableRowCache.isCurrentRowCacheEnabled()) {
			Long tableId = KeyFactory.stringToKey(tableIdString);
			// Lookup the version number for this update.
			long lastUpdatedRowVersion = tableRowCache.getLatestCurrentRowVersionNumber(tableId);

			// Check each version greater than the version for the etag (must do this in ascending order)
			RowSetAccessor lastestVersionsFromS3 = super.getLatestVersionsWithRowData(tableIdString, rowIds, lastUpdatedRowVersion + 1,
					columnMapper);

			Set<Long> rowIdsLeft = Sets.difference(rowIds, lastestVersionsFromS3.getRowIdToRowMap().keySet());
			final Map<Long, RowAccessor> latestVersionsFromCache = tableRowCache.getCurrentRowsFromCache(tableId, rowIdsLeft, columnMapper);

			latestVersionsFromCache.putAll(lastestVersionsFromS3.getRowIdToRowMap());

			return new RowSetAccessor() {
				@Override
				public Map<Long, RowAccessor> getRowIdToRowMap() {
					return latestVersionsFromCache;
				}
			};
		}
		return super.getLatestVersionsWithRowData(tableIdString, rowIds, minVersion, columnMapper);
	}

	@Override
	public Map<Long, Long> getLatestVersions(String tableIdString, Set<Long> rowIds, long minVersion) throws IOException {
		try {
			Long tableId = KeyFactory.stringToKey(tableIdString);
			// Lookup the version number for this update.
			long lastCachedVersion = tableRowCache.getLatestCurrentVersionNumber(tableId);

			// Check each version greater than the version for the etag (must do this in ascending order)
			Map<Long, Long> lastestVersionsFromS3 = super.getLatestVersions(tableIdString, rowIds, lastCachedVersion + 1);
			Set<Long> rowIdsLeft = Sets.difference(rowIds, lastestVersionsFromS3.keySet());
			Map<Long, Long> lastestVersionsFromCache = tableRowCache.getCurrentVersionNumbers(tableId, rowIdsLeft);

			lastestVersionsFromCache.putAll(lastestVersionsFromS3);
			return lastestVersionsFromCache;
		} catch (Exception e) {
			log.error("Error getting latest from cache: " + e.getMessage(), e);
		}
		return super.getLatestVersions(tableIdString, rowIds, minVersion);
	}

	@Override
	public Map<Long, Long> getLatestVersions(String tableIdString, long minVersion, long rowIdOffset, long limit) throws IOException,
			NotFoundException, TableUnavilableException {
		try {
			verifyCurrentCacheUptodateEnough(tableIdString);

			Long tableId = KeyFactory.stringToKey(tableIdString);
			// Lookup the version number for this update.
			long lastCachedVersion = tableRowCache.getLatestCurrentVersionNumber(tableId);

			// Check each version greater than the version for the etag (must do this in ascending order)
			Map<Long, Long> lastestVersionsFromS3 = super.getLatestVersions(tableIdString, lastCachedVersion + 1, rowIdOffset, limit);
			Map<Long, Long> lastestVersionsFromCache = tableRowCache.getCurrentVersionNumbers(tableId, rowIdOffset, limit);

			// merge the two by overwriting the cached versions with the ones from S3
			lastestVersionsFromCache.putAll(lastestVersionsFromS3);
			return lastestVersionsFromCache;
		} catch (TableUnavilableException e) {
			throw e;
		} catch (Exception e) {
			log.error("Error getting latest from cache: " + e.getMessage(), e);
		}
		return super.getLatestVersions(tableIdString, minVersion, rowIdOffset, limit);
	}
}
