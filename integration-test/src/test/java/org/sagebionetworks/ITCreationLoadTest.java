package org.sagebionetworks;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.sagebionetworks.client.Synapse;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.Data;
import org.sagebionetworks.repo.model.Project;



@Ignore // this is to investigate PLFM-1431
public class ITCreationLoadTest {

	private static Synapse synapse = null;
	
	
	@BeforeClass
	public static void beforeClass() throws Exception {
		String authEndpoint = StackConfiguration.getAuthenticationServicePrivateEndpoint();
		String repoEndpoint = StackConfiguration.getRepositoryServiceEndpoint();
		synapse = new Synapse();
		synapse.setAuthEndpoint(authEndpoint);
		synapse.setRepositoryEndpoint(repoEndpoint);
		synapse.login(StackConfiguration.getIntegrationTestUserAdminName(),
				StackConfiguration.getIntegrationTestUserAdminPassword());
	}
	
	private List<String> idsToDelete = null;
	
	private void deleteIds() throws Exception {
		if (idsToDelete==null) return;
		for (String id : idsToDelete) {
			synapse.deleteAndPurgeEntityById(id);
		}
		idsToDelete.clear();
	}
	
	private Project project = null;
	
	@Before
	public void before() throws Exception {
		idsToDelete = new ArrayList<String>();
		deleteIds();
		project = new Project();
		project.setName("ITCreationLoad_"+System.currentTimeMillis());
		project = synapse.createEntity(project);
		idsToDelete.add(project.getId());
	}
	
	@After
	public void after() throws Exception {
		deleteIds();
	}
	
	private static final int NUM_THREADS = 20;
	
	private String makeAData(String name) throws Exception {
		Data data = new Data();
		data.setParentId(project.getId());
		data.setName(name);
		data = synapse.createEntity(data);
		Annotations annots = synapse.getAnnotations(data.getId());
		Map<String,List<String>> stringAnnotations = annots.getStringAnnotations();
		if (stringAnnotations==null) {
			stringAnnotations = new HashMap<String,List<String>>();
			annots.setStringAnnotations(stringAnnotations);
		}
		stringAnnotations.put("drug", Arrays.asList(new String[]{"drug-"+name}));
		synapse.updateAnnotations(data.getId(), annots);
		return data.getId();
	}
	
	@Test
	public void testCreation() throws Exception {
		List<Thread> threads = new ArrayList<Thread>();
		final List<Exception> exceptions = new ArrayList<Exception>();
		final List<String> dataIds = new ArrayList<String>();
		for (int i=0; i<NUM_THREADS; i++) {
			final String name = ""+i;
			Thread t = new Thread() {
				public void run() {
					try {
						dataIds.add(makeAData(name));
					} catch (Exception e) {
						exceptions.add(e);
						e.printStackTrace();
					}
				}
			};
			threads.add(t);
			t.start();
		}
		int liveCount = NUM_THREADS;
		while (liveCount>0) {
			liveCount = 0;
			for (Thread t : threads) {
				if (t.isAlive()) {
					liveCount++;
				}
			}
			//System.out.println(""+liveCount+" threads still alive.");
			Thread.sleep(100L);
		}
		//System.out.println("\ncreated "+dataIds.size()+" ids: "+dataIds+"\n");
		assertTrue(exceptions.toString(), exceptions.isEmpty());
	}
	
}
