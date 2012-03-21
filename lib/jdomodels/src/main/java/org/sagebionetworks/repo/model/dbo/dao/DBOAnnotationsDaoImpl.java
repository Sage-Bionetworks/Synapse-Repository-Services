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
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Transactional(readOnly = true)
public class DBOAnnotationsDaoImpl implements DBOAnnotationsDao {
	
	private static String[] ANNOTATION_TABLES = new String[]{
		TABLE_STRING_ANNOTATIONS,
		TABLE_LONG_ANNOTATIONS,
		TABLE_DOUBLE_ANNOTATIONS,
		TABLE_DATE_ANNOTATIONS,
	};
	
	private static String DELETE_SQL_FORMAT = "DELETE FROM `%1$s` WHERE "+ANNOTATION_OWNER_ID_COLUMN+" = ?";
	// Contains all of the delete sql
	private static String[] ALL_DELETE_SQL = new String[ANNOTATION_TABLES.length];
	static{
		for(int i=0; i<ANNOTATION_TABLES.length; i++){
			ALL_DELETE_SQL[i] = String.format(DELETE_SQL_FORMAT, ANNOTATION_TABLES[i]);
		}
	}
	
	private static String SELECT_FORMAT = "SELECT "+ANNOTATION_ATTRIBUTE_COLUMN+", "+ANNOTATION_VALUE_COLUMN+" FROM `%1$s` WHERE "+ANNOTATION_OWNER_ID_COLUMN+" = ?";
	
	/**
	 * Get the string annotations.
	 */
	private static String SELECT_STRING_ANNOS = String.format(SELECT_FORMAT, TABLE_STRING_ANNOTATIONS);
	private static String SELECT_LONG_ANNOS = String.format(SELECT_FORMAT, TABLE_LONG_ANNOTATIONS);
	private static String SELECT_DOUBLE_ANNOS = String.format(SELECT_FORMAT, TABLE_DOUBLE_ANNOTATIONS);
	private static String SELECT_DATE_ANNOS = String.format(SELECT_FORMAT, TABLE_DATE_ANNOTATIONS);
	
	@Autowired
	private SimpleJdbcTemplate simpleJdbcTempalte;

	@Autowired
	DBOBasicDao dboBasicDao;
	
	@Transactional(readOnly = true)
	@Override
	public Annotations getAnnotations(Long owner) {
		final Annotations results = new Annotations();
		// First select the string annotations
		simpleJdbcTempalte.query(SELECT_STRING_ANNOS, new RowMapper<String>() {
			@Override
			public String mapRow(ResultSet rs, int rowNum) throws SQLException {
				String key = rs.getString(ANNOTATION_ATTRIBUTE_COLUMN);
				String value = rs.getString(ANNOTATION_VALUE_COLUMN);
				results.addAnnotation(key, value);
				return value;
			}
		}, owner);
		// Get the longs
		simpleJdbcTempalte.query(SELECT_LONG_ANNOS, new RowMapper<String>() {
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
		simpleJdbcTempalte.query(SELECT_DOUBLE_ANNOS, new RowMapper<String>() {
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
		simpleJdbcTempalte.query(SELECT_DATE_ANNOS, new RowMapper<String>() {
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
		for(String deleteSql: ALL_DELETE_SQL){
			simpleJdbcTempalte.update(deleteSql, ownerId);
		}
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

}
