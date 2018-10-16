package org.sagebionetworks.search;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.sagebionetworks.repo.model.search.Document;
import org.sagebionetworks.repo.model.search.DocumentFields;
import org.sagebionetworks.repo.model.search.DocumentTypeNames;

public class CloudSearchDocumentFileIteratorTest {

	private static List<File> filesToDelete = new LinkedList<>();

	List<Document> documentList;

	Document document;
	Document document2;

	int maxSingleDocumentSizeInBytes;
	int maxDocumentBatchSizeInBytes;

	CloudSearchDocumentFileIterator iterator;

	@Before
	public void setUp(){
		document = new Document();
		document.setId("syn123");
		document.setType(DocumentTypeNames.add);
		document.setFields(new DocumentFields());

		document2 = new Document();
		document2.setId("syn456");
		document2.setType(DocumentTypeNames.add);
		document2.setFields(new DocumentFields());

		documentList = Arrays.asList(document, document2);


	}

	@AfterClass
	public static void cleanUpClass(){
		for(File file : filesToDelete){
			file.delete();
		}
	}

	@Test (expected = IllegalArgumentException.class)
	public void testConstructor_singleDocumentSizeLimitPlusDelimitersThanBatchSizeLimit(){
		maxDocumentBatchSizeInBytes = 6;
		maxSingleDocumentSizeInBytes = maxDocumentBatchSizeInBytes
				- CloudSearchDocumentFileIterator.PREFIX_BYTES.length
				- CloudSearchDocumentFileIterator.SUFFIX_BYTES.length
				+ 1;
		iterator = new CloudSearchDocumentFileIterator(documentList.iterator(), maxSingleDocumentSizeInBytes, maxDocumentBatchSizeInBytes);
	}


	private int byteSizeOfDocument(Document document){
		return SearchUtil.convertSearchDocumentToJSONString(document).getBytes(StandardCharsets.UTF_8).length;
	};

}
