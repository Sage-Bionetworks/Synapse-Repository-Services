package org.sagebionetworks.repo.model.query;

import org.sagebionetworks.repo.model.ACCESS_TYPE;

public enum QueryObjectType {
	
	EVALUATION	(
			SQLConstants.PREFIX_SUBSTATUS,
			SQLConstants.COL_SUBSTATUS_ANNO_SUBID,
			ACCESS_TYPE.READ_PRIVATE_SUBMISSION
			);
	
	/** The prefix of the SQL tables backing Annotations for this type. */
	private String tablePrefix;
	/** The column on which the backing typed tables for this type should be joined. */
	private String joinColumn;
	/** The permission required to see private Annotations for this type. */
	private ACCESS_TYPE privateAccessType;

	QueryObjectType(String tablePrefix, String joinColumn, ACCESS_TYPE privateAccessType) {
		this.tablePrefix = tablePrefix;
		this.joinColumn = joinColumn;
		this.privateAccessType = privateAccessType;
	}

	public String tablePrefix() {
		return tablePrefix;
	}

	public String joinColumn() {
		return joinColumn;
	}

	public ACCESS_TYPE getPrivateAccessType() {
		return privateAccessType;
	}
	
}
