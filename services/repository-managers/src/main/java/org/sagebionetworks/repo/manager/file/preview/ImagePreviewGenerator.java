package org.sagebionetworks.repo.manager.file.preview;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;

import org.imgscalr.Scalr;
import org.imgscalr.Scalr.Mode;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.StackConfigurationSingleton;

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
	 * The supported content types for this generator, and the memory multipler that should be used.
	 */
	private static Map<String, Float> SUPPORTED_CONTENT_TYPES;
	static{
		SUPPORTED_CONTENT_TYPES = new HashMap<String, Float>();
		// Map the types to the memory requirements.
		// Since it is better to error on the high side, we multiple the calculated
		// memory use for each type by a fudge factor.
		float fudgeFactor = 1.2f;
		SUPPORTED_CONTENT_TYPES.put(IMAGE_BMP, 4.05f*fudgeFactor);
		SUPPORTED_CONTENT_TYPES.put(IMAGE_PJPEG, 23.38f*fudgeFactor);
		SUPPORTED_CONTENT_TYPES.put(IMAGE_JPEG, 23.38f*fudgeFactor);
		SUPPORTED_CONTENT_TYPES.put(IMAGE_GIF, 19.98f*fudgeFactor);
		SUPPORTED_CONTENT_TYPES.put(IMAGE_PNG, 46.28f*fudgeFactor);
	}


	@Override
	public PreviewOutputMetadata generatePreview(InputStream from, OutputStream to) throws IOException {
		// First load the image
		BufferedImage image = ImageIO.read(from);
		if(image == null){
			throw new IllegalArgumentException("The passed input stream was not an image");
		}
		// Let image scalar do the heavy lifting!
		int maxWidthPixels = StackConfigurationSingleton.singleton().getMaximumPreviewWidthPixels();
		int maxHeightPixels = StackConfigurationSingleton.singleton().getMaximumPreviewHeightPixels();
		//only resize if original image is bigger than our preview max size
		int height = image.getHeight();
		int width = image.getWidth();
		if (height > maxHeightPixels || width > maxWidthPixels) {
			if (width > maxWidthPixels ) {
				image = Scalr.resize(image, Mode.FIT_TO_WIDTH, maxWidthPixels);
				height = image.getHeight();
			}
			if (height > maxHeightPixels)
				image = Scalr.resize(image, Mode.FIT_TO_HEIGHT, maxHeightPixels);
		} 
		ImageIO.write(image, "png", to);
		// the resulting image is a png
		return new PreviewOutputMetadata(IMAGE_PNG, ".png");
	}

	@Override
	public boolean supportsContentType(String contentType, String extension) {
		return SUPPORTED_CONTENT_TYPES.keySet().contains(contentType.toLowerCase());
	}
	
	/**
	 * Calculate the memory requirements of the passed list of files.
	 * 
	 * @param args
	 * @throws IOException 
	 * @throws InterruptedException 
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 */
	public static void main(String[] args) throws IOException, InterruptedException, InstantiationException, IllegalAccessException{
		for(String filePath: args){
			File toRead = new File(filePath);
			PreviewGeneratorUtils.calculateMemoryRequirments(toRead, ImagePreviewGenerator.class);
		}
	}

	@Override
	public long calculateNeededMemoryBytesForPreview(String mimeType, long contentSize) {
		double multiplier = SUPPORTED_CONTENT_TYPES.get(mimeType);
		long memoryNeededBytes = (long) Math.ceil((((double) contentSize) * multiplier));
		return memoryNeededBytes;
	}
}
