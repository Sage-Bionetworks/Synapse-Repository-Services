package org.sagebionetworks.evaluation.dao;

import static org.sagebionetworks.repo.model.query.SQLConstants.ANNO_BLOB;
import static org.sagebionetworks.repo.model.query.SQLConstants.COL_SUBMISSION_EVAL_ID;
import static org.sagebionetworks.repo.model.query.SQLConstants.COL_SUBMISSION_ID;
import static org.sagebionetworks.repo.model.query.SQLConstants.COL_SUBSTATUS_ANNO_ATTRIBUTE;
import static org.sagebionetworks.repo.model.query.SQLConstants.COL_SUBSTATUS_ANNO_BLOB;
import static org.sagebionetworks.repo.model.query.SQLConstants.COL_SUBSTATUS_ANNO_EVALID;
import static org.sagebionetworks.repo.model.query.SQLConstants.COL_SUBSTATUS_ANNO_IS_PRIVATE;
import static org.sagebionetworks.repo.model.query.SQLConstants.COL_SUBSTATUS_ANNO_SUBID;
import static org.sagebionetworks.repo.model.query.SQLConstants.COL_SUBSTATUS_ANNO_VALUE;
import static org.sagebionetworks.repo.model.query.SQLConstants.COL_SUBSTATUS_ANNO_VERSION;
import static org.sagebionetworks.repo.model.query.SQLConstants.COL_SUBSTATUS_SUBMISSION_ID;
import static org.sagebionetworks.repo.model.query.SQLConstants.COL_SUBSTATUS_VERSION;
import static org.sagebionetworks.repo.model.query.SQLConstants.TABLE_SUBMISSION;
import static org.sagebionetworks.repo.model.query.SQLConstants.TABLE_SUBSTATUS;
import static org.sagebionetworks.repo.model.query.SQLConstants.TABLE_SUBSTATUS_ANNO_BLOB;
import static org.sagebionetworks.repo.model.query.SQLConstants.TABLE_SUBSTATUS_ANNO_OWNER;
import static org.sagebionetworks.repo.model.query.SQLConstants.TABLE_SUBSTATUS_DOUBLEANNO;
import static org.sagebionetworks.repo.model.query.SQLConstants.TABLE_SUBSTATUS_LONGANNO;
import static org.sagebionetworks.repo.model.query.SQLConstants.TABLE_SUBSTATUS_STRINGANNO;

import java.sql.Blob;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.sagebionetworks.evaluation.dbo.AnnotationsBlobDBO;
import org.sagebionetworks.evaluation.dbo.AnnotationsOwnerDBO;
import org.sagebionetworks.evaluation.dbo.DoubleAnnotationDBO;
import org.sagebionetworks.evaluation.dbo.LongAnnotationDBO;
import org.sagebionetworks.evaluation.dbo.StringAnnotationDBO;
import org.sagebionetworks.evaluation.dbo.SubmissionDBO;
import org.sagebionetworks.evaluation.dbo.SubmissionStatusDBO;
import org.sagebionetworks.evaluation.model.Submission;
import org.sagebionetworks.evaluation.model.SubmissionBundle;
import org.sagebionetworks.evaluation.model.SubmissionStatus;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.annotation.Annotations;
import org.sagebionetworks.repo.model.annotation.DoubleAnnotation;
import org.sagebionetworks.repo.model.annotation.LongAnnotation;
import org.sagebionetworks.repo.model.annotation.StringAnnotation;
import org.sagebionetworks.repo.model.dbo.AnnotationDBOUtils;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.evaluation.AnnotationsDAO;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.JSONObjectAdapterImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

public class AnnotationsDAOImpl implements AnnotationsDAO {
	
	private static final String SELECT_FROM_ANNO_OWNER = "SELECT * " +
			" FROM " + TABLE_SUBSTATUS_ANNO_OWNER + " WHERE " + 
			COL_SUBSTATUS_ANNO_SUBID + " = ?";
	
	private static final String DELETE_FROM_ANNO_OWNERS = "DELETE " + 
			" FROM " + TABLE_SUBSTATUS_ANNO_OWNER + " WHERE " + 
			COL_SUBSTATUS_ANNO_SUBID + " IN (:"+COL_SUBSTATUS_ANNO_SUBID+")";
	
	private static final String SELECT_FORMAT =
			"SELECT " + COL_SUBSTATUS_ANNO_ATTRIBUTE + ", " + COL_SUBSTATUS_ANNO_VALUE + ", " +
			COL_SUBSTATUS_ANNO_IS_PRIVATE + " FROM `%1$s` WHERE " + COL_SUBSTATUS_ANNO_SUBID+" = ?";
	
	private static final String SELECT_STRING_ANNOS = String.format(SELECT_FORMAT, TABLE_SUBSTATUS_STRINGANNO);
	private static final String SELECT_LONG_ANNOS = String.format(SELECT_FORMAT, TABLE_SUBSTATUS_LONGANNO);
	private static final String SELECT_DOUBLE_ANNOS = String.format(SELECT_FORMAT, TABLE_SUBSTATUS_DOUBLEANNO);
	
	private static final String SELECT_ANNO_BLOB = "SELECT " + COL_SUBSTATUS_ANNO_BLOB + " FROM " +
			TABLE_SUBSTATUS_ANNO_BLOB + " WHERE " + COL_SUBSTATUS_ANNO_SUBID + " = ?";
	
	//	select s.*, t.* from JDOSUBMISSION s, JDOSUBMISSION_STATUS t 
	//	left outer join SUBSTATUS_ANNOTATIONS_BLOB a on a.SUBMISSION_ID=t.ID
	//	where
	//	s.ID=t.ID and
	//	a.VERISION IS NULL OR a.VERSION<>t.VERSION
	//	and s.EVALUATION_ID=:EVALUATION_ID;
	private static final String SELECT_MISSING_OR_CHANGED_SUBSTATUSES = 
			"SELECT s.*, t.* FROM "+TABLE_SUBMISSION+" s, "+TABLE_SUBSTATUS+" t "+
			" LEFT OUTER JOIN "+TABLE_SUBSTATUS_ANNO_BLOB+" a ON a."+COL_SUBSTATUS_ANNO_SUBID+"=t."+
			COL_SUBSTATUS_SUBMISSION_ID+
			" WHERE s."+COL_SUBMISSION_ID+"=t."+COL_SUBSTATUS_SUBMISSION_ID+" AND (a."+
			COL_SUBSTATUS_ANNO_VERSION+" IS NULL OR a."+
			COL_SUBSTATUS_ANNO_VERSION+"<>t."+COL_SUBSTATUS_VERSION+") "+
			" AND s."+COL_SUBMISSION_EVAL_ID+"=:"+COL_SUBMISSION_EVAL_ID;
	
	private static final String SELECT_IDS_FOR_DELETED_SUBMISSIONS = 
			"SELECT o."+COL_SUBSTATUS_ANNO_SUBID+" FROM "+
			TABLE_SUBSTATUS_ANNO_OWNER+" o left outer join "+
			TABLE_SUBMISSION+" s on o."+COL_SUBSTATUS_ANNO_SUBID+
			"=s."+COL_SUBMISSION_ID+" WHERE o."+COL_SUBSTATUS_ANNO_EVALID+"=:"+
			COL_SUBSTATUS_ANNO_EVALID+" AND s."+COL_SUBMISSION_ID+" is null";
	
	// DELETE FROM SUBSTATUS_ANNOTATIONS_OWNER o WHERE 
	// o.SUBMISSION_ID IN (...)
	private static final String DELETE_ANNOS_FOR_DELETED_SUBMISSIONS = 
			"DELETE FROM "+TABLE_SUBSTATUS_ANNO_OWNER+" WHERE "+
			COL_SUBSTATUS_ANNO_SUBID+" IN (:"+COL_SUBSTATUS_ANNO_SUBID+")";

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private NamedParameterJdbcTemplate namedJdbcTemplate;
	
	@Autowired
	private DBOBasicDao dboBasicDao;
	
	@Override
	public Annotations getAnnotations(Long owner) {
		final Annotations results = new Annotations();
		results.setDoubleAnnos(new ArrayList<DoubleAnnotation>());
		results.setLongAnnos(new ArrayList<LongAnnotation>());
		results.setStringAnnos(new ArrayList<StringAnnotation>());
		
		// Get the Owner
		jdbcTemplate.query(SELECT_FROM_ANNO_OWNER, new RowMapper<String>() {
			@Override
			public String mapRow(ResultSet rs, int rowNum) throws SQLException {
				String subId = rs.getString(COL_SUBSTATUS_ANNO_SUBID);
				String evalId = rs.getString(COL_SUBSTATUS_ANNO_EVALID);
				results.setObjectId(subId);
				results.setScopeId(evalId);
				return subId;
			}
		}, owner);
		
		// Get the Strings
		jdbcTemplate.query(SELECT_STRING_ANNOS, new RowMapper<String>() {
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
		jdbcTemplate.query(SELECT_LONG_ANNOS, new RowMapper<String>() {
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
		jdbcTemplate.query(SELECT_DOUBLE_ANNOS, new RowMapper<String>() {
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
		jdbcTemplate.query(SELECT_ANNO_BLOB, new RowMapper<String>() {
			@Override
			public String mapRow(ResultSet rs, int rowNum) throws SQLException {
				String json = null;
				Blob blob = rs.getBlob(ANNO_BLOB);
				if (blob != null){
					json = new String(blob.getBytes(1, (int) blob.length()));
				}
				try {
					annos.initializeFromJSONObject(new JSONObjectAdapterImpl(json));
				} catch (JSONObjectAdapterException e) {
					throw new SQLException(e);
				}
				return json;
			}
		}, owner);
		
		return annos;
	}

	@Override
	public void replaceAnnotations(List<Annotations> annotationsList)
			throws DatastoreException, JSONObjectAdapterException {
		// Create DBOs
		// Note that a copy of every Annotation is stored on the String table, regardless of type.
		// This is necessary to support queries on Annotations of unknown type.
		List<LongAnnotationDBO> longAnnoDBOs = new ArrayList<LongAnnotationDBO>();
		List<DoubleAnnotationDBO> doubleAnnoDBOs = new ArrayList<DoubleAnnotationDBO>();
		List<StringAnnotationDBO> stringAnnoDBOs = new ArrayList<StringAnnotationDBO>();
		List<AnnotationsBlobDBO> ssAnnoBlobDBOs = new ArrayList<AnnotationsBlobDBO>();
		List<Long> ownerIds = new ArrayList<Long>();
		
		Long scopeId = null;
		for (Annotations annotations : annotationsList) {
			if(annotations == null) throw new IllegalArgumentException("Annotations cannot be null");
			if(annotations.getObjectId() == null) throw new IllegalArgumentException("Annotations owner id cannot be null");
			Long ownerId = KeyFactory.stringToKey(annotations.getObjectId());
			ownerIds.add(ownerId);
			if (annotations.getScopeId() != null) {
				Long thisScopeId = KeyFactory.stringToKey(annotations.getScopeId());
				if (scopeId==null) {
					scopeId=thisScopeId;
				} else {
					if (!scopeId.equals(thisScopeId))
						throw new IllegalArgumentException("Expected all annotations to have have the same scope but found "+
								scopeId+ " and "+thisScopeId);
				}
			}
			
			List<LongAnnotation> longAnnos = annotations.getLongAnnos();
			if (longAnnos != null) {
				for (LongAnnotation la : longAnnos) {
					longAnnoDBOs.add(AnnotationDBOUtils.createLongAnnotationDBO(ownerId, la));
					stringAnnoDBOs.add(AnnotationDBOUtils.createStringAnnotationDBO(ownerId, la));
				}
			}
			List<DoubleAnnotation> doubleAnnos = annotations.getDoubleAnnos();
			if (doubleAnnos != null) {
				for (DoubleAnnotation da : doubleAnnos) {
					if (da.getValue()==null || (!Double.isInfinite(da.getValue()) && !Double.isNaN(da.getValue()))) {
						doubleAnnoDBOs.add(AnnotationDBOUtils.createDoubleAnnotationDBO(ownerId, da));
					}
					stringAnnoDBOs.add(AnnotationDBOUtils.createStringAnnotationDBO(ownerId, da));
				}
			}
			List<StringAnnotation> stringAnnos = annotations.getStringAnnos();
			if (stringAnnos != null) {
				for (StringAnnotation sa : stringAnnos) {
					stringAnnoDBOs.add(AnnotationDBOUtils.createStringAnnotationDBO(ownerId, sa));
				}
			}
			// Create the serialized blob
			AnnotationsBlobDBO ssAnnoBlobDBO = new AnnotationsBlobDBO();
			ssAnnoBlobDBO.setSubmissionId(ownerId);
			Long version = annotations.getVersion();
			if (version==null) throw new IllegalArgumentException("Version cannot be null.");
			ssAnnoBlobDBO.setVersion(version);
			JSONObjectAdapter joa = new JSONObjectAdapterImpl();
			try {
				annotations.writeToJSONObject(joa);
			} catch (JSONObjectAdapterException e) {
				throw new RuntimeException(e);
			}
			ssAnnoBlobDBO.setAnnoBlob(joa.toJSONString().getBytes());
			ssAnnoBlobDBOs.add(ssAnnoBlobDBO);
		}
		
		// Persist the DBOs
		
		// Delete existing annos for this object
		deleteAnnotationsByOwnerIds(ownerIds);
		
		// Create an owners for these objects
		List<AnnotationsOwnerDBO> ownerDBOs = new ArrayList<AnnotationsOwnerDBO>();
		for (Long ownerId : ownerIds) {
			AnnotationsOwnerDBO ownerDBO = new AnnotationsOwnerDBO();
			ownerDBO.setSubmissionId(ownerId);
			ownerDBO.setEvaluationId(scopeId);
			ownerDBOs.add(ownerDBO);
		}
		dboBasicDao.createBatch(ownerDBOs);
		
		dboBasicDao.createBatch(ssAnnoBlobDBOs);
		
		// Create the typed annos
		if (!longAnnoDBOs.isEmpty()) {
			dboBasicDao.createBatch(longAnnoDBOs);
		}
		if (!doubleAnnoDBOs.isEmpty()) {
			dboBasicDao.createBatch(doubleAnnoDBOs);
		}
		if (!stringAnnoDBOs.isEmpty()) {
			dboBasicDao.createBatch(stringAnnoDBOs);
		}
	}
	
	private void deleteAnnotationsByOwnerIds(List<Long> ownerIds) {
		if (ownerIds == null || ownerIds.isEmpty()) throw new IllegalArgumentException("Owner ids required");
		// Delete the annotation's owner which will trigger the cascade delete of all annotations.
		MapSqlParameterSource param = new MapSqlParameterSource();
		Collections.sort(ownerIds);
		param.addValue(COL_SUBSTATUS_ANNO_SUBID, ownerIds);
		namedJdbcTemplate.update(DELETE_FROM_ANNO_OWNERS, param);
	}
	
	
	private static final RowMapper<SubmissionDBO> submissionRowMapper = ((new SubmissionDBO()).getTableMapping());

	private static final RowMapper<SubmissionStatusDBO> statusRowMapper = ((new SubmissionStatusDBO()).getTableMapping());

	
	@Override
	public List<SubmissionBundle> getChangedSubmissions(Long scopeId) {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(COL_SUBMISSION_EVAL_ID, scopeId);
		return namedJdbcTemplate.query(SELECT_MISSING_OR_CHANGED_SUBSTATUSES, param,
				new RowMapper<SubmissionBundle>() {
					@Override
					public SubmissionBundle mapRow(ResultSet rs, int rowNum)
							throws SQLException {
						SubmissionBundle sb = new SubmissionBundle();
						SubmissionDBO submissionDBO = submissionRowMapper.mapRow(rs,  rowNum);
						Submission submission = new Submission();
						SubmissionUtils.copyDboToDto(submissionDBO, submission);
						sb.setSubmission(submission);
						
						SubmissionStatusDBO statusDBO = statusRowMapper.mapRow(rs,  rowNum);
						SubmissionStatus submissionStatus = SubmissionUtils.convertDboToDto(statusDBO);
						sb.setSubmissionStatus(submissionStatus);
						return sb;
					}
		});
	}
	
	@WriteTransaction
	@Override
	public void deleteAnnotationsByScope(Long scopeId) {
		if (scopeId == null) throw new IllegalArgumentException("Owner id cannot be null");
		// first find the IDs that are in the annotations table but (no longer) in the SUBMISSION (truth) table
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(COL_SUBSTATUS_ANNO_EVALID, scopeId);
		List<Long> idsToDelete = namedJdbcTemplate.query(
				SELECT_IDS_FOR_DELETED_SUBMISSIONS, param,
				new RowMapper<Long>(){
					@Override
					public Long mapRow(ResultSet rs, int rowNum)
							throws SQLException {
						return rs.getLong(COL_SUBSTATUS_ANNO_SUBID);
					}});
		
		// Now delete the annotation table entries
		// deleting the annotations' owners which will trigger the cascade delete of all annotations.
		if (idsToDelete.isEmpty()) return;
		param = new MapSqlParameterSource();
		param.addValue(COL_SUBSTATUS_ANNO_SUBID, idsToDelete);
		namedJdbcTemplate.update(DELETE_ANNOS_FOR_DELETED_SUBMISSIONS, param);
	}

}
