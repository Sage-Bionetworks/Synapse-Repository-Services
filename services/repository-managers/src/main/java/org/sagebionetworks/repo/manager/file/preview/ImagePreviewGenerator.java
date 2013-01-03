package org.sagebionetworks.repo.manager.file.preview;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
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
	
	public static final float ONE_MEGA_BYTE = (float) Math.pow(2, 20.0);
	
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
		return SUPPORTED_CONTENT_TYPES.keySet().contains(contentType.toLowerCase());
	}
	
	/**
	 * Calculate the memory requirements of the passed list of files.
	 * 
	 * @param args
	 * @throws IOException 
	 * @throws InterruptedException 
	 */
	public static void main(String[] args) throws IOException, InterruptedException{
		for(String filePath: args){
			File toRead = new File(filePath);
			calculateMemoryRequirments(toRead);
		}
	}

	/**
	 * @param toRead
	 * @throws IOException
	 * @throws FileNotFoundException
	 * @throws InterruptedException
	 */
	public static void calculateMemoryRequirments(File toRead) throws IOException,
			FileNotFoundException, InterruptedException {
		double startFreeMB = freeMagaBytes();
		File tempOut = File.createTempFile("ImagePreviewGenerator", "tmp");
		FileOutputStream fos = new FileOutputStream(tempOut);
		FileInputStream fis = new FileInputStream(toRead);
		try{
			ImagePreviewGenerator gen = new ImagePreviewGenerator();
			gen.generatePreview(fis, fos);
			double peakFreeMB = freeMagaBytes();
			// see how much we can reclaim
			Runtime.getRuntime().gc();
			Thread.sleep(2000);
			double endFreeMB = freeMagaBytes();
			double peakUseMB = startFreeMB-peakFreeMB;
			double fileSizeMB = ((float)toRead.length())/ONE_MEGA_BYTE;
			double memoryMultiple = peakUseMB/fileSizeMB;
			System.out.println(toRead.getName()+" data:");
			System.out.println(String.format("\tFile size: %1$.2f MB, Peak memrory useage: %2$.2f MB, Start free: %3$.2f MB, Peak free: %4$.2f MB, End free: %5$.2f MB, Memory used: %6$.2f x fileSize", fileSizeMB, peakUseMB,startFreeMB, peakFreeMB, endFreeMB, memoryMultiple));
		}finally{
			fis.close();
			fos.close();
			tempOut.delete();
		}
	}

	/**
	 * @return
	 */
	public static float freeMagaBytes() {
		return ((float)Runtime.getRuntime().freeMemory())/ONE_MEGA_BYTE;
	}

	@Override
	public float getMemoryMultiplierForContentType(String contentType) {
		return SUPPORTED_CONTENT_TYPES.get(contentType);
	}

}
