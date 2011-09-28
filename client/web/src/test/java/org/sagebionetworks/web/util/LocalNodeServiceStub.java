package org.sagebionetworks.web.util;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.logging.Logger;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.sagebionetworks.web.client.SearchService;
import org.sagebionetworks.web.server.ColumnConfigProvider;
import org.sagebionetworks.web.server.ServerConstants;
import org.sagebionetworks.web.server.servlet.QueryStringUtils;
import org.sagebionetworks.web.shared.Annotations;
import org.sagebionetworks.web.shared.ColumnInfo;
import org.sagebionetworks.web.shared.Dataset;
import org.sagebionetworks.web.shared.FilterEnumeration;
import org.sagebionetworks.web.shared.HeaderData;
import org.sagebionetworks.web.shared.SearchParameters;
import org.sagebionetworks.web.shared.users.AclPrincipal;
import org.sagebionetworks.web.shared.users.UserLogin;
import org.sagebionetworks.web.shared.users.UserRegistration;
import org.sagebionetworks.web.shared.users.UserSession;

import com.sun.grizzly.http.SelectorThread;
import com.sun.jersey.api.Responses;
import com.sun.jersey.api.container.grizzly.GrizzlyWebContainerFactory;

/**
 * Launches all of the local service stubs.
 * @author jmhill
 *
 */
@Path("/repo/v1")
public class LocalNodeServiceStub {

	private static Logger logger = Logger.getLogger(LocalNodeServiceStub.class.getName());

	
	public static List<AclPrincipal> groups = new ArrayList<AclPrincipal>();
	public static List<AclPrincipal> users = new ArrayList<AclPrincipal>();

	private static Map<String, AclPrincipal> sessions = new LinkedHashMap<String, AclPrincipal>();

	private static List<Dataset> allDataSets = new ArrayList<Dataset>();
	private static Map<String, Annotations> datasetAnnoations = new HashMap<String, Annotations>();
	private static final Random rand = new Random(12345);

	public final static String NULL = "NULL";
	
	private static List<Map<String, Object>> rows = new ArrayList<Map<String, Object>>();
	
	private Properties props;
	private ColumnConfigProvider columnConfigProvider;
	private List<ColumnInfo> validColumns;

	
	public LocalNodeServiceStub() throws IOException {
		
		// Search Service setup
		props = ServerPropertiesUtils.loadProperties();
		String columnConfigFile = props.getProperty(ServerConstants.KEY_COLUMN_CONFIG_XML_FILE);
		// Create the column config from the classpath
		columnConfigProvider = new ColumnConfigProvider(columnConfigFile);
		
		// Load the filter enumeration file
		String filterFile = props.getProperty(ServerConstants.KEY_FILTER_ENUMERATION_CONFIG_XML_FILE);
		columnConfigProvider.setFilterEnumerations(filterFile);
		
		// Determine what the valid columns are
		validColumns = new ArrayList<ColumnInfo>();
		Iterator<String> keyIt = columnConfigProvider.getKeyIterator();
		while(keyIt.hasNext()){
			// We only care about the base ColumnInfo here
			String columnKey = keyIt.next();
			HeaderData header = columnConfigProvider.get(columnKey);
			if(header instanceof ColumnInfo){
				validColumns.add((ColumnInfo)header);
			}
		}

	}
	
	@POST 
	@Consumes("application/json")@Produces("application/json")
	@Path("/user")
	public String createUser(UserRegistration userInfo) {
//		users.add(new AclPrincipal(userInfo.getEmail(), userInfo.getDisplayName(), new Date(), null, null, true));
		return "";
	}
	
	@PUT
	@Consumes("application/json")@Produces("application/json")
	@Path("/user")
	public String updateUser(UserRegistration updatedUser) {
		// For now, there shall be no test users that share the same first/last name combined.
		
		boolean found = false;
		for(AclPrincipal user : users) {
			if(user.getName().equals(updatedUser.getDisplayName())) {
				user = new AclPrincipal(user.getId(), updatedUser.getDisplayName(), null, null, null, true);
				found = true;
				break;
			}
		}
		
		if(!found) {
			throw new BadRequestException();			
		}
		return "";
	}

	@POST
	@Consumes("application/json")@Produces("application/json")
	@Path("/userPasswordEmail")
	public String sendPasswordChangeEmail(@QueryParam("userId") String userId) {		
		return "";
	}
	
	@POST
	@Consumes("application/json")@Produces("application/json")
	@Path("/apiPasswordEmail")
	public String sendSetApiPasswordEmail(@QueryParam("userId") String userId) {
		return "";
	}
		
	@POST
	@Consumes("application/json")@Produces("application/json")	
	@Path("/session")
	public UserSession initiateSession(UserLogin userLogin) throws BadRequestException {
		AclPrincipal foundUser = null;
		String userId = userLogin.getEmail();
		for(AclPrincipal user : users) {
			if(userId.equals(user.getId())) {
				foundUser = user;
				break;
			}			
		}
		
		if(foundUser == null) {
			throw new BadRequestException();
		}
		
		UserSession session = new UserSession();
		session.setDisplayName(foundUser.getName());
		session.setSessionToken(foundUser.toString());
		sessions.put(session.getSessionToken(), foundUser);
		return session;
	}
	
	@PUT
	@Consumes("application/json")@Produces("application/json")
	@Path("/session")
	public String refreshSession(@QueryParam("sessionToken") String token) throws NotFoundException {
		if(!sessions.containsKey(token)) { 
			throw new NotFoundException();
		}
		return "";
	}
	
	@DELETE
	@Consumes("application/json")@Produces("application/json")
	@Path("/session")
	public String terminateSession(@QueryParam("sessionToken") String token) {
		if(sessions.containsKey(token)) { 
			sessions.remove(token);
		}
		return "";
	}
	
	@GET
	@Path("/clear/all")
	public boolean clearAll() {
		sessions.clear();
		users.clear();
		return true;
	}
		
	@GET 
	@Produces("application/json")
	@Path("/user")
	public String getAllUsers() throws JSONException {		
		JSONArray arr = new JSONArray();
		
		for (int i = 0; i < users.size(); i++) {
			JSONObject obj = new JSONObject();
			AclPrincipalTest acl = new AclPrincipalTest();
			AclPrincipal currentUser = users.get(i);
			acl.creationDate = currentUser.getCreationDate().getTime();

			obj.put("name", currentUser.getName());
			obj.put("id", currentUser.getId());
			obj.put("individual", currentUser.isIndividual());
			if(acl.creationDate != null) obj.put("creationDate", acl.creationDate);
			
			arr.put(obj);
		}
		
		return arr.toString();
	}
	
	@GET
	@Produces("application/json")
	@Path("/userGroup")
	public String getAllGroups() throws JSONException {
		
		JSONArray arr = new JSONArray();

		for (int i = 0; i < groups.size(); i++) {
			JSONObject obj = new JSONObject();
			AclPrincipalTest acl = new AclPrincipalTest();
			AclPrincipal currentGroup = groups.get(i);
			acl.creationDate = currentGroup.getCreationDate().getTime();
			
			obj.put("name", currentGroup.getName());
			obj.put("id", currentGroup.getId());
			obj.put("individual", currentGroup.isIndividual());
			if(acl.creationDate != null) obj.put("creationDate", acl.creationDate);
			
			arr.put(obj);
		}
		
		return arr.toString();
	}
	
	/**
	 * Start-up a local Grizzly container with this class deployed.
	 * @param host
	 * @param port
	 * @return SelectorThread is used to stop the server when ready.
	 * @throws IOException
	 */
	public static SelectorThread startServer(String host, int port) throws IOException{
		URI baseUri = UriBuilder.fromUri("http://"+host+"/").port(port).build();
		
		final Map<String, String> initParams = new HashMap<String, String>();
		// Map this package as the service entry point.
		initParams.put("com.sun.jersey.config.property.packages", LocalNodeServiceStub.class.getPackage().getName());
		// Turn on the JSON marshaling
		initParams.put("com.sun.jersey.api.json.POJOMappingFeature", "true");
		System.out.println("Starting grizzly...");
		System.out.println("Listening at URL: "+baseUri.toURL().toString());
		return GrizzlyWebContainerFactory.create(baseUri, initParams);
	}

	
	/**
	 * Starts the local web service.
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException{
		if(args == null ||  args.length < 2){
			throw new IllegalArgumentException("The first argument should be the service host, and the second should be the port");
		}
		SelectorThread selector = startServer(args[0], Integer.parseInt(args[1]));
		// Wait for user input
		System.out.println("Press any key to stop the server...");
		System.in.read();
		selector.stopEndpoint();
		System.out.println("Server stoped.");
	}

	/*
	 * Helper class
	 */
	
	public class AclPrincipalTest {
		public String id;
		public String name;
		public Long creationDate;
		public String uri;
		public String etag;
		public Boolean individual;
		
		public AclPrincipalTest() {	
		}
		
		public AclPrincipalTest(String id, String name, Long creationDate, String uri, String etag, boolean individual) {		
			this.id = id;
			this.name = name;
			this.creationDate = creationDate;
			this.uri = uri;
			this.etag = etag;
			this.individual = individual;
		}

		public boolean equals(AclPrincipal obj) {
			if (obj == null)
				return false;
			if (creationDate == null) {
				if (obj.getCreationDate() != null)
					return false;
			} else if (!creationDate.equals(obj.getCreationDate().getTime()))
				return false;
			if (etag == null) {
				if (obj.getEtag() != null)
					return false;
			} else if (!etag.equals(obj.getEtag()))
				return false;
			if (id == null) {
				if (obj.getId() != null)
					return false;
			} else if (!id.equals(obj.getId()))
				return false;
			if (individual != obj.isIndividual())
				return false;
			if (name == null) {
				if (obj.getName() != null)
					return false;
			} else if (!name.equals(obj.getName()))
				return false;
			if (uri == null) {
				if (obj.getUri() != null)
					return false;
			} else if (!uri.equals(obj.getUri()))
				return false;
			return true;
		}
		
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			AclPrincipalTest other = (AclPrincipalTest) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (creationDate == null) {
				if (other.creationDate != null)
					return false;
			} else if (!creationDate.equals(other.creationDate))
				return false;
			if (etag == null) {
				if (other.etag != null)
					return false;
			} else if (!etag.equals(other.etag))
				return false;
			if (id == null) {
				if (other.id != null)
					return false;
			} else if (!id.equals(other.id))
				return false;
			if (individual != other.individual)
				return false;
			if (name == null) {
				if (other.name != null)
					return false;
			} else if (!name.equals(other.name))
				return false;
			if (uri == null) {
				if (other.uri != null)
					return false;
			} else if (!uri.equals(other.uri))
				return false;
			return true;
		}

		private LocalNodeServiceStub getOuterType() {
			return LocalNodeServiceStub.this;
		}

		@Override
		public String toString() {
			return "AclPrincipalTest [id=" + id + ", name=" + name
					+ ", creationDate=" + creationDate + ", uri=" + uri
					+ ", etag=" + etag + ", individual=" + individual + "]";
		}
	}
	/*
	 * Response/exception mappings
	 */	
	public class BadRequestException extends WebApplicationException {
		/**
		 * Create a HTTP 404 (Not Found) exception.
		 */
		public BadRequestException() {
			super(401);
		}

		/**
		 * Create a HTTP 401 (Unauthorized) exception.
		 * 
		 * @param message
		 *            the String that is the entity of the 401 response.
		 */
		public BadRequestException(String message) {
			super(Response.status(401).entity(message)
					.type("text/plain").build());
		}

	}
	public class NotFoundException extends WebApplicationException {
		/**
		 * Create a HTTP 404 (Not Found) exception.
		 */
		public NotFoundException() {
			super(Responses.notFound().build());
		}

		/**
		 * Create a HTTP 404 (Not Found) exception.
		 * 
		 * @param message
		 *            the String that is the entity of the 404 response.
		 */
		public NotFoundException(String message) {
			super(Response.status(Responses.NOT_FOUND).entity(message)
					.type("text/plain").build());
		}

	}

	// Generates random data
	@GET @Produces("text/html")
	@Path("/populate/random")
	public String generateRandomDatasets(@DefaultValue("25") @QueryParam("number") int number){
		// We get around threading issues by making a copy
		List<Dataset> copy = new ArrayList<Dataset>();
		copy.addAll(allDataSets);
		
		// Add to the list of datasts.
		for(int i=0; i<number; i++){
			copy.add(RandomDataset.createRandomDataset());
		}
		// the copy becomes the new list
		allDataSets = copy;
		return "Total dataset count: "+Integer.toString(allDataSets.size());
	}
	
	public List<Dataset> getAllForTests(){
		return allDataSets;
	}

	/*
	 * Query Service
	 */
	@GET @Produces("application/json")
	@Path("/query")
	public LinkedHashMap<String, Object> executeSearch(@QueryParam("query") String query){
		logger.info(query);
		LinkedHashMap<String, Object> results = new LinkedHashMap<String, Object>();
		// Extract the query
		SearchParameters params = QueryStringUtils.parseQueryString(query);
		
		// First apply the filter
		List<Map<String, Object>> start = rows;
		if(params.getWhere() != null){
			start = ListUtils.getFilteredCopy(params.getWhere(), rows);
		}
		
		// First sort
		List<Map<String, Object>> fullList = ListUtils.getSortedCopy(params.getSort(), params.isAscending(), start, Map.class);
		// Get the sub-set based on pagination
		List<Map<String, Object>> subList = ListUtils.getSubList(params.getOffset(), params.getLimit(), fullList);
		
		results.put(SearchService.KEY_TOTAL_NUMBER_OF_RESULTS, start.size());
		results.put(SearchService.KEY_RESULTS, subList);
		return results;
	}
	
	// Generates random data
	@GET @Produces("text/html")
	@Path("/query/populate/random")
	public String generateRandomRows(@DefaultValue("25") @QueryParam("number") int number){
		// We get around threading issues by making a copy
		List<Map<String, Object>> copy = new ArrayList<Map<String, Object>>();
		copy.addAll(rows);
		
		// Add to the list of datasts.
		for(int i=0; i<number; i++){
			// Add a new Row
			Map<String, Object> newRow = new HashMap<String, Object>();
			// Add random data for each valid column
			for(ColumnInfo column: validColumns){
				Object value = RandomColumnData.createRandomValue(column.getType());
				newRow.put(column.getId(), value);
			}
			// Add this row
			copy.add(newRow);
		}
		// the copy becomes the new list
		rows = copy;
		return "Total row count: "+Integer.toString(rows.size());
	}
	
	// clear all data
	@GET @Produces("text/html")
	@Path("/query/clear/all")
	public String clearAllQuery(){
		// Replace the old list with a new list
		rows = new ArrayList<Map<String, Object>>();
		return "Total rows count: "+Integer.toString(rows.size());
	}


	/**
	 * Generate random data using the passed datsets name and id.
	 * @param list
	 */
	public void generateRandomRows(List<Dataset> list) {
		// We get around threading issues by making a copy
		List<Map<String, Object>> copy = new ArrayList<Map<String, Object>>();
		copy.addAll(rows);
		
		// Add to the list of datasts.
		for(int i=0; i<list.size(); i++){
			Dataset dataset = list.get(i);
			// Add a new Row
			Map<String, Object> newRow = new HashMap<String, Object>();
			// Add random data for each valid column
			for(ColumnInfo column: validColumns){
				Object value = RandomColumnData.createRandomValue(column.getType());
				newRow.put(column.getId(), value);
			}
			// Use the dataset data for name and id
			newRow.put("name", dataset.getName());
			newRow.put("dataset.id", dataset.getId());			
			Integer layerInt = rand.nextInt(3);
			String layerTypeChar = "";
			switch (layerInt) {
			case 0:
				layerTypeChar = "E";
				break;
			case 1:
				layerTypeChar = "C";
				break;
			case 2:
				layerTypeChar = "G";
				break;
			}
			newRow.put("layer.type", layerTypeChar);
			
			List<FilterEnumeration> filters = columnConfigProvider.getFilterEnumerations();
			// For each filter create random data from the enumeration
			if(filters != null){
				for(FilterEnumeration filter: filters){
					HeaderData hd = columnConfigProvider.get(filter.getColumnId());
					Object value = RandomColumnData.createRandomValue(filter, hd);
					newRow.put(filter.getColumnId(), value);
				}
			}
			
			// Add this row
			copy.add(newRow);
		}
		// the copy becomes the new list
		rows = copy;
	}
	
}
