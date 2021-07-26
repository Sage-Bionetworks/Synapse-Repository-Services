package org.sagebionetworks.repo.manager.table.migration;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Set;

import org.apache.http.HttpStatus;
import org.sagebionetworks.aws.CannotDetermineBucketLocationException;
import org.sagebionetworks.aws.SynapseS3Client;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.manager.migration.MigrationTypeListener;
import org.sagebionetworks.repo.manager.table.ColumnModelManager;
import org.sagebionetworks.repo.model.dbo.persistence.table.DBOTableRowChange;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.SparseChangeSetDto;
import org.sagebionetworks.repo.model.table.TableChangeType;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.table.cluster.utils.TableModelUtils;
import org.sagebionetworks.table.model.SparseChangeSet;
import org.sagebionetworks.util.TemporaryCode;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.AmazonServiceException.ErrorType;
import com.amazonaws.services.s3.model.AmazonS3Exception;

@TemporaryCode(author = "marco.marasca@sagebase.org", comment = "This migration listener should be removed after the first migration deployed to production")
@Service
public class TableRowChangeMigrationListener implements MigrationTypeListener<DBOTableRowChange> {

	private IdGenerator idGenerator;
	
	private SynapseS3Client s3Client;
	
	private ColumnModelManager columnModelManager;
	
	@Autowired
	public TableRowChangeMigrationListener(IdGenerator idGenerator, SynapseS3Client s3Client, ColumnModelManager columnModelManager) {
		this.idGenerator = idGenerator;
		this.s3Client = s3Client;
		this.columnModelManager = columnModelManager;
	}
	
	@Override
	public boolean supports(MigrationType type) {
		return MigrationType.TABLE_CHANGE == type;
	}

	@Override
	public void beforeCreateOrUpdate(List<DBOTableRowChange> batch) {
		batch.forEach(change -> {
			if (change.getId() == null) {
				change.setId(idGenerator.generateNewId(IdType.TABLE_CHANGE_ID));
			}
			if (change.getHasFileRefs() == null) {
				change.setHasFileRefs(hasFileRefs(change));
			}
		});
		
	}

	@Override
	public void afterCreateOrUpdate(List<DBOTableRowChange> delta) {
		// Nothing to do
	}
	
	private boolean hasFileRefs(DBOTableRowChange change) {
		if (!TableChangeType.ROW.name().equals(change.getChangeType())) {
			return false;
		}
		
		Set<Long> fileHandleIds;
		
		try (InputStream inputStream = s3Client.getObject(change.getBucket(), change.getKeyNew()).getObjectContent()) {
			SparseChangeSetDto changeSet = TableModelUtils.readSparseChangeSetDtoFromGzStream(inputStream);
			
			List<ColumnModel> schema = columnModelManager.getAndValidateColumnModels(changeSet.getColumnIds());
			
			fileHandleIds = new SparseChangeSet(changeSet, schema).getFileHandleIdsInSparseChangeSet();
		} catch (CannotDetermineBucketLocationException e) {
			// If the bucket does not exist (some old tests) then we cannot read the change anyway
			return false;
		} catch (NotFoundException e) {
			// If the column model does not exist, nothing we can read?
			return false;
		} catch (AmazonServiceException e) {
			// The key does not exist anymore (some old tests), we cannot read the change anyway
			if (e instanceof AmazonS3Exception && e.getStatusCode() == HttpStatus.SC_NOT_FOUND) {
				return false;
			}
			if (ErrorType.Service.equals(e.getErrorType())) {
				throw new RecoverableMessageException(e);
			}
			throw e;
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
		
		return fileHandleIds != null && !fileHandleIds.isEmpty();
	}

}
