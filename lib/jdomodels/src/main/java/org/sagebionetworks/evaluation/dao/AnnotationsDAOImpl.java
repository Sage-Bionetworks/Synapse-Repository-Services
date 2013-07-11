package org.sagebionetworks.evaluation.dao;

import static org.sagebionetworks.repo.model.query.SQLConstants.*;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sagebionetworks.evaluation.dbo.DBOConstants;
import org.sagebionetworks.evaluation.dbo.AnnotationsBlobDBO;
import org.sagebionetworks.evaluation.dbo.AnnotationsOwnerDBO;
import org.sagebionetworks.evaluation.dbo.DoubleAnnotationDBO;
import org.sagebionetworks.evaluation.dbo.LongAnnotationDBO;
import org.sagebionetworks.evaluation.dbo.StringAnnotationDBO;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.annotation.Annotations;
import org.sagebionetworks.repo.model.annotation.DoubleAnnotation;
import org.sagebionetworks.repo.model.annotation.LongAnnotation;
import org.sagebionetworks.repo.model.annotation.StringAnnotation;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.dao.AnnotationUtils;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.JSONObjectAdapterImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public class AnnotationsDAOImpl implements AnnotationsDAO {
	
	private static final String FROM_ANNO_OWNER =
			" FROM " + TABLE_SUBSTATUS_ANNO_OWNER + " WHERE " + COL_SUBSTATUS_ANNO_SUBID + " = ?";
	
	private static final String DELETE_FROM_ANNO_OWNER = "DELETE" + FROM_ANNO_OWNER;
	
	private static final String SELECT_FROM_ANNO_OWNER = "SELECT *" + FROM_ANNO_OWNER;
	
	private static final String SELECT_FORMAT =
			"SELECT " + COL_SUBSTATUS_ANNO_ATTRIBUTE + ", " + COL_SUBSTATUS_ANNO_VALUE + ", " +
			COL_SUBSTATUS_ANNO_IS_PRIVATE + " FROM `%1$s` WHERE " + COL_SUBSTATUS_ANNO_SUBID+" = ?";
	
	private static final String SELECT_STRING_ANNOS = String.format(SELECT_FORMAT, TABLE_SUBSTATUS_STRINGANNO);
	private static final String SELECT_LONG_ANNOS = String.format(SELECT_FORMAT, TABLE_SUBSTATUS_LONGANNO);
	private static final String SELECT_DOUBLE_ANNOS = String.format(SELECT_FORMAT, TABLE_SUBSTATUS_DOUBLEANNO);
	
	private static final String SELECT_ANNO_BLOB = "SELECT " + COL_SUBSTATUS_ANNO_BLOB + " FROM " +
			TABLE_SUBSTATUS_ANNO_BLOB + " WHERE " + COL_SUBSTATUS_ANNO_SUBID + " = ?";
	
	@Autowired
	private SimpleJdbcTemplate simpleJdbcTemplate;
	@Autowired
	private DBOBasicDao dboBasicDao;
	
	@Override
	public Annotations getAnnotations(Long owner) {
		final Annotations results = new Annotations();
		results.setDoubleAnnos(new ArrayList<DoubleAnnotation>());
		results.setLongAnnos(new ArrayList<LongAnnotation>());
		results.setStringAnnos(new ArrayList<StringAnnotation>());
		
		// Get the Owner
		simpleJdbcTemplate.query(SELECT_FROM_ANNO_OWNER, new RowMapper<String>() {
			@Override
			public String mapRow(ResultSet rs, int rowNum) throws SQLException {
				String subId = rs.getString(COL_SUBSTATUS_ANNO_SUBID);
				String evalId = rs.getString(COL_SUBSTATUS_ANNO_EVALID);
				results.setOwnerId(subId);
				results.setOwnerParentId(evalId);
				return subId;
			}
		}, owner);
		
		// Get the Strings
		simpleJdbcTemplate.query(SELECT_STRING_ANNOS, new RowMapper<String>() {
			@Override
			public String mapRow(ResultSet rs, int rowNum) throws SQLException {
				StringAnnotation sa = new StringAnnotation();				
				sa.setKey(rs.getString(COL_SUBSTATUS_ANNO_ATTRIBUTE));
				sa.setValue(rs.getString(COL_SUBSTATUS_ANNO_VALUE));
				sa.setIsPrivate(rs.getBoolean(COL_SUBSTATUS_ANNO_IS_PRIVATE));
				results.getStringAnnos().add(sa);
				return sa.getKey();
			}
		}, owner);
		// Get the longs
		simpleJdbcTemplate.query(SELECT_LONG_ANNOS, new RowMapper<String>() {
			@Override
			public String mapRow(ResultSet rs, int rowNum) throws SQLException {
				Long value = rs.getLong(COL_SUBSTATUS_ANNO_VALUE);				
				if (rs.wasNull()) {
					value = null;
				}
				LongAnnotation la = new LongAnnotation();
				la.setKey(rs.getString(COL_SUBSTATUS_ANNO_ATTRIBUTE));
				la.setValue(value);
				la.setIsPrivate(rs.getBoolean(COL_SUBSTATUS_ANNO_IS_PRIVATE));
				results.getLongAnnos().add(la);
				return la.getKey();
			}
		}, owner);
		// Get the doubles
		simpleJdbcTemplate.query(SELECT_DOUBLE_ANNOS, new RowMapper<String>() {
			@Override
			public String mapRow(ResultSet rs, int rowNum) throws SQLException {
				Double value = rs.getDouble(COL_SUBSTATUS_ANNO_VALUE);
				if (rs.wasNull()) {
					value = null;
				}
				DoubleAnnotation da = new DoubleAnnotation();
				da.setKey(rs.getString(COL_SUBSTATUS_ANNO_ATTRIBUTE));
				da.setValue(value);
				da.setIsPrivate(rs.getBoolean(COL_SUBSTATUS_ANNO_IS_PRIVATE));
				results.getDoubleAnnos().add(da);
				return da.getKey();
			}
		}, owner);
		
		return results;
	}
	
	@Override
	public Annotations getAnnotationsFromBlob(Long owner) {
		final Annotations annos = new Annotations();
		simpleJdbcTemplate.query(SELECT_ANNO_BLOB, new RowMapper<String>() {
			@Override
			public String mapRow(ResultSet rs, int rowNum) throws SQLException {
				String json = null;
				java.sql.Blob blob = rs.getBlob(ANNO_BLOB);
				if (blob != null){
					json = new String(blob.getBytes(1, (int) blob.length()));
				}
				try {
					annos.initializeFromJSONObject(new JSONObjectAdapterImpl(json));
				} catch (JSONObjectAdapterException e) {
					// TODO Auto-generated catch block
					return null;
				}
				return json;
			}
		}, owner);
		
		return annos;
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void replaceAnnotations(Annotations annotations) throws DatastoreException, JSONObjectAdapterException {
		if(annotations == null) throw new IllegalArgumentException("Annotations cannot be null");
		if(annotations.getOwnerId() == null) throw new IllegalArgumentException("Annotations owner id cannot be null");
		Long ownerId = KeyFactory.stringToKey(annotations.getOwnerId());
		Long ownerParentId = 0L;
		if (annotations.getOwnerParentId() != null) {
			ownerParentId = KeyFactory.stringToKey(annotations.getOwnerParentId());
		}

		// Prepare all DBOs. We use Maps since we will be inserting system-defined Annotations
		// describing the owner Submission.
		Map<String, LongAnnotationDBO> longAnnoMap =
				new HashMap<String, LongAnnotationDBO>();
		Map<String, DoubleAnnotationDBO> doubleAnnoMap =
				new HashMap<String, DoubleAnnotationDBO>();
		Map<String, StringAnnotationDBO> stringAnnoMap =
				new HashMap<String, StringAnnotationDBO>();
		
		// NOTE: every Annotation, regardless of type, is stored in the StringAnno table. This
		// is necessary to support queries on Annotations of unknown type.
		
		// Prepare the LongAnno DBOs
		List<LongAnnotation> longAnnos = annotations.getLongAnnos();
		for (LongAnnotation la : longAnnos) {
			insertLongAnnoDBO(ownerId, ownerParentId, longAnnoMap, stringAnnoMap, la);
		}
		
		// Prepare the DoubleAnno DBOs
		List<DoubleAnnotation> doubleAnnos = annotations.getDoubleAnnos();
		for (DoubleAnnotation da : doubleAnnos) {
			insertDoubleAnnoDBO(ownerId, ownerParentId, doubleAnnoMap, stringAnnoMap, da);
		}
		
		// Prepare the StringAnno DBOs
		List<StringAnnotation> stringAnnos = annotations.getStringAnnos();
		for (StringAnnotation sa : stringAnnos) {
			insertStringAnnoDBO(ownerId, ownerParentId, stringAnnoMap, sa);
		}
		
		// Insert system-defined Annotations
		LongAnnotation ownerIdAnno = new LongAnnotation();
		ownerIdAnno.setIsPrivate(false);
		ownerIdAnno.setKey(DBOConstants.PARAM_ANNOTATION_OWNER_ID);
		ownerIdAnno.setValue(ownerId);
		insertLongAnnoDBO(ownerId, ownerParentId, longAnnoMap, stringAnnoMap, ownerIdAnno);
		
		LongAnnotation ownerParentIdAnno = new LongAnnotation();
		ownerParentIdAnno.setIsPrivate(false);
		ownerParentIdAnno.setKey(DBOConstants.PARAM_ANNOTATION_OWNER_PARENT_ID);
		ownerParentIdAnno.setValue(ownerParentId);
		insertLongAnnoDBO(ownerId, ownerParentId, longAnnoMap, stringAnnoMap, ownerParentIdAnno);
		
		// Persist the DBOs		
		// Delete existing annos for this object
		deleteAnnotationsByOwnerId(ownerId);
		
		// Create an owner for this object
		AnnotationsOwnerDBO ownerDBO = new AnnotationsOwnerDBO();
		ownerDBO.setSubmissionId(ownerId);
		ownerDBO.setEvaluationId(ownerParentId);
		dboBasicDao.createNew(ownerDBO);
		
		// Create the serialized blob
		AnnotationsBlobDBO ssAnnoBlobDBO = new AnnotationsBlobDBO();
		ssAnnoBlobDBO.setSubmissionId(ownerId);
		JSONObjectAdapter joa = new JSONObjectAdapterImpl();
		annotations.writeToJSONObject(joa);
		ssAnnoBlobDBO.setAnnoBlob(joa.toJSONString().getBytes());
		dboBasicDao.createNew(ssAnnoBlobDBO);
		
		// Create the typed annos
		if (!longAnnoMap.isEmpty()) {
			dboBasicDao.createBatch(
					new ArrayList<LongAnnotationDBO>(longAnnoMap.values()));
		}
		if (!doubleAnnoMap.isEmpty()) {
			dboBasicDao.createBatch(
					new ArrayList<DoubleAnnotationDBO>(doubleAnnoMap.values()));			
		}
		if (!stringAnnoMap.isEmpty()) {
			dboBasicDao.createBatch(
					new ArrayList<StringAnnotationDBO>(stringAnnoMap.values()));
		}
		
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void deleteAnnotationsByOwnerId(Long ownerId) {
		if (ownerId == null) throw new IllegalArgumentException("Owner id cannot be null");
		// Delete the annotation's owner which will trigger the cascade delete of all annotations.
		simpleJdbcTemplate.update(DELETE_FROM_ANNO_OWNER, ownerId);
	}

	private void insertStringAnnoDBO(Long ownerId, Long ownerParentId,
			Map<String, StringAnnotationDBO> stringAnnoMap,
			StringAnnotation sa) {
		String key = sa.getKey();
		
		// add as a String DBO
		StringAnnotationDBO stringDBO =
				AnnotationUtils.createStringAnnotationDBO(ownerId, ownerParentId, sa);
		stringAnnoMap.put(key, stringDBO);
	}

	private void insertDoubleAnnoDBO(Long ownerId, Long ownerParentId,
			Map<String, DoubleAnnotationDBO> doubleAnnoMap,
			Map<String, StringAnnotationDBO> stringAnnoMap,
			DoubleAnnotation da) {
		String key = da.getKey();
		
		// first add as a Double DBO
		DoubleAnnotationDBO longDbo = 
				AnnotationUtils.createDoubleAnnotationDBO(ownerId, ownerParentId, da);
		doubleAnnoMap.put(key, longDbo);
		
		// then add as a String DBO
		StringAnnotationDBO stringDBO =
				AnnotationUtils.createStringAnnotationDBO(ownerId, ownerParentId, da);
		stringAnnoMap.put(key, stringDBO);
	}

	private void insertLongAnnoDBO(Long ownerId, Long ownerParentId,
			Map<String, LongAnnotationDBO> longAnnoMap,
			Map<String, StringAnnotationDBO> stringAnnoMap,
			LongAnnotation la) {
		String key = la.getKey();
		
		// first add as a Long DBO
		LongAnnotationDBO longDbo = 
				AnnotationUtils.createLongAnnotationDBO(ownerId, ownerParentId, la);
		longAnnoMap.put(key, longDbo);
		
		// then add as a String DBO
		StringAnnotationDBO stringDBO =
				AnnotationUtils.createStringAnnotationDBO(ownerId, ownerParentId, la);
		stringAnnoMap.put(key, stringDBO);
	}

}
