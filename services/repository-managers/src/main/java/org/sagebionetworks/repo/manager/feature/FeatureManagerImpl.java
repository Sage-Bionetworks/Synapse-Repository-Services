package org.sagebionetworks.repo.manager.feature;

import java.util.Set;

import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dbo.feature.FeatureStatusDao;
import org.sagebionetworks.repo.model.feature.Feature;
import org.sagebionetworks.repo.model.feature.FeatureStatus;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class FeatureManagerImpl implements FeatureManager {
	
	private static final String UNAUTHORIZED_MESSAGE = "You must be an administrator to perform this operation.";
	
	private FeatureStatusDao featureStatusDao;

	@Autowired
	public FeatureManagerImpl(FeatureStatusDao featureTestingDao) {
		this.featureStatusDao = featureTestingDao;
	}
	
	@Override
	public boolean isFeatureEnabled(Feature feature) {
		ValidateArgument.required(feature, "The feature");
		// If there is no record in the DB we assume the feature is disabled
		return featureStatusDao.isFeatureEnabled(feature).orElse(false);
	}
	
	@Override
	public FeatureStatus getFeatureStatus(UserInfo user, Feature feature) {
		ValidateArgument.required(user, "The user");
		ValidateArgument.required(feature, "The feature");
		
		verifyAdmin(user);
		
		return getFeatureStatus(feature);
	}
	
	@Override
	@WriteTransaction
	public FeatureStatus setFeatureStatus(UserInfo user, Feature feature, FeatureStatus status) {
		ValidateArgument.required(user, "The user");
		ValidateArgument.required(feature, "The feature");
		ValidateArgument.required(status, "The status");
		ValidateArgument.required(status.getEnabled(), "The status.enabled");
		
		verifyAdmin(user);
		
		featureStatusDao.setFeatureEnabled(feature, status.getEnabled());
		
		return getFeatureStatus(feature);
	}
	
	private void verifyAdmin(UserInfo user) {
		if (!user.isAdmin()) {
			throw new UnauthorizedException(UNAUTHORIZED_MESSAGE);
		}
	}
	
	private FeatureStatus getFeatureStatus(Feature feature) {
		boolean isEnabled = isFeatureEnabled(feature);
		
		FeatureStatus status = new FeatureStatus();
		
		status.setFeature(feature);
		status.setEnabled(isEnabled);
		
		return status;
	}

	@Override
	public boolean isUserInTestingGroup(UserInfo user) {
		ValidateArgument.required(user, "The user");
		
		final Set<Long> userGroup = user.getGroups();
		
		if (userGroup == null || userGroup.isEmpty()) {
			return false;
		}
		
		return userGroup.contains(BOOTSTRAP_PRINCIPAL.SYNAPSE_TESTING_GROUP.getPrincipalId());
	}

}
