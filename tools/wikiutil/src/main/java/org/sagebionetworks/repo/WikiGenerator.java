package org.sagebionetworks.repo;

import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.log4j.Logger;
import org.json.JSONObject;
import org.sagebionetworks.client.Synapse;

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
 */
public class WikiGenerator {

	/**
	 * Expose the prefix so that if/when this is used as a test, assertions know
	 * what to look for
	 */
	public static final String ERROR_PREFIX = "WikiGenerator Failure: ";

	private static final Logger log = Logger.getLogger(WikiGenerator.class
			.getName());

	private static final int JSON_INDENT = 2;
	private static final String LOGIN_URI = "/session";
	private static final String LOGIN_REQUEST_ENTITY = "{\"email\":\"me@myEmail.com\", \"password\":\"thisIsAFakePassword\"}";
	private static final String LOGIN_RESPONSE_ENTITY = "{\"displayName\":\"MyFirstName MyLastName\",\"sessionToken\":\"XXXXXXXXXXXX\"}";

	private Synapse synapse;
	private String repoEndpoint;
	private String repoLocation;
	private String repoPrefix;
	private String authEndpoint;
	private String authLocation;
	private String authPrefix;
	private String username;
	private String password;

	private int numErrors = 0;

	private static final Map<String, String> defaultGETDELETEHeaders;
	private static final Map<String, String> defaultPOSTPUTHeaders;
	static {
		Map<String, String> readOnlyHeaders = new HashMap<String, String>();
		readOnlyHeaders.put("Accept", "application/json");
		readOnlyHeaders.put("sessionToken", "YourSessionToken");
		defaultGETDELETEHeaders = Collections.unmodifiableMap(readOnlyHeaders);
		Map<String, String> readWriteHeaders = new HashMap<String, String>();
		readWriteHeaders.putAll(readOnlyHeaders);
		readWriteHeaders.put("Content-Type", "application/json");
		defaultPOSTPUTHeaders = Collections.unmodifiableMap(readWriteHeaders);
	}

	/**
	 * @param args
	 * @return WikiGenerator
	 * @throws Exception
	 */
	public static WikiGenerator createWikiGeneratorFromArgs(String args[])
			throws Exception {

		Options options = new Options();
		options
				.addOption(
						"e",
						"repoEndpoint",
						true,
						"the repository service endpoint (e.g. https://repositoryservice.sagebase.org/repo/v1)");
		options
				.addOption(
						"a",
						"authEndpoint",
						true,
						"the authentication service endpoint (e.g. https://staging-auth.elasticbeanstalk.com/auth/v1)");
		options.addOption("u", "username", true,
				"the Synapse username (e.g. first.last@sagebase.org)");
		options.addOption("p", "password", true, "the Synapse password");
		options.addOption("h", "help", false, "print this usage message");

		String repoEndpoint = null;
		String authEndpoint = null;
		String username = null;
		String password = null;

		try {
			CommandLineParser parser = new PosixParser();
			CommandLine line = parser.parse(options, args);

			if (line.hasOption("help")) {
				HelpFormatter helpFormatter = new HelpFormatter();
				helpFormatter.setWidth(80);
				helpFormatter.printHelp("wiki generator tool",
						"how to use this tool", options, "have fun!");
				System.exit(0);
			}

			if (line.hasOption("repoEndpoint")
					&& line.hasOption("authEndpoint")
					&& line.hasOption("username") && line.hasOption("password")) {
				repoEndpoint = line.getOptionValue("repoEndpoint");
				authEndpoint = line.getOptionValue("authEndpoint");
				username = line.getOptionValue("username");
				password = line.getOptionValue("password");
			} else {
				throw new ParseException("missing required arguments");
			}
		} catch (ParseException exp) {
			HelpFormatter helpFormatter = new HelpFormatter();
			helpFormatter.setWidth(80);
			helpFormatter.printHelp("wiki generator tool", exp.getMessage(),
					options, "have fun!");
			System.exit(1);
		}

		return new WikiGenerator(repoEndpoint, authEndpoint, username, password);
	}
	
	public Synapse getClient() {return synapse;}

	/**
	 * @param repoEndpoint
	 * @param authEndpoint
	 * @param username
	 * @param password
	 * @throws Exception
	 */
	public WikiGenerator(String repoEndpoint, String authEndpoint,
			String username, String password) throws Exception {
		this.repoEndpoint = repoEndpoint;
		URL parsedRepoEndpoint = new URL(repoEndpoint);
		repoPrefix = parsedRepoEndpoint.getPath();
		repoLocation = repoEndpoint.substring(0, repoEndpoint.length()
				- repoPrefix.length());

		this.authEndpoint = authEndpoint;
		URL parsedAuthEndpoint = new URL(authEndpoint);
		authPrefix = parsedAuthEndpoint.getPath();
		authLocation = authEndpoint.substring(0, authEndpoint.length()
				- authPrefix.length());

		this.username = username;
		this.password = password;

		synapse = new Synapse();
		synapse.setRepositoryEndpoint(this.repoEndpoint);
		synapse.setAuthEndpoint(this.authEndpoint);
	}

	/**
	 * Document logging into Synapse
	 * 
	 * Dev Note: This method is special, we do not want to show actual passwords
	 * and sessionTokens so we are hardcoding some of the wiki content
	 * 
	 * @param wikiHeading
	 * @param wikiDetails
	 * @throws Exception
	 */
	public void doLogin(String wikiHeading, String wikiDetails)
			throws Exception {
		log.info("");
		log.info("");
		log.info(wikiHeading);
		log.info(wikiDetails);
		log.info("");

		Map<String, String> requestHeaders = new HashMap<String, String>();
		requestHeaders.putAll(defaultPOSTPUTHeaders);

		JSONObject requestEntity = new JSONObject(LOGIN_REQUEST_ENTITY);
		JSONObject responseEntity = new JSONObject(LOGIN_RESPONSE_ENTITY);

		String curl = "curl -i ";
		for (Entry<String, String> header : requestHeaders.entrySet()) {
			curl += " -H " + header.getKey() + ":" + header.getValue();
		}
		curl += " -d '" + requestEntity.toString(JSON_INDENT) + "' "
				+ authEndpoint + LOGIN_URI + "{code}";
		log.info("*Request* {code}" + curl);
		log.info("*Response* {code}");

		try {
			synapse.login(username, password);
			log.info(responseEntity.toString(JSON_INDENT) + "{code}");
		} catch (Exception e) {
			numErrors++;
			log.info(ERROR_PREFIX, e);
			log.info("{code}");
			// TODO maybe we only want to fail the wikigenerator for some
			// exceptions, right now we are failing for all
			throw e;
		}
	}

	/**
	 * @param uri
	 * @param wikiHeading
	 * @param wikiDetails
	 * @return the retrieved entity
	 * @throws Exception
	 */
	public JSONObject doGet(String uri, String wikiHeading, String wikiDetails)
			throws Exception {
		log.info("");
		log.info("");
		log.info(wikiHeading);
		log.info(wikiDetails);
		log.info("");

		URL requestUrl;
		if (uri.startsWith(repoPrefix)) {
			requestUrl = new URL(repoLocation + uri);
		} else {
			requestUrl = new URL(repoEndpoint + uri);
		}

		Map<String, String> requestHeaders = new HashMap<String, String>();
		requestHeaders.putAll(defaultGETDELETEHeaders);

		String curl = "curl -i ";
		for (Entry<String, String> header : requestHeaders.entrySet()) {
			curl += " -H " + header.getKey() + ":" + header.getValue();
		}
		curl += " '" + requestUrl + "'{code}";
		log.info("*Request* {code}" + curl);
		log.info("*Response* {code}");

		try {
			JSONObject results = synapse.getEntity(uri);
			log.info(results.toString(JSON_INDENT) + "{code}");
			return results;
		} catch (Exception e) {
			numErrors++;
			log.info(ERROR_PREFIX, e);
			log.info("{code}");
			// TODO maybe we only want to fail the wikigenerator for some
			// exceptions, right now we are failing for all
			throw e;
		}
	}

	/**
	 * @param uri
	 * @param entity
	 * @param wikiHeading
	 * @param wikiDetails
	 * @return the updated entity
	 * @throws Exception
	 */
	public JSONObject doPost(String uri, JSONObject entity, String wikiHeading,
			String wikiDetails) throws Exception {
		log.info("");
		log.info("");
		log.info(wikiHeading);
		log.info(wikiDetails);
		log.info("");

		URL requestUrl;
		if (uri.startsWith(repoPrefix)) {
			requestUrl = new URL(repoLocation + uri);
		} else {
			requestUrl = new URL(repoEndpoint + uri);
		}

		Map<String, String> requestHeaders = new HashMap<String, String>();
		requestHeaders.putAll(defaultPOSTPUTHeaders);

		String curl = "curl -i ";
		for (Entry<String, String> header : requestHeaders.entrySet()) {
			curl += " -H " + header.getKey() + ":" + header.getValue();
		}
		curl += " -d '" + entity.toString(JSON_INDENT) + "' " + requestUrl
				+ "{code}";
		log.info("*Request* {code}" + curl);
		log.info("*Response* {code}");

		try {
			JSONObject results = synapse.createEntity(uri, entity);
			log.info(results.toString(JSON_INDENT) + "{code}");
			return results;
		} catch (Exception e) {
			numErrors++;
			log.info(ERROR_PREFIX, e);
			log.info("{code}");
			// TODO maybe we only want to fail the wikigenerator for some
			// exceptions, right now we are failing for all
			throw e;
		}
	}

	/**
	 * @param uri
	 * @param entity
	 * @param wikiHeading
	 * @param wikiDetails
	 * @return the updated entity
	 * @throws Exception
	 */
	public JSONObject doPut(String uri, JSONObject entity, String wikiHeading,
			String wikiDetails) throws Exception {
		log.info("");
		log.info("");
		log.info(wikiHeading);
		log.info(wikiDetails);
		log.info("");

		URL requestUrl;
		if (uri.startsWith(repoPrefix)) {
			requestUrl = new URL(repoLocation + uri);
		} else {
			requestUrl = new URL(repoEndpoint + uri);
		}
		Map<String, String> requestHeaders = new HashMap<String, String>();
		requestHeaders.putAll(defaultPOSTPUTHeaders);
		requestHeaders.put("ETag", entity.getString("etag"));

		String curl = "curl -i ";
		for (Entry<String, String> header : requestHeaders.entrySet()) {
			curl += " -H " + header.getKey() + ":" + header.getValue();
		}
		curl += " -X PUT -d '" + entity.toString(JSON_INDENT) + "' "
				+ requestUrl + "{code}";
		log.info("*Request* {code}" + curl);
		log.info("*Response* {code}");

		try {
			JSONObject results = synapse.putEntity(uri, entity);
			log.info(results.toString(JSON_INDENT) + "{code}");
			return results;
		} catch (Exception e) {
			numErrors++;
			log.info(ERROR_PREFIX, e);
			log.info("{code}");
			// TODO maybe we only want to fail the wikigenerator for some
			// exceptions, right now we are failing for all
			throw e;
		}
	}

	/**
	 * @param uri
	 * @param wikiHeading
	 * @param wikiDetails
	 * @throws Exception
	 */
	public void doDelete(String uri, String wikiHeading, String wikiDetails)
			throws Exception {
		log.info("");
		log.info("");
		log.info(wikiHeading);
		log.info(wikiDetails);
		log.info("");

		URL requestUrl;
		if (uri.startsWith(repoPrefix)) {
			requestUrl = new URL(repoLocation + uri);
		} else {
			requestUrl = new URL(repoEndpoint + uri);
		}

		Map<String, String> requestHeaders = new HashMap<String, String>();
		requestHeaders.putAll(defaultGETDELETEHeaders);
		String curl = "curl -i ";
		for (Entry<String, String> header : requestHeaders.entrySet()) {
			curl += " -H " + header.getKey() + ":" + header.getValue();
		}
		curl += " -X DELETE " + requestUrl + "{code}";
		log.info("*Request* {code}" + curl);
		log.info("*Response* {code}");

		try {
			synapse.deleteEntity(uri);
			log.info("{code}");
			return;
		} catch (Exception e) {
			numErrors++;
			log.info(ERROR_PREFIX, e);
			log.info("{code}");
			// TODO maybe we only want to fail the wikigenerator for some
			// exceptions, right now we are failing for all
			throw e;
		}
	}

	/**
	 * @return the numErrors
	 */
	public int getNumErrors() {
		return numErrors;
	}

}
