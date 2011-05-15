package org.sagebionetworks.web.util;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.UriBuilder;

import org.sagebionetworks.web.shared.users.InitiateSession;

import com.sun.grizzly.http.SelectorThread;
import com.sun.jersey.api.container.grizzly.GrizzlyWebContainerFactory;

/**
 * Launches all of the local service stubs.
 * @author jmhill
 *
 */
@Path("/auth/v1")
public class LocalAuthStubLauncher {
	
	
	private static List<User> users = new ArrayList<User>();
	private static Map<String, User> sessions = new LinkedHashMap<String, User>();
	
	@POST 
	@Path("/user")
	public void createUser(@QueryParam("userId") String userId,
			@QueryParam("email") String email,
			@QueryParam("firstName") String firstName,
			@QueryParam("lastName") String lastName,
			@QueryParam("displayName") String displayName) {
		users.add(new User(userId, email, firstName, lastName, displayName));
		// return 201?
	}
	
	@PUT
	public void updateUser(@QueryParam("userId") String userId,
			@QueryParam("email") String email,
			@QueryParam("firstName") String firstName,
			@QueryParam("lastName") String lastName,
			@QueryParam("displayName") String displayName) {
		boolean found = false;
		for(User user : users) {
			if(userId.equals(user.userId)) {
				user = new User(userId, email, firstName, lastName, displayName);
				found = true;
				break;
			}
		}
		
		if(!found) {
			// throw 400 
		}
		// return 200
	}

	@POST
	@Path("/userPasswordEmail")
	public void sendPasswordChangeEmail(@QueryParam("userId") String userId) {
		// return 200
	}
		
	@POST
	@Produces("application/json")
	@Path("/session")
	public InitiateSession initiateSession(@QueryParam("userId") String userId, @QueryParam("password") String password) {
		User foundUser = null;
		for(User user : users) {
			if(userId.equals(user.userId)) {
				foundUser = user;
				break;
			}			
		}
		
		if(foundUser == null) {
			return null; // throw 400
		}
		
		InitiateSession session = new InitiateSession();
		session.setDisplayName(foundUser.displayName);
		session.setSessionToken(foundUser.toString());
		sessions.put(session.getSessionToken(), foundUser);
		return session;
	}
	
	@PUT
	@Path("/session")
	public void refreshSession(@QueryParam("sessionToken") String token) {
		if(!sessions.containsKey(token)) { 
			// throw 404
		}
		// return 200
	}
	
	@PUT
	@Produces("application/json")
	@Path("/session")
	public void deleteSession(@QueryParam("sessionToken") String token) {
		if(sessions.containsKey(token)) { 
			sessions.remove(token);
		}
		// return 204
	}
	
	@GET
	@Path("/clear/all")
	public void clearAll() {
		sessions.clear();
		users.clear();
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
		initParams.put("com.sun.jersey.config.property.packages", LocalAuthStubLauncher.class.getPackage().getName());
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
	 * Helper classes
	 */
	private class User {
		public String userId;
		public String email;
		public String firstName;
		public String lastName;
		public String displayName;
		
		public User() { }

		public User(String userId, String email, String firstName,
				String lastName, String displayName) {
			super();
			this.userId = userId;
			this.email = email;
			this.firstName = firstName;
			this.lastName = lastName;
			this.displayName = displayName;
		}
		
	}
	
}
