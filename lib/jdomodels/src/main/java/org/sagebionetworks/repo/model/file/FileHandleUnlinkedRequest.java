package org.sagebionetworks.repo.model.file;

import java.util.Objects;

import com.amazonaws.services.athena.model.QueryExecution;

/**
 * DTO that represents an SQS message with the Athena query execution whose results contain the
 * files to be set as unlinked
 */
public class FileHandleUnlinkedRequest {

	private String queryName;
	private String functionExecutionId;
	private QueryExecution queryExecution;
	private String pageToken;

	public String getQueryName() {
		return queryName;
	}

	public FileHandleUnlinkedRequest withQueryName(String queryName) {
		this.queryName = queryName;
		return this;
	}

	public String getFunctionExecutionId() {
		return functionExecutionId;
	}

	public FileHandleUnlinkedRequest withFunctionExecutionId(String functionExecutionId) {
		this.functionExecutionId = functionExecutionId;
		return this;
	}

	public QueryExecution getQueryExecution() {
		return queryExecution;
	}

	public FileHandleUnlinkedRequest withQueryExecution(QueryExecution queryExecution) {
		this.queryExecution = queryExecution;
		return this;
	}

	public String getPageToken() {
		return pageToken;
	}

	public FileHandleUnlinkedRequest withPageToken(String pageToken) {
		this.pageToken = pageToken;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(functionExecutionId, pageToken, queryExecution, queryName);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		FileHandleUnlinkedRequest other = (FileHandleUnlinkedRequest) obj;
		return Objects.equals(functionExecutionId, other.functionExecutionId) && Objects.equals(pageToken, other.pageToken)
				&& Objects.equals(queryExecution, other.queryExecution) && Objects.equals(queryName, other.queryName);
	}

	@Override
	public String toString() {
		return "FileHandleUnlinkedRequest [queryName=" + queryName + ", functionExecutionId=" + functionExecutionId + ", queryExecution="
				+ queryExecution + ", pageToken=" + pageToken + "]";
	}

}
