package org.sagebionetworks.table.model;

import org.sagebionetworks.repo.web.NotFoundException;

/**
 * Represents a single row a sparsely populated change set. A cell value might
 * not exist for every column of the change set. Use
 * {@link #hasCellValue(String)} to determine if value exists for a given cell.
 */
public interface SparseRow {

	/**
	 * The unique Id assigned to this row.
	 * 
	 * @return
	 */
	public Long getRowId();

	/**
	 * The unique Id assigned to this row.
	 * 
	 * @param rowId
	 */
	public void setRowId(Long rowId);
	
	/**
	 *  This row's version number.
	 * 
	 * @param rowVersionNumber
	 */
	public void setVersionNumber(Long rowVersionNumber);

	/**
	 *  This row's version number.
	 * 
	 */
	public Long getVersionNumber();
	
	/**
	 * The etag of a view row.
	 * @param etag
	 */
	public void setRowEtag(String etag);
	
	/**
	 * The etag of a view row.
	 * @return
	 */
	public String getRowEtag();

	/**
	 * Does this row have a cell value for the given column ID. Note: A NULL
	 * value is considered to be a valid value. So if this method will return
	 * 'true' if a cell value is set to NULL.
	 * 
	 * To remove a cell value use {@link #removeValue(String)}.
	 * 
	 * @param columnId
	 * @return
	 */
	public boolean hasCellValue(String columnId);

	/**
	 * Get the string value for the given column ID.
	 * 
	 * @param columnId
	 * @return
	 * @throws NotFoundException
	 *             When this row does not have a value for the given column ID.
	 *             See: {@link #hasCellValue(String)}.
	 * 
	 */
	public String getCellValue(String columnId) throws NotFoundException;

	/**
	 * Set the value for a given columnId for this row.
	 * 
	 * @param columnId
	 *            The ID of the column to assign a cell value for.
	 * @param value
	 *            The new cell value. Note: passing a NULL value is not the same
	 *            as removing the value. To remove a value use
	 *            {@link #removeValue(String)}.
	 */
	public void setCellValue(String columnId, String value);

	/**
	 * Remove the value for a given column ID. A call to
	 * {@link #hasCellValue(String)} will return 'false' for any column where
	 * the value is removed.
	 * 
	 * @param columnId
	 */
	public void removeValue(String columnId);
	
	/**
	 * The index of this row within the change set.
	 * 
	 * @return
	 */
	public int getRowIndex();
	
	/**
	 * A row with no values is treated as a row delete.
	 * 
	 * @return
	 */
	public boolean isDelete();

}
