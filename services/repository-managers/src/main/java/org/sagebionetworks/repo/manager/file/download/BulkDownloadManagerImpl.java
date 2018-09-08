package org.sagebionetworks.repo.manager.file.download;

import java.util.LinkedList;
import java.util.List;

import org.sagebionetworks.common.util.progress.SynchronizedProgressCallback;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.table.TableQueryManager;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityChildrenRequest;
import org.sagebionetworks.repo.model.EntityChildrenResponse;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dbo.file.download.BulkDownloadDAO;
import org.sagebionetworks.repo.model.file.DownloadList;
import org.sagebionetworks.repo.model.file.FileHandleAssociation;
import org.sagebionetworks.repo.model.table.Query;
import org.sagebionetworks.repo.model.table.QueryBundleRequest;
import org.sagebionetworks.repo.model.table.QueryResultBundle;
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.repo.model.table.TableFailedException;
import org.sagebionetworks.repo.model.table.TableUnavailableException;
import org.sagebionetworks.repo.transactions.WriteTransactionReadCommitted;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.table.query.ParseException;
import org.sagebionetworks.util.ValidateArgument;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.sagebionetworks.workers.util.semaphore.LockUnavilableException;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Lists;

public class BulkDownloadManagerImpl implements BulkDownloadManager {

	public static final String FILES_CAN_ONLY_BE_ADDED_FROM_A_FILE_VIEW_QUERY = "Files can only be added from a file view query.";
	public static final int MAX_FILES_PER_DOWNLOAD_LIST = 100;
	public static final long QUERY_ONLY_PART_MASK = 0x1;

	public static final String EXCEEDED_MAX_NUMBER_ROWS = "Exceeded the maximum number of "
			+ MAX_FILES_PER_DOWNLOAD_LIST + " files.";

	@Autowired
	EntityManager entityManager;

	@Autowired
	NodeDAO nodeDoa;

	@Autowired
	BulkDownloadDAO bulkDownloadDao;

	@Autowired
	TableQueryManager tableQueryManager;

	@WriteTransactionReadCommitted
	@Override
	public DownloadList addFilesFromFolder(UserInfo user, String folderId) {
		ValidateArgument.required(user, "UserInfo");
		ValidateArgument.required(folderId, "Folder ID");
		List<EntityType> includeTypes = Lists.newArrayList(EntityType.file);
		String nextPageToken = null;
		do {
			EntityChildrenRequest entityChildrenRequest = new EntityChildrenRequest();
			entityChildrenRequest.setIncludeTypes(includeTypes);
			entityChildrenRequest.setParentId(folderId);
			entityChildrenRequest.setNextPageToken(nextPageToken);
			// page through the children of the given container.
			EntityChildrenResponse entityChildrenResponse = entityManager.getChildren(user, entityChildrenRequest);
			// get the files handles for the resulting files
			List<String> entityIds = new LinkedList<>();
			for (EntityHeader header : entityChildrenResponse.getPage()) {
				entityIds.add(header.getId());
			}
			// get the files handle associations for each file.
			List<FileHandleAssociation> toAdd = nodeDoa.getFileHandleAssociationsForCurrentVersion(entityIds);
			attemptToAddFilesToUsersDownloadList(user, toAdd);

			// use the token to get the next page.
			nextPageToken = entityChildrenResponse.getNextPageToken();
			// continue as long as we have a next page token.
		} while (nextPageToken != null);
		// return the final state of the download list.
		return bulkDownloadDao.getUsersDownloadList(user.getId().toString());
	}

	/**
	 * Attempt to add the given files to the user's download list.
	 * 
	 * @param user
	 * @param toAdd
	 * @throws IllegalArgumentException If the resulting total number of files
	 *                                  exceeds the limit.
	 * 
	 */
	DownloadList attemptToAddFilesToUsersDownloadList(UserInfo user, List<FileHandleAssociation> toAdd) {
		DownloadList list = bulkDownloadDao.addFilesToDownloadList(user.getId().toString(), toAdd);
		if (list.getFilesToDownload().size() > MAX_FILES_PER_DOWNLOAD_LIST) {
			throw new IllegalArgumentException(EXCEEDED_MAX_NUMBER_ROWS);
		}
		return list;
	}

	@WriteTransactionReadCommitted
	@Override
	public DownloadList addFilesFromQuery(UserInfo user, Query query)
			throws DatastoreException, NotFoundException, TableFailedException, RecoverableMessageException {
		ValidateArgument.required(user, "UserInfo");
		ValidateArgument.required(query, "Query");
		try {
			SynchronizedProgressCallback callback = new SynchronizedProgressCallback();
			QueryBundleRequest queryBundle = new QueryBundleRequest();
			queryBundle.setPartMask(QUERY_ONLY_PART_MASK);
			queryBundle.setQuery(query);
			/*
			 * Setting a limit ensures we never read too many rows. Using a limit of max
			 * plus one provides a mechanism for detecting a query result that would yield
			 * too many rows.
			 */
			query.setLimit(MAX_FILES_PER_DOWNLOAD_LIST + 1L);
			QueryResultBundle queryResult = this.tableQueryManager.queryBundle(callback, user, queryBundle);
			// validate this is query against a file view.
			String tableId = queryResult.getQueryResult().getQueryResults().getTableId();
			EntityType tableType = entityManager.getEntityType(user, tableId);
			if (!EntityType.entityview.equals(tableType)) {
				throw new IllegalArgumentException(FILES_CAN_ONLY_BE_ADDED_FROM_A_FILE_VIEW_QUERY);
			}
			List<Row> rows = queryResult.getQueryResult().getQueryResults().getRows();
			if (rows.size() > MAX_FILES_PER_DOWNLOAD_LIST) {
				throw new IllegalArgumentException(EXCEEDED_MAX_NUMBER_ROWS);
			}
			// get the files handles for the resulting files
			List<String> entityIds = new LinkedList<>();
			for (Row row : rows) {
				entityIds.add(row.getRowId().toString());
			}
			// get the files handle associations for each file.
			List<FileHandleAssociation> toAdd = nodeDoa.getFileHandleAssociationsForCurrentVersion(entityIds);
			return attemptToAddFilesToUsersDownloadList(user, toAdd);

		} catch (LockUnavilableException | TableUnavailableException e) {
			// can re-try when the view becomes available.
			throw new RecoverableMessageException();
		} catch (ParseException e) {
			throw new IllegalArgumentException(e);
		}
	}

	@WriteTransactionReadCommitted
	@Override
	public DownloadList addFileHandleAssociations(UserInfo user, List<FileHandleAssociation> toAdd) {
		ValidateArgument.required(user, "UserInfo");
		ValidateArgument.required(toAdd, "List<FileHandleAssociation>");
		return attemptToAddFilesToUsersDownloadList(user, toAdd);
	}

	@WriteTransactionReadCommitted
	@Override
	public DownloadList removeFileHandleAssociations(UserInfo user, List<FileHandleAssociation> toRemove) {
		ValidateArgument.required(user, "UserInfo");
		ValidateArgument.required(toRemove, "List<FileHandleAssociation>");
		return bulkDownloadDao.removeFilesFromDownloadList(user.getId().toString(), toRemove);
	}

	@Override
	public DownloadList getDownloadList(UserInfo user) {
		ValidateArgument.required(user, "UserInfo");
		return bulkDownloadDao.getUsersDownloadList(user.getId().toString());
	}

	@WriteTransactionReadCommitted
	@Override
	public DownloadList clearDownloadList(UserInfo user) {
		ValidateArgument.required(user, "UserInfo");
		return bulkDownloadDao.clearDownloadList(user.getId().toString());
	}

	@Override
	public void truncateAllDownloadDataForAllUsers(UserInfo admin) {
		ValidateArgument.required(admin, "UserInfo");
		if (!admin.isAdmin()) {
			throw new UnauthorizedException("Only an administrator may call this method");
		}
		this.bulkDownloadDao.truncateAllDownloadDataForAllUsers();
	}

}
