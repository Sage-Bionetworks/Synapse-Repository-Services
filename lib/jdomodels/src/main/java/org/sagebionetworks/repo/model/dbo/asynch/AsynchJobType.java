package org.sagebionetworks.repo.model.dbo.asynch;

import org.sagebionetworks.StackConfigurationSingleton;
import org.sagebionetworks.repo.model.UnmodifiableXStream;
import org.sagebionetworks.repo.model.asynch.AsynchronousRequestBody;
import org.sagebionetworks.repo.model.asynch.AsynchronousResponseBody;
import org.sagebionetworks.repo.model.doi.v2.DoiRequest;
import org.sagebionetworks.repo.model.doi.v2.DoiResponse;
import org.sagebionetworks.repo.model.download.DownloadListQueryRequest;
import org.sagebionetworks.repo.model.download.DownloadListQueryResponse;
import org.sagebionetworks.repo.model.file.AddFileToDownloadListRequest;
import org.sagebionetworks.repo.model.file.AddFileToDownloadListResponse;
import org.sagebionetworks.repo.model.file.BulkFileDownloadRequest;
import org.sagebionetworks.repo.model.file.BulkFileDownloadResponse;
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
 * @author jmhill
 *
 */
public enum AsynchJobType  {
	
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
	
	QUERY_DOWNLOAD_LIST(DownloadListQueryRequest.class, DownloadListQueryResponse.class);

	private Class<? extends AsynchronousRequestBody> requestClass;
	private Class<? extends AsynchronousResponseBody> responseClass;

	//since both request and response alias to the same name, we must create separate XStream instances for request and response.
	private static final UnmodifiableXStream REQUEST_X_STREAM;
	private static final UnmodifiableXStream RESPONSE_X_STREAM;

	static {
		UnmodifiableXStream.Builder requestXStreamBuilder = UnmodifiableXStream.builder();
		requestXStreamBuilder.allowTypeHierarchy(AsynchronousRequestBody.class);
		UnmodifiableXStream.Builder responseXStreamBuilder = UnmodifiableXStream.builder();
		responseXStreamBuilder.allowTypeHierarchy(AsynchronousResponseBody.class);

		for (AsynchJobType type : values()) {
			requestXStreamBuilder.alias(type.name(), type.requestClass);
			responseXStreamBuilder.alias(type.name(), type.responseClass);
		}

		REQUEST_X_STREAM = requestXStreamBuilder.build();
		RESPONSE_X_STREAM = responseXStreamBuilder.build();
	}

	
	AsynchJobType(Class<? extends AsynchronousRequestBody> requestClass, Class<? extends AsynchronousResponseBody> responseClass) {
		this.requestClass = requestClass;
		this.responseClass = responseClass;
	}
	
	/**
	 * Lookup the Type for a given class
	 * @param clazz
	 * @return
	 */
	public static AsynchJobType findTypeFromRequestClass(Class<? extends AsynchronousRequestBody> clazz){
		for(AsynchJobType type: AsynchJobType.values()){
			if(type.requestClass.equals(clazz)){
				return type;
			}
		}
		throw new IllegalArgumentException("Unknown type for class:"+clazz);
	}
	
	/**
	 * The class bound to this type.
	 * @return
	 */
	public Class<? extends AsynchronousRequestBody> getRequestClass(){
		return this.requestClass;
	}
	
	public Class<? extends AsynchronousResponseBody> getResponseClass(){
		return this.responseClass;
	}

	/**
	 *
	 * @return XStream used for serializing/deserializing AsynchronousRequestBody
	 */
	public static UnmodifiableXStream getRequestXStream(){
		return REQUEST_X_STREAM;
	}

	/**
	 *
	 * @return XStream used for serializing/deserializing AsynchronousResponseBody
	 */
	public static UnmodifiableXStream getResponseXStream(){
		return RESPONSE_X_STREAM;
	}

	/**
	 * The suffix of the queue name where jobs of this type are published. 
	 * @return
	 */
	public String getQueueName(){
		return StackConfigurationSingleton.singleton().getQueueName(this.name());
	}
}
