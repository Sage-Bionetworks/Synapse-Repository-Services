package org.sagebionetworks.web.client.transform;


import org.sagebionetworks.web.shared.Agreement;
import org.sagebionetworks.web.shared.Annotations;
import org.sagebionetworks.web.shared.Dataset;
import org.sagebionetworks.web.shared.DownloadLocation;
import org.sagebionetworks.web.shared.EULA;
import org.sagebionetworks.web.shared.EntityTypeResponse;
import org.sagebionetworks.web.shared.Layer;
import org.sagebionetworks.web.shared.LayerPreview;
import org.sagebionetworks.web.shared.PagedResults;
import org.sagebionetworks.web.shared.Project;
import org.sagebionetworks.web.shared.exceptions.ForbiddenException;
import org.sagebionetworks.web.shared.exceptions.RestServiceException;
import org.sagebionetworks.web.shared.exceptions.UnauthorizedException;

public interface NodeModelCreator {

	Dataset createDataset(String json) throws RestServiceException;
	
	Layer createLayer(String json) throws RestServiceException;
	
	Annotations createAnnotations(String json) throws RestServiceException;
	
	Project createProject(String json) throws RestServiceException;
	
	EULA createEULA(String json) throws RestServiceException;
	
	Agreement createAgreement(String json) throws RestServiceException;
	
	PagedResults createPagedResults(String json) throws RestServiceException;

	LayerPreview createLayerPreview(String json) throws RestServiceException;
	
	DownloadLocation createDownloadLocation(String json) throws RestServiceException;
	
	EntityTypeResponse createEntityTypeResponse(String json) throws RestServiceException;
	
	/**
	 * Validates that the json parses and does not throw any RestService exceptions
	 * this is useful for json that doesn't have a model object (like schemas)
	 * @param json
	 * @throws UnauthorizedException
	 * @throws ForbiddenException
	 */
	void validate(String json) throws RestServiceException;
}
