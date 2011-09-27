package org.sagebionetworks.workflow.curation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.apache.log4j.Logger;
import org.json.JSONObject;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.sagebionetworks.client.Synapse;
import org.sagebionetworks.workflow.Constants;
import org.sagebionetworks.workflow.UnrecoverableException;
import org.sagebionetworks.workflow.activity.Curation;
import org.sagebionetworks.workflow.activity.DataIngestion;
import org.sagebionetworks.workflow.activity.Notification;
import org.sagebionetworks.workflow.activity.Processing;
import org.sagebionetworks.workflow.activity.DataIngestion.DownloadResult;
import org.sagebionetworks.workflow.activity.Processing.ScriptResult;

import com.amazonaws.AmazonServiceException;

/**
 * Note that this integration test should pass when the system is clean (no
 * files downloaded, no metadata created) and also when the tests have already
 * been run once. All these activities are supposed to be idempotent and it is
 * an error if they are not.
 * 
 * @author deflaux
 * 
 */
public class TcgaWorkflowITCase {

	private static final Logger log = Logger.getLogger(TcgaWorkflowITCase.class
			.getName());

	// These variables are used to pass data between tests
	static private String datasetId = null;
	static private String clinicalLayerId = null;
	static private String expressionLevel1LayerId = null;
	static private String expressionLevel2LayerId = null;
	static private DownloadResult clinicalDownloadResult;
	static private DownloadResult expressionLevel1DownloadResult;
	static private DownloadResult expressionLevel2DownloadResult;

	/**
	 * @throws java.lang.Exception
	 */
	@BeforeClass
	static public void setUpBeforeClass() throws Exception {
		String datasetName = "Colon Adenocarcinoma TCGA";

		Synapse synapse = ConfigHelper.createSynapseClient();
		JSONObject results = synapse
				.query("select * from dataset where dataset.name == '"
						+ datasetName + "'");

		int numDatasetsFound = results.getInt("totalNumberOfResults");
		if (1 == numDatasetsFound) {
			datasetId = results.getJSONArray("results").getJSONObject(0)
			.getString("dataset.id");
		} else {
			throw new UnrecoverableException("We have " + numDatasetsFound
					+ " datasets with name " + datasetName);
		}
	}

	@Test
	public void testTCGAAbbreviation2Name() throws Exception {
		assertEquals("Colon Adenocarcinoma TCGA", ConfigHelper.getTCGADatasetName("coad"));
	}
	
	/**
	 * @throws Exception
	 */
	@Test
	public void testDoCreateClinicalMetadata() throws Exception {
		clinicalLayerId = Curation
				.doCreateSynapseMetadataForTcgaSourceLayer(
						false,
						datasetId,
						"http://tcga-data.nci.nih.gov/tcgafiles/ftp_auth/distro_ftpusers/anonymous/tumor/coad/bcr/minbiotab/clin/clinical_public_coad.tar.gz");
		assertFalse(Constants.WORKFLOW_DONE.equals(clinicalLayerId));
	}

	/**
	 * @throws Exception
	 */
	@Test
	public void testDoCreateExpressionLevel1Metadata() throws Exception {
		expressionLevel1LayerId = Curation
				.doCreateSynapseMetadataForTcgaSourceLayer(
						false,
						datasetId,
						"http://tcga-data.nci.nih.gov/tcgafiles/ftp_auth/distro_ftpusers/anonymous/tumor/coad/cgcc/unc.edu/agilentg4502a_07_3/transcriptome/unc.edu_COAD.AgilentG4502A_07_3.Level_1.1.4.0.tar.gz");
		assertFalse(Constants.WORKFLOW_DONE.equals(expressionLevel1LayerId));
	}

	/**
	 * @throws Exception
	 */
	@Test
	public void testDoCreateExpressionLevel2Metadata() throws Exception {
		expressionLevel2LayerId = Curation
				.doCreateSynapseMetadataForTcgaSourceLayer(
						false,
						datasetId,
						"http://tcga-data.nci.nih.gov/tcgafiles/ftp_auth/distro_ftpusers/anonymous/tumor/coad/cgcc/unc.edu/agilentg4502a_07_3/transcriptome/unc.edu_COAD.AgilentG4502A_07_3.Level_2.2.0.0.tar.gz");
		assertFalse(Constants.WORKFLOW_DONE.equals(expressionLevel2LayerId));
	}

	/**
	 * @throws Exception
	 */
	@Test
	public void testDoCreateGeneticMetadata() throws Exception {
		String geneticLayerId = Curation
				.doCreateSynapseMetadataForTcgaSourceLayer(
						false,
						datasetId,
						"http://tcga-data.nci.nih.gov/tcgafiles/ftp_auth/distro_ftpusers/anonymous/tumor/coad/cgcc/broad.mit.edu/genome_wide_snp_6/snp/broad.mit.edu_COAD.Genome_Wide_SNP_6.mage-tab.1.1007.0.tar.gz");
		assertFalse(Constants.WORKFLOW_DONE.equals(geneticLayerId));
	}

	/**
	 * @throws Exception
	 */
	@Test
	public void testRScriptWorkflowSkip() throws Exception {

		ScriptResult scriptResult = null;

		Synapse synapse = ConfigHelper.createSynapseClient();
		JSONObject results = synapse
				.query("select * from dataset where dataset.name == 'MSKCC Prostate Cancer'");
		assertEquals(1, results.getInt("totalNumberOfResults"));
		String mskccId = results.getJSONArray("results").getJSONObject(0)
				.getString("dataset.id");

		// Pass the id for the
		scriptResult = Processing.doProcessLayer(
				"./src/test/resources/createMatrix.r", 
				mskccId, "fakeLayerId");

		assertEquals(Constants.WORKFLOW_DONE, scriptResult
				.getProcessedLayerId());

	}

	/**
	 * @throws Exception
	 */
	@Test
	public void testDoFormulateNotificationMessage() throws Exception {
		String message = Curation
				.formulateLayerCreationMessage(expressionLevel2LayerId);
		assertNotNull(message);
	}

	/**
	 * @throws Exception
	 */
	@Test
	public void testDoDownloadClinicalDataFromTcga() throws Exception {

		clinicalDownloadResult = DataIngestion
				.doDownloadFromTcga("http://tcga-data.nci.nih.gov/tcgafiles/ftp_auth/distro_ftpusers/anonymous/tumor/coad/bcr/minbiotab/clin/clinical_public_coad.tar.gz");

		assertTrue(clinicalDownloadResult.getLocalFilepath().endsWith(
				"clinical_public_coad.tar.gz"));

		assertNotNull(clinicalDownloadResult.getMd5());
	}

	/**
	 * @throws Exception
	 */
	@Test
	@Ignore // The download takes too long
	public void testDoDownloadExpressionLevel1DataFromTcga() throws Exception {

		expressionLevel1DownloadResult = DataIngestion
				.doDownloadFromTcga("http://tcga-data.nci.nih.gov/tcgafiles/ftp_auth/distro_ftpusers/anonymous/tumor/coad/cgcc/unc.edu/agilentg4502a_07_3/transcriptome/unc.edu_COAD.AgilentG4502A_07_3.Level_1.1.4.0.tar.gz");

		assertTrue(expressionLevel1DownloadResult.getLocalFilepath().endsWith(
				"unc.edu_COAD.AgilentG4502A_07_3.Level_1.1.4.0.tar.gz"));

		assertEquals("add6f8369383e777f1f4011cdeceb99d",
				expressionLevel1DownloadResult.getMd5());
	}

	/**
	 * @throws Exception
	 */
	@Test
	public void testDoDownloadExpressionLevel2DataFromTcga() throws Exception {

		expressionLevel2DownloadResult = DataIngestion
				.doDownloadFromTcga("http://tcga-data.nci.nih.gov/tcgafiles/ftp_auth/distro_ftpusers/anonymous/tumor/coad/cgcc/unc.edu/agilentg4502a_07_3/transcriptome/unc.edu_COAD.AgilentG4502A_07_3.Level_2.2.0.0.tar.gz");

		assertTrue(expressionLevel2DownloadResult.getLocalFilepath().endsWith(
				"unc.edu_COAD.AgilentG4502A_07_3.Level_2.2.0.0.tar.gz"));

		assertEquals("33183779e53ce0cfc35f59cc2a762cbd",
				expressionLevel2DownloadResult.getMd5());
	}

	/**
	 * @throws Exception
	 */
	@Test
	public void testRScript() throws Exception {

		ScriptResult scriptResult = null;

		scriptResult = Processing.doProcessLayer(
				"./src/test/resources/createMatrix.r", datasetId,
				expressionLevel2LayerId);

		assertFalse(Constants.WORKFLOW_DONE.equals(scriptResult
				.getProcessedLayerId()));

	}

	/**
	 * @throws Exception
	 */
	@Test
	public void testDoProcessData() throws Exception {
		ScriptResult scriptResult = Processing.doProcessLayer(
				"./src/test/resources/stdoutKeepAlive.sh", datasetId,
				expressionLevel2LayerId);
		assertFalse(Constants.WORKFLOW_DONE.equals(scriptResult
				.getProcessedLayerId()));

	}

	/**
	 */
	@Test
	public void testDoNotifyFollowers() {
		try {
			String topic = ConfigHelper.getWorkflowSnsTopic();
			Notification.doSnsNotifyFollowers(topic,
					"integration test subject",
					"integration test message, yay!");
		} catch (AmazonServiceException e) {
			log.error(e);
		}
	}
}
