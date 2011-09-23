package org.sagebionetworks.repopipeline;

import java.io.File;
import java.io.FilenameFilter;
import java.net.URL;

import org.junit.Ignore;
import org.junit.Test;


public class CelReaderTest {

	@Ignore
	@Test
	public void testReadCelFile() throws Exception {
		File dir = new File(".");
		String[] files = dir.list(new FilenameFilter() {
			public boolean accept(File f, String s) {
				return (s.toLowerCase().endsWith(".cel"));
			}
		});
		for (String f : files)  {
			CelInfo ci = CelReader.readCel(f);
			System.out.println(f+" scan date: "+ci.getScanDate()+" platform: "+ci.getPlatform());
		}
	}
	
	@Ignore
	@Test
	public void testTarReader() throws Exception {
		String studyID = "GSE1000";
		String fileName = studyID+"_RAW.tar";
		URL url = new URL("ftp://ftp.ncbi.nih.gov/pub/geo/DATA/supplementary/series/"+studyID+"/"+fileName);
		System.out.println(
				//TARDownloader.download(url, new File("."))
		);
	}
	

}
