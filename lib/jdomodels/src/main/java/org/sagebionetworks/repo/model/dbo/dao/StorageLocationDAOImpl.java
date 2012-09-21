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
import java.util.List;
import java.util.Map;

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

public class StorageLocationDAOImpl implements StorageLocationDAO {

	private static final String SELECT_ID_FOR_NODE =
			"SELECT " + COL_STORAGE_LOCATION_ID +
			" FROM " + TABLE_STORAGE_LOCATION +
			" WHERE " + COL_STORAGE_LOCATION_NODE_ID + " = :" + COL_STORAGE_LOCATION_NODE_ID;

	private static final RowMapper<Long> idRowMapper = ParameterizedSingleColumnRowMapper.newInstance(Long.class);

	private static final String SELECT_TOTAL_USAGE_FOR_USER =
			"SELECT SUM(" + COL_STORAGE_LOCATION_CONTENT_SIZE + ")" +
			" FROM " + TABLE_STORAGE_LOCATION +
			" WHERE " + COL_STORAGE_LOCATION_USER_ID + " = :" + COL_STORAGE_LOCATION_USER_ID;

	private static final String COL_TOTAL = "TOTAL";
	private static final String SELECT_AGGREGATED_USAGE_FOR_USER_PART_1 =
			"SELECT SUM(" + COL_STORAGE_LOCATION_CONTENT_SIZE + ") AS " + COL_TOTAL;
	private static final String SELECT_AGGREGATED_USAGE_FOR_USER_PART_2 =
			" FROM " + TABLE_STORAGE_LOCATION +
			" WHERE " + COL_STORAGE_LOCATION_USER_ID + " = :" + COL_STORAGE_LOCATION_USER_ID +
			" GROUP BY ";

	private static final String SELECT_STORAGE_LOCATION_FOR_USER_PAGINATED =
			"SELECT *" +
			" FROM " + TABLE_STORAGE_LOCATION +
			" WHERE " + COL_STORAGE_LOCATION_USER_ID + " = :" + COL_STORAGE_LOCATION_USER_ID +
			" LIMIT :" + LIMIT_PARAM_NAME + " OFFSET :" + OFFSET_PARAM_NAME;

	private static final String SELECT_STORAGE_LOCATION_COUNT_FOR_USER =
			"SELECT COUNT(" + COL_STORAGE_LOCATION_ID + ")" +
			" FROM " + TABLE_STORAGE_LOCATION +
			" WHERE " + COL_STORAGE_LOCATION_USER_ID + " = :" + COL_STORAGE_LOCATION_USER_ID;

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
			basicDao.createBatch(batch);
		} catch (AmazonClientException e) {
			throw new DatastoreException(e);
		}
	}

	@Transactional(readOnly = true)
	@Override
	public Long getTotalUsage(String userId) throws DatastoreException {

		if (userId == null || userId.isEmpty()) {
			throw new NullPointerException();
		}

		MapSqlParameterSource paramMap = new MapSqlParameterSource();
		Long userIdLong = KeyFactory.stringToKey(userId);
		paramMap.addValue(COL_STORAGE_LOCATION_USER_ID, userIdLong);
		long total = simpleJdbcTemplate.queryForLong(SELECT_TOTAL_USAGE_FOR_USER, paramMap);

		return total;
	}

	@Transactional(readOnly = true)
	@Override
	public StorageUsageSummaryList getAggregatedUsage(String userId,
			List<StorageUsageDimension> dimensionList)
			throws DatastoreException, InvalidModelException {

		if (userId == null || userId.isEmpty()) {
			throw new NullPointerException();
		}

		if (dimensionList == null) {
			throw new NullPointerException();
		}

		StorageUsageSummaryList summaryList = new StorageUsageSummaryList();
		summaryList.setUserId(userId);

		Long grandTotal = getTotalUsage(userId);
		summaryList.setGrandTotal(grandTotal);

		List<StorageUsageSummary> susList = new ArrayList<StorageUsageSummary>();
		summaryList.setSummaryList(susList);

		if (dimensionList.isEmpty()) {
			return summaryList;
		}

		List<String> columnList = getGroupByColumns(dimensionList);
		assert columnList.size() > 0; // Otherwise we should have returned

		StringBuilder sql = new StringBuilder(SELECT_AGGREGATED_USAGE_FOR_USER_PART_1);
		for (String column : columnList) {
			sql.append(", ");
			sql.append(column);
		}
		sql.append(SELECT_AGGREGATED_USAGE_FOR_USER_PART_2);
		sql.append(columnList.get(0));
		int i = 1;
		while (i < columnList.size()) {
			sql.append(", ");
			sql.append(columnList.get(i));
			i++;
		}

		MapSqlParameterSource paramMap = new MapSqlParameterSource();
		Long userIdLong = KeyFactory.stringToKey(userId);
		paramMap.addValue(COL_STORAGE_LOCATION_USER_ID, userIdLong);
		List<Map<String, Object>> rows = simpleJdbcTemplate.queryForList(sql.toString(), paramMap);
		for (Map<String, Object> row : rows) {
			StorageUsageSummary sus = new StorageUsageSummary();
			sus.setUserId(userId);
			Object sum = row.get(COL_TOTAL);
			Long total = (sum == null ? 0L : ((BigDecimal)sum).longValue());
			sus.setTotalSize(total);
			List<StorageUsageDimensionValue> dValList = new ArrayList<StorageUsageDimensionValue>();
			for (String column : columnList) {
				String value = row.get(column).toString();
				StorageUsageDimensionValue val = new StorageUsageDimensionValue();
				val.setDimension(StorageUsageDimension.valueOf(column));
				val.setValue(value);
				dValList.add(val);
			}
			sus.setDimensionList(dValList);
			susList.add(sus);
		}

		return summaryList;
	}

	@Transactional(readOnly = true)
	@Override
	public List<StorageUsage> getStorageUsageInRange(String userId, long beginIncl, long endExcl)
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

	@Transactional(readOnly = true)
	@Override
	public Long getCount(String userId) throws DatastoreException {

		if (userId == null || userId.isEmpty()) {
			throw new NullPointerException();
		}

		MapSqlParameterSource paramMap = new MapSqlParameterSource();
		Long userIdLong = KeyFactory.stringToKey(userId);
		paramMap.addValue(COL_STORAGE_LOCATION_USER_ID, userIdLong);
		Long count = simpleJdbcTemplate.queryForLong(SELECT_STORAGE_LOCATION_COUNT_FOR_USER, paramMap);
		return count;
	}

	private List<String> getGroupByColumns(List<StorageUsageDimension> dimensionList) {

		List<String> colNames = new ArrayList<String>();
		FieldColumn[] columns = (new DBOStorageLocation()).getTableMapping().getFieldColumns();
		// Both lists should be small thus we can afford a N^2 lookup.
		for (FieldColumn col : columns) {
			String colName = col.getColumnName().toUpperCase();
			for (StorageUsageDimension d : dimensionList) {
				if (colName.equals(d.name().toUpperCase())) {
					colNames.add(colName);
					break;
				}
			}
		}

		if (colNames.size() != dimensionList.size()) {
			throw new InvalidModelException("The list of StorageUsageDimension has invalid column names.");
		}

		return colNames;
	}
}
