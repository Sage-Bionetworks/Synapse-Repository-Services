package org.sagebionetworks.tool.migration;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.sagebionetworks.authutil.CrowdAuthUtil;
import org.sagebionetworks.authutil.User;
import org.sagebionetworks.client.HttpClientProvider;
import org.sagebionetworks.client.HttpClientProviderImpl;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.repo.model.PrincipalBackup;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.tool.migration.dao.EntityData;
import org.sagebionetworks.utils.HttpClientHelperException;

import com.thoughtworks.xstream.XStream;

/**
 * 
 * This class is specifically for retrieving user groups and user profiles from pre-0.13 
 * versions of Synapse and will be deprecated once the transition to 0.13 is done
 * 
 * To use:
 * - login
 * - getPrincipalData() gives all the principal IDs in the system
 * - getPrincipalData() gives the IDs and etags for all the principals in the system
 * - writePrincipalBackups() serializes a collection of principal backup objects and writes to a file
 * 
 * @author brucehoff
 *
 */
public class PrincipalRetriever012 {
	private  HttpClientProvider clientProvider = new HttpClientProviderImpl();
	private String authEndpoint;
	private String repoEndpoint;
	private String sessionToken;
	
	public PrincipalRetriever012(String authEndpoint, String repoEndpoint) {
		this.authEndpoint = authEndpoint;
		this.repoEndpoint = repoEndpoint;
	}

	private String get(String requestUrl) throws ClientProtocolException, IOException, HttpClientHelperException, JSONException, SynapseException, NotFoundException {
		String requestMethod = "GET";
		String requestContent = null; // for a GET there's no body
		Map<String,String> requestHeaders = new HashMap<String,String>();
		requestHeaders.put("sessionToken", sessionToken);
		requestHeaders.put("Accept", "application/json");
		HttpResponse response = null;
		try {
			response = clientProvider.performRequest(repoEndpoint+requestUrl, requestMethod, requestContent, requestHeaders);
		} catch (HttpClientHelperException e) {
			if(e.getHttpStatus()==HttpStatus.SC_NOT_FOUND) throw new NotFoundException(e.getMessage());
		}
		return EntityUtils.toString(response.getEntity());
	}
	
	private JSONObject post(String requestUrl, String requestContent) throws ClientProtocolException, IOException, HttpClientHelperException, JSONException {
		String requestMethod = "POST";
		Map<String,String> requestHeaders = new HashMap<String,String>();
		requestHeaders.put("sessionToken", sessionToken);
		requestHeaders.put("Content-Type", "application/json");
		requestHeaders.put("Accept", "application/json");
		HttpResponse response = clientProvider.performRequest(authEndpoint+requestUrl, requestMethod, requestContent, requestHeaders);
		String responseString = EntityUtils.toString(response.getEntity());
		return new JSONObject(responseString);
	}
	
	private static UserGroup groupFromJSON(JSONObject groupJson, boolean individualExpected) {
		try {
			UserGroup group = new UserGroup();
			group.setId(groupJson.getString("id"));
			group.setName(groupJson.getString("name"));
			String creationDateString = groupJson.getString("creationDate");
			group.setCreationDate(creationDateString==null || creationDateString.equals("null") ? null : new Date(Long.parseLong(creationDateString)));
			boolean isIndividual = groupJson.getBoolean("individual");
			if (individualExpected != isIndividual) throw new RuntimeException("isIndividual="+isIndividual+" for "+group.getName());
			group.setIsIndividual(isIndividual);
			return group;
		} catch (Exception e) {
			throw new RuntimeException(groupJson.toString(), e);
		}
	}
	
	private static UserProfile profileFromJSON(JSONObject json) throws JSONException {
		UserProfile profile = new UserProfile();
		profile.setDisplayName(json.getString("displayName"));
		profile.setEtag(json.getString("etag"));
		profile.setFirstName(json.getString("firstName"));
		profile.setLastName(json.getString("lastName"));
		profile.setOwnerId(json.getString("ownerId"));
		if (json.has("rStudioUrl")) profile.setRStudioUrl(json.getString("rStudioUrl"));
		if (json.has("userName")) profile.setUserName(json.getString("userName"));
		return profile;
	}
	
	public void login(String user, String pw) throws ClientProtocolException, IOException, HttpClientHelperException, JSONException {
		JSONObject reqBody = new JSONObject();
		reqBody.put("email", user);
		reqBody.put("password", pw);
		JSONObject response = post("/session", reqBody.toString());
		sessionToken = response.getString("sessionToken");
	}
	
	// this is duplicated from NodeSerializerUtil in the repo-svs project
	// duplication is OK since this class will be removed after the 0.12 migration
	public static void writePrincipalBackups(Collection<PrincipalBackup> principalBackups, File file) throws IOException {
		OutputStream out = new FileOutputStream(file);
		OutputStreamWriter writer = new OutputStreamWriter(out);
		XStream xstream = new XStream();
		xstream.toXML(principalBackups, writer);
	}

	
	public static void main(String[] args) throws Exception {
		if (args.length<2) throw new Exception("Need to specify admin user and password");
		PrincipalRetriever012 pr = new PrincipalRetriever012("https://auth-prod.sagebase.org", "https://repo-prod.sagebase.org");
		pr.login(args[0], args[1]);
		Collection<PrincipalBackup> pbs = pr.getPrincipals();
		System.out.println("backed up "+pbs.size()+" principals.");
	}
	
	public Collection<EntityData> getPrincipalData() throws ClientProtocolException, JSONException, IOException, HttpClientHelperException, SynapseException, NotFoundException {
		List<EntityData> ans = new ArrayList<EntityData>();
		
		JSONArray groupArray = new JSONArray(get("/userGroup"));
		for (int i=0; i<groupArray.length(); i++) {
			UserGroup group = groupFromJSON((JSONObject)groupArray.get(i), false/*individualExpected*/);
			ans.add(new EntityData(group.getId(), "0", null));
		}
		JSONArray userArray = new JSONArray(get("/user"));
		for (int i=0; i<userArray.length(); i++) {
			UserGroup group = groupFromJSON((JSONObject)userArray.get(i), true/*individualExpected*/);
			String etag = null;
			try {
				JSONObject profileJson = new JSONObject(get("/userProfile/"+group.getId()));
				UserProfile userProfile = profileFromJSON(profileJson);
				etag = userProfile.getEtag();
			} catch (NotFoundException e) {
				etag = null;
			}
			ans.add(new EntityData(group.getId(), etag, null));
		}
		
		return ans;
		
	}
	
	/**
	 * use the pre-0.13 API to get the users and user profiles
	 * @return
	 * @throws HttpClientHelperException
	 * @throws IOException
	 * @throws JSONException
	 * @throws NotFoundException 
	 */
	public Collection<PrincipalBackup> getPrincipals() throws HttpClientHelperException, IOException, JSONException, SynapseException, NotFoundException {
		List<PrincipalBackup> ans = new ArrayList<PrincipalBackup>();
		
		JSONArray groupArray = new JSONArray(get("/userGroup"));
		for (int i=0; i<groupArray.length(); i++) {
			UserGroup group = groupFromJSON((JSONObject)groupArray.get(i), false/*individualExpected*/);
			PrincipalBackup pb = new PrincipalBackup();
			pb.setUserGroup(group);
			ans.add(pb);
		}
		JSONArray userArray = new JSONArray(get("/user"));
		for (int i=0; i<userArray.length(); i++) {
			UserGroup group = groupFromJSON((JSONObject)userArray.get(i), true/*individualExpected*/);
			PrincipalBackup pb = new PrincipalBackup();
			pb.setUserGroup(group);
			ans.add(pb);
			UserProfile userProfile = new UserProfile();
			userProfile.setOwnerId(group.getId());
			userProfile.setEtag("0");
			JSONObject profileJson = null;
			try {
				profileJson = new JSONObject(get("/userProfile/"+group.getId()));
				userProfile = profileFromJSON(profileJson);
			} catch (NotFoundException e) {
				// Create a profile from the information in Crowd
				try {
					User crowdUserData = CrowdAuthUtil.getUser(group.getName());
					userProfile.setDisplayName(crowdUserData.getDisplayName());
					userProfile.setFirstName(crowdUserData.getFirstName());
					userProfile.setLastName(crowdUserData.getLastName());
					userProfile.setOwnerId(group.getId());
					//System.out.println("Can't find user profile for "+group.getName()+".   Created new profile.");
				} catch (Exception e2) {
					//System.out.println("Can't find or create user profile for "+group.getName()+".");
				}
			} catch (JSONException e) {
				//System.out.println("Error extracting fields from user profile for "+group.getName()+". JSON: "+profileJson);
			}
			pb.setUserProfile(userProfile);
		}
		
		return ans;
	}

}
