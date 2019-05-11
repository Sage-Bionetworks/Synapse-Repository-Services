package org.sagebionetworks.repo.manager.table.change;

import java.util.List;

import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.table.cluster.ColumnChangeDetails;

import com.amazonaws.services.kinesisanalyticsv2.model.UnsupportedOperationException;

public class SchemaChange implements ChangeData {
	
	List<ColumnChangeDetails> schemaChange;

	public SchemaChange(List<ColumnChangeDetails> schemaChange) {
		this.schemaChange = schemaChange;
	}

	@Override
	public List<ColumnModel> getChangeSchema() {
		throw new UnsupportedOperationException("Not sure how to implement this yet");
	}

}
