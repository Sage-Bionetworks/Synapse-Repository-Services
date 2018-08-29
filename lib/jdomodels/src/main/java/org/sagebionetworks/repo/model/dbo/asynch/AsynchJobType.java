package org.sagebionetworks.repo.model.dbo.asynch;

import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.StackConfigurationSingleton;
import org.sagebionetworks.repo.model.asynch.AsynchronousRequestBody;
import org.sagebionetworks.repo.model.asynch.AsynchronousResponseBody;
import org.sagebionetworks.repo.model.file.BulkFileDownloadRequest;
import org.sagebionetworks.repo.model.file.BulkFileDownloadResponse;
import org.sagebionetworks.repo.model.migration.AsyncMigrationRequest;
import org.sagebionetworks.repo.model.migration.AsyncMigrationResponse;
import org.sagebionetworks.repo.model.table.AppendableRowSetRequest;
import org.sagebionetworks.repo.model.table.DownloadFromTableRequest;
import org.sagebionetworks.repo.model.table.DownloadFromTableResult;
import org.sagebionetworks.repo.model.table.QueryBundleRequest;
import org.sagebionetworks.repo.model.table.QueryNextPageToken;
import org.sagebionetworks.repo.model.table.QueryResult;
import org.sagebionetworks.repo.model.table.QueryResultBundle;
import org.sagebionetworks.repo.model.table.RowReferenceSetResults;
import org.sagebionetworks.repo.model.table.TableUpdateTransactionRequest;
import org.sagebionetworks.repo.model.table.TableUpdateTransactionResponse;
import org.sagebionetworks.repo.model.table.UploadToTablePreviewRequest;
import org.sagebionetworks.repo.model.table.UploadToTablePreviewResult;
import org.sagebionetworks.repo.model.table.UploadToTableRequest;
import org.sagebionetworks.repo.model.table.UploadToTableResult;

/**
 * This enum maps types to classes.
 * 
 * @author jmhill
 *
 */
public enum AsynchJobType  {
	
	TABLE_UPDATE_TRANSACTION(TableUpdateTransactionRequest.class, TableUpdateTransactionResponse.class),
	
	APPEND_ROW_SET_TO_TABLE(AppendableRowSetRequest.class, RowReferenceSetResults.class),

	UPLOAD_CSV_TO_TABLE(UploadToTableRequest.class, UploadToTableResult.class),
	
	UPLOAD_CSV_TO_TABLE_PREVIEW(UploadToTablePreviewRequest.class, UploadToTablePreviewResult.class),

	DOWNLOAD_CSV_FROM_TABLE(DownloadFromTableRequest.class, DownloadFromTableResult.class),

	QUERY(QueryBundleRequest.class, QueryResultBundle.class),

	QUERY_NEXT_PAGE(QueryNextPageToken.class, QueryResult.class),
	
	BULK_FILE_DOWNLOAD(BulkFileDownloadRequest.class, BulkFileDownloadResponse.class),
	
	MIGRATION(AsyncMigrationRequest.class, AsyncMigrationResponse.class);

	private Class<? extends AsynchronousRequestBody> requestClass;
	private Class<? extends AsynchronousResponseBody> responseClass;
	
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
	 * The suffix of the queue name where jobs of this type are published. 
	 * @return
	 */
	public String getQueueName(){
		return StackConfigurationSingleton.singleton().getAsyncQueueName(this.name());
	}
}
