package org.sagebionetworks.repo.model.table;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;

import org.sagebionetworks.repo.model.table.AbstractRow;
import org.sagebionetworks.repo.model.table.TableChangeType;

/**
 * Provides abstract access to all types of row change set.
 * 
 * @author John
 * 
 */
public interface ChangeSetAccessor {

	/**
	 * Iterate over all of the rows of this change set.
	 * 
	 * @return
	 */
	Iterator<? extends AbstractRow> getRowIterator();

	/**
	 * Iterate over all cell of this change set.
	 * 
	 * @return
	 */
	Iterator<CellValue> getCellValueIterator();

	/**
	 * The change type for the change set.
	 * 
	 * @return
	 */
	TableChangeType getChangeType();

	/**
	 * Does this change set require row level conflict checking? Row level
	 * conflict checking is required for change sets that change the entire row.
	 * Partial row changes do not require row level checking.
	 * 
	 * @return
	 */
	boolean requiresRowLevelConflictChecking();
	
	/**
	 * Write this change set to the given output stream.
	 * 
	 * @param out
	 * @throws IOException 
	 */
	public void writeToStream(OutputStream out) throws IOException;
}
