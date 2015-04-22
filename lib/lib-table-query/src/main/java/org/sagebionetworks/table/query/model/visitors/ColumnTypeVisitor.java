package org.sagebionetworks.table.query.model.visitors;

import java.util.Map;

import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.table.query.model.ColumnName;

public class ColumnTypeVisitor implements Visitor {

	private final Map<String, ColumnModel> columnNameToModelMap;
	private final boolean isAggregatedResult;

	private ColumnModel columnModel = null;
	private ColumnType columnType = null;

	public ColumnTypeVisitor(Map<String, ColumnModel> columnNameToModelMap, boolean isAggregatedResult) {
		this.columnNameToModelMap = columnNameToModelMap;
		this.isAggregatedResult = isAggregatedResult;
	}

	public ColumnModel getColumnModel() {
		return columnModel;
	}

	public ColumnType getColumnType() {
		return columnType;
	}

	public void setColumnType(ColumnType columnType) {
		if (this.columnType != null) {
			throw new IllegalStateException("ColumnType was already set to " + this.columnType + " and now is being set to " + columnType);
		}
		this.columnType = columnType;
	}

	public void setColumnReference(ColumnName nameRHS) {
		ToNameStringVisitor keyVisitor = new ToNameStringVisitor();
		nameRHS.doVisit(keyVisitor);
		ColumnModel foundColumnModel = columnNameToModelMap.get(keyVisitor.getName());
		if (foundColumnModel != null) {
			setColumnType(foundColumnModel.getColumnType());
			if (!isAggregatedResult) {
				this.columnModel = foundColumnModel;
			}
		}
	}
}
