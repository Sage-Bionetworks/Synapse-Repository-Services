package org.sagebionetworks.repo.manager.search;

import org.sagebionetworks.repo.model.table.ColumnConstants;
import org.sagebionetworks.repo.model.table.ColumnType;

/**
 * Enum that maps each Synapse {@link ColumnType} to its OpenSearch field mapping characteristics.
 * This drives index creation: determining the primary OS field category, default analyzer,
 * and ignoreAbove limits for keyword sub-fields.
 *
 * <p>Adding a new {@link ColumnType} requires adding a corresponding constant here,
 * forcing the developer to specify all required mapping attributes at compile time.
 *
 * <p>Mapping summary:
 * <ul>
 *   <li>STRING, STRING_LIST, MEDIUMTEXT, LARGETEXT: text + .keyword sub-field</li>
 *   <li>LINK: depends on effective analyzer (KEYWORD analyzer: keyword + .searchable; other: text + .keyword)</li>
 *   <li>INTEGER, DATE, INTEGER_LIST, DATE_LIST, FILEHANDLEID, SUBMISSIONID, EVALUATIONID: long</li>
 *   <li>ENTITYID, USERID, ENTITYID_LIST, USERID_LIST: keyword (ignoreAbove=256)</li>
 *   <li>DOUBLE: double</li>
 *   <li>BOOLEAN, BOOLEAN_LIST: boolean</li>
 *   <li>JSON: object (dynamic:true)</li>
 * </ul>
 */
public enum ColumnTypeToOpenSearchMapping {

	STRING       (ColumnType.STRING,        OpenSearchFieldCategory.TEXT,    TextAnalyzerBootstrapper.SCIENTIFIC_ID, "org.sagebionetworks-SCIENTIFIC", ColumnConstants.MAX_ALLOWED_STRING_SIZE.intValue(), false),
	STRING_LIST  (ColumnType.STRING_LIST,   OpenSearchFieldCategory.TEXT,    TextAnalyzerBootstrapper.SCIENTIFIC_ID, "org.sagebionetworks-SCIENTIFIC", ColumnConstants.MAX_ALLOWED_STRING_SIZE.intValue(), true),
	MEDIUMTEXT   (ColumnType.MEDIUMTEXT,    OpenSearchFieldCategory.TEXT,    TextAnalyzerBootstrapper.SCIENTIFIC_ID, "org.sagebionetworks-SCIENTIFIC", (int) ColumnConstants.MAX_MEDIUM_TEXT_CHARACTERS, false),
	LARGETEXT    (ColumnType.LARGETEXT,     OpenSearchFieldCategory.TEXT,    TextAnalyzerBootstrapper.SCIENTIFIC_ID, "org.sagebionetworks-SCIENTIFIC", 8192, false),
	LINK         (ColumnType.LINK,          OpenSearchFieldCategory.LINK,   TextAnalyzerBootstrapper.KEYWORD_ID,    "org.sagebionetworks-KEYWORD",    ColumnConstants.MAX_ALLOWED_STRING_SIZE.intValue(), false),
	INTEGER      (ColumnType.INTEGER,       OpenSearchFieldCategory.LONG,   TextAnalyzerBootstrapper.KEYWORD_ID,    "org.sagebionetworks-KEYWORD",    null, false),
	INTEGER_LIST (ColumnType.INTEGER_LIST,  OpenSearchFieldCategory.LONG,   TextAnalyzerBootstrapper.KEYWORD_ID,    "org.sagebionetworks-KEYWORD",    null, true),
	DATE         (ColumnType.DATE,          OpenSearchFieldCategory.LONG,   TextAnalyzerBootstrapper.KEYWORD_ID,    "org.sagebionetworks-KEYWORD",    null, false),
	DATE_LIST    (ColumnType.DATE_LIST,     OpenSearchFieldCategory.LONG,   TextAnalyzerBootstrapper.KEYWORD_ID,    "org.sagebionetworks-KEYWORD",    null, true),
	FILEHANDLEID (ColumnType.FILEHANDLEID,  OpenSearchFieldCategory.LONG,   TextAnalyzerBootstrapper.KEYWORD_ID,    "org.sagebionetworks-KEYWORD",    null, false),
	SUBMISSIONID (ColumnType.SUBMISSIONID,  OpenSearchFieldCategory.LONG,   TextAnalyzerBootstrapper.KEYWORD_ID,    "org.sagebionetworks-KEYWORD",    null, false),
	EVALUATIONID (ColumnType.EVALUATIONID,  OpenSearchFieldCategory.LONG,   TextAnalyzerBootstrapper.KEYWORD_ID,    "org.sagebionetworks-KEYWORD",    null, false),
	ENTITYID     (ColumnType.ENTITYID,      OpenSearchFieldCategory.KEYWORD, TextAnalyzerBootstrapper.KEYWORD_ID,   "org.sagebionetworks-KEYWORD",    256, false),
	USERID       (ColumnType.USERID,        OpenSearchFieldCategory.KEYWORD, TextAnalyzerBootstrapper.KEYWORD_ID,   "org.sagebionetworks-KEYWORD",    256, false),
	ENTITYID_LIST(ColumnType.ENTITYID_LIST, OpenSearchFieldCategory.KEYWORD, TextAnalyzerBootstrapper.KEYWORD_ID,   "org.sagebionetworks-KEYWORD",    256, true),
	USERID_LIST  (ColumnType.USERID_LIST,   OpenSearchFieldCategory.KEYWORD, TextAnalyzerBootstrapper.KEYWORD_ID,   "org.sagebionetworks-KEYWORD",    256, true),
	DOUBLE       (ColumnType.DOUBLE,        OpenSearchFieldCategory.DOUBLE, TextAnalyzerBootstrapper.KEYWORD_ID,    "org.sagebionetworks-KEYWORD",    null, false),
	BOOLEAN      (ColumnType.BOOLEAN,       OpenSearchFieldCategory.BOOLEAN, TextAnalyzerBootstrapper.KEYWORD_ID,   "org.sagebionetworks-KEYWORD",    null, false),
	BOOLEAN_LIST (ColumnType.BOOLEAN_LIST,  OpenSearchFieldCategory.BOOLEAN, TextAnalyzerBootstrapper.KEYWORD_ID,   "org.sagebionetworks-KEYWORD",    null, true),
	JSON         (ColumnType.JSON,          OpenSearchFieldCategory.JSON,   TextAnalyzerBootstrapper.STANDARD_ID,   "org.sagebionetworks-STANDARD",   null, false);

	/**
	 * The categories of OpenSearch field types used in search index mappings.
	 */
	public enum OpenSearchFieldCategory {
		TEXT, KEYWORD, LONG, DOUBLE, BOOLEAN, JSON, LINK
	}

	private final ColumnType columnType;
	private final OpenSearchFieldCategory fieldCategory;
	private final Long defaultAnalyzerId;
	private final String defaultAnalyzerQualifiedName;
	private final Integer ignoreAbove;
	private final boolean isList;

	ColumnTypeToOpenSearchMapping(ColumnType columnType, OpenSearchFieldCategory fieldCategory,
			Long defaultAnalyzerId, String defaultAnalyzerQualifiedName, Integer ignoreAbove,
			boolean isList) {
		this.columnType = columnType;
		this.fieldCategory = fieldCategory;
		this.defaultAnalyzerId = defaultAnalyzerId;
		this.defaultAnalyzerQualifiedName = defaultAnalyzerQualifiedName;
		this.ignoreAbove = ignoreAbove;
		this.isList = isList;
	}

	/**
	 * Lookup the mapping info for a given ColumnType.
	 *
	 * @param type The Synapse column type
	 * @return The mapping info
	 * @throws IllegalArgumentException if the type has no mapping
	 */
	public static ColumnTypeToOpenSearchMapping getInfoForType(ColumnType type) {
		for (ColumnTypeToOpenSearchMapping info : values()) {
			if (info.columnType == type) {
				return info;
			}
		}
		throw new IllegalArgumentException("Unknown ColumnType: " + type);
	}

	public ColumnType getColumnType() {
		return columnType;
	}

	public OpenSearchFieldCategory getFieldCategory() {
		return fieldCategory;
	}

	public Long getDefaultAnalyzerId() {
		return defaultAnalyzerId;
	}

	public String getDefaultAnalyzerQualifiedName() {
		return defaultAnalyzerQualifiedName;
	}

	public Integer getIgnoreAbove() {
		return ignoreAbove;
	}

	// Static convenience methods for callers that operate on ColumnType directly

	public static boolean isTextType(ColumnType columnType) {
		return getInfoForType(columnType).fieldCategory == OpenSearchFieldCategory.TEXT;
	}

	public static boolean isKeywordType(ColumnType columnType) {
		return getInfoForType(columnType).fieldCategory == OpenSearchFieldCategory.KEYWORD;
	}

	public static boolean isLongType(ColumnType columnType) {
		return getInfoForType(columnType).fieldCategory == OpenSearchFieldCategory.LONG;
	}

	public static boolean isDoubleType(ColumnType columnType) {
		return getInfoForType(columnType).fieldCategory == OpenSearchFieldCategory.DOUBLE;
	}

	public static boolean isBooleanType(ColumnType columnType) {
		return getInfoForType(columnType).fieldCategory == OpenSearchFieldCategory.BOOLEAN;
	}

	public static boolean isListType(ColumnType columnType) {
		return getInfoForType(columnType).isList;
	}

	public static boolean isJsonType(ColumnType columnType) {
		return getInfoForType(columnType).fieldCategory == OpenSearchFieldCategory.JSON;
	}

	public static boolean isLinkType(ColumnType columnType) {
		return getInfoForType(columnType).fieldCategory == OpenSearchFieldCategory.LINK;
	}

	public static boolean isNumericType(ColumnType columnType) {
		OpenSearchFieldCategory cat = getInfoForType(columnType).fieldCategory;
		return cat == OpenSearchFieldCategory.LONG || cat == OpenSearchFieldCategory.DOUBLE;
	}

	public static Long getDefaultAnalyzerId(ColumnType columnType) {
		return getInfoForType(columnType).defaultAnalyzerId;
	}

	public static String getDefaultAnalyzerQualifiedName(ColumnType columnType) {
		return getInfoForType(columnType).defaultAnalyzerQualifiedName;
	}

	public static Integer getIgnoreAbove(ColumnType columnType) {
		return getInfoForType(columnType).ignoreAbove;
	}
}
