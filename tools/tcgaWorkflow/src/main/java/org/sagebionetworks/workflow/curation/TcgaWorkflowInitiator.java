package org.sagebionetworks.workflow.curation;

import org.sagebionetworks.workflow.curation.ConfigHelper;

import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflow;
import com.amazonaws.services.simpleworkflow.client.asynchrony.decider.annotations.ActivityAnnotationProcessor;
import com.amazonaws.services.simpleworkflow.client.asynchrony.decider.annotations.AsyncWorkflowStartContext;

/**
 * This is a class just for testing purposes. It will later be replaced by a
 * periodic activity that crawls TCGA and kicks off a workflow for each new data
 * layer found.
 * 
 * @author deflaux
 * 
 */
public class TcgaWorkflowInitiator {

	/**
	 * This is used for launching a Workflow instance of TcgaWorkflow
	 */
	private static AmazonSimpleWorkflow swfService;

	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		ConfigHelper configHelper = ConfigHelper.createConfig(
				"TcgaWorkflowInitiatorMain", args);

		// Running Annotation Processor is very important, otherwise it will
		// treat Workflows and Activities as regular java method calls
		ActivityAnnotationProcessor.processAnnotations();

		// Create the client for Simple Workflow Service
		swfService = configHelper.createSWFClient();

		// Start workflow instance
		AsyncWorkflowStartContext.initialize(swfService);
		TcgaWorkflow
				.doWorkflow(
						"HelloWorkflow",
						7,
						"http://tcga-data.nci.nih.gov/tcgafiles/ftp_auth/distro_ftpusers/anonymous/tumor/coad/cgcc/unc.edu/agilentg4502a_07_3/transcriptome/unc.edu_COAD.AgilentG4502A_07_3.Level_2.2.0.0.tar.gz");

		System.exit(0);
	}

}
