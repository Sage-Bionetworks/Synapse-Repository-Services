package org.sagebionetworks.repo.manager.file.scanner.tables;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;

import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.repo.manager.file.scanner.ScannedFileHandleAssociation;
import org.sagebionetworks.repo.manager.table.TableEntityManager;
import org.sagebionetworks.repo.model.dbo.dao.table.TableRowChangeUtils;
import org.sagebionetworks.repo.model.dbo.persistence.table.DBOTableRowChange;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.table.TableRowChange;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.table.model.SparseChangeSet;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.AmazonServiceException.ErrorType;
import com.amazonaws.services.s3.model.AmazonS3Exception;

@Service
public class TableFileHandleAssociationMapper implements RowMapper<ScannedFileHandleAssociation> {
	
	private static final Logger LOG = LogManager.getLogger(TableFileHandleAssociationMapper.class);

	private TableEntityManager tableManager;
	
	private static final RowMapper<DBOTableRowChange> DBO_MAPPER = new DBOTableRowChange().getTableMapping();
	
	@Autowired
	public TableFileHandleAssociationMapper(TableEntityManager tableManager) {
		this.tableManager = tableManager;
	}

	@Override
	public ScannedFileHandleAssociation mapRow(ResultSet rs, int rowNum) throws SQLException {
	
		TableRowChange changeMetadata = TableRowChangeUtils.ceateDTOFromDBO(DBO_MAPPER.mapRow(rs, rowNum));
		
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
