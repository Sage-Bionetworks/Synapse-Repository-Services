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
	
	
	/**
	 * The amount of memory needed (as a multiple of the original file size) to generate a Preview.
	 * 
	 * @return If the memory needs are not a function of the file size then return zero.  Otherwise 
	 * return the multiple of the file size.  For example, to generate a preview of image, the entire
	 * image might need be loaded into memory, and the resulting preview might also reside in memory.
	 * In such a case it will take 2.0 times the file size of memory to complete the preview so 2.0 should
	 * be returned.
	 * 
	 * Note: Error on setting this too high rather than too low.  If it is too low, the application 
	 * could run out of memory and crash.  If it is too high previews will not be generated for very large
	 * files. 
	 */
	public float memoryNeededAsMultipleOfFileSize();


}
