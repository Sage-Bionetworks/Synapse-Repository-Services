package org.sagebionetworks.repo.service.metadata;

import java.util.List;

import org.sagebionetworks.repo.manager.evaluation.EvaluationPermissionsManager;
import org.sagebionetworks.repo.manager.table.TableViewManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.table.SubmissionView;
import org.sagebionetworks.repo.model.table.ViewEntityType;
import org.sagebionetworks.repo.model.table.ViewScope;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service("submissionViewMetadataProvider")
public class SubmissionViewMetadataProvider extends ViewMetadataProvider<SubmissionView> implements EntityValidator<SubmissionView> {
	
	@Autowired
	private EvaluationPermissionsManager evaluationPermissionManager;
	
	@Autowired
	public SubmissionViewMetadataProvider(TableViewManager viewManager, EvaluationPermissionsManager evaluationPermissionManager) {
		super(viewManager);
		this.evaluationPermissionManager = evaluationPermissionManager;
	}
	
	@Override
	public void validateEntity(SubmissionView view, EntityEvent event)
			throws InvalidModelException, NotFoundException, DatastoreException, UnauthorizedException {
		
		super.validateEntity(view, event);
		
		validateScopeAccess(event.getUserInfo(), view.getScopeIds());		
	}
	
	@Override
	public ViewScope createViewScope(SubmissionView view) {
		ViewScope scope = new ViewScope();
		
		scope.setViewEntityType(ViewEntityType.submissionview);
		scope.setScope(view.getScopeIds());
		scope.setViewTypeMask(0L);
		
		return scope;
	}
	
	private void validateScopeAccess(UserInfo userInfo, List<String> scope) {
		if (scope == null || scope.isEmpty()) {
			return;
		}
		evaluationPermissionManager.hasAccess(userInfo, ACCESS_TYPE.READ_PRIVATE_SUBMISSION, scope);
	}
	
}
