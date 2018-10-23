package org.sagebionetworks.search;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;

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
	Document document1;
	Document document2;


	CloudSearchDocumentFileIterator iterator;


	@Before
	public void setUp() throws IOException {
		document1 = new Document();
		document1.setId("syn123");
		document1.setType(DocumentTypeNames.add);
		document1.setFields(new DocumentFields());

		document2 = new Document();
		document2.setId("syn456");
		document2.setType(DocumentTypeNames.add);
		document2.setFields(new DocumentFields());
	}
//
//	@Test (expected = IllegalArgumentException.class)
//	public void testConstructor_singleDocumentSizeLimitPlusDelimitersThanBatchSizeLimit(){
//
//	}
//
//	@Test
//	public void testHasNext_Idempotence(){
//		iterator = new CloudSearchDocumentFileIterator(Arrays.asList(document1).iterator(), maxSingleDocumentSizeInBytes, maxDocumentBatchSizeInBytes);
//		//check twice
//		assertTrue(iterator.hasNext());
//		assertTrue(iterator.hasNext());
//
//		Path file = iterator.next();
//		filesToDelete.add(file);
//		assertNotNull(file);
//
//		//check twice again
//		assertFalse(iterator.hasNext());
//		assertFalse(iterator.hasNext());
//	}
//
//	@Test
//	public void testNext_NoMoreElements(){
//		iterator = new CloudSearchDocumentFileIterator(Arrays.asList(document1).iterator(), maxSingleDocumentSizeInBytes, maxDocumentBatchSizeInBytes);
//		assertNotNull(iterator.next());
//		try{
//			iterator.next();
//			fail("expected NoSuchElementException to be thrown");
//		} catch (NoSuchElementException e){
//			//expected
//		}
//	}

}
