package org.sagebionetworks.repo.model.dbo.dao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.sagebionetworks.evaluation.dao.AnnotationsDAO;
import org.sagebionetworks.evaluation.dbo.DBOConstants;
import org.sagebionetworks.evaluation.model.CancelControl;
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
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SubmissionStatusAnnotationsAsyncManagerImpl implements SubmissionStatusAnnotationsAsyncManager {
	
	public static final String REPOSITORY_NAME = "repositoryName";
	public static final String CAN_CANCEL = "canCancel";
	public static final String CANCEL_REQUESTED = "cancelRequested";
	public static final String CANCEL_CONTROL = "cancelControl";
	private static final String BUNDLE_ENTITY_FIELD = "entity";	
	public static final boolean SYSTEM_GENERATED_ANNO_IS_PRIVATE = false;

	private AnnotationsDAO annotationsDAO;

	@Autowired
	public SubmissionStatusAnnotationsAsyncManagerImpl(AnnotationsDAO annotationsDAO) {
		this.annotationsDAO = annotationsDAO;
	}

	@WriteTransaction
	@Override
	public void createEvaluationSubmissionStatuses(String evalId)
			throws NotFoundException, DatastoreException,
			JSONObjectAdapterException {
		createOrUpdateEvaluationSubmissionStatuses(evalId);
	}

	@WriteTransaction
	@Override
	public void updateEvaluationSubmissionStatuses(String evalId)
			throws NotFoundException, DatastoreException,
			JSONObjectAdapterException {
		createOrUpdateEvaluationSubmissionStatuses(evalId);
	}
	
	private void createOrUpdateEvaluationSubmissionStatuses(String evalId)
			throws NumberFormatException, NotFoundException, DatastoreException, JSONObjectAdapterException {
		if (evalId == null) throw new IllegalArgumentException("Id cannot be null");
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
	public void deleteEvaluationSubmissionStatuses(String evalIdString) {
		if (evalIdString == null) throw new IllegalArgumentException("Id cannot be null");
		Long evalId = KeyFactory.stringToKey(evalIdString);
		annotationsDAO.deleteAnnotationsByScope(evalId);
	}

	private static Annotations fillInAnnotations(Submission submission, SubmissionStatus subStatus) throws JSONObjectAdapterException {
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
			Map<String, StringAnnotation> stringAnnoMap) throws JSONObjectAdapterException {
		// owner ID (the Submission ID)
		LongAnnotation ownerIdAnno = new LongAnnotation();
		ownerIdAnno.setIsPrivate(SYSTEM_GENERATED_ANNO_IS_PRIVATE);
		ownerIdAnno.setKey(DBOConstants.PARAM_ANNOTATION_OBJECT_ID);
		if (submission.getId() != null) {
			ownerIdAnno.setValue(KeyFactory.stringToKey(submission.getId()));
		}
		insertAnnotation(ownerIdAnno, longAnnoMap, doubleAnnoMap, stringAnnoMap);

		// ownerParent ID (the Evaluation ID)
		LongAnnotation ownerParentIdAnno = new LongAnnotation();
		ownerParentIdAnno.setIsPrivate(SYSTEM_GENERATED_ANNO_IS_PRIVATE);
		ownerParentIdAnno.setKey(DBOConstants.PARAM_ANNOTATION_SCOPE_ID);
		if (submission.getEvaluationId() != null) {
			ownerParentIdAnno.setValue(KeyFactory.stringToKey(submission.getEvaluationId()));
		}
		insertAnnotation(ownerParentIdAnno, longAnnoMap, doubleAnnoMap, stringAnnoMap);
		
		// creator userId
		LongAnnotation creatorIdAnno = new LongAnnotation();
		creatorIdAnno.setIsPrivate(SYSTEM_GENERATED_ANNO_IS_PRIVATE);
		creatorIdAnno.setKey(DBOConstants.PARAM_SUBMISSION_USER_ID);
		if (submission.getUserId() != null) {
			creatorIdAnno.setValue(KeyFactory.stringToKey(submission.getUserId()));
		}
		insertAnnotation(creatorIdAnno, longAnnoMap, doubleAnnoMap, stringAnnoMap);
		
		// submitterAlias
		StringAnnotation submitterAnno = new StringAnnotation();
		submitterAnno.setIsPrivate(SYSTEM_GENERATED_ANNO_IS_PRIVATE);
		submitterAnno.setKey(DBOConstants.PARAM_SUBMISSION_SUBMITTER_ALIAS);
		submitterAnno.setValue(submission.getSubmitterAlias());
		insertAnnotation(submitterAnno, longAnnoMap, doubleAnnoMap, stringAnnoMap);
		
		// entityId
		StringAnnotation entityIdAnno = new StringAnnotation();
		entityIdAnno.setIsPrivate(SYSTEM_GENERATED_ANNO_IS_PRIVATE);
		entityIdAnno.setKey(DBOConstants.PARAM_SUBMISSION_ENTITY_ID);
		entityIdAnno.setValue(submission.getEntityId());
		insertAnnotation(entityIdAnno, longAnnoMap, doubleAnnoMap, stringAnnoMap);
		
		// entity version
		LongAnnotation versionAnno = new LongAnnotation();
		versionAnno.setIsPrivate(SYSTEM_GENERATED_ANNO_IS_PRIVATE);
		versionAnno.setKey(DBOConstants.PARAM_SUBMISSION_ENTITY_VERSION);
		versionAnno.setValue(submission.getVersionNumber());
		insertAnnotation(versionAnno, longAnnoMap, doubleAnnoMap, stringAnnoMap);
		
		// name
		StringAnnotation nameAnno = new StringAnnotation();
		nameAnno.setIsPrivate(SYSTEM_GENERATED_ANNO_IS_PRIVATE);
		nameAnno.setKey(DBOConstants.PARAM_SUBMISSION_NAME);
		nameAnno.setValue(submission.getName());
		insertAnnotation(nameAnno, longAnnoMap, doubleAnnoMap, stringAnnoMap);
		
		// createdOn
		LongAnnotation createdOnAnno = new LongAnnotation();
		createdOnAnno.setIsPrivate(SYSTEM_GENERATED_ANNO_IS_PRIVATE);
		createdOnAnno.setKey(DBOConstants.PARAM_SUBMISSION_CREATED_ON);
		if (submission.getCreatedOn() != null) {
			createdOnAnno.setValue(submission.getCreatedOn().getTime());
		}
		insertAnnotation(createdOnAnno, longAnnoMap, doubleAnnoMap, stringAnnoMap);
		
		// modifiedOn
		LongAnnotation modifiedOnAnno = new LongAnnotation();
		modifiedOnAnno.setIsPrivate(SYSTEM_GENERATED_ANNO_IS_PRIVATE);
		modifiedOnAnno.setKey(DBOConstants.PARAM_SUBSTATUS_MODIFIED_ON);
		if (subStatus.getModifiedOn() != null) {
			modifiedOnAnno.setValue(subStatus.getModifiedOn().getTime());
		}
		insertAnnotation(modifiedOnAnno, longAnnoMap, doubleAnnoMap, stringAnnoMap);
		
		// status
		StringAnnotation statusAnno = new StringAnnotation();
		statusAnno.setIsPrivate(SYSTEM_GENERATED_ANNO_IS_PRIVATE);
		statusAnno.setKey(DBOConstants.PARAM_SUBSTATUS_STATUS);
		if (subStatus.getStatus() != null) {
			statusAnno.setValue(subStatus.getStatus().toString());
		}
		insertAnnotation(statusAnno, longAnnoMap, doubleAnnoMap, stringAnnoMap);
		
		// teamId
		LongAnnotation teamAnno = new LongAnnotation();
		teamAnno.setIsPrivate(SYSTEM_GENERATED_ANNO_IS_PRIVATE);
		teamAnno.setKey(DBOConstants.PARAM_SUBMISSION_TEAM_ID);
		teamAnno.setValue(submission.getTeamId()==null?null:Long.parseLong(submission.getTeamId()));
		insertAnnotation(teamAnno, longAnnoMap, doubleAnnoMap, stringAnnoMap);
		
		// Docker repo name
		String dockerRepositoryName = getDockerRepositoryNameFromSubmission(submission);
		if (!StringUtils.isEmpty(dockerRepositoryName)) {
			StringAnnotation repoNameAnno = new StringAnnotation();
			repoNameAnno.setIsPrivate(SYSTEM_GENERATED_ANNO_IS_PRIVATE);
			repoNameAnno.setKey(REPOSITORY_NAME);
			repoNameAnno.setValue(dockerRepositoryName);
			insertAnnotation(repoNameAnno, longAnnoMap, doubleAnnoMap, stringAnnoMap);
		}
		
		// Docker Digest
		if (!StringUtils.isEmpty(submission.getDockerDigest())) {
			StringAnnotation digestAnno = new StringAnnotation();
			digestAnno.setIsPrivate(SYSTEM_GENERATED_ANNO_IS_PRIVATE);
			digestAnno.setKey(DBOConstants.PARAM_SUBMISSION_DOCKER_DIGEST);
			digestAnno.setValue(submission.getDockerDigest());
			insertAnnotation(digestAnno, longAnnoMap, doubleAnnoMap, stringAnnoMap);
		}

		// canCancel
		Boolean canCancel = Boolean.FALSE;
		if (subStatus.getCanCancel() != null && subStatus.getCanCancel()) {
			canCancel = Boolean.TRUE;
		}
		StringAnnotation canCancelAnno = new StringAnnotation();
		canCancelAnno.setIsPrivate(SYSTEM_GENERATED_ANNO_IS_PRIVATE);
		canCancelAnno.setKey(CAN_CANCEL);
		canCancelAnno.setValue(canCancel.toString());
		insertAnnotation(canCancelAnno, longAnnoMap, doubleAnnoMap, stringAnnoMap);

		// cancelRequested
		Boolean cancelRequested = Boolean.FALSE;
		if (subStatus.getCancelRequested() != null && subStatus.getCancelRequested()) {
			cancelRequested = Boolean.TRUE;
		}
		StringAnnotation cancelRequestedAnno = new StringAnnotation();
		cancelRequestedAnno.setIsPrivate(SYSTEM_GENERATED_ANNO_IS_PRIVATE);
		cancelRequestedAnno.setKey(CANCEL_REQUESTED);
		cancelRequestedAnno.setValue(cancelRequested.toString());
		insertAnnotation(cancelRequestedAnno, longAnnoMap, doubleAnnoMap, stringAnnoMap);

		// cancelControl
		CancelControl cancelControl = new CancelControl();
		cancelControl.setCanCancel(canCancel);
		cancelControl.setCancelRequested(cancelRequested);
		cancelControl.setSubmissionId(submission.getId());
		cancelControl.setUserId(submission.getUserId());
		StringAnnotation cancelControlAnno = new StringAnnotation();
		cancelControlAnno.setIsPrivate(SYSTEM_GENERATED_ANNO_IS_PRIVATE);
		cancelControlAnno.setKey(CANCEL_CONTROL);
		cancelControlAnno.setValue(EntityFactory.createJSONStringForEntity(cancelControl));
		insertAnnotation(cancelControlAnno, longAnnoMap, doubleAnnoMap, stringAnnoMap);
		
		// submitterId - will be teamId if the user is submitting for a team or the userId if individual
		LongAnnotation submitterId = new LongAnnotation();
		submitterId.setIsPrivate(SYSTEM_GENERATED_ANNO_IS_PRIVATE);
		submitterId.setKey(DBOConstants.PARAM_SUBMISSION_SUBMITTER_ID);
		Long subId = StringUtils.isEmpty(submission.getTeamId()) ?
				Long.parseLong(submission.getUserId()) : Long.parseLong(submission.getTeamId());
		submitterId.setValue(subId);
		insertAnnotation(submitterId, longAnnoMap, doubleAnnoMap, stringAnnoMap);
	}
	
	public static String getDockerRepositoryNameFromSubmission(Submission submission) {
		try{
			JSONObject bundle = new JSONObject(submission.getEntityBundleJSON());
			return bundle.getJSONObject(BUNDLE_ENTITY_FIELD).getString(REPOSITORY_NAME);
		} catch (JSONException e) {
			// if the serialized entity isn't as expected, just return null
			return null;
		}
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
