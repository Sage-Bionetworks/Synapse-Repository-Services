package org.sagebionetworks.repo.model.dbo.file.download.v2;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DOWNLOAD_LIST_ITEM_V2_ADDED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DOWNLOAD_LIST_ITEM_V2_ENTITY_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DOWNLOAD_LIST_ITEM_V2_PRINCIPAL_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DOWNLOAD_LIST_ITEM_V2_VERSION_NUMBER;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DOWNLOAD_LIST_V2_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DOWNLOAD_LIST_V2_PRINCIPAL_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DOWNLOAD_LIST_V2_UPDATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_FILES_CONTENT_SIZE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_FILES_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_FILES_METADATA_TYPE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_NODE_CURRENT_REV;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_NODE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_NODE_PARENT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_NODE_TYPE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_REVISION_FILE_HANDLE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_REVISION_NUMBER;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_REVISION_OWNER_NODE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_REVISION_USER_ANNOS_JSON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_DOWNLOAD_LIST_ITEM_V2;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_DOWNLOAD_LIST_V2;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_FILES;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_NODE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_REVISION;

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

import org.json.JSONObject;
import org.sagebionetworks.repo.model.EntityRef;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.NodeConstants;
import org.sagebionetworks.repo.model.annotation.v2.Annotations;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsV2Utils;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsValue;
import org.sagebionetworks.repo.model.dao.FileHandleMetadataType;
import org.sagebionetworks.repo.model.dbo.DDLUtilsImpl;
import org.sagebionetworks.repo.model.download.Action;
import org.sagebionetworks.repo.model.download.ActionRequiredCount;
import org.sagebionetworks.repo.model.download.AvailableFilter;
import org.sagebionetworks.repo.model.download.DownloadListItem;
import org.sagebionetworks.repo.model.download.DownloadListItemResult;
import org.sagebionetworks.repo.model.download.FilesStatisticsResponse;
import org.sagebionetworks.repo.model.download.MeetAccessRequirement;
import org.sagebionetworks.repo.model.download.RequestDownload;
import org.sagebionetworks.repo.model.download.Sort;
import org.sagebionetworks.repo.model.download.SortField;
import org.sagebionetworks.repo.model.file.FileConstants;
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

	public static final String ACTUAL_VERSION = "ACTUAL_VERSION";
	public static final String PROJECT_ID = "PROJECT_ID";
	public static final String PROJECT_NAME = "PROJECT_NAME";
	public static final String CONTENT_SIZE = "CONTENT_SIZE";
	public static final String CREATED_ON = "CREATED_ON";
	public static final String CREATED_BY = "CREATED_BY";
	public static final String ENTITY_NAME = "ENTITY_NAME";
	public static final String IS_ELIGIBLE_FOR_PACKAGING = "IS_ELIGIBLE_FOR_PACKAGING";

	public static final String DOWNLOAD_LIST_RESULT_TEMPLATE = DDLUtilsImpl
			.loadSQLFromClasspath("sql/DownloadListResultsTemplate.sql");
	
	public static final String DOWNLOAD_LIST_STATISTICS_TEMPLATE = DDLUtilsImpl
			.loadSQLFromClasspath("sql/DownloadListStatistics.sql");
	
	public static final String DOWNLOAD_LIST_ACTION_REQUIRED_TEMPLATE = DDLUtilsImpl
			.loadSQLFromClasspath("sql/DownloadListActionRequired.sql");
	
	public static final String TEMP_ACTION_REQUIRED_TEMPLATE = DDLUtilsImpl
			.loadSQLFromClasspath("sql/TempActionRequired-ddl.sql");

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
		r.setVersionNumber(rs.getLong(COL_DOWNLOAD_LIST_ITEM_V2_VERSION_NUMBER));
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
		r.setIsEligibleForPackaging(rs.getBoolean(IS_ELIGIBLE_FOR_PACKAGING));
		r.setFileHandleId(rs.getString(COL_FILES_ID));
		return r;
	};
	
	private static final RowMapper<FilesStatisticsResponse> STATS_MAPPER = (ResultSet rs, int rowNum) -> {
		FilesStatisticsResponse stats = new FilesStatisticsResponse();
		stats.setTotalNumberOfFiles(rs.getLong("TOTAL_FILE_COUNT"));
		stats.setNumberOfFilesAvailableForDownload(rs.getLong("AVAILABLE_COUNT"));
		stats.setNumberOfFilesAvailableForDownloadAndEligibleForPackaging(rs.getLong("ELIGIBLE_FOR_PACKAGING_COUNT"));
		stats.setSumOfFileSizesAvailableForDownload(rs.getLong("SUM_AVAIABLE_SIZE"));
		stats.setNumberOfFilesRequiringAction(
				stats.getTotalNumberOfFiles() - stats.getNumberOfFilesAvailableForDownload());
		return stats;
	};
	
	private static final RowMapper<ActionRequiredCount> ACTION_MAPPER = (ResultSet rs, int rowNum) -> {
		ActionType type = ActionType.valueOf(rs.getString("ACTION_TYPE"));
		Long actionId = rs.getLong("ACTION_ID");
		Long count = rs.getLong("COUNT");
		Action action = null;
		switch (type) {
		case ACCESS_REQUIREMENT:
			action = new MeetAccessRequirement().setAccessRequirementId(actionId);
			break;
		case DOWNLOAD_PERMISSION:
			action = new RequestDownload().setBenefactorId(actionId);
			break;
		default:
			throw new IllegalStateException("Unknown type: " + type.name());
		}
		return new ActionRequiredCount().setCount(count).setAction(action);
	};

	private static final RowMapper<JSONObject> JSON_OBJECT_MAPPER = (ResultSet rs, int rowNum) -> {
		JSONObject json = new JSONObject();
		for (ManifestKeys key : ManifestKeys.values()) {
			String value = rs.getString(key.getColumnName());
			if (ManifestKeys.ID.equals(key) || ManifestKeys.parentId.equals(key)) {
				value = "syn" + value;
			}
			json.put(key.name(), value);
		}
		String jsonString = rs.getString(COL_REVISION_USER_ANNOS_JSON);
		Annotations annos = AnnotationsV2Utils.fromJSONString(jsonString);
		if (annos != null && annos.getAnnotations() != null) {
			for (String key : annos.getAnnotations().keySet()) {
				AnnotationsValue value = annos.getAnnotations().get(key);
				if (value != null) {
					if (!json.has(key)) {
						json.put(key, AnnotationsV2Utils.toJSONString(value));
					}
				}
			}
		}
		return json;
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
						+ "," + COL_DOWNLOAD_LIST_ITEM_V2_ENTITY_ID + "," + COL_DOWNLOAD_LIST_ITEM_V2_VERSION_NUMBER
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
	public long removeBatchOfFilesFromDownloadList(Long userId, List<? extends DownloadListItem> batchToRemove) {
		ValidateArgument.required(userId, "User Id");
		if (batchToRemove == null || batchToRemove.isEmpty()) {
			return 0;
		}
		createOrUpdateDownloadList(userId);
		DownloadListItem[] batchArray = batchToRemove.toArray(new DownloadListItem[batchToRemove.size()]);
		int[] updates = jdbcTemplate.batchUpdate("DELETE FROM " + TABLE_DOWNLOAD_LIST_ITEM_V2 + " WHERE "
				+ COL_DOWNLOAD_LIST_V2_PRINCIPAL_ID + " = ? AND " + COL_DOWNLOAD_LIST_ITEM_V2_ENTITY_ID + " = ? AND "
				+ COL_DOWNLOAD_LIST_ITEM_V2_VERSION_NUMBER + " = ?", new BatchPreparedStatementSetter() {

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
				+ COL_DOWNLOAD_LIST_ITEM_V2_VERSION_NUMBER, LIST_ITEM_MAPPER, userId);
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
			params.addValue("maxEligibleSize", FileConstants.MAX_FILE_SIZE_ELIGIBLE_FOR_PACKAGING);
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
			Long userId, AvailableFilter filter, List<Sort> sort, Long limit, Long offset) {
		/*
		 * The first step is to create a temporary table containing all of the entity
		 * IDs from the user's download list that the user can download.
		 */
		String tempTableName = createTemporaryTableOfAvailableFiles(accessCallback, userId, BATCH_SIZE);
		try {
			StringBuilder sqlBuilder = new StringBuilder(String.format(DOWNLOAD_LIST_RESULT_TEMPLATE, tempTableName));
			sqlBuilder.append(buildAvailableFilter(filter));
			sqlBuilder.append(buildAvailableDownloadQuerySuffix(sort, limit, offset));
			MapSqlParameterSource params = new MapSqlParameterSource();
			params.addValue("principalId", userId);
			params.addValue("depth", NodeConstants.MAX_PATH_DEPTH_PLUS_ONE);
			params.addValue("limit", limit);
			params.addValue("offset", offset);
			params.addValue("maxEligibleSize", FileConstants.MAX_FILE_SIZE_ELIGIBLE_FOR_PACKAGING);
			return namedJdbcTemplate.query(sqlBuilder.toString(), params, RESULT_MAPPER);
		} finally {
			dropTemporaryTable(tempTableName);
		}
	}

	/**
	 * Build the where clause based on the provided filter.
	 * @param filter
	 * @return
	 */
	public static String buildAvailableFilter(AvailableFilter filter) {
		if (filter == null) {
			return "";
		}
		String typeOperator, conditionOperator, sizeOperator;
		switch (filter) {
		case eligibleForPackaging:
			typeOperator = "=";
			conditionOperator = "AND";
			sizeOperator = "<=";
			break;
		case ineligibleForPackaging:
			typeOperator = "<>";
			conditionOperator = "OR";
			sizeOperator = ">";
			break;
		default:
			throw new IllegalArgumentException("Unknown type: " + filter.name());
		}
		return String.format(
				" WHERE F." + COL_FILES_METADATA_TYPE + " %s '%s' %s F." + COL_FILES_CONTENT_SIZE
						+ " %s :maxEligibleSize",
				typeOperator, FileHandleMetadataType.S3, conditionOperator, sizeOperator);
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
		case isEligibleForPackaging:
			return IS_ELIGIBLE_FOR_PACKAGING;
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
			batch = getBatchOfFileIdsFromUsersDownloadList(userId, limit, offset);
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

	@WriteTransaction
	@Override
	public FilesStatisticsResponse getListStatistics(EntityAccessCallback createAccessCallback, Long userId) {
		/*
		 * The first step is to create a temporary table containing all of the entity
		 * IDs from the user's download list that the user can download.
		 */
		String tempTableName = createTemporaryTableOfAvailableFiles(createAccessCallback, userId, BATCH_SIZE);
		try {
			String sql = String.format(DOWNLOAD_LIST_STATISTICS_TEMPLATE, tempTableName);
			MapSqlParameterSource params = new MapSqlParameterSource();
			params.addValue("principalId", userId);
			params.addValue("maxEligibleSize", FileConstants.MAX_FILE_SIZE_ELIGIBLE_FOR_PACKAGING);
			return namedJdbcTemplate.queryForObject(sql, params,STATS_MAPPER);
		} finally {
			dropTemporaryTable(tempTableName);
		}
	}
	
	/**
	 * Helper to add the given batch of entity IDs to a temporary table.
	 * 
	 * @param entityIdsToAdd
	 * @param tableName
	 */
	void addBatchOfActionsToTempTable(FileActionRequired[] actions, String tableName) {
		if (actions.length < 1) {
			return;
		}
		String sql = String.format("INSERT IGNORE INTO %S (FILE_ID, ACTION_TYPE, ACTION_ID) VALUES (?,?,?)", tableName);
		jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
			@Override
			public void setValues(PreparedStatement ps, int i) throws SQLException {
				FileActionRequired required = actions[i];
				int index = 0;
				ps.setLong(++index, required.getFileId());
				Action action = required.getAction();
				if(action instanceof MeetAccessRequirement) {
					ps.setString(++index, ActionType.ACCESS_REQUIREMENT.name());
					ps.setLong(++index, ((MeetAccessRequirement)action).getAccessRequirementId());
				}else if(action instanceof RequestDownload) {
					ps.setString(++index, ActionType.DOWNLOAD_PERMISSION.name());
					ps.setLong(++index, ((RequestDownload)action).getBenefactorId());
				}else {
					throw new IllegalStateException("Unknown action type: "+action.getClass().getName());
				}
			}
			@Override
			public int getBatchSize() {
				return actions.length;
			}
		});
	}
	
	/**
	 * Create a temporary table of all actions the user must
	 * @param callback
	 * @param userId
	 * @param batchSize
	 * @return
	 */
	String createTemporaryTableOfActionsRequired(EntityActionRequiredCallback callback, Long userId, int batchSize) {
		String tableName = "U" + userId + "A";
		String sql = String.format(TEMP_ACTION_REQUIRED_TEMPLATE,tableName);
		jdbcTemplate.update(sql);

		List<Long> batch = null;
		long limit = batchSize;
		long offset = 0L;
		do {
			batch = getBatchOfFileIdsFromUsersDownloadList(userId, limit, offset);
			offset += limit;
			if (batch.isEmpty()) {
				break;
			}
			// Determine the sub-set that the user can actually download.
			List<FileActionRequired> actions = callback.filter(batch);
			// Add the sub-set to the temporary table.
			addBatchOfActionsToTempTable(actions.toArray(new FileActionRequired[actions.size()]), tableName);
		} while (batch.size() == batchSize);

		return tableName;
	}

	/**
	 * Get a batch of file IDs from the user's download list.
	 * @param userId
	 * @param limit
	 * @param offset
	 * @return
	 */
	List<Long> getBatchOfFileIdsFromUsersDownloadList(Long userId, long limit, long offset) {
		// Gather a single batch of distinct entity IDs from the user's download list
		return jdbcTemplate.queryForList("SELECT DISTINCT " + COL_DOWNLOAD_LIST_ITEM_V2_ENTITY_ID + " FROM "
				+ TABLE_DOWNLOAD_LIST_ITEM_V2 + " WHERE " + COL_DOWNLOAD_LIST_ITEM_V2_PRINCIPAL_ID + " = ? ORDER BY "
				+ COL_DOWNLOAD_LIST_ITEM_V2_ENTITY_ID
				+ " LIMIT ? OFFSET ?", Long.class, userId, limit, offset);
	}

	@WriteTransaction
	@Override
	public List<ActionRequiredCount> getActionsRequiredFromDownloadList(EntityActionRequiredCallback callback,
			Long userId, Long limit, Long offset) {
		/*
		 * Build a temp table of all actions the user must take to gain access to files on their download list.
		 */
		String tempTableName = createTemporaryTableOfActionsRequired(callback, userId, BATCH_SIZE);
		try {
			String sql = String.format(DOWNLOAD_LIST_ACTION_REQUIRED_TEMPLATE, tempTableName);
			MapSqlParameterSource params = new MapSqlParameterSource();
			params.addValue("principalId", userId);
			params.addValue("limit", limit);
			params.addValue("offset", offset);
			return namedJdbcTemplate.query(sql, params, ACTION_MAPPER);
		} finally {
			dropTemporaryTable(tempTableName);
		}
	}

	@WriteTransaction
	@Override
	public Long addChildrenToDownloadList(Long userId, Long parentId, boolean useVersion, long limit) {
		String versionString = useVersion ? COL_NODE_CURRENT_REV : "-1";
		String sql = String.format("INSERT IGNORE INTO " + TABLE_DOWNLOAD_LIST_ITEM_V2 + " ("
				+ COL_DOWNLOAD_LIST_ITEM_V2_PRINCIPAL_ID + "," + COL_DOWNLOAD_LIST_ITEM_V2_ENTITY_ID + ","
				+ COL_DOWNLOAD_LIST_ITEM_V2_VERSION_NUMBER + "," + COL_DOWNLOAD_LIST_ITEM_V2_ADDED_ON + ")  SELECT ?, "
				+ COL_NODE_ID + ", %s, NOW(3) FROM " + TABLE_NODE + " WHERE " + COL_NODE_PARENT_ID + " = ? AND "
				+ COL_NODE_TYPE + " = '" + EntityType.file.name() + "' LIMIT ?", versionString);
		createOrUpdateDownloadList(userId);
		return (long) jdbcTemplate.update(sql, userId, parentId, limit);
	}

	@Override
	public JSONObject getItemManifestDetails(DownloadListItem item) {
		String sql = null;
		if (item.getVersionNumber() == null) {
			sql = "SELECT " + ManifestKeys.buildSelect() + ", R." + COL_REVISION_USER_ANNOS_JSON + " FROM " + TABLE_NODE
					+ " N JOIN " + TABLE_REVISION + " R ON (N." + COL_NODE_ID + " = R." + COL_REVISION_OWNER_NODE
					+ " AND N." + COL_NODE_CURRENT_REV + " = R." + COL_REVISION_NUMBER + ") JOIN " + TABLE_FILES
					+ " F ON (R." + COL_REVISION_FILE_HANDLE_ID + " = F." + COL_FILES_ID + ") WHERE N." + COL_NODE_ID
					+ " = :synId";
		}else {
			sql = "SELECT " + ManifestKeys.buildSelect() + ", R." + COL_REVISION_USER_ANNOS_JSON + " FROM " + TABLE_NODE
					+ " N JOIN " + TABLE_REVISION + " R ON (N." + COL_NODE_ID + " = R." + COL_REVISION_OWNER_NODE
					+ " ) JOIN " + TABLE_FILES + " F ON (R." + COL_REVISION_FILE_HANDLE_ID + " = F." + COL_FILES_ID
					+ ") WHERE N." + COL_NODE_ID + " = :synId AND R." + COL_REVISION_NUMBER + " = :version";
		}
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue("synId", KeyFactory.stringToKey(item.getFileEntityId()));
		params.addValue("version", item.getVersionNumber());
		return namedJdbcTemplate.queryForObject(sql, params, JSON_OBJECT_MAPPER);
	}

	@WriteTransaction
	@Override
	public Long addDatasetItemsToDownloadList(Long userId, List<EntityRef> items, long limit) {
		createOrUpdateDownloadList(userId);
		String sql = "INSERT IGNORE INTO " + TABLE_DOWNLOAD_LIST_ITEM_V2 + " ("
				+ COL_DOWNLOAD_LIST_ITEM_V2_PRINCIPAL_ID + ", " + COL_DOWNLOAD_LIST_ITEM_V2_ENTITY_ID + ", "
				+ COL_DOWNLOAD_LIST_ITEM_V2_VERSION_NUMBER + ", " + COL_DOWNLOAD_LIST_ITEM_V2_ADDED_ON + ") "
				+ "VALUES(?, ?, ?, NOW(3))";
		items = items.subList(0, Math.min((int)limit, items.size()));
		EntityRef[] itemsArray = items.toArray(new EntityRef[items.size()]);
		int[] updates = jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
			@Override
			public void setValues(PreparedStatement ps, int i) throws SQLException {
				ps.setLong(1, userId);
				ps.setLong(2, KeyFactory.stringToKey(itemsArray[i].getEntityId()));
				ps.setLong(3, itemsArray[i].getVersionNumber());
			}
			
			@Override
			public int getBatchSize() {
				return itemsArray.length;
			}
		});
		return (long)IntStream.of(updates).sum();
	}

}
