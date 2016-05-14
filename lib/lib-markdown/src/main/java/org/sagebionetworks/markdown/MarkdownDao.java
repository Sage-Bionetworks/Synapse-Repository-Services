package org.sagebionetworks.markdown;

/**
 * Abstract for interacting with Markdown Server: http://markdownit.prod.sagebase.org
 */
public interface MarkdownDao {

	/**
	 * Send a request to the Markdown Server to convert the rawMarkdown to html
	 * 
	 * @param rawMarkdown
	 * @return
	 */
	public String convertToHtml(String rawMarkdown);
}
