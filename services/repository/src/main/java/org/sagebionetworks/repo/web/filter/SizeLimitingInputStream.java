package org.sagebionetworks.repo.web.filter;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.input.ProxyInputStream;


/**
 * ProxyInputStream that will enforce the maximum number of bytes that can be read from the stream.
 *
 */
public class SizeLimitingInputStream extends ProxyInputStream {
	
	long maximumBytes;
	long byteCount;

	public SizeLimitingInputStream(InputStream proxy, long maximumBytes) {
		super(proxy);
		this.maximumBytes = maximumBytes;
		this.byteCount = 0;
	}
	
    public int read(byte[] b) throws IOException {
        int read = super.read(b);
        bytesRead(read);
        return read;
    }

    public int read(byte[] b, int off, int len) throws IOException {
        int read = super.read(b, off, len);
        bytesRead(read);
        return read;
    }

    public int read() throws IOException {
        int read = super.read();
        bytesRead(read);
        return read;
    }

    public long skip(final long length) throws IOException {
        final long skip = super.skip(length);
        bytesRead(skip);
        return skip;
    }
    
    private void bytesRead(long bytesRead) {
    	if(bytesRead > 0) {
    		this.byteCount += bytesRead;
    	}
    	if(this.byteCount > maximumBytes) {
    		throw new IllegalArgumentException("Request size exceeded maximum number of bytes: "+maximumBytes);
    	}
    }

}
