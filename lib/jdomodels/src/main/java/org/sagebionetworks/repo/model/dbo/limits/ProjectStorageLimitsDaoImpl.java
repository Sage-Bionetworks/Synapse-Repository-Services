package org.sagebionetworks.repo.model.dbo.limits;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_PROJECT_STORAGE_DATA_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_PROJECT_STORAGE_DATA_LOCATION_DATA;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_PROJECT_STORAGE_DATA_MODIFIED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_PROJECT_STORAGE_DATA_PROJECT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_PROJECT_STORAGE_DATA_RUNTIME_MS;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_PROJECT_STORAGE_LIMIT_CREATED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_PROJECT_STORAGE_LIMIT_CREATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_PROJECT_STORAGE_LIMIT_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_PROJECT_STORAGE_LIMIT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_PROJECT_STORAGE_LIMIT_LOCATION_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_PROJECT_STORAGE_LIMIT_MAX_BYTES;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_PROJECT_STORAGE_LIMIT_MODIFIED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_PROJECT_STORAGE_LIMIT_MODIFIED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_PROJECT_STORAGE_LIMIT_PROJECT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_PROJECT_STORAGE_DATA;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_PROJECT_STORAGE_LIMIT;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.json.JSONObject;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.limits.ProjectStorageData;
import org.sagebionetworks.repo.model.limits.ProjectStorageLocationLimit;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ProjectStorageLimitsDaoImpl implements ProjectStorageLimitsDao {
	
	private static String locationDataToJson(Map<String, Long> storageLocationData) {
		return new JSONObject(storageLocationData).toString();
	}
	
	private Map<String, Long> jsonToLocationData(String jsonData) {
		JSONObject jsonObject = new JSONObject(jsonData);
		return jsonObject.keySet().stream()
			.collect(Collectors.toMap(Function.identity(), jsonObject::getLong));
	}
	
	private JdbcTemplate jdbcTemplate;
	private IdGenerator idGenerator;
	
	public ProjectStorageLimitsDaoImpl(JdbcTemplate jdbcTemplate, IdGenerator idGenerator) {
		this.jdbcTemplate = jdbcTemplate;
		this.idGenerator = idGenerator;
	}
	
	@Override
	public boolean isStorageDataModifiedOnAfter(Long projectId, Instant instant) {
	
		String sql = "SELECT IFNULL(MAX(" + COL_PROJECT_STORAGE_DATA_MODIFIED_ON + ") > ?, FALSE) FROM " + TABLE_PROJECT_STORAGE_DATA
			+ " WHERE " + COL_PROJECT_STORAGE_DATA_PROJECT_ID + "=?";
		
		return jdbcTemplate.queryForObject(sql, Boolean.class, Timestamp.from(instant), projectId);
	}

	@Override
	@WriteTransaction
	public void setStorageData(List<ProjectStorageData> projectStorageData) {
		String sql = "INSERT INTO " + TABLE_PROJECT_STORAGE_DATA + "(" 
			+ COL_PROJECT_STORAGE_DATA_PROJECT_ID + ","
			+ COL_PROJECT_STORAGE_DATA_ETAG + ","
			+ COL_PROJECT_STORAGE_DATA_MODIFIED_ON + ","
			+ COL_PROJECT_STORAGE_DATA_RUNTIME_MS + ","
			+ COL_PROJECT_STORAGE_DATA_LOCATION_DATA +") "
			+ "VALUES(?, UUID(), ?, ?, ?) as data(PROJECT_ID,ETAG,MODIFIED_ON,RUNTIME_MS,LOCATION_DATA) ON DUPLICATE KEY UPDATE "
			+ COL_PROJECT_STORAGE_DATA_ETAG + "=data.ETAG,"
			+ COL_PROJECT_STORAGE_DATA_MODIFIED_ON + "=data.MODIFIED_ON,"
			+ COL_PROJECT_STORAGE_DATA_RUNTIME_MS + "=data.RUNTIME_MS,"
			+ COL_PROJECT_STORAGE_DATA_LOCATION_DATA + "=data.LOCATION_DATA";
		
		Timestamp now = Timestamp.from(Instant.now());
		
		jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
			
			@Override
			public void setValues(PreparedStatement ps, int i) throws SQLException {
				ProjectStorageData projectData = projectStorageData.get(i);
				
				int paramIndex = 0;
				
				ps.setLong(++paramIndex, projectData.getProjectId());
				ps.setTimestamp(++paramIndex, now);
				ps.setLong(++paramIndex, projectData.getRuntimeMs());
				ps.setString(++paramIndex, locationDataToJson(projectData.getStorageLocationData()));
			}
			
			@Override
			public int getBatchSize() {
				return projectStorageData.size();
			}
		});
	}

	@Override
	public Optional<ProjectStorageData> getStorageData(Long projectId) {
		String sql = "SELECT " 
			+ COL_PROJECT_STORAGE_DATA_PROJECT_ID + ","
			+ COL_PROJECT_STORAGE_DATA_ETAG + ","
			+ COL_PROJECT_STORAGE_DATA_MODIFIED_ON + ","
			+ COL_PROJECT_STORAGE_DATA_RUNTIME_MS + ","
			+ COL_PROJECT_STORAGE_DATA_LOCATION_DATA 
			+ " FROM " + TABLE_PROJECT_STORAGE_DATA
			+ " WHERE " + COL_PROJECT_STORAGE_DATA_PROJECT_ID + "=?";
		
		return jdbcTemplate.query(sql, rs -> {
			if (rs.next()) {
				return Optional.of(new ProjectStorageData()
					.setProjectId(rs.getLong(COL_PROJECT_STORAGE_DATA_PROJECT_ID))
					.setEtag(rs.getString(COL_PROJECT_STORAGE_DATA_ETAG))
					.setModifiedOn(new Date(rs.getTimestamp(COL_PROJECT_STORAGE_DATA_MODIFIED_ON).getTime()))
					.setRuntimeMs(rs.getLong(COL_PROJECT_STORAGE_DATA_RUNTIME_MS))
					.setStorageLocationData(jsonToLocationData(rs.getString(COL_PROJECT_STORAGE_DATA_LOCATION_DATA)))
				);
			}
			return Optional.empty();
		}, projectId);
	}

	@Override
	@WriteTransaction
	public ProjectStorageLocationLimit setStorageLocationLimit(long userId, ProjectStorageLocationLimit limit) {
		String sql = "INSERT INTO " + TABLE_PROJECT_STORAGE_LIMIT + "("
			+ COL_PROJECT_STORAGE_LIMIT_ID + ","
			+ COL_PROJECT_STORAGE_LIMIT_ETAG + ","
			+ COL_PROJECT_STORAGE_LIMIT_CREATED_BY + ","
			+ COL_PROJECT_STORAGE_LIMIT_CREATED_ON + ","
			+ COL_PROJECT_STORAGE_LIMIT_MODIFIED_BY + ","
			+ COL_PROJECT_STORAGE_LIMIT_MODIFIED_ON + ","
			+ COL_PROJECT_STORAGE_LIMIT_PROJECT_ID + ","
			+ COL_PROJECT_STORAGE_LIMIT_LOCATION_ID + ","
			+ COL_PROJECT_STORAGE_LIMIT_MAX_BYTES 
			+ ") VALUES (?, UUID(), ?, ?, ?, ?, ?, ?, ?) as data(ID,ETAG,CREATED_BY,CREATED_ON,MODIFIED_BY,MODIFIED_ON,PROJECT_ID,LOCATION_ID,MAX_BYTES) ON DUPLICATE KEY UPDATE "
			+ COL_PROJECT_STORAGE_LIMIT_ETAG + "=data.ETAG,"
			+ COL_PROJECT_STORAGE_LIMIT_MODIFIED_BY + "=data.MODIFIED_BY,"
			+ COL_PROJECT_STORAGE_LIMIT_MODIFIED_ON + "=data.MODIFIED_ON,"
			+ COL_PROJECT_STORAGE_LIMIT_PROJECT_ID + "=data.PROJECT_ID,"
			+ COL_PROJECT_STORAGE_LIMIT_LOCATION_ID + "=data.LOCATION_ID,"
			+ COL_PROJECT_STORAGE_LIMIT_MAX_BYTES + "=data.MAX_BYTES";
		
		Timestamp now = Timestamp.from(Instant.now());
		
		Long newId = idGenerator.generateNewId(IdType.PROJECT_STORAGE_LIMIT_ID);		
		Long projectId = KeyFactory.stringToKey(limit.getProjectId());
		Long storageLocationId = KeyFactory.stringToKey(limit.getStorageLocationId());
		
		jdbcTemplate.update(sql, newId, userId, now, userId, now, projectId, storageLocationId, limit.getMaxAllowedFileBytes());
		
		return getStorageLocationLimit(projectId, storageLocationId).orElseThrow();
	}
	
	@Override
	public Optional<ProjectStorageLocationLimit> getStorageLocationLimit(Long projectId, Long storageLocationId) {
		return getStorageLocationLimits(projectId, storageLocationId).stream().findFirst();
	}

	@Override
	public List<ProjectStorageLocationLimit> getStorageLocationLimits(Long projectId) {
		return getStorageLocationLimits(projectId, null);
	}
	
	private List<ProjectStorageLocationLimit> getStorageLocationLimits(Long projectId, Long storageLocationId) {
		String sql = "SELECT " + COL_PROJECT_STORAGE_LIMIT_LOCATION_ID + "," + COL_PROJECT_STORAGE_LIMIT_MAX_BYTES
			+ " FROM " + TABLE_PROJECT_STORAGE_LIMIT
			+ " WHERE " + COL_PROJECT_STORAGE_LIMIT_PROJECT_ID + "=?";
	
		Object[] args = new Long[storageLocationId == null ? 1 : 2];
		
		args[0] = projectId;
		
		if (storageLocationId != null) {
			sql += " AND " + COL_PROJECT_STORAGE_LIMIT_LOCATION_ID + "=?";
			args[1] = storageLocationId;
		}
		
		sql += " ORDER BY " + COL_PROJECT_STORAGE_LIMIT_LOCATION_ID;
		
		String projectIdKey = KeyFactory.keyToString(projectId);
		
		return jdbcTemplate.query(sql, (rs,  i) -> new ProjectStorageLocationLimit()
			.setProjectId(projectIdKey)
			.setStorageLocationId(String.valueOf(rs.getLong(COL_PROJECT_STORAGE_LIMIT_LOCATION_ID)))
			.setMaxAllowedFileBytes(rs.getLong(COL_PROJECT_STORAGE_LIMIT_MAX_BYTES)), args);
		
	}

	// For testing
	@Override
	public void truncateAll() {
		jdbcTemplate.update("TRUNCATE TABLE " + TABLE_PROJECT_STORAGE_DATA);
		jdbcTemplate.update("TRUNCATE TABLE " + TABLE_PROJECT_STORAGE_LIMIT);
	}

}
