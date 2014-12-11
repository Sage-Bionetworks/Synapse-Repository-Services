package org.sagebionetworks.repo.model.dbo.dao.table;

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.dao.table.RowAccessor;
import org.sagebionetworks.repo.model.dao.table.RowHandler;
import org.sagebionetworks.repo.model.dao.table.RowSetAccessor;
import org.sagebionetworks.repo.model.dao.table.TableRowCache;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.repo.model.table.RowReference;
import org.sagebionetworks.repo.model.table.RowReferenceSet;
import org.sagebionetworks.repo.model.table.RowSet;
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
import com.google.common.collect.SetMultimap;
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
	public void checkForRowLevelConflict(String tableIdString, RowSet delta, long minVersion) throws IOException {
		if (delta.getEtag() == null)
			throw new IllegalArgumentException("RowSet.etag cannot be null when rows are being updated.");
		long versionOfEtag = getVersionForEtag(tableIdString, delta.getEtag());

		Iterable<Row> updatingRows = Iterables.filter(delta.getRows(), new Predicate<Row>() {
			@Override
			public boolean apply(Row row) {
				return !TableModelUtils.isNullOrInvalid(row.getRowId());
			}
		});

		Set<Long> rowIds = TableModelUtils.getDistictValidRowIds(updatingRows);
		Map<Long, Long> rowIdVersions = getLatestVersions(tableIdString, rowIds, minVersion);

		for (Map.Entry<Long, Long> entry : rowIdVersions.entrySet()) {
			if (entry.getValue().longValue() > versionOfEtag) {
				throw new ConflictingUpdateException("Row id: " + entry.getKey()
						+ " has been changed since last read.  Please get the latest value for this row and then attempt to update it again.");
			}
		}
	}

	@Override
	public void truncateAllRowData() {
		super.truncateAllRowData();
		if (tableRowCache.isEnabled()) {
			tableRowCache.truncateAllData();
		}
	}

	/**
	 * Get the RowSet original for each row referenced.
	 * 
	 * @throws NotFoundException
	 */
	@Override
	public List<RowSet> getRowSetOriginals(RowReferenceSet ref)
			throws IOException, NotFoundException {
		if (ref == null)
			throw new IllegalArgumentException("RowReferenceSet cannot be null");
		if (ref.getTableId() == null)
			throw new IllegalArgumentException(
					"RowReferenceSet.tableId cannot be null");
		if (ref.getHeaders() == null)
			throw new IllegalArgumentException(
					"RowReferenceSet.headers cannot be null");
		if (ref.getRows() == null)
			throw new IllegalArgumentException(
					"RowReferenceSet.rows cannot be null");
		List<RowSet> results = getRowSetOriginalsFromCache(ref);
		if (results != null) {
			return results;
		} else {
			return super.getRowSetOriginals(ref);
		}
	}

	private List<RowSet> getRowSetOriginalsFromCache(RowReferenceSet ref) throws IOException, NotFoundException {

		if (!tableRowCache.isEnabled()) {
			return null;
		}
		SetMultimap<Long, Long> versions = TableModelUtils.createVersionToRowsMap(ref.getRows());

		List<RowSet> results = new LinkedList<RowSet>();
		for (Entry<Long, Collection<Long>> versionWithRows : versions.asMap().entrySet()) {
			Set<Long> rowsToGet = (Set<Long>) versionWithRows.getValue();
			Long version = versionWithRows.getKey();

			TableRowChange trc = getTableRowChange(ref.getTableId(), version);

			final Map<Long, Row> resultRows = getRowsFromCacheOrS3(ref.getTableId(), rowsToGet, version, trc);

			RowSet thisSet = new RowSet();
			thisSet.setTableId(ref.getTableId());
			thisSet.setEtag(trc.getEtag());
			thisSet.setHeaders(trc.getHeaders());
			thisSet.setRows(Lists.newArrayList(resultRows.values()));

			results.add(thisSet);
		}
		return results;
	}

	private Map<Long, Row> getRowsFromCacheOrS3(String tableIdString, Set<Long> rowsToGet, Long version, TableRowChange trc)
			throws IOException {
		Long tableId = KeyFactory.stringToKey(tableIdString);
		final Map<Long, Row> resultRows = tableRowCache.getRowsFromCache(tableId, version, rowsToGet);
		if (resultRows.size() != rowsToGet.size()) {
			// we are still missing some (or all) rows here. Read them from S3 and add to cache
			final List<Row> rows = Lists.newArrayListWithCapacity(rowsToGet.size() - resultRows.size());
			scanChange(new RowHandler() {
				@Override
				public void nextRow(Row row) {
					// Is this a row we are still looking for?
					if (!resultRows.containsKey(row.getRowId())) {
						// This is a match
						rows.add(row);
					}
				}
			}, trc);
			tableRowCache.putRowsInCache(tableId, rows);
			for (Row row : rows) {
				if (rowsToGet.contains(row.getRowId())) {
					resultRows.put(row.getRowId(), row);
				}
			}
		}
		return resultRows;
	}

	/**
	 * Get the RowSet original for each row referenced.
	 * 
	 * @throws NotFoundException
	 */
	@Override
	public Row getRowOriginal(String tableIdString, final RowReference ref, List<ColumnModel> columns) throws IOException, NotFoundException {
		if (ref == null)
			throw new IllegalArgumentException("RowReferenceSet cannot be null");
		if (tableIdString == null)
			throw new IllegalArgumentException("RowReferenceSet.tableId cannot be null");
		Long tableId = KeyFactory.stringToKey(tableIdString);
		// first try to get the row from the cache
		Row rowResult = tableRowCache.getRowFromCache(tableId, ref.getRowId(), ref.getVersionNumber());
		if (rowResult != null) {
			TableRowChange trc = getTableRowChange(tableIdString, ref.getVersionNumber());
			Map<String, Integer> columnIndexMap = TableModelUtils.createColumnIdToIndexMap(trc);
			return TableModelUtils.convertToSchemaAndMerge(rowResult, columnIndexMap, columns);
		} else {
			return super.getRowOriginal(tableIdString, ref, columns);
		}
	}

	@Override
	public void updateLatestVersionCache(String tableIdString, ProgressCallback<Long> progressCallback) throws IOException {
		if (tableRowCache.isEnabled()) {
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
		if (tableRowCache.isEnabled()) {
			tableRowCache.removeFromCache(tableId);
		}
	}

	/**
	 * The current cache is uptodate enough if we are only a few row sets behind. Reasoning: too far behind, means we
	 * need to read too many S3 files and potentially run make it a very expensive operation. We don't want to wait
	 * until we are fully uptodate, because then we might never get there if the table is consistently being updated.
	 * This way, at most MAX_CACHE_BEHIND S3 files will be read twice, but we can use the table even when the cache is a
	 * bit behind
	 */
	private void verifyCurrentCacheUptodateEnough(String tableIdString) throws TableUnavilableException {
		if (tableRowCache.isEnabled()) {
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
	public RowSetAccessor getLatestVersionsWithRowData(String tableIdString, Set<Long> rowIds, long minVersion) throws IOException {
		try {
			if (tableRowCache.isEnabled()) {
				Long tableId = KeyFactory.stringToKey(tableIdString);
				// Lookup the version number for this update.
				long lastCachedVersion = tableRowCache.getLatestCurrentVersionNumber(tableId);

				// Check each version greater than the version for the etag (must do this in ascending order)
				RowSetAccessor lastestVersionsFromS3 = super.getLatestVersionsWithRowData(tableIdString, rowIds, lastCachedVersion + 1);

				Set<Long> rowIdsLeft = Sets.difference(rowIds, lastestVersionsFromS3.getRowIdToRowMap().keySet());
				final Map<Long, RowAccessor> latestVersionsFromCache = getLatestVersionsFromCache(tableIdString, rowIdsLeft);

				latestVersionsFromCache.putAll(lastestVersionsFromS3.getRowIdToRowMap());

				return new RowSetAccessor() {
					@Override
					public Map<Long, RowAccessor> getRowIdToRowMap() {
						return latestVersionsFromCache;
					}
				};
			}
		} catch (Exception e) {
			log.error("Error getting latest from cache: " + e.getMessage(), e);
		}
		return super.getLatestVersionsWithRowData(tableIdString, rowIds, minVersion);
	}

	private Map<Long, RowAccessor> getLatestVersionsFromCache(String tableIdString, Set<Long> rowIdsInOut) throws NotFoundException,
			IOException {
		Long tableId = KeyFactory.stringToKey(tableIdString);

		Map<Long, Long> currentVersionNumbers = tableRowCache.getCurrentVersionNumbers(tableId, rowIdsInOut);

		SetMultimap<Long, Long> versions = TableModelUtils.createVersionToRowsMap(currentVersionNumbers);

		Map<Long, RowAccessor> rowIdToRowMap = Maps.newHashMap();
		for (Entry<Long, Collection<Long>> versionWithRows : versions.asMap().entrySet()) {
			Set<Long> rowsToGet = (Set<Long>)versionWithRows.getValue();
			Long version = versionWithRows.getKey();

			TableRowChange rowChange = getTableRowChange(tableIdString, versionWithRows.getKey());
			List<String> rowChangeHeaders = rowChange.getHeaders();

			Map<Long, Row> resultRows = getRowsFromCacheOrS3(tableIdString, rowsToGet, version, rowChange);

			for (Row row : resultRows.values()) {
				appendRowDataToMap(rowIdToRowMap, rowChangeHeaders, row);
			}
		}
		return rowIdToRowMap;
	}

	@Override
	public Map<Long, Long> getLatestVersions(String tableIdString, Set<Long> rowIds, long minVersion) throws IOException {
		try {
			if (tableRowCache.isEnabled()) {
				Long tableId = KeyFactory.stringToKey(tableIdString);
				// Lookup the version number for this update.
				long lastCachedVersion = tableRowCache.getLatestCurrentVersionNumber(tableId);

				// Check each version greater than the version for the etag (must do this in ascending order)
				Map<Long, Long> lastestVersionsFromS3 = super.getLatestVersions(tableIdString, rowIds, lastCachedVersion + 1);
				Set<Long> rowIdsLeft = Sets.difference(rowIds, lastestVersionsFromS3.keySet());
				Map<Long, Long> lastestVersionsFromCache = tableRowCache.getCurrentVersionNumbers(tableId, rowIdsLeft);

				lastestVersionsFromCache.putAll(lastestVersionsFromS3);
				return lastestVersionsFromCache;
			}
		} catch (Exception e) {
			log.error("Error getting latest from cache: " + e.getMessage(), e);
		}
		return super.getLatestVersions(tableIdString, rowIds, minVersion);
	}

	@Override
	public Map<Long, Long> getLatestVersions(String tableIdString, long minVersion, long rowIdOffset, long limit) throws IOException,
			NotFoundException, TableUnavilableException {
		try {
			if (tableRowCache.isEnabled()) {
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
			}
		} catch (TableUnavilableException e) {
			throw e;
		} catch (Exception e) {
			log.error("Error getting latest from cache: " + e.getMessage(), e);
		}
		return super.getLatestVersions(tableIdString, minVersion, rowIdOffset, limit);
	}
}
