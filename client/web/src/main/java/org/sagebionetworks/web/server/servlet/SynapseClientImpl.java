package org.sagebionetworks.web.server.servlet;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
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
			JSONObjectAdapter entityJson = entity.writeToJSONObject(jsonObjectAdapter.createNew());
			entityWrapper.setEntityJson(entityJson.toJSONString());			
		} catch (SynapseException e) {
			entityWrapper.setRestServiceException(ExceptionUtil.convertSynapseException(e));
		} catch (JSONObjectAdapterException e) {
			entityWrapper.setRestServiceException(new UnknownErrorException(e.getMessage()));
		}		
		
		return entityWrapper;
	}
	
	@Override
	public String getEntityTypeRegistryJSON() {		
		ClassLoader classLoader = EntityType.class.getClassLoader();
		InputStream in = classLoader.getResourceAsStream(EntityType.REGISTER_JSON_FILE_NAME);
		if(in == null) throw new IllegalStateException("Cannot find the "+EntityType.REGISTER_JSON_FILE_NAME+" file on the classpath");
		String jsonString = "";
		try {
			jsonString = readToString(in);
		} catch (IOException e) {
			// error reading file
		}
		return jsonString;
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

	/**
	 * Read an input stream into a string.
	 * 
	 * @param in
	 * @return
	 * @throws IOException
	 */
	private static String readToString(InputStream in) throws IOException {
		try {
			BufferedInputStream bufferd = new BufferedInputStream(in);
			byte[] buffer = new byte[1024];
			StringBuilder builder = new StringBuilder();
			int index = -1;
			while ((index = bufferd.read(buffer, 0, buffer.length)) > 0) {
				builder.append(new String(buffer, 0, index, "UTF-8"));
			}
			return builder.toString();
		} finally {
			in.close();
		}
	}
	

}
