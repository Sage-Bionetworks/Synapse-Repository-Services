package org.sagebionetworks.markdown;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;

public class MarkdownDaoImpl implements MarkdownDao{

	public static final String MARKDOWN = "markdown";
	public static final String OUTPUT = "output";
	public static final String RESULT = "result";

	static private Logger log = LogManager.getLogger(MarkdownDaoImpl.class);

	@Autowired
	MarkdownClient markdownClient;

	@Override
	public String convertMarkdown(String rawMarkdown, String outputType) {
		ValidateArgument.required(rawMarkdown, "rawMarkdown");
		try {
			JSONObject request = new JSONObject();
			request.put(MARKDOWN, rawMarkdown);
			if (outputType != null) {
				request.put(OUTPUT, outputType);
			}
			JSONObject response = new JSONObject(markdownClient.requestMarkdownConversion(request.toString()));
			if (response.has(RESULT)) {
				return response.getString(RESULT);
			}
		} catch (Exception e) {
			log.info("Error converting markdown to html for: "+rawMarkdown+". Exception: "+e.getMessage());
		}
		return null;
	}

}
