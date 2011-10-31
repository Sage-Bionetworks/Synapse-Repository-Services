package org.sagebionetworks.utils;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.List;

import org.junit.Test;
import org.sagebionetworks.downloadtools.TARDownloader;

public class TARDownloaderTest {

	@Test
	public void testTARDownloader() throws Exception {
		String studyID = "GSE12800";
		String fileName = studyID+"_RAW.tar";
		String ftpServer = "ftp.ncbi.nih.gov";
		File remoteFile = new File("/pub/geo/DATA/supplementary/series/"+studyID, fileName);
		File dir = new File(".");
		List<File> files = TARDownloader.ftpDownload(ftpServer, remoteFile, dir);
		assertTrue(files.size()>0);
		for (File file : files) file.delete();
	}
}
