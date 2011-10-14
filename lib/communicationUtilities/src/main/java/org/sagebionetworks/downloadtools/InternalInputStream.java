package org.sagebionetworks.downloadtools;

import java.io.IOException;
import java.io.InputStream;

/**
 * This class provides an input stream which is a subset of another stream
 * ending at a given position in the stream
 */
public class InternalInputStream extends InputStream {
	private InputStream is;
	private long len;
	private long p=0;
	
	/**
	 * @param is the original input stream
	 * @param len the number of bytes that can be read, at which point
	 * the end of the stream is deemed to have been reached
	 */
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

