package org.sagebionetworks.repo.model.image;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.junit.Test;

/**
 * Test for the ImagePreviewUtils
 * 
 * @author John
 *
 */
public class ImagePreviewUtilsTest {
	
	@Test
	public void testCreatePreviewJPG() throws IOException{
		String fileName = "images/tallSkinny.jpg";
		InputStream in = ImagePreviewUtilsTest.class.getClassLoader().getResourceAsStream(fileName);
		assertNotNull("Failed to find: "+fileName+" on the classpath", in);
		// Write the results to a temp file
		File temp = File.createTempFile("Preview", ".png");
		FileOutputStream out = new FileOutputStream(temp);
		try{
			long start = System.currentTimeMillis();
			ImagePreviewUtils.createPreviewImage(in, 160, out);
			long end = System.currentTimeMillis();
			out.close();
			assertTrue(temp.length() > 0);
			assertTrue("The size must be less than 100K", temp.length() < 100000);
			System.out.println("jpg size: "+temp.length()+" bytes in "+(end-start)+" ms");
		}finally{
			out.close();
			temp.delete();
		}
	}
	
	@Test
	public void testCreatePreviewPng() throws IOException{
		String fileName = "images/squarish.png";
		InputStream in = ImagePreviewUtilsTest.class.getClassLoader().getResourceAsStream(fileName);
		assertNotNull("Failed to find: "+fileName+" on the classpath", in);
		// Write the results to a temp file
		File temp = File.createTempFile("Preview", ".png");
		FileOutputStream out = new FileOutputStream(temp);
		try{
			long start = System.currentTimeMillis();
			ImagePreviewUtils.createPreviewImage(in, 160, out);
			long end = System.currentTimeMillis();
			out.close();
			assertTrue(temp.length() > 0);
			assertTrue("The size must be less than 100K", temp.length() < 100000);
			System.out.println("jpg size: "+temp.length()+" bytes in "+(end-start)+" ms");
		}finally{
			out.close();
			temp.delete();
		}
	}
	
	@Test
	public void testCreatePreviewGif() throws IOException{
		String fileName = "images/shortWide.gif";
		InputStream in = ImagePreviewUtilsTest.class.getClassLoader().getResourceAsStream(fileName);
		assertNotNull("Failed to find: "+fileName+" on the classpath", in);
		// Write the results to a temp file
		File temp = File.createTempFile("Preview", ".png");
		FileOutputStream out = new FileOutputStream(temp);
		try{
			long start = System.currentTimeMillis();
			ImagePreviewUtils.createPreviewImage(in, 160, out);
			long end = System.currentTimeMillis();
			out.close();
			assertTrue(temp.length() > 0);
			assertTrue("The size must be less than 100K", temp.length() < 100000);
			System.out.println("gif size: "+temp.length()+" bytes in "+(end-start)+" ms");
		}finally{
			out.close();
			temp.delete();
		}
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testCreatePreviewNotImage() throws IOException{
		String fileName = "images/notAnImage.txt";
		InputStream in = ImagePreviewUtilsTest.class.getClassLoader().getResourceAsStream(fileName);
		assertNotNull("Failed to find: "+fileName+" on the classpath", in);
		// Write the results to a temp file
		File temp = File.createTempFile("Preview", ".png");
		FileOutputStream out = new FileOutputStream(temp);
		try{
			long start = System.currentTimeMillis();
			ImagePreviewUtils.createPreviewImage(in, 160, out);
			long end = System.currentTimeMillis();
			out.close();
			assertTrue(temp.length() > 0);
			assertTrue("The size must be less than 100K", temp.length() < 100000);
			System.out.println("gif size: "+temp.length()+" bytes in "+(end-start)+" ms");
		}finally{
			out.close();
			temp.delete();
		}
	}
}
