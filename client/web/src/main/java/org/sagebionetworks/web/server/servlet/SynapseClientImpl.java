package org.sagebionetworks.web.server.servlet;

import java.util.logging.Logger;

import org.sagebionetworks.client.Synapse;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.JSONObjectAdapterImpl;
import org.sagebionetworks.web.client.SynapseClient;
import org.sagebionetworks.web.shared.EntityWrapper;
import org.sagebionetworks.web.shared.exceptions.ExceptionUtil;
import org.sagebionetworks.web.shared.exceptions.UnknownErrorException;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import com.google.inject.Inject;

@SuppressWarnings("serial")
public class SynapseClientImpl extends RemoteServiceServlet implements SynapseClient, TokenProvider  {

	@SuppressWarnings("unused")	
	private static Logger logger = Logger.getLogger(SynapseClientImpl.class.getName());
	private TokenProvider tokenProvider = this;
	JSONObjectAdapter jsonObjectAdapter = new JSONObjectAdapterImpl();
				
	/**
	 * Injected with Gin
	 */
	@SuppressWarnings("unused")
	private ServiceUrlProvider urlProvider;
		
	/**
	 * Essentially the constructor. Setup synapse client.
	 * @param provider
	 */
	@Inject
	public void setServiceUrlProvider(ServiceUrlProvider provider){
		this.urlProvider = provider;
	}

	/**
	 * This allows integration tests to override the token provider.
	 * @param tokenProvider
	 */
	public void setTokenProvider(TokenProvider tokenProvider){
		this.tokenProvider = tokenProvider;
	}

	/**
	 * Validate that the service is ready to go. If any of the injected data is
	 * missing then it cannot run. Public for tests.
	 */
	public void validateService() {
		if (urlProvider == null)
			throw new IllegalStateException(
					"The org.sagebionetworks.rest.api.root.url was not set");
		if(tokenProvider == null){
			throw new IllegalStateException(
			"The token provider was not set");
		}
	}
	
	@Override
	public String getSessionToken() {
		// By default, we get the token from the request cookies.
		return UserDataProvider.getThreadLocalUserToken(this.getThreadLocalRequest());
	}

	
	/*
	 * SynapseClient Service Methods
	 */
	
	/**
	 * Get an Entity by its id
	 */
	@Override
	public EntityWrapper getEntity(String entityId) {
		Synapse synapseClient = createSynapseClient();		
		EntityWrapper entityWrapper = new EntityWrapper();
		
		try {
			Entity entity = synapseClient.getEntityById(entityId);
			EntityType entityType = EntityType.getFirstTypeInUrl(entity.getUri());
			JSONObjectAdapter entityJson = entity.writeToJSONObject(jsonObjectAdapter.createNew());
			entityWrapper.setEntityJson(entityJson.toJSONString());
			JSONObjectAdapter entityTypeJson = entityType.getMetadata().writeToJSONObject(jsonObjectAdapter.createNew());
			entityWrapper.setEntityMetadata(entityTypeJson.toJSONString());
		} catch (SynapseException e) {
			entityWrapper.setRestServiceException(ExceptionUtil.convertSynapseException(e));
		} catch (JSONObjectAdapterException e) {
			entityWrapper.setRestServiceException(new UnknownErrorException(e.getMessage()));
		}		
		
		return entityWrapper;
		
//		String entityTypeResponseJson = getNodeType(entityId);
//		if(entityTypeResponseJson != null) {
//			JSONObject etr;
//			try {
//				etr = new JSONObject(entityTypeResponseJson);
//				if(etr != null) {
//					String typeString = etr.getString("type").substring(1); // remove leading "/"
//					NodeType type = NodeType.valueOf(typeString.toUpperCase());
//					return getNodeJSON(type, entityId);
//				}
//			} catch (JSONException e) {
//				Log.warn(e.getMessage());
//				e.printStackTrace();
//			}
//		}
//		return null;
	}

	
	/*
	 * Private Methods
	 */

	/**
	 * The synapse client is stateful so we must create a new one for each request
	 */
	private Synapse createSynapseClient() {
		Synapse synapseClient = new Synapse();
		synapseClient.setSessionToken(tokenProvider.getSessionToken());
		synapseClient.setRepositoryEndpoint(urlProvider.getRepositoryServiceUrl());
		synapseClient.setAuthEndpoint(urlProvider.getPublicAuthBaseUrl());
		return synapseClient;
	}	

	
	
}
