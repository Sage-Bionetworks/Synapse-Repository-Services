package org.sagebionetworks.repo.manager.table.change;

import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.table.query.util.ColumnTypeListMappings;
import org.sagebionetworks.util.ValidateArgument;

public class ListColumnIndexTableChange {

	ListIndexTableChangeType listIndexTableChangeType;
	Long oldColumnId;
	ColumnModel newColumnChange;

	//TODO: test
	public ListColumnIndexTableChange(ListIndexTableChangeType listIndexTableChangeType, Long oldColumnId, ColumnModel newColumnChange) {
		ValidateArgument.required(listIndexTableChangeType, "listChangeType");

		if(listIndexTableChangeType == ListIndexTableChangeType.ADD
				|| listIndexTableChangeType == ListIndexTableChangeType.RENAME
				|| listIndexTableChangeType == ListIndexTableChangeType.TYPE_CHANGE){
			ValidateArgument.required(newColumnChange, "newColumnChange");
			ValidateArgument.required(newColumnChange.getId(), "newColumnChange.id");
			ValidateArgument.requirement(ColumnTypeListMappings.isList(newColumnChange.getColumnType()), "newColumnChange must be a LIST type");
			if(newColumnChange.getColumnType() == ColumnType.STRING_LIST){
				ValidateArgument.required(newColumnChange.getMaximumSize(), "newColumnChange.maximumSize");
			}
		}

		if(listIndexTableChangeType == ListIndexTableChangeType.REMOVE
				|| listIndexTableChangeType == ListIndexTableChangeType.RENAME
				|| listIndexTableChangeType == ListIndexTableChangeType.TYPE_CHANGE){
			ValidateArgument.required(oldColumnId, "oldColumnId");
		}

		this.listIndexTableChangeType = listIndexTableChangeType;
		this.oldColumnId = oldColumnId;
		this.newColumnChange = newColumnChange;
	}

	public ListIndexTableChangeType getListIndexTableChangeType() {
		return listIndexTableChangeType;
	}

	public Long getOldColumnId() {
		return oldColumnId;
	}

	public ColumnModel getNewColumnChange() {
		return newColumnChange;
	}

	public boolean requiresTableRepopulation(){
		return this.listIndexTableChangeType == ListIndexTableChangeType.ADD;
	}

	public enum ListIndexTableChangeType {
		ADD,
		REMOVE,
		RENAME,
		TYPE_CHANGE;
	}
}
