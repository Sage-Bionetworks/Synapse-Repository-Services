package org.sagebionetworks.repo.model.dbo.dao;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_STORAGE_LOCATION_CONTENT_SIZE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_STORAGE_LOCATION_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_STORAGE_LOCATION_NODE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_STORAGE_LOCATION_USER_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.LIMIT_PARAM_NAME;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.OFFSET_PARAM_NAME;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_STORAGE_LOCATION;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.LocationTypeNames;
import org.sagebionetworks.repo.model.StorageLocationDAO;
import org.sagebionetworks.repo.model.StorageLocations;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.persistence.DBOStorageLocation;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.storage.StorageUsage;
import org.sagebionetworks.repo.model.storage.StorageUsageDimension;
import org.sagebionetworks.repo.model.storage.StorageUsageDimensionValue;
import org.sagebionetworks.repo.model.storage.StorageUsageSummary;
import org.sagebionetworks.repo.model.storage.StorageUsageSummaryList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.ParameterizedSingleColumnRowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.s3.AmazonS3;

public final class StorageLocationDAOImpl implements StorageLocationDAO {

	private static final String SELECT_ID_FOR_NODE =
			"SELECT " + COL_STORAGE_LOCATION_ID +
			" FROM " + TABLE_STORAGE_LOCATION +
			" WHERE " + COL_STORAGE_LOCATION_NODE_ID + " = :" + COL_STORAGE_LOCATION_NODE_ID;

	private static final RowMapper<Long> idRowMapper = ParameterizedSingleColumnRowMapper.newInstance(Long.class);

	private static final String SELECT_STORAGE_USAGE =
			"SELECT SUM(" + COL_STORAGE_LOCATION_CONTENT_SIZE + ")" +
			" FROM " + TABLE_STORAGE_LOCATION;

	private static final String SELECT_STORAGE_USAGE_FOR_USER =
			"SELECT SUM(" + COL_STORAGE_LOCATION_CONTENT_SIZE + ")" +
			" FROM " + TABLE_STORAGE_LOCATION +
			" WHERE " + COL_STORAGE_LOCATION_USER_ID + " = :" + COL_STORAGE_LOCATION_USER_ID;

	private static final String SELECT_STORAGE_LOCATION_COUNT =
			"SELECT COUNT(" + COL_STORAGE_LOCATION_ID + ")" +
			" FROM " + TABLE_STORAGE_LOCATION;

	private static final String SELECT_STORAGE_LOCATION_COUNT_FOR_USER =
			"SELECT COUNT(" + COL_STORAGE_LOCATION_ID + ")" +
			" FROM " + TABLE_STORAGE_LOCATION +
			" WHERE " + COL_STORAGE_LOCATION_USER_ID + " = :" + COL_STORAGE_LOCATION_USER_ID;

	private static final String COL_RESULTS = "RESULTS";
	private static final String SELECT_AGGREGATED_USAGE_PART_1 =
			"SELECT SUM(" + COL_STORAGE_LOCATION_CONTENT_SIZE + ") AS " + COL_RESULTS;
	private static final String SELECT_AGGREGATED_USAGE_PART_2 =
			" FROM " + TABLE_STORAGE_LOCATION +
			" GROUP BY ";
	private static final String SELECT_AGGREGATED_USAGE_FOR_USER_PART_2 =
			" FROM " + TABLE_STORAGE_LOCATION +
			" WHERE " + COL_STORAGE_LOCATION_USER_ID + " = :" + COL_STORAGE_LOCATION_USER_ID +
			" GROUP BY ";

	private static final String SELECT_AGGREGATED_COUNT_PART_1 =
			"SELECT COUNT(" + COL_STORAGE_LOCATION_ID + ") AS " + COL_RESULTS;
	private static final String SELECT_AGGREGATED_COUNT_PART_2 =
			" FROM " + TABLE_STORAGE_LOCATION +
			" GROUP BY ";
	private static final String SELECT_AGGREGATED_COUNT_FOR_USER_PART_2 =
			" FROM " + TABLE_STORAGE_LOCATION +
			" WHERE " + COL_STORAGE_LOCATION_USER_ID + " = :" + COL_STORAGE_LOCATION_USER_ID +
			" GROUP BY ";

	private static final String ORDER_BY_DESC_LIMIT =
			" ORDER BY " + COL_RESULTS + " DESC " +
			" LIMIT :" + LIMIT_PARAM_NAME + " OFFSET :" + OFFSET_PARAM_NAME;

	private static final String SELECT_STORAGE_LOCATION_FOR_USER_PAGINATED =
			"SELECT *" +
			" FROM " + TABLE_STORAGE_LOCATION +
			" WHERE " + COL_STORAGE_LOCATION_USER_ID + " = :" + COL_STORAGE_LOCATION_USER_ID +
			" LIMIT :" + LIMIT_PARAM_NAME + " OFFSET :" + OFFSET_PARAM_NAME;

	private static final RowMapper<DBOStorageLocation> rowMapper = (new DBOStorageLocation()).getTableMapping();

	@Autowired
	private DBOBasicDao basicDao;

	@Autowired
	private SimpleJdbcTemplate simpleJdbcTemplate;

	@Autowired
	private AmazonS3 amazonS3Client;

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void replaceLocationData(StorageLocations locations)
			throws DatastoreException {

		if (locations == null) {
			return;
		}

		// First DELETE
		// Note: To avoid extensive locking, we select then delete by id.
		Long nodeId = locations.getNodeId();
		MapSqlParameterSource paramMap = new MapSqlParameterSource();
		paramMap.addValue(COL_STORAGE_LOCATION_NODE_ID, nodeId);
		List<Long> idList = simpleJdbcTemplate.query(SELECT_ID_FOR_NODE, idRowMapper, paramMap);
		for (Long id : idList) {
			MapSqlParameterSource params = new MapSqlParameterSource();
			params.addValue(COL_STORAGE_LOCATION_ID.toLowerCase(), id);
			basicDao.deleteObjectById(DBOStorageLocation.class, params);
		}

		// Then CREATE
		try {
			List<DBOStorageLocation> batch = StorageLocationUtils.createBatch(
					locations, amazonS3Client);
			if (batch.size() > 0) {
				basicDao.createBatch(batch);
			}
		} catch (AmazonClientException e) {
			throw new DatastoreException(e);
		}
	}

	@Override
	public Long getUsage() throws DatastoreException {
		long total = simpleJdbcTemplate.queryForLong(SELECT_STORAGE_USAGE);
		return total;
	}

	@Override
	public Long getUsageForUser(String userId) throws DatastoreException {

		if (userId == null || userId.isEmpty()) {
			throw new NullPointerException();
		}

		MapSqlParameterSource paramMap = new MapSqlParameterSource();
		Long userIdLong = KeyFactory.stringToKey(userId);
		paramMap.addValue(COL_STORAGE_LOCATION_USER_ID, userIdLong);
		long total = simpleJdbcTemplate.queryForLong(SELECT_STORAGE_USAGE_FOR_USER, paramMap);

		return total;
	}

	@Override
	public Long getCount() throws DatastoreException {
		long count = simpleJdbcTemplate.queryForLong(SELECT_STORAGE_LOCATION_COUNT);
		return count;
	}

	@Override
	public Long getCountForUser(String userId) throws DatastoreException {

		if (userId == null || userId.isEmpty()) {
			throw new NullPointerException();
		}

		MapSqlParameterSource paramMap = new MapSqlParameterSource();
		Long userIdLong = KeyFactory.stringToKey(userId);
		paramMap.addValue(COL_STORAGE_LOCATION_USER_ID, userIdLong);
		Long count = simpleJdbcTemplate.queryForLong(SELECT_STORAGE_LOCATION_COUNT_FOR_USER, paramMap);
		return count;
	}

	@Override
	public StorageUsageSummaryList getAggregatedUsage(
			List<StorageUsageDimension> dimensionList)
			throws DatastoreException, InvalidModelException {

		if (dimensionList == null) {
			throw new NullPointerException();
		}

		StorageUsageSummaryList summaryList = getAggregatedResults(dimensionList,
				SELECT_AGGREGATED_USAGE_PART_1, SELECT_AGGREGATED_USAGE_PART_2);

		return summaryList;
	}

	@Override
	public StorageUsageSummaryList getAggregatedUsageForUser(String userId,
			List<StorageUsageDimension> dimensionList)
			throws DatastoreException, InvalidModelException {

		if (userId == null || userId.isEmpty()) {
			throw new NullPointerException();
		}
		if (dimensionList == null) {
			throw new NullPointerException();
		}

		StorageUsageSummaryList summaryList = getAggregatedResultsForUser(userId, dimensionList,
				SELECT_AGGREGATED_USAGE_PART_1, SELECT_AGGREGATED_USAGE_FOR_USER_PART_2);

		return summaryList;
	}

	@Override
	public StorageUsageSummaryList getAggregatedCount(
			List<StorageUsageDimension> dimensionList)
			throws DatastoreException, InvalidModelException {

		if (dimensionList == null) {
			throw new NullPointerException();
		}

		StorageUsageSummaryList summaryList = getAggregatedResults(dimensionList,
				SELECT_AGGREGATED_COUNT_PART_1, SELECT_AGGREGATED_COUNT_PART_2);

		return summaryList;
	}

	@Override
	public StorageUsageSummaryList getAggregatedCountForUser(String userId,
			List<StorageUsageDimension> dimensionList)
			throws DatastoreException, InvalidModelException {

		if (userId == null || userId.isEmpty()) {
			throw new NullPointerException();
		}
		if (dimensionList == null) {
			throw new NullPointerException();
		}

		StorageUsageSummaryList summaryList = getAggregatedResultsForUser(userId, dimensionList,
				SELECT_AGGREGATED_COUNT_PART_1, SELECT_AGGREGATED_COUNT_FOR_USER_PART_2);

		return summaryList;
	}

	@Override
	public List<StorageUsage> getUsageInRangeForUser(String userId, long beginIncl, long endExcl)
			throws DatastoreException {

		if (userId == null || userId.isEmpty()) {
			throw new NullPointerException();
		}

		if (beginIncl >= endExcl) {
			String msg = "begin must be greater than end (begin = " + beginIncl;
			msg += "; end = ";
			msg += endExcl;
			msg += ")";
			throw new IllegalArgumentException(msg);
		}

		MapSqlParameterSource paramMap = new MapSqlParameterSource();
		paramMap.addValue(OFFSET_PARAM_NAME, beginIncl);
		paramMap.addValue(LIMIT_PARAM_NAME, endExcl - beginIncl);
		Long userIdLong = KeyFactory.stringToKey(userId);
		paramMap.addValue(COL_STORAGE_LOCATION_USER_ID, userIdLong);
		List<DBOStorageLocation> dboList = simpleJdbcTemplate.query(
				SELECT_STORAGE_LOCATION_FOR_USER_PAGINATED, rowMapper, paramMap);

		List<StorageUsage> usageList = new ArrayList<StorageUsage>();
		for (DBOStorageLocation dbo : dboList) {
			StorageUsage su = new StorageUsage();
			usageList.add(su);
			su.setId(dbo.getId().toString());
			su.setNodeId(KeyFactory.keyToString(dbo.getNodeId()));
			su.setUserId(dbo.getUserId().toString());
			su.setIsAttachment(dbo.getIsAttachment());
			su.setLocation(dbo.getLocation());
			su.setStorageProvider(LocationTypeNames.valueOf(dbo.getStorageProvider()));
			su.setContentType(dbo.getContentType());
			su.setContentSize(dbo.getContentSize());
			su.setContentMd5(dbo.getContentMd5());
		}

		usageList = Collections.unmodifiableList(usageList);
		return usageList;
	}

	@Override
	public StorageUsageSummaryList getAggregatedCountByUserInRange(long beginIncl, long endExcl) {

		if (beginIncl >= endExcl) {
			String msg = "begin must be greater than end (begin = " + beginIncl;
			msg += "; end = ";
			msg += endExcl;
			msg += ")";
			throw new IllegalArgumentException(msg);
		}

		StorageUsageSummaryList summaryList = getAggregatedResults(COL_STORAGE_LOCATION_USER_ID,
				SELECT_AGGREGATED_COUNT_PART_1, SELECT_AGGREGATED_COUNT_PART_2, beginIncl, endExcl);

		return summaryList;
	}

	@Override
	public StorageUsageSummaryList getAggregatedCountByNodeInRange(long beginIncl, long endExcl) {

		if (beginIncl >= endExcl) {
			String msg = "begin must be greater than end (begin = " + beginIncl;
			msg += "; end = ";
			msg += endExcl;
			msg += ")";
			throw new IllegalArgumentException(msg);
		}

		StorageUsageSummaryList summaryList = getAggregatedResults(COL_STORAGE_LOCATION_NODE_ID,
				SELECT_AGGREGATED_COUNT_PART_1, SELECT_AGGREGATED_COUNT_PART_2, beginIncl, endExcl);

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

		StorageUsageSummaryList summaryList = getAggregatedResults(COL_STORAGE_LOCATION_USER_ID,
				SELECT_AGGREGATED_USAGE_PART_1, SELECT_AGGREGATED_USAGE_PART_2, beginIncl, endExcl);

		return summaryList;
	}

	@Override
	public StorageUsageSummaryList getAggregatedUsageByNodeInRange(long beginIncl, long endExcl) {

		if (beginIncl >= endExcl) {
			String msg = "begin must be greater than end (begin = " + beginIncl;
			msg += "; end = ";
			msg += endExcl;
			msg += ")";
			throw new IllegalArgumentException(msg);
		}

		StorageUsageSummaryList summaryList = getAggregatedResults(COL_STORAGE_LOCATION_NODE_ID,
				SELECT_AGGREGATED_USAGE_PART_1, SELECT_AGGREGATED_USAGE_PART_2, beginIncl, endExcl);

		return summaryList;
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
	private StorageUsageSummaryList getAggregatedResultsForUser(String userId,
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
		Long userIdLong = KeyFactory.stringToKey(userId);
		paramMap.addValue(COL_STORAGE_LOCATION_USER_ID, userIdLong);
		List<Map<String, Object>> rows = simpleJdbcTemplate.queryForList(sql, paramMap);
		List<StorageUsageSummary> summaries = summaryList.getSummaryList();
		fillSummaryList(columnList, summaries, rows);

		return summaryList;
	}

	private StorageUsageSummaryList createEmptySummaryList(String userId) {

		StorageUsageSummaryList summaryList = new StorageUsageSummaryList();

		if (userId != null) {
			Long usage = getUsageForUser(userId);
			summaryList.setUsage(usage);
			Long count = getCountForUser(userId);
			summaryList.setCount(count);
		} else {
			Long usage = getUsage();
			summaryList.setUsage(usage);
			Long count = getCount();
			summaryList.setCount(count);
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

		List<String> colNameList = new ArrayList<String>();
		// The set below is used to remove duplicate dimensions.
		Set<String> colNameSet = new HashSet<String>();
		FieldColumn[] columns = (new DBOStorageLocation()).getTableMapping().getFieldColumns();
		// Both lists should be small thus we can afford a N^2 lookup.
		for (StorageUsageDimension d : dimensionList) {
			String dimName = d.name().toUpperCase();
			if (!colNameSet.contains(dimName)) {
				// Not already in the list
				boolean found = false;
				for (FieldColumn col : columns) {
					String colName = col.getColumnName().toUpperCase();
					if (colName.equals(dimName) ) {
						found = true;
						colNameList.add(colName);
						colNameSet.add(colName);
						break;
					}
				}
				if (!found) {
					throw new InvalidModelException("The aggregating dimension " + dimName +
							" is not a valid dimension.");
				}
			}
		}

		assert colNameList.size() <= dimensionList.size();

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
			Object sum = row.get(COL_RESULTS);
			if (sum == null) {
				summary.setAggregatedValue(0L);
			} else if (sum instanceof BigDecimal) {
				summary.setAggregatedValue(((BigDecimal)sum).longValue());
			} else if (sum instanceof Long) {
				summary.setAggregatedValue(((Long)sum).longValue());
			} else {
				throw new DatastoreException("Unknown type of 'sum': " + sum.getClass().getName());
			}
			List<StorageUsageDimensionValue> dValList = new ArrayList<StorageUsageDimensionValue>();
			for (String column : columnList) {
				String value = row.get(column).toString();
				StorageUsageDimensionValue val = new StorageUsageDimensionValue();
				val.setDimension(StorageUsageDimension.valueOf(column));
				val.setValue(value);
				dValList.add(val);
			}
			summary.setDimensionList(dValList);
			summaryList.add(summary);
		}

		assert summaryList.size() == rowList.size();
	}
}
