package org.sagebionetworks.repo.model.query;

public enum QueryObjectType {
	
	SUBMISSION	(SQLConstants.PREFIX_SUBSTATUS,		SQLConstants.COL_SUBSTATUS_ANNO_SUBID);
	
	private String tablePrefix;
	private String joinColumn;

	QueryObjectType(String tablePrefix, String joinColumn) {
		this.tablePrefix = tablePrefix;
		this.joinColumn = joinColumn;
	}

	public String tablePrefix() {
		return tablePrefix;
	}

	public String joinColumn() {
		return joinColumn;
	}
	
}
