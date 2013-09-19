package org.sagebionetworks.usagemetrics;

import org.sagebionetworks.client.SynapseClientImpl;
import org.sagebionetworks.client.exceptions.SynapseException;

public class ProfilingUsers {
	private static final long NANOSECOND_PER_MILLISECOND = 1000000L;

	public static void main(String[] args) throws Exception {
		SynapseClientImpl synapse = new SynapseClientImpl();
		String username = args[0];
		String password = args[1];
		synapse.login(username, password);
		
		profileGetUsers(synapse);

		profileGetGroups(synapse);
		
	}

	private static void profileGetGroups(SynapseClientImpl synapse)
			throws SynapseException {
		long start = System.nanoTime();
		synapse.getGroups(0, 1000);
		long end = System.nanoTime();
		
		long latencyMS = (end - start) / NANOSECOND_PER_MILLISECOND;

		System.out.format("Groups took: %dms%n", latencyMS);			
		
	}

	private static void profileGetUsers(SynapseClientImpl synapse)
			throws SynapseException {
		long start = System.nanoTime();
		synapse.getUsers(0, 600);
		long end = System.nanoTime();
		
		long latencyMS = (end - start) / NANOSECOND_PER_MILLISECOND;

		System.out.format("Users took: %dms%n", latencyMS);			
		
	}
}
