package org.sagebionetworks.table.model;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.sagebionetworks.repo.model.table.AbstractRow;
import org.sagebionetworks.repo.model.table.CellValue;
import org.sagebionetworks.repo.model.table.ChangeSetAccessor;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.RawRowSet;
import org.sagebionetworks.repo.model.table.TableChangeType;
import org.sagebionetworks.table.cluster.utils.TableModelUtils;
import org.sagebionetworks.util.ValidateArgument;

/**
 * Provides abstract access to a RawRowset.
 * 
 * @author John
 *
 */
public class FullRowSetAccessor implements ChangeSetAccessor  {
	
	List<ColumnModel> schema;
	RawRowSet rowset;
	List<CellValue> cellValues;


	@Override
	public Iterator<? extends AbstractRow> getRowIterator() {
		return rowset.getRows().iterator();
	}

	@Override
	public Iterator<CellValue> getCellValueIterator() {
		return cellValues.iterator();
	}

	@Override
	public TableChangeType getChangeType() {
		return TableChangeType.ROW;
	}

	@Override
	public boolean requiresRowLevelConflictChecking() {
		return true;
	}

	@Override
	public void writeToStream(OutputStream out) throws IOException {
		TableModelUtils.validateAnWriteToCSVgz(schema, rowset, out);
	}

}
