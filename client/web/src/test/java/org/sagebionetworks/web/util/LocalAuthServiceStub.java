package org.sagebionetworks.web.util;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

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
@Path("/auth/v1")
public class LocalAuthServiceStub {
	
	private static List<User> users = new ArrayList<User>();
	private static Map<String, User> sessions = new LinkedHashMap<String, User>();
	
	@POST 
	@Consumes("application/json")@Produces("application/json")
	@Path("/user")
	public String createUser(UserRegistration userInfo) {
		users.add(new User(userInfo.getEmail(), userInfo.getEmail(), userInfo.getFirstName(), userInfo.getLastName(), userInfo.getDisplayName()));
		return "";
	}
	
	@PUT
	@Consumes("application/json")@Produces("application/json")
	@Path("/user")
	public String updateUser(UserRegistration updatedUser) {
		boolean found = false;
		String userId = updatedUser.getEmail();
		for(User user : users) {
			if(userId.equals(user.userId)) {
				user = new User(updatedUser.getEmail(), updatedUser.getEmail(), updatedUser.getFirstName(), updatedUser.getLastName(), updatedUser.getDisplayName());
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
	@Path("/session")
	public UserSession initiateSession(UserLogin userLogin) throws BadRequestException {
		User foundUser = null;
		String userId = userLogin.getEmail();
		for(User user : users) {
			if(userId.equals(user.userId)) {
				foundUser = user;
				break;
			}			
		}
		
		if(foundUser == null) {
			throw new BadRequestException();
		}
		
		UserSession session = new UserSession();
		session.setDisplayName(foundUser.displayName);
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
		initParams.put("com.sun.jersey.config.property.packages", LocalAuthServiceStub.class.getPackage().getName());
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
	
}
