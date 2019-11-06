package org.sagebionetworks.search;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.sagebionetworks.repo.model.search.Document;
import org.sagebionetworks.repo.model.search.DocumentFields;
import org.sagebionetworks.repo.model.search.DocumentTypeNames;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.sagebionetworks.util.FileProvider;

import com.google.common.collect.Sets;

@RunWith(MockitoJUnitRunner.class)
public class CloudSearchDocumentBatchBuilderTest {

	@Mock
	FileProvider mockFileProvider;

	@Mock
	File mockFile;

	ByteArrayOutputStream spyOutputStream;


	CloudSearchDocumentBatchBuilder builder;

	Document document1;
	Document document2;
	Document documentLarge;

	int maxSingleDocumentSizeInBytes;
	int maxDocumentBatchSizeInBytes;

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

		//document that must fit in its own page
		documentLarge = new Document();
		documentLarge.setId("syn789");
		documentLarge.setType(DocumentTypeNames.add);
		documentLarge.setFields(new DocumentFields());
		documentLarge.getFields().setDescription("asdfasdfasdfasdfasdfasdfasdfasdfasdfasdfassdfasdf");

		//make documentLarge "large" enough that it needs to be in a file by itself
		documentLarge.getFields().setDescription(StringUtils.repeat("text", 10));
		int documentLargeSize = byteSizeOfDocument(documentLarge);
		maxSingleDocumentSizeInBytes = documentLargeSize;
		maxDocumentBatchSizeInBytes = documentLargeSize
				+ CloudSearchDocumentBatchBuilder.PREFIX_BYTES.length
				+ CloudSearchDocumentBatchBuilder.SUFFIX_BYTES.length;


		when(mockFileProvider.createTempFile(anyString(), anyString())).thenReturn(mockFile);

		//make sure that all calls to constructor of CloudSearchDocumentBatchBuilder will use a new OutputStream
		when(mockFileProvider.createFileOutputStream(mockFile)).thenAnswer((invocationOnMock)->{
			spyOutputStream = spy(new ByteArrayOutputStream());
			return spyOutputStream;
		});

		builder = new CloudSearchDocumentBatchBuilder(mockFileProvider, maxSingleDocumentSizeInBytes, maxDocumentBatchSizeInBytes);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testConstuctor_singleDocumentSizeExceedsMaxDocumentBatchSize() throws IOException {
		maxDocumentBatchSizeInBytes = 6;
		maxSingleDocumentSizeInBytes = maxDocumentBatchSizeInBytes
				- CloudSearchDocumentBatchBuilder.PREFIX_BYTES.length
				- CloudSearchDocumentBatchBuilder.SUFFIX_BYTES.length
				+ 1;
		new CloudSearchDocumentBatchBuilder(mockFileProvider, maxSingleDocumentSizeInBytes, maxDocumentBatchSizeInBytes);
	}

	//////////////////////////
	// tryAddDocument() Tests
	//////////////////////////

	@Test (expected = IllegalArgumentException.class)
	public void testTryAddDocument_nullDocument() throws Exception{
		builder.tryAddDocument(null);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testTryAddDocument_DocumentHasNullId() throws Exception{
		document1.setId(null);
		builder.tryAddDocument(document1);
	}

	@Test
	public void testTryAddDocument_alreadyBuilt() throws Exception{
		//set really large limits but build the builder already
		builder = new CloudSearchDocumentBatchBuilder(mockFileProvider, 99999999L,9999999999999L);
		builder.tryAddDocument(document1);
		builder.build();

		try {
			builder.tryAddDocument(document1);
			fail("expected exception to be thrown");
		}catch (IllegalStateException e){
			//expected
		}
	}

	//PLFM-5570
	@Test
	public void testTryAddDocument_singleDocumentSizeExceeded() throws Exception {
		maxSingleDocumentSizeInBytes = 1;
		builder = new CloudSearchDocumentBatchBuilder(mockFileProvider, maxSingleDocumentSizeInBytes, maxDocumentBatchSizeInBytes);
		//pretend that we can add documents that are too large but don't actually do it. i.e. ignore files that are too large
		assertTrue(builder.tryAddDocument(document1));
	}

	@Test
	public void testTryAddDocument_multipleFiles_largeFileAddedFirst() throws Exception{
		assertTrue(builder.tryAddDocument(documentLarge));

		assertFalse(builder.tryAddDocument(document1));
		assertFalse(builder.tryAddDocument(document2));

		builder.build();

		List<Document> documentsFromFiles = documentsInOutPutStream();
		List<Document> expectedList = Arrays.asList(documentLarge);
		assertEquals(expectedList, documentsFromFiles);

	}

	@Test
	public void testTryAddDocument_multipleFiles_largeFileAddedLast() throws Exception{
		assertTrue(builder.tryAddDocument(document1));
		assertTrue(builder.tryAddDocument(document2));

		assertFalse(builder.tryAddDocument(documentLarge));

		builder.build();

		List<Document> documentsFromFiles = documentsInOutPutStream();
		List<Document> expectedList = Arrays.asList(document1, document2);
		assertEquals(expectedList, documentsFromFiles);
	}

	/////////////////
	// build() tests
	/////////////////

	@Test
	public void testBuild() throws IOException {
		assertTrue(builder.tryAddDocument(document1));
		assertTrue(builder.tryAddDocument(document2));

		CloudSearchDocumentBatchImpl batch = (CloudSearchDocumentBatchImpl) builder.build();


		assertEquals(CloudSearchDocumentBatchBuilder.PREFIX_BYTES.length
				+ byteSizeOfDocument(document1)
				+ CloudSearchDocumentBatchBuilder.DELIMITER_BYTES.length
				+ byteSizeOfDocument(document2)
				+ CloudSearchDocumentBatchBuilder.SUFFIX_BYTES.length

				, batch.byteSize);

		assertEquals(mockFile, batch.documentBatchFile);
		assertEquals(Sets.newHashSet(document1.getId(), document2.getId()), batch.documentIds);
	}


	@Test
	public void testBuild_multipleCallsToBuild() throws IOException {
		assertTrue(builder.tryAddDocument(document1));
		assertTrue(builder.tryAddDocument(document2));

		builder.build();
		try {
			builder.build();
			fail("expected exception to be thrown");
		} catch (IllegalStateException e){
			//expected
		}
	}

	/////////////////
	// close() Tests
	/////////////////

	@Test
	public void testClose_BeforeBuild_beforeBuildCalled() throws IOException {
		try(CloudSearchDocumentBatchBuilder batchBuilder = new CloudSearchDocumentBatchBuilder(mockFileProvider)){
			batchBuilder.tryAddDocument(document1);
		}

		verify(spyOutputStream).close();
		verify(mockFile).delete();
	}

	@Test
	public void testClose_AfterBuild() throws IOException {
		try(CloudSearchDocumentBatchBuilder batchBuilder = new CloudSearchDocumentBatchBuilder(mockFileProvider)){
			batchBuilder.tryAddDocument(document1);
			batchBuilder.build();
		}

		verify(spyOutputStream).close(); // the close() call happened in build()
		verify(mockFile, never()).delete();
	}

	////////////////
	// test helpers
	////////////////

	private int byteSizeOfDocument(Document document){
		return SearchUtil.convertSearchDocumentToJSONString(document).getBytes(StandardCharsets.UTF_8).length;
	};

	private List<Document> documentsInOutPutStream() throws Exception{
		List<Document> documents = new LinkedList<>();

		String JsonString = new String(spyOutputStream.toByteArray(), CloudSearchDocumentBatchBuilder.CHARSET);
		JSONArray jsonArray = new JSONArray(JsonString);
		for(int i = 0; i < jsonArray.length(); i++){
			Document document = EntityFactory.createEntityFromJSONObject(jsonArray.getJSONObject(i), Document.class);
			documents.add(document);
		}

		return documents;
	}

}
