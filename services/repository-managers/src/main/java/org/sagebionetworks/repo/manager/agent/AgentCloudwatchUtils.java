package org.sagebionetworks.repo.manager.agent;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.sagebionetworks.cloudwatch.ProfileData;
import org.sagebionetworks.repo.manager.agent.handler.ReturnControlEvent;
import org.sagebionetworks.repo.model.agent.GridAgentSessionContext;
import org.sagebionetworks.repo.model.agent.SessionContext;
import org.sagebionetworks.util.ValidateArgument;

class AgentCloudwatchUtils {

    public enum AgentCloudwatchEventName {
        InvocationInput,
        InvocationInputFailure,
    }

    public static ProfileData generateCloudwatchProfileDataForInvocationInput(AgentCloudwatchEventName eventName, String namespace, SessionContext context, ReturnControlEvent returnControlEvent, Exception optionalException) {
        ValidateArgument.required(eventName, "eventName");

        ProfileData cloudWatchEvent = new ProfileData();
        cloudWatchEvent.setNamespace(namespace);
        cloudWatchEvent.setName(eventName.toString());
        cloudWatchEvent.setValue(1.0);
        cloudWatchEvent.setUnit("Count");
        cloudWatchEvent.setTimestamp(new Date());

        Map<String, String> dimensions = new HashMap<>();
        if (returnControlEvent != null) {
            dimensions.put("ActionGroup", returnControlEvent.getActionGroup());
            dimensions.put("FunctionName", returnControlEvent.getFunction());
            if (returnControlEvent.getParameters() != null) {
                dimensions.put("Parameters", returnControlEvent.getParameters().toString());
            }
        }
        if (context != null) {
            dimensions.put("SessionContextType", context.getConcreteType());
            if (context instanceof GridAgentSessionContext) {
                GridAgentSessionContext ctx = (GridAgentSessionContext) context;
                dimensions.put("GridSessionId", ctx.getGridSessionId());
                dimensions.put("GridAgentReplicaId", String.valueOf(ctx.getAgentsReplicaId()));
            }
        }


        if (optionalException != null) {
            dimensions.put("ExceptionType", optionalException.getClass().getSimpleName());
            dimensions.put("ExceptionMessage", optionalException.getMessage());
        }

        cloudWatchEvent.setDimension(dimensions);

        return cloudWatchEvent;
    }
}