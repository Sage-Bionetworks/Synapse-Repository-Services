package org.sagebionetworks.workflow.curation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.apache.log4j.Logger;
import org.json.JSONObject;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sagebionetworks.client.Synapse;
import org.sagebionetworks.workflow.UnrecoverableException;
import org.sagebionetworks.workflow.activity.Curation;
import org.sagebionetworks.workflow.activity.DataIngestion;
import org.sagebionetworks.workflow.activity.Notification;
import org.sagebionetworks.workflow.activity.Processing;
import org.sagebionetworks.workflow.activity.Storage;
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
	static private int datasetId = -1;
	static private int rawLayerId = -1;
	static private int clinicalLayerId = -1;
	static private DownloadResult expressionDownloadResult;

	/**
	 * @throws java.lang.Exception
	 */
	@BeforeClass
	static public void setUpBeforeClass() throws Exception {
		String datasetName = "coad";

		Synapse synapse = ConfigHelper.createConfig().createSynapseClient();
		JSONObject results = synapse
				.query("select * from dataset where dataset.name == '"
						+ datasetName + "'");

		int numDatasetsFound = results.getInt("totalNumberOfResults");
		if (0 == numDatasetsFound) {

			JSONObject dataset = new JSONObject();
			dataset.put("name", datasetName);

			// TODO put a unique constraint on the dataset name, and if we catch
			// an exception here for that, we should retry this workflow step
			JSONObject storedDataset = synapse
					.createEntity("/dataset", dataset);
			datasetId = storedDataset.getInt("id");
		} else {
			if (1 == numDatasetsFound) {
				datasetId = results.getJSONArray("results").getJSONObject(0)
						.getInt("dataset.id");
			} else {
				throw new UnrecoverableException("We have " + numDatasetsFound
						+ " datasets with name " + datasetName);
			}
		}
	}

	/**
	 * @throws Exception
	 */
	@Test
	public void testDoCreateExpressionMetadata() throws Exception {
		rawLayerId = Curation
				.doCreateMetadataForTcgaSourceLayer(
						datasetId,
						"http://tcga-data.nci.nih.gov/tcgafiles/ftp_auth/distro_ftpusers/anonymous/tumor/coad/cgcc/unc.edu/agilentg4502a_07_3/transcriptome/unc.edu_COAD.AgilentG4502A_07_3.Level_2.2.0.0.tar.gz");
		assertTrue(-1 < rawLayerId);
	}

	/**
	 * @throws Exception
	 */
	@Test
	public void testDoDownloadExpressionDataFromTcga() throws Exception {

		expressionDownloadResult = DataIngestion
				.doDownloadFromTcga("http://tcga-data.nci.nih.gov/tcgafiles/ftp_auth/distro_ftpusers/anonymous/tumor/coad/cgcc/unc.edu/agilentg4502a_07_3/transcriptome/unc.edu_COAD.AgilentG4502A_07_3.Level_2.2.0.0.tar.gz");

		assertTrue(expressionDownloadResult.getLocalFilepath().endsWith(
				"unc.edu_COAD.AgilentG4502A_07_3.Level_2.2.0.0.tar.gz"));

		assertEquals("33183779e53ce0cfc35f59cc2a762cbd",
				expressionDownloadResult.getMd5());
	}

	/**
	 * @throws Exception
	 */
	@Test
	public void testDoCreateClinicalMetadata() throws Exception {
		clinicalLayerId = Curation
				.doCreateMetadataForTcgaSourceLayer(
						datasetId,
						"http://tcga-data.nci.nih.gov/tcgafiles/ftp_auth/distro_ftpusers/anonymous/tumor/coad/bcr/minbiotab/clin/clinical_patient_public_coad.txt");
		assertTrue(-1 < clinicalLayerId);
	}

	/**
	 * @throws Exception
	 */
	@Test
	public void testDoDownloadClinicalDataFromTcga() throws Exception {

		DownloadResult clinicalDownloadResult = DataIngestion
				.doDownloadFromTcga("http://tcga-data.nci.nih.gov/tcgafiles/ftp_auth/distro_ftpusers/anonymous/tumor/coad/bcr/minbiotab/clin/clinical_patient_public_coad.txt");

		assertTrue(clinicalDownloadResult.getLocalFilepath().endsWith(
				"clinical_patient_public_coad.txt"));

		assertNotNull(clinicalDownloadResult.getMd5());
	}

	/**
	 * @throws Exception
	 */
	@Test
	public void testRScript() throws Exception {

		ScriptResult scriptResult = null;

		scriptResult = Processing.doProcessLayer(
				"./src/test/resources/createMatrix.r", datasetId, rawLayerId,
				expressionDownloadResult.getLocalFilepath());

		// TODO assert not equals, our script makes them the same right now
		assertEquals(rawLayerId, scriptResult.getProcessedLayerId());

	}

	/**
	 * @throws Exception
	 */
	@Test
	public void testDoUploadDataToS3() throws Exception {
		Storage.doUploadLayerToStorage(datasetId, rawLayerId,
				expressionDownloadResult.getLocalFilepath(),
				expressionDownloadResult.getMd5());
	}

	/**
	 * @throws Exception
	 */
	@Test
	public void testDoProcessData() throws Exception {
		ScriptResult scriptResult = Processing.doProcessLayer(
				"./src/test/resources/stdoutKeepAlive.sh", datasetId,
				rawLayerId, expressionDownloadResult.getLocalFilepath());
		assertTrue(0 <= scriptResult.getProcessedLayerId());

	}

	/**
	 */
	@Test
	public void testDoFormulateNotificationMessage() {
	}

	/**
	 */
	@Test
	public void testDoNotifyFollowers() {
		try {
			String topic = ConfigHelper.createConfig().getSnsTopic();
			Notification.doSnsNotifyFollowers(topic,
					"integration test subject", "integration test message, yay!");
		} catch (AmazonServiceException e) {
			log.error(e);
		}
	}

}
