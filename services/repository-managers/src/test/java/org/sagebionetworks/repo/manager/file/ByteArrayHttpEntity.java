package org.sagebionetworks.repo.manager.file;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.http.entity.AbstractHttpEntity;

/**
 * A very simple http entity to test uploading raw bytes.
 * 
 * @author John
 * 
 */
public class ByteArrayHttpEntity extends AbstractHttpEntity {
	byte[] content;
	long length;

	public ByteArrayHttpEntity(byte[] content, long length) {
		this.content = content;
		this.length = length;
	}

	@Override
	public boolean isRepeatable() {
		return true;
	}

	@Override
	public long getContentLength() {
		return length;
	}

	@Override
	public InputStream getContent() throws IOException, IllegalStateException {
		return new ByteArrayInputStream(this.content, 0, (int) length);
	}

	@Override
	public void writeTo(OutputStream outstream) throws IOException {
		outstream.write(content, 0, (int) length);
		outstream.flush();
	}

	@Override
	public boolean isStreaming() {
		return false;
	}

	/**
	 * Fill the passed buffer from the passed input stream.
	 * 
	 * @param buffer
	 * @param in
	 * @return the number of bytes written to the buffer.
	 * @throws IOException
	 */
	public static int fillBufferFromStream(byte[] buffer, InputStream in)throws IOException {
		int totalRead = 0;
		int read;
		while ((read = in.read(buffer, totalRead, buffer.length - totalRead)) > 0) {
			totalRead += read;
		}
		return totalRead;
	}

}
