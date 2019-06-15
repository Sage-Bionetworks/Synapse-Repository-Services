package org.sagebionetworks.repo.manager.file.preview;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.ImmutableSet;

/**
 * Generates previews for text content types.
 * 
 * @author Jay
 *
 */
public class TextPreviewGenerator implements PreviewGenerator {
	
	public static final String TEXT_PLAIN 	= "text/plain";
	public static final Set<String> TEXT_MIME_TYPES = ImmutableSet.<String> builder()
			.add("application/text", "application/x-sh", "application/x-javascript").build();
	
	public static final String TEXT_SLASH 	= "text/";
	public static final int MAX_CHARACTER_COUNT = 1500;
	@Override
	public PreviewOutputMetadata generatePreview(InputStream from, OutputStream to) throws IOException {
		// load the text
		//LineIterator works great until we run into a big file with a single line of text (that's when the memory requirements could be > 8x filesize)
		//LineIterator iterator = IOUtils.lineIterator(from, "UTF-8");
		String output = read(from);
		//and abbreviate
		IOUtils.write(StringUtils.abbreviate(output, MAX_CHARACTER_COUNT), to, "UTF-8");
		return new PreviewOutputMetadata(TEXT_PLAIN, ".txt");
	}
	
	public String read(InputStream from) throws IOException{
		StringBuilder buffer = new StringBuilder();
        InputStreamReader isr = new InputStreamReader(from, "UTF-8");
        Reader in = new BufferedReader(isr);
        int ch;
        int count = 0;
        while ((ch = in.read()) > -1 && count < MAX_CHARACTER_COUNT+10) {
            buffer.append((char)ch);
            count++;
        }
        in.close();
        return buffer.toString();
	}

	@Override
	public boolean supportsContentType(String contentType, String extension) {
		// supported if it's text or js or sh
		return contentType.startsWith(TEXT_SLASH) || TEXT_MIME_TYPES.contains(contentType);
	}

	@Override
	public long calculateNeededMemoryBytesForPreview(String mimeType, long contentSize) {
		return contentSize;
	}
	
	public static void main(String[] args) throws IOException, InterruptedException, InstantiationException, IllegalAccessException{
		for(String filePath: args){
			File toRead = new File(filePath);
			PreviewGeneratorUtils.calculateMemoryRequirments(toRead, TextPreviewGenerator.class);
		}
	}


}
