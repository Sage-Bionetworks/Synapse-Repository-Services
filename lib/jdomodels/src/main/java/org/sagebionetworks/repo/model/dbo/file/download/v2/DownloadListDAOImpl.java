package org.sagebionetworks.repo.model.dbo.file.download.v2;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DOWNLOAD_LIST_ITEM_V2_ADDED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DOWNLOAD_LIST_ITEM_V2_ENTITY_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DOWNLOAD_LIST_ITEM_V2_PRINCIPAL_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DOWNLOAD_LIST_ITEM_V2_VERION_NUMBER;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DOWNLOAD_LIST_V2_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DOWNLOAD_LIST_V2_PRINCIPAL_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DOWNLOAD_LIST_V2_UPDATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_DOWNLOAD_LIST_ITEM_V2;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_DOWNLOAD_LIST_V2;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.IntStream;

import org.sagebionetworks.repo.model.download.DownloadListItem;
import org.sagebionetworks.repo.model.download.DownloadListItemResult;
import org.sagebionetworks.repo.model.download.Sort;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class DownloadListDAOImpl implements DownloadListDAO {

	private static final int BATCH_SIZE = 1000;

	public static final Long NULL_VERSION_NUMBER = -1L;

	@Autowired
	private JdbcTemplate streamingJdbcTemplate;

	private static final RowMapper<DBODownloadList> LIST_MAPPER = new DBODownloadList().getTableMapping();
	private static final RowMapper<DBODownloadListItem> LIST_ITEM_MAPPER = new DBODownloadListItem().getTableMapping();
	
	private static final RowMapper<DownloadListItemResult> RESULT_MAPPER = (ResultSet rs, int rowNum) -> {
		DownloadListItemResult r = new DownloadListItemResult();
		r.setFileEntityId(KeyFactory.keyToString(rs.getLong(COL_DOWNLOAD_LIST_ITEM_V2_ENTITY_ID)));
		r.setVersionNumber(rs.getLong(COL_DOWNLOAD_LIST_ITEM_V2_VERION_NUMBER));
		if (NULL_VERSION_NUMBER.equals(r.getVersionNumber())) {
			r.setVersionNumber(null);
		}
		r.setAddedOn(rs.getTimestamp(COL_DOWNLOAD_LIST_ITEM_V2_ADDED_ON));
		return r;
	};

	@WriteTransaction
	@Override
	public long addBatchOfFilesToDownloadList(Long userId, List<DownloadListItem> batchToAdd) {
		ValidateArgument.required(userId, "User Id");
		if (batchToAdd == null || batchToAdd.isEmpty()) {
			return 0;
		}
		final Timestamp now = new Timestamp(System.currentTimeMillis());
		createOrUpdateDownloadList(userId);
		DownloadListItem[] batchArray = batchToAdd.toArray(new DownloadListItem[batchToAdd.size()]);
		int[] updates = streamingJdbcTemplate.batchUpdate(
				"INSERT IGNORE INTO " + TABLE_DOWNLOAD_LIST_ITEM_V2 + " (" + COL_DOWNLOAD_LIST_ITEM_V2_PRINCIPAL_ID
						+ "," + COL_DOWNLOAD_LIST_ITEM_V2_ENTITY_ID + "," + COL_DOWNLOAD_LIST_ITEM_V2_VERION_NUMBER
						+ "," + COL_DOWNLOAD_LIST_ITEM_V2_ADDED_ON + ")  VALUES(?,?,?,?)",
				new BatchPreparedStatementSetter() {

					@Override
					public void setValues(PreparedStatement ps, int i) throws SQLException {
						DownloadListItem idAndVersion = batchArray[i];

						if (idAndVersion == null) {
							throw new IllegalArgumentException("Null Item found at index: " + i);
						}
						if (idAndVersion.getFileEntityId() == null) {
							throw new IllegalArgumentException("Null fileEntityId at index: " + i);
						}
						ps.setLong(1, userId);

						ps.setLong(2, KeyFactory.stringToKey(idAndVersion.getFileEntityId()));
						Long versionNumber = NULL_VERSION_NUMBER;
						if (idAndVersion.getVersionNumber() != null) {
							versionNumber = idAndVersion.getVersionNumber();
						}
						ps.setLong(3, versionNumber);
						ps.setTimestamp(4, now);
					}

					@Override
					public int getBatchSize() {
						return batchArray.length;
					}
				});
		return IntStream.of(updates).sum();
	}

	@WriteTransaction
	@Override
	public long removeBatchOfFilesFromDownloadList(Long userId, List<DownloadListItem> batchToRemove) {
		ValidateArgument.required(userId, "User Id");
		if (batchToRemove == null || batchToRemove.isEmpty()) {
			return 0;
		}
		createOrUpdateDownloadList(userId);
		DownloadListItem[] batchArray = batchToRemove.toArray(new DownloadListItem[batchToRemove.size()]);
		int[] updates = streamingJdbcTemplate.batchUpdate("DELETE FROM " + TABLE_DOWNLOAD_LIST_ITEM_V2 + " WHERE "
				+ COL_DOWNLOAD_LIST_V2_PRINCIPAL_ID + " = ? AND " + COL_DOWNLOAD_LIST_ITEM_V2_ENTITY_ID + " = ? AND "
				+ COL_DOWNLOAD_LIST_ITEM_V2_VERION_NUMBER + " = ?", new BatchPreparedStatementSetter() {

					@Override
					public void setValues(PreparedStatement ps, int i) throws SQLException {
						DownloadListItem item = batchArray[i];

						if (item == null) {
							throw new IllegalArgumentException("Null Item at index: " + i);
						}
						if (item.getFileEntityId() == null) {
							throw new IllegalArgumentException("Null fileEntityId at index: " + i);
						}
						ps.setLong(1, userId);
						ps.setLong(2, KeyFactory.stringToKey(item.getFileEntityId()));
						Long versionNumber = NULL_VERSION_NUMBER;
						if (item.getVersionNumber() != null) {
							versionNumber = item.getVersionNumber();
						}
						ps.setLong(3, versionNumber);
					}

					@Override
					public int getBatchSize() {
						return batchArray.length;
					}
				});
		return IntStream.of(updates).sum();
	}

	private void createOrUpdateDownloadList(Long userId) {
		ValidateArgument.required(userId, "User Id");
		streamingJdbcTemplate.update("INSERT INTO " + TABLE_DOWNLOAD_LIST_V2 + "(" + COL_DOWNLOAD_LIST_V2_PRINCIPAL_ID
				+ ", " + COL_DOWNLOAD_LIST_V2_UPDATED_ON + ", " + COL_DOWNLOAD_LIST_V2_ETAG
				+ ") VALUES (?, NOW(3), UUID()) ON DUPLICATE KEY UPDATE " + COL_DOWNLOAD_LIST_V2_UPDATED_ON
				+ " = NOW(3), " + COL_DOWNLOAD_LIST_V2_ETAG + " = UUID()", userId);
	}

	@Override
	public void clearDownloadList(Long userId) {
		ValidateArgument.required(userId, "User Id");
		createOrUpdateDownloadList(userId);
		streamingJdbcTemplate.update("DELETE FROM " + TABLE_DOWNLOAD_LIST_ITEM_V2 + " WHERE "
				+ COL_DOWNLOAD_LIST_ITEM_V2_PRINCIPAL_ID + " = ?", userId);
	}

	@Override
	public void truncateAllData() {
		streamingJdbcTemplate.update(
				"DELETE FROM " + TABLE_DOWNLOAD_LIST_V2 + " WHERE " + COL_DOWNLOAD_LIST_ITEM_V2_PRINCIPAL_ID + " > -1");
	}

	@Override
	public DBODownloadList getDBODownloadList(Long userId) {
		ValidateArgument.required(userId, "User Id");
		return streamingJdbcTemplate.queryForObject(
				"SELECT * FROM " + TABLE_DOWNLOAD_LIST_V2 + " WHERE " + COL_DOWNLOAD_LIST_V2_PRINCIPAL_ID + " = ?",
				LIST_MAPPER, userId);
	}

	@Override
	public List<DBODownloadListItem> getDBODownloadListItems(Long userId) {
		ValidateArgument.required(userId, "User Id");
		return streamingJdbcTemplate.query("SELECT * FROM " + TABLE_DOWNLOAD_LIST_ITEM_V2 + " WHERE "
				+ COL_DOWNLOAD_LIST_V2_PRINCIPAL_ID + " = ? ORDER BY " + COL_DOWNLOAD_LIST_ITEM_V2_ENTITY_ID + ", "
				+ COL_DOWNLOAD_LIST_ITEM_V2_VERION_NUMBER, LIST_ITEM_MAPPER, userId);
	}
	
	@Override
	public DownloadListItemResult getDownloadListItem(Long userId, DownloadListItem item) {
		ValidateArgument.required(userId, "User Id");
		ValidateArgument.required(item, "item");
		Long entityId = KeyFactory.stringToKey(item.getFileEntityId());
		Long versionNumber = item.getVersionNumber();
		if (versionNumber == null) {
			versionNumber = NULL_VERSION_NUMBER;
		}
		return streamingJdbcTemplate.queryForObject(
				"SELECT * FROM " + TABLE_DOWNLOAD_LIST_ITEM_V2 + " WHERE " + COL_DOWNLOAD_LIST_ITEM_V2_ENTITY_ID
						+ " = ? AND " + COL_DOWNLOAD_LIST_ITEM_V2_VERION_NUMBER + " = ?",
				RESULT_MAPPER, entityId, versionNumber);
	}
	
	@Override
	public List<DownloadListItemResult> getDownloadListItems(Long userId, DownloadListItem...items) {
		ValidateArgument.required(userId, "User Id");
		ValidateArgument.required(items, "item");
		List<DownloadListItemResult> results = new ArrayList<>(items.length);
		for(DownloadListItem item: items) {
			results.add(getDownloadListItem(userId, item));
		}
		return results;
	}

	@Override
	public List<DownloadListItemResult> getFilesAvailableToDownloadFromDownloadList(EntityAccessCallback accessCallback,
			Long userId, List<Sort> sort, Long limit, Long offset) {
		/*
		 * The first step is to create a temporary table containing all of the entity
		 * IDs from the user's download list that the user can download.
		 */
		String tempTableName = createTempoaryTableOfAvailableFiles(accessCallback, userId, BATCH_SIZE);

		String sql = String.format("SELECT T.ENTITY_ID, D." + COL_DOWNLOAD_LIST_ITEM_V2_VERION_NUMBER + ", D."
				+ COL_DOWNLOAD_LIST_ITEM_V2_ADDED_ON + " FROM %S T JOIN " + TABLE_DOWNLOAD_LIST_ITEM_V2
				+ " D ON (T.ENTITY_ID = D.ENTITY_ID AND "+COL_DOWNLOAD_LIST_V2_PRINCIPAL_ID+" = ?)", tempTableName);
		return streamingJdbcTemplate.query(sql, RESULT_MAPPER, userId);
	}

	/**
	 * Create a temporary table containing all of the Entity IDs from the given
	 * user's download list that the user can download.
	 * 
	 * @param accessCallback
	 * @param userId
	 * @param batchSize
	 * @return The name of the temporary table.
	 */
	String createTempoaryTableOfAvailableFiles(EntityAccessCallback accessCallback, Long userId, int batchSize) {
		String tableName = "U" + userId + "T";
		String sql = String.format("CREATE TEMPORARY TABLE %S (`ENTITY_ID` BIGINT NOT NULL, PRIMARY KEY (`ENTITY_ID`))",
				tableName);
		streamingJdbcTemplate.update(sql);
		List<Long> batchToCheck = new LinkedList<>();
		// Stream over all of the files in the user's download list and add all files that the user can download in batches to the temp table.
		streamingJdbcTemplate.query("SELECT " + COL_DOWNLOAD_LIST_ITEM_V2_ENTITY_ID + " FROM "
				+ TABLE_DOWNLOAD_LIST_ITEM_V2 + " WHERE " + COL_DOWNLOAD_LIST_ITEM_V2_PRINCIPAL_ID + " = ? ORDER BY "
				+ COL_DOWNLOAD_LIST_ITEM_V2_ENTITY_ID + " ASC", new RowCallbackHandler() {

					@Override
					public void processRow(ResultSet rs) throws SQLException {
						batchToCheck.add(rs.getLong(COL_DOWNLOAD_LIST_ITEM_V2_ENTITY_ID));
						if (batchToCheck.size() >= batchSize) {
							// Get the sub-set of files the user can download.
							List<Long> canDownload = accessCallback.canDownload(batchToCheck);
							addBatchOfEntityIdsToTempTable(canDownload.toArray(new Long[canDownload.size()]),
									tableName);
							batchToCheck.clear();
						}
					}
				}, userId);
		// Add any remaining elements to the table.
		if (batchToCheck.size() > 0) {
			// Get the sub-set of files the user can download.
			List<Long> canDownload = accessCallback.canDownload(batchToCheck);
			addBatchOfEntityIdsToTempTable(canDownload.toArray(new Long[canDownload.size()]), tableName);
		}
		return tableName;
	}

	/**
	 * Helper to add the given batch of entity IDs to a temporary table.
	 * 
	 * @param entityIdsToAdd
	 * @param tableName
	 */
	void addBatchOfEntityIdsToTempTable(Long[] entityIdsToAdd, String tableName) {
		if(entityIdsToAdd.length < 1) {
			return;
		}
		String sql = String.format("INSERT INTO %S (ENTITY_ID) VALUES (?)", tableName);
		streamingJdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
			@Override
			public void setValues(PreparedStatement ps, int i) throws SQLException {
				ps.setLong(1, entityIdsToAdd[i]);
			}

			@Override
			public int getBatchSize() {
				return entityIdsToAdd.length;
			}
		});
	}

}
