package org.sagebionetworks.repo.manager.file.preview;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import org.imgscalr.Scalr;
import org.imgscalr.Scalr.Mode;
import org.sagebionetworks.StackConfigurationSingleton;

/**
 * Generates previews for image content types.
 * 
 * @author John
 *
 */
public class ImagePreviewGenerator implements PreviewGenerator {

	public static final String IMAGE_EXCEEDS_THE_MAXIMUM_SIZE = "Image exceeds the maximum size.";
	public static final String IMAGE_BMP = "image/bmp";
	public static final String IMAGE_PJPEG = "image/pjpeg";
	public static final String IMAGE_JPEG = "image/jpeg";
	public static final String IMAGE_GIF = "image/gif";
	public static final String IMAGE_PNG = "image/png";

	/**
	 * The maximum image size is 1001 x 1001 pixels.
	 */
	public static final long MAX_IMAGE_SIZE = 1000 * 1000;
	/**
	 * The supported content types for this generator, and the memory multipler that
	 * should be used.
	 */
	private static Map<String, Float> SUPPORTED_CONTENT_TYPES;
	static {
		SUPPORTED_CONTENT_TYPES = new HashMap<String, Float>();
		// Map the types to the memory requirements.
		// Since it is better to error on the high side, we multiple the calculated
		// memory use for each type by a fudge factor.
		float fudgeFactor = 1.2f;
		SUPPORTED_CONTENT_TYPES.put(IMAGE_BMP, 4.05f * fudgeFactor);
		SUPPORTED_CONTENT_TYPES.put(IMAGE_PJPEG, 23.38f * fudgeFactor);
		SUPPORTED_CONTENT_TYPES.put(IMAGE_JPEG, 23.38f * fudgeFactor);
		SUPPORTED_CONTENT_TYPES.put(IMAGE_GIF, 19.98f * fudgeFactor);
		SUPPORTED_CONTENT_TYPES.put(IMAGE_PNG, 46.28f * fudgeFactor);
	}

	@Override
	public PreviewOutputMetadata generatePreview(InputStream from, OutputStream to) throws IOException {
		// Determine the size of the image
		// First load the image
		BufferedImage image;
		try {
			image = loadImageWithSizeCheck(from, MAX_IMAGE_SIZE);
		}catch (ArrayIndexOutOfBoundsException e){
			throw new PreviewGenerationNotSupportedException("Improperly formatted image", e);
		}

		if (image == null) {
			throw new PreviewGenerationNotSupportedException("The passed input stream was not an image");
		}
		// Let image scalar do the heavy lifting!
		int maxWidthPixels = StackConfigurationSingleton.singleton().getMaximumPreviewWidthPixels();
		int maxHeightPixels = StackConfigurationSingleton.singleton().getMaximumPreviewHeightPixels();
		// only resize if original image is bigger than our preview max size
		int height = image.getHeight();
		int width = image.getWidth();
		if (height > maxHeightPixels || width > maxWidthPixels) {
			if (width > maxWidthPixels) {
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
	public static void main(String[] args)
			throws IOException, InterruptedException, InstantiationException, IllegalAccessException {
		for (String filePath : args) {
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

	/**
	 * Load an image from the given input stream as long as the image size
	 * (width*height) is less than the provided maxSize.  The size check will
	 * occur before the entire image is loaded into memory.
	 * <p>
	 * Note: This method was expanded from the {@link javax.imageio.ImageIO#read(InputStream)}
	 * </p>
	 * @param from
	 * @param maxSize The maximum size (width*height) of the image to be loaded.
	 * @return BufferedImage
	 * @throws PreviewGenerationNotSupportedException if the given image size (width*height) is
	 *                                  larger than the provided maxSize.
	 */
	public static BufferedImage loadImageWithSizeCheck(InputStream from, long maxSize) throws IOException {
		ImageInputStream stream = ImageIO.createImageInputStream(from);
		Iterator<ImageReader> iter = ImageIO.getImageReaders(stream);
		if (!iter.hasNext()) {
			return null;
		}
		ImageReader reader = iter.next();
		ImageReadParam param = reader.getDefaultReadParam();
		reader.setInput(stream, true, true);
		long width = reader.getWidth(0);
		long height = reader.getHeight(0);
		long imageSize = width * height;
		if (imageSize > maxSize) {
			throw new PreviewGenerationNotSupportedException(IMAGE_EXCEEDS_THE_MAXIMUM_SIZE);
		}
		BufferedImage image;
		try {
			image = reader.read(0, param);
		} finally {
			reader.dispose();
			stream.close();
		}
		if (image == null) {
			stream.close();
		}
		return image;
	}
}
