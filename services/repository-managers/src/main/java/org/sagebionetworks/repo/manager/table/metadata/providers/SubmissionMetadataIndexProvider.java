package org.sagebionetworks.repo.manager.table.metadata.providers;

import java.util.Optional;
import java.util.Set;

import org.sagebionetworks.evaluation.dao.EvaluationDAO;
import org.sagebionetworks.evaluation.dao.SubmissionDAO;
import org.sagebionetworks.evaluation.dao.SubmissionField;
import org.sagebionetworks.evaluation.model.SubmissionStatus;
import org.sagebionetworks.repo.manager.evaluation.SubmissionManager;
import org.sagebionetworks.repo.manager.table.metadata.DefaultColumnModel;
import org.sagebionetworks.repo.manager.table.metadata.MetadataIndexProvider;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.annotation.v2.Annotations;
import org.sagebionetworks.repo.model.dbo.dao.table.ViewScopeDao;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.ObjectField;
import org.sagebionetworks.repo.model.table.ReplicationType;
import org.sagebionetworks.repo.model.table.SubType;
import org.sagebionetworks.repo.model.table.ViewObjectType;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.table.cluster.view.filter.HierarchicaFilter;
import org.sagebionetworks.table.cluster.view.filter.ViewFilter;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.collect.Sets;

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

	private final SubmissionManager submissionManager;
	private final SubmissionDAO submissionDao;
	private final EvaluationDAO evaluationDao;
	private final ViewScopeDao viewScopeDao;

	@Autowired
	public SubmissionMetadataIndexProvider(SubmissionManager submissionManager, SubmissionDAO submissionDao,
			EvaluationDAO evaluationDao, ViewScopeDao viewScopeDao) {
		this.submissionManager = submissionManager;
		this.submissionDao = submissionDao;
		this.evaluationDao = evaluationDao;
		this.viewScopeDao = viewScopeDao;
	}

	@Override
	public ViewObjectType getObjectType() {
		return OBJECT_TYPE;
	}
	
	Set<SubType> getSubTypes(){
		// Submissions are not hierarchical
		return Sets.newHashSet(SubType.submission);
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
	public DefaultColumnModel getDefaultColumnModel(Long viewTypeMask) {
		return DEFAULT_COLUMNS;
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
	public ViewFilter getViewFilter(Long viewId) {
		Set<Long> scope = viewScopeDao.getViewScope(viewId);
		return new HierarchicaFilter(ReplicationType.SUBMISSION, getSubTypes(), scope);
	}

	@Override
	public ViewFilter getViewFilter(Long viewTypeMask, Set<Long> containerIds) {
		return new HierarchicaFilter(ReplicationType.SUBMISSION, getSubTypes(), containerIds);
	}

	@Override
	public void validateScopeAndType(Long typeMask, Set<Long> scopeIds, int maxContainersPerView) {
		if (scopeIds != null && scopeIds.size() > maxContainersPerView) {
			throw new IllegalArgumentException(String.format(SCOPE_SIZE_LIMITED_EXCEEDED, maxContainersPerView));
		}
	}
	
}
