package org.sagebionetworks.markdown;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;

public class MarkdownDaoImpl implements MarkdownDao{

	public static final String MARKDOWN = "markdown";

	public static final String HTML = "html";

	static private Logger log = LogManager.getLogger(MarkdownDaoImpl.class);

	@Autowired
	MarkdownClient markdownClient;

	@Override
	public String convertToHtml(String rawMarkdown) {
		ValidateArgument.required(rawMarkdown, "rawMarkdown");
		try {
			JSONObject request = new JSONObject();
			request.put(MARKDOWN, rawMarkdown);
			JSONObject response = new JSONObject(markdownClient.requestMarkdownConversion(request.toString()));
			if (response.has(HTML)) {
				return response.getString(HTML);
			}
		} catch (Exception e) {
			log.info("Error converting markdown to html for: "+rawMarkdown+". Exception: "+e.getMessage());
		}
		return null;
	}

}
