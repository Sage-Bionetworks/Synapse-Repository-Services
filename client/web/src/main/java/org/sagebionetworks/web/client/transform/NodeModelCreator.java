package org.sagebionetworks.web.client.transform;


import org.sagebionetworks.repo.model.Agreement;
import org.sagebionetworks.repo.model.Analysis;
import org.sagebionetworks.repo.model.Dataset;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.Eula;
import org.sagebionetworks.repo.model.Layer;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.Step;
import org.sagebionetworks.repo.model.registry.EntityTypeMetadata;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.web.shared.Annotations;
import org.sagebionetworks.web.shared.DownloadLocation;
import org.sagebionetworks.web.shared.EntityTypeResponse;
import org.sagebionetworks.web.shared.EntityWrapper;
import org.sagebionetworks.web.shared.LayerPreview;
import org.sagebionetworks.web.shared.PagedResults;
import org.sagebionetworks.web.shared.exceptions.ForbiddenException;
import org.sagebionetworks.web.shared.exceptions.RestServiceException;
import org.sagebionetworks.web.shared.exceptions.UnauthorizedException;

public interface NodeModelCreator {

	Entity createEntity(EntityWrapper entityWrapper) throws RestServiceException;
	
	// Specific Types:
	
	Dataset createDataset(String json) throws RestServiceException;
	
	Layer createLayer(String json) throws RestServiceException;
	
	Annotations createAnnotations(String json) throws RestServiceException;
	
	Project createProject(String json) throws RestServiceException;
	
	Eula createEULA(String json) throws RestServiceException;
	
	Agreement createAgreement(String json) throws RestServiceException;
	
	String createAgreementJSON(Agreement agreement) throws JSONObjectAdapterException;
	
	PagedResults createPagedResults(String json) throws RestServiceException;

	LayerPreview createLayerPreview(String json) throws RestServiceException;
	
	DownloadLocation createDownloadLocation(String json) throws RestServiceException;
	
	EntityTypeResponse createEntityTypeResponse(String json) throws RestServiceException;
	
	Analysis createAnalysis(String json) throws RestServiceException;

	Step createStep(String json) throws RestServiceException;

	/**
	 * Validates that the json parses and does not throw any RestService exceptions
	 * this is useful for json that doesn't have a model object (like schemas)
	 * @param json
	 * @throws UnauthorizedException
	 * @throws ForbiddenException
	 */
	void validate(String json) throws RestServiceException;

}
