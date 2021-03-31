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
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.sagebionetworks.repo.model.NodeConstants;
import org.sagebionetworks.repo.model.dbo.DDLUtilsImpl;
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
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.google.common.base.Objects;

@Repository
public class DownloadListDAOImpl implements DownloadListDAO {

	public static final String DOWNLOAD_LIST_RESULT_TEMPLATE = DDLUtilsImpl
			.loadSQLFromClasspath("sql/DownloadListResultsTemplate.sql");

	private static final int BATCH_SIZE = 1000;

	public static final Long NULL_VERSION_NUMBER = -1L;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private NamedParameterJdbcTemplate namedJdbcTemplate;

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
		r.setFileName(rs.getString("ENTITY_NAME"));
		r.setCreatedBy(rs.getString("CREATED_BY"));
		r.setCreatedOn(new Date(rs.getLong("CREATED_ON")));
		r.setProjectId(KeyFactory.keyToString(rs.getLong("PROJECT_ID")));
		r.setProjectName(rs.getString("PROJECT_NAME"));
		r.setFileSizeBytes(rs.getLong("CONTENT_SIZE"));
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
		int[] updates = jdbcTemplate.batchUpdate(
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
		int[] updates = jdbcTemplate.batchUpdate("DELETE FROM " + TABLE_DOWNLOAD_LIST_ITEM_V2 + " WHERE "
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
		jdbcTemplate.update("INSERT INTO " + TABLE_DOWNLOAD_LIST_V2 + "(" + COL_DOWNLOAD_LIST_V2_PRINCIPAL_ID
				+ ", " + COL_DOWNLOAD_LIST_V2_UPDATED_ON + ", " + COL_DOWNLOAD_LIST_V2_ETAG
				+ ") VALUES (?, NOW(3), UUID()) ON DUPLICATE KEY UPDATE " + COL_DOWNLOAD_LIST_V2_UPDATED_ON
				+ " = NOW(3), " + COL_DOWNLOAD_LIST_V2_ETAG + " = UUID()", userId);
	}

	@Override
	public void clearDownloadList(Long userId) {
		ValidateArgument.required(userId, "User Id");
		createOrUpdateDownloadList(userId);
		jdbcTemplate.update("DELETE FROM " + TABLE_DOWNLOAD_LIST_ITEM_V2 + " WHERE "
				+ COL_DOWNLOAD_LIST_ITEM_V2_PRINCIPAL_ID + " = ?", userId);
	}

	@Override
	public void truncateAllData() {
		jdbcTemplate.update(
				"DELETE FROM " + TABLE_DOWNLOAD_LIST_V2 + " WHERE " + COL_DOWNLOAD_LIST_ITEM_V2_PRINCIPAL_ID + " > -1");
	}

	@Override
	public DBODownloadList getDBODownloadList(Long userId) {
		ValidateArgument.required(userId, "User Id");
		return jdbcTemplate.queryForObject(
				"SELECT * FROM " + TABLE_DOWNLOAD_LIST_V2 + " WHERE " + COL_DOWNLOAD_LIST_V2_PRINCIPAL_ID + " = ?",
				LIST_MAPPER, userId);
	}

	@Override
	public List<DBODownloadListItem> getDBODownloadListItems(Long userId) {
		ValidateArgument.required(userId, "User Id");
		return jdbcTemplate.query("SELECT * FROM " + TABLE_DOWNLOAD_LIST_ITEM_V2 + " WHERE "
				+ COL_DOWNLOAD_LIST_V2_PRINCIPAL_ID + " = ? ORDER BY " + COL_DOWNLOAD_LIST_ITEM_V2_ENTITY_ID + ", "
				+ COL_DOWNLOAD_LIST_ITEM_V2_VERION_NUMBER, LIST_ITEM_MAPPER, userId);
	}

	@Override
	public List<DownloadListItemResult> getDownloadListItems(Long userId, DownloadListItem... items) {
		ValidateArgument.required(userId, "User Id");
		ValidateArgument.required(items, "item");
		// Create a temp table that contains only the IDs we want to keep.
		Set<Long> idsToKeep = Arrays.stream(items).map(i -> KeyFactory.stringToKey(i.getFileEntityId()))
				.collect(Collectors.toSet());
		String tempTableName = createTempoaryTableOfAvailableFiles(
				t -> t.stream().filter(i -> idsToKeep.contains(i)).collect(Collectors.toList()), userId, BATCH_SIZE);

		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue("principalId", userId);
		params.addValue("depth", NodeConstants.MAX_PATH_DEPTH_PLUS_ONE);
		String sql = String.format(DOWNLOAD_LIST_RESULT_TEMPLATE, tempTableName);
		List<DownloadListItemResult> unorderedResults = namedJdbcTemplate.query(sql, params, RESULT_MAPPER);

		// Put the results in same order as the request. Note: O(n*m) where both n and m
		// should be small.
		return Arrays.stream(items).map(i -> unorderedResults.stream().filter(u -> isMatch(i, u)).findFirst().get())
				.collect(Collectors.toList());

	}

	/**
	 * Does the given DownloadListItem match the given DownloadListItemResult?
	 * 
	 * @param item
	 * @param result
	 * @return
	 */
	public static boolean isMatch(DownloadListItem item, DownloadListItemResult result) {
		if (item.getFileEntityId().equals(result.getFileEntityId())) {
			return Objects.equal(item.getVersionNumber(), result.getVersionNumber());
		}
		return false;
	}

	@Override
	public List<DownloadListItemResult> getFilesAvailableToDownloadFromDownloadList(EntityAccessCallback accessCallback,
			Long userId, List<Sort> sort, Long limit, Long offset) {
		/*
		 * The first step is to create a temporary table containing all of the entity
		 * IDs from the user's download list that the user can download.
		 */
		String tempTableName = createTempoaryTableOfAvailableFiles(accessCallback, userId, BATCH_SIZE);

		String sql = String.format(
				"SELECT T.ENTITY_ID, D." + COL_DOWNLOAD_LIST_ITEM_V2_VERION_NUMBER + ", D."
						+ COL_DOWNLOAD_LIST_ITEM_V2_ADDED_ON + " FROM %S T JOIN " + TABLE_DOWNLOAD_LIST_ITEM_V2
						+ " D ON (T.ENTITY_ID = D.ENTITY_ID AND " + COL_DOWNLOAD_LIST_V2_PRINCIPAL_ID + " = ?)",
				tempTableName);
		return jdbcTemplate.query(sql, RESULT_MAPPER, userId);
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
		jdbcTemplate.update(sql);
		
		List<Long> batch = null;
		long limit = batchSize;
		long offset = 0L;
		do {
			// Gather a single batch of distinct entity IDs from the user's download list
			batch = jdbcTemplate.queryForList(
					"SELECT DISTINCT " + COL_DOWNLOAD_LIST_ITEM_V2_ENTITY_ID + " FROM " + TABLE_DOWNLOAD_LIST_ITEM_V2
							+ " WHERE " + COL_DOWNLOAD_LIST_ITEM_V2_PRINCIPAL_ID + " = ? LIMIT ? OFFSET ?",
					new Long[] { userId, limit, offset }, Long.class);
			offset += limit;
			// Determine the sub-set that the user can actually download.
			List<Long> canDownload = accessCallback.canDownload(batch);
			// Add the sub-set to the temporary table.
			addBatchOfEntityIdsToTempTable(canDownload.toArray(new Long[canDownload.size()]), tableName);
		}while(batch.size() == batchSize);
		
		return tableName;
	}

	@Override
	public List<Long> readTempoaryTableOfAvailableFiles(EntityAccessCallback accessCallback, Long userId,
			int batchSize) {
		String tempTableName = createTempoaryTableOfAvailableFiles(accessCallback, userId, batchSize);
		return jdbcTemplate.queryForList("SELECT ENTITY_ID FROM " + tempTableName+" ORDER BY ENTITY_ID ASC", Long.class);
	}

	/**
	 * Helper to add the given batch of entity IDs to a temporary table.
	 * 
	 * @param entityIdsToAdd
	 * @param tableName
	 */
	void addBatchOfEntityIdsToTempTable(Long[] entityIdsToAdd, String tableName) {
		if (entityIdsToAdd.length < 1) {
			return;
		}
		String sql = String.format("INSERT INTO %S (ENTITY_ID) VALUES (?)", tableName);
		jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
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
