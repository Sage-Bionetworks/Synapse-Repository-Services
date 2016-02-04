package org.sagebionetworks.repo.manager.file.preview;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Set;

import javax.imageio.ImageIO;

import org.apache.commons.io.IOUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.util.PDFImageWriter;
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
        PDDocument document = PDDocument.load(from);
		List<PDPage> list = document.getDocumentCatalog().getAllPages();
		PDPage firstPage = list.get(0);
		BufferedImage image = firstPage.convertToImage();
		Image thumbnail = image.getScaledInstance(StackConfiguration.getMaximumPreviewWidthPixels(), -1, Image.SCALE_SMOOTH);
		BufferedImage bufferedThumbnail = new BufferedImage(thumbnail.getWidth(null),
                thumbnail.getHeight(null),
                BufferedImage.TYPE_INT_RGB);
		bufferedThumbnail.getGraphics().drawImage(thumbnail, 0, 0, null);
		ImageIO.write(bufferedThumbnail, "png", to);
        PreviewOutputMetadata metadata = new PreviewOutputMetadata("image/png", ".png");
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
