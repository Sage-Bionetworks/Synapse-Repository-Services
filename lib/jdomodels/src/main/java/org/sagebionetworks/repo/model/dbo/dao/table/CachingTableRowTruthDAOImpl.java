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
import org.sagebionetworks.repo.model.table.CurrentRowCacheStatus;
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.repo.model.table.RowReference;
import org.sagebionetworks.repo.model.table.RowReferenceSet;
import org.sagebionetworks.repo.model.table.RowSet;
import org.sagebionetworks.repo.model.table.TableRowChange;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.ProgressCallback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.base.Predicate;
import com.google.common.collect.HashMultimap;
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
						+ " has been changes since last read.  Please get the latest value for this row and then attempt to update it again.");
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
		SetMultimap<Long, Long> versions = createVersionToRowsMap(ref.getRows());

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
			CurrentRowCacheStatus currentStatus = tableRowCache.getLatestCurrentVersionNumber(tableId);
			// Check each version greater than the latest cached version (must do this in ascending order)
			List<TableRowChange> changes = currentStatus.getLatestCachedVersionNumber() == null ? listRowSetsKeysForTable(tableIdString)
					: listRowSetsKeysForTableGreaterThanVersion(tableIdString, currentStatus.getLatestCachedVersionNumber());

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
				tableRowCache.updateCurrentVersionNumbers(tableId, rowIdVersionNumbers);
				if (progressCallback != null) {
					progressCallback.progressMade(change.getRowVersion());
				}
				tableRowCache.setLatestCurrentVersionNumber(currentStatus, change.getRowVersion());
				currentStatus = tableRowCache.getLatestCurrentVersionNumber(tableId);
			}
		}
	}

	@Override
	public void removeLatestVersionCache(String tableIdString) throws IOException {
		if (tableRowCache.isEnabled()) {
			Long tableId = KeyFactory.stringToKey(tableIdString);
			tableRowCache.removeFromCache(tableId);
		}
	}

	@Override
	public RowSetAccessor getLatestVersionsWithRowData(String tableIdString, Set<Long> rowIds, long minVersion) throws IOException {
		try {
			if (tableRowCache.isEnabled()) {
				Long tableId = KeyFactory.stringToKey(tableIdString);
				// Lookup the version number for this update.
				CurrentRowCacheStatus currentStatus = tableRowCache.getLatestCurrentVersionNumber(tableId);
				// Check each version greater than the version for the etag (must do this in ascending order)
				long lastCachedVersion = currentStatus.getLatestCachedVersionNumber() == null ? -1 : currentStatus
						.getLatestCachedVersionNumber();

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

		SetMultimap<Long, Long> versions = createVersionToRowsMap(currentVersionNumbers);

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
				CurrentRowCacheStatus currentStatus = tableRowCache.getLatestCurrentVersionNumber(tableId);
				// Check each version greater than the version for the etag (must do this in ascending order)
				long lastCachedVersion = currentStatus.getLatestCachedVersionNumber() == null ? -1 : currentStatus
						.getLatestCachedVersionNumber();

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
	public Map<Long, Long> getLatestVersions(String tableIdString, long minVersion) throws IOException, NotFoundException {
		try {
			if (tableRowCache.isEnabled()) {
				Long tableId = KeyFactory.stringToKey(tableIdString);
				// Lookup the version number for this update.
				CurrentRowCacheStatus currentStatus = tableRowCache.getLatestCurrentVersionNumber(tableId);
				// Check each version greater than the version for the etag (must do this in ascending order)
				long lastCachedVersion = currentStatus.getLatestCachedVersionNumber() == null ? -1 : currentStatus
						.getLatestCachedVersionNumber();

				Map<Long, Long> lastestVersionsFromS3 = super.getLatestVersions(tableIdString, lastCachedVersion + 1);

				Map<Long, Long> lastestVersionsFromCache = tableRowCache.getCurrentVersionNumbers(tableId);
				lastestVersionsFromCache.putAll(lastestVersionsFromS3);
				return lastestVersionsFromCache;
			}
		} catch (Exception e) {
			log.error("Error getting latest from cache: " + e.getMessage(), e);
		}
		return super.getLatestVersions(tableIdString, minVersion);
	}

	private SetMultimap<Long, Long> createVersionToRowsMap(Map<Long, Long> currentVersionNumbers) {
		// create a map from version to set of row ids map
		SetMultimap<Long, Long> versions = HashMultimap.create();
		for (Entry<Long, Long> rowVersion : currentVersionNumbers.entrySet()) {
			versions.put(rowVersion.getValue(), rowVersion.getKey());
		}
		return versions;
	}

	private SetMultimap<Long, Long> createVersionToRowsMap(Iterable<RowReference> refs) {
		// create a map from version to set of row ids map
		SetMultimap<Long, Long> versions = HashMultimap.create();
		for (RowReference ref : refs) {
			versions.put(ref.getVersionNumber(), ref.getRowId());
		}
		return versions;
	}
}
