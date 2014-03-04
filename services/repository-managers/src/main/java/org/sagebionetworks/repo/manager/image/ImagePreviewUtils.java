package org.sagebionetworks.repo.manager.image;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.imageio.ImageIO;

import org.imgscalr.Scalr;

/**
 * A class for processing images.
 * @author John
 *
 */
public class ImagePreviewUtils {

	
	/**
	 * Reads the input file, creates the 
	 * @param inputfile
	 * @param maxSize
	 * @param out
	 * @throws IOException
	 */
	public static void createPreviewImage(InputStream input, int maxSize, OutputStream out) throws IOException{
		// First load the image
		BufferedImage image = ImageIO.read(input);
		if(image == null){
			throw new IllegalArgumentException("The passed input stream was not an image");
		}
		// Let image scalar do the heavy lifting!
		BufferedImage thumbnail = Scalr.resize(image, maxSize);
		ImageIO.write(thumbnail, "png", out);
	}
}
