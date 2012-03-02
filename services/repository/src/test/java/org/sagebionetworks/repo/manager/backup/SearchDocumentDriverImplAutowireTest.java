package org.sagebionetworks.repo.manager.backup;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.search.Document;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author deflaux
 * 
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class SearchDocumentDriverImplAutowireTest {

	@Autowired
	SearchDocumentDriver searchDocumentDriver;

	/**
	 * @throws Exception
	 */
	@Before
	public void before() throws Exception {
	}

	/**
	 * 
	 */
	@After
	public void after() {
	}

	/**
	 * @throws Exception
	 */
	@Test
	public void testWriteAllSearchDocuments() throws Exception {

		File destination = File.createTempFile("foo", ".txt");
		destination.deleteOnExit();

		destination.createNewFile();
		searchDocumentDriver.writeSearchDocument(destination, new Progress(),
				null);
		assertTrue(256 < destination.length());
		String serializedSearchDocuments = readFile(destination);
		System.out.println("FIX ME: " + serializedSearchDocuments);
		JSONArray searchDocuments = new JSONArray(serializedSearchDocuments);
		assertTrue(0 < searchDocuments.length());
		JSONObject searchDocument = searchDocuments.getJSONObject(0);
		Document document = EntityFactory.createEntityFromJSONObject(searchDocument, Document.class);
		assertNotNull(document);
	}
	
	// http://stackoverflow.com/questions/326390/how-to-create-a-java-string-from-the-contents-of-a-file
	private static String readFile(File file) throws IOException {
		  FileInputStream stream = new FileInputStream(file);
		  try {
		    FileChannel fc = stream.getChannel();
		    MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());
		    /* Instead of using default, pass in a decoder. */
		    return Charset.forName("UTF-8").decode(bb).toString();
		  }
		  finally {
		    stream.close();
		  }
		}

}
