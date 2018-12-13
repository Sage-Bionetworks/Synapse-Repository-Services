package org.sagebionetworks.search;

import java.io.IOException;

import org.sagebionetworks.util.FileProvider;
import org.springframework.beans.factory.annotation.Autowired;

public class CloudSearchDocumentBuilderProvider {
	@Autowired
	FileProvider fileProvider;

	CloudSearchDocumentBatchBuilder getBuilder() throws IOException {
		return new CloudSearchDocumentBatchBuilder(fileProvider);
	}
}
