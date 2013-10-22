package org.sagebionetworks.usagemetrics;

import org.sagebionetworks.client.SynapseClientImpl;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.repo.model.Project;

public class GetProjectNames {

	static String[] projectIds = { "syn113927", "syn139028", "syn156493",
			"syn156497", "syn160764", "syn163125", "syn163352", "syn300022",
			"syn308212", "syn164229", "syn164247", "syn274384", "syn28100",
			"syn296559", "syn299154", "syn31282", "syn346131", "syn346224",
			"syn365501", "syn371716", "syn392319", "syn443970", "syn447981" };

	public static void main(String[] args) throws Exception {
		SynapseClientImpl synapse = new SynapseClientImpl();
		String username = args[0];
		String password = args[1];
		synapse.login(username, password);

		printProjectNames(synapse);
	}

	private static void printProjectNames(SynapseClientImpl synapse) {
		for (String id : projectIds) {
			Project entity;
			try {
				entity = synapse.getEntity(id, Project.class);
			} catch (SynapseException e) {
				continue;
			}
			String name = entity.getName();
			String userName = entity.getCreatedBy();
			
			System.out.format("Project Id:%s - created by %s - %s%n", id, userName, name);
		}
	}
}
