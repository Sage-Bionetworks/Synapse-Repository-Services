package org.sagebionetworks.markdown;

import org.json.JSONException;
import org.json.JSONObject;

public class MarkdownDaoImpl implements MarkdownDao{

	public static final String MARKDOWN = "markdown";
	public static final String OUTPUT = "output";
	public static final String RESULT = "result";
	public static final String BASE_URL = "baseURL";

	private final MarkdownClient markdownClient;
	private final String synapseBaseUrl;

	public MarkdownDaoImpl(MarkdownClient markdownClient, String synapseBaseUrl) {
		this.markdownClient = markdownClient;
		this.synapseBaseUrl = synapseBaseUrl;
	}

	public String getSynapseBaseUrl() {
		return synapseBaseUrl;
	}

	@Override
	public String convertMarkdown(String rawMarkdown, String outputType) throws JSONException, MarkdownClientException {
		if (rawMarkdown == null) {
			throw new IllegalArgumentException("rawMarkdown cannot be null");
		}
		JSONObject request = new JSONObject();
		request.put(MARKDOWN, rawMarkdown);
		request.put(BASE_URL, synapseBaseUrl);
		if (outputType != null) {
			request.put(OUTPUT, outputType);
		}
		JSONObject response = new JSONObject(markdownClient.requestMarkdownConversion(request.toString()));
		return response.getString(RESULT);
	}

}
