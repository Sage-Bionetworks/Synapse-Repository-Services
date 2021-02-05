package org.sagebionetworks.repo.model.dbo.dao.table;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ID_SEQUENCE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ID_SEQUENCE_TABLE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TABLE_ROW_KEY_NEW;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TABLE_ROW_TABLE_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TABLE_ROW_TABLE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TABLE_ROW_TRX_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TABLE_ROW_TYPE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TABLE_ROW_VERSION;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TABLE_TRX_TO_VER_TRX_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TABLE_TRX_TO_VER_VER_NUM;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_ROW_CHANGE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_TABLE_ID_SEQUENCE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_TABLE_TRX_TO_VERSION;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.sagebionetworks.aws.SynapseS3Client;
import org.sagebionetworks.repo.model.dao.table.TableRowTruthDAO;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.persistence.table.ColumnModelUtils;
import org.sagebionetworks.repo.model.dbo.persistence.table.DBOTableIdSequence;
import org.sagebionetworks.repo.model.dbo.persistence.table.DBOTableRowChange;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.table.ColumnChange;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.IdRange;
import org.sagebionetworks.repo.model.table.SparseChangeSetDto;
import org.sagebionetworks.repo.model.table.TableChangeType;
import org.sagebionetworks.repo.model.table.TableRowChange;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.table.cluster.utils.TableModelUtils;
import org.sagebionetworks.util.FileProvider;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import com.amazonaws.services.s3.model.S3Object;

/**
 * Basic S3 & RDS implementation of the TableRowTruthDAO.
 * 
 * @author John
 * 
 */
public class TableRowTruthDAOImpl implements TableRowTruthDAO {

	private static final String SELECT_FIRST_ROW_VERSION_FOR_TABLE = "SELECT " + COL_TABLE_ROW_VERSION + " FROM "
			+ TABLE_ROW_CHANGE + " WHERE " + COL_TABLE_ROW_TYPE + " = ? AND TABLE_ID = ? LIMIT 1";

	private static final String SELECT_LAST_TRANSACTION_ID = "SELECT " + COL_TABLE_ROW_TRX_ID + " FROM "
			+ TABLE_ROW_CHANGE + " WHERE " + COL_TABLE_ROW_TABLE_ID + " = ? ORDER BY " + COL_TABLE_ROW_VERSION
			+ " DESC LIMIT 1";

	private static final String SQL_LAST_CHANGE_NUMBER = "SELECT MAX(" + COL_TABLE_ROW_VERSION + ") FROM "
			+ TABLE_ROW_CHANGE + " WHERE " + COL_TABLE_ROW_TABLE_ID + " = ?";

	private static final String SQL_LAST_CHANGE_NUMBER_FOR_VERSION = "SELECT MAX(" + COL_TABLE_ROW_VERSION + ") FROM "
			+ TABLE_ROW_CHANGE + " C JOIN " + TABLE_TABLE_TRX_TO_VERSION + " V ON (C." + COL_TABLE_ROW_TRX_ID + " = V."
			+ COL_TABLE_TRX_TO_VER_TRX_ID + ") WHERE C." + COL_TABLE_ROW_TABLE_ID + " = ? AND V."
			+ COL_TABLE_TRX_TO_VER_VER_NUM + " = ?";

	public static final String SCAN_ROWS_TYPE_ERROR = "Can only scan over table changes of type: "
			+ TableChangeType.ROW;

	private static final String SQL_SELECT_VERSION_FOR_ETAG = "SELECT " + COL_TABLE_ROW_VERSION + " FROM "
			+ TABLE_ROW_CHANGE + " WHERE " + COL_TABLE_ROW_TABLE_ID + " = ? AND " + COL_TABLE_ROW_TABLE_ETAG + " = ? ";
	private static final String SQL_SELECT_MAX_ROWID = "SELECT " + COL_ID_SEQUENCE + " FROM " + TABLE_TABLE_ID_SEQUENCE
			+ " WHERE " + COL_ID_SEQUENCE_TABLE_ID + " = ?";

	private static final String SQL_SELECT_LAST_ROW_CHANGE_FOR_TABLE = "SELECT * FROM " + TABLE_ROW_CHANGE + " WHERE "
			+ COL_TABLE_ROW_TABLE_ID + " = ? ORDER BY " + COL_TABLE_ROW_VERSION + " DESC LIMIT 1";

	private static final String SQL_SELECT_LAST_ROW_CHANGE_FOR_TABLE_WITH_TYPE = "SELECT * FROM " + TABLE_ROW_CHANGE
			+ " WHERE " + COL_TABLE_ROW_TABLE_ID + " = ? AND " + COL_TABLE_ROW_TYPE + " = ? ORDER BY "
			+ COL_TABLE_ROW_VERSION + " DESC LIMIT 1";

	private static final String SQL_SELECT_ROW_CHANGE_FOR_TABLE_AND_VERSION = "SELECT * FROM " + TABLE_ROW_CHANGE
			+ " WHERE " + COL_TABLE_ROW_TABLE_ID + " = ? AND " + COL_TABLE_ROW_VERSION + " = ?";

	private static final String SQL_LIST_ALL_KEYS = "SELECT " + COL_TABLE_ROW_KEY_NEW + " FROM " + TABLE_ROW_CHANGE
			+ " WHERE " + COL_TABLE_ROW_KEY_NEW + " IS NOT NULL" + " UNION SELECT " + COL_TABLE_ROW_KEY_NEW + " FROM "
			+ TABLE_ROW_CHANGE + " WHERE " + COL_TABLE_ROW_KEY_NEW + " IS NOT NULL";

	private static final String SQL_LIST_ALL_KEYS_FOR_TABLE = "SELECT " + COL_TABLE_ROW_KEY_NEW + " FROM "
			+ TABLE_ROW_CHANGE + " WHERE " + COL_TABLE_ROW_KEY_NEW + " IS NOT NULL AND " + COL_TABLE_ROW_TABLE_ID
			+ " = ?";

	private static final String SQL_SELECT_ALL_ROW_CHANGES_FOR_TABLE = "SELECT * FROM " + TABLE_ROW_CHANGE + " WHERE "
			+ COL_TABLE_ROW_TABLE_ID + " = ? ORDER BY " + COL_TABLE_ROW_VERSION + " ASC LIMIT ? OFFSET ?";

	private static final String SQL_ALL_ROW_CHANGES_FOR_TABLE_GREATER_VERSION_BASE = "FROM " + TABLE_ROW_CHANGE
			+ " WHERE " + COL_TABLE_ROW_TABLE_ID + " = ? AND " + COL_TABLE_ROW_VERSION + " > ? AND "
			+ COL_TABLE_ROW_TYPE + " = '" + TableChangeType.ROW + "' ORDER BY " + COL_TABLE_ROW_VERSION + " ASC";

	private static final String SQL_SELECT_ALL_ROW_CHANGES_FOR_TABLE_GREATER_VERSION = "SELECT * "
			+ SQL_ALL_ROW_CHANGES_FOR_TABLE_GREATER_VERSION_BASE;

	private static final String SQL_DELETE_ROW_DATA_FOR_TABLE = "DELETE FROM " + TABLE_TABLE_ID_SEQUENCE + " WHERE "
			+ COL_ID_SEQUENCE_TABLE_ID + " = ?";
	private static final String KEY_TEMPLATE = "%1$s.csv.gz";
	private static final String SQL_TRUNCATE_SEQUENCE_TABLE = "DELETE FROM " + TABLE_TABLE_ID_SEQUENCE + " WHERE "
			+ COL_ID_SEQUENCE_TABLE_ID + " > 0";
	private static final String SQL_SELECT_SEQUENCE_FOR_UPDATE = "SELECT * FROM " + TABLE_TABLE_ID_SEQUENCE + " WHERE "
			+ COL_ID_SEQUENCE_TABLE_ID + " = ? FOR UPDATE";
	@Autowired
	private DBOBasicDao basicDao;
	@Autowired
	private JdbcTemplate jdbcTemplate;
	@Autowired
	private SynapseS3Client s3Client;
	@Autowired
	private FileProvider fileProvider;

	private String s3Bucket;

	RowMapper<DBOTableIdSequence> sequenceRowMapper = new DBOTableIdSequence().getTableMapping();
	RowMapper<DBOTableRowChange> rowChangeMapper = new DBOTableRowChange().getTableMapping();

	@WriteTransaction
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

	@WriteTransaction
	@Override
	public String appendRowSetToTable(String userId, String tableId, String etag, long versionNumber,
			List<ColumnModel> columns, final SparseChangeSetDto delta, long transactionId) {
		// Write the delta to S3
		String key = saveToS3((OutputStream out) -> TableModelUtils.writeSparesChangeSetToGz(delta, out));
		// record the change
		DBOTableRowChange changeDBO = new DBOTableRowChange();
		changeDBO.setTableId(KeyFactory.stringToKey(tableId));
		changeDBO.setRowVersion(versionNumber);
		changeDBO.setEtag(etag);
		changeDBO.setCreatedBy(Long.parseLong(userId));
		changeDBO.setCreatedOn(System.currentTimeMillis());
		changeDBO.setKeyNew(key);
		changeDBO.setBucket(s3Bucket);
		changeDBO.setRowCount(new Long(delta.getRows().size()));
		changeDBO.setChangeType(TableChangeType.ROW.name());
		changeDBO.setTransactionId(transactionId);
		basicDao.createNew(changeDBO);
		return key;
	}

	@Override
	public long appendSchemaChangeToTable(String userId, String tableId, List<String> current,
			final List<ColumnChange> changes, long transactionId) {

		long coutToReserver = 1;
		IdRange range = reserveIdsInRange(tableId, coutToReserver);
		// We are ready to convert the file to a CSV and save it to S3.
		String key = saveToS3((OutputStream out) -> ColumnModelUtils.writeSchemaChangeToGz(changes, out));
		// record the change
		DBOTableRowChange changeDBO = new DBOTableRowChange();
		changeDBO.setTableId(KeyFactory.stringToKey(tableId));
		changeDBO.setRowVersion(range.getVersionNumber());
		changeDBO.setEtag(range.getEtag());
		changeDBO.setColumnIds(TableModelUtils.createDelimitedColumnModelIdString(current));
		changeDBO.setCreatedBy(Long.parseLong(userId));
		changeDBO.setCreatedOn(System.currentTimeMillis());
		changeDBO.setKeyNew(key);
		changeDBO.setBucket(s3Bucket);
		changeDBO.setRowCount(0L);
		changeDBO.setChangeType(TableChangeType.COLUMN.name());
		changeDBO.setTransactionId(transactionId);
		basicDao.createNew(changeDBO);
		return range.getVersionNumber();
	}

	/**
	 * Write the data from the given callback to S3.
	 * 
	 * @param callback
	 * @return
	 * @throws IOException
	 */
	String saveToS3(WriterCallback callback) {
		// First write to a temp file.
		try {
			File temp = fileProvider.createTempFile("tempToS3", ".gz");
			try (OutputStream out = fileProvider.createFileOutputStream(temp);) {
				// write to the temp file.
				callback.write(out);
				out.flush();
				out.close();
				// upload it to S3.
				String key = String.format(KEY_TEMPLATE, UUID.randomUUID().toString());
				s3Client.putObject(s3Bucket, key, temp);
				return key;
			} finally {
				if (temp != null) {
					temp.delete();
				}
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public List<ColumnChange> getSchemaChangeForVersion(String tableId, long versionNumber) throws IOException {
		TableRowChange dto = getTableRowChange(tableId, versionNumber);
		// Download the file from S3
		S3Object object = s3Client.getObject(dto.getBucket(), dto.getKeyNew());
		try {
			return ColumnModelUtils.readSchemaChangeFromGz(object.getObjectContent());
		} finally {
			// Need to close the stream unconditionally.
			object.getObjectContent().close();
		}
	}

	@Override
	public long getVersionForEtag(String tableIdString, String etag) {
		if (tableIdString == null)
			throw new IllegalArgumentException("TableId cannot be null");
		long tableId = KeyFactory.stringToKey(tableIdString);
		try {
			return jdbcTemplate.queryForObject(SQL_SELECT_VERSION_FOR_ETAG, new RowMapper<Long>() {
				@Override
				public Long mapRow(ResultSet rs, int rowNum) throws SQLException {
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
			return -1L;
		}
	}

	@Override
	public TableRowChange getLastTableRowChange(String tableIdString) {
		if (tableIdString == null)
			throw new IllegalArgumentException("TableId cannot be null");
		long tableId = KeyFactory.stringToKey(tableIdString);
		try {
			DBOTableRowChange dbo = jdbcTemplate.queryForObject(SQL_SELECT_LAST_ROW_CHANGE_FOR_TABLE, rowChangeMapper,
					tableId);
			return TableRowChangeUtils.ceateDTOFromDBO(dbo);
		} catch (EmptyResultDataAccessException e) {
			// presumably, no rows have been added yet
			return null;
		}
	}

	@Override
	public TableRowChange getLastTableRowChange(String tableIdString, TableChangeType changeType) {
		ValidateArgument.required(tableIdString, "tableId");
		ValidateArgument.required(changeType, "TableChangeType");
		long tableId = KeyFactory.stringToKey(tableIdString);
		try {
			DBOTableRowChange dbo = jdbcTemplate.queryForObject(SQL_SELECT_LAST_ROW_CHANGE_FOR_TABLE_WITH_TYPE,
					rowChangeMapper, tableId, changeType.name());
			return TableRowChangeUtils.ceateDTOFromDBO(dbo);
		} catch (EmptyResultDataAccessException e) {
			// presumably, no rows have been added yet
			return null;
		}
	}

	/**
	 * List all changes for this table.
	 */
	@Deprecated
	@Override
	public List<TableRowChange> listRowSetsKeysForTable(String tableIdString) {
		long limit = Long.MAX_VALUE;
		long offset = 0L;
		return getTableChangePage(tableIdString, limit, offset);
	}

	@Override
	public List<TableRowChange> listRowSetsKeysForTableGreaterThanVersion(String tableIdString, long versionNumber) {
		if (tableIdString == null)
			throw new IllegalArgumentException("TableId cannot be null");
		long tableId = KeyFactory.stringToKey(tableIdString);
		List<DBOTableRowChange> dboList = jdbcTemplate.query(SQL_SELECT_ALL_ROW_CHANGES_FOR_TABLE_GREATER_VERSION,
				rowChangeMapper, tableId, versionNumber);
		return TableRowChangeUtils.ceateDTOFromDBO(dboList);
	}

	@Override
	public TableRowChange getTableRowChange(String tableIdString, long rowVersion) throws NotFoundException {
		if (tableIdString == null)
			throw new IllegalArgumentException("TableID cannot be null");
		long tableId = KeyFactory.stringToKey(tableIdString);
		try {
			DBOTableRowChange dbo = jdbcTemplate.queryForObject(SQL_SELECT_ROW_CHANGE_FOR_TABLE_AND_VERSION,
					rowChangeMapper, tableId, rowVersion);
			return TableRowChangeUtils.ceateDTOFromDBO(dbo);
		} catch (EmptyResultDataAccessException e) {
			throw new NotFoundException(
					"TableRowChange does not exist for tableId: " + tableId + " and row version: " + rowVersion);
		}
	}

	@Override
	public SparseChangeSetDto getRowSet(String tableId, long rowVersion) throws IOException {
		TableRowChange dto = getTableRowChange(tableId, rowVersion);
		return getRowSet(dto);
	}

	@Override
	public SparseChangeSetDto getRowSet(TableRowChange dto) throws IOException {
		// Download the file from S3
		S3Object object = s3Client.getObject(dto.getBucket(), dto.getKeyNew());
		try {
			return TableModelUtils.readSparseChangeSetDtoFromGzStream(object.getObjectContent());
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
		return jdbcTemplate.query(SQL_LIST_ALL_KEYS, new RowMapper<String>() {
			@Override
			public String mapRow(ResultSet rs, int rowNum) throws SQLException {
				return rs.getString(COL_TABLE_ROW_KEY_NEW);
			}
		});
	}

	/**
	 * List all of the S3 Keys for a table
	 * 
	 * @return
	 */
	private List<String> listAllKeysForTable(String tableId) {
		long tableIdLong = KeyFactory.stringToKey(tableId);
		return jdbcTemplate.query(SQL_LIST_ALL_KEYS_FOR_TABLE, new RowMapper<String>() {
			@Override
			public String mapRow(ResultSet rs, int rowNum) throws SQLException {
				return rs.getString(COL_TABLE_ROW_KEY_NEW);
			}
		}, tableIdLong);
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

	@Override
	public List<TableRowChange> getTableChangePage(String tableIdString, long limit, long offset) {
		ValidateArgument.required(tableIdString, "tableId");
		long tableId = KeyFactory.stringToKey(tableIdString);
		List<DBOTableRowChange> dboList = jdbcTemplate.query(SQL_SELECT_ALL_ROW_CHANGES_FOR_TABLE, rowChangeMapper,
				tableId, limit, offset);
		return TableRowChangeUtils.ceateDTOFromDBO(dboList);
	}

	@Override
	public Optional<Long> getLastTableChangeNumber(long tableId) {
		Long changeNumber = this.jdbcTemplate.queryForObject(SQL_LAST_CHANGE_NUMBER, Long.class, tableId);
		return Optional.ofNullable(changeNumber);
	}

	@Override
	public Optional<Long> getLastTableChangeNumber(long tableId, long tableVersion) {
		Long changeNumber = this.jdbcTemplate.queryForObject(SQL_LAST_CHANGE_NUMBER_FOR_VERSION, Long.class, tableId,
				tableVersion);
		return Optional.ofNullable(changeNumber);
	}

	@Override
	public boolean hasAtLeastOneChangeOfType(String tableIdString, TableChangeType type) {
		ValidateArgument.required(tableIdString, "tableId");
		ValidateArgument.required(type, "TableChangeType");
		try {
			long tableId = KeyFactory.stringToKey(tableIdString);
			Long firstRowVersion = jdbcTemplate.queryForObject(SELECT_FIRST_ROW_VERSION_FOR_TABLE, Long.class,
					type.name(), tableId);
			return firstRowVersion != null;
		} catch (EmptyResultDataAccessException e) {
			return false;
		}
	}

	@Override
	public Optional<Long> getLastTransactionId(String tableIdString) {
		ValidateArgument.required(tableIdString, "tableId");
		try {
			long tableId = KeyFactory.stringToKey(tableIdString);
			Long transactionId = jdbcTemplate.queryForObject(SELECT_LAST_TRANSACTION_ID, Long.class, tableId);
			return Optional.of(transactionId);
		} catch (EmptyResultDataAccessException e) {
			return Optional.empty();
		}
	}

	@WriteTransaction
	@Override
	public void deleteChangeNumber(String tableIdString, long changeNumber) {
		ValidateArgument.required(tableIdString, "tableId");
		long tableId = KeyFactory.stringToKey(tableIdString);
		jdbcTemplate.update("DELETE FROM " + TABLE_ROW_CHANGE + " WHERE " + COL_TABLE_ROW_TABLE_ID + " = ? AND "
				+ COL_TABLE_ROW_VERSION + " = ?", tableId, changeNumber);
	}

	@Override
	public boolean isEtagInTablesChangeHistory(String tableIdString, String etag) {
		ValidateArgument.required(tableIdString, "tableId");
		ValidateArgument.required(etag, "etag");
		long tableId = KeyFactory.stringToKey(tableIdString);
		try {
			jdbcTemplate.queryForObject(
					"SELECT " + COL_TABLE_ROW_TABLE_ETAG + " FROM " + TABLE_ROW_CHANGE + " WHERE "
							+ COL_TABLE_ROW_TABLE_ID + " = ? AND " + COL_TABLE_ROW_TABLE_ETAG + " = ?",
					String.class, tableId, etag);
			return true;
		} catch (EmptyResultDataAccessException e) {
			return false;
		}
	}

}
