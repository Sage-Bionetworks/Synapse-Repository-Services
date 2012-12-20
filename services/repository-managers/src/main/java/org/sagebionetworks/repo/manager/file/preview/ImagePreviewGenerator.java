package org.sagebionetworks.repo.manager.file.preview;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.imageio.ImageIO;

import org.imgscalr.Scalr;
import org.sagebionetworks.StackConfiguration;

/**
 * Generates previews for image content types.
 * 
 * @author John
 *
 */
public class ImagePreviewGenerator implements PreviewGenerator {
	
	public static final String IMAGE_BMP 	= "image/bmp";
	public static final String IMAGE_PJPEG	= "image/pjpeg";
	public static final String IMAGE_JPEG	= "image/jpeg";
	public static final String IMAGE_GIF	= "image/gif";
	public static final String IMAGE_PNG	= "image/png";
	/**
	 * The supported content types for this generator.
	 */
	private static Set<String> SUPPORTED_CONTENT_TYPES = new HashSet<String>(Arrays.asList(new String[]{
			IMAGE_GIF,
			IMAGE_JPEG,
			IMAGE_PJPEG,
			IMAGE_PNG,
			IMAGE_BMP,
	}));

	@Override
	public String generatePreview(InputStream from, OutputStream to) throws IOException {
		// First load the image
		BufferedImage image = ImageIO.read(from);
		if(image == null){
			throw new IllegalArgumentException("The passed input stream was not an image");
		}
		// Let image scalar do the heavy lifting!
		BufferedImage thumbnail = Scalr.resize(image, StackConfiguration.getMaximumPreivewPixels());
		ImageIO.write(thumbnail, "png", to);
		// the resulting image is a png
		return IMAGE_PNG;
	}

	@Override
	public boolean supportsContentType(String contentType) {
		return SUPPORTED_CONTENT_TYPES.contains(contentType.toLowerCase());
	}

}
