package org.sagebionetworks.repo.manager.file.preview;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Generates previews for text content types.
 * 
 * @author Jay
 *
 */
public class CsvPreviewGenerator extends TabCsvPreviewGenerator implements PreviewGenerator {
	
	@Override
	public PreviewOutputMetadata generatePreview(InputStream from, OutputStream to) throws IOException {
		setDelimiter(COMMA);
		return super.generatePreview(from, to);
	}
	
	
	@Override
	public boolean supportsContentType(String contentType) {
		return contentType.toLowerCase().equals(TEXT_CSV_SEPARATED_VALUES);
	}

	@Override
	public float getMemoryMultiplierForContentType(String contentType) {
		return super.getMemoryMultiplierForContentType(contentType);
	}
	
	public static void main(String[] args) throws IOException, InterruptedException, InstantiationException, IllegalAccessException{
		for(String filePath: args){
			File toRead = new File(filePath);
			PreviewGeneratorUtils.calculateMemoryRequirments(toRead, CsvPreviewGenerator.class);
		}
	}
}
