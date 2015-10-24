package org.sagebionetworks.repo.model.dbo.dao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sagebionetworks.evaluation.dbo.DBOConstants;
import org.sagebionetworks.evaluation.model.EvaluationSubmissions;
import org.sagebionetworks.evaluation.model.Submission;
import org.sagebionetworks.evaluation.model.SubmissionBundle;
import org.sagebionetworks.evaluation.model.SubmissionStatus;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.SubmissionStatusAnnotationsAsyncManager;
import org.sagebionetworks.repo.model.annotation.AnnotationBase;
import org.sagebionetworks.repo.model.annotation.Annotations;
import org.sagebionetworks.repo.model.annotation.DoubleAnnotation;
import org.sagebionetworks.repo.model.annotation.LongAnnotation;
import org.sagebionetworks.repo.model.annotation.StringAnnotation;
import org.sagebionetworks.repo.model.evaluation.AnnotationsDAO;
import org.sagebionetworks.repo.model.evaluation.EvaluationSubmissionsDAO;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.springframework.beans.factory.annotation.Autowired;

import org.sagebionetworks.repo.transactions.WriteTransaction;

public class SubmissionStatusAnnotationsAsyncManagerImpl implements SubmissionStatusAnnotationsAsyncManager {

	@Autowired
	private AnnotationsDAO annotationsDAO;	
	@Autowired
	private EvaluationSubmissionsDAO evaluationSubmissionsDAO;

	public SubmissionStatusAnnotationsAsyncManagerImpl() {};

	/**
	 * Constructor for testing.
	 */
	public SubmissionStatusAnnotationsAsyncManagerImpl(AnnotationsDAO annotationsDAO,  
			EvaluationSubmissionsDAO evaluationSubmissionsDAO) {
		this.annotationsDAO = annotationsDAO;
		this.evaluationSubmissionsDAO=evaluationSubmissionsDAO;
	}

	private boolean isSubmissionsEtagValid(String evalId, String submissionsEtag) throws NumberFormatException {
		EvaluationSubmissions evalSubs = evaluationSubmissionsDAO.getForEvaluationIfExists(Long.parseLong(evalId));
		if (evalSubs == null) {
			return false;
		}
		return evalSubs.getEtag()!=null && evalSubs.getEtag().equals(submissionsEtag);
	}

	@WriteTransaction
	@Override
	public void createEvaluationSubmissionStatuses(String evalId, String submissionsEtag)
			throws NotFoundException, DatastoreException,
			JSONObjectAdapterException {
		createOrUpdateEvaluationSubmissionStatuses(evalId, submissionsEtag);
	}

	@WriteTransaction
	@Override
	public void updateEvaluationSubmissionStatuses(String evalId, String submissionsEtag)
			throws NotFoundException, DatastoreException,
			JSONObjectAdapterException {
		createOrUpdateEvaluationSubmissionStatuses(evalId, submissionsEtag);
	}
	
	private void createOrUpdateEvaluationSubmissionStatuses(String evalId, String submissionsEtag) 
			throws NumberFormatException, NotFoundException, DatastoreException, JSONObjectAdapterException {
		if (evalId == null) throw new IllegalArgumentException("Id cannot be null");
		if (!isSubmissionsEtagValid(evalId, submissionsEtag)) return;
		replaceAnnotationsForEvaluation(evalId);
		Long evalIdLong = KeyFactory.stringToKey(evalId);
		// delete any annotations for which the SubmissionStatus has been deleted
		annotationsDAO.deleteAnnotationsByScope(evalIdLong);
		
	}

	private void replaceAnnotationsForEvaluation(String evalId) throws DatastoreException, NotFoundException, JSONObjectAdapterException {
		// get the submissions and statuses that are new or have changed
		List<SubmissionBundle> changedSubmissions = annotationsDAO.getChangedSubmissions(Long.parseLong(evalId));
		if (changedSubmissions.isEmpty()) return;
		// create the updated annotations
		List<Annotations> annoList = new ArrayList<Annotations>();
		for (SubmissionBundle sb : changedSubmissions) {
			annoList.add(fillInAnnotations(sb.getSubmission(), sb.getSubmissionStatus()));
		}
		// push the updated annotations to the database
		annotationsDAO.replaceAnnotations(annoList);
	}

	@WriteTransaction
	@Override
	public void deleteEvaluationSubmissionStatuses(String evalIdString, String submissionsEtag) {
		if (evalIdString == null) throw new IllegalArgumentException("Id cannot be null");
		Long evalId = KeyFactory.stringToKey(evalIdString);
		annotationsDAO.deleteAnnotationsByScope(evalId);
	}

	private static Annotations fillInAnnotations(Submission submission, SubmissionStatus subStatus) {
		Annotations annos = subStatus.getAnnotations();
		if (annos == null) {
			annos = new Annotations();
		}
		annos.setObjectId(submission.getId());
		annos.setScopeId(submission.getEvaluationId());
		annos.setVersion(subStatus.getStatusVersion());
				
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
		return annos;
	}
	
	private static void insertSystemAnnotations(Submission submission, SubmissionStatus subStatus,
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
		StringAnnotation entityIdAnno = new StringAnnotation();
		entityIdAnno.setIsPrivate(false);
		entityIdAnno.setKey(DBOConstants.PARAM_SUBMISSION_ENTITY_ID);
		entityIdAnno.setValue(submission.getEntityId());
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
		
		// teamId
		LongAnnotation teamAnno = new LongAnnotation();
		teamAnno.setIsPrivate(true);
		teamAnno.setKey(DBOConstants.PARAM_SUBMISSION_TEAM_ID);
		teamAnno.setValue(submission.getTeamId()==null?null:Long.parseLong(submission.getTeamId()));
		insertAnnotation(teamAnno, longAnnoMap, doubleAnnoMap, stringAnnoMap);
	}
	
	private static void insertAnnotation(AnnotationBase anno, Map<String, LongAnnotation> longAnnoMap, 
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
