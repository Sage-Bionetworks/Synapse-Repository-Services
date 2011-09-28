package org.sagebionetworks.web.util;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.UriBuilder;

import org.sagebionetworks.web.shared.Dataset;

import com.sun.grizzly.http.SelectorThread;
import com.sun.jersey.api.container.grizzly.GrizzlyWebContainerFactory;

/**
 * Launches all of the local service stubs.
 * @author jmhill
 *
 */
@Path("/")
public class LocalStubLauncher {
	
	@Path("repo/v1")
	public LocalNodeServiceStub getNodeServiceStub() throws IOException{
		return new LocalNodeServiceStub();
	}
	
	@Path("repo/v1/query")
	public LocalSearchServiceStub getSearchServiceStub() throws IOException{
		return new LocalSearchServiceStub();
	}
	
	/**
	 * When called at this level we populate the datasets and then ues the results
	 * to populate the search resutls
	 * @return
	 * @throws IOException 
	 */
	@GET @Produces("text/html")
	@Path("/populate/random")
	public String populateRandom(@DefaultValue("25") @QueryParam("number") int number) throws IOException{
		LocalNodeServiceStub nodeServiceStub = getNodeServiceStub();
		LocalSearchServiceStub searchStub = getSearchServiceStub();
		nodeServiceStub.generateRandomDatasets(number);
		List<Dataset> list = nodeServiceStub.getAllForTests();
		searchStub.generateRandomRows(list);
		return "Total rows: "+list.size();
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
		initParams.put("com.sun.jersey.config.property.packages", LocalDatasetServiceStub.class.getPackage().getName());
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

}
