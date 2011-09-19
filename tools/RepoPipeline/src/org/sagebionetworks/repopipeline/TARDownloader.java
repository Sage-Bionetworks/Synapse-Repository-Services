package org.sagebionetworks.repopipeline;

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
 */
public class TARDownloader {
	
	static int RECORDLEN = 512, NAMEOFF = 0, NAMELEN = 100, SIZEOFF = 124, SIZELEN = 12, MTIMEOFF = 136, MTIMELEN = 12; 

	/**
	 * Downloads the tar file at 'url' and puts the contained files into directory 'dir'
	 * If any contained file ends in 'gz', 'zip' or 'gzip' it tries to unzip the file.
	 */
	public static List<File> download(URL url, File dir) throws IOException {
		if (!dir.exists()) throw new IOException(dir.getPath()+" does not exist.");
		byte [] buffer = new byte[RECORDLEN]; 
		URLConnection conn = (URLConnection)url.openConnection();
		InputStream is = conn.getInputStream();
		List<File> fileList = new ArrayList<File>();
//		long actual=0L;
		while ( ((is.read(buffer)) == RECORDLEN) && (buffer[NAMEOFF] != 0) ) { 
			String name = new String(buffer, NAMEOFF, NAMELEN, Charset.defaultCharset()).trim(); 
			String s = new String(buffer, SIZEOFF, SIZELEN, Charset.defaultCharset()).trim(); 
			int size = Integer.parseInt(s, 8); 
			int padding = (size%RECORDLEN==0?0:RECORDLEN-size%RECORDLEN);
//			System.out.println("entry will be padded by "+padding);
//			s = new String(buffer, MTIMEOFF, MTIMELEN, Charset.defaultCharset()).trim(); 
//			long l = Integer.parseInt(s, 8); 
//			Date mtime = new Date( l*1000 ); 
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
				File file = new File(dir, name);
				if (file.exists()) throw new IOException(file.getPath()+" already exists.");
				FileOutputStream fos = new FileOutputStream(file); 
				io(is, fos);
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

class InternalInputStream extends InputStream {
	private InputStream is;
	private long len;
	private long p=0;
	public InternalInputStream(InputStream is, long len) {
		this.is=is;
		this.len=len;
		p=0L;
	}
	/**
	 * @return
	 * @throws IOException
	 * @see java.io.InputStream#read()
	 */
	public int read() throws IOException {
		if (p>=len) return -1;
		int i = is.read();
		p++;
		return i;
	}
	/**
	 * @param b
	 * @return
	 * @throws IOException
	 * @see java.io.InputStream#read(byte[])
	 */
	public int read(byte[] b) throws IOException {
		if (p>=len) return -1;
		byte[] bInt = null;
		if (b.length<=len-p) {
			bInt = b;
		} else {
			bInt = new byte[(int)(len-p)];
		}
		int i = is.read(bInt);
		p += i;
		if (b.length>len-p) {
			System.arraycopy(bInt, 0, b, 0, i);
		}
		return i;
	}
	/**
	 * @param b
	 * @param off
	 * @param len
	 * @return
	 * @throws IOException
	 * @see java.io.InputStream#read(byte[], int, int)
	 */
	public int read(byte[] b, int off, int l) throws IOException {
		l = Math.min(l, (int)(len-p));
		int i = is.read(b, off, l);
		p += i;
		return i;
	}
	/**
	 * @param n
	 * @return
	 * @throws IOException
	 * @see java.io.InputStream#skip(long)
	 */
	public long skip(long n) throws IOException {
		long i = is.skip(Math.min(n, (int)(len-p)));
		p += i;
		return i;
	}
	/**
	 * @return
	 * @throws IOException
	 * @see java.io.InputStream#available()
	 */
	public int available() throws IOException {
		int i = is.available();
		i = Math.min(i, (int)(len-p));
		return i;
	}
	/**
	 * @throws IOException
	 * @see java.io.InputStream#close()
	 */
	public void close() throws IOException {
		//is.close();
	}
	/**
	 * @param readlimit
	 * @see java.io.InputStream#mark(int)
	 */
	public void mark(int readlimit) {
		is.mark(readlimit);
	}
	/**
	 * @throws IOException
	 * @see java.io.InputStream#reset()
	 */
	public void reset() throws IOException {
		is.reset();
	}
	/**
	 * @return
	 * @see java.io.InputStream#markSupported()
	 */
	public boolean markSupported() {
		return is.markSupported();
	}
	
}
