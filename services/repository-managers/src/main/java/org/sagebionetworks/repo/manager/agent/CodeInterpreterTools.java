package org.sagebionetworks.repo.manager.agent;

import org.springaicommunity.agentcore.codeinterpreter.AgentCoreCodeInterpreterClient;
import org.springaicommunity.agentcore.codeinterpreter.CodeExecutionResult;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

@Service
public class CodeInterpreterTools {

	static final int MAX_RESPONSE_CHARS = 10_000;

	private final AgentCoreCodeInterpreterClient codeInterpreterClient;

	public CodeInterpreterTools(AgentCoreCodeInterpreterClient codeInterpreterClient) {
		this.codeInterpreterClient = codeInterpreterClient;
	}

	@Tool(description = "Execute a Python script in the current code interpreter session. "
			+ "Returns the script's stdout/stderr output.")
	public String runPython(String script, ToolContext toolContext) {
		String sessionId = (String) toolContext.getContext().get("sessionId");
		if (sessionId == null) {
			return "Error: No code interpreter session ID available";
		}

		CodeExecutionResult result = codeInterpreterClient.executeCode(sessionId, "python", script);
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
