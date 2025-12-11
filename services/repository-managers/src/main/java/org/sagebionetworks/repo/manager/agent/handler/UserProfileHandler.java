package org.sagebionetworks.repo.manager.agent.handler;

import org.sagebionetworks.repo.manager.agent.parameter.ParameterUtils;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.service.UserProfileService;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.springframework.stereotype.Service;

@Service
public class UserProfileHandler implements ReturnControlHandler {

	private final UserProfileService userProfileService;

	public UserProfileHandler(UserProfileService userProfileService) {
		super();
		this.userProfileService = userProfileService;
	}

	@Override
	public String getActionGroup() {
		return "org_sage_zero";
	}

	@Override
	public String getFunction() {
		return "org_sage_zero_get_user_profile";
	}

	@Override
	public boolean needsWriteAccess() {
		return false;
	}

	@Override
	public String handleEvent(ReturnControlEvent event) throws Exception {
		String userId = ParameterUtils.extractParameter(String.class, "userId", event.getParameters())
				.orElseThrow(() -> new IllegalArgumentException("Parameter 'userId' of type string is required"));

		UserProfile result = userProfileService.getUserProfileByOwnerId(event.getRunAsUserId(), userId);
		return EntityFactory.createJSONStringForEntity(result);
	}

}
