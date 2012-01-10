package org.sagebionetworks.web.client.transform;

import org.sagebionetworks.gwt.client.schema.adapter.JSONObjectGwt;
import org.sagebionetworks.repo.model.Agreement;
import org.sagebionetworks.repo.model.Analysis;
import org.sagebionetworks.repo.model.Dataset;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.EntityPath;
import org.sagebionetworks.repo.model.Eula;
import org.sagebionetworks.repo.model.Layer;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.Step;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.web.client.DisplayUtils;
import org.sagebionetworks.web.client.EntityTypeProvider;
import org.sagebionetworks.web.shared.Annotations;
import org.sagebionetworks.web.shared.DownloadLocation;
import org.sagebionetworks.web.shared.EntityType;
import org.sagebionetworks.web.shared.EntityTypeResponse;
import org.sagebionetworks.web.shared.EntityWrapper;
import org.sagebionetworks.web.shared.LayerPreview;
import org.sagebionetworks.web.shared.PagedResults;
import org.sagebionetworks.web.shared.exceptions.RestServiceException;

import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.inject.Inject;

/**
 * This class exists to isolate JSONObject creation from any classes that need JVM based tests
 * This class doesn't need to be tested as the business logic is located elsewhere.
 * (JSONObect creation and JSONParser should not be used in classes that need testing)
 * @author dburdick
 *
 */
public class NodeModelCreatorImpl implements NodeModelCreator {		
	
	private JSONObjectAdapter jsonObjectAdapter; 
	private EntityTypeProvider entityTypeProvider;	
	
	@Inject
	public NodeModelCreatorImpl(JSONObjectAdapter jsonObjectAdapter, EntityTypeProvider entityTypeProvider) {
		this.jsonObjectAdapter = jsonObjectAdapter;
		this.entityTypeProvider = entityTypeProvider;
	}
	
	@Override
	public Entity createEntity(EntityWrapper entityWrapper) throws RestServiceException {
		Entity entity = null;
		if(entityWrapper.getRestServiceException() != null) {
			throw entityWrapper.getRestServiceException();
		}
		// TODO : change this to use a GWT.create( full class package name ) generator 
		String json = entityWrapper.getEntityJson();
		if(json != null) {			
			try {
				// What I want to do:
				JSONObjectAdapter obj = jsonObjectAdapter.createNew(json);			
				String typeString = obj.getString("uri");
				EntityType entityType = entityTypeProvider.getEntityTypeForUri(typeString); 
				if (typeString != null) {
					if("/dataset".equals(entityType.getUrlPrefix())) {
						entity = new Dataset(obj);
					} else if("/layer".equals(entityType.getUrlPrefix())) {
						entity = new Layer(obj);
					} else if("/project".equals(entityType.getUrlPrefix())) {
						entity = new Project(obj);
					} else if("/eula".equals(entityType.getUrlPrefix())) {
						entity = new Eula(obj);
					} else if("/agreement".equals(entityType.getUrlPrefix())) {
						entity = new Agreement(obj);
					} else if("/analysis".equals(entityType.getUrlPrefix())) {
						entity = new Analysis(obj);
					} else if("/step".equals(entityType.getUrlPrefix())) {
						entity = new Step(obj);
					} 
				}			
			} catch (JSONObjectAdapterException e) {
				throw new RestServiceException(e.getMessage());
			}
		}
		return entity;
	}


	@Override
	public EntityPath createEntityPath(EntityWrapper entityWrapper)
			throws RestServiceException {
		EntityPath entityPath = null;
		if(entityWrapper.getRestServiceException() != null) {
			throw entityWrapper.getRestServiceException();
		}
 
		String json = entityWrapper.getEntityJson();
		if(json != null) {			
			try {
				JSONObjectAdapter obj = jsonObjectAdapter.createNew(json);			
				entityPath = new EntityPath(obj);
			} catch (JSONObjectAdapterException e) {
				throw new RestServiceException(e.getMessage());
			}
		}
		return entityPath;
	}

	
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
			throw new RestServiceException(e.getMessage());
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
			throw new RestServiceException(e.getMessage());
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
			throw new RestServiceException(e.getMessage());
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
			throw new RestServiceException(e.getMessage());
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
			throw new RestServiceException(e.getMessage());
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
			throw new RestServiceException(e.getMessage());
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
			throw new RestServiceException(e.getMessage());
		}
	}

}

