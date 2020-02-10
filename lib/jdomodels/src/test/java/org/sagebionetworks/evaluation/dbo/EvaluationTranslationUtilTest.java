package org.sagebionetworks.evaluation.dbo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.UnsupportedEncodingException;

import org.junit.Test;
import org.sagebionetworks.evaluation.model.EvaluationStatus;
import org.sagebionetworks.repo.model.jdo.KeyFactory;

public class EvaluationTranslationUtilTest {
	
	@Test
	public void testGetContentSource(){
		Long result = EvaluationTranslationUtil.getContentSource("syn123");
		assertEquals(new Long(123), result);
	}
	
	@Test
	public void testGetContentSourceNoSYN(){
		Long result = EvaluationTranslationUtil.getContentSource("123");
		assertEquals(new Long(123), result);
	}
	
	@Test
	public void testGetContentSourceNull(){
		Long result = EvaluationTranslationUtil.getContentSource(null);
		assertEquals(KeyFactory.ROOT_ID, result);
	}
	
	@Test
	public void testGetContentSourceNotANumber(){
		Long result = EvaluationTranslationUtil.getContentSource("foo");
		assertEquals(KeyFactory.ROOT_ID, result);
	}
	
	@Test
	public void testGetContentSourceBadSYN(){
		Long result = EvaluationTranslationUtil.getContentSource("synBar");
		assertEquals(KeyFactory.ROOT_ID, result);
	}
	
	@Test
	public void testGetContentSourceSYNUpper(){
		Long result = EvaluationTranslationUtil.getContentSource("SyN456");
		assertEquals(new Long(456), result);
	}


	@Test
	public void testRoundTrip() throws UnsupportedEncodingException{
		EvaluationBackup backup = new EvaluationBackup();
		backup.setContentSource("syn123");
		backup.setCreatedOn(System.currentTimeMillis());
		backup.setDescription("description".getBytes("UTF-8"));
		backup.seteTag("tag");
		backup.setId(456l);
		backup.setName("name");
		backup.setOwnerId(999l);
		backup.setStatus(EvaluationStatus.COMPLETED.ordinal());
		backup.setSubmissionInstructions("foo".getBytes("UTF-8"));
		backup.setSubmissionReceiptMessage("bar".getBytes("UTF-8"));
		backup.setQuota((new String("evaluation quota info goes here")).getBytes());
		backup.setStartTimestamp(System.currentTimeMillis());
		backup.setEndTimestamp(System.currentTimeMillis()+1000L);
		// Create the dbo
		EvaluationDBO dbo = EvaluationTranslationUtil.createDatabaseObjectFromBackup(backup);
		assertNotNull(dbo);
		EvaluationBackup clone = EvaluationTranslationUtil.createBackupFromDatabaseObject(dbo);
		assertEquals(backup, clone);
	}
}
