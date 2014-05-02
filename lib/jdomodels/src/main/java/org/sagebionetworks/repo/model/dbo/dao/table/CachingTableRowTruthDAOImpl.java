package org.sagebionetworks.repo.model.dbo.dao.table;

import java.io.IOException;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.HashSet;
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
import org.sagebionetworks.repo.model.dbo.persistence.table.DBOTableIdSequence;
import org.sagebionetworks.repo.model.dbo.persistence.table.DBOTableRowChange;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.CurrentRowCacheStatus;
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.repo.model.table.RowReference;
import org.sagebionetworks.repo.model.table.RowReferenceSet;
import org.sagebionetworks.repo.model.table.RowSet;
import org.sagebionetworks.repo.model.table.TableRowChange;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.base.Predicate;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
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
	public void checkForRowLevelConflict(String tableId, RowSet delta) throws IOException {
		if (checkAndUpdateCurrentRowCache(tableId)) {
			checkForRowLevelConflictUsingCache(tableId, delta);
		} else {
			// Validate that this update does not contain any row level conflicts.
			super.checkForRowLevelConflict(tableId, delta);
		}
	}

	private boolean checkAndUpdateCurrentRowCache(String tableId) {
		try {
			if (tableRowCache.isEnabled()) {
				// Lookup the version number for this update.
				CurrentRowCacheStatus currentStatus = tableRowCache.getLatestCurrentVersionNumber(tableId);
				// Check each version greater than the version for the etag (must do this in ascending order)
				List<TableRowChange> changes = currentStatus.getLatestCachedVersionNumber() == null ? listRowSetsKeysForTable(tableId)
						: listRowSetsKeysForTableGreaterThanVersion(tableId, currentStatus.getLatestCachedVersionNumber());

				long newLastCurrentVersion = -1L;
				for (TableRowChange change : changes) {
					newLastCurrentVersion = Math.max(newLastCurrentVersion, change.getRowVersion());

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
				}
				if (newLastCurrentVersion >= 0) {
					try {
						tableRowCache.setLatestCurrentVersionNumber(currentStatus, newLastCurrentVersion);
					} catch (ConcurrentModificationException e) {
						// in the case of a concurrent update, we don't want to keep retrying. Instead, we fall back to
						// uncached behaviour for this call
						log.warn("Concurrent updates of cache: " + e.getMessage());
						return false;
					}
				}
				return true;
			}
		} catch (Exception e) {
			log.error("Error updating cache: " + e.getMessage(), e);
		}
		return false;
	}

	/**
	 * Check for a row level conflicts in the passed change sets, by checking the cache for the latest version of each
	 * row
	 * 
	 * @param tableId
	 * @param delta
	 * @param coutToReserver
	 * @throws ConflictingUpdateException when a conflict is found
	 */
	private void checkForRowLevelConflictUsingCache(String tableId, RowSet delta) throws IOException {
		if (delta.getEtag() == null)
			throw new IllegalArgumentException("RowSet.etag cannot be null when rows are being updated.");
		// Lookup the version number for this update.
		long versionOfEtag = getVersionForEtag(tableId, delta.getEtag());

		Iterable<Row> updatingRows = Iterables.filter(delta.getRows(), new Predicate<Row>() {
			@Override
			public boolean apply(Row row) {
				return !TableModelUtils.isNullOrInvalid(row.getRowId());
			}
		});

		Set<Long> rowToGetVersionsFor = Sets.newHashSet();
		for (Row row : updatingRows) {
			if (!rowToGetVersionsFor.add(row.getRowId())) {
				// the row id is found twice int the same rowset
				throw new IllegalArgumentException("The row id " + row.getRowId() + " is included more than once in the rowset");
			}
		}

		Map<Long, Long> rowIdVersions = tableRowCache.getCurrentVersionNumbers(tableId, rowToGetVersionsFor);

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

	private Map<Long, Row> getRowsFromCacheOrS3(String tableId, Set<Long> rowsToGet, Long version, TableRowChange trc) throws IOException {
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
	public Row getRowOriginal(String tableId, final RowReference ref, List<ColumnModel> columns) throws IOException, NotFoundException {
		if (ref == null)
			throw new IllegalArgumentException("RowReferenceSet cannot be null");
		if (tableId == null)
			throw new IllegalArgumentException("RowReferenceSet.tableId cannot be null");
		// first try to get the row from the cache
		Row rowResult = tableRowCache.getRowFromCache(tableId, ref.getRowId(), ref.getVersionNumber());
		if (rowResult != null) {
			TableRowChange trc = getTableRowChange(tableId, ref.getVersionNumber());
			Map<String, Integer> columnIndexMap = TableModelUtils.createColumnIdToIndexMap(trc);
			return TableModelUtils.convertToSchemaAndMerge(rowResult, columnIndexMap, columns);
		} else {
			return super.getRowOriginal(tableId, ref, columns);
		}
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public RowSetAccessor getLatestVersions(String tableId, Set<Long> rowIds, String etag) throws IOException, NotFoundException {
		if (etag == null) {
			throw new IllegalArgumentException("A valid etag must be passed in");
		}
		final Map<Long, RowAccessor> rowIdToRowMap;
		if (checkAndUpdateCurrentRowCache(tableId)) {
			rowIdToRowMap = getLatestVersionsFromCache(tableId, rowIds);

			return new RowSetAccessor() {
				@Override
				protected Map<Long, RowAccessor> getRowIdToRowMap() {
					return rowIdToRowMap;
				}
			};
		} else {
			return super.getLatestVersions(tableId, rowIds, etag);
		}
	}

	private Map<Long, RowAccessor> getLatestVersionsFromCache(String tableId, Set<Long> rowIds) throws NotFoundException,
			IOException {

		Map<Long, Long> currentVersionNumbers = tableRowCache.getCurrentVersionNumbers(tableId, rowIds);

		SetMultimap<Long, Long> versions = createVersionToRowsMap(currentVersionNumbers);

		Map<Long, RowAccessor> rowIdToRowMap = Maps.newHashMap();
		for (Entry<Long, Collection<Long>> versionWithRows : versions.asMap().entrySet()) {
			Set<Long> rowsToGet = (Set<Long>)versionWithRows.getValue();
			Long version = versionWithRows.getKey();

			TableRowChange rowChange = getTableRowChange(tableId, versionWithRows.getKey());
			List<String> rowChangeHeaders = rowChange.getHeaders();

			Map<Long, Row> resultRows = getRowsFromCacheOrS3(tableId, rowsToGet, version, rowChange);

			for (Row row : resultRows.values()) {
				appendRowDataToMap(rowIdToRowMap, rowChangeHeaders, row);
			}
		}
		return rowIdToRowMap;
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
