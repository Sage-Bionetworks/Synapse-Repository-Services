package org.sagebionetworks.repo.model.dbo.file.download;

import java.util.List;

import org.sagebionetworks.repo.model.file.DownloadList;
import org.sagebionetworks.repo.model.file.FileHandleAssociation;
import org.sagebionetworks.repo.transactions.WriteTransactionReadCommitted;

public class BulkDownloadDAOImpl implements BulkDownloadDAO {
	

	@WriteTransactionReadCommitted
	@Override
	public DownloadList addFilesToDownloadList(String ownerId, List<FileHandleAssociation> toAdd) {
		// TODO Auto-generated method stub
		return null;
	}

	@WriteTransactionReadCommitted
	@Override
	public DownloadList removeFilesFromDownloadList(String ownerId, List<FileHandleAssociation> toRemove) {
		// TODO Auto-generated method stub
		return null;
	}

	@WriteTransactionReadCommitted
	@Override
	public DownloadList clearDownloadList(String ownerId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long getDownloadListFileCount(String ownerId) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public DownloadList getUsersDownloadList(String ownerPrincipalId) {
		// TODO Auto-generated method stub
		return null;
	}

}
