package org.sagebionetworks.web.util;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.UriBuilder;

import org.sagebionetworks.web.shared.Dataset;
import org.sagebionetworks.web.shared.PaginatedDatasets;

import com.sun.grizzly.http.SelectorThread;
import com.sun.jersey.api.container.grizzly.GrizzlyWebContainerFactory;

/**
 * As simple stub implementation of the data sets service.
 * 
 * @author jmhill
 *
 */
@Path("/repo/v1")
public class LocalDatasetServiceStub {
	
	public final static String NULL = "NULL";
	
	/**
	 * This is the shared list of datasets
	 */
	private static List<Dataset> allDataSets = new ArrayList<Dataset>();
	
	public LocalDatasetServiceStub(){
	}
	
	@GET @Produces("application/json")
	@Path("/dataset")
	public PaginatedDatasets getAllDatasets(@DefaultValue("1") @QueryParam("offset") int offset
											,@DefaultValue("10") @QueryParam("limit") int limit
											,@DefaultValue(NULL) @QueryParam("sort") String sort
											,@DefaultValue("true") @QueryParam("ascending") boolean ascending){
		// Create a sub-list from the offset and limit
		// If the sort key is null then set it to null
		if(NULL.equals(sort)){
			sort = null;
		}
		List<Dataset> fullList = ListUtils.getSortedCopy(sort, ascending, allDataSets);
		List<Dataset> subList = ListUtils.getSubList(offset, limit, fullList);
		PaginatedDatasets results = new PaginatedDatasets();
		results.setTotalNumberOfResults(allDataSets.size());
		results.setResults(subList);
		return results;
	}
	
	
	public List<Dataset> getAllForTests(){
		return allDataSets;
	}
	
	@GET @Produces("application/json")
	@Path("/dataset/{id}")
	public Dataset getDataset(@PathParam("id") String id){
		if(id == null) return null;
		id  = id.trim();
		if("".equals(id)) return null;
		// simple linear search
		for(Dataset dataset: allDataSets){
			if(id.equals(dataset.getId())){
				return dataset;
			}
		}
		// Failed to find it
		return null;		
	}
	
	
	// Generates random data
	@GET @Produces("text/html")
	@Path("/dataset/populate/random")
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
	
	// clear all data
	@GET @Produces("text/html")
	@Path("/dataset/clear/all")
	public String clearAll(){
		// Replace the old list with a new list
		allDataSets = new ArrayList<Dataset>();
		return "Total dataset count: "+Integer.toString(allDataSets.size());
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
