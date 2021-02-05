package org.sagebionetworks.repo.manager.file.scanner.tables;

import java.io.IOException;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.Collectors;

import org.sagebionetworks.repo.manager.file.scanner.ScannedFileHandleAssociation;
import org.sagebionetworks.repo.manager.table.TableEntityManager;
import org.sagebionetworks.repo.manager.table.change.TableChangeMetaData;
import org.sagebionetworks.repo.model.exception.RecoverableException;
import org.sagebionetworks.repo.model.exception.UnrecoverableException;
import org.sagebionetworks.repo.model.table.TableChangeType;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.table.model.ChangeData;
import org.sagebionetworks.table.model.SparseChangeSet;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.AmazonServiceException.ErrorType;

/**
 * Implementation of an iterator over all the file handles of a single table
 * 
 * @author Marco Marasca
 */
public class TableFileHandleIterator implements Iterator<ScannedFileHandleAssociation> {
	
	private Long tableId;
	private Iterator<TableChangeMetaData> tableChangeIterator;
	
	public TableFileHandleIterator(TableEntityManager tableManager, Long tableId) {
		this.tableId = tableId;
		// TODO: This iterator fetches all the changes, but we actually need only ROW changes that contain file handle ids. 
		// We could filter by changeType in the iterator? Could we store if the change has file handles in it?
		this.tableChangeIterator = tableManager.newTableChangeIterator(tableId.toString());
	}

	@Override
	public boolean hasNext() {
		return tableChangeIterator.hasNext();
	}

	@Override
	public ScannedFileHandleAssociation next() {
		ScannedFileHandleAssociation association = new ScannedFileHandleAssociation(tableId.toString());
		
		TableChangeMetaData changeMetadata = tableChangeIterator.next();
		
		if (TableChangeType.ROW.equals(changeMetadata.getChangeType())) {
			ChangeData<SparseChangeSet> changeData;
			
			try {
				 changeData = changeMetadata.loadChangeData(SparseChangeSet.class);
			} catch (NotFoundException | IOException e) {
				throw new UnrecoverableException(e);
			} catch (AmazonServiceException e) {
				// According to the docs the service type error are for requests received by AWS that cannot be fulfilled at the moment
				// The caller can try iterating again from scratch
				if (ErrorType.Service.equals(e.getErrorType())) {
					throw new RecoverableException(e);
				}
				throw new UnrecoverableException(e);
			}
				
			if (changeData != null && changeData.getChange() != null) {
				SparseChangeSet changeSet = changeData.getChange();
				
				Set<Long> fileHandleSet = changeSet.getFileHandleIdsInSparseChangeSet();
				
				association.withFileHandleIds(fileHandleSet.stream().collect(Collectors.toList()));
			}
				
			
		}

		return association;
	}

}
