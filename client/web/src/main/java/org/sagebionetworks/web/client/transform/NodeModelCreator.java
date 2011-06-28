package org.sagebionetworks.web.client.transform;


import org.sagebionetworks.web.shared.Annotations;
import org.sagebionetworks.web.shared.Dataset;
import org.sagebionetworks.web.shared.DownloadLocation;
import org.sagebionetworks.web.shared.FileDownload;
import org.sagebionetworks.web.shared.Layer;
import org.sagebionetworks.web.shared.LayerPreview;
import org.sagebionetworks.web.shared.PagedResults;
import org.sagebionetworks.web.shared.Project;
import org.sagebionetworks.web.shared.exceptions.ForbiddenException;
import org.sagebionetworks.web.shared.exceptions.UnauthorizedException;

public interface NodeModelCreator {

	Dataset createDataset(String json) throws UnauthorizedException, ForbiddenException;
	
	Layer createLayer(String json) throws UnauthorizedException, ForbiddenException;
	
	Annotations createAnnotations(String json) throws UnauthorizedException, ForbiddenException;
	
	Project createProject(String json) throws UnauthorizedException, ForbiddenException;
	
	PagedResults createPagedResults(String json) throws UnauthorizedException, ForbiddenException;

	LayerPreview createLayerPreview(String json) throws UnauthorizedException, ForbiddenException;
	
	DownloadLocation createDownloadLocation(String json) throws UnauthorizedException, ForbiddenException;
	
	/**
	 * Validates that the json parses and does not throw any RestService exceptions
	 * this is useful for json that doesn't have a model object (like schemas)
	 * @param json
	 * @throws UnauthorizedException
	 * @throws ForbiddenException
	 */
	void validate(String json) throws UnauthorizedException, ForbiddenException;
}
