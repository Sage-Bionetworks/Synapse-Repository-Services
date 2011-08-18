package org.sagebionetworks.repo;

import static org.junit.Assert.*;

import java.io.IOException;

import org.junit.Test;

public class CRUDWikiGeneratorTest {
	
	@Test
	public void testLoadFile() throws IOException{
		String fromFile = CRUDWikiGenerator.loadStaticContentFromClasspath("VersionStaticContent.txt");
		assertNotNull(fromFile);
		System.out.println(fromFile);
	}

}
