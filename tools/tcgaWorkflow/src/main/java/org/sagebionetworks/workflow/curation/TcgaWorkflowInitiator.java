package org.sagebionetworks.workflow.curation;

import java.net.SocketTimeoutException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.json.JSONObject;
import org.sagebionetworks.client.Synapse;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.utils.WebCrawler;
import org.sagebionetworks.utils.SimpleObserver;
import org.sagebionetworks.workflow.activity.Curation;

import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflow;
import com.amazonaws.services.simpleworkflow.client.asynchrony.decider.annotations.ActivityAnnotationProcessor;
import com.amazonaws.services.simpleworkflow.client.asynchrony.decider.annotations.AsyncWorkflowStartContext;

/**
 * Discover all data archives for datasets and types of data in which we are
 * interested. Kick off a workflow for each. Note that this sends all urls
 * everytime. It is up to the workflow to recognize and skip work already
 * processed.
 * 
 * TODO this could actually be a workflow in and of itself, but it actually
 * completes in tens of seconds so its not worth the effort at this time
 * 
 * @author deflaux
 */
public class TcgaWorkflowInitiator {

	private static final String TCGA_REPOSITORY = "http://tcga-data.nci.nih.gov/tcgafiles/ftp_auth/distro_ftpusers/anonymous/tumor/";
	private static final Logger log = Logger
			.getLogger(TcgaWorkflowInitiator.class.getName());

	class ArchiveObserver implements SimpleObserver<String> {
		Map<String, String> versionMap = new HashMap<String, String>();

		public void update(String url) throws Exception {
			// We are only interested in archives that do not contain images
			if (url.endsWith("tar.gz") && (0 > url.indexOf("image"))) {
				JSONObject annotations = new JSONObject();
				JSONObject layer = Curation.formulateLayerMetadataFromTcgaUrl(
						url, annotations);

				// Since the URLs come in in order of lowest to highest
				// revision, this will keep overwriting the earlier revisions in
				// the map and at the end we will only have the latest revisions
				if (annotations.has("tcgaRevision")) {
					versionMap.put(url.replace(annotations
							.getString("tcgaRevision"), ""), url);
				} else {
					versionMap.put(url, url);
				}
			}
		}

		public final Collection<String> getResults() {
			return versionMap.values();
		}
	}

	class DatasetObserver implements SimpleObserver<String> {
		ConfigHelper configHelper;
		Synapse synapse;

		DatasetObserver() throws Exception {
			synapse = ConfigHelper.createSynapseClient();
		}

		public void update(String url) throws Exception {

			String urlComponents[] = url.split("/");

			String datasetAbbreviation = urlComponents[urlComponents.length - 1];
			String datasetName = configHelper.getTCGADatasetName(datasetAbbreviation);
			
			JSONObject results = null;
			int sleep = 1000;
			while(null == results) {
				try {
					results = synapse.query("select * from dataset where dataset.name == '"
							+ datasetName + "'");
				}
				catch(SynapseException ex) {
					if(ex.getCause() instanceof SocketTimeoutException) {
						Thread.sleep(sleep);
						sleep = sleep * 2; // exponential backoff
					}
				}
			}
			
			if(results != null) {
				int numDatasetsFound = results.getInt("totalNumberOfResults");
				if (0 == numDatasetsFound) {
					// If Synapse doesn't have a dataset for it, skip it
					log.debug("Skipping dataset " + datasetName + " at url " + url);
				} else {
					JSONObject datasetQueryResult = results.getJSONArray("results")
							.getJSONObject(0);
					String datasetId = datasetQueryResult.getString("dataset.id");
					log.debug("WebCrawler dataset " + datasetName + "("
							+ datasetQueryResult.getString("dataset.id")
							+ ") at url " + url);
					WebCrawler archiveCrawler = new WebCrawler();
					ArchiveObserver observer = new ArchiveObserver();
					archiveCrawler.addObserver(observer);
					archiveCrawler.doCrawl(url, true);
	
					Collection<String> urls = observer.getResults();
					for (String dataLayerUrl : urls) {
						TcgaWorkflow.doWorkflow("Workflow for TCGA Dataset "
								+ datasetName, datasetId, dataLayerUrl);
						log.info("Kicked off workflow for " + dataLayerUrl);
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
		// Running Annotation Processor is very important, otherwise it will
		// treat Workflows and Activities as regular java method calls
		ActivityAnnotationProcessor.processAnnotations();

		// Create the client for Simple Workflow Service
		AmazonSimpleWorkflow swfService = ConfigHelper.createSWFClient();
		AsyncWorkflowStartContext.initialize(swfService);

		TcgaWorkflowInitiator initiator = new TcgaWorkflowInitiator();
		initiator.initiateWorkflowTasks();
		
		System.exit(0);
	}

}
