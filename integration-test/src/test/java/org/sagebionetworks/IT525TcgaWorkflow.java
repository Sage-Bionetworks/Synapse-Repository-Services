package org.sagebionetworks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.apache.log4j.Logger;
import org.json.JSONObject;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sagebionetworks.client.Synapse;
import org.sagebionetworks.repo.model.Layer;
import org.sagebionetworks.repo.model.LayerTypeNames;
import org.sagebionetworks.repo.model.LocationTypeNames;
import org.sagebionetworks.workflow.Constants;
import org.sagebionetworks.workflow.Notification;
import org.sagebionetworks.workflow.UnrecoverableException;
import org.sagebionetworks.workflow.curation.TcgaWorkflowConfigHelper;
import org.sagebionetworks.workflow.curation.TcgaCuration;

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

public class IT525TcgaWorkflow {

	private static final Logger log = Logger.getLogger(IT525TcgaWorkflow.class
			.getName());

	static private Synapse synapse;

	// These variables are used to pass data between tests
	static private String datasetId = null;
	static private String layerId = null;

	/**
	 * @throws java.lang.Exception
	 */
	@BeforeClass
	static public void setUpBeforeClass() throws Exception {
		String datasetName = "Colon Adenocarcinoma TCGA";

		synapse = TcgaWorkflowConfigHelper.getSynapseClient();
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

	/**
	 * @throws Exception
	 */
	@Test
	public void testTCGAAbbreviation2Name() throws Exception {
		assertEquals("Colon Adenocarcinoma TCGA", TcgaWorkflowConfigHelper
				.getTCGADatasetName("coad"));

	}

	/**
	 * @throws Exception
	 */
	@Test
	public void testDoCreateClinicalMetadata() throws Exception {
		String url = "http://tcga-data.nci.nih.gov/tcgafiles/ftp_auth/distro_ftpusers/anonymous/tumor/coad/bcr/minbiotab/clin/clinical_public_coad.tar.gz";
		layerId = TcgaCuration.createMetadata(datasetId, url, false);
		assertFalse(Constants.WORKFLOW_DONE.equals(layerId));
		boolean layerWasUpdated = TcgaCuration.updateLocation(url, layerId);
		assertTrue(layerWasUpdated);
		layerWasUpdated = TcgaCuration.updateLocation(url, layerId);
		assertFalse(layerWasUpdated);

		Layer layer = synapse.getEntity(layerId, Layer.class);

		assertTrue(0 < layer.getMd5().length());
		assertEquals(1, layer.getLocations().size());
		assertEquals(LocationTypeNames.awss3, layer.getLocations().get(0)
				.getType());

		JSONObject allAnnotations = synapse.getEntity(layer.getAnnotations());
		JSONObject annotations = allAnnotations
				.getJSONObject("stringAnnotations");

		assertEquals(LayerTypeNames.C, layer.getType());
		assertEquals("clinical_public_coad", layer.getName());
		assertEquals("raw", layer.getStatus());
		assertEquals("tsv", annotations.getJSONArray("format").get(0));
	}

	/**
	 * @throws Exception
	 */
	@Test
	public void testDoCreateExpressionLevel1Metadata() throws Exception {
		String url = "http://tcga-data.nci.nih.gov/tcgafiles/ftp_auth/distro_ftpusers/anonymous/tumor/coad/cgcc/unc.edu/agilentg4502a_07_3/transcriptome/unc.edu_COAD.AgilentG4502A_07_3.Level_1.1.4.0.tar.gz";
		layerId = TcgaCuration.createMetadata(datasetId, url, false);
		assertFalse(Constants.WORKFLOW_DONE.equals(layerId));
		boolean layerWasUpdated = TcgaCuration.updateLocation(url, layerId);
		assertTrue(layerWasUpdated);
		layerWasUpdated = TcgaCuration.updateLocation(url, layerId);
		assertFalse(layerWasUpdated);

		Layer layer = synapse.getEntity(layerId, Layer.class);

		assertTrue(0 < layer.getMd5().length());
		assertEquals(1, layer.getLocations().size());
		assertEquals(LocationTypeNames.external, layer.getLocations().get(0)
				.getType());

		JSONObject allAnnotations = synapse.getEntity(layer.getAnnotations());
		JSONObject annotations = allAnnotations
				.getJSONObject("stringAnnotations");

		assertEquals(LayerTypeNames.E, layer.getType());
		assertEquals("unc.edu_COAD.AgilentG4502A_07_3.Level_1.1.4.0", layer
				.getName());
		assertEquals("raw", layer.getStatus());
		assertEquals("unc.edu", annotations.getJSONArray("tcgaDomain").get(0));
		assertEquals("COAD", annotations.getJSONArray("tcgaDiseaseStudy")
				.get(0));
		assertEquals("AgilentG4502A_07_3", layer.getPlatform());
		assertEquals("Level_1", annotations.getJSONArray("tcgaLevel").get(0));
		assertEquals("1", annotations.getJSONArray("tcgaArchiveSerialIndex")
				.get(0));
		assertEquals("4", annotations.getJSONArray("tcgaRevision").get(0));
		assertEquals("0", annotations.getJSONArray("tcgaSeries").get(0));
		assertEquals("tsv", annotations.getJSONArray("format").get(0));
	}

	/**
	 * @throws Exception
	 */
	@Test
	public void testDoCreateExpressionLevel2Metadata() throws Exception {
		String url = "http://tcga-data.nci.nih.gov/tcgafiles/ftp_auth/distro_ftpusers/anonymous/tumor/coad/cgcc/unc.edu/agilentg4502a_07_3/transcriptome/unc.edu_COAD.AgilentG4502A_07_3.Level_2.2.0.0.tar.gz";
		layerId = TcgaCuration.createMetadata(datasetId, url, false);
		assertFalse(Constants.WORKFLOW_DONE.equals(layerId));
		boolean layerWasUpdated = TcgaCuration.updateLocation(url, layerId);
		assertTrue(layerWasUpdated);
		layerWasUpdated = TcgaCuration.updateLocation(url, layerId);
		assertFalse(layerWasUpdated);

		Layer layer = synapse.getEntity(layerId, Layer.class);

		assertTrue(0 < layer.getMd5().length());
		assertEquals(1, layer.getLocations().size());
		assertEquals(LocationTypeNames.external, layer.getLocations().get(0)
				.getType());

		JSONObject allAnnotations = synapse.getEntity(layer.getAnnotations());
		JSONObject annotations = allAnnotations
				.getJSONObject("stringAnnotations");

		assertEquals(LayerTypeNames.E, layer.getType());
		assertEquals("unc.edu_COAD.AgilentG4502A_07_3.Level_2.2.0.0", layer
				.getName());
		assertEquals("raw", layer.getStatus());
		assertEquals("unc.edu", annotations.getJSONArray("tcgaDomain").get(0));
		assertEquals("COAD", annotations.getJSONArray("tcgaDiseaseStudy")
				.get(0));
		assertEquals("AgilentG4502A_07_3", layer.getPlatform());
		assertEquals("Level_2", annotations.getJSONArray("tcgaLevel").get(0));
		assertEquals("2", annotations.getJSONArray("tcgaArchiveSerialIndex")
				.get(0));
		assertEquals("0", annotations.getJSONArray("tcgaRevision").get(0));
		assertEquals("0", annotations.getJSONArray("tcgaSeries").get(0));
		assertEquals("tsv", annotations.getJSONArray("format").get(0));
	}

	/**
	 * @throws Exception
	 */
	@Test
	public void testDoCreateGeneticMetadata() throws Exception {
		String url = "http://tcga-data.nci.nih.gov/tcgafiles/ftp_auth/distro_ftpusers/anonymous/tumor/coad/cgcc/broad.mit.edu/genome_wide_snp_6/snp/broad.mit.edu_COAD.Genome_Wide_SNP_6.mage-tab.1.1007.0.tar.gz";
		layerId = TcgaCuration.createMetadata(datasetId, url, false);
		assertFalse(Constants.WORKFLOW_DONE.equals(layerId));
		boolean layerWasUpdated = TcgaCuration.updateLocation(url, layerId);
		assertTrue(layerWasUpdated);
		layerWasUpdated = TcgaCuration.updateLocation(url, layerId);
		assertFalse(layerWasUpdated);

		Layer layer = synapse.getEntity(layerId, Layer.class);

		assertTrue(0 < layer.getMd5().length());
		assertEquals(1, layer.getLocations().size());
		assertEquals(LocationTypeNames.external, layer.getLocations().get(0)
				.getType());

		JSONObject allAnnotations = synapse.getEntity(layer.getAnnotations());
		JSONObject annotations = allAnnotations
				.getJSONObject("stringAnnotations");

		assertEquals(LayerTypeNames.G, layer.getType());
		assertEquals("broad.mit.edu_COAD.Genome_Wide_SNP_6.mage-tab.1.1007.0",
				layer.getName());
		assertEquals("raw", layer.getStatus());
		assertEquals("tsv", annotations.getJSONArray("format").get(0));
		assertEquals("broad.mit.edu", annotations.getJSONArray("tcgaDomain")
				.get(0));
		assertEquals("COAD", annotations.getJSONArray("tcgaDiseaseStudy")
				.get(0));
		assertEquals("Genome_Wide_SNP_6", layer.getPlatform());
		assertEquals("mage-tab", annotations.getJSONArray("tcgaLevel").get(0));
		assertEquals("1", annotations.getJSONArray("tcgaArchiveSerialIndex")
				.get(0));
		assertEquals("1007", annotations.getJSONArray("tcgaRevision").get(0));
		assertEquals("0", annotations.getJSONArray("tcgaSeries").get(0));
	}

	/**
	 * @throws Exception
	 */
	@Test
	public void testDoFormulateNotificationMessage() throws Exception {
		String message = TcgaCuration
				.formulateLayerNotificationMessage(layerId);
		assertNotNull(message);
	}

	/**
	 */
	@Test
	public void testDoNotifyFollowers() {
		try {
			String topic = TcgaWorkflowConfigHelper.getWorkflowSnsTopic();
			Notification.doSnsNotifyFollowers(TcgaWorkflowConfigHelper
					.getSNSClient(), topic, "integration test subject",
					"integration test message, yay!");
		} catch (AmazonServiceException e) {
			log.error(e);
		}
	}
}
