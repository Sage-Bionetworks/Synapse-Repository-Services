package org.sagebionetworks.search;

import java.util.Iterator;

import org.sagebionetworks.repo.model.search.Document;
import org.springframework.beans.factory.annotation.Autowired;

public class CloudSearchDocumentBatchIteratorProvider {
	@Autowired
	CloudSearchDocumentBuilderProvider documentBuilderProvider;

	Iterator<CloudSearchDocumentBatch> getIterator(Iterator<Document> documentIterator){
		return new CloudSearchDocumentBatchIterator(documentIterator, documentBuilderProvider);
	}
}
