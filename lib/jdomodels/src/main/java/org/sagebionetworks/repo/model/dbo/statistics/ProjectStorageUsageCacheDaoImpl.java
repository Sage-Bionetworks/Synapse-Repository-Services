package org.sagebionetworks.repo.model.dbo.statistics;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_PROJECT_STORAGE_USAGE_LOCATION_DATA;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_PROJECT_STORAGE_USAGE_PROJECT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_PROJECT_STORAGE_USAGE_UPDATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_PROJECT_STORAGE_USAGE;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import org.json.JSONObject;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ProjectStorageUsageCacheDaoImpl implements ProjectStorageUsageCacheDao {

	private JdbcTemplate jdbcTemplate;
	
	public ProjectStorageUsageCacheDaoImpl(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}
	
	@Override
	public boolean isUpdatedOnAfter(long projectId, Instant instant) {
	
		String sql = "SELECT IFNULL(MAX(" + COL_PROJECT_STORAGE_USAGE_UPDATED_ON + ") > ?, FALSE) FROM " + TABLE_PROJECT_STORAGE_USAGE
			+ " WHERE " + COL_PROJECT_STORAGE_USAGE_PROJECT_ID + "=?";
		
		return jdbcTemplate.queryForObject(sql, Boolean.class, Timestamp.from(instant), projectId);
	}

	@Override
	@WriteTransaction
	public void setStorageUsageMap(long projectId, Map<String, Long> storageLocationSize) {
		String jsonData = new JSONObject(storageLocationSize).toString();
		
		String sql = "INSERT INTO " + TABLE_PROJECT_STORAGE_USAGE + "(" 
				+ COL_PROJECT_STORAGE_USAGE_PROJECT_ID + ","
				+ COL_PROJECT_STORAGE_USAGE_UPDATED_ON + ","
				+ COL_PROJECT_STORAGE_USAGE_LOCATION_DATA +") "
				+ "VALUES(?, ?, ?) as data(P_ID, U_ON, L_D) ON DUPLICATE KEY UPDATE " 
				+ COL_PROJECT_STORAGE_USAGE_UPDATED_ON + "=data.U_ON,"
				+ COL_PROJECT_STORAGE_USAGE_LOCATION_DATA + "=data.L_D";
		
		Timestamp now = Timestamp.from(Instant.now());
		
		storageLocationSize.forEach((storageLocationId, size) -> 
			jdbcTemplate.update(sql, projectId, now, jsonData)
		);
		
	}

	@Override
	public Map<String, Long> getStorageUsageMap(long projectId) {
		String sql = "SELECT " + COL_PROJECT_STORAGE_USAGE_LOCATION_DATA 
			+ " FROM " + TABLE_PROJECT_STORAGE_USAGE
			+ " WHERE " + COL_PROJECT_STORAGE_USAGE_PROJECT_ID + "=?";
		
		Map<String, Long> storageLocationMap = new HashMap<>();
		
		jdbcTemplate.query(sql, rs -> {
			JSONObject jsonData = new JSONObject(rs.getString(COL_PROJECT_STORAGE_USAGE_LOCATION_DATA));
			
			jsonData.keySet().stream().forEach( storageLocationId -> {
				storageLocationMap.put(storageLocationId, jsonData.getLong(storageLocationId));
			});
			
		}, projectId);
		
		return storageLocationMap;
	}

	// For testing
	void truncateAll() {
		jdbcTemplate.update("TRUNCATE TABLE " + TABLE_PROJECT_STORAGE_USAGE);
	}

}
