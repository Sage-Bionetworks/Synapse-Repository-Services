package org.sagebionetworks.repo.model.dbo.dao.table;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ID_SEQUENCE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ID_SEQUENCE_TABLE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TABLE_ROW_KEY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TABLE_ROW_TABLE_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TABLE_ROW_TABLE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TABLE_ROW_VERSION;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_ROW_CHANGE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_TABLE_ID_SEQUENCE;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.dao.table.RowAccessor;
import org.sagebionetworks.repo.model.dao.table.RowHandler;
import org.sagebionetworks.repo.model.dao.table.RowSetAccessor;
import org.sagebionetworks.repo.model.dao.table.TableRowTruthDAO;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.persistence.table.DBOTableIdSequence;
import org.sagebionetworks.repo.model.dbo.persistence.table.DBOTableRowChange;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.table.ColumnMapper;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnModelMapper;
import org.sagebionetworks.repo.model.table.IdRange;
import org.sagebionetworks.repo.model.table.RawRowSet;
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.repo.model.table.RowReference;
import org.sagebionetworks.repo.model.table.RowReferenceSet;
import org.sagebionetworks.repo.model.table.RowSet;
import org.sagebionetworks.repo.model.table.TableRowChange;
import org.sagebionetworks.repo.model.table.TableUnavilableException;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.table.cluster.utils.TableModelUtils;
import org.sagebionetworks.util.ProgressCallback;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.S3Object;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * Basic S3 & RDS implementation of the TableRowTruthDAO.
 * 
 * @author John
 * 
 */
public class TableRowTruthDAOImpl implements TableRowTruthDAO {
	private static Logger log = LogManager.getLogger(TableRowTruthDAOImpl.class);

	private static final String SQL_SELECT_VERSION_FOR_ETAG = "SELECT "
			+ COL_TABLE_ROW_VERSION + " FROM " + TABLE_ROW_CHANGE + " WHERE "
			+ COL_TABLE_ROW_TABLE_ID + " = ? AND " + COL_TABLE_ROW_TABLE_ETAG
			+ " = ? ";
	private static final String SQL_SELECT_MAX_ROWID = "SELECT " + COL_ID_SEQUENCE + " FROM " + TABLE_TABLE_ID_SEQUENCE + " WHERE "
			+ COL_ID_SEQUENCE_TABLE_ID + " = ?";
	private static final String SQL_SELECT_LAST_ROW_CHANGE_FOR_TABLE = "SELECT * FROM " + TABLE_ROW_CHANGE + " WHERE "
			+ COL_TABLE_ROW_TABLE_ID + " = ? ORDER BY " + COL_TABLE_ROW_VERSION + " DESC LIMIT 1";
	private static final String SQL_SELECT_ROW_CHANGE_FOR_TABLE_AND_VERSION = "SELECT * FROM "
			+ TABLE_ROW_CHANGE
			+ " WHERE "
			+ COL_TABLE_ROW_TABLE_ID
			+ " = ? AND " + COL_TABLE_ROW_VERSION + " = ?";
	private static final String SQL_LIST_ALL_KEYS = "SELECT "
			+ COL_TABLE_ROW_KEY + " FROM " + TABLE_ROW_CHANGE;
	private static final String SQL_LIST_ALL_KEYS_FOR_TABLE = "SELECT " + COL_TABLE_ROW_KEY + " FROM " + TABLE_ROW_CHANGE + " WHERE "
			+ COL_TABLE_ROW_TABLE_ID + " = ?";
	private static final String SQL_SELECT_ALL_ROW_CHANGES_FOR_TABLE = "SELECT * FROM "
			+ TABLE_ROW_CHANGE
			+ " WHERE "
			+ COL_TABLE_ROW_TABLE_ID
			+ " = ? ORDER BY " + COL_TABLE_ROW_VERSION + " ASC";
	private static final String SQL_ALL_ROW_CHANGES_FOR_TABLE_GREATER_VERSION_BASE = "FROM "
			+ TABLE_ROW_CHANGE
			+ " WHERE "
			+ COL_TABLE_ROW_TABLE_ID
			+ " = ? AND "
			+ COL_TABLE_ROW_VERSION
			+ " > ? ORDER BY "
			+ COL_TABLE_ROW_VERSION + " ASC";
	private static final String SQL_SELECT_ALL_ROW_CHANGES_FOR_TABLE_GREATER_VERSION = "SELECT * "
			+ SQL_ALL_ROW_CHANGES_FOR_TABLE_GREATER_VERSION_BASE;
	private static final String SQL_COUNT_ALL_ROW_CHANGES_FOR_TABLE_GREATER_VERSION = "SELECT COUNT(*) "
			+ SQL_ALL_ROW_CHANGES_FOR_TABLE_GREATER_VERSION_BASE;
	private static final String SQL_DELETE_ROW_DATA_FOR_TABLE = "DELETE FROM " + TABLE_TABLE_ID_SEQUENCE + " WHERE "
			+ COL_ID_SEQUENCE_TABLE_ID
			+ " = ?";
	private static final String KEY_TEMPLATE = "%1$s.csv.gz";
	private static final String SQL_TRUNCATE_SEQUENCE_TABLE = "DELETE FROM "
			+ TABLE_TABLE_ID_SEQUENCE + " WHERE " + COL_ID_SEQUENCE_TABLE_ID
			+ " > 0";
	private static final String SQL_SELECT_SEQUENCE_FOR_UPDATE = "SELECT * FROM "
			+ TABLE_TABLE_ID_SEQUENCE
			+ " WHERE "
			+ COL_ID_SEQUENCE_TABLE_ID
			+ " = ? FOR UPDATE";
	@Autowired
	private DBOBasicDao basicDao;
	@Autowired
	private JdbcTemplate jdbcTemplate;
	@Autowired
	private AmazonS3Client s3Client;

	private String s3Bucket;

	RowMapper<DBOTableIdSequence> sequenceRowMapper = new DBOTableIdSequence()
			.getTableMapping();
	RowMapper<DBOTableRowChange> rowChangeMapper = new DBOTableRowChange()
			.getTableMapping();

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public IdRange reserveIdsInRange(String tableIdString, long countToReserver) {
		if (tableIdString == null)
			throw new IllegalArgumentException("TableId cannot be null");
		long tableId = KeyFactory.stringToKey(tableIdString);

		// Setup the dbo
		DBOTableIdSequence dbo = null;
		boolean exists = false;
		// If this table already exists, lock the row and get the current value.
		Long currentSequence;
		Long currentVersion;
		try {
			// First lock row for this table
			dbo = jdbcTemplate.queryForObject(SQL_SELECT_SEQUENCE_FOR_UPDATE, sequenceRowMapper, tableId);
			currentSequence = dbo.getSequence();
			currentVersion = dbo.getVersionNumber();
			exists = true;
		} catch (EmptyResultDataAccessException e) {
			// This table does not exist yet
			currentSequence = -1l;
			currentVersion = -1l;
			exists = false;
		}
		// Create the new values
		dbo = new DBOTableIdSequence();
		dbo.setSequence(currentSequence + countToReserver);
		dbo.setTableId(tableId);
		dbo.setEtag(UUID.randomUUID().toString());
		dbo.setVersionNumber(currentVersion + 1);
		// create or update
		if (exists) {
			// update
			basicDao.update(dbo);
		} else {
			// create
			basicDao.createNew(dbo);
		}
		// Prepare the results
		IdRange range = new IdRange();
		if (countToReserver > 0) {
			range.setMaximumId(dbo.getSequence());
			range.setMinimumId(dbo.getSequence() - countToReserver + 1);
		}
		range.setMaximumUpdateId(currentSequence);
		range.setVersionNumber(dbo.getVersionNumber());
		range.setEtag(dbo.getEtag());
		return range;
	}

	/**
	 * Called after bean creation.
	 */
	public void initialize() {
		// Create the bucket as needed
		s3Client.createBucket(s3Bucket);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public RowReferenceSet appendRowSetToTable(String userId, String tableId, ColumnModelMapper models, RawRowSet delta)
			throws IOException {
		// Now set the row version numbers and ID.
		int coutToReserver = TableModelUtils.countEmptyOrInvalidRowIds(delta);
		// Reserver IDs for the missing
		IdRange range = reserveIdsInRange(tableId, coutToReserver);
		// Are any rows being updated?
		if (coutToReserver < delta.getRows().size()) {
			// Validate that this update does not contain any row level conflicts.
			checkForRowLevelConflict(tableId, delta, 0);
		}

		// Now assign the rowIds and set the version number
		TableModelUtils.assignRowIdsAndVersionNumbers(delta, range);
		// We are ready to convert the file to a CSV and save it to S3.
		String key = saveCSVToS3(models.getColumnModels(), delta);
		// record the change
		DBOTableRowChange changeDBO = new DBOTableRowChange();
		changeDBO.setTableId(KeyFactory.stringToKey(tableId));
		changeDBO.setRowVersion(range.getVersionNumber());
		changeDBO.setEtag(range.getEtag());
		changeDBO.setColumnIds(TableModelUtils.createDelimitedColumnModelIdString(Lists.transform(models.getColumnModels(),
				TableModelUtils.COLUMN_MODEL_TO_ID)));
		changeDBO.setCreatedBy(Long.parseLong(userId));
		changeDBO.setCreatedOn(System.currentTimeMillis());
		changeDBO.setKey(key);
		changeDBO.setBucket(s3Bucket);
		changeDBO.setRowCount(new Long(delta.getRows().size()));
		basicDao.createNew(changeDBO);

		// Prepare the results
		RowReferenceSet results = new RowReferenceSet();
		results.setHeaders(models.getSelectColumns());
		results.setTableId(tableId);
		results.setEtag(changeDBO.getEtag());
		List<RowReference> refs = new LinkedList<RowReference>();
		// Build up the row references
		for (Row row : delta.getRows()) {
			RowReference ref = new RowReference();
			ref.setRowId(row.getRowId());
			ref.setVersionNumber(row.getVersionNumber());
			refs.add(ref);
		}
		results.setRows(refs);
		return results;
	}

	/**
	 * Check for a row level conflicts in the passed change sets, by scanning
	 * each row of each change set and looking for the intersection with the
	 * passed row Ids.
	 * 
	 * @param tableId
	 * @param delta
	 * @param coutToReserver
	 * @throws ConflictingUpdateException
	 *             when a conflict is found
	 */
	public void checkForRowLevelConflict(String tableId, RawRowSet delta, long minVersion) throws IOException {
		if (delta.getEtag() != null) {
			// Lookup the version number for this update.
			long versionOfEtag = getVersionForEtag(tableId, delta.getEtag());
			long firstVersionToCheck = Math.max(minVersion - 1, versionOfEtag);
			// Check each version greater than the version for the etag
			List<TableRowChange> changes = listRowSetsKeysForTableGreaterThanVersion(tableId, firstVersionToCheck);
			// check for row level conflicts
			Set<Long> rowIds = TableModelUtils.getDistictValidRowIds(delta.getRows()).keySet();
			for (TableRowChange change : changes) {
				checkForRowLevelConflict(change, rowIds);
			}
		} else {
			// we have to check each row individually
			// Check each version greater than the min version
			List<TableRowChange> changes = listRowSetsKeysForTableGreaterThanVersion(tableId, minVersion - 1);
			// check for row level conflicts
			// we scan from highest version down and once we encounter a row, we remove it from the map, since we are only interested in comparing to the latest version
			Map<Long, Long> rowIdsAndVersions = TableModelUtils.getDistictValidRowIds(delta.getRows());
			for (TableRowChange change : Lists.reverse(changes)) {
				checkForRowLevelConflict(change, rowIdsAndVersions);
			}
		}
	}

	/**
	 * Check for a row level conflict in the passed change set, by scanning the
	 * rows of the change and looking for the intersection with the passed row
	 * Ids.
	 * 
	 * @param change
	 * @param rowIds
	 * @throws ConflictingUpdateException
	 *             when a conflict is found
	 */
	private void checkForRowLevelConflict(final TableRowChange change,
			final Set<Long> rowIds) throws IOException {
		scanChange(new RowHandler() {
			@Override
			public void nextRow(Row row) {
				// Does this row match?
				if (rowIds.contains(row.getRowId())) {
					throwUpdateConflict(row.getRowId());
				}
			}
		}, change);
	}

	/**
	 * Check for a row level conflict in the passed map of row ids and versions, by scanning the rows of the change and
	 * looking for the intersection with the passed row Ids.
	 * 
	 * @param change
	 * @param rowIdsAndVersions (this map will be modified!)
	 * @throws ConflictingUpdateException when a conflict is found
	 */
	private void checkForRowLevelConflict(final TableRowChange change, final Map<Long, Long> rowIdsAndVersions) throws IOException {
		scanChange(new RowHandler() {
			@Override
			public void nextRow(Row row) {
				// Does this row match?
				Long version = rowIdsAndVersions.remove(row.getRowId());
				if(version != null){
					if (version.longValue() < row.getVersionNumber().longValue()) {
						throwUpdateConflict(row.getRowId());
					}
				}
			}
		}, change);
	}

	@Override
	public long getVersionForEtag(String tableIdString, String etag) {
		if (tableIdString == null)
			throw new IllegalArgumentException("TableId cannot be null");
		long tableId = KeyFactory.stringToKey(tableIdString);
		try {
			return jdbcTemplate.queryForObject(
					SQL_SELECT_VERSION_FOR_ETAG, new RowMapper<Long>() {
						@Override
						public Long mapRow(ResultSet rs, int rowNum)
								throws SQLException {
							return rs.getLong(COL_TABLE_ROW_VERSION);
						}
					}, tableId, etag);
		} catch (EmptyResultDataAccessException e) {
			throw new IllegalArgumentException("Invalid etag: " + etag);
		}
	}

	@Override
	public long getMaxRowId(String tableIdString) {
		if (tableIdString == null)
			throw new IllegalArgumentException("TableId cannot be null");
		long tableId = KeyFactory.stringToKey(tableIdString);
		try {
			return jdbcTemplate.queryForObject(SQL_SELECT_MAX_ROWID, Long.class, tableId);
		} catch (EmptyResultDataAccessException e) {
			// presumably, no rows have been added yet
			return 0L;
		}
	}

	@Override
	public TableRowChange getLastTableRowChange(String tableIdString) {
		if (tableIdString == null)
			throw new IllegalArgumentException("TableId cannot be null");
		long tableId = KeyFactory.stringToKey(tableIdString);
		try {
			DBOTableRowChange dbo = jdbcTemplate.queryForObject(SQL_SELECT_LAST_ROW_CHANGE_FOR_TABLE, rowChangeMapper, tableId);
			return TableRowChangeUtils.ceateDTOFromDBO(dbo);
		} catch (EmptyResultDataAccessException e) {
			// presumably, no rows have been added yet
			return null;
		}
	}

	/**
	 * Save a change to S3
	 * 
	 * @param models
	 * @param delta
	 * @param isDeletion
	 * @return
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
	private String saveCSVToS3(List<ColumnModel> models, RawRowSet delta)
			throws IOException, FileNotFoundException {
		File temp = File.createTempFile("rowSet", "csv.gz");
		FileOutputStream out = null;
		try {
			out = new FileOutputStream(temp);
			// Save this to the the zipped CSV
			TableModelUtils.validateAnWriteToCSVgz(models, delta, out);
			// upload it to S3.
			String key = String.format(KEY_TEMPLATE, UUID.randomUUID()
					.toString());
			s3Client.putObject(s3Bucket, key, temp);
			return key;
		} finally {
			if (out != null) {
				out.close();
			}
			if (temp != null) {
				temp.delete();
			}
		}
	}

	/**
	 * List all changes for this table.
	 */
	@Override
	public List<TableRowChange> listRowSetsKeysForTable(String tableIdString) {
		if (tableIdString == null)
			throw new IllegalArgumentException("TableId cannot be null");
		long tableId = KeyFactory.stringToKey(tableIdString);
		List<DBOTableRowChange> dboList = jdbcTemplate.query(
				SQL_SELECT_ALL_ROW_CHANGES_FOR_TABLE, rowChangeMapper, tableId);
		return TableRowChangeUtils.ceateDTOFromDBO(dboList);
	}

	@Override
	public List<TableRowChange> listRowSetsKeysForTableGreaterThanVersion(
			String tableIdString, long versionNumber) {
		if (tableIdString == null)
			throw new IllegalArgumentException("TableId cannot be null");
		long tableId = KeyFactory.stringToKey(tableIdString);
		List<DBOTableRowChange> dboList = jdbcTemplate.query(
				SQL_SELECT_ALL_ROW_CHANGES_FOR_TABLE_GREATER_VERSION,
				rowChangeMapper, tableId, versionNumber);
		return TableRowChangeUtils.ceateDTOFromDBO(dboList);
	}

	@Override
	public int countRowSetsForTableGreaterThanVersion(String tableIdString, long versionNumber) {
		ValidateArgument.required(tableIdString, "TableId");
		long tableId = KeyFactory.stringToKey(tableIdString);
		int count = jdbcTemplate.queryForObject(SQL_COUNT_ALL_ROW_CHANGES_FOR_TABLE_GREATER_VERSION, Integer.class, tableId, versionNumber);
		return count;
	}

	@Override
	public TableRowChange getTableRowChange(String tableIdString,
			long rowVersion) throws NotFoundException {
		if (tableIdString == null)
			throw new IllegalArgumentException("TableID cannot be null");
		long tableId = KeyFactory.stringToKey(tableIdString);
		try {
			DBOTableRowChange dbo = jdbcTemplate.queryForObject(
					SQL_SELECT_ROW_CHANGE_FOR_TABLE_AND_VERSION,
					rowChangeMapper, tableId, rowVersion);
			return TableRowChangeUtils.ceateDTOFromDBO(dbo);
		} catch (EmptyResultDataAccessException e) {
			throw new NotFoundException(
					"TableRowChange does not exist for tableId: " + tableId
							+ " and row version: " + rowVersion);
		}
	}

	/**
	 * Read the RowSet from S3.
	 * 
	 * @throws NotFoundException
	 */
	@Override
	public RowSet getRowSet(String tableId, long rowVersion, Set<Long> rowsToGet, ColumnModelMapper schema)
			throws IOException, NotFoundException {
		TableRowChange dto = getTableRowChange(tableId, rowVersion);
		// Downlaod the file from S3
		S3Object object = s3Client.getObject(dto.getBucket(), dto.getKey());
		try {
			RowSet set = new RowSet();
			List<Row> rows = TableModelUtils.readFromCSVgzStream(object.getObjectContent(), rowsToGet);
			set.setTableId(tableId);
			set.setHeaders(TableModelUtils.getSelectColumnsFromColumnIds(dto.getIds(), schema));
			set.setRows(rows);
			set.setEtag(dto.getEtag());
			return set;
		} finally {
			// Need to close the stream unconditionally.
			object.getObjectContent().close();
		}
	}

	@Override
	public TableRowChange scanRowSet(String tableId, long rowVersion,
			RowHandler handler) throws IOException, NotFoundException {
		TableRowChange dto = getTableRowChange(tableId, rowVersion);
		// stream the file from S3
		scanChange(handler, dto);
		return dto;
	}

	/**
	 * @param handler
	 * @param dto
	 * @return
	 * @throws IOException
	 */
	public void scanChange(RowHandler handler, TableRowChange dto)
			throws IOException {
		S3Object object = s3Client.getObject(dto.getBucket(), dto.getKey());
		try {
			TableModelUtils.scanFromCSVgzStream(object.getObjectContent(),
					handler);
		} finally {
			// Need to close the stream unconditionally.
			object.getObjectContent().close();
		}
	}

	@Override
	public void deleteAllRowDataForTable(String tableId) {
		// List key so we can delete them
		List<String> keysToDelete = listAllKeysForTable(tableId);
		// Delete each object from S3
		for (String key : keysToDelete) {
			s3Client.deleteObject(s3Bucket, key);
		}
		// let cascade delete take care of deleting the row changes
		jdbcTemplate.update(SQL_DELETE_ROW_DATA_FOR_TABLE, KeyFactory.stringToKey(tableId));
	}

	@Override
	public void truncateAllRowData() {
		// List key so we can delete them
		List<String> keysToDelete = listAllKeys();
		// Delete each object from S3
		for (String key : keysToDelete) {
			s3Client.deleteObject(s3Bucket, key);
		}
		jdbcTemplate.update(SQL_TRUNCATE_SEQUENCE_TABLE);
	}

	/**
	 * List all of the S3 Keys
	 * 
	 * @return
	 */
	private List<String> listAllKeys() {
		return jdbcTemplate.query(SQL_LIST_ALL_KEYS,
				new RowMapper<String>() {
					@Override
					public String mapRow(ResultSet rs, int rowNum)
							throws SQLException {
						return rs.getString(COL_TABLE_ROW_KEY);
					}
				});
	}

	/**
	 * List all of the S3 Keys for a table
	 * 
	 * @return
	 */
	private List<String> listAllKeysForTable(String tableId) {
		return jdbcTemplate.query(SQL_LIST_ALL_KEYS_FOR_TABLE, new RowMapper<String>() {
			@Override
			public String mapRow(ResultSet rs, int rowNum) throws SQLException {
				return rs.getString(COL_TABLE_ROW_KEY);
			}
		}, KeyFactory.stringToKey(tableId));
	}

	/**
	 * Get the RowSet original for each row referenced.
	 * 
	 * @throws NotFoundException
	 */
	@Override
	public List<RawRowSet> getRowSetOriginals(RowReferenceSet ref, ColumnModelMapper columnMapper)
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
		// First determine the versions we will need to inspect for this query.
		Set<Long> versions = TableModelUtils.getDistictVersions(ref.getRows());
		final Set<RowReference> rowsToFetch = new HashSet<RowReference>(
				ref.getRows());
		List<RawRowSet> results = Lists.newArrayListWithCapacity(versions.size());
		// For each version of the table
		for (Long version : versions) {
			// Scan over the delta
			final List<Row> rows = Lists.newLinkedList();
			TableRowChange trc = scanRowSet(ref.getTableId(), version, new RowHandler() {
				@Override
				public void nextRow(Row row) {
					// Is this a row we are looking for?
					RowReference thisRowRef = new RowReference();
					thisRowRef.setRowId(row.getRowId());
					thisRowRef.setVersionNumber(row.getVersionNumber());
					if (rowsToFetch.contains(thisRowRef)) {
						// This is a match
						rows.add(row);
					}
				}
			});
			// fill in the rest of the values
			results.add(new RawRowSet(trc.getIds(), trc.getEtag(), ref.getTableId(), rows));
		}
		return results;
	}

	/**
	 * Get the RowSet original for each row referenced.
	 * 
	 * @throws NotFoundException
	 */
	@Override
	public Row getRowOriginal(String tableId, final RowReference ref, ColumnModelMapper resultSchema) throws IOException, NotFoundException {
		if (ref == null)
			throw new IllegalArgumentException("RowReferenceSet cannot be null");
		if (tableId == null)
			throw new IllegalArgumentException("RowReferenceSet.tableId cannot be null");
		// First determine the versions we will need to inspect for this query.
		final List<Row> results = Lists.newArrayList();
		TableRowChange trc = scanRowSet(tableId, ref.getVersionNumber(), new RowHandler() {
			@Override
			public void nextRow(Row row) {
				// Is this a row we are looking for?
				if (row.getRowId().equals(ref.getRowId())) {
					results.add(row);
				}
			}
		});
		if (results.size() == 0) {
			throw new NotFoundException("Row not found, row=" + ref.getRowId() + ", version=" + ref.getVersionNumber());
		}
		Map<Long, Integer> columnIndexMap = TableModelUtils.createColumnIdToIndexMap(trc);
		return TableModelUtils.convertToSchemaAndMerge(results.get(0), columnIndexMap, resultSchema);
	}

	@Override
	public RowSetAccessor getLatestVersionsWithRowData(String tableId, Set<Long> rowIds, long minVersion, ColumnMapper columnMapper)
			throws IOException {
		final Map<Long, RowAccessor> rowIdToRowMap = Maps.newHashMap();

		List<TableRowChange> rowChanges = listRowSetsKeysForTableGreaterThanVersion(tableId, minVersion - 1);

		final Set<Long> rowsToFind = Sets.newHashSet(rowIds);
		// we are scanning backwards through the row changes.
		// For each version of the table (starting at the last one)
		for (final TableRowChange rowChange : Lists.reverse(rowChanges)) {
			if (rowsToFind.isEmpty()) {
				// we found all the rows that we need to find
				break;
			}

			final List<Long> rowChangeColumnIds = rowChange.getIds();
			// Scan over the delta
			scanChange(new RowHandler() {
				@Override
				public void nextRow(final Row row) {
					// if we still needed it, we no longer need to find this one
					if (rowsToFind.remove(row.getRowId())) {
						appendRowDataToMap(rowIdToRowMap, rowChangeColumnIds, row);
					}
				}
			}, rowChange);
		}

		return new RowSetAccessor() {
			@Override
			public Map<Long, RowAccessor> getRowIdToRowMap() {
				return rowIdToRowMap;
			}
		};
	}

	@Override
	public Map<Long, Long> getLatestVersions(String tableId, Set<Long> rowIds, long minVersion) throws IOException {
		final Map<Long, Long> rowVersions = Maps.newHashMap();

		List<TableRowChange> rowChanges = listRowSetsKeysForTableGreaterThanVersion(tableId, minVersion - 1);

		final Set<Long> rowsToFind = Sets.newHashSet(rowIds);
		// we are scanning backwards through the row changes.
		// For each version of the table (starting at the last one)
		for (final TableRowChange rowChange : Lists.reverse(rowChanges)) {
			if (rowsToFind.isEmpty()) {
				// we found all the rows that we need to find
				break;
			}

			// Scan over the delta
			scanChange(new RowHandler() {
				@Override
				public void nextRow(final Row row) {
					// if we still needed it, we no longer need to find this one
					if (rowsToFind.remove(row.getRowId())) {
						rowVersions.put(row.getRowId(), row.getVersionNumber());
					}
				}
			}, rowChange);
		}

		return rowVersions;
	}

	@Override
	public Map<Long, Long> getLatestVersions(String tableId, final long minVersion, final long rowIdOffset, final long limit)
			throws IOException, NotFoundException, TableUnavilableException {
		final Map<Long, Long> rowVersions = Maps.newHashMap();

		List<TableRowChange> rowChanges = listRowSetsKeysForTableGreaterThanVersion(tableId, minVersion - 1);

		// scan forward (rowChanges is ordered lowest version first)
		for (final TableRowChange rowChange : rowChanges) {
			scanChange(new RowHandler() {
				@Override
				public void nextRow(final Row row) {
					if (row.getRowId() >= rowIdOffset && row.getRowId() < rowIdOffset + limit) {
						// since we are iterating forward, we can just overwrite previous values here
						rowVersions.put(row.getRowId(), row.getVersionNumber());
					}
				}
			}, rowChange);
		}

		return rowVersions;
	}

	protected void appendRowDataToMap(final Map<Long, RowAccessor> rowIdToRowMap, final List<Long> rowChangeColumnIds, final Row row) {
		if (TableModelUtils.isDeletedRow(row)) {
			rowIdToRowMap.remove(row.getRowId());
		} else {
			rowIdToRowMap.put(row.getRowId(), new RowAccessor() {
				Map<Long, Integer> columnIdToIndexMap = null;

				@Override
				public String getCellById(Long columnId) {
					if (columnIdToIndexMap == null) {
						columnIdToIndexMap = TableModelUtils.createColumnIdToIndexMap(rowChangeColumnIds);
					}
					Integer index = columnIdToIndexMap.get(columnId);
					if (row.getValues() == null || index == null || index >= row.getValues().size()) {
						return null;
					}
					return row.getValues().get(index);
				}

				@Override
				public Long getRowId() {
					return row.getRowId();
				}

				@Override
				public Long getVersionNumber() {
					return row.getVersionNumber();
				}
			});
		}
	}

	@Override
	public RowSet getRowSet(RowReferenceSet ref, ColumnModelMapper resultSchema)
			throws IOException, NotFoundException {
		// Get all of the data in the raw form.
		List<RawRowSet> allSets = getRowSetOriginals(ref, resultSchema);
		// the list of rowsets is sorted by version number. The highest version (last rowset) is the most recent for all
		// rows. We return that as the etag
		String etag = null;
		if (!allSets.isEmpty()) {
			etag = allSets.get(allSets.size() - 1).getEtag();
		}
		// Convert and merge all data into the requested form
		return TableModelUtils.convertToSchemaAndMerge(allSets, resultSchema, ref.getTableId(), etag);
	}

	@Override
	public void updateLatestVersionCache(String tableId, ProgressCallback<Long> progressCallback) throws IOException {
		// do nothing here, only caching version needs to do anything
	}

	@Override
	public void removeCaches(Long tableId) throws IOException {
		// do nothing here, only caching version needs to do anything
	}

	public String getS3Bucket() {
		return s3Bucket;
	}

	/**
	 * IoC
	 * 
	 * @param s3Bucket
	 */
	public void setS3Bucket(String s3Bucket) {
		this.s3Bucket = s3Bucket;
	}

	protected void throwUpdateConflict(Long rowId) {
		throw new ConflictingUpdateException("Row id: " + rowId
				+ " has been changed since last read.  Please get the latest value for this row and then attempt to update it again.");
	}
}
