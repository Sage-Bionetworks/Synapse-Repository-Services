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
	 * Return true for each content types supported. Return false for types that are not supported. The content types
	 * are defined by <a href="http://en.wikipedia.org/wiki/Internet_media_type">Internet_media_type</a>
	 * 
	 * @param contentType
	 * @param extension
	 * @return
	 */
	public boolean supportsContentType(String contentType, String extension);
	
	/**
	 * Generate a preview from the given input, and write it out to the given output stream.
	 * @param from - This stream contains the source data to generate a preview from.
	 * @param to - The preview should be written to this stream.
	 * @return  Must return the content type of generated preview.
	 * @throws IOException 
	 */
	public PreviewOutputMetadata generatePreview(InputStream from, OutputStream to) throws IOException;
	
	
	/**
	 * The amount of memory needed to generate a Preview based on the size of the input file.
	 * 
	 * @param mimeType The mime type of the file to load.
	 * @param contentSize The size of the file to preview.
	 * @return If the memory needs are a function of the file size then return a multiple of the file size. For example,
	 *         to generate a preview of a PNG image, the entire image might need be loaded into memory. Since the PNG
	 *         image format is compressed on disk, a 1 MB image on disk could take upwards to 50 MB to load in memory.
	 * 
	 *         Note: Error on setting this too high rather than too low. If it is too low, the application could run out
	 *         of memory and crash. If it is too high previews will not be generated for very large files.
	 */
	public long calculateNeededMemoryBytesForPreview(String mimeType, long contentSize);

}
