package org.sagebionetworks.repo.manager.agent.context;

import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.agent.SessionContext;

public interface AgentContextValidatorHandler<T extends SessionContext> {

	/**
	 * The type of context handled by this implementation.
	 * 
	 * @return
	 */
	Class<? extends T> getContextType();

	/**
	 * Do the context validation.
	 * 
	 * @param user
	 * @param context
	 */
	void doContextValidation(UserInfo user, T context);

}
