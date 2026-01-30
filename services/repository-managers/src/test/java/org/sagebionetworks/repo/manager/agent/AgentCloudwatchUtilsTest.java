package org.sagebionetworks.repo.manager.agent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sagebionetworks.cloudwatch.ProfileData;
import org.sagebionetworks.repo.manager.agent.handler.ReturnControlEvent;
import org.sagebionetworks.repo.manager.agent.parameter.Parameter;
import org.sagebionetworks.repo.model.agent.GridAgentSessionContext;
import org.sagebionetworks.repo.model.agent.SessionContext;

import software.amazon.awssdk.services.bedrockagentruntime.model.FunctionInvocationInput;
import software.amazon.awssdk.services.bedrockagentruntime.model.InvocationInputMember;

class AgentCloudwatchUtilsTest {
    SessionContext context;
    String namespace;
    String actionGroup;
    String functionName;
    List<Parameter> parameters;
    ReturnControlEvent returnControlEvent;
    Exception exception;

    @BeforeEach
    public void before() {
        context = null;
        namespace = "ns";
        actionGroup = "testActionGroup";
        functionName = "testFunction";
        returnControlEvent = new ReturnControlEvent(123L, actionGroup, functionName, parameters);
        exception = null;
    }

    @Test
    public void testNullEventName() {
        assertThrows(IllegalArgumentException.class, () -> AgentCloudwatchUtils.generateCloudwatchProfileDataForInvocationInput(null, namespace, context, returnControlEvent, exception));
    }

    @Test
    public void testWithParameters() {
        parameters = List.of(new Parameter("param1", "string", "value1"), new Parameter("param2", "number", "42"));
        returnControlEvent = new ReturnControlEvent(123L, actionGroup, functionName, parameters);

        ProfileData pd = AgentCloudwatchUtils.generateCloudwatchProfileDataForInvocationInput(AgentCloudwatchUtils.AgentCloudwatchEventName.InvocationInput, namespace, context, returnControlEvent, exception);
        assertNotNull(pd);
        assertEquals(namespace, pd.getNamespace());
        assertEquals(AgentCloudwatchUtils.AgentCloudwatchEventName.InvocationInput.toString(), pd.getName());
        assertEquals(1.0, pd.getValue());
        assertEquals("Count", pd.getUnit());
        assertNotNull(pd.getTimestamp());
        Map<String, String> dims = pd.getDimension();
        assertEquals(actionGroup, dims.get("ActionGroup"));
        assertEquals(functionName, dims.get("FunctionName"));
        assertEquals(parameters.toString(), dims.get("Parameters"));
    }

    @Test
    public void testNullParameters() {
        parameters = null;

        ProfileData pd = AgentCloudwatchUtils.generateCloudwatchProfileDataForInvocationInput(AgentCloudwatchUtils.AgentCloudwatchEventName.InvocationInput, namespace, context, returnControlEvent, exception);
        Map<String, String> dims = pd.getDimension();
        assertEquals(null, dims.get("Parameters"));
    }


    @Test
    public void testInvocationInputException() {
        exception = new RuntimeException("something bad");
        ProfileData pd = AgentCloudwatchUtils.generateCloudwatchProfileDataForInvocationInput(AgentCloudwatchUtils.AgentCloudwatchEventName.InvocationInputFailure, namespace, context, returnControlEvent, exception);

        Map<String, String> dims = pd.getDimension();
        assertEquals("RuntimeException", dims.get("ExceptionType"));
        assertEquals("something bad", dims.get("ExceptionMessage"));

    }

    @Test
    public void testGridContext() {
        GridAgentSessionContext gridContext = new GridAgentSessionContext();
        gridContext.setGridSessionId("grid-123");
        gridContext.setAgentsReplicaId(42L);

        context = gridContext;

        FunctionInvocationInput fin = FunctionInvocationInput.builder().actionGroup("action").function("gridFn").build();
        InvocationInputMember member = InvocationInputMember.builder().functionInvocationInput(fin).build();

        ProfileData pd = AgentCloudwatchUtils.generateCloudwatchProfileDataForInvocationInput(AgentCloudwatchUtils.AgentCloudwatchEventName.InvocationInput, namespace, context, returnControlEvent, exception);

        Map<String, String> dims = pd.getDimension();
        assertEquals("org.sagebionetworks.repo.model.agent.GridAgentSessionContext", dims.get("SessionContextType"));
        assertEquals("grid-123", dims.get("GridSessionId"));
        assertEquals("42", dims.get("GridAgentReplicaId"));
    }

}