package org.sagebionetworks.repo.manager.file.preview;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;

import org.im4java.core.ConvertCmd;
import org.im4java.core.IM4JavaException;
import org.im4java.core.IMOperation;
import org.im4java.process.Pipe;
import org.sagebionetworks.StackConfiguration;

import com.google.common.collect.ImmutableSet;

/**
 * Generates previews for pdfs.
 * 
 */
public class PdfPreviewGenerator implements PreviewGenerator {

	public static final String ENCODING = "UTF-8";

	public static final Set<String> PDF_MIME_TYPES = ImmutableSet.<String> builder().add("text/pdf", "text/x-pdf", "application/pdf").build();

	public static final String IMAGE_MAGICK_SEARCH_PATH = "/usr/bin" + File.pathSeparator + "C:/cygwin64/bin/";

	@Override
	public PreviewOutputMetadata generatePreview(InputStream from, OutputStream to) throws IOException {
		IMOperation op = new IMOperation();

		Pipe pipeIn = new Pipe(from, null);
		Pipe pipeOut = new Pipe(null, to);
		ConvertCmd convert = new ConvertCmd();

		convert.setSearchPath(IMAGE_MAGICK_SEARCH_PATH);
		convert.setInputProvider(pipeIn);
		convert.setOutputConsumer(pipeOut);

		op.resize(StackConfiguration.getMaximumPreviewWidthPixels(), StackConfiguration.getMaximumPreviewHeightPixels());
		// this forces imagemagick to turn off unsupported transparent color which breaks resizing otherwise
		op.flatten();
		op.addImage("-[0]", "gif:-");

		try {
			convert.run(op);
		} catch (InterruptedException e) {
			throw new RuntimeException("ImageMagick convert interrupted: " + e.getMessage(), e);
		} catch (IM4JavaException e) {
			throw new RuntimeException("ImageMagick convert failed: " + e.getMessage(), e);
		}

		PreviewOutputMetadata metadata = new PreviewOutputMetadata("image/gif", ".gif");
		return metadata;
	}

	@Override
	public boolean supportsContentType(String contentType, String extension) {
		return PDF_MIME_TYPES.contains(contentType);
	}

	@Override
	public long calculateNeededMemoryBytesForPreview(String mimeType, long contentSize) {
		// whole file is read into memory pretty much
		return contentSize * 2;
	}
}
