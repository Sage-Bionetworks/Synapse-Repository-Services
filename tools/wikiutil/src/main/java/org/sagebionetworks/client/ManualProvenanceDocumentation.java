package org.sagebionetworks.client;

import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.sagebionetworks.repo.model.Analysis;
import org.sagebionetworks.repo.model.Data;
import org.sagebionetworks.repo.model.GenericData;
import org.sagebionetworks.repo.model.PhenotypeData;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.Reference;
import org.sagebionetworks.repo.model.Step;

/**
 * @author deflaux
 * 
 */
public class ManualProvenanceDocumentation {

	private static final Logger log = Logger
			.getLogger(ManualProvenanceDocumentation.class.getName());

	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {

		SynapseRESTDocumentationGenerator synapse = SynapseRESTDocumentationGenerator
				.createFromArgs(args);

		log.info("<h1>Manual Provenance Example</h1>");

		log.info("<h2>Log in</h2>");
		synapse.login();

		log.info("<h2>Create a Project entity</h2>");
		Project project = new Project();
		project.setName("REST API Provenance Documentation Project - "
				+ DateTime.now().toString(
						DateTimeFormat.forPattern("dd-MMM-yyyy HH-mm")));
		project
				.setDescription("A project created to help illustrate the use of provenance in the Synapse Repository Service API");
		project = synapse.createEntity(project);

		log.info("<h2>Create an Analysis entity</h2>");
		Analysis analysis = new Analysis();
		analysis.setName("Analysis of ");
		analysis.setParentId(project.getId());
		analysis = synapse.createEntity(analysis);

		log.info("<h2>Start the Step</h2>");
		Step step = new Step();
		step.setName("Clinical Variable Normalization");
		step.setParentId(analysis.getId());
		step.setStartDate(DateTime.now().toDate());
		step
				.setDescription("The first step in this analysis is to normalize the clinical variables");
		step = synapse.createEntity(step);

		log
				.info("<h2>Pull down the curated clinical data as input to this step in the analysis where we QC the clinical data</h2>");
		String entityId = "syn4635";
		//PhenotypeData input = (PhenotypeData) synapse.getEntityById(entityId);
		Data input = (Data)synapse.getEntityById(entityId);

		// TODO actually download the data from S3 and QC it?

		log
				.info("<h2>Update the step to reflect the input to your analysis</h2>");
		Reference inputReference = new Reference();
		inputReference.setTargetId(input.getId());
		inputReference.setTargetVersionNumber(input.getVersionNumber());
		Set<Reference> inputSet = new HashSet<Reference>();
		inputSet.add(inputReference);
		step.setInput(inputSet);
		step = synapse.putEntity(step);

		log
				.info("<h2>Do your analysis</h2><p>Imagine some code was run to tranform the input data to the output data . . .");

		log.info("<h2>Create an entity for the new QC-ed clinical data</h2>");
		PhenotypeData output = new PhenotypeData();
		output.setParentId(project.getId());
		output.setDisease(input.getDisease());
		output.setSpecies(input.getSpecies());
		output = synapse.createEntity(output);

		log.info("<h2>Upload the QC-ed clinical data</h2>");
		log.info(EntityDocumentation.UPLOAD_DESCRIPTION);
		output = (PhenotypeData) synapse.uploadLocationableToSynapse(output,
				SynapseRESTDocumentationGenerator
						.createTempFile("first version of the data"));

		log
				.info("<h2>Update the step to reflect the output from your analysis</h2>");
		Reference outputReference = new Reference();
		outputReference.setTargetId(output.getId());
		outputReference.setTargetVersionNumber(output.getVersionNumber());
		Set<Reference> outputSet = new HashSet<Reference>();
		outputSet.add(outputReference);
		step.setOutput(outputSet);
		step = synapse.putEntity(step);

		log.info("<h2>End the Step</h2>");
		step.setEndDate(DateTime.now().toDate());
		step = synapse.putEntity(step);
	}
}
