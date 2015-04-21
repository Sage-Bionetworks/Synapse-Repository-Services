package org.sagebionetworks.repo.model.dbo.dao;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_FILES_CONTENT_SIZE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_FILES_CONTENT_TYPE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_FILES_CREATED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_FILES_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_FILES_METADATA_TYPE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.LIMIT_PARAM_NAME;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.OFFSET_PARAM_NAME;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_FILES;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.StorageUsageQueryDao;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.persistence.DBOFileHandle;
import org.sagebionetworks.repo.model.storage.StorageUsage;
import org.sagebionetworks.repo.model.storage.StorageUsageDimension;
import org.sagebionetworks.repo.model.storage.StorageUsageDimensionValue;
import org.sagebionetworks.repo.model.storage.StorageUsageSummary;
import org.sagebionetworks.repo.model.storage.StorageUsageSummaryList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;

public final class StorageUsageQueryDaoImpl implements StorageUsageQueryDao {

	private static final String S3_FILTER = COL_FILES_METADATA_TYPE + " = 'S3'";

	private static final String SELECT_SUM_SIZE =
			"SELECT SUM(" + COL_FILES_CONTENT_SIZE + ")" +
			" FROM " + TABLE_FILES;

	private static final String SELECT_SUM_SIZE_FOR_USER =
			"SELECT SUM(" + COL_FILES_CONTENT_SIZE + ")" +
			" FROM " + TABLE_FILES +
			" WHERE " + COL_FILES_CREATED_BY + " = :" + COL_FILES_CREATED_BY;

	private static final String SELECT_COUNT =
			"SELECT COUNT(" + COL_FILES_ID + ")" +
			" FROM " + TABLE_FILES;

	private static final String SELECT_COUNT_FOR_USER =
			"SELECT COUNT(" + COL_FILES_ID + ")" +
			" FROM " + TABLE_FILES +
			" WHERE " + COL_FILES_CREATED_BY + " = :" + COL_FILES_CREATED_BY;

	/**
	 * Provides mapping from StorageUsageDimension to Files table columns.
	 */
	private static final Map<String, String> DIM_COL_MAP;
	static {
		Map<String, String> map = new HashMap<String, String>();
		map.put(StorageUsageDimension.CONTENT_TYPE.name(), COL_FILES_CONTENT_TYPE);
		map.put(StorageUsageDimension.STORAGE_PROVIDER.name(), COL_FILES_METADATA_TYPE);
		map.put(StorageUsageDimension.USER_ID.name(), COL_FILES_CREATED_BY);
		DIM_COL_MAP = Collections.unmodifiableMap(map);
	}

	/**
	 * Provides mapping from Files table columns to StorageUsageDimension.
	 */
	private static final Map<String, String> COL_DIM_MAP;
	static {
		Map<String, String> map = new HashMap<String, String>();
		map.put(COL_FILES_CONTENT_TYPE, StorageUsageDimension.CONTENT_TYPE.name());
		map.put(COL_FILES_METADATA_TYPE, StorageUsageDimension.STORAGE_PROVIDER.name());
		map.put(COL_FILES_CREATED_BY, StorageUsageDimension.USER_ID.name());
		COL_DIM_MAP = Collections.unmodifiableMap(map);
	}

	private static final String COL_SUM_SIZE = "SUM_SIZE";
	private static final String COL_COUNT_ID = "COUNT_ID";
	private static final String SELECT_AGGREGATED_USAGE_PART_1 =
			"SELECT" +
			" SUM(" + COL_FILES_CONTENT_SIZE + ") AS " + COL_SUM_SIZE + ", " +
			" COUNT(" + COL_FILES_ID + ") AS " + COL_COUNT_ID;
	private static final String SELECT_AGGREGATED_USAGE_PART_2 =
			" FROM " + TABLE_FILES +
			" GROUP BY ";
	private static final String SELECT_AGGREGATED_USAGE_FOR_USER_PART_2 =
			" FROM " + TABLE_FILES +
			" WHERE " + COL_FILES_CREATED_BY + " = :" + COL_FILES_CREATED_BY +
			" GROUP BY ";

	private static final String ORDER_BY_DESC_LIMIT =
			" ORDER BY " + COL_SUM_SIZE + " DESC, " + COL_COUNT_ID + " DESC " +
			" LIMIT :" + LIMIT_PARAM_NAME + " OFFSET :" + OFFSET_PARAM_NAME;

	private static final String SELECT_STORAGE_USAGE_FOR_USER_PAGINATED =
			"SELECT *" +
			" FROM " + TABLE_FILES +
			" WHERE " + COL_FILES_CREATED_BY + " = :" + COL_FILES_CREATED_BY +
			" LIMIT :" + LIMIT_PARAM_NAME + " OFFSET :" + OFFSET_PARAM_NAME;

	private static final RowMapper<DBOFileHandle> rowMapper = (new DBOFileHandle()).getTableMapping();

	@Autowired
	private DBOBasicDao basicDao;

	@Autowired
	private SimpleJdbcTemplate simpleJdbcTemplate;

	@Override
	public Long getTotalSize() throws DatastoreException {
		return getTotalSize(true);
	}

	@Override
	public Long getTotalSizeForUser(Long userId) throws DatastoreException {

		if (userId == null) {
			throw new IllegalArgumentException("User ID cannot be null or empty.");
		}

		return getTotalSizeForUser(userId, true);
	}

	@Override
	public Long getTotalCount() throws DatastoreException {
		return getTotalCount(true);
	}

	@Override
	public Long getTotalCountForUser(Long userId) throws DatastoreException {

		if (userId == null) {
			throw new IllegalArgumentException("User ID cannot be null or empty.");
		}

		return getTotalCountForUser(userId, true);
	}

	@Override
	public StorageUsageSummaryList getAggregatedUsage(List<StorageUsageDimension> dimensionList)
			throws DatastoreException, InvalidModelException {

		if (dimensionList == null) {
			throw new IllegalArgumentException("Dimension list cannot be null.");
		}

		StorageUsageSummaryList summaryList = getAggregatedResults(dimensionList,
				SELECT_AGGREGATED_USAGE_PART_1, SELECT_AGGREGATED_USAGE_PART_2);

		return summaryList;
	}

	@Override
	public StorageUsageSummaryList getAggregatedUsageForUser(Long userId,
			List<StorageUsageDimension> dimensionList)
			throws DatastoreException, InvalidModelException {

		if (userId == null) {
			throw new IllegalArgumentException("User ID cannot be null or empty.");
		}
		if (dimensionList == null) {
			throw new IllegalArgumentException("Dimension list cannot be null.");
		}

		StorageUsageSummaryList summaryList = getAggregatedResultsForUser(userId, dimensionList,
				SELECT_AGGREGATED_USAGE_PART_1, SELECT_AGGREGATED_USAGE_FOR_USER_PART_2);

		return summaryList;
	}

	@Override
	public StorageUsageSummaryList getAggregatedUsageByUserInRange(long beginIncl, long endExcl) {

		if (beginIncl >= endExcl) {
			String msg = "begin must be greater than end (begin = " + beginIncl;
			msg += "; end = ";
			msg += endExcl;
			msg += ")";
			throw new IllegalArgumentException(msg);
		}

		StorageUsageSummaryList summaryList = getAggregatedResults(COL_FILES_CREATED_BY,
				SELECT_AGGREGATED_USAGE_PART_1, SELECT_AGGREGATED_USAGE_PART_2, beginIncl, endExcl);

		return summaryList;
	}

	@Override
	public List<StorageUsage> getUsageInRangeForUser(Long userId, long beginIncl, long endExcl)
			throws DatastoreException {

		if (userId == null) {
			throw new IllegalArgumentException("User ID cannot be null or empty.");
		}

		if (beginIncl >= endExcl) {
			String msg = "begin must be greater than end [begin=" + beginIncl;
			msg += ", end=";
			msg += endExcl;
			msg += "]";
			throw new IllegalArgumentException(msg);
		}

		MapSqlParameterSource paramMap = new MapSqlParameterSource();
		paramMap.addValue(OFFSET_PARAM_NAME, beginIncl);
		paramMap.addValue(LIMIT_PARAM_NAME, endExcl - beginIncl);
		paramMap.addValue(COL_FILES_CREATED_BY, userId);
		List<DBOFileHandle> dboList = simpleJdbcTemplate.query(
				SELECT_STORAGE_USAGE_FOR_USER_PAGINATED, rowMapper, paramMap);

		List<StorageUsage> usageList = new ArrayList<StorageUsage>();
		for (DBOFileHandle dbo : dboList) {
			StorageUsage su = new StorageUsage();
			usageList.add(su);
			su.setId(dbo.getId().toString());
			su.setName(dbo.getName());
			su.setStorageProvider(dbo.getMetadataTypeEnum().name());
			su.setLocation(dbo.getKey());
			su.setUserId(dbo.getCreatedBy().toString());
			su.setCreatedOn(dbo.getCreatedOn());
			su.setContentType(dbo.getContentType());
			su.setContentSize(dbo.getContentSize());
			su.setContentMd5(dbo.getContentMD5());
		}

		usageList = Collections.unmodifiableList(usageList);
		return usageList;
	}

	// Private Methods ////////////////////////////////////////////////////////////////////////////

	private Long getTotalSize(boolean s3Only) throws DatastoreException {
		String sql = SELECT_SUM_SIZE;
		if (s3Only) {
			sql = sql + " WHERE " + S3_FILTER;
		}
		long total = simpleJdbcTemplate.queryForLong(sql);
		return total;
	}

	private Long getTotalSizeForUser(Long userId, boolean s3Only) throws DatastoreException {

		String sql = SELECT_SUM_SIZE_FOR_USER;
		if (s3Only) {
			sql = sql + " AND " + S3_FILTER;
		}

		MapSqlParameterSource paramMap = new MapSqlParameterSource();
		paramMap.addValue(COL_FILES_CREATED_BY, userId);
		long total = simpleJdbcTemplate.queryForLong(sql, paramMap);
		return total;
	}

	private Long getTotalCount(boolean s3Only) throws DatastoreException {
		String sql = s3Only ? SELECT_COUNT + " WHERE " + S3_FILTER : SELECT_COUNT;
		long count = simpleJdbcTemplate.queryForLong(sql);
		return count;
	}

	private Long getTotalCountForUser(Long userId, boolean s3Only) throws DatastoreException {

		String sql = SELECT_COUNT_FOR_USER;
		if (s3Only) {
			sql = sql + " AND " + S3_FILTER;
		}

		MapSqlParameterSource paramMap = new MapSqlParameterSource();
		paramMap.addValue(COL_FILES_CREATED_BY, userId);
		Long count = simpleJdbcTemplate.queryForLong(sql, paramMap);
		return count;
	}

	/**
	 * Gets aggregated results. The results will be sorted in descending order.
	 * Gets the specific type of aggregations (sum, count, etc.)
	 * by passing in the appropriate SQL parts.
	 */
	private StorageUsageSummaryList getAggregatedResults(String column,
			String sqlPart1, String sqlPart2, long beginIncl, long endExcl) {

		assert column != null;
		assert sqlPart1 != null;
		assert sqlPart2 != null;
		assert beginIncl < endExcl;

		List<String> columnList = new ArrayList<String>(1);
		columnList.add(column);

		String sql = getAggregateSql(sqlPart1, sqlPart2, columnList);
		sql = sql + ORDER_BY_DESC_LIMIT;

		MapSqlParameterSource paramMap = new MapSqlParameterSource();
		paramMap.addValue(OFFSET_PARAM_NAME, beginIncl);
		paramMap.addValue(LIMIT_PARAM_NAME, endExcl - beginIncl);

		List<Map<String, Object>> rows = simpleJdbcTemplate.queryForList(sql, paramMap);
		StorageUsageSummaryList summaryList = createEmptySummaryList(null);
		List<StorageUsageSummary> summaries = summaryList.getSummaryList();
		fillSummaryList(columnList, summaries, rows);

		return summaryList;
	}

	/**
	 * Gets aggregated results. Gets the specific type of aggregations (sum, count, etc.)
	 * by passing in the appropriate SQL parts. 
	 */
	private StorageUsageSummaryList getAggregatedResults(List<StorageUsageDimension> dimensionList,
			String sqlPart1, String sqlPart2) {

		assert dimensionList != null;
		assert sqlPart1 != null;
		assert sqlPart2 != null;

		StorageUsageSummaryList summaryList = createEmptySummaryList(null);
		if (dimensionList.isEmpty()) {
			return summaryList;
		}

		List<String> columnList = getGroupByColumns(dimensionList);
		assert columnList.size() > 0 : "We should have returned otherwise.";

		String sql = getAggregateSql(sqlPart1, sqlPart2, columnList);
		List<Map<String, Object>> rows = simpleJdbcTemplate.queryForList(sql);
		List<StorageUsageSummary> summaries = summaryList.getSummaryList();
		fillSummaryList(columnList, summaries, rows);

		return summaryList;
	}

	/**
	 * Gets aggregated results for user. Gets the specific type of aggregations (sum, count, etc.)
	 * by passing in the appropriate SQL parts. 
	 */
	private StorageUsageSummaryList getAggregatedResultsForUser(Long userId,
			List<StorageUsageDimension> dimensionList, String sqlPart1, String sqlPart2)
			throws DatastoreException, InvalidModelException {

		assert userId != null;
		assert dimensionList != null;
		assert sqlPart1 != null;
		assert sqlPart2 != null;

		StorageUsageSummaryList summaryList = createEmptySummaryList(userId);
		if (dimensionList.isEmpty()) {
			return summaryList;
		}

		List<String> columnList = getGroupByColumns(dimensionList);
		assert columnList.size() > 0 : "We should have returned otherwise.";

		String sql = getAggregateSql(sqlPart1, sqlPart2, columnList);
		MapSqlParameterSource paramMap = new MapSqlParameterSource();
		paramMap.addValue(COL_FILES_CREATED_BY, userId);
		List<Map<String, Object>> rows = simpleJdbcTemplate.queryForList(sql, paramMap);
		List<StorageUsageSummary> summaries = summaryList.getSummaryList();
		fillSummaryList(columnList, summaries, rows);

		return summaryList;
	}

	private StorageUsageSummaryList createEmptySummaryList(Long userId) {

		StorageUsageSummaryList summaryList = new StorageUsageSummaryList();

		if (userId != null) {
			Long usage = getTotalSizeForUser(userId, false);
			summaryList.setTotalSize(usage);
			Long count = getTotalCountForUser(userId, false);
			summaryList.setTotalCount(count);
		} else {
			Long usage = getTotalSize(false);
			summaryList.setTotalSize(usage);
			Long count = getTotalCount(false);
			summaryList.setTotalCount(count);
		}

		List<StorageUsageSummary> susList = new ArrayList<StorageUsageSummary>();
		summaryList.setSummaryList(susList);

		return summaryList;
	}

	/**
	 * @throws InvalidModelException When the list of dimensions has invalid column names
	 */
	private List<String> getGroupByColumns(List<StorageUsageDimension> dimensionList) {

		assert dimensionList != null;
		assert dimensionList.size() > 0;

		// The list is needed to preserve the aggregation order
		List<String> colNameList = new ArrayList<String>();
		// The set is needed to remove duplicate dimensions
		Set<String> colNameSet = new HashSet<String>();
		for (StorageUsageDimension d : dimensionList) {
			String colName = DIM_COL_MAP.get(d.name().toUpperCase());
			if (colName == null) {
				throw new InvalidModelException("The aggregating dimension " + d.name() +
						" is not a valid dimension.");
			}
			if (!colNameSet.contains(colName)) {
				colNameSet.add(colName);
				colNameList.add(colName);
			}
		}

		return Collections.unmodifiableList(colNameList);
	}

	private String getAggregateSql(String sqlPart1, String sqlPart2,
			List<String> columnList) {

		assert columnList != null;
		assert columnList.size() > 0;

		StringBuilder sql = new StringBuilder(sqlPart1);
		for (String column : columnList) {
			sql.append(", ");
			sql.append(column);
		}
		sql.append(sqlPart2);
		sql.append(columnList.get(0));
		int i = 1;
		while (i < columnList.size()) {
			sql.append(", ");
			sql.append(columnList.get(i));
			i++;
		}

		return sql.toString();
	}

	private void fillSummaryList(List<String> columnList,
			List<StorageUsageSummary> summaryList, List<Map<String, Object>> rowList) {

		assert columnList != null;
		assert columnList.size() > 0;
		assert summaryList != null;
		assert rowList != null;

		for (Map<String, Object> row : rowList) {

			StorageUsageSummary summary = new StorageUsageSummary();

			Object size = row.get(COL_SUM_SIZE);
			if (size == null) {
				summary.setAggregatedSize(0L);
			} else if (size instanceof BigDecimal) {
				summary.setAggregatedSize(((BigDecimal)size).longValue());
			} else if (size instanceof Long) {
				summary.setAggregatedSize(((Long)size).longValue());
			} else {
				throw new DatastoreException("Unknown type of 'size': " + size.getClass().getName());
			}
			size = null;

			Object count = row.get(COL_COUNT_ID);
			if (count == null) {
				summary.setAggregatedCount(0L);
			} else if (count instanceof BigDecimal) {
				summary.setAggregatedCount(((BigDecimal)count).longValue());
			} else if (count instanceof Long) {
				summary.setAggregatedCount(((Long)count).longValue());
			} else {
				throw new DatastoreException("Unknown type of 'size': " + count.getClass().getName());
			}
			count = null;

			List<StorageUsageDimensionValue> dValList = new ArrayList<StorageUsageDimensionValue>();
			for (String column : columnList) {
				Object valObj = row.get(column);
				String value = (valObj == null ? "UNKNOWN" : valObj.toString());
				StorageUsageDimensionValue val = new StorageUsageDimensionValue();
				StorageUsageDimension dim = StorageUsageDimension.valueOf(COL_DIM_MAP.get(column));
				val.setDimension(dim);
				val.setValue(value);
				dValList.add(val);
			}
			summary.setDimensionList(dValList);
			summaryList.add(summary);
		}

		assert summaryList.size() == rowList.size();
	}
}
