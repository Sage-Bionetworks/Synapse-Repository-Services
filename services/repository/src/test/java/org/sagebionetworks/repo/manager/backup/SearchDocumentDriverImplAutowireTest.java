package org.sagebionetworks.repo.manager.backup;

import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
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
	NodeBackupDriver searchDriver;
	
	/**
	 * @throws Exception
	 */
	@Before
	public void before() throws Exception{
	}
	
	/**
	 * 
	 */
	@After
	public void after(){
	}
	
	/**
	 * @throws Exception
	 */
	@Test
	public void testWriteAllSearchDocuments() throws Exception{
		
		// uncomment this and comment the other two lines to write out an actual file to upload to CloudSearch		
//		File destination = new File("/Users/deflaux/CloudSearch/searchDocuments/sanityCheck.json");
		File destination = File.createTempFile("foo", ".txt");
		destination.deleteOnExit();

		destination.createNewFile();
		searchDriver.writeBackup(destination, new Progress(), null);
		assertTrue(256 < destination.length());
	}

}
