/**
 * 
 */
package org.sagebionetworks.workflow.curation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.workflow.curation.activity.DownloadFromTcga;
import org.sagebionetworks.workflow.curation.activity.ProcessTcgaSourceLayer;
import org.sagebionetworks.workflow.curation.activity.DownloadFromTcga.DownloadResult;
import org.sagebionetworks.workflow.curation.activity.ProcessTcgaSourceLayer.ScriptResult;

/**
 * @author deflaux
 * 
 */
public class TcgaWorkflowTest {

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
	}

	/**
	 * Test method for
	 * {@link org.sagebionetworks.workflow.curation.TcgaWorkflow#doCreateMetadata(java.lang.String, java.lang.Integer, java.lang.String, com.amazonaws.services.simpleworkflow.client.asynchrony.Settable)}
	 * .
	 */
	@Test
	public void testDoCreateMetadata() {
	}

	/**
	 * Test method for
	 * {@link org.sagebionetworks.workflow.curation.TcgaWorkflow#doDownloadDataFromTcga(java.lang.String, java.lang.String, com.amazonaws.services.simpleworkflow.client.asynchrony.Settable, com.amazonaws.services.simpleworkflow.client.asynchrony.Settable, com.amazonaws.services.simpleworkflow.client.asynchrony.Settable)}
	 * .
	 * 
	 * @throws Exception
	 */
	@Test
	public void testDoDownloadDataFromTcga() throws Exception {
		
		DownloadResult result = DownloadFromTcga.doDownloadFromTcga(
				"http://tcga-data.nci.nih.gov/tcgafiles/ftp_auth/distro_ftpusers/anonymous/tumor/coad/cgcc/unc.edu/agilentg4502a_07_3/transcriptome/unc.edu_COAD.AgilentG4502A_07_3.Level_2.2.0.0.tar.gz");
		assertTrue(result.getLocalFilepath().endsWith(
				"unc.edu_COAD.AgilentG4502A_07_3.Level_2.2.0.0.tar.gz"));
		assertEquals("33183779e53ce0cfc35f59cc2a762cbd", result.getMd5());

		ScriptResult scriptResult = ProcessTcgaSourceLayer.doProcessTcgaSourceLayer(
				"/Users/deflaux/platform/deflaux/scripts/stdoutKeepAlive.sh",
				23, 
				result.getLocalFilepath());
		assertTrue(0 <= scriptResult.getProcessedLayerId());

	}

	/**
	 * Test method for
	 * {@link org.sagebionetworks.workflow.curation.TcgaWorkflow#doUploadDataToS3(java.lang.String, java.lang.Integer, java.lang.String, java.lang.String, java.lang.String)}
	 * .
	 */
	@Test
	public void testDoUploadDataToS3() {
	}

	/**
	 * Test method for
	 * {@link org.sagebionetworks.workflow.curation.TcgaWorkflow#doProcessData(java.lang.String, java.lang.String, java.lang.Integer, java.lang.String, java.lang.String, com.amazonaws.services.simpleworkflow.client.asynchrony.Settable, com.amazonaws.services.simpleworkflow.client.asynchrony.Settable, com.amazonaws.services.simpleworkflow.client.asynchrony.Settable)}
	 * .
	 */
	@Test
	public void testDoProcessData() {
	}

	/**
	 * Test method for
	 * {@link org.sagebionetworks.workflow.curation.TcgaWorkflow#doFormulateNotificationMessage(java.lang.String, java.lang.Integer, com.amazonaws.services.simpleworkflow.client.asynchrony.Settable)}
	 * .
	 */
	@Test
	public void testDoFormulateNotificationMessage() {
	}

	/**
	 * Test method for
	 * {@link org.sagebionetworks.workflow.curation.TcgaWorkflow#doNotifyFollowers(java.lang.String, java.lang.Integer, java.lang.String)}
	 * .
	 */
	@Test
	public void testDoNotifyFollowers() {
	}

}
