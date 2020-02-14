package org.sagebionetworks.repo.manager.table.change;

import java.util.Objects;

import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.table.query.util.ColumnTypeListMappings;
import org.sagebionetworks.util.ValidateArgument;

public class ListColumnIndexTableChange {

	private ListIndexTableChangeType listIndexTableChangeType;
	private Long oldColumnId;
	private ColumnModel newColumnChange;


	public static ListColumnIndexTableChange newRemoval(Long oldColumnId){
		return new ListColumnIndexTableChange(ListIndexTableChangeType.REMOVE, oldColumnId, null);
	}

	public static ListColumnIndexTableChange newAddition(ColumnModel newColumnChange){
		return new ListColumnIndexTableChange(ListIndexTableChangeType.ADD, null, newColumnChange);
	}

	public static ListColumnIndexTableChange newUpdate(Long oldColumnId, ColumnModel newColumnChange){
		return new ListColumnIndexTableChange(ListIndexTableChangeType.UPDATE, oldColumnId, newColumnChange);
	}

	private ListColumnIndexTableChange(ListIndexTableChangeType listIndexTableChangeType, Long oldColumnId, ColumnModel newColumnChange) {
		ValidateArgument.required(listIndexTableChangeType, "listChangeType");

		if(listIndexTableChangeType == ListIndexTableChangeType.ADD
				|| listIndexTableChangeType == ListIndexTableChangeType.UPDATE){
			ValidateArgument.required(newColumnChange, "newColumnChange");
			ValidateArgument.required(newColumnChange.getId(), "newColumnChange.id");
			ValidateArgument.requirement(ColumnTypeListMappings.isList(newColumnChange.getColumnType()), "newColumnChange must be a LIST type");
			if(newColumnChange.getColumnType() == ColumnType.STRING_LIST){
				ValidateArgument.required(newColumnChange.getMaximumSize(), "newColumnChange.maximumSize");
			}
		}

		if(listIndexTableChangeType == ListIndexTableChangeType.REMOVE
				|| listIndexTableChangeType == ListIndexTableChangeType.UPDATE){
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

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		ListColumnIndexTableChange that = (ListColumnIndexTableChange) o;
		return listIndexTableChangeType == that.listIndexTableChangeType &&
				Objects.equals(oldColumnId, that.oldColumnId) &&
				Objects.equals(newColumnChange, that.newColumnChange);
	}

	@Override
	public int hashCode() {
		return Objects.hash(listIndexTableChangeType, oldColumnId, newColumnChange);
	}

	@Override
	public String toString() {
		return "ListColumnIndexTableChange{" +
				"listIndexTableChangeType=" + listIndexTableChangeType +
				", oldColumnId=" + oldColumnId +
				", newColumnChange=" + newColumnChange +
				'}';
	}

	public enum ListIndexTableChangeType {
		ADD,
		REMOVE,
		UPDATE,
	}
}
