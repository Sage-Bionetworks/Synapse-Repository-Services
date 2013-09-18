package org.sagebionetworks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

/**
 * Test that we can get profile data for a call.
 * @author jmhill
 *
 */
public class ITPerCallProfileTest {
	
	private static final String KEY_PROFILE_REQUEST = "profile_request";
	// The JSON object for the profile results
	public static final String KEY_PROFILE_RESPONSE_OBJECT = "profile_response_object";
	// The profile print string.
	public static final String KEY_PROFILE_RESPONSE_PRINT = "profile_response_print";
	
	// The template used for the tests
	private RestTemplate tempalte;
	String sessionToken;

	private Map<String, String> toDelete;
	/**
	 * @throws Exception
	 * 
	 */
	@Before
	public void before() throws Exception {
		tempalte = new RestTemplate();
		// Create a session token
		sessionToken = getNewSessionToken();
		toDelete = new HashMap<String, String>();
	}
	
	@After
	public void after(){
		if(tempalte != null && sessionToken != null && toDelete!= null){
			Iterator<String> it = toDelete.keySet().iterator();
			while(it.hasNext()){
				String id = it.next();
				String suffix = toDelete.get(id);
				deleteEntity(id, suffix);
			}
		}
	}
	
	@Ignore
	@Test
	public void testPerCallProfileOn() throws JSONException, UnsupportedEncodingException{
		assertNotNull(sessionToken);
		// First get a token
		ResponseEntity<String> response = createProject("ITPerCallProfileTest.testPerCallProfileOn", true);
		// Now get the frame from the header
		System.out.println(response.getHeaders());
		List<String> list = response.getHeaders().get(KEY_PROFILE_RESPONSE_OBJECT);
		assertNotNull(list);
		assertEquals(1, list.size());
		String encoded = list.get(0);
		String decoded = new String(Base64.decodeBase64(encoded.getBytes("UTF-8")), "UTF-8");
		System.out.println("Results: "+decoded);
		JSONObject frame = new JSONObject(decoded);
		assertNotNull(frame.getLong("startTime"));
		assertNotNull(frame.getString("name"));
		assertNotNull(frame.getLong("elapse"));
		
	}

	@Ignore
	@Test
	public void testPerCallProfileOff() throws JSONException, UnsupportedEncodingException{
		assertNotNull(sessionToken);
		ResponseEntity<String> response = createProject("ITPerCallProfileTest.testPerCallProfileOff", false);
		// Now get the frame from the header
		System.out.println(response.getHeaders());
		// The profile data should not be in the header when profiling if off.
		List<String> list = response.getHeaders().get(KEY_PROFILE_RESPONSE_OBJECT);
		assertTrue(list == null);
		
	}
	/**
	 * Helper to create a project
	 * @return
	 * @throws JSONException
	 */
	public ResponseEntity<String> createProject(String name, boolean profile) throws JSONException {
		System.out.println("sessionToken:"+sessionToken);
		// Now profile the creation of a new project
		JSONObject project = new JSONObject("{\"name\":\""+name+"\"}");
		HttpMethod method = HttpMethod.POST;
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		// Only add this to profile.
		if(profile){
			headers.set(KEY_PROFILE_REQUEST, "true");
		}
		headers.set("sessionToken", sessionToken);
		HttpEntity<String> entity = new HttpEntity<String>(project.toString(), headers);
		String suffix = "/project";
		// We have not parameters for this call		
		// Make the actual call.
		ResponseEntity<String> response = tempalte.exchange(StackConfiguration.getRepositoryServiceEndpoint()+suffix, method, entity, String.class, new HashMap<String, String>());
		HttpStatus expected = HttpStatus.CREATED;
		if(response.getStatusCode() != expected){
			throw new IllegalArgumentException("Expected: "+expected+" but was: "+response.getStatusCode());
		}
		JSONObject json = new JSONObject(response.getBody());
		assertNotNull(json);
		String id = json.getString("id");
		assertNotNull(id);
		System.out.println("Results: "+json);
		// Make sure we delete this entity
		toDelete.put(id, suffix);
		return response;
	}
	
	/**
	 * Get a new session token
	 * @return
	 * @throws org.json.JSONException
	 */
	private String getNewSessionToken() throws org.json.JSONException{
		HttpMethod method = HttpMethod.POST;
		// Create the request.
		JSONObject loginRequest = new JSONObject();
		loginRequest.put("email", StackConfiguration.getIntegrationTestUserOneName());
		loginRequest.put("password", StackConfiguration.getIntegrationTestUserOnePassword());		
		// Setup the header
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<String> entity = new HttpEntity<String>(loginRequest.toString(), headers);
		// We have not parameters for this call		
		// Make the actual call.
		ResponseEntity<String> response = tempalte.exchange(StackConfiguration.getAuthenticationServicePrivateEndpoint()+"/session", method, entity, String.class, new HashMap<String, String>());
		HttpStatus expected = HttpStatus.CREATED;
		if(response.getStatusCode() != expected){
			throw new IllegalArgumentException("Expected: "+expected+" but was: "+response.getStatusCode());
		}
		JSONObject json = new JSONObject(response.getBody());
		return json.getString("sessionToken");
	}
	
	/**
	 * Delete entities
	 * @param id
	 * @return
	 */
	public void deleteEntity(String id, String suffix){
		HttpMethod method = HttpMethod.DELETE;
		// Create the request.	
		// Setup the header
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.set("sessionToken", sessionToken);
		HttpEntity<String> entity = new HttpEntity<String>("", headers);
		// Make the call
		tempalte.exchange(StackConfiguration.getRepositoryServiceEndpoint()+suffix+"/"+id, method, entity, null);
	}
	

}
