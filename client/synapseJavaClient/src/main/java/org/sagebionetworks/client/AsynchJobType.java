package org.sagebionetworks.client;


import static org.sagebionetworks.client.SynapseClientImpl.ASYNC_GET;
import static org.sagebionetworks.client.SynapseClientImpl.ASYNC_START;
import static org.sagebionetworks.client.SynapseClientImpl.DOI;
import static org.sagebionetworks.client.SynapseClientImpl.DOWNLOAD_LIST_QUERY;
import static org.sagebionetworks.client.SynapseClientImpl.DOWNLOAD_LIST_ADD;
import static org.sagebionetworks.client.SynapseClientImpl.FILE_BULK;
import static org.sagebionetworks.client.SynapseClientImpl.SCHEMA_TYPE_CREATE;
import static org.sagebionetworks.client.SynapseClientImpl.SCHEMA_TYPE_VALIDATION;
import static org.sagebionetworks.client.SynapseClientImpl.STORAGE_REPORT;
import static org.sagebionetworks.client.SynapseClientImpl.TABLE_APPEND;
import static org.sagebionetworks.client.SynapseClientImpl.TABLE_DOWNLOAD_CSV;
import static org.sagebionetworks.client.SynapseClientImpl.TABLE_QUERY;
import static org.sagebionetworks.client.SynapseClientImpl.TABLE_QUERY_NEXTPAGE;
import static org.sagebionetworks.client.SynapseClientImpl.TABLE_TRANSACTION;
import static org.sagebionetworks.client.SynapseClientImpl.TABLE_UPLOAD_CSV;
import static org.sagebionetworks.client.SynapseClientImpl.TABLE_UPLOAD_CSV_PREVIEW;
import static org.sagebionetworks.client.SynapseClientImpl.VIEW_COLUMNS;

import org.sagebionetworks.repo.model.asynch.AsynchronousRequestBody;
import org.sagebionetworks.repo.model.asynch.AsynchronousResponseBody;
import org.sagebionetworks.repo.model.doi.v2.DoiResponse;
import org.sagebionetworks.repo.model.download.DownloadListQueryResponse;
import org.sagebionetworks.repo.model.file.AddFileToDownloadListResponse;
import org.sagebionetworks.repo.model.file.BulkFileDownloadResponse;
import org.sagebionetworks.repo.model.report.DownloadStorageReportResponse;
import org.sagebionetworks.repo.model.schema.CreateSchemaResponse;
import org.sagebionetworks.repo.model.schema.GetValidationSchemaResponse;
import org.sagebionetworks.repo.model.table.DownloadFromTableResult;
import org.sagebionetworks.repo.model.table.HasEntityId;
import org.sagebionetworks.repo.model.table.QueryResult;
import org.sagebionetworks.repo.model.table.QueryResultBundle;
import org.sagebionetworks.repo.model.table.RowReferenceSetResults;
import org.sagebionetworks.repo.model.table.TableUpdateTransactionResponse;
import org.sagebionetworks.repo.model.table.UploadToTablePreviewResult;
import org.sagebionetworks.repo.model.table.UploadToTableRequest;
import org.sagebionetworks.repo.model.table.UploadToTableResult;
import org.sagebionetworks.repo.model.table.ViewColumnModelResponse;
/**
 * Maps job types to the URL prefix needed for each type.
 * 
 * @author John
 *
 */
public enum AsynchJobType {
	
	TableAppendRowSet(TABLE_APPEND, RowReferenceSetResults.class, RestEndpointType.repo),
	TableQuery(TABLE_QUERY, QueryResultBundle.class, RestEndpointType.repo),
	TableQueryNextPage(TABLE_QUERY_NEXTPAGE, QueryResult.class, RestEndpointType.repo),
	TableCSVUpload(TABLE_UPLOAD_CSV, UploadToTableResult.class, RestEndpointType.repo),
	TableCSVUploadPreview(TABLE_UPLOAD_CSV_PREVIEW, UploadToTablePreviewResult.class, RestEndpointType.repo),
	TableCSVDownload(TABLE_DOWNLOAD_CSV, DownloadFromTableResult.class, RestEndpointType.repo), 
	BulkFileDownload(FILE_BULK, BulkFileDownloadResponse.class, RestEndpointType.file),
	TableTransaction(TABLE_TRANSACTION, TableUpdateTransactionResponse.class, RestEndpointType.repo),
	Doi(DOI, DoiResponse .class, RestEndpointType.repo),
	AddFileToDownloadList(DOWNLOAD_LIST_ADD, AddFileToDownloadListResponse.class,  RestEndpointType.file),
	DownloadStorageReport(STORAGE_REPORT, DownloadStorageReportResponse.class, RestEndpointType.repo),
	CreateJsonSchema(SCHEMA_TYPE_CREATE, CreateSchemaResponse.class, RestEndpointType.repo),
	GetValidationSchema(SCHEMA_TYPE_VALIDATION, GetValidationSchemaResponse.class, RestEndpointType.repo),
	ViewColumnModelRequest(VIEW_COLUMNS, ViewColumnModelResponse.class, RestEndpointType.repo),
	QueryDownloadList(DOWNLOAD_LIST_QUERY, DownloadListQueryResponse.class, RestEndpointType.repo);

	String prefix;
	Class<? extends AsynchronousResponseBody> responseClass;
	RestEndpointType restEndpointType;
	
	AsynchJobType(String prefix, Class<? extends AsynchronousResponseBody> responseClass, RestEndpointType endpoint){
		this.prefix = prefix;
		this.responseClass = responseClass;
		this.restEndpointType = endpoint;
	}
	
	/**
	 * Get the URL used to start this job type.
	 * @param request
	 */
	public  String getStartUrl(AsynchronousRequestBody request){
		String entityId = getEntityIdFromRequest(request);
		if (entityId != null) {
			return "/entity/" + entityId + prefix + ASYNC_START;
		} else {
			return prefix + ASYNC_START;
		}
	}

	/*
	 * extracts the entityId from the request body 
	 * throws an exception if the request has an entityId field but the entityId is null
	 * If the request body does not have an entityId field, returns null.
	 */
	private String getEntityIdFromRequest(AsynchronousRequestBody request) {
		if (request instanceof UploadToTableRequest && ((UploadToTableRequest) request).getTableId() != null) {
			return ((UploadToTableRequest) request).getTableId();
		} else if (request instanceof HasEntityId && ((HasEntityId) request).getEntityId() != null) {
			return ((HasEntityId) request).getEntityId();
		} else if ((request instanceof UploadToTableRequest && ((UploadToTableRequest) request).getTableId() == null) ||
					(request instanceof HasEntityId && ((HasEntityId) request).getEntityId() == null)) {
			throw new IllegalArgumentException("entityId cannot be null");
		} else {
			return null;
		}
	}

	/**
	 * Get the URL used to get the results for this job type.
	 * @param token
	 * @param request
	 * @return
	 */
	public String getResultUrl(String token, AsynchronousRequestBody request){
		return getResultUrl(token, getEntityIdFromRequest(request));
	}

	/**
	 * Get the URL used to get the results for this job type.
	 * 
	 * @param token
	 * @param entityId
	 * @return
	 */
	public String getResultUrl(String token, String entityId){
		if (entityId != null) {
			return "/entity/" + entityId + prefix + ASYNC_GET + token;
		}
		return prefix+ASYNC_GET + token;
	}

	/**
	 * Get the response class.
	 * @return
	 */
	public Class<? extends AsynchronousResponseBody> getReponseClass(){
		return responseClass;
	}
	
	/**
	 * The endpoint for this type.
	 * @return
	 */
	public RestEndpointType getRestEndpoint(){
		return this.restEndpointType;
	}
}
