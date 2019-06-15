package org.sagebionetworks.search;

import java.io.Closeable;
import java.io.InputStream;
import java.util.Set;

public interface CloudSearchDocumentBatch extends Closeable {

	/**
	 * Returns the content size in bytes of this document batch.
	 * @return
	 */
	public long size();

	/**
	 * Returns an inputStream for the contents of this batch.
	 * @return
	 */
	public InputStream getNewInputStream();

	/**
	 * Set CloudSearch Document IDs included in this batch.
	 * @return
	 */
	public Set<String> getDocumentIds();
}
