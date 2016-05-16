package org.sagebionetworks.markdown;

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
	 */
	String convertMarkdown(String rawMarkdown, String outputType);
}
