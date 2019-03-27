package org.sagebionetworks.search;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.sagebionetworks.repo.model.search.Document;
import org.sagebionetworks.repo.model.search.DocumentFields;
import org.sagebionetworks.repo.model.search.DocumentTypeNames;

@RunWith(MockitoJUnitRunner.class)
public class CloudSearchDocumentBatchIteratorTest {
	Document document1;
	Document document2;

	@Mock
	CloudSearchDocumentBuilderProvider mockBuiderProvider;

	@Mock
	CloudSearchDocumentBatchBuilder mockBuilder;
	@Mock
	CloudSearchDocumentBatchBuilder mockBuilder2;

	@Mock
	Iterator<Document> mockDocumentIterator;

	CloudSearchDocumentBatchIterator spyCloudSearchDocumentBatchIterator;

	CloudSearchDocumentBatch documentBatch;
	CloudSearchDocumentBatch documentBatch2;


	List<Document> documentList;
	Iterator<Document> documentListIterator;

	@Before
	public void setUp() throws IOException {
		spyCloudSearchDocumentBatchIterator = spy(new CloudSearchDocumentBatchIterator(mockDocumentIterator, mockBuiderProvider));

		document1 = new Document();
		document1.setId("syn123");
		document1.setType(DocumentTypeNames.add);
		document1.setFields(new DocumentFields());

		document2 = new Document();
		document2.setId("syn456");
		document2.setType(DocumentTypeNames.add);
		document2.setFields(new DocumentFields());

		documentList = Arrays.asList(document1, document2);

		documentBatch = new CloudSearchDocumentBatchImpl(new File("fake/path"), Collections.emptySet(), 42);
		documentBatch2 = new CloudSearchDocumentBatchImpl(new File("fake/path2"), Collections.emptySet(), 24);

		when(mockBuilder.build()).thenReturn(documentBatch);
		when(mockBuilder2.build()).thenReturn(documentBatch2);

		when(mockBuiderProvider.getBuilder()).thenReturn(mockBuilder, mockBuilder2);



		//mockDocumentIterator's values are based off of the values stored in documentList and the state of documentList Iterator;
		when(mockDocumentIterator.next()).thenAnswer((invocationOnMock -> {
			if(documentListIterator == null){
				documentListIterator = documentList.iterator();
			}
			return documentListIterator.next();
		}));
		when(mockDocumentIterator.hasNext()).thenAnswer((invocationOnMock -> {
			if(documentListIterator == null){
				documentListIterator = documentList.iterator();
			}
			return documentListIterator.hasNext();
		}));
	}


	@Test
	public void testHasNext_Idempotence_whenTrue() throws IOException {
		doReturn(documentBatch) // return real value
				.doReturn(null) // then return null
				.when(spyCloudSearchDocumentBatchIterator).processDocumentFile();

		//check twice
		assertTrue(spyCloudSearchDocumentBatchIterator.hasNext());
		assertTrue(spyCloudSearchDocumentBatchIterator.hasNext());
	}


	@Test
	public void testHasNext_noMore() throws IOException {
		doReturn(null)
		.when(spyCloudSearchDocumentBatchIterator).processDocumentFile();

		//check twice
		assertFalse(spyCloudSearchDocumentBatchIterator.hasNext());
		assertFalse(spyCloudSearchDocumentBatchIterator.hasNext());
	}

	@Test
	public void testNext_hasElements() throws IOException {
		doReturn(documentBatch).when(spyCloudSearchDocumentBatchIterator).processDocumentFile();

		CloudSearchDocumentBatch batch = spyCloudSearchDocumentBatchIterator.next();
		assertEquals(documentBatch, batch);
	}

	@Test
	public void testNext_NoMoreElements(){
		doReturn(false).when(spyCloudSearchDocumentBatchIterator).hasNext();
		try{
			spyCloudSearchDocumentBatchIterator.next();
			fail("expected NoSuchElementException to be thrown");
		} catch (NoSuchElementException e){
			//expected
		}
	}

	@Test
	public void testProcessDocumentFile_documenIteratorisExhausted_noUnwrittenDocuments() throws IOException {
		when(mockDocumentIterator.hasNext()).thenReturn(false);

		assertNull(spyCloudSearchDocumentBatchIterator.processDocumentFile());
		verify(mockDocumentIterator).hasNext();
	}

	@Test
	public void testProcessDocumentFile_documentsAddedToSingleBuilder() throws IOException {
		when(mockBuilder.tryAddDocument(any())).thenReturn(true);

		assertEquals(documentBatch, spyCloudSearchDocumentBatchIterator.processDocumentFile());

		verify(mockBuilder).tryAddDocument(document1);
		verify(mockBuilder).tryAddDocument(document2);
		verify(mockBuilder).build();
	}

	@Test
	public void testProcessDocumentFile_LastDocumentNotFitInPreviousBuilder() throws IOException {
		//first builder will only accept document1
		when(mockBuilder.tryAddDocument(document1)).thenReturn(true);
		when(mockBuilder.tryAddDocument(document2)).thenReturn(false);

		when(mockBuilder2.tryAddDocument(document2)).thenReturn(true);

		//method under test
		assertEquals(documentBatch, spyCloudSearchDocumentBatchIterator.processDocumentFile());
		//method under test again
		assertEquals(documentBatch2, spyCloudSearchDocumentBatchIterator.processDocumentFile());


		verify(mockBuilder).tryAddDocument(document1);
		verify(mockBuilder).tryAddDocument(document2);
		verify(mockBuilder).build();
		verify(mockBuilder).close();
		verifyNoMoreInteractions(mockBuilder);

		verify(mockBuilder2).tryAddDocument(document2);
		verify(mockBuilder2).build();
		verify(mockBuilder2).close();
		verifyNoMoreInteractions(mockBuilder);


		verify(mockBuiderProvider, times(2)).getBuilder();
	}


	@Test
	public void testProcessDocumentFile_MiddleDocumentMustBeAddedToBuilder() throws IOException {
		Document document3 = new Document();
		document3.setId("doc3");
		documentList = Arrays.asList(document1, document2, document3);

		//first builder will only accept document1
		when(mockBuilder.tryAddDocument(document1)).thenReturn(true);
		when(mockBuilder.tryAddDocument(document2)).thenReturn(false);

		//second builder will accept document 2 and 3
		when(mockBuilder2.tryAddDocument(document2)).thenReturn(true);
		when(mockBuilder2.tryAddDocument(document3)).thenReturn(true);

		//method under test
		assertEquals(documentBatch, spyCloudSearchDocumentBatchIterator.processDocumentFile());
		//method under test again
		assertEquals(documentBatch2, spyCloudSearchDocumentBatchIterator.processDocumentFile());

		verify(mockBuiderProvider, times(2)).getBuilder();

		verify(mockBuilder).tryAddDocument(document1);
		verify(mockBuilder).tryAddDocument(document2);
		verify(mockBuilder).build();
		verify(mockBuilder).close();
		verifyNoMoreInteractions(mockBuilder);

		verify(mockBuilder2).tryAddDocument(document2);
		verify(mockBuilder2).tryAddDocument(document3);
		verify(mockBuilder2).build();
		verify(mockBuilder2).close();
		verifyNoMoreInteractions(mockBuilder2);
	}

}
