package org.sagebionetworks.repo.model.dbo.dao;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.ANNOTATION_ATTRIBUTE_COLUMN;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.ANNOTATION_OWNER_ID_COLUMN;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.ANNOTATION_VALUE_COLUMN;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ANNOTATION_OWNER;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_ANNOTATIONS_OWNER;
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
import org.sagebionetworks.repo.model.dbo.AnnotationDBOUtils;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.persistence.DBOAnnotationOwner;
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

public class DBOAnnotationsDaoImpl implements DBOAnnotationsDao {
	
	private static final String SQL_DELETE_ANNOTATIONS_OWNER = "DELETE FROM "+TABLE_ANNOTATIONS_OWNER+" WHERE "+COL_ANNOTATION_OWNER+" = ?";
	
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
		// Create an owner for this node.
		DBOAnnotationOwner onwer = new DBOAnnotationOwner();
		onwer.setOwnerId(ownerId);
		// If another thread is attempting to update the same annotations, then this will trigger a primary key constraint violation.
		dboBasicDao.createNew(onwer);

		// Create the string.
		Map<String, List<String>> stringAnnos = annotations.getStringAnnotations();
		if(stringAnnos != null && stringAnnos.size() > 0){
			List<DBOStringAnnotation> stringBatch = AnnotationDBOUtils.createStringAnnotations(ownerId, stringAnnos);
			dboBasicDao.createBatch(stringBatch);
		}
		
		// Create the long.
		Map<String, List<Long>> longAnnos = annotations.getLongAnnotations();
		if(longAnnos != null && longAnnos.size() > 0){
			List<DBOLongAnnotation> batch = AnnotationDBOUtils.createLongAnnotations(ownerId, longAnnos);
			dboBasicDao.createBatch(batch);
		}
		// Create the double
		Map<String, List<Double>> doubleAnnos = annotations.getDoubleAnnotations();
		if(doubleAnnos != null && doubleAnnos.size() > 0){
			List<DBODoubleAnnotation> batch = AnnotationDBOUtils.createDoubleAnnotations(ownerId, doubleAnnos);
			dboBasicDao.createBatch(batch);
		}
		// Create the dates
		Map<String, List<Date>> dateAnnos = annotations.getDateAnnotations();
		if(dateAnnos != null && dateAnnos.size() > 0){
			List<DBODateAnnotation> batch = AnnotationDBOUtils.createDateAnnotations(ownerId, dateAnnos);
			dboBasicDao.createBatch(batch);
		}
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void deleteAnnotationsByOwnerId(Long ownerId) {
		if (ownerId == null) throw new IllegalArgumentException("Owner id cannot be null");
		// Delete the annotation's owner which will trigger the cascade delete of all annotations.
		simpleJdbcTemplate.update(SQL_DELETE_ANNOTATIONS_OWNER, ownerId);
	}

}
