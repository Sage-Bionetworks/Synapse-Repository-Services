package org.sagebionetworks.search;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

public class CloudSearchDocumentBatchImpl implements CloudSearchDocumentBatch{
	@Override
	public long size() {
		return 0;
	}

	@Override
	public InputStream getInputStream() {
		return null;
	}

	@Override
	public Set<String> getDocumentIds() {
		return null;
	}

	@Override
	public void close() throws IOException {

	}
}
