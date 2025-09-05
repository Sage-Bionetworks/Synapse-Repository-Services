package org.sagebionetworks.repo.manager.agent.context;

import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.agent.SessionContext;

public interface AgentContextValidator {

	/**
	 * Validate the provided session context.
	 * @param user
	 * @param context
	 * @return
	 */
	void validate(UserInfo user, SessionContext context);
}
