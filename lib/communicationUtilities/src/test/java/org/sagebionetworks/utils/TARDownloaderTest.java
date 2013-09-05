package org.sagebionetworks.utils;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.InputStream;
import java.util.List;

import org.junit.Test;
import org.sagebionetworks.downloadtools.TARDownloader;

public class TARDownloaderTest {

	@Test
	public void testUntar() throws Exception {
		File dir = new File(".");
		InputStream is = TARDownloaderTest.class.getClassLoader().getResourceAsStream("test.tar");
		List<File> files = TARDownloader.untar(is, dir);
		is.close();
		assertTrue(files.size()>0);
		for (File file : files) file.delete();
	}
}
