package org.sagebionetworks.workflow.curation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.json.JSONObject;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sagebionetworks.client.Synapse;
import org.sagebionetworks.workflow.UnrecoverableException;
import org.sagebionetworks.workflow.curation.activity.CreateMetadataForTcgaSourceLayer;
import org.sagebionetworks.workflow.curation.activity.DownloadFromTcga;
import org.sagebionetworks.workflow.curation.activity.ProcessTcgaSourceLayer;
import org.sagebionetworks.workflow.curation.activity.DownloadFromTcga.DownloadResult;
import org.sagebionetworks.workflow.curation.activity.ProcessTcgaSourceLayer.ScriptResult;

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
		rawLayerId = CreateMetadataForTcgaSourceLayer
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

		expressionDownloadResult = DownloadFromTcga
				.doDownloadFromTcga("http://tcga-data.nci.nih.gov/tcgafiles/ftp_auth/distro_ftpusers/anonymous/tumor/coad/cgcc/unc.edu/agilentg4502a_07_3/transcriptome/unc.edu_COAD.AgilentG4502A_07_3.Level_2.2.0.0.tar.gz");

		assertTrue(expressionDownloadResult.getLocalFilepath().endsWith(
				"unc.edu_COAD.AgilentG4502A_07_3.Level_2.2.0.0.tar.gz"));

		assertEquals("33183779e53ce0cfc35f59cc2a762cbd", expressionDownloadResult
				.getMd5());
	}

	/**
	 * @throws Exception
	 */
	@Test
	public void testDoCreateClinicalMetadata() throws Exception {
		clinicalLayerId = CreateMetadataForTcgaSourceLayer
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

		DownloadResult clinicalDownloadResult = DownloadFromTcga
				.doDownloadFromTcga("http://tcga-data.nci.nih.gov/tcgafiles/ftp_auth/distro_ftpusers/anonymous/tumor/coad/bcr/minbiotab/clin/clinical_patient_public_coad.txt");

		assertTrue(clinicalDownloadResult.getLocalFilepath().endsWith(
				"clinical_patient_public_coad.txt"));

		assertEquals("903ff3e93fda8d0f0b17c5c1ec23fc89", clinicalDownloadResult
				.getMd5());
	}

	/**
	 * @throws Exception
	 */
	@Test
	public void testRScript() throws Exception {
		
		ScriptResult scriptResult = null;
		
		scriptResult = ProcessTcgaSourceLayer
				.doProcessTcgaSourceLayer(
						"./src/test/resources/createMatrix.r", datasetId, rawLayerId,
						expressionDownloadResult.getLocalFilepath());

		// TODO assert not equals, our script makes them the same right now
		assertEquals(rawLayerId, scriptResult.getProcessedLayerId());

	}

	/**
	 */
	@Test
	public void testDoUploadDataToS3() {
	}

	/**
	 * @throws Exception
	 */
	@Test
	public void testDoProcessData() throws Exception {
		ScriptResult scriptResult = ProcessTcgaSourceLayer
				.doProcessTcgaSourceLayer(
						"./src/test/resources/stdoutKeepAlive.sh", datasetId, rawLayerId,
						expressionDownloadResult.getLocalFilepath());
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
	}

}
