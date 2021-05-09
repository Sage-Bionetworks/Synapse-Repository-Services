package org.sagebionetworks.repo.manager.table.metadata.providers;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.sagebionetworks.evaluation.dao.EvaluationDAO;
import org.sagebionetworks.evaluation.dao.SubmissionDAO;
import org.sagebionetworks.evaluation.dao.SubmissionField;
import org.sagebionetworks.evaluation.model.SubmissionStatus;
import org.sagebionetworks.repo.manager.evaluation.SubmissionManager;
import org.sagebionetworks.repo.manager.table.metadata.DefaultColumnModel;
import org.sagebionetworks.repo.manager.table.metadata.MetadataIndexProvider;
import org.sagebionetworks.repo.model.IdAndEtag;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.annotation.v2.Annotations;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.ObjectDataDTO;
import org.sagebionetworks.repo.model.table.ObjectField;
import org.sagebionetworks.repo.model.table.ViewObjectType;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.collect.ImmutableList;

@Service
public class SubmissionMetadataIndexProvider implements MetadataIndexProvider {

	private static final ViewObjectType OBJECT_TYPE = ViewObjectType.	SUBMISSION;

	static final String SCOPE_SIZE_LIMITED_EXCEEDED = "The view's scope exceeds the maximum number of "
			+ "%d evaluations.";

	// @formatter:off

	static final DefaultColumnModel DEFAULT_COLUMNS = DefaultColumnModel.builder(OBJECT_TYPE)
			.withObjectField(
					ObjectField.id,
					ObjectField.name, 
					ObjectField.createdOn, 
					ObjectField.createdBy,
					ObjectField.etag, 
					ObjectField.modifiedOn,
					ObjectField.projectId
			)
			.withCustomField(SubmissionField.values())
			.build();
			 
	// @formatter:on

	private SubmissionManager submissionManager;
	private SubmissionDAO submissionDao;
	private EvaluationDAO evaluationDao;

	@Autowired
	public SubmissionMetadataIndexProvider(SubmissionManager submissionManager, SubmissionDAO submissionDao,
			EvaluationDAO evaluationDao) {
		this.submissionManager = submissionManager;
		this.submissionDao = submissionDao;
		this.evaluationDao = evaluationDao;
	}

	@Override
	public ViewObjectType getObjectType() {
		return OBJECT_TYPE;
	}

	@Override
	public List<String> getSubTypesForMask(Long typeMask) {
		// Submissions are not hierarchical
		return ImmutableList.of(OBJECT_TYPE.defaultSubType());
	}

	@Override
	public boolean isFilterScopeByObjectId(Long typeMask) {
		// No special treatment, always filter by the evaluation (parent)
		return false;
	}

	@Override
	public ColumnType getIdColumnType() {
		return ColumnType.SUBMISSIONID;
	}

	@Override
	public ColumnType getParentIdColumnType() {
		// The scope of a submission is the evaluation the submission belongs to
		return ColumnType.EVALUATIONID;
	}

	@Override
	public ColumnType getBenefactorIdColumnType() {
		// The evaluation the submission is part of drives the ACL
		return ColumnType.EVALUATIONID;
	}

	@Override
	public ObjectType getBenefactorObjectType() {
		return ObjectType.EVALUATION;
	}

	@Override
	public DefaultColumnModel getDefaultColumnModel(Long viewTypeMask) {
		return DEFAULT_COLUMNS;
	}

	@Override
	public List<ObjectDataDTO> getObjectData(List<Long> objectIds, int maxAnnotationChars) {
		return submissionDao.getSubmissionData(objectIds, maxAnnotationChars);
	}

	@Override
	public Set<Long> getContainerIdsForScope(Set<Long> scope, Long viewTypeMask, int containerLimit) {
		// The submissions are not hierarchical, so the scope cannot be expanded
		return scope;
	}

	@Override
	public String createViewOverLimitMessage(Long viewTypeMask, int containerLimit) {
		return String.format(SCOPE_SIZE_LIMITED_EXCEEDED, containerLimit);
	}

	@Override
	public Optional<Annotations> getAnnotations(UserInfo userInfo, String objectId) {
		// Fetch the current status
		SubmissionStatus status = getSubmissionStatus(userInfo, objectId);
		
		return Optional.ofNullable(status.getSubmissionAnnotations());
	}

	@WriteTransaction
	@Override
	public void updateAnnotations(UserInfo userInfo, String objectId, Annotations annotations) {
		ValidateArgument.required(annotations, "The annotations");
		ValidateArgument.required(annotations.getEtag(), "The annotations etag");
		
		// Fetch the current status
		SubmissionStatus status = getSubmissionStatus(userInfo, objectId);
		
		// Sync the etag to avoid overriding eventual conflicting updates
		status.setEtag(annotations.getEtag());
		// Sync the annotations
		status.setSubmissionAnnotations(annotations);
		
		submissionManager.updateSubmissionStatus(userInfo, status);
	}
	
	private SubmissionStatus getSubmissionStatus(UserInfo userInfo, String objectId) {
		ValidateArgument.required(userInfo, "The user");
		ValidateArgument.required(objectId, "The object id");
		
		// The object id might come in with the syn prefix
		Long submissionId = KeyFactory.stringToKey(objectId);
		
		return submissionManager.getSubmissionStatus(userInfo, submissionId.toString());
	}

	@Override
	public boolean canUpdateAnnotation(ColumnModel model) {
		boolean isCustomField = DEFAULT_COLUMNS.findCustomFieldByColumnName(model.getName()).isPresent();
		// Does not allow updating a custom field for now since it would
		return !isCustomField;
	}

	@Override
	public Set<Long> getContainerIdsForReconciliation(Set<Long> scope, Long viewTypeMask, int containerLimit) {
		// Always reconcile on the scope
		return getContainerIdsForScope(scope, viewTypeMask, containerLimit);
	}

	@Override
	public Set<Long> getAvailableContainers(List<Long> containerIds) {
		return evaluationDao.getAvailableEvaluations(containerIds);
	}

	@Override
	public List<IdAndEtag> getChildren(Long containerId) {
		return submissionDao.getSubmissionIdAndEtag(containerId);
	}

	@Override
	public Map<Long, Long> getSumOfChildCRCsForEachContainer(List<Long> containerIds) {
		return submissionDao.getSumOfSubmissionCRCsForEachEvaluation(containerIds);
	}

	@Override
	public void validateTypeMask(Long viewTypeMask) {
		// Nothing to validate, the mask is not used
	}
}
