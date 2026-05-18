package org.sagebionetworks.table.query.util;

import org.sagebionetworks.repo.model.table.ColumnConstants;
import org.sagebionetworks.repo.model.table.ColumnType;

/**
 * Registry of ColumnTypes that are treated as Lists. Maps each list ColumnType
 * to its non-list counterpart and carries the per-element size metadata needed
 * to estimate in-memory row sizes.
 *
 * <p>Each entry encodes two size-related properties:</p>
 * <ul>
 *   <li><b>defaultMaxCharsPerItem</b> — the maximum number of characters in the
 *       string representation of a single element (e.g. 20 for a Long, 5 for a
 *       boolean "false"). For STRING_LIST this is {@link ColumnConstants#MAX_ALLOWED_STRING_SIZE}
 *       and can be overridden per-column via {@code maximumSize}.</li>
 *   <li><b>bytesPerChar</b> — bytes required per character in memory.
 *       STRING_LIST uses {@link ColumnConstants#MAX_BYTES_PER_CHAR_UTF_8} (4)
 *       because string values can contain arbitrary Unicode. All other list types
 *       store ASCII-only representations (digits, signs, letters) so each
 *       character is exactly 1 byte.</li>
 * </ul>
 *
 * <p>The product {@code bytesPerChar * maxCharsPerItem * maxListLength} gives the
 * maximum in-memory byte footprint of one list column value. This is capped at
 * {@link ColumnConstants#MAX_BYTES_PER_LIST_COLUMN_ESTIMATE} so that 152 list
 * columns fit within MySQL's 64 KB inline row limit.</p>
 */
public enum ColumnTypeListMappings {
	STRING(ColumnType.STRING,   ColumnType.STRING_LIST,   ColumnConstants.MAX_ALLOWED_STRING_SIZE,                          ColumnConstants.MAX_BYTES_PER_CHAR_UTF_8),
	INTEGER(ColumnType.INTEGER, ColumnType.INTEGER_LIST,  Long.valueOf(ColumnConstants.MAX_INTEGER_CHARACTERS_AS_STRING),   1),
	BOOLEAN(ColumnType.BOOLEAN, ColumnType.BOOLEAN_LIST,  Long.valueOf(ColumnConstants.MAX_BOOLEAN_CHARACTERS_AS_STRING),   1),
	DATE(ColumnType.DATE,       ColumnType.DATE_LIST,     Long.valueOf(ColumnConstants.MAX_INTEGER_CHARACTERS_AS_STRING),   1),
	ENTITYID(ColumnType.ENTITYID, ColumnType.ENTITYID_LIST, Long.valueOf(ColumnConstants.MAX_ENTITY_ID_CHARACTERS_AS_STRING), 1),
	USERID(ColumnType.USERID,   ColumnType.USERID_LIST,   Long.valueOf(ColumnConstants.MAX_INTEGER_CHARACTERS_AS_STRING),   1);

	private ColumnType nonListType;
	private ColumnType listType;
	private Long defaultMaxCharsPerItem;
	private int bytesPerChar;

	ColumnTypeListMappings(ColumnType nonListType, ColumnType listType, Long defaultMaxCharsPerItem, int bytesPerChar) {
		this.nonListType = nonListType;
		this.listType = listType;
		this.defaultMaxCharsPerItem = defaultMaxCharsPerItem;
		this.bytesPerChar = bytesPerChar;
	}

	public ColumnType getNonListType() {
		return nonListType;
	}

	public ColumnType getListType() {
		return listType;
	}

	public static ColumnTypeListMappings forListType(ColumnType listType) {
		for (ColumnTypeListMappings mappings : values()) {
			if (mappings.getListType() == listType) {
				return mappings;
			}
		}
		throw new IllegalArgumentException(listType + " is not a list ColumnType");
	}

	public static ColumnTypeListMappings forNonListType(ColumnType nonListType) {
		for (ColumnTypeListMappings mappings : values()) {
			if (mappings.getNonListType() == nonListType) {
				return mappings;
			}
		}
		throw new IllegalArgumentException(
				nonListType + " is not a ColumnType that has a list type associated with it");
	}

	public static ColumnType nonListType(ColumnType listType) {
		return forListType(listType).getNonListType();
	}

	public static ColumnType listType(ColumnType nonListType) {
		return forNonListType(nonListType).getListType();
	}

	public static boolean isList(ColumnType columnType) {
		try {
			forListType(columnType);
			return true;
		} catch (IllegalArgumentException e) {
			return false;
		}
	}

	/**
	 * Returns the effective maximum number of characters per list element: uses
	 * {@code maximumSize} when provided, otherwise falls back to this type's
	 * built-in default (e.g. 1,000 for STRING_LIST, 20 for INTEGER_LIST).
	 *
	 * @param maximumSize the column's maximumSize field (may be null)
	 */
	public long getEffectiveMaxCharsPerItem(Long maximumSize) {
		return maximumSize != null ? maximumSize : defaultMaxCharsPerItem;
	}

	/**
	 * Returns the maximum in-memory byte size for one value of this list column type,
	 * capped at {@link ColumnConstants#MAX_BYTES_PER_LIST_COLUMN_ESTIMATE}.
	 *
	 * @param maxCharsPerItem the column's maximumSize (null → use this type's default)
	 * @param maxListLength   the column's maximumListLength (null → derive from the
	 *                        {@link ColumnConstants#MAX_ALLOWED_LIST_TOTAL_CHARACTERS} budget)
	 */
	public int calculateMaxSize(Long maxCharsPerItem, Long maxListLength) {
		long effectiveMaxCharsPerItem = getEffectiveMaxCharsPerItem(maxCharsPerItem);
		long effectiveMaxListLength = maxListLength != null ? maxListLength
				: ColumnConstants.MAX_ALLOWED_LIST_TOTAL_CHARACTERS / effectiveMaxCharsPerItem;
		return (int) Math.min(
				bytesPerChar * effectiveMaxCharsPerItem * effectiveMaxListLength,
				ColumnConstants.MAX_BYTES_PER_LIST_COLUMN_ESTIMATE);
	}
}
