package org.sagebionetworks.repo.manager.statistics.project;

import org.sagebionetworks.repo.model.athena.project.FileEventTableNameProvider;
import org.sagebionetworks.repo.model.statistics.FileEvent;

public class FileEventTableNameProviderStub extends FileEventTableNameProvider {

	private String tableName;

	public FileEventTableNameProviderStub(String tableName) {
		super(null);
		this.tableName = tableName;
	}

	@Override
	public String getTableName(FileEvent fileEvent) {
		return tableName;
	}

}
