package org.sagebionetworks.repo.model.dbo.file.download.v2;

import java.util.List;

public interface FilesBatchProvider {

	List<Long> getBatchOfFiles(long limit, long offset);
	
}
