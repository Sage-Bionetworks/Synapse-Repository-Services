package org.sagebionetworks.downloadtools;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;


/**
 * 
 * Downloads the tar file at 'url' and puts the contained files into directory 'dir'
 * If any contained file ends in 'gz', 'zip' or 'gzip' it tries to unzip the file.
 * 
 */
public class TARDownloader {
	
	private static int RECORDLEN = 512, NAMEOFF = 0, NAMELEN = 100, SIZEOFF = 124, SIZELEN = 12; //, MTIMEOFF = 136, MTIMELEN = 12; 

	/**
	 * Downloads the tar file at 'url' and puts the contained files into directory 'dir'
	 * If any contained file ends in 'gz', 'zip' or 'gzip' it tries to unzip the file.
	 * 
	 * TODO:  A possible extension is to handle TAR files which are themselves zipped.
	 */
	public static List<File> download(URL url, File dir) throws IOException {
		if (!dir.exists()) throw new IOException(dir.getPath()+" does not exist.");
		byte [] buffer = new byte[RECORDLEN]; 
		URLConnection conn = (URLConnection)url.openConnection();
		InputStream is = conn.getInputStream();
		List<File> fileList = new ArrayList<File>();
		while ( ((is.read(buffer)) == RECORDLEN) && (buffer[NAMEOFF] != 0) ) { 
			String name = new String(buffer, NAMEOFF, NAMELEN, Charset.defaultCharset()).trim(); 
			String s = new String(buffer, SIZEOFF, SIZELEN, Charset.defaultCharset()).trim(); 
			int size = Integer.parseInt(s, 8); 
			int padding = (size%RECORDLEN==0?0:RECORDLEN-size%RECORDLEN);
			if (isZipFile(name)) {
				// we pass just the section of the TAR file for the current entry
				// to the ZipInputStream
				ZipInputStream zis = new ZipInputStream(new InternalInputStream(is, size));
				ZipEntry entry = zis.getNextEntry();
				while (entry!=null) {
					String entryName = entry.getName();
					File file = new File(dir, entryName);
					if (file.exists()) throw new IOException(file.getPath()+" already exists.");
					FileOutputStream fos = new FileOutputStream(file); 
					io(zis, fos);
					fos.close();
					fileList.add(file);
					entry = zis.getNextEntry();
				}
			} else if (isGZipFile(name)) {
				// we pass just the section of the TAR file for the current entry
				// to the GZIPInputStream
				GZIPInputStream zis = new GZIPInputStream(new InternalInputStream(is, size));
				File file = new File(dir, name.substring(0, name.length()-gzipSuffix(name).length()));
				if (file.exists()) throw new IOException(file.getPath()+" already exists.");
				FileOutputStream fos = new FileOutputStream(file); 
				io(zis, fos);
				fos.close();
				fileList.add(file);
			} else {
				// not gzipped or zipped
				InputStream iis = new InternalInputStream(is, size);
				File file = new File(dir, name);
				if (file.exists()) throw new IOException(file.getPath()+" already exists.");
				FileOutputStream fos = new FileOutputStream(file); 
				io(iis, fos);
				fos.close();
				fileList.add(file);
			}
			{
				int count = 0;
				while (count<padding) {
					// don't always skip the requested distance, so may have to retry
					long skipped = is.skip(padding-count);
					count += skipped; 
				}
			}
		} 
		return fileList;
	}
	
	// copy from the input stream to the output stream until nothing's left
	private static void io(InputStream is, OutputStream os) throws IOException {
		byte[] buffer = new byte[RECORDLEN];
		while (true) {
			int n=is.read(buffer);
			if (n<=0) break;
			os.write(buffer, 0, n);
		}
	}
	
	// must be lower case
	private static final String[] zipSuffixes = {".zip"};
	private static final String[] gzipSuffixes = {".gz", ".gzip"};
	
	private static boolean isZipFile(String name) {
		name = name.toLowerCase();
		for (String z : zipSuffixes) if (name.endsWith(z)) return true;
		return false;
	}
	 
	private static boolean isGZipFile(String name) {
		name = name.toLowerCase();
		for (String z : gzipSuffixes) if (name.endsWith(z)) return true;
		return false;
	}
	 
	private static String gzipSuffix(String name) {
		name = name.toLowerCase();
		for (String z : gzipSuffixes) if (name.endsWith(z)) return z;
		return null;
	}
	 
}

