package org.sagebionetworks;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sagebionetworks.client.SynapseAdminClient;
import org.sagebionetworks.client.SynapseAdminClientImpl;
import org.sagebionetworks.client.SynapseClient;
import org.sagebionetworks.client.SynapseClientImpl;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.client.exceptions.SynapseResultNotReadyException;
import org.sagebionetworks.repo.manager.EntityTypeConvertionError;
import org.sagebionetworks.repo.model.AsyncLocationableTypeConversionRequest;
import org.sagebionetworks.repo.model.AsyncLocationableTypeConversionResults;
import org.sagebionetworks.repo.model.Data;
import org.sagebionetworks.repo.model.FileEntity;
import org.sagebionetworks.repo.model.Folder;
import org.sagebionetworks.repo.model.LocationData;
import org.sagebionetworks.repo.model.LocationTypeNames;
import org.sagebionetworks.repo.model.LocationableTypeConversionResult;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.Study;
import org.sagebionetworks.repo.model.table.QueryResultBundle;
import org.sagebionetworks.util.Pair;
import org.sagebionetworks.util.TimeUtils;

/**
 * Test for the service to convert locationables.
 * This test can be removed when we are done with conversion.
 * @author jhill
 *
 */
public class IT044LocationableConversion {
	
	private static SynapseAdminClient adminSynapse;
	private static SynapseClient synapse;
	private static Long userToDelete;
	
	private static long MAX_WAIT_MS = 1000*60*5;
	
	private Project project;

	@BeforeClass
	public static void beforeClass() throws Exception {
		// Create a user
		adminSynapse = new SynapseAdminClientImpl();
		SynapseClientHelper.setEndpoints(adminSynapse);
		adminSynapse.setUserName(StackConfiguration.getMigrationAdminUsername());
		adminSynapse.setApiKey(StackConfiguration.getMigrationAdminAPIKey());
		adminSynapse.clearAllLocks();
		synapse = new SynapseClientImpl();
		userToDelete = SynapseClientHelper.createUser(adminSynapse, synapse);
	}
	
	@Before
	public void before() throws SynapseException {
		// Create a project, this will own the file entity
		project = new Project();
		project = synapse.createEntity(project);
	}

	@After
	public void after() throws Exception {
		if(project != null){
			synapse.deleteEntity(project, true);
		}
	}
	
	@AfterClass
	public static void afterClass() throws Exception {
		try {
			adminSynapse.deleteUser(userToDelete);
		} catch (SynapseException e) { }
	}
	
	@Test
	public void testConvertStudy() throws Exception{
		Study study = new Study();
		study.setParentId(project.getId());
		study.setDisease("cancer");
		LocationData ld = new LocationData();
		ld.setPath("http://www.google.com/somedoc");
		ld.setType(LocationTypeNames.external);
		study.setContentType("text/plain");
		study.setLocations(Arrays.asList(ld));
		study = adminSynapse.createEntity(study);
		// Convert it to a folder
		List<LocationableTypeConversionResult> results = waitForResulst(Arrays.asList(study.getId()));
		assertNotNull(results);
		assertEquals(1, results.size());
		LocationableTypeConversionResult r = results.get(0);
		assertEquals(study.getId(), r.getEntityId());
		// It should have a child
		assertEquals(new Long(1), adminSynapse.getChildCount(r.getEntityId()));
	}
	
	@Test
	public void testConvertNonStudyLocationable() throws Exception{
		Data data = new Data();
		data.setParentId(project.getId());
		data.setDisease("cancer");
		LocationData ld = new LocationData();
		ld.setPath("http://www.google.com/somedoc");
		ld.setType(LocationTypeNames.external);
		data.setContentType("text/plain");
		data.setLocations(Arrays.asList(ld));
		data = adminSynapse.createEntity(data);
		// Convert it to a file
		List<LocationableTypeConversionResult> results = waitForResulst(Arrays.asList(data.getId()));
		assertNotNull(results);
		assertEquals(1, results.size());
		LocationableTypeConversionResult r = results.get(0);
		assertEquals(data.getId(), r.getEntityId());
		// It should not have children
		assertEquals(new Long(0), adminSynapse.getChildCount(r.getEntityId()));
	}
	
	
	/**
	 * Wait for the results
	 * @param locationableId
	 * @return
	 * @throws Exception
	 */
	public List<LocationableTypeConversionResult> waitForResulst(List<String> locationableId) throws Exception {
		AsyncLocationableTypeConversionRequest request = new AsyncLocationableTypeConversionRequest();
		request.setLocationableIdsToConvert(locationableId);
		final String asyncToken = synapse.startLocationableTypeConvertJob(request);
		return TimeUtils.waitFor(MAX_WAIT_MS, 500L, new Callable<Pair<Boolean, List<LocationableTypeConversionResult>>>() {
			@Override
			public Pair<Boolean, List<LocationableTypeConversionResult>> call()
					throws Exception {
				try {
					AsyncLocationableTypeConversionResults result = synapse.getLocationableTypeConverJobResults(asyncToken);
					return Pair.create(true, result.getResults());
				} catch (SynapseResultNotReadyException e) {
					return Pair.create(false, null);
				}
			}
		});
	}
}
