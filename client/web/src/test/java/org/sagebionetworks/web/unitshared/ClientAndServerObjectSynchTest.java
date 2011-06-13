package org.sagebionetworks.web.unitshared;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.util.HashSet;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/**
 * The purpose of this test is to detect changes between the client-side JSON objects,
 * and the server-side JSON objects.  When client and server objects are out-of-synch
 *  with each other, there will be JSON marshaling exception.  This test detects when
 *  the objects are out-of-synch.
 *  
 *  In the future, the plan is to have a JSON schema for these objects and auto-generate
 *  the objects from the shared schema.  When that occurs, this test can be deleted.
 *  
 * @author jmhill
 *
 */

@SuppressWarnings({"rawtypes","unused"})
public class ClientAndServerObjectSynchTest {
	
	/**
	 * Simple helper to pair the client and server classes.
	 * @author jmhill
	 *
	 */
	private class Pair{
		Class clientClass;
		Class serverClass;
		public Class getClientClass() {
			return clientClass;
		}
		public void setClientClass(Class clientClass) {
			this.clientClass = clientClass;
		}
		public Class getServerClass() {
			return serverClass;
		}
		public void setServerClass(Class serverClass) {
			this.serverClass = serverClass;
		}
		public Pair(Class clientClass, Class serverClass) {
			super();
			this.clientClass = clientClass;
			this.serverClass = serverClass;
		}
	}
	
	private Pair[] classPairs;
	
	@Before
	public void before(){
		classPairs = new Pair[]{
				// Dataset
				new Pair(org.sagebionetworks.web.shared.Dataset.class, org.sagebionetworks.repo.model.Dataset.class),
				// Layer
				new Pair(org.sagebionetworks.web.shared.Layer.class, org.sagebionetworks.repo.model.InputDataLayer.class),
				// LayerPreview
				new Pair(org.sagebionetworks.web.shared.LayerPreview.class, org.sagebionetworks.repo.model.LayerPreview.class),
				// Preview
				new Pair(org.sagebionetworks.web.shared.Preview.class, org.sagebionetworks.repo.model.StoredLayerPreview.class),
				// LayerLocation
				new Pair(org.sagebionetworks.web.shared.DownloadLocation.class, org.sagebionetworks.repo.model.LayerLocation.class),
				// Annotations
				new Pair(org.sagebionetworks.web.shared.Annotations.class, org.sagebionetworks.repo.model.Annotations.class),
		};
	}
	
	@Ignore
	@Test
	public void testAllPairs(){
		for(Pair pair: classPairs){
			testPair(pair.getClientClass(), pair.getServerClass());
		}
	}
	
	private void testPair(Class clientClass, Class serverClass){
		// First make sure all fields of the client exist in the server
		assertNotNull(clientClass);
		assertNotNull(serverClass);
		Field[] clientFields = clientClass.getDeclaredFields();
		Field[] serverFields = serverClass.getDeclaredFields();
		// While this is faster, it is not as informative.
//		assertEquals("For class: "+clientClass.getSimpleName()+" the client object has: "+clientFields.length+" fields, while the server object has: "+serverFields.length,clientFields.length, serverFields.length);
		// Put the client names into a set
		HashSet<String> clientSet = new HashSet<String>();
		for(Field client: clientFields){
			clientSet.add(client.getName());
		}
		HashSet<String> serverSet = new HashSet<String>();
		for(Field server: serverFields){
			serverSet.add(server.getName());
		}
		// Check client against server
		for(Field client: clientFields){
			assertTrue("The client object: "+clientClass.getName()+" has field named: "+client.getName()+" but the server object: "+serverClass.getName()+" does not have the same field",serverSet.contains(client.getName()));
		}
		// Check the server against client
		for(Field server: serverFields){
			assertTrue("The server object: "+serverClass.getName()+" has field named: "+server.getName()+" but the client object: "+clientClass.getName()+" does not have the same field",clientSet.contains(server.getName()));
		}
	}
	

}
