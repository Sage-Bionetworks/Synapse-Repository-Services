package org.sagebionetworks.repo.manager.feature;

import java.util.Set;

import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dbo.feature.Feature;
import org.sagebionetworks.repo.model.dbo.feature.FeatureStatusDao;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class FeatureManagerImpl implements FeatureManager {
	
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
	public boolean isUserInTestingGroup(UserInfo user) {
		ValidateArgument.required(user, "The user");
		
		final Set<Long> userGroup = user.getGroups();
		
		if (userGroup == null || userGroup.isEmpty()) {
			return false;
		}
		
		return userGroup.contains(BOOTSTRAP_PRINCIPAL.SYNAPSE_TESTING_GROUP.getPrincipalId());
	}

}
