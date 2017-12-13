package org.sagebionetworks.repo.web.filter;

import java.io.IOException;

import javax.servlet.ServletInputStream;

/**
 * ServletInputStream proxy that enforces a maximum number of bytes that can be read from the stream.
 * 
 * This class overrides all methods of ServletInputStream and InputStream to pass the calls to the
 * wrapped stream.
 *
 */
public class ThrottlingProxyInputStream extends ServletInputStream {
	
    /**
     * This class is a proxy that wraps this stream.
     */
    protected volatile ServletInputStream wrapped;
    protected volatile long totalBytesRead;
    protected volatile long maxAllowedBytes;


	public ThrottlingProxyInputStream(ServletInputStream toWrap, long maximumInputStreamBytes) {
		this.wrapped = toWrap;
		this.maxAllowedBytes = maximumInputStreamBytes;
		this.totalBytesRead = 0;
	}

	@Override
	public int read() throws IOException {
		// All input stream implementations must implement this method, so this is where the size check occurs.
		int read = wrapped.read();
		if(read > -1) {
			// one byte was read.
			bytesRead(1);
		}
		return read;
	}
	
	@Override
	public int readLine(byte[] b, int off, int len) throws IOException {
		int read = wrapped.readLine(b,off,len);
		bytesRead(read);
		return read;
	}

	@Override
	public int read(byte[] b) throws IOException {
		int read = wrapped.read(b);
		bytesRead(read);
		return read;
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		int read = wrapped.read(b, off, len);
		bytesRead(read);
		return read;
	}

	@Override
	public long skip(long n) throws IOException {
		return wrapped.skip(n);
	}

	@Override
	public int available() throws IOException {
		return wrapped.available();
	}

	@Override
	public void close() throws IOException {
		wrapped.close();
	}

	@Override
	public synchronized void mark(int readlimit) {
		wrapped.mark(readlimit);
	}

	@Override
	public synchronized void reset() throws IOException {
		wrapped.reset();
	}

	@Override
	public boolean markSupported() {
		return wrapped.markSupported();
	}

	/**
	 * Called whenever bytes are read from the stream.
	 * @param bytesRead
	 */
	private void bytesRead(int bytesRead) {
		if(bytesRead > -1) {
			totalBytesRead += bytesRead;
		}
		if(totalBytesRead > maxAllowedBytes) {
			throw new ByteLimitExceededException("Request size exceeded maximum number of bytes: "+maxAllowedBytes);
		}
	}
}
