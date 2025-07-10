package org.sagebionetworks.repo.manager.grid.row.translator;

import org.sagebionetworks.repo.model.table.ColumnType;

public enum ColumnTypeToConType {
	// Strings
	STRING(ColumnType.STRING, new StringTranslator()),
	// doubles
	DOUBLE(ColumnType.DOUBLE, new DoubleTranslator()),
	// numbers
	INTEGER(ColumnType.INTEGER, new LongTranslator()),
	// boolean
	BOOLEAN(ColumnType.BOOLEAN, new BooleanTranslator()),
	// date
	DATE(ColumnType.DATE, new LongTranslator()),
	// filehandle
	FILEHANDLEID(ColumnType.FILEHANDLEID, new LongTranslator()),
	// entity id.
	ENTITYID(ColumnType.ENTITYID, new StringTranslator()),
	// submission id
	SUBMISSIONID(ColumnType.SUBMISSIONID, new LongTranslator()),
	// evaluation id
	EVALUATIONID(ColumnType.EVALUATIONID, new LongTranslator()),
	// link
	LINK(ColumnType.LINK, new StringTranslator()),
	// med text
	MEDIUMTEXT(ColumnType.MEDIUMTEXT, new StringTranslator()),
	// large text
	LARGETEXT(ColumnType.LARGETEXT, new StringTranslator()),
	// user id
	USERID(ColumnType.USERID, new LongTranslator()),
	// string list
	STRING_LIST(ColumnType.STRING_LIST, new ArrayTranslator()),
	// integer list
	INTEGER_LIST(ColumnType.INTEGER_LIST, new ArrayTranslator()),
	// boolean list
	BOOLEAN_LIST(ColumnType.BOOLEAN_LIST, new ArrayTranslator()),
	// date list
	DATE_LIST(ColumnType.DATE_LIST, new ArrayTranslator()),
	// entity ID lsit
	ENTITYID_LIST(ColumnType.ENTITYID_LIST, new ArrayTranslator()),
	// user ID list
	USERID_LIST(ColumnType.USERID_LIST, new ArrayTranslator()),
	// JSON
	JSON(ColumnType.JSON, new JSONTranslator());

	private final ColumnType columnType;
	private final Translator translator;

	private ColumnTypeToConType(ColumnType columnType, Translator translator) {
		this.columnType = columnType;
		this.translator = translator;
	}

	/**
	 * Lookup a {@link ColumnTypeToConType} for the provied {@link ColumnType}.
	 * 
	 * @param ct
	 * @return
	 */
	public static ColumnTypeToConType lookUpType(ColumnType ct) {
		for (ColumnTypeToConType cttc : ColumnTypeToConType.values()) {
			if (cttc.columnType.equals(ct)) {
				return cttc;
			}
		}
		throw new IllegalArgumentException("Unknown type: " + ct);
	}

	public ColumnType getColumnType() {
		return columnType;
	}

	/**
	 * The translator to be used for this type.
	 * 
	 * @return
	 */
	public Translator getTranslator() {
		return translator;
	}

}
