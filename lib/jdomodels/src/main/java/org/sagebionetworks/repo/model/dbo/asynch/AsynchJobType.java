package org.sagebionetworks.repo.model.dbo.asynch;

import org.sagebionetworks.StackConfigurationSingleton;
import org.sagebionetworks.repo.model.agent.AgentChatRequest;
import org.sagebionetworks.repo.model.agent.AgentChatResponse;
import org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus;
import org.sagebionetworks.repo.model.asynch.AsynchronousRequestBody;
import org.sagebionetworks.repo.model.asynch.AsynchronousResponseBody;
import org.sagebionetworks.repo.model.doi.v2.DoiRequest;
import org.sagebionetworks.repo.model.doi.v2.DoiResponse;
import org.sagebionetworks.repo.model.download.AddToDownloadListRequest;
import org.sagebionetworks.repo.model.download.AddToDownloadListResponse;
import org.sagebionetworks.repo.model.download.AddToDownloadListStatsRequest;
import org.sagebionetworks.repo.model.download.AddToDownloadListStatsResponse;
import org.sagebionetworks.repo.model.download.DownloadListManifestRequest;
import org.sagebionetworks.repo.model.download.DownloadListManifestResponse;
import org.sagebionetworks.repo.model.download.DownloadListPackageRequest;
import org.sagebionetworks.repo.model.download.DownloadListPackageResponse;
import org.sagebionetworks.repo.model.download.DownloadListQueryRequest;
import org.sagebionetworks.repo.model.download.DownloadListQueryResponse;
import org.sagebionetworks.repo.model.file.AddFileToDownloadListRequest;
import org.sagebionetworks.repo.model.file.AddFileToDownloadListResponse;
import org.sagebionetworks.repo.model.file.BulkFileDownloadRequest;
import org.sagebionetworks.repo.model.file.BulkFileDownloadResponse;
import org.sagebionetworks.repo.model.file.FileHandleArchivalRequest;
import org.sagebionetworks.repo.model.file.FileHandleArchivalResponse;
import org.sagebionetworks.repo.model.file.FileHandleRestoreRequest;
import org.sagebionetworks.repo.model.file.FileHandleRestoreResponse;
import org.sagebionetworks.repo.model.grid.CreateGridRequest;
import org.sagebionetworks.repo.model.grid.CreateGridResponse;
import org.sagebionetworks.repo.model.grid.DownloadFromGridRequest;
import org.sagebionetworks.repo.model.grid.DownloadFromGridResult;
import org.sagebionetworks.repo.model.grid.GridCsvImportRequest;
import org.sagebionetworks.repo.model.grid.GridCsvImportResponse;
import org.sagebionetworks.repo.model.grid.GridRecordSetExportRequest;
import org.sagebionetworks.repo.model.grid.GridRecordSetExportResponse;
import org.sagebionetworks.repo.model.grid.SynchronizeGridRequest;
import org.sagebionetworks.repo.model.grid.SynchronizeGridResponse;
import org.sagebionetworks.repo.model.migration.AsyncMigrationRequest;
import org.sagebionetworks.repo.model.migration.AsyncMigrationResponse;
import org.sagebionetworks.repo.model.report.DownloadStorageReportRequest;
import org.sagebionetworks.repo.model.report.DownloadStorageReportResponse;
import org.sagebionetworks.repo.model.schema.CreateSchemaRequest;
import org.sagebionetworks.repo.model.schema.CreateSchemaResponse;
import org.sagebionetworks.repo.model.schema.GetValidationSchemaRequest;
import org.sagebionetworks.repo.model.schema.GetValidationSchemaResponse;
import org.sagebionetworks.repo.model.table.DownloadFromTableRequest;
import org.sagebionetworks.repo.model.table.DownloadFromTableResult;
import org.sagebionetworks.repo.model.table.DownloadPFBRequest;
import org.sagebionetworks.repo.model.table.DownloadPFBResult;
import org.sagebionetworks.repo.model.table.QueryBundleRequest;
import org.sagebionetworks.repo.model.table.QueryNextPageToken;
import org.sagebionetworks.repo.model.table.QueryResult;
import org.sagebionetworks.repo.model.table.QueryResultBundle;
import org.sagebionetworks.repo.model.table.TableUpdateTransactionRequest;
import org.sagebionetworks.repo.model.table.TableUpdateTransactionResponse;
import org.sagebionetworks.repo.model.table.UploadToTablePreviewRequest;
import org.sagebionetworks.repo.model.table.UploadToTablePreviewResult;
import org.sagebionetworks.repo.model.table.ViewColumnModelRequest;
import org.sagebionetworks.repo.model.table.ViewColumnModelResponse;

/**
 * This enum maps types to classes.
 *
 */
public enum AsynchJobType {

	TABLE_UPDATE_TRANSACTION(TableUpdateTransactionRequest.class, TableUpdateTransactionResponse.class),

	UPLOAD_CSV_TO_TABLE_PREVIEW(UploadToTablePreviewRequest.class, UploadToTablePreviewResult.class),

	DOWNLOAD_CSV_FROM_TABLE(DownloadFromTableRequest.class, DownloadFromTableResult.class),

	QUERY(QueryBundleRequest.class, QueryResultBundle.class),

	QUERY_NEXT_PAGE(QueryNextPageToken.class, QueryResult.class),

	BULK_FILE_DOWNLOAD(BulkFileDownloadRequest.class, BulkFileDownloadResponse.class),

	MIGRATION(AsyncMigrationRequest.class, AsyncMigrationResponse.class),

	DOI(DoiRequest.class, DoiResponse.class),

	ADD_FILES_TO_DOWNLOAD_LIST(AddFileToDownloadListRequest.class, AddFileToDownloadListResponse.class),

	STORAGE_REPORT(DownloadStorageReportRequest.class, DownloadStorageReportResponse.class),

	JSON_SCHEMA_CREATE(CreateSchemaRequest.class, CreateSchemaResponse.class),

	VIEW_COLUMN_MODEL_REQUEST(ViewColumnModelRequest.class, ViewColumnModelResponse.class),

	GET_VALIDATION_SCHEMA(GetValidationSchemaRequest.class, GetValidationSchemaResponse.class),

	QUERY_DOWNLOAD_LIST(DownloadListQueryRequest.class, DownloadListQueryResponse.class),

	ADD_TO_DOWNLOAD_LIST(AddToDownloadListRequest.class, AddToDownloadListResponse.class),

	ADD_TO_DOWNLOAD_LIST_STATS(AddToDownloadListStatsRequest.class, AddToDownloadListStatsResponse.class),

	DOWNLOAD_LIST_PACKAGE(DownloadListPackageRequest.class, DownloadListPackageResponse.class),

	DOWNLOAD_LIST_MANIFEST(DownloadListManifestRequest.class, DownloadListManifestResponse.class),

	FILE_HANDLE_ARCHIVAL_REQUEST(FileHandleArchivalRequest.class, FileHandleArchivalResponse.class),

	FILE_HANDLE_RESTORE_REQUEST(FileHandleRestoreRequest.class, FileHandleRestoreResponse.class),

	AGENT_CHAT(AgentChatRequest.class, AgentChatResponse.class),

	QUERY_AS_PFB(DownloadPFBRequest.class, DownloadPFBResult.class),

	GRID_CREATE(CreateGridRequest.class, CreateGridResponse.class),

	DOWNLOAD_CSV_FROM_GRID(DownloadFromGridRequest.class, DownloadFromGridResult.class),

	GRID_EXPORT_RECORDSET(GridRecordSetExportRequest.class, GridRecordSetExportResponse.class),

	GRID_IMPORT_CSV(GridCsvImportRequest.class, GridCsvImportResponse.class),

	GRID_SYNCHRONIZATION(SynchronizeGridRequest.class, SynchronizeGridResponse.class,
			(s) -> new FifoQueueParameters()
					.setMessageGroupId(((SynchronizeGridRequest) s.getRequestBody()).getGridSessionId())
					.setMessageDeduplicationId(s.getJobId()));

	private final Class<? extends AsynchronousRequestBody> requestClass;
	private final Class<? extends AsynchronousResponseBody> responseClass;
	private final FifoQueueParameterProvider fifoParameterProvider;

	AsynchJobType(Class<? extends AsynchronousRequestBody> requestClass,
			Class<? extends AsynchronousResponseBody> responseClass) {
		this.requestClass = requestClass;
		this.responseClass = responseClass;
		this.fifoParameterProvider = null;
	}

	AsynchJobType(Class<? extends AsynchronousRequestBody> requestClass,
			Class<? extends AsynchronousResponseBody> responseClass, FifoQueueParameterProvider fifoParameterProvider) {
		this.requestClass = requestClass;
		this.responseClass = responseClass;
		this.fifoParameterProvider = fifoParameterProvider;
	}

	/**
	 * Lookup the Type for a given class
	 * 
	 * @param clazz
	 * @return
	 */
	public static AsynchJobType findTypeFromRequestClass(Class<? extends AsynchronousRequestBody> clazz) {
		for (AsynchJobType type : AsynchJobType.values()) {
			if (type.requestClass.equals(clazz)) {
				return type;
			}
		}
		throw new IllegalArgumentException("Unknown type for class:" + clazz);
	}

	/**
	 * The class bound to this type.
	 * 
	 * @return
	 */
	public Class<? extends AsynchronousRequestBody> getRequestClass() {
		return this.requestClass;
	}

	public Class<? extends AsynchronousResponseBody> getResponseClass() {
		return this.responseClass;
	}

	public boolean isFifoQueue() {
		return fifoParameterProvider != null;
	}

	public FifoQueueParameters getFifoParameters(AsynchronousJobStatus status) {
		if (fifoParameterProvider == null) {
			throw new UnsupportedOperationException("This job type does not use FIFO queues");
		}
		return fifoParameterProvider.getParameters(status);
	}

	/**
	 * The suffix of the queue name where jobs of this type are published.
	 * 
	 * @return
	 */
	public String getQueueName() {
		StringBuilder nameBuilder = new StringBuilder(this.name());
		if (isFifoQueue()) {
			nameBuilder.append(".fifo");
		}
		return StackConfigurationSingleton.singleton().getQueueName(nameBuilder.toString());
	}
}
