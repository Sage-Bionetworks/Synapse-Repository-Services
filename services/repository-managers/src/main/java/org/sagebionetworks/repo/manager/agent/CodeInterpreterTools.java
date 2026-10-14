package org.sagebionetworks.repo.manager.agent;

import org.sagebionetworks.repo.manager.agent.tool.JSONEntityTool;
import org.sagebionetworks.repo.manager.agent.tool.JSONEntityToolBase;
import org.sagebionetworks.repo.manager.agent.tool.JSONEntityToolParam;
import org.sagebionetworks.repo.model.agent.RunPythonRequest;
import org.springaicommunity.agentcore.codeinterpreter.AgentCoreCodeInterpreterClient;
import org.springaicommunity.agentcore.codeinterpreter.CodeExecutionResult;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.stereotype.Service;

/**
 * Code-interpreter tool built on {@link JSONEntityToolBase}. Exposing {@code runPython} through the
 * base (rather than Spring AI's native {@code @Tool}/{@code MethodToolCallback} path) means the base
 * owns the argument parse: a script whose JSON string value contains an unescaped control character
 * yields a model-visible corrective error so the model can resubmit with the character escaped,
 * instead of a hard failure in the framework's strict Jackson parser.
 */
@Service
public class CodeInterpreterTools extends JSONEntityToolBase {

	static final int MAX_RESPONSE_CHARS = 10_000;

	private final AgentCoreCodeInterpreterClient codeInterpreterClient;

	public CodeInterpreterTools(AgentCoreCodeInterpreterClient codeInterpreterClient) {
		super();
		this.codeInterpreterClient = codeInterpreterClient;
	}

	@JSONEntityTool(name = "runPython", description = "Execute a Python script in the current code interpreter session. "
			+ "Returns the script's stdout/stderr output.")
	public String runPython(
			@JSONEntityToolParam(description = "The Python script to execute", required = true) RunPythonRequest request,
			ToolContext toolContext) {
		String sessionId = CodeSessionSupplier.resolveSessionId(toolContext);
		if (sessionId == null) {
			return "Error: No code interpreter session ID available";
		}

		CodeExecutionResult result = codeInterpreterClient.executeCode(sessionId, "python", request.getScript());
		if (result.isError()) {
			return truncateOutput("Error: " + result.textOutput());
		}
		return truncateOutput(result.textOutput());
	}

	String truncateOutput(String output) {
		if (output == null) {
			return "";
		}
		if (output.length() <= MAX_RESPONSE_CHARS) {
			return output;
		}
		return output.substring(0, MAX_RESPONSE_CHARS) + "\n... [truncated at " + MAX_RESPONSE_CHARS + " chars]";
	}
}
