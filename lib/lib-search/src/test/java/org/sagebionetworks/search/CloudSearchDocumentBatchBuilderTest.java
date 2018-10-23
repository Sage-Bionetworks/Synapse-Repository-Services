package org.sagebionetworks.search;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import org.sagebionetworks.repo.model.search.Document;
import org.sagebionetworks.repo.model.search.DocumentFields;
import org.sagebionetworks.repo.model.search.DocumentTypeNames;
import org.sagebionetworks.util.FileProvider;

@RunWith(MockitoJUnitRunner.class)
public class CloudSearchDocumentBatchBuilderTest {

	@Mock
	FileProvider mockFileProvider;

	@Mock
	File mockFile;

	@Spy
	OutputStream spyOutputStream = new ByteArrayOutputStream();


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

		//make documentLarge "large" enough that it needs to be in a file by itself
		documentLarge.getFields().setDescription(StringUtils.repeat("text", 10));
		int documentLargeSize = byteSizeOfDocument(documentLarge);
		maxSingleDocumentSizeInBytes = documentLargeSize;
		maxDocumentBatchSizeInBytes = documentLargeSize
				+ CloudSearchDocumentBatchBuilder.PREFIX_BYTES.length
				+ CloudSearchDocumentBatchBuilder.SUFFIX_BYTES.length;


		when(mockFileProvider.createTempFile(anyString(), anyString())).thenReturn(mockFile);
		when(mockFileProvider.createFileOutputStream(mockFile)).thenReturn((FileOutputStream) spyOutputStream);
	}


	private int byteSizeOfDocument(Document document){
		return SearchUtil.convertSearchDocumentToJSONString(document).getBytes(StandardCharsets.UTF_8).length;
	};

	@Test
	public void testThing(){

	}

	private static String readFromOutputStream(){
		return null;
	}

}
