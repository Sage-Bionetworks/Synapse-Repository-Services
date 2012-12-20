package org.sagebionetworks.repo.manager.file.preview;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Abstraction for generating previews.
 * 
 * @author John
 *
 */
public interface PreviewGenerator {
	
	/**
	 * Return true for each content types supported.  Return false for types that are not supported.
	 * The content types are defined by <a href="http://en.wikipedia.org/wiki/Internet_media_type">Internet_media_type</a> 
	 * @param contentType
	 * @return
	 */
	public boolean supportsContentType(String contentType);
	/**
	 * Generate a preview from the given input, and write it out to the given output stream.
	 * @param from - This stream contains the source data to generate a preview from.
	 * @param to - The preview should be written to this stream.
	 * @return  Must return the content type of generated preview.
	 * @throws IOException 
	 */
	public String generatePreview(InputStream from, OutputStream to) throws IOException;

}
