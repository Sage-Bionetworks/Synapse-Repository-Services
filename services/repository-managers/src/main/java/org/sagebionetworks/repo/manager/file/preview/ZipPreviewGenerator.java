package org.sagebionetworks.repo.manager.file.preview;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
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
		try {
			ZipInputStream zip = new ZipInputStream(from);
			ZipEntry zipEntry;
			StringBuilder sb = new StringBuilder();
			while ((zipEntry = zip.getNextEntry()) != null) {
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
		} catch (ZipException e){
			//can't support encrypted zips
			if (e.getMessage().contains("encrypted ZIP entry not supported")){
				throw new PreviewGenerationNotSupportedException("ZIP file is encrypted", e);
			}else {
				//rethrow for other zip exceptions
				throw e;
			}
		} catch (EOFException e){
			throw new PreviewGenerationNotSupportedException("Improperly formatted zip file", e);
		}
	}

	@Override
	public boolean supportsContentType(String contentType, String extension) {
		return APPLICATION_ZIP.equals(contentType);
	}

	@Override
	public long calculateNeededMemoryBytesForPreview(String mimeType, long contentSize) {
		//looking at the zipentries does not seem to depend on file size, so conservatively setting to 1
		//Here's the output of a 1.6GB archive:
		//Xcode.app.zip data:
		//File size: 1664.05 MB, Peak memory usage: 261.07 MB, Start free: 1072.94 MB, Peak free: 811.87 MB, End free: 1077.34 MB, Memory used: 0.16 x fileSize
		return contentSize;
	}
	
	public static void main(String[] args) throws IOException, InterruptedException, InstantiationException, IllegalAccessException{
		for(String filePath: args){
			File toRead = new File(filePath);
			PreviewGeneratorUtils.calculateMemoryRequirments(toRead, ZipPreviewGenerator.class);
		}
	}
}
