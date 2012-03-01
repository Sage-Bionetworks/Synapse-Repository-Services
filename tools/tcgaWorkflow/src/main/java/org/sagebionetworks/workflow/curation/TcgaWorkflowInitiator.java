package org.sagebionetworks.workflow.curation;

import java.net.SocketTimeoutException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.json.JSONObject;
import org.sagebionetworks.client.Synapse;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.utils.SimpleObserver;
import org.sagebionetworks.utils.WebCrawler;
import org.sagebionetworks.workflow.Constants;

import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflow;

/**
 * Discover all data archives for datasets and types of data in which we are
 * interested. Kick off a workflow for each. Note that this sends all urls
 * everytime. It is up to the workflow to recognize and skip work already
 * processed.
 * 
 * TODO this could actually be a workflow in and of itself, but its cheaper to
 * run this in cron
 * 
 * @author deflaux
 */
public class TcgaWorkflowInitiator {

	private static final String RAW_DATA_PROJECT_NAME = "Synapse Commons Repository";
	private static final String OLD_RAW_DATA_PROJECT_NAME = "SageBioCuration";
	private static final String TCGA_REPOSITORY = "http://tcga-data.nci.nih.gov/tcgafiles/ftp_auth/distro_ftpusers/anonymous/tumor/";
	private static final Logger log = Logger
			.getLogger(TcgaWorkflowInitiator.class.getName());

	TcgaWorkflowClientExternalFactory clientFactory;

	/**
	 * @param clientFactory
	 */
	public TcgaWorkflowInitiator(TcgaWorkflowClientExternalFactory clientFactory) {
		this.clientFactory = clientFactory;
	}

	class ArchiveObserver implements SimpleObserver<String> {
		Map<String, String> versionMap = new HashMap<String, String>();

		public void update(String url) throws Exception {
			// We are only interested in archives
			if (!url.endsWith("tar.gz"))
				return;
			// We are only interested in archives that do not contain images
			if (0 <= url.indexOf("image"))
				return;
			// Skip this deprecated data from TCGA PLFM-881
			// http://tcga-data.nci.nih.gov/tcgafiles/ftp_auth/distro_ftpusers/anonymous/tumor/coad/cgcc/bcgsc.ca/miRNASeq/bcgsc.ca_COAD.IlluminaGA_miRNASeq.Level_3.1.0.0.README.txt
			if (0 <= url.indexOf("bcgsc.ca/miRNASeq/"))
				return;

			Map<String, String> metadata = TcgaCuration
					.formulateMetadataFromTcgaUrl(url);

			// Since the URLs come in in order of lowest to highest
			// revision, this will keep overwriting the earlier revisions in
			// the map and at the end we will only have the latest revisions
			if (metadata.containsKey("tcgaRevision")) {
				versionMap.put(url.replace(metadata.get("tcgaRevision"), ""),
						url);
			} else {
				versionMap.put(url, url);
			}
		}

		public final Collection<String> getResults() {
			return versionMap.values();
		}
	}

	class DatasetObserver implements SimpleObserver<String> {
		TcgaWorkflowConfigHelper configHelper;
		Synapse synapse;
		String projectId;

		DatasetObserver() throws Exception {
			synapse = TcgaWorkflowConfigHelper.getSynapseClient();

			JSONObject results = synapse
					.query("select * from project where project.name == '"
							+ RAW_DATA_PROJECT_NAME + "'");
			if (1 != results.getInt("totalNumberOfResults")) {
				// Staging does not currently have Synapse Commons Repository so
				// fall back to SageBioCuration
				results = synapse
						.query("select * from project where project.name == '"
								+ OLD_RAW_DATA_PROJECT_NAME + "'");
				if (1 != results.getInt("totalNumberOfResults")) {
					throw new Exception("Found "
							+ results.getInt("totalNumberOfResults")
							+ " projects with name " + RAW_DATA_PROJECT_NAME);
				}
			}
			JSONObject projectQueryResult = results.getJSONArray("results")
					.getJSONObject(0);
			projectId = projectQueryResult.getString("project.id");
			log.debug("WebCrawler project " + RAW_DATA_PROJECT_NAME + "("
					+ projectQueryResult.getString("project.id") + ")");
		}

		public void update(String url) throws Exception {

			String urlComponents[] = url.split("/");

			String datasetAbbreviation = urlComponents[urlComponents.length - 1];
			String datasetName = TcgaWorkflowConfigHelper
					.getTCGADatasetName(datasetAbbreviation);

			JSONObject results = null;
			int sleep = 1000;
			while (null == results) {
				try {
					if (null == datasetName) {
						// Try finding the right dataset to update via
						// annotation tcgaDiseaseStudy
						results = synapse
								.query("select * from dataset where dataset.tcgaDiseaseStudy == '"
										+ datasetAbbreviation
										+ "' and dataset.parentId == "
										+ projectId);
						int numDatasetsFound = results
								.getInt("totalNumberOfResults");
						if (0 == numDatasetsFound) {
							results = synapse
									.query("select * from dataset where dataset.tcgaDiseaseStudy == '"
											+ datasetAbbreviation.toUpperCase()
											+ "' and dataset.parentId == "
											+ projectId);
						}
					} else {
						// Try finding the right dataset to update via our
						// static mapping of TCGA disease codes to SageBio
						// dataset names
						results = synapse
								.query("select * from dataset where dataset.name == '"
										+ datasetName
										+ "' and dataset.parentId == "
										+ projectId);
					}
				} catch (SynapseException ex) {
					if (ex.getCause() instanceof SocketTimeoutException) {
						Thread.sleep(sleep);
						sleep = sleep * 2; // exponential backoff
					}
				}
			}

			if (results != null) {
				int numDatasetsFound = results.getInt("totalNumberOfResults");
				if (0 == numDatasetsFound) {
					// If Synapse doesn't have a dataset for it, skip it
					log.debug("Skipping dataset " + datasetAbbreviation
							+ " at url " + url);
				} else {
					JSONObject datasetQueryResult = results.getJSONArray(
							"results").getJSONObject(0);
					String datasetId = datasetQueryResult
							.getString("dataset.id");
					log.debug("WebCrawler dataset " + datasetAbbreviation + "("
							+ datasetQueryResult.getString("dataset.id")
							+ ") at url " + url);
					WebCrawler archiveCrawler = new WebCrawler();
					ArchiveObserver observer = new ArchiveObserver();
					archiveCrawler.addObserver(observer);
					archiveCrawler.doCrawl(url, true);

					Collection<String> urls = observer.getResults();
					for (String layerUrl : urls) {
						try {
							String layerId = TcgaCuration.createMetadata(datasetId, layerUrl, true);
							if(!Constants.WORKFLOW_DONE.equals(layerId)) {
								TcgaWorkflowClientExternal workflow = clientFactory
									.getClient();
								workflow.addLocationToRawTcgaLayer(layerId, layerUrl);
								log.info("Kicked off workflow for " + layerUrl);
							}
						}
						catch(Exception e) {
							log.error(e);
						}
					}
				}
			}
		}
	}

	/**
	 * Crawl all top level TCGA datasets and identify the ones in which we are
	 * interested
	 */
	void initiateWorkflowTasks() throws Exception {
		WebCrawler datasetsCrawler = new WebCrawler();
		DatasetObserver observer = new DatasetObserver();
		datasetsCrawler.addObserver(observer);
		datasetsCrawler.doCrawl(TCGA_REPOSITORY, false);
	}

	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		// Create the client for Simple Workflow Service
		AmazonSimpleWorkflow swfService = TcgaWorkflowConfigHelper.getSWFClient();
		String domain = TcgaWorkflowConfigHelper.getStack();

		TcgaWorkflowClientExternalFactory clientFactory = new TcgaWorkflowClientExternalFactoryImpl(
				swfService, domain);

		TcgaWorkflowInitiator initiator = new TcgaWorkflowInitiator(
				clientFactory);
		initiator.initiateWorkflowTasks();

		System.exit(0);
	}

}
