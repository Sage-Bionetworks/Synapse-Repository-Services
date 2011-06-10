package org.sagebionetworks.workflow.curation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.apache.log4j.Logger;
import org.json.JSONObject;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.sagebionetworks.client.Synapse;
import org.sagebionetworks.workflow.UnrecoverableException;
import org.sagebionetworks.workflow.activity.Crawling;
import org.sagebionetworks.workflow.activity.Curation;
import org.sagebionetworks.workflow.activity.DataIngestion;
import org.sagebionetworks.workflow.activity.Notification;
import org.sagebionetworks.workflow.activity.Processing;
import org.sagebionetworks.workflow.activity.SimpleObserver;
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
	static private int clinicalLayerId = -1;
	static private int expressionLevel1LayerId = -1;
	static private int expressionLevel2LayerId = -1;
	static private DownloadResult clinicalDownloadResult;
	static private DownloadResult expressionLevel1DownloadResult;
	static private DownloadResult expressionLevel2DownloadResult;

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
	public void testDoCreateClinicalMetadata() throws Exception {
		clinicalLayerId = Curation
				.doCreateSynapseMetadataForTcgaSourceLayer(
						datasetId,
						"http://tcga-data.nci.nih.gov/tcgafiles/ftp_auth/distro_ftpusers/anonymous/tumor/coad/bcr/minbiotab/clin/clinical_public_coad.tar.gz");
		assertTrue(-1 < clinicalLayerId);
	}

	/**
	 * @throws Exception
	 */
	@Test
	public void testDoCreateExpressionLevel1Metadata() throws Exception {
		expressionLevel1LayerId = Curation
				.doCreateSynapseMetadataForTcgaSourceLayer(
						datasetId,
						"http://tcga-data.nci.nih.gov/tcgafiles/ftp_auth/distro_ftpusers/anonymous/tumor/coad/cgcc/unc.edu/agilentg4502a_07_3/transcriptome/unc.edu_COAD.AgilentG4502A_07_3.Level_1.1.4.0.tar.gz");
		assertTrue(-1 < expressionLevel1LayerId);
	}

	/**
	 * @throws Exception
	 */
	@Test
	public void testDoCreateExpressionLevel2Metadata() throws Exception {
		expressionLevel2LayerId = Curation
				.doCreateSynapseMetadataForTcgaSourceLayer(
						datasetId,
						"http://tcga-data.nci.nih.gov/tcgafiles/ftp_auth/distro_ftpusers/anonymous/tumor/coad/cgcc/unc.edu/agilentg4502a_07_3/transcriptome/unc.edu_COAD.AgilentG4502A_07_3.Level_2.2.0.0.tar.gz");
		assertTrue(-1 < expressionLevel2LayerId);
	}

	/**
	 * @throws Exception
	 */
	@Test
	public void testRScriptWorkflowSkip() throws Exception {

		ScriptResult scriptResult = null;

		Synapse synapse = ConfigHelper.createConfig().createSynapseClient();
		JSONObject results = synapse
				.query("select * from dataset where dataset.name == 'MSKCC Prostate Cancer'");
		assertEquals(1, results.getInt("totalNumberOfResults"));
		int mskccId = results.getJSONArray("results").getJSONObject(0).getInt(
				"dataset.id");

		// Pass the id for the
		scriptResult = Processing.doProcessLayer(
				"./src/test/resources/createMatrix.r", mskccId, -99,
				"/var/tmp/foo.txt");

		
		assertEquals(-1, scriptResult.getProcessedLayerId());

	}

	/**
	 * @throws Exception
	 */
	@Test
	public void testDoFormulateNotificationMessage() throws Exception {
		String message = Curation.formulateLayerCreationMessage(expressionLevel2LayerId);
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
	public void testDoUploadClinicalDataToS3() throws Exception {
		Storage.doUploadLayerToStorage(datasetId, clinicalLayerId,
				clinicalDownloadResult.getLocalFilepath(),
				clinicalDownloadResult.getMd5());
	}

	/**
	 * @throws Exception
	 */
	@Test
	@Ignore // the upload takes too long
	public void testDoUploadExpressionLevel1DataToS3() throws Exception {
		Storage.doUploadLayerToStorage(datasetId, expressionLevel1LayerId,
				expressionLevel1DownloadResult.getLocalFilepath(),
				expressionLevel1DownloadResult.getMd5());
	}

	/**
	 * @throws Exception
	 */
	@Test
	public void testDoUploadExpressionLevel2DataToS3() throws Exception {
		Storage.doUploadLayerToStorage(datasetId, expressionLevel2LayerId,
				expressionLevel2DownloadResult.getLocalFilepath(),
				expressionLevel2DownloadResult.getMd5());
	}

	/**
	 * @throws Exception
	 */
	@Test
	public void testRScript() throws Exception {

		ScriptResult scriptResult = null;

		scriptResult = Processing.doProcessLayer(
				"./src/test/resources/createMatrix.r", datasetId, expressionLevel2LayerId,
				expressionLevel2DownloadResult.getLocalFilepath());

		assertTrue(-1 < scriptResult.getProcessedLayerId());

	}

	/**
	 * @throws Exception
	 */
	@Test
	public void testDoProcessData() throws Exception {
		ScriptResult scriptResult = Processing.doProcessLayer(
				"./src/test/resources/stdoutKeepAlive.sh", datasetId,
				expressionLevel2LayerId, expressionLevel2DownloadResult.getLocalFilepath());
		assertTrue(0 <= scriptResult.getProcessedLayerId());

	}

	/**
	 */
	@Test
	public void testDoNotifyFollowers() {
		try {
			String topic = ConfigHelper.createConfig().getSnsTopic();
			Notification.doSnsNotifyFollowers(topic,
					"integration test subject",
					"integration test message, yay!");
		} catch (AmazonServiceException e) {
			log.error(e);
		}
	}

	/**
	 * @throws Exception
	 */
	@Test
	public void testDoTcgaCrawl() throws Exception {

		class CrawlObserver implements SimpleObserver<String> {
			int numUrlsFound = 0;
			int numArchivesFound = 0;
			Boolean foundExpression = false;
			Boolean foundClinical = false;

			@Override
			public void update(String url) {
				numUrlsFound++;
				if (url.endsWith("tar.gz")) {
					numArchivesFound++;
				}
				if (url.endsWith("/clinical_public_coad.tar.gz")) {
					// Make sure we are not returning the same terminal url more
					// than once
					assertTrue(!foundClinical);
					foundClinical = true;
				}
				if (url
						.endsWith("/unc.edu_COAD.AgilentG4502A_07_3.Level_2.2.0.0.tar.gz")) {
					// Make sure we are not returning the same terminal url more
					// than once
					assertTrue(!foundExpression);
					foundExpression = true;
				}

			}

		}

		CrawlObserver testObserver = new CrawlObserver();
		Crawling crawler = new Crawling();
		crawler.addObserver(testObserver);
		crawler
				.doCrawl(
						"http://tcga-data.nci.nih.gov/tcgafiles/ftp_auth/distro_ftpusers/anonymous/tumor/coad/",
						true);
		// The amount of data for this dataset should only grow
		assertTrue(3719 <= testObserver.numUrlsFound);
		assertTrue(64 <= testObserver.numArchivesFound);
		assertTrue(testObserver.foundClinical);
		assertTrue(testObserver.foundExpression);
	}

}
