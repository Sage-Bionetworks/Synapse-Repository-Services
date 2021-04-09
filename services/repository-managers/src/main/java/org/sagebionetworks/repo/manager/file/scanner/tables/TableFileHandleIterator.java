package org.sagebionetworks.repo.manager.file.scanner.tables;

import java.io.IOException;
import java.util.Iterator;
import java.util.Set;

import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
import com.amazonaws.services.s3.model.AmazonS3Exception;

/**
 * Implementation of an iterator over all the file handles of a single table
 * 
 * @author Marco Marasca
 */
public class TableFileHandleIterator implements Iterator<ScannedFileHandleAssociation> {
	
	private static final Logger LOG = LogManager.getLogger(TableFileHandleIterator.class);
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
		ScannedFileHandleAssociation association = new ScannedFileHandleAssociation(tableId);
		
		TableChangeMetaData changeMetadata = tableChangeIterator.next();
		
		if (TableChangeType.ROW.equals(changeMetadata.getChangeType())) {
			ChangeData<SparseChangeSet> changeData;
			
			try {
				 changeData = changeMetadata.loadChangeData(SparseChangeSet.class);
			} catch (IOException e) {
				throw new UnrecoverableException(e);
			} catch (NotFoundException e) {
				LOG.warn("Could not load change data for table " + tableId + " (Change Number: " + changeMetadata.getChangeNumber() + "): " + e.getMessage(), e);
				return association;
			} catch (AmazonServiceException e) {
				// If the key wasn't found for the change data we cannot do anything about it, see PLFM-6651
				if (e instanceof AmazonS3Exception && e.getStatusCode() == HttpStatus.SC_NOT_FOUND) {
					LOG.warn("Change data for table " + tableId + " not found (Change Number: " + changeMetadata.getChangeNumber() + "): " + e.getMessage(), e);
					return association;
				}
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
				
				association.withFileHandleIds(fileHandleSet);
			}
				
			
		}

		return association;
	}

}
