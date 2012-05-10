package org.sagebionetworks.client;

import static org.apache.commons.lang.StringEscapeUtils.escapeHtml;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.sagebionetworks.repo.model.Data;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.search.query.SearchQuery;

/**
 * @author deflaux
 * 
 */
public class EntityDocumentation {

	private static final Logger log = Logger
			.getLogger(EntityDocumentation.class.getName());

	public static final String QUERY_SYNTAX = "SELECT <one or more comma-separated field names> FROM <data type> WHERE <expression> (AND <expression>)* (LIMIT <#>) (OFFSET #)\n\n"
			+ "<expression> := <field name> <operator> <value>\n"
			+ " where <field name> is a primary field or an annotation name.\n"
			+ "<value> should be in quotes for strings, but not numbers (i.e. name == \"Smith\" AND size > 10). "
			+ "Dates are in milliseconds since Jan 1, 1970.\n\n"
			+ "Curently supported <operators> with their required URL escape codes:\n"
			+ "||Operator ||Value|| URL Escape Code \n"
			+ "| Equal| == | %3D%3D|\n"
			+ "| Does Not equal| != | !%3D|\n"
			+ "| Greater Than | > | %3E |\n"
			+ "| Less than | < | %3C |\n"
			+ "| Greater than or equals | >= | %3E%3D |\n"
			+ "| Less than or equals | <= | %3C%3D |\n"
			+ "Note: 'OFFSET' starts at 1.";

	public static final Object UPLOAD_DESCRIPTION = "<p>Note that data uploads have three steps"
			+ "<ol>"
			+ "<li>First get an s3Token (with includes a presigned URL suitable for a PUT of your file and accessKey/secretKey/securityToken for use with a multipart upload tool)"
			+ "<li>Upload the data file to S3"
			+ "<li>Inform Synapse of the new S3 location" 
			+ "</ol>";

	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {

		SynapseRESTDocumentationGenerator synapse = SynapseRESTDocumentationGenerator
				.createFromArgs(args);

		log.info("<h1>CRUD Examples</h1>");

		log.info("<h2>Get a publicy readable entity</h2>");
		String entityId = "syn4494";
		synapse.getEntityById(entityId);

		log.info("<h2>Log in</h2>");
		synapse.login();

		log.info("<h2>Create a Project entity</h2>");
		Project project = new Project();
		project.setName("REST API Documentation Project - "
				+ DateTime.now().toString(
						DateTimeFormat.forPattern("dd-MMM-yyyy HH-mm")));
		project
				.setDescription("A project created to help illustrate the use of the Synapse Repository Service API");
		project = synapse.createEntity(project);

		log.info("<h2>Create a Data entity</h2>");
		Data data = new Data();
		data.setParentId(project.getId());
		data.setDisease("Cancer");
		data.setPlatform("moe430a");
		data.setSpecies("Homo sapien");
		data.setTissueType("Adipose");
		data = synapse.createEntity(data);

		log.info("<h2>Upload the actual data to S3</h2>");
		log.info(UPLOAD_DESCRIPTION);
		data = (Data) synapse.uploadLocationableToSynapse(data,
				SynapseRESTDocumentationGenerator
						.createTempFile("first version of the data"));

		log.info("<h2>Update an entity</h2>");
		data.setNumSamples(256L);
		data = synapse.putEntity(data);

		log
				.info("<h2>Update Data</h2> Note that the version number was automatically bumped when the md5 was changed<p>");
		log.info(UPLOAD_DESCRIPTION);
		data = (Data) synapse
				.uploadLocationableToSynapse(
						data,
						SynapseRESTDocumentationGenerator
								.createTempFile("this is the second version of the data"));

		log.info("<h2>Delete an entity</h2>");
		synapse.deleteEntity(project);

		log.info("<h1>Search and Query Examples</h1>");

		log.info("<h2>Query for entities</h2>");
		log
				.info("Query Syntax: <pre>" + escapeHtml(QUERY_SYNTAX)
						+ "</pre><p>");
		// TODO change this to the JSONEntity version of QueryResults when its
		// available, but for now we
		// know what the synapse id is, so just hard code it instead of looking
		// in the query results returned
		synapse
				.query("select id, name from study where name == \"MSKCC Prostate Cancer\"");

		log.info("<h2>Search for entities</h2>");
		log
				.info("See <a href=\"http://aws.amazon.com/cloudsearch/\">CloudSearch</a> documentation for search syntax<p>");
		SearchQuery searchQuery = new SearchQuery();
		List<String> queryTerms = new ArrayList<String>();
		queryTerms.add("cancer");
		searchQuery.setQueryTerm(queryTerms);
		List<String> returnFields = new ArrayList<String>();
		returnFields.add("id");
		returnFields.add("name");
		searchQuery.setReturnFields(returnFields);
		synapse.search(searchQuery);
	}
}
