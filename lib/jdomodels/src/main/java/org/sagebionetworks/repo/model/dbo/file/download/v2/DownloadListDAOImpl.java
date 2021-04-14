package org.sagebionetworks.repo.model.dbo.file.download.v2;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DOWNLOAD_LIST_ITEM_V2_ADDED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DOWNLOAD_LIST_ITEM_V2_ENTITY_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DOWNLOAD_LIST_ITEM_V2_PRINCIPAL_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DOWNLOAD_LIST_ITEM_V2_VERION_NUMBER;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DOWNLOAD_LIST_V2_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DOWNLOAD_LIST_V2_PRINCIPAL_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DOWNLOAD_LIST_V2_UPDATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_NODE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_NODE_TYPE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_DOWNLOAD_LIST_ITEM_V2;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_DOWNLOAD_LIST_V2;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_NODE;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.NodeConstants;
import org.sagebionetworks.repo.model.dbo.DDLUtilsImpl;
import org.sagebionetworks.repo.model.download.DownloadListItem;
import org.sagebionetworks.repo.model.download.DownloadListItemResult;
import org.sagebionetworks.repo.model.download.Sort;
import org.sagebionetworks.repo.model.download.SortField;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.google.common.base.Objects;

@Repository
public class DownloadListDAOImpl implements DownloadListDAO {

	private static final String ACTUAL_VERSION = "ACTUAL_VERSION";
	public static final String PROJECT_ID = "PROJECT_ID";
	public static final String PROJECT_NAME = "PROJECT_NAME";
	public static final String CONTENT_SIZE = "CONTENT_SIZE";
	public static final String CREATED_ON = "CREATED_ON";
	public static final String CREATED_BY = "CREATED_BY";
	public static final String ENTITY_NAME = "ENTITY_NAME";

	public static final String DOWNLOAD_LIST_RESULT_TEMPLATE = DDLUtilsImpl
			.loadSQLFromClasspath("sql/DownloadListResultsTemplate.sql");

	private static final int BATCH_SIZE = 10000;

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
		r.setFileName(rs.getString(ENTITY_NAME));
		r.setCreatedBy(rs.getString(CREATED_BY));
		r.setCreatedOn(new Date(rs.getLong(CREATED_ON)));
		r.setProjectId(KeyFactory.keyToString(rs.getLong(PROJECT_ID)));
		r.setProjectName(rs.getString(PROJECT_NAME));
		r.setFileSizeBytes(rs.getLong(CONTENT_SIZE));
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
		jdbcTemplate.update("INSERT INTO " + TABLE_DOWNLOAD_LIST_V2 + "(" + COL_DOWNLOAD_LIST_V2_PRINCIPAL_ID + ", "
				+ COL_DOWNLOAD_LIST_V2_UPDATED_ON + ", " + COL_DOWNLOAD_LIST_V2_ETAG
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
		String tempTableName = createTemporaryTableOfAvailableFiles(
				t -> t.stream().filter(i -> idsToKeep.contains(i)).collect(Collectors.toList()), userId, BATCH_SIZE);
		try {
			MapSqlParameterSource params = new MapSqlParameterSource();
			params.addValue("principalId", userId);
			params.addValue("depth", NodeConstants.MAX_PATH_DEPTH_PLUS_ONE);
			String sql = String.format(DOWNLOAD_LIST_RESULT_TEMPLATE, tempTableName);
			List<DownloadListItemResult> unorderedResults = namedJdbcTemplate.query(sql, params, RESULT_MAPPER);

			// Put the results in same order as the request. Note: O(n*m) where both n and m
			// should be small.
			return Arrays.stream(items).map(i -> unorderedResults.stream().filter(u -> isMatch(i, u)).findFirst().get())
					.collect(Collectors.toList());
		} finally {
			dropTemporaryTable(tempTableName);
		}
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

	@WriteTransaction
	@Override
	public List<DownloadListItemResult> getFilesAvailableToDownloadFromDownloadList(EntityAccessCallback accessCallback,
			Long userId, List<Sort> sort, Long limit, Long offset) {
		/*
		 * The first step is to create a temporary table containing all of the entity
		 * IDs from the user's download list that the user can download.
		 */
		String tempTableName = createTemporaryTableOfAvailableFiles(accessCallback, userId, BATCH_SIZE);
		try {
			StringBuilder sqlBuilder = new StringBuilder(String.format(DOWNLOAD_LIST_RESULT_TEMPLATE, tempTableName));
			sqlBuilder.append(buildAvailableDownloadQuerySuffix(sort, limit, offset));
			MapSqlParameterSource params = new MapSqlParameterSource();
			params.addValue("principalId", userId);
			params.addValue("depth", NodeConstants.MAX_PATH_DEPTH_PLUS_ONE);
			params.addValue("limit", limit);
			params.addValue("offset", offset);
			return namedJdbcTemplate.query(sqlBuilder.toString(), params, RESULT_MAPPER);
		} finally {
			dropTemporaryTable(tempTableName);
		}
	}

	/**
	 * Build the SQL suffix to handle both sorting and paging based on the provided
	 * sorting and paging.
	 * 
	 * @param sort
	 * @param limit
	 * @param offset
	 * @return
	 */
	public static String buildAvailableDownloadQuerySuffix(List<Sort> sort, Long limit, Long offset) {
		StringBuilder builder = new StringBuilder();
		if (sort != null && !sort.isEmpty()) {
			builder.append(" ORDER BY");
			builder.append(sort.stream().map(DownloadListDAOImpl::sortToSql).collect(Collectors.joining(",")));
		}
		if (limit != null) {
			builder.append(" LIMIT :limit");
		}
		if (offset != null) {
			builder.append(" OFFSET :offset");
		}
		return builder.toString();
	}

	/**
	 * Generate the SQL for the given Sort.
	 * 
	 * @param sort
	 * @return
	 */
	public static String sortToSql(Sort sort) {
		ValidateArgument.required(sort, "sort");
		ValidateArgument.required(sort.getField(), "sort.field");
		StringBuilder builder = new StringBuilder(" ");
		builder.append(getColumnName(sort.getField()));
		if (sort.getDirection() != null) {
			builder.append(" ").append(sort.getDirection().name());
		}
		return builder.toString();
	}

	/**
	 * Get the column name for the given SortField.
	 * 
	 * @param field
	 * @return
	 */
	public static String getColumnName(SortField field) {
		ValidateArgument.required(field, "field");
		switch (field) {
		case fileName:
			return ENTITY_NAME;
		case projectName:
			return PROJECT_NAME;
		case synId:
			return COL_DOWNLOAD_LIST_ITEM_V2_ENTITY_ID;
		case versionNumber:
			return ACTUAL_VERSION;
		case addedOn:
			return COL_DOWNLOAD_LIST_ITEM_V2_ADDED_ON;
		case createdBy:
			return CREATED_BY;
		case createdOn:
			return CREATED_ON;
		case fileSize:
			return CONTENT_SIZE;
		default:
			throw new IllegalArgumentException("Unknown SortField: " + field.name());
		}
	}

	/**
	 * Drop the given temporary table by name.
	 * 
	 * @param tempTableName
	 */
	void dropTemporaryTable(String tempTableName) {
		String sql = String.format("DROP TEMPORARY TABLE IF EXISTS %S ", tempTableName);
		jdbcTemplate.update(sql);
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
	String createTemporaryTableOfAvailableFiles(EntityAccessCallback accessCallback, Long userId, int batchSize) {
		String tableName = "U" + userId + "T";
		String sql = String.format("CREATE TEMPORARY TABLE %S (`ENTITY_ID` BIGINT NOT NULL)", tableName);
		jdbcTemplate.update(sql);

		List<Long> batch = null;
		long limit = batchSize;
		long offset = 0L;
		do {
			// Gather a single batch of distinct entity IDs from the user's download list
			batch = jdbcTemplate.queryForList(
					"SELECT DISTINCT " + COL_DOWNLOAD_LIST_ITEM_V2_ENTITY_ID + " FROM " + TABLE_DOWNLOAD_LIST_ITEM_V2
							+ " WHERE " + COL_DOWNLOAD_LIST_ITEM_V2_PRINCIPAL_ID + " = ? LIMIT ? OFFSET ?",
					Long.class, userId, limit, offset);
			offset += limit;
			if (batch.isEmpty()) {
				break;
			}
			// Determine the sub-set that the user can actually download.
			List<Long> canDownload = accessCallback.filter(batch);
			// Add the sub-set to the temporary table.
			addBatchOfEntityIdsToTempTable(canDownload.toArray(new Long[canDownload.size()]), tableName);
		} while (batch.size() == batchSize);

		return tableName;
	}

	@WriteTransaction
	@Override
	public List<Long> getAvailableFilesFromDownloadList(EntityAccessCallback accessCallback, Long userId,
			int batchSize) {
		String tempTableName = createTemporaryTableOfAvailableFiles(accessCallback, userId, batchSize);
		return jdbcTemplate.queryForList("SELECT ENTITY_ID FROM " + tempTableName + " ORDER BY ENTITY_ID ASC",
				Long.class);
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

	@Override
	public long getTotalNumberOfFilesOnDownloadList(Long userId) {
		ValidateArgument.required(userId, "userId");
		return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + TABLE_DOWNLOAD_LIST_ITEM_V2 + " WHERE "
				+ COL_DOWNLOAD_LIST_V2_PRINCIPAL_ID + " = ?", Long.class, userId);
	}

	@Override
	public List<DownloadListItem> filterUnsupportedTypes(List<DownloadListItem> batch) {
		ValidateArgument.required(batch, "batch");
		if(batch.isEmpty()) {
			return Collections.emptyList();
		}
		Set<Long> allIds = batch.stream().map(i -> KeyFactory.stringToKey(i.getFileEntityId()))
				.collect(Collectors.toSet());
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue("ids", allIds);
		Set<Long> fileIds = new HashSet<>(namedJdbcTemplate.queryForList("SELECT " + COL_NODE_ID + " FROM " + TABLE_NODE
				+ " WHERE " + COL_NODE_ID + " IN (:ids) AND " + COL_NODE_TYPE + " = '" + EntityType.file.name() + "'",
				params, Long.class));
		return batch.stream().filter(i -> fileIds.contains(KeyFactory.stringToKey(i.getFileEntityId())))
				.collect(Collectors.toList());
	}

}
