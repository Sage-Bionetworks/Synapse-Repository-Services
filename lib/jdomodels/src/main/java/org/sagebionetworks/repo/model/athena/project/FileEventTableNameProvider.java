package org.sagebionetworks.repo.model.athena.project;

import org.sagebionetworks.repo.model.athena.AthenaSupport;
import org.sagebionetworks.repo.model.statistics.FileEvent;
import org.springframework.stereotype.Service;

@Service
public class FileEventTableNameProvider {

	private AthenaSupport athenaSupport;
	
	public FileEventTableNameProvider(AthenaSupport athenaSupport) {
		this.athenaSupport = athenaSupport;
	}
	
	public String getTableName(FileEvent fileEvent) {
		return athenaSupport.getTableName(fileEvent.getGlueTableName());
	}
	
}
