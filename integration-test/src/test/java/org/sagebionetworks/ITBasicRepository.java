package org.sagebionetworks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.junit.BeforeClass;
import org.junit.Test;
import org.sagebionetworks.web.server.servlet.UserDataProvider;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

/**
 * TODO instead use the Synapse Java Client for this sort of testing
 * 
 */
public class ITBasicRepository {

	private static Logger log = Logger.getLogger(ITBasicRepository.class
			.getName());
	private static List<String> urlsToTest = new ArrayList<String>();
	private static String repoBaseUrl = null;
	private static RestTemplate template;

	/**
	 * 
	 */
	@BeforeClass
	public static void beforeClass() {
		// Load the required system properties
		repoBaseUrl = StackConfiguration.getRepositoryServiceEndpoint();
		assertNotNull(
				"Failed to find the system property for repository service base url",
				repoBaseUrl);
		log.info("Loaded system property: " + repoBaseUrl);
		urlsToTest.add("/dataset?sort=name&limit=3");
		template = new RestTemplate();
	}

	/**
	 * @throws UnsupportedEncodingException 
	 * 
	 */
	@Test
	public void testAllURLs() throws UnsupportedEncodingException {
		System.out.println("Starting the test...");
		// run each url on the list
		for (String suffix : urlsToTest) {
			String url = repoBaseUrl + suffix;
			System.out.println("Testing url: " + url);
			HttpHeaders headers = new HttpHeaders();
			String userId = URLEncoder.encode("admin", "UTF-8");
			headers.add(UserDataProvider.SESSION_TOKEN_KEY, userId);
			headers.setContentType(MediaType.APPLICATION_JSON);
			HttpEntity<String> entity = new HttpEntity<String>("", headers);
			ResponseEntity<Object> response = template.exchange(url,
					HttpMethod.GET, entity, Object.class);
			assertNotNull(response);
			response.getStatusCode();
			System.out.println(response.getBody());
			assertEquals(HttpStatus.OK, response.getStatusCode());
		}
	}

}
