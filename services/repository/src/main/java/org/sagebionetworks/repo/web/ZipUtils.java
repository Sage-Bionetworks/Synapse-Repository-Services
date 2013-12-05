package org.sagebionetworks.repo.web;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class ZipUtils {
	/**
	 * Zip up content into a compressed gz file
	 */
	public static File writeStringToCompressedFile(String content) throws IOException {
		File temp = File.createTempFile("compressed", ".txt.gz");
		FileOutputStream fos = new FileOutputStream(temp);
		GZIPOutputStream gzout = null;
		OutputStreamWriter out = null;
		try {
			gzout = new GZIPOutputStream(fos);
			out = new OutputStreamWriter(gzout, "UTF-8");
			out.append(content);
			out.flush();
			out.close();
		} finally {
			fos.close();
		}
		return temp;
	}
	
	/**
	 * Read compressed data from file as a string.
	 */
	public static String readCompressedFileAsString(File file) throws IOException {
		FileInputStream fis = new FileInputStream(file);
		GZIPInputStream gzin = new GZIPInputStream(fis);
		BufferedInputStream bis = new BufferedInputStream(gzin);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		
		try {
			byte[] buffer = new byte[1024];
			int read = -1;
			while((read = bis.read(buffer)) > 0){
				baos.write(buffer, 0, read);
			}
		} finally {
			baos.close();
			bis.close();
			gzin.close();
			fis.close();
		}
		String fromZip = new String(baos.toByteArray(), "UTF-8");
		return fromZip;
	}
}
