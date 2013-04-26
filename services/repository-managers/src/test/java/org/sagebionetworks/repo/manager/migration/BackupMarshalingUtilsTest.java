package org.sagebionetworks.repo.manager.migration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.LinkedList;
import java.util.List;

import org.junit.Test;
import org.sagebionetworks.repo.model.backup.FileHandleBackup;

/**
 * Test for BackupMarshalingUtils
 * 
 * @author John
 *
 */
public class BackupMarshalingUtilsTest {

	@Test
	public void testRoundTrip() throws UnsupportedEncodingException{
		String alias = "files";
		List<FileHandleBackup> list = new LinkedList<FileHandleBackup>();
		for(int i=0; i<5; i++){
			FileHandleBackup fhb = new FileHandleBackup();
			fhb.setId(new Long(i*i));
			fhb.setKey("key"+i);
			fhb.setBucketName("bucket"+i);
			list.add(fhb);
		}
		// Mashal it
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		BackupMarshalingUtils.writeBackupToStream(list, alias, out);
		String xml = new String(out.toByteArray(), "UTF-8");
		System.out.println(xml);
		ByteArrayInputStream in = new ByteArrayInputStream(xml.getBytes("UTF-8"));
		// Now make back
		List<FileHandleBackup> clone = BackupMarshalingUtils.readBacckupFromStream(FileHandleBackup.class, alias, in);
		assertNotNull(clone);
		assertEquals(list.size(), clone.size());
		assertEquals(list, clone);
	}
}
