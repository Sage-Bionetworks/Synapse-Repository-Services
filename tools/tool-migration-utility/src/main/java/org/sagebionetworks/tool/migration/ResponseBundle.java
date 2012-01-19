package org.sagebionetworks.tool.migration;

import org.sagebionetworks.tool.migration.job.BuilderResponse;

/**
 * Bundle of responses.
 * @author John
 *
 */
public class ResponseBundle {

	BuilderResponse createResponse;
	BuilderResponse updateResponse;
	BuilderResponse deleteResponse;
	
	public ResponseBundle(BuilderResponse createResponse, BuilderResponse updateResponse, BuilderResponse deleteResponse) {
		super();
		this.createResponse = createResponse;
		this.updateResponse = updateResponse;
		this.deleteResponse = deleteResponse;
	}
	public BuilderResponse getCreateResponse() {
		return createResponse;
	}
	public BuilderResponse getUpdateResponse() {
		return updateResponse;
	}
	public BuilderResponse getDeleteResponse() {
		return deleteResponse;
	}
	
}
