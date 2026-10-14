package org.sagebionetworks.repo.manager.agent.specialist.filesummary;

import java.io.StringWriter;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.sagebionetworks.repo.manager.agent.CodeSessionSupplier;
import org.sagebionetworks.repo.manager.agent.tool.JSONEntityTool;
import org.sagebionetworks.repo.manager.agent.tool.JSONEntityToolBase;
import org.sagebionetworks.repo.manager.agent.tool.JSONEntityToolParam;
import org.springaicommunity.agentcore.codeinterpreter.AgentCoreCodeInterpreterClient;
import org.springaicommunity.agentcore.codeinterpreter.CodeExecutionResult;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.stereotype.Service;

/**
 * Tools available to the file summary specialist. These inspect files that already exist on the
 * code interpreter session filesystem without loading their full contents into the agent's context,
 * which protects the supervisor's context window.
 */
@Service
public class FileSummaryTools extends JSONEntityToolBase {

	static final String INSPECT_TEMPLATE = "code-templates/file-inspect.py.vtp";
	static final String PDF_EXTRACT_TEMPLATE = "code-templates/pdf-extract.py.vtp";

	/**
	 * Maximum number of bytes of file content (the "head") returned by {@link #inspectFile}.
	 * Bounds how much raw file data can enter the agent's context.
	 */
	static final int MAX_HEAD_BYTES = 4_000;

	/**
	 * Maximum number of characters of extracted PDF text returned by {@link #extractPdfText}.
	 * Bounds how much document text can enter the agent's context.
	 */
	static final int MAX_PDF_TEXT_CHARS = 8_000;

	/**
	 * Maximum number of characters returned to the agent for a single tool response.
	 */
	static final int MAX_RESPONSE_CHARS = 10_000;

	private final AgentCoreCodeInterpreterClient codeInterpreterClient;
	private final VelocityEngine velocityEngine;

	public FileSummaryTools(AgentCoreCodeInterpreterClient codeInterpreterClient, VelocityEngine velocityEngine) {
		super();
		this.codeInterpreterClient = codeInterpreterClient;
		this.velocityEngine = velocityEngine;
	}

	@JSONEntityTool(description = "Inspect a file on the code interpreter session filesystem without loading its full contents. "
			+ "Returns the file size in bytes, the line count, and a bounded preview of the beginning of the file. "
			+ "Use this to understand a file's shape and content before summarizing it.")
	public String inspectFile(
			@JSONEntityToolParam(description = "Session-relative path of the file to inspect, e.g. 'query_specialist/results.csv'", required = true) String filePath,
			ToolContext toolContext) {
		String sessionId = extractSessionId(toolContext);
		if (sessionId == null) {
			return "Error: No code interpreter session ID available";
		}
		try {
			VelocityContext context = new VelocityContext();
			context.put("filePath", filePath);
			context.put("maxBytes", MAX_HEAD_BYTES);
			String inspectCode = renderTemplate(INSPECT_TEMPLATE, context);

			CodeExecutionResult result = codeInterpreterClient.executeCode(sessionId, "python", inspectCode);
			if (result.isError()) {
				return truncateOutput("Error inspecting file '" + filePath + "': " + result.textOutput());
			}
			return truncateOutput(result.textOutput());
		} catch (Exception e) {
			return "Error inspecting file '" + filePath + "': " + e.getMessage();
		}
	}

	@JSONEntityTool(description = "Extract text from a PDF file on the code interpreter session. Returns the page count, "
			+ "any document metadata, and the extracted text (bounded). Provide a page number to extract just that "
			+ "page (1-based); omit it or use 0 to extract text across the whole document. Use this instead of "
			+ "inspectFile for PDF files, which are binary.")
	public String extractPdfText(
			@JSONEntityToolParam(description = "Session-relative path of the PDF file, e.g. 'summary_specialist/report.pdf'", required = true) String filePath,
			@JSONEntityToolParam(description = "The 1-based page number to extract. Omit or use 0 to extract the whole document.", required = false) Integer pageNumber,
			ToolContext toolContext) {
		String sessionId = extractSessionId(toolContext);
		if (sessionId == null) {
			return "Error: No code interpreter session ID available";
		}
		try {
			VelocityContext context = new VelocityContext();
			context.put("filePath", filePath);
			context.put("pageNumber", pageNumber == null ? 0 : pageNumber.intValue());
			context.put("maxChars", MAX_PDF_TEXT_CHARS);
			String extractCode = renderTemplate(PDF_EXTRACT_TEMPLATE, context);

			CodeExecutionResult result = codeInterpreterClient.executeCode(sessionId, "python", extractCode);
			if (result.isError()) {
				return truncateOutput("Error extracting text from PDF '" + filePath + "': " + result.textOutput());
			}
			return truncateOutput(result.textOutput());
		} catch (Exception e) {
			return "Error extracting text from PDF '" + filePath + "': " + e.getMessage();
		}
	}

	private String extractSessionId(ToolContext toolContext) {
		return CodeSessionSupplier.resolveSessionId(toolContext);
	}

	private String renderTemplate(String templateName, VelocityContext context) {
		Template template = velocityEngine.getTemplate(templateName);
		StringWriter writer = new StringWriter();
		template.merge(context, writer);
		return writer.toString();
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
