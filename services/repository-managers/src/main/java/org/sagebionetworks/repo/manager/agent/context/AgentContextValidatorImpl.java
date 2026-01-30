package org.sagebionetworks.repo.manager.agent.context;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.agent.SessionContext;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AgentContextValidatorImpl implements AgentContextValidator {

	private final Map<Class<? extends SessionContext>, AgentContextValidatorHandler<? extends SessionContext>> handlerMap;

	@Autowired
	public AgentContextValidatorImpl(List<AgentContextValidatorHandler<? extends SessionContext>> handlers) {
		this.handlerMap = handlers.stream()
				.collect(Collectors.toMap(AgentContextValidatorHandler::getContextType, Function.identity()));
	}

	@Override
	public SessionContext validate(UserInfo user, SessionContext context) {
		ValidateArgument.required(user, "user");
		ValidateArgument.required(context, "context");

		AgentContextValidatorHandler<SessionContext> handler = (AgentContextValidatorHandler<SessionContext>) handlerMap
				.get(context.getClass());

		if (handler == null) {
			throw new IllegalArgumentException(
					"No validator handler found for context type: " + context.getClass().getName());
		}
		return handler.doContextValidation(user, context);
	}
}
