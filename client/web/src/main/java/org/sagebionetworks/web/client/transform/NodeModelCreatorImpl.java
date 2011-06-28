package org.sagebionetworks.web.client.transform;

import org.sagebionetworks.web.client.DisplayUtils;
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

import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;

/**
 * This class exists to isolate JSONObject creation from any classes that need JVM based tests
 * This class doesn't need to be tested as the business logic is located elsewhere.
 * (JSONObect creation and JSONParser should not be used in classes that need testing)
 * @author dburdick
 *
 */
public class NodeModelCreatorImpl implements NodeModelCreator {

	@Override
	public Dataset createDataset(String json) throws UnauthorizedException, ForbiddenException {
		JSONObject obj = JSONParser.parseStrict(json).isObject();
		DisplayUtils.checkForErrors(obj);
		return new Dataset(obj);
	}

	@Override
	public Layer createLayer(String json) throws UnauthorizedException, ForbiddenException {
		JSONObject obj = JSONParser.parseStrict(json).isObject();
		DisplayUtils.checkForErrors(obj);
		return new Layer(obj);
	}

	@Override
	public Annotations createAnnotations(String json) throws UnauthorizedException, ForbiddenException {
		JSONObject obj = JSONParser.parseStrict(json).isObject();
		DisplayUtils.checkForErrors(obj);
		return new Annotations(obj);
	}

	@Override
	public Project createProject(String json) throws UnauthorizedException, ForbiddenException {
		JSONObject obj = JSONParser.parseStrict(json).isObject();
		DisplayUtils.checkForErrors(obj);
		return new Project(obj);
	}

	@Override
	public PagedResults createPagedResults(String json) throws UnauthorizedException, ForbiddenException {
		JSONObject obj = JSONParser.parseStrict(json).isObject();
		DisplayUtils.checkForErrors(obj);
		return new PagedResults(obj);
	}

	@Override
	public LayerPreview createLayerPreview(String json) throws UnauthorizedException, ForbiddenException {
		JSONObject obj = JSONParser.parseStrict(json).isObject();
		DisplayUtils.checkForErrors(obj);
		return new LayerPreview(obj);
	}

	@Override
	public DownloadLocation createDownloadLocation(String json) throws UnauthorizedException, ForbiddenException {
		JSONObject obj = JSONParser.parseStrict(json).isObject();
		DisplayUtils.checkForErrors(obj);
		return new DownloadLocation(obj);
	}

	@Override
	public void validate(String json) throws UnauthorizedException, ForbiddenException {
		JSONObject obj = JSONParser.parseStrict(json).isObject();
		DisplayUtils.checkForErrors(obj);		
	}

}

