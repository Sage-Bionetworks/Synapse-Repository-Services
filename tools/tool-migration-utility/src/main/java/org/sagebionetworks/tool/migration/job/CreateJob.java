package org.sagebionetworks.tool.migration.job;

import java.util.Set;

public class CreateJob implements Comparable<CreateJob>{
	
	public Set<String> idsToCreate;
	public String dedpenancyId;
	
	
	@Override
	public int compareTo(CreateJob o) {
		// First look at the dependancy
		return 0;
	}

}
