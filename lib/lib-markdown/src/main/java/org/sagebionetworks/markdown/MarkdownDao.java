package org.sagebionetworks.markdown;

import java.io.IOException;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;

/**
 * Abstract for interacting with Markdown Server: http://markdownit.prod.sagebase.org
 */
public interface MarkdownDao {

	/**
	 * Send a request to the Markdown Server to convert the rawMarkdown to the output type.
	 * If the output type is not specified, the raw markdown will be converted to html with
	 * Synapse style.
	 * 
	 * @param rawMarkdown
	 * @param outputType
	 * @return
	 * @throws IOException 
	 * @throws ClientProtocolException
	 * @throws JSONException 
	 * @throws MarkdownClientException 
	 */
	String convertMarkdown(String rawMarkdown, String outputType) throws ClientProtocolException, IOException, JSONException, MarkdownClientException;
}
