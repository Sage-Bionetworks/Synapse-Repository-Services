package org.sagebionetworks.table.model;

import java.io.OutputStream;
import java.util.Iterator;

import org.sagebionetworks.repo.model.table.AbstractRow;
import org.sagebionetworks.repo.model.table.CellValue;
import org.sagebionetworks.repo.model.table.ChangeSetAccessor;
import org.sagebionetworks.repo.model.table.TableChangeType;

public class PartialRowSetAccessor implements ChangeSetAccessor {

	@Override
	public Iterator<? extends AbstractRow> getRowIterator() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Iterator<CellValue> getCellValueIterator() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public TableChangeType getChangeType() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean requiresRowLevelConflictChecking() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void writeToStream(OutputStream out) {
		// TODO Auto-generated method stub
		
	}

}
