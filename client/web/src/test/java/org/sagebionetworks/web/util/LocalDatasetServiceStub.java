package org.sagebionetworks.web.util;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import org.sagebionetworks.web.shared.Dataset;
import org.sagebionetworks.web.shared.PaginatedDatasets;

/**
 * As simple stub implementation of the data sets service.
 * 
 * @author jmhill
 *
 */
@Path("/dataset")
public class LocalDatasetServiceStub {
	
	public final static String NULL = "NULL";
	
	/**
	 * This is the shared list of datasets
	 */
	private static List<Dataset> allDataSets = new ArrayList<Dataset>();
	
	public LocalDatasetServiceStub(){
	}
	
	@GET @Produces("application/json")
	public PaginatedDatasets getAllDatasets(@DefaultValue("1") @QueryParam("offset") int offset
											,@DefaultValue("10") @QueryParam("limit") int limit
											,@DefaultValue(NULL) @QueryParam("sort") String sort
											,@DefaultValue("true") @QueryParam("ascending") boolean ascending){
		// Create a sub-list from the offset and limit
		// If the sort key is null then set it to null
		if(NULL.equals(sort)){
			sort = null;
		}
		List<Dataset> fullList = ListUtils.getSortedCopy(sort, ascending, allDataSets, Dataset.class);
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
	@Path("/{id}")
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
	
	// clear all data
	@GET @Produces("text/html")
	@Path("/clear/all")
	public String clearAll(){
		// Replace the old list with a new list
		allDataSets = new ArrayList<Dataset>();
		return "Total dataset count: "+Integer.toString(allDataSets.size());
	}

}
