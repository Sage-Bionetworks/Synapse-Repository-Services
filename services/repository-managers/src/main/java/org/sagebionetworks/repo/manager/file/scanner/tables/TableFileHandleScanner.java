package org.sagebionetworks.repo.manager.file.scanner.tables;

import java.io.IOException;
import java.util.Iterator;
import java.util.Set;

import org.apache.commons.collections4.iterators.TransformIterator;
import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.repo.manager.file.scanner.FileHandleAssociationScanner;
import org.sagebionetworks.repo.manager.file.scanner.ScannedFileHandleAssociation;
import org.sagebionetworks.repo.manager.table.TableEntityManager;
import org.sagebionetworks.repo.model.IdRange;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.table.TableRowChange;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.table.model.SparseChangeSet;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.AmazonServiceException.ErrorType;
import com.amazonaws.services.s3.model.AmazonS3Exception;

public class TableFileHandleScanner implements FileHandleAssociationScanner {
	
	private static final Logger LOG = LogManager.getLogger(TableFileHandleScanner.class);
	
	private static final long MAX_SCAN_ID_RANGE = 10_000;
			
	private TableEntityManager tableManager;
	
	public TableFileHandleScanner(TableEntityManager tableManager) {
		this.tableManager = tableManager;
	}
	
	@Override
	public long getMaxIdRangeSize() {
		return MAX_SCAN_ID_RANGE;
	}
	
	@Override
	public IdRange getIdRange() {
		return tableManager.getTableRowChangeIdRange();
	}

	@Override
	public Iterable<ScannedFileHandleAssociation> scanRange(IdRange range) {
		final Iterator<TableRowChange> changeIterator = tableManager.newTableRowChangeWithFileRefsIterator(range);
		
		// An iterable has only one method that returns the iterator
		return () ->  new TransformIterator<>(changeIterator, this::mapTableRowChange);
	}
	
	public ScannedFileHandleAssociation mapTableRowChange(TableRowChange changeMetadata) {
		final Long tableId = KeyFactory.stringToKey(changeMetadata.getTableId());
		
		ScannedFileHandleAssociation association = new ScannedFileHandleAssociation(tableId);
		
		try {
			SparseChangeSet changeSet = tableManager.getSparseChangeSet(changeMetadata);
			
			Set<Long> fileHandleSet = changeSet.getFileHandleIdsInSparseChangeSet();
			
			association.withFileHandleIds(fileHandleSet);
		} catch (IOException e) {
			throw new IllegalStateException(e);
		} catch (NotFoundException e) {
			LOG.warn("Could not load change data for table " + tableId + " (Row Version: " + changeMetadata.getRowVersion() + "): " + e.getMessage(), e);
			return association;
		} catch (AmazonServiceException e) {
			// If the key wasn't found for the change data we cannot do anything about it, see PLFM-6651
			if (e instanceof AmazonS3Exception && e.getStatusCode() == HttpStatus.SC_NOT_FOUND) {
				LOG.warn("Change data for table " + tableId + " not found (Row Version: " + changeMetadata.getRowVersion() + "): " + e.getMessage(), e);
				return association;
			}
			// According to the docs the service type error are for requests received by AWS that cannot be fulfilled at the moment
			// The caller can try iterating again from scratch
			if (ErrorType.Service.equals(e.getErrorType())) {
				throw new RecoverableMessageException(e);
			}
			throw e;
		}
		
		return association;
	}
			
}
