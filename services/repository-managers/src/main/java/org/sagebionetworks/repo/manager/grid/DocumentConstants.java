package org.sagebionetworks.repo.manager.grid;

/**
 * Constants for the Grid document schema.
 * 
 * @see <a href=
 *      "https://sagebionetworks.jira.com/wiki/spaces/PLFM/pages/4123361355/Grid+Implementation+Using+JSON+Joy">Grid
 *      Implementation Using JSON Joy</a>
 */
public class DocumentConstants {

	/**
	 * The semantic version of the document schema. This value will change anytime
	 * the schema changes.
	 */
	public static final String DOC_VERSION = "doc_version";

	/**
	 * Vector that captures the name of columns in their natural order (order they
	 * were added). Vectors are append-only (up to 256 values) and allow LWW changes
	 * to each index.
	 */
	public static final String COLUMN_NAMES = "columnNames";

	/**
	 * Array that defines the order columns should be displayed. Each value is an
	 * index into columnNames. The array can be reordered.
	 */
	public static final String COLUMN_ORDER = "columnOrder";

	/**
	 * Array of row objects. Each row contains data and metadata properties. The
	 * array represents the mutable row order.
	 */
	public static final String ROWS = "rows";

	/**
	 * The row data vector represents the data in a row. The order matches the
	 * columnNames vector. Each cell can be changed to a new constant. Cells with
	 * identical values typically share a reference.
	 */
	public static final String DATA = "data";

	/**
	 * Reference to a constant containing JSON serialization of [rowId, rowVersion,
	 * etag]. Maps a row back to its source from a Synapse Table or View. Omitted if
	 * items are not defined.
	 */
	public static final String SYNAPSE_ROW = "synapseRow";

	/**
	 * Object containing supplemental information about the row. Should be treated
	 * as read-only for all replicas other than the hub. May be omitted if no
	 * additional information is included.
	 */
	public static final String METADATA = "metadata";

	/**
	 * Reference to a constant containing JSON serialization of the ValidationResult
	 * object. The ValidationResult is identical to results provided for Entity
	 * Validation.
	 */
	public static final String ROW_VALIDATION = "rowValidation";
}
