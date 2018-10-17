package org.sagebionetworks.search;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;

import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.repo.model.search.Document;
import org.sagebionetworks.repo.model.search.DocumentFields;
import org.sagebionetworks.repo.model.search.DocumentTypeNames;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;

public class CloudSearchDocumentFileIteratorTest {

	private static List<File> filesToDelete = new LinkedList<>();

	Document document1;
	Document document2;
	Document documentLarge;

	int maxSingleDocumentSizeInBytes;
	int maxDocumentBatchSizeInBytes;

	CloudSearchDocumentFileIterator iterator;

	@Before
	public void setUp(){
		document1 = new Document();
		document1.setId("syn123");
		document1.setType(DocumentTypeNames.add);
		document1.setFields(new DocumentFields());

		document2 = new Document();
		document2.setId("syn456");
		document2.setType(DocumentTypeNames.add);
		document2.setFields(new DocumentFields());

		//document that must fit in its own page
		documentLarge = new Document();
		documentLarge.setId("syn789");
		documentLarge.setType(DocumentTypeNames.add);
		documentLarge.setFields(new DocumentFields());

		//make documentLarge "large" enough that it needs to be in a file by itself
		documentLarge.getFields().setDescription(StringUtils.repeat("text", 10));
		int documentLargeSize = byteSizeOfDocument(documentLarge);
		maxSingleDocumentSizeInBytes = documentLargeSize;
		maxDocumentBatchSizeInBytes = documentLargeSize
				+ CloudSearchDocumentFileIterator.PREFIX_BYTES.length
				+ CloudSearchDocumentFileIterator.SUFFIX_BYTES.length;
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
		iterator = new CloudSearchDocumentFileIterator(Arrays.asList(document1).iterator(), maxSingleDocumentSizeInBytes, maxDocumentBatchSizeInBytes);
	}

	@Test
	public void testHasNext_Idempotence(){
		iterator = new CloudSearchDocumentFileIterator(Arrays.asList(document1).iterator(), maxSingleDocumentSizeInBytes, maxDocumentBatchSizeInBytes);
		//check twice
		assertTrue(iterator.hasNext());
		assertTrue(iterator.hasNext());

		File file = iterator.next();
		filesToDelete.add(file);
		assertNotNull(file);

		//check twice again
		assertFalse(iterator.hasNext());
		assertFalse(iterator.hasNext());
	}

	@Test
	public void testNext_NoMoreElements(){
		iterator = new CloudSearchDocumentFileIterator(Arrays.asList(document1).iterator(), maxSingleDocumentSizeInBytes, maxDocumentBatchSizeInBytes);
		assertNotNull(iterator.next());
		try{
			iterator.next();
			fail("expected NoSuchElementException to be thrown");
		} catch (NoSuchElementException e){
			//expected
		}
	}

	@Test (expected = RuntimeException.class)
	public void testIterator_singleDoumentSizeExceeded() throws Exception{
		maxSingleDocumentSizeInBytes = 1;
		iterator = new CloudSearchDocumentFileIterator(Arrays.asList(document1).iterator(), maxSingleDocumentSizeInBytes, maxDocumentBatchSizeInBytes);
		iterator.next();
	}

	@Test
	public void testIterator_multipleFilesLargeFileAtBeginning() throws Exception{
		//test iterator when the large document is at the beginning
		List<Document> documentList = Arrays.asList(documentLarge, document1, document2);
		iterator = new CloudSearchDocumentFileIterator(documentList.iterator(), maxSingleDocumentSizeInBytes, maxDocumentBatchSizeInBytes);
		List<List<Document>> documentsFromFiles = readFromIterator(iterator);
		List<List<Document>> expectedList = Arrays.asList( Arrays.asList(documentLarge), Arrays.asList(document1, document2));
		assertEquals(expectedList, documentsFromFiles);

	}

	@Test
	public void testIterator_multipleFilesLargeFileAtEnd() throws Exception{
		//test iterator when the large document is at the end
		List<Document> documentList = Arrays.asList(document1, document2, documentLarge);
		iterator = new CloudSearchDocumentFileIterator(documentList.iterator(), maxSingleDocumentSizeInBytes, maxDocumentBatchSizeInBytes);
		List<List<Document>> documentsFromFiles = readFromIterator(iterator);
		List<List<Document>> expectedList = Arrays.asList(Arrays.asList(document1, document2), Arrays.asList(documentLarge));
		assertEquals(expectedList, documentsFromFiles);
	}




	private int byteSizeOfDocument(Document document){
		return SearchUtil.convertSearchDocumentToJSONString(document).getBytes(StandardCharsets.UTF_8).length;
	};

	private static List<List<Document>> readFromIterator(Iterator<File> iterator) throws Exception{
		List<List<Document>> documents = new LinkedList<>();
		while(iterator.hasNext()){
			File file = iterator.next();
			filesToDelete.add(file);

			List<Document> nestedDocuments = new LinkedList<>();

			String JsonString = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
			JSONArray jsonArray = new JSONArray(JsonString);
			for(int i = 0; i < jsonArray.length(); i++){
				Document document = EntityFactory.createEntityFromJSONObject(jsonArray.getJSONObject(i), Document.class);
				nestedDocuments.add(document);
			}

			documents.add(nestedDocuments);
		}
		return documents;
	}

}
