package org.sagebionetworks.repo.manager.file.preview;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.repo.web.ServiceUnavailableException;

public class ZipPreviewTest {
	ZipPreviewGenerator zipPreviewGenerator;
	
	@Before
	public void before() throws IOException, ServiceUnavailableException{
		zipPreviewGenerator = new ZipPreviewGenerator();
	}
	
	@Test
	public void testGeneratePreview() throws IOException {
		// create a zip file.
		File f = File.createTempFile("ZipPreview", ".zip");
		try {
			StringBuilder sb = new StringBuilder();
			sb.append("Test String");

			final ZipOutputStream out = new ZipOutputStream(
					new FileOutputStream(f));
			for (int i = 0; i < 10; i++) {
				// write 10 entries
				ZipEntry e = new ZipEntry("textFile" + i + ".txt");
				out.putNextEntry(e);
				byte[] data = sb.toString().getBytes();
				out.write(data, 0, data.length);
				out.closeEntry();
			}
			out.close();
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			InputStream from = new FileInputStream(f);
			String type = zipPreviewGenerator.generatePreview(from, baos);
			assertEquals(ZipPreviewGenerator.TEXT_CSV, type);
			String actual = baos.toString();
			String expected = "textFile0.txt\ntextFile1.txt\ntextFile2.txt\ntextFile3.txt\ntextFile4.txt\ntextFile5.txt\ntextFile6.txt\ntextFile7.txt\ntextFile8.txt\ntextFile9.txt\n";
			assertEquals(expected, actual);
		} finally {
			f.delete();
		}
	}

}
