package org.sagebionetworks.repo.manager.table.change;

import java.util.List;

import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.table.model.SparseChangeSet;

/**
 * A ChangeData wrapper of a {@link SparseChangeSet}.
 *
 */
public class RowChange implements ChangeData {
	
	SparseChangeSet sparseChangeSet;
	
	
	public RowChange(SparseChangeSet sparseChangeSet) {
		super();
		this.sparseChangeSet = sparseChangeSet;
	}

	@Override
	public List<ColumnModel> getChangeSchema() {
		return sparseChangeSet.getSchema();
	}

	public SparseChangeSet getSparseChangeSet() {
		return sparseChangeSet;
	}

}
