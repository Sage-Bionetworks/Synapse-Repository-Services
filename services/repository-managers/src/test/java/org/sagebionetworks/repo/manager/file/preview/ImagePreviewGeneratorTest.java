package org.sagebionetworks.repo.manager.file.preview;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;

import org.junit.Test;

public class ImagePreviewGeneratorTest {
	
	String overMaxFileName = "images/OverMax.jpg";
	String atMaxFileName = "images/AtMaxSize.png";

	/**
	 * Tests added for PLFM-5019
	 * @throws IOException
	 */
	@Test
	public void testloadImageWithSizeCheckOverLimit() throws IOException {
		try (InputStream in = ImagePreviewGeneratorTest.class.getClassLoader().getResourceAsStream(overMaxFileName);) {
			assertNotNull("Failed to find a test file on the classpath: " + overMaxFileName, in);
			try {
				// call under test
				ImagePreviewGenerator.loadImageWithSizeCheck(in, ImagePreviewGenerator.MAX_IMAGE_SIZE);
				fail();
			} catch (PreviewGenerationNotSupportedException e) {
				assertTrue(ImagePreviewGenerator.IMAGE_EXCEEDS_THE_MAXIMUM_SIZE.equals(e.getMessage()));
			}
		}
	}

	/**
	 * Tests added for PLFM-5019
	 * @throws IOException
	 */
	@Test
	public void testloadImageWithSizeCheckAtLimit() throws IOException {
		try (InputStream in = ImagePreviewGeneratorTest.class.getClassLoader().getResourceAsStream(atMaxFileName);) {
			assertNotNull("Failed to find a test file on the classpath: " + atMaxFileName, in);
			// call under test
			BufferedImage image = ImagePreviewGenerator.loadImageWithSizeCheck(in,
					ImagePreviewGenerator.MAX_IMAGE_SIZE);
			assertNotNull(image);
			assertEquals(1000, image.getWidth());
			assertEquals(1000, image.getHeight());
		}
	}
	

	@Test
	public void testGeneratePreview() throws IOException {
		File temp = File.createTempFile("ImagePreviewGeneratorTest", ".png");
		try (InputStream in = ImagePreviewGeneratorTest.class.getClassLoader().getResourceAsStream(atMaxFileName);
			FileOutputStream out = new FileOutputStream(temp)) {
			assertNotNull("Failed to find a test file on the classpath: " + atMaxFileName, in);
			
			ImagePreviewGenerator genertor = new ImagePreviewGenerator();
			// call under test
			PreviewOutputMetadata meta = genertor.generatePreview(in, out);
			assertNotNull(meta);
			assertEquals("image/png", meta.getContentType());
			assertEquals(".png", meta.getExtension());
			
			// Validate the result file
			out.flush();
			out.close();
			try(FileInputStream tempIn = new FileInputStream(temp)){
				BufferedImage image = ImageIO.read(tempIn);
				assertNotNull(image);
				// the preview should be smaller than the original file.
				assertTrue(image.getWidth() < 1000);
				assertTrue(image.getHeight() < 1000);
			}
		}finally {
			temp.delete();
		}
	}
}
