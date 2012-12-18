package org.sagebionetworks.repo.model.dbo.dao;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.ANNOTATION_ATTRIBUTE_COLUMN;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.ANNOTATION_OWNER_ID_COLUMN;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.ANNOTATION_VALUE_COLUMN;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_DATE_ANNOTATIONS;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_DOUBLE_ANNOTATIONS;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_LONG_ANNOTATIONS;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_STRING_ANNOTATIONS;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.persistence.DBODateAnnotation;
import org.sagebionetworks.repo.model.dbo.persistence.DBODoubleAnnotation;
import org.sagebionetworks.repo.model.dbo.persistence.DBOLongAnnotation;
import org.sagebionetworks.repo.model.dbo.persistence.DBOStringAnnotation;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public class DBOAnnotationsDaoImpl implements DBOAnnotationsDao {
	
	private static String[] ANNOTATION_TABLES = new String[]{
		TABLE_STRING_ANNOTATIONS,
		TABLE_LONG_ANNOTATIONS,
		TABLE_DOUBLE_ANNOTATIONS,
		TABLE_DATE_ANNOTATIONS,
	};
	
	private static String SELECT_FORMAT = "SELECT "+ANNOTATION_ATTRIBUTE_COLUMN+", "+ANNOTATION_VALUE_COLUMN+" FROM `%1$s` WHERE "+ANNOTATION_OWNER_ID_COLUMN+" = ?";
	
	/**
	 * Get the string annotations.
	 */
	private static String SELECT_STRING_ANNOS = String.format(SELECT_FORMAT, TABLE_STRING_ANNOTATIONS);
	private static String SELECT_LONG_ANNOS = String.format(SELECT_FORMAT, TABLE_LONG_ANNOTATIONS);
	private static String SELECT_DOUBLE_ANNOS = String.format(SELECT_FORMAT, TABLE_DOUBLE_ANNOTATIONS);
	private static String SELECT_DATE_ANNOS = String.format(SELECT_FORMAT, TABLE_DATE_ANNOTATIONS);
	
	@Autowired
	private SimpleJdbcTemplate simpleJdbcTemplate;
	@Autowired
	DBOBasicDao dboBasicDao;
	
	@Override
	public Annotations getAnnotations(Long owner) {
		final Annotations results = new Annotations();
		// First select the string annotations
		simpleJdbcTemplate.query(SELECT_STRING_ANNOS, new RowMapper<String>() {
			@Override
			public String mapRow(ResultSet rs, int rowNum) throws SQLException {
				String key = rs.getString(ANNOTATION_ATTRIBUTE_COLUMN);
				String value = rs.getString(ANNOTATION_VALUE_COLUMN);
				results.addAnnotation(key, value);
				return value;
			}
		}, owner);
		// Get the longs
		simpleJdbcTemplate.query(SELECT_LONG_ANNOS, new RowMapper<String>() {
			@Override
			public String mapRow(ResultSet rs, int rowNum) throws SQLException {
				String key = rs.getString(ANNOTATION_ATTRIBUTE_COLUMN);
				Long value = rs.getLong(ANNOTATION_VALUE_COLUMN);
				if(rs.wasNull()){
					value = null;
				}
				results.addAnnotation(key, value);
				return key;
			}
		}, owner);
		// Get the doubles
		simpleJdbcTemplate.query(SELECT_DOUBLE_ANNOS, new RowMapper<String>() {
			@Override
			public String mapRow(ResultSet rs, int rowNum) throws SQLException {
				String key = rs.getString(ANNOTATION_ATTRIBUTE_COLUMN);
				Double value = rs.getDouble(ANNOTATION_VALUE_COLUMN);
				if(rs.wasNull()){
					value = null;
				}
				results.addAnnotation(key, value);
				return key;
			}
		}, owner);
		// Get the dates
		simpleJdbcTemplate.query(SELECT_DATE_ANNOS, new RowMapper<String>() {
			@Override
			public String mapRow(ResultSet rs, int rowNum) throws SQLException {
				String key = rs.getString(ANNOTATION_ATTRIBUTE_COLUMN);
				Date value = rs.getTimestamp(ANNOTATION_VALUE_COLUMN);
				results.addAnnotation(key, value);
				return key;
			}
		}, owner);
		
		return results;
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void replaceAnnotations(Annotations annotations) throws DatastoreException {
		if(annotations == null) throw new IllegalArgumentException("Annotations cannot be null");
		if(annotations.getId() == null) throw new IllegalArgumentException("Annotations owner id cannot be null");
		Long ownerId = KeyFactory.stringToKey(annotations.getId());

		// First we must delete all annotations for this node.
		deleteAnnotationsByOwnerId(ownerId);

		// Create the string.
		Map<String, List<String>> stringAnnos = annotations.getStringAnnotations();
		if(stringAnnos != null && stringAnnos.size() > 0){
			List<DBOStringAnnotation> stringBatch = AnnotationUtils.createStringAnnotations(ownerId, stringAnnos);
			dboBasicDao.createBatch(stringBatch);
		}
		
		// Create the long.
		Map<String, List<Long>> longAnnos = annotations.getLongAnnotations();
		if(longAnnos != null && longAnnos.size() > 0){
			List<DBOLongAnnotation> batch = AnnotationUtils.createLongAnnotations(ownerId, longAnnos);
			dboBasicDao.createBatch(batch);
		}
		// Create the double
		Map<String, List<Double>> doubleAnnos = annotations.getDoubleAnnotations();
		if(doubleAnnos != null && doubleAnnos.size() > 0){
			List<DBODoubleAnnotation> batch = AnnotationUtils.createDoubleAnnotations(ownerId, doubleAnnos);
			dboBasicDao.createBatch(batch);
		}
		// Create the dates
		Map<String, List<Date>> dateAnnos = annotations.getDateAnnotations();
		if(dateAnnos != null && dateAnnos.size() > 0){
			List<DBODateAnnotation> batch = AnnotationUtils.createDateAnnotations(ownerId, dateAnnos);
			dboBasicDao.createBatch(batch);
		}
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void deleteAnnotationsByOwnerId(Long ownerId) {
		if (ownerId == null) throw new IllegalArgumentException("Owner id cannot be null");
		for(String tableName: ANNOTATION_TABLES){
			deleteWithoutGapLockFromTable(tableName, ownerId);
		}
	}

	/**
	 * In order to avoid MySQL gap locks which cause deadlock, we need to delete by a unique key.
	 * This means we need to first for row IDs that match the owner.  We then use the ids to
	 * delete the rows.  
	 * @param tableName
	 * @param ownerId
	 */
	private void deleteWithoutGapLockFromTable(String tableName, Long ownerId){
		// First get all IDs for rows that belong to the passed owner.
		List<Long> idsToDelete = simpleJdbcTemplate.query("SELECT ID FROM "+tableName+" WHERE "+ANNOTATION_OWNER_ID_COLUMN+" = ? ORDER BY ID ASC", new RowMapper<Long>(){
			@Override
			public Long mapRow(ResultSet rs, int rowNum) throws SQLException {
				return rs.getLong("ID");
			}}, ownerId);
		// Prepare to batch delete the rows by their primary key.
		MapSqlParameterSource[] params = new MapSqlParameterSource[idsToDelete.size()];
		for(int i=0; i<idsToDelete.size(); i++){
			params[i] = new MapSqlParameterSource("idParam", idsToDelete.get(i));
		}
		simpleJdbcTemplate.batchUpdate("DELETE FROM "+tableName+" WHERE ID = :idParam", params);
	}

}
