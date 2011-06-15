package org.sagebionetworks.workflow.curation;

import org.apache.log4j.Logger;
import org.json.JSONObject;
import org.sagebionetworks.client.Synapse;
import org.sagebionetworks.workflow.activity.Crawling;
import org.sagebionetworks.workflow.activity.SimpleObserver;

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
 * 
 */
public class TcgaWorkflowInitiator {

	private static final String TCGA_REPOSITORY = "http://tcga-data.nci.nih.gov/tcgafiles/ftp_auth/distro_ftpusers/anonymous/tumor/";
	private static final Logger log = Logger
			.getLogger(TcgaWorkflowInitiator.class.getName());

	class ArchiveObserver implements SimpleObserver<String> {
		String datasetName;
		Integer datasetId;

		ArchiveObserver(String datasetName, String datasetId) {
			this.datasetName = datasetName;
			this.datasetId = new Integer(datasetId);
		}

		public void update(String url) throws Exception {

			// We are only interested in archives that do not contain images

			// TODO configurable includes/excludes for what to pull down
			if (url.endsWith("tar.gz") && (0 > url.indexOf("image"))) {

				if (url
						.matches(".*jhu-usc.edu_COAD.HumanMethylation27.Level_2.8.0.0.*")) {
				
//				if (url
//						.matches(".*jhu-usc.edu_COAD.HumanMethylation27.Level_3.8.0.0.*")) {
				
//				if (url
//						.matches(".*broad.mit.edu_COAD.Genome_Wide_SNP_6.Level_3.76.*")) {
				
//				// This one has an MD5 failure because two different urls have the same filename
//				if (url
//						.matches(".*bcgsc.ca_COAD.IlluminaGA_miRNASeq.Level_3.1.0.0.*")) {
				
//				if (url
//						.matches(".*jhu-usc.edu_COAD.HumanMethylation27.Level_1.4.0.0.*")) {
				
//				if (url
//						.matches(".*bcgsc.ca_COAD.IlluminaGA_miRNASeq.*")) {
				
//				if (url
//						.matches(".*unc.edu_COAD.AgilentG4502A_07_3.Level_3.1.5.0.*")) {
				
//				if (url
//						.matches(".*unc.edu_COAD.AgilentG4502A_07_3.Level_1.1.4.0.*")) {
					TcgaWorkflow.doWorkflow("Workflow for TCGA Dataset "
							+ datasetName, datasetId, url);
					log.info("Kicked off workflow for " + url);
				}

			}
		}
	}

	class DatasetObserver implements SimpleObserver<String> {
		Synapse synapse;

		DatasetObserver() throws Exception {
			synapse = ConfigHelper.createConfig().createSynapseClient();
		}

		public void update(String url) throws Exception {

			String urlComponents[] = url.split("/");

			String datasetName = urlComponents[urlComponents.length - 1];

			JSONObject results = synapse
					.query("select * from dataset where dataset.name == '"
							+ datasetName + "'");

			int numDatasetsFound = results.getInt("totalNumberOfResults");
			if (0 == numDatasetsFound) {
				// If Synapse doesn't have a dataset for it, skip it
				log.debug("Skipping dataset " + datasetName + " at url " + url);
			} else {
				JSONObject datasetQueryResult = results.getJSONArray("results")
						.getJSONObject(0);

				log.debug("Crawling dataset " + datasetName + "("
						+ datasetQueryResult.getString("dataset.id")
						+ ") at url " + url);
				Crawling archiveCrawler = new Crawling();
				ArchiveObserver observer = new ArchiveObserver(datasetName,
						datasetQueryResult.getString("dataset.id"));
				archiveCrawler.addObserver(observer);
				archiveCrawler.doCrawl(url, true);
			}
		}
	}

	/**
	 * Crawl all top level TCGA datasets and identify the ones in which we are
	 * interested
	 */
	void initiateWorkflowTasks() throws Exception {
		Crawling datasetsCrawler = new Crawling();
		DatasetObserver observer = new DatasetObserver();
		datasetsCrawler.addObserver(observer);
		datasetsCrawler.doCrawl(TCGA_REPOSITORY, false);
	}

	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		ConfigHelper configHelper = ConfigHelper.createConfig();

		// Running Annotation Processor is very important, otherwise it will
		// treat Workflows and Activities as regular java method calls
		ActivityAnnotationProcessor.processAnnotations();

		// Create the client for Simple Workflow Service
		AmazonSimpleWorkflow swfService = configHelper.createSWFClient();
		AsyncWorkflowStartContext.initialize(swfService);

		TcgaWorkflowInitiator initiator = new TcgaWorkflowInitiator();
		initiator.initiateWorkflowTasks();

		System.exit(0);
	}

}
