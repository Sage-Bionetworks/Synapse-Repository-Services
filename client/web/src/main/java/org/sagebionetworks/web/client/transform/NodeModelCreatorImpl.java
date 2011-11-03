package org.sagebionetworks.web.client.transform;

import org.sagebionetworks.gwt.client.schema.adapter.JSONObjectGwt;
import org.sagebionetworks.repo.model.Agreement;
import org.sagebionetworks.repo.model.Analysis;
import org.sagebionetworks.repo.model.Dataset;
import org.sagebionetworks.repo.model.Eula;
import org.sagebionetworks.repo.model.Layer;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.Step;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.web.client.DisplayUtils;
import org.sagebionetworks.web.shared.Annotations;
import org.sagebionetworks.web.shared.DownloadLocation;
import org.sagebionetworks.web.shared.EntityTypeResponse;
import org.sagebionetworks.web.shared.LayerPreview;
import org.sagebionetworks.web.shared.PagedResults;
import org.sagebionetworks.web.shared.exceptions.RestServiceException;

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
	public Dataset createDataset(String json) throws RestServiceException {
		JSONObject obj = JSONParser.parseStrict(json).isObject();
		DisplayUtils.checkForErrors(obj);
		Dataset entity = new Dataset();
		JSONObjectAdapter adapter = new JSONObjectGwt(obj);
		try {
			entity.initializeFromJSONObject(adapter);
			return entity;
		} catch (JSONObjectAdapterException e) {
			throw new RestServiceException(e);
		}
	}

	@Override
	public Layer createLayer(String json) throws RestServiceException {
		JSONObject obj = JSONParser.parseStrict(json).isObject();
		DisplayUtils.checkForErrors(obj);
		Layer entity = new Layer();
		JSONObjectAdapter adapter = new JSONObjectGwt(obj);
		try {
			entity.initializeFromJSONObject(adapter);
			return entity;
		} catch (JSONObjectAdapterException e) {
			throw new RestServiceException(e);
		}
	}

	@Override
	public Annotations createAnnotations(String json) throws RestServiceException {
		JSONObject obj = JSONParser.parseStrict(json).isObject();
		DisplayUtils.checkForErrors(obj);
		return new Annotations(obj);
	}

	@Override
	public Project createProject(String json) throws RestServiceException {
		JSONObject obj = JSONParser.parseStrict(json).isObject();
		DisplayUtils.checkForErrors(obj);
		Project entity = new Project();
		JSONObjectAdapter adapter = new JSONObjectGwt(obj);
		try {
			entity.initializeFromJSONObject(adapter);
			return entity;
		} catch (JSONObjectAdapterException e) {
			throw new RestServiceException(e);
		}
	}
	
	@Override
	public Eula createEULA(String json) throws RestServiceException {
		JSONObject obj = JSONParser.parseStrict(json).isObject();
		DisplayUtils.checkForErrors(obj);
		Eula entity = new Eula();
		JSONObjectAdapter adapter = new JSONObjectGwt(obj);
		try {
			entity.initializeFromJSONObject(adapter);
			return entity;
		} catch (JSONObjectAdapterException e) {
			throw new RestServiceException(e);
		}
	}

	@Override
	public Agreement createAgreement(String json) throws RestServiceException {
		JSONObject obj = JSONParser.parseStrict(json).isObject();
		DisplayUtils.checkForErrors(obj);
		Agreement entity = new Agreement();
		JSONObjectAdapter adapter = new JSONObjectGwt(obj);
		try {
			entity.initializeFromJSONObject(adapter);
			return entity;
		} catch (JSONObjectAdapterException e) {
			throw new RestServiceException(e);
		}
	}
	
	@Override
	public String createAgreementJSON(Agreement agreement)	throws JSONObjectAdapterException {
		// Write it to an adapter
		JSONObjectAdapter adapter = agreement.writeToJSONObject(JSONObjectGwt.createNewAdapter());
		return adapter.toJSONString();
	}

	@Override
	public PagedResults createPagedResults(String json) throws RestServiceException {
		JSONObject obj = JSONParser.parseStrict(json).isObject();
		DisplayUtils.checkForErrors(obj);
		return new PagedResults(obj);
	}

	@Override
	public LayerPreview createLayerPreview(String json) throws RestServiceException {
		JSONObject obj = JSONParser.parseStrict(json).isObject();
		DisplayUtils.checkForErrors(obj);
		return new LayerPreview(obj);
	}

	@Override
	public DownloadLocation createDownloadLocation(String json) throws RestServiceException {
		JSONObject obj = JSONParser.parseStrict(json).isObject();
		DisplayUtils.checkForErrors(obj);
		return new DownloadLocation(obj);
	}

	@Override
	public EntityTypeResponse createEntityTypeResponse(String json) throws RestServiceException {
		JSONObject obj = JSONParser.parseStrict(json).isObject();
		DisplayUtils.checkForErrors(obj);
		return new EntityTypeResponse(obj);
	}

	@Override
	public void validate(String json) throws RestServiceException {
		if(!"".equals(json)) {
			JSONObject obj = JSONParser.parseStrict(json).isObject();
			DisplayUtils.checkForErrors(obj);
		}
	}

	@Override
	public Analysis createAnalysis(String json) throws RestServiceException {
		JSONObject obj = JSONParser.parseStrict(json).isObject();
		DisplayUtils.checkForErrors(obj);
		Analysis entity = new Analysis();
		JSONObjectAdapter adapter = new JSONObjectGwt(obj);
		try {
			entity.initializeFromJSONObject(adapter);
			return entity;
		} catch (JSONObjectAdapterException e) {
			throw new RestServiceException(e);
		}
	}

	@Override
	public Step createStep(String json) throws RestServiceException {
		JSONObject obj = JSONParser.parseStrict(json).isObject();
		DisplayUtils.checkForErrors(obj);
		Step entity = new Step();
		JSONObjectAdapter adapter = new JSONObjectGwt(obj);
		try {
			entity.initializeFromJSONObject(adapter);
			return entity;
		} catch (JSONObjectAdapterException e) {
			throw new RestServiceException(e);
		}
	}


}

