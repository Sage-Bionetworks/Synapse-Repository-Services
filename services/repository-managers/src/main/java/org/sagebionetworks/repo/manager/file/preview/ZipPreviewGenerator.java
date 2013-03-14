package org.sagebionetworks.repo.manager.file.preview;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.IOUtils;

/**
 * Generates previews for zip content types.
 * 
 * @author Jay
 *
 */
public class ZipPreviewGenerator implements PreviewGenerator {
	
	public static final String APPLICATION_ZIP 	= "application/zip";
	public static final String TEXT_CSV 	= "text/csv";
	@Override
	public PreviewOutputMetadata generatePreview(InputStream from, OutputStream to) throws IOException {
		ZipInputStream zip = new ZipInputStream(from);
		ZipEntry zipEntry;
		StringBuilder sb = new StringBuilder();
		while((zipEntry = zip.getNextEntry()) != null) {
			String name = zipEntry.getName();
			//ignore these special entries
			if (name.startsWith("__MACOSX") || name.endsWith(".DS_Store")) {
				continue;
			}
			sb.append(zipEntry.getName());
			sb.append("\n");
		}
		IOUtils.write(sb.toString(), to, "UTF-8");
		
		return new PreviewOutputMetadata(TEXT_CSV, ".csv");
	}

	@Override
	public boolean supportsContentType(String contentType) {
		return APPLICATION_ZIP.equals(contentType.toLowerCase());
	}

	@Override
	public float getMemoryMultiplierForContentType(String contentType) {
		//looking at the zipentries does not seem to depend on file size, so conservatively setting to 1
		//Here's the output of a 1.6GB archive:
		//Xcode.app.zip data:
		//File size: 1664.05 MB, Peak memory usage: 261.07 MB, Start free: 1072.94 MB, Peak free: 811.87 MB, End free: 1077.34 MB, Memory used: 0.16 x fileSize
		return 1;
	}
	
	public static void main(String[] args) throws IOException, InterruptedException, InstantiationException, IllegalAccessException{
		for(String filePath: args){
			File toRead = new File(filePath);
			PreviewGeneratorUtils.calculateMemoryRequirments(toRead, ZipPreviewGenerator.class);
		}
	}
}
