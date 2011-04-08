package org.sagebionetworks.repo;

import org.apache.log4j.Logger;
import org.json.JSONObject;

/**
 * WikiGenerator is used to auto-generate the wiki for the Platform Repository
 * Service.
 * 
 * All the log output goes to stdout. See generateRepositoryServiceWiki.sh for
 * how the log output is cleaned and turned into an actual wiki page. The reason
 * why this writes log output file instead of a normal output to stdout is
 * because I want to include the response headers logged by HttpClient and this
 * was a quick way to make that happen.
 * 
 * Also note that I originally wrote this against HtmlUnit so that I could
 * include a bit of testing to make sure the responses coming back were sane,
 * but HtmlUnit does not support PUT or DELETE.
 * 
 * {code} svn checkout
 * https://sagebionetworks.jira.com/svn/PLFM/trunk/tools/wikiutil cd wikiutil
 * ~/platform/trunk/tools/wikiutil>mvn clean compile
 * ~/platform/trunk/tools/wikiutil>./generateRepositoryServiceWiki.sh
 * http://localhost:8888 > wiki.txt {code}
 * 
 */
public class CRUDWikiGenerator {

	private static final Logger log = Logger.getLogger(WikiGenerator.class
			.getName());

	private static String serviceEndpoint = "http://localhost:8888";

	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {

		if (1 <= args.length) {
			serviceEndpoint = args[0];
		}

		WikiGenerator wiki = new WikiGenerator(serviceEndpoint);

		log.info("h1. REST API Examples (Create/Update/Delete)");
		JSONObject dataset = wiki
				.doPost(
						"/repo/v1/dataset",
						new JSONObject(
								"{\"status\": \"Pending\", \"description\": \"Genetic and epigenetic alterations have been identified that ...\", \"creator\": \"Charles Sawyers\", \"releaseDate\": \"2008-09-14\", \"version\": \"1.0.0\", \"name\": \"MSKCC Prostate Cancer\"}"),
						"h2. Create a Dataset", "TODO content type");

		dataset.put("status", "Current");
		wiki.doPut(dataset.getString("uri"), dataset, "h2. Update a Dataset",
				"TODO\n" + "* ETag\n" + "* use id returned\n"
						+ "* which field I changed -> status");

		wiki.doDelete(dataset.getString("uri"), "h2. Delete a Dataset",
				"TODO\n" + "* ETag\n" + "* use id returned\n"
						+ "* which field I changed -> status");
	}
}