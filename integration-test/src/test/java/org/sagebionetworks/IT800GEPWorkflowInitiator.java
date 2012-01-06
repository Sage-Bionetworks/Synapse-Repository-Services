package org.sagebionetworks;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.sagebionetworks.client.Synapse;
import org.sagebionetworks.client.exceptions.SynapseServiceException;
import org.sagebionetworks.client.exceptions.SynapseUserException;
import org.sagebionetworks.gepipeline.GEPWorkflowInitiator;
import org.sagebionetworks.repo.model.Dataset;
import org.sagebionetworks.repo.model.Layer;
import org.sagebionetworks.repo.model.LayerTypeNames;
import org.sagebionetworks.repo.model.Location;
import org.sagebionetworks.repo.model.LocationData;
import org.sagebionetworks.repo.model.LocationTypeNames;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;

public class IT800GEPWorkflowInitiator {
	private static Synapse synapse = null;
	private static Project sourceProject = null;
	private static Project targetProject = null;

	/**
	 * @throws Exception
	 * 
	 */
	@BeforeClass
	public static void beforeClass() throws Exception {

		synapse = new Synapse();
		synapse.setAuthEndpoint(StackConfiguration
				.getAuthenticationServicePrivateEndpoint());
		synapse.setRepositoryEndpoint(StackConfiguration
				.getRepositoryServiceEndpoint());
		synapse.login(StackConfiguration.getIntegrationTestUserOneName(),
				StackConfiguration.getIntegrationTestUserOnePassword());

		Project p = new Project();
		p.setName("source_project");
		sourceProject = synapse.createEntity(p);
		p = new Project();
		p.setName("dest_project");
		targetProject = synapse.createEntity(p);
		Dataset dataset = new Dataset();
		dataset.setParentId(sourceProject.getId());
		dataset.setName("test_dataset");
		dataset = synapse.createEntity(dataset);
		// create layer with type E or G
		Layer layer = new Layer();
		layer.setParentId(dataset.getId());
		layer.setName("test_layer");
		layer.setType(LayerTypeNames.E);
		layer = synapse.createEntity(layer);
		File dataFile = createDataFile();
		layer = (Layer)synapse.uploadLocationableToSynapse(layer, dataFile);
		assertNotNull(layer.getContentType());
		assertNotNull(layer.getMd5());

		List<LocationData> locations = layer.getLocations();
		assertEquals(1, locations.size());
		LocationData locationData = locations.get(0);
		assertEquals(LocationTypeNames.awss3, locationData.getType());
		assertNotNull(locationData.getPath());
		assertTrue(locationData.getPath().startsWith("http"));
		
		// now check that location is there:
		if (false) { // TODO:  make this work
			JSONObject locationQueryResult = synapse.query("select * from location where parentId=="+layer.getId());
			JSONArray jsonLocations = locationQueryResult.getJSONArray("results");
			assertEquals(locationQueryResult.toString(), 1, jsonLocations.length());
			// get location mdsum
			Location location = (Location) EntityFactory.createEntityFromJSONObject(jsonLocations.getJSONObject(0), Location.class);
			String md5Sum = location.getMd5sum();
			assertNotNull(md5Sum);
			String layerLocation = location.getPath();
			assertNotNull(layerLocation);
		}
	}
	
	public static File createDataFile() throws IOException {
		File file = File.createTempFile("foo", "bar");
		PrintWriter pw = new PrintWriter(file);
		pw.println("foo bar bas");
		pw.close();
		return file;
	}

	/**
	 * @throws HttpException
	 * @throws IOException
	 * @throws JSONException
	 * @throws SynapseUserException
	 * @throws SynapseServiceException
	 */
	@AfterClass
	public static void afterClass() throws Exception {
		if(null != sourceProject) {
			synapse.deleteEntity(sourceProject);
		}
	}

	/**
	 * @throws Exception
	 */
	@Test
	public void testCommonsCrawler() throws Exception {
		String sourceProjectId = sourceProject.getId();
		String targetProjectId = targetProject.getId();
		Collection<Map<String,Object>> layerTasks = GEPWorkflowInitiator.crawlSourceProject(synapse, sourceProjectId, targetProjectId);
	}
}
