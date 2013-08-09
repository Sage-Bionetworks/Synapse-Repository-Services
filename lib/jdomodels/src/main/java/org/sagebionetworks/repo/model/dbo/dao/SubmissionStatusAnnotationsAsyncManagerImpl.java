package org.sagebionetworks.repo.model.dbo.dao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.sagebionetworks.evaluation.dao.AnnotationsDAO;
import org.sagebionetworks.evaluation.dao.SubmissionDAO;
import org.sagebionetworks.evaluation.dao.SubmissionStatusDAO;
import org.sagebionetworks.evaluation.dbo.DBOConstants;
import org.sagebionetworks.evaluation.model.Submission;
import org.sagebionetworks.evaluation.model.SubmissionStatus;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.SubmissionStatusAnnotationsAsyncManager;
import org.sagebionetworks.repo.model.annotation.AnnotationBase;
import org.sagebionetworks.repo.model.annotation.Annotations;
import org.sagebionetworks.repo.model.annotation.DoubleAnnotation;
import org.sagebionetworks.repo.model.annotation.LongAnnotation;
import org.sagebionetworks.repo.model.annotation.StringAnnotation;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public class SubmissionStatusAnnotationsAsyncManagerImpl implements SubmissionStatusAnnotationsAsyncManager {

	@Autowired
	private SubmissionDAO submissionDAO;
	@Autowired
	private SubmissionStatusDAO submissionStatusDAO;
	@Autowired
	private AnnotationsDAO annotationsDAO;	

	public SubmissionStatusAnnotationsAsyncManagerImpl() {};

	/**
	 * Constructor for testing.
	 */
	public SubmissionStatusAnnotationsAsyncManagerImpl(SubmissionDAO submissionDAO, 
			SubmissionStatusDAO submissionStatusDAO, AnnotationsDAO annotationsDAO) {
		this.submissionDAO = submissionDAO;
		this.submissionStatusDAO = submissionStatusDAO;
		this.annotationsDAO = annotationsDAO;
	}


	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void createSubmissionStatus(String subId) 
			throws NotFoundException, DatastoreException, JSONObjectAdapterException {
		if (subId == null) throw new IllegalArgumentException("Id cannot be null");
		replaceAnnotations(subId);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void updateSubmissionStatus(String subId) 
			throws NotFoundException, DatastoreException, JSONObjectAdapterException {
		if (subId == null) throw new IllegalArgumentException("Id cannot be null");
		replaceAnnotations(subId);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void deleteSubmission(String id) {
		if (id == null) throw new IllegalArgumentException("Id cannot be null");
		Long subId = KeyFactory.stringToKey(id);
		annotationsDAO.deleteAnnotationsByOwnerId(subId);
	}

	private void replaceAnnotations(String subId) 
			throws DatastoreException, NotFoundException, JSONObjectAdapterException {
		Submission submission = submissionDAO.get(subId);
		SubmissionStatus subStatus = submissionStatusDAO.get(subId);

		if (submission == null || subStatus == null) {
			throw new NotFoundException("Could not find Submission " + subId);
		}
		
		replaceAnnotations(submission, subStatus);
	}
		
	/**
	 * This is a short-circuit method to replace Annotations directly from the provided objects.
	 * It is not defined in the header class, and should only be used for testing purposes.
	 * 
	 * @param submission
	 * @param subStatus
	 * @throws DatastoreException
	 * @throws JSONObjectAdapterException
	 */
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void replaceAnnotations(Submission submission, SubmissionStatus subStatus) 
			throws DatastoreException, JSONObjectAdapterException {
		String subId = submission.getId();
		if (!subId.equals(subStatus.getId())) {
			throw new IllegalArgumentException("Submission and SubmissionStatus IDs do not match!");
		}
		
		// Prepare all Annotations
		Annotations annos = subStatus.getAnnotations();
		if (annos == null) {
			annos = new Annotations();
		}
		annos.setObjectId(subId);
		annos.setScopeId(submission.getEvaluationId());
				
		// We use Maps since we will be inserting system-defined Annotations which may overwrite
		// user Annotations
		Map<String, LongAnnotation> longAnnoMap = new HashMap<String, LongAnnotation>();
		Map<String, DoubleAnnotation> doubleAnnoMap = new HashMap<String, DoubleAnnotation>();
		Map<String, StringAnnotation> stringAnnoMap = new HashMap<String, StringAnnotation>();

		// Prepare the LongAnnos
		if (annos.getLongAnnos() != null) {
			for (LongAnnotation la : annos.getLongAnnos()) {
				longAnnoMap.put(la.getKey(), la);
			}
		}

		// Prepare the DoubleAnno DBOs
		if (annos.getDoubleAnnos() != null) {
			for (DoubleAnnotation da : annos.getDoubleAnnos()) {
				doubleAnnoMap.put(da.getKey(), da);
			}
		}

		// Prepare the StringAnno DBOs
		if (annos.getStringAnnos() != null) {
			for (StringAnnotation sa : annos.getStringAnnos()) {
				stringAnnoMap.put(sa.getKey(), sa);
			}
		}

		// Insert system-defined Annotations for this Submission
		insertSystemAnnotations(submission, subStatus, longAnnoMap, doubleAnnoMap, stringAnnoMap);
		
		// Pass along to the DAO
		annos.setLongAnnos(new ArrayList<LongAnnotation>(longAnnoMap.values()));
		annos.setDoubleAnnos(new ArrayList<DoubleAnnotation>(doubleAnnoMap.values()));
		annos.setStringAnnos(new ArrayList<StringAnnotation>(stringAnnoMap.values()));
		annotationsDAO.replaceAnnotations(annos);
	}

	private void insertSystemAnnotations(Submission submission, SubmissionStatus subStatus,
			Map<String, LongAnnotation> longAnnoMap,
			Map<String, DoubleAnnotation> doubleAnnoMap,
			Map<String, StringAnnotation> stringAnnoMap) {
		// owner ID (the Submission ID)
		LongAnnotation ownerIdAnno = new LongAnnotation();
		ownerIdAnno.setIsPrivate(false);
		ownerIdAnno.setKey(DBOConstants.PARAM_ANNOTATION_OBJECT_ID);
		if (submission.getId() != null) {
			ownerIdAnno.setValue(KeyFactory.stringToKey(submission.getId()));
		}
		insertAnnotation(ownerIdAnno, longAnnoMap, doubleAnnoMap, stringAnnoMap);

		// ownerParent ID (the Evaluation ID)
		LongAnnotation ownerParentIdAnno = new LongAnnotation();
		ownerParentIdAnno.setIsPrivate(false);
		ownerParentIdAnno.setKey(DBOConstants.PARAM_ANNOTATION_SCOPE_ID);
		if (submission.getEvaluationId() != null) {
			ownerParentIdAnno.setValue(KeyFactory.stringToKey(submission.getEvaluationId()));
		}
		insertAnnotation(ownerParentIdAnno, longAnnoMap, doubleAnnoMap, stringAnnoMap);
		
		// creator userId
		LongAnnotation creatorIdAnno = new LongAnnotation();
		creatorIdAnno.setIsPrivate(true);
		creatorIdAnno.setKey(DBOConstants.PARAM_SUBMISSION_USER_ID);
		if (submission.getUserId() != null) {
			creatorIdAnno.setValue(KeyFactory.stringToKey(submission.getUserId()));
		}
		insertAnnotation(creatorIdAnno, longAnnoMap, doubleAnnoMap, stringAnnoMap);
		
		// submitterAlias
		StringAnnotation submitterAnno = new StringAnnotation();
		submitterAnno.setIsPrivate(true);
		submitterAnno.setKey(DBOConstants.PARAM_SUBMISSION_SUBMITTER_ALIAS);
		submitterAnno.setValue(submission.getSubmitterAlias());
		insertAnnotation(submitterAnno, longAnnoMap, doubleAnnoMap, stringAnnoMap);
		
		// entityId
		LongAnnotation entityIdAnno = new LongAnnotation();
		entityIdAnno.setIsPrivate(false);
		entityIdAnno.setKey(DBOConstants.PARAM_SUBMISSION_ENTITY_ID);
		if (submission.getEntityId() != null) {
			entityIdAnno.setValue(KeyFactory.stringToKey(submission.getEntityId()));
		}
		insertAnnotation(entityIdAnno, longAnnoMap, doubleAnnoMap, stringAnnoMap);
		
		// entity version
		LongAnnotation versionAnno = new LongAnnotation();
		versionAnno.setIsPrivate(false);
		versionAnno.setKey(DBOConstants.PARAM_SUBMISSION_ENTITY_VERSION);
		versionAnno.setValue(submission.getVersionNumber());
		insertAnnotation(versionAnno, longAnnoMap, doubleAnnoMap, stringAnnoMap);
		
		// name
		StringAnnotation nameAnno = new StringAnnotation();
		nameAnno.setIsPrivate(true);
		nameAnno.setKey(DBOConstants.PARAM_SUBMISSION_NAME);
		nameAnno.setValue(submission.getName());
		insertAnnotation(nameAnno, longAnnoMap, doubleAnnoMap, stringAnnoMap);
		
		// createdOn
		LongAnnotation createdOnAnno = new LongAnnotation();
		createdOnAnno.setIsPrivate(true);
		createdOnAnno.setKey(DBOConstants.PARAM_SUBMISSION_CREATED_ON);
		if (submission.getCreatedOn() != null) {
			createdOnAnno.setValue(submission.getCreatedOn().getTime());
		}
		insertAnnotation(createdOnAnno, longAnnoMap, doubleAnnoMap, stringAnnoMap);
		
		// modifiedOn
		LongAnnotation modifiedOnAnno = new LongAnnotation();
		modifiedOnAnno.setIsPrivate(false);
		modifiedOnAnno.setKey(DBOConstants.PARAM_SUBSTATUS_MODIFIED_ON);
		if (subStatus.getModifiedOn() != null) {
			modifiedOnAnno.setValue(subStatus.getModifiedOn().getTime());
		}
		insertAnnotation(modifiedOnAnno, longAnnoMap, doubleAnnoMap, stringAnnoMap);
		
		// status
		StringAnnotation statusAnno = new StringAnnotation();
		statusAnno.setIsPrivate(false);
		statusAnno.setKey(DBOConstants.PARAM_SUBSTATUS_STATUS);
		if (subStatus.getStatus() != null) {
			statusAnno.setValue(subStatus.getStatus().toString());
		}
		insertAnnotation(statusAnno, longAnnoMap, doubleAnnoMap, stringAnnoMap);
	}
	
	private void insertAnnotation(AnnotationBase anno, Map<String, LongAnnotation> longAnnoMap, 
			Map<String, DoubleAnnotation> doubleAnnoMap, Map<String, StringAnnotation> stringAnnoMap) {
		String key = anno.getKey();
		if (anno instanceof LongAnnotation) {
			longAnnoMap.put(key, (LongAnnotation) anno);
			doubleAnnoMap.remove(key);
			stringAnnoMap.remove(key);
		} else if (anno instanceof DoubleAnnotation) {
			doubleAnnoMap.put(key, (DoubleAnnotation) anno);
			longAnnoMap.remove(key);
			stringAnnoMap.remove(key);
		} else if (anno instanceof StringAnnotation) {
			stringAnnoMap.put(key, (StringAnnotation) anno);
			longAnnoMap.remove(key);
			doubleAnnoMap.remove(key);
		} else {
			throw new IllegalArgumentException("Unknown Annotation type: " + anno.getClass());
		}
	}
}
