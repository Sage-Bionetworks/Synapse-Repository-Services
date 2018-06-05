package org.sagebionetworks;

/**
 * TemplatedConfiguration should serve as the base for any configuration we need
 * for any of our Java software. It encapsulates the core functionality of a
 * default properties file, a property override file, checking of that override
 * against a template, encrypted properties, and loading properties files from
 * S3. It also exposes some properties common to all software stacks.
 * 
 * Here's the first stab at a configuration system for our various stacks. It
 * solves several problems we are currently having:
 * 
 * (1) a proliferation of system properties for non-password and non-credential
 * configuration values
 * 
 * (2) a way to build one artifact that can run on many stacks
 * 
 * (3) a place to encapsulate and limit the scope of property names, components
 * that depend upon this retrieve values by method instead of by property name
 * 
 * (4) standardization of property names because since they are close together,
 * we can all see the naming pattern
 * 
 * @author deflaux
 * 
 */
public interface TemplatedConfiguration {

	/**
	 * @param propertyName
	 * @return the property value or a NullPointerException will be thrown
	 *         (throwing it preemptively here so as to provide a better error
	 *         message)
	 */
	public String getProperty(String propertyName);

	/**
	 * @param propertyName
	 * @return the decrypted property value or a NullPointerException will be
	 *         thrown (throwing it preemptively here so as to provide a better
	 *         error message)
	 */
	public String getDecryptedProperty(String propertyName);

	/**
	 * The name of the stack.
	 * 
	 * @return name of the stack
	 */
	public String getStack();

	/**
	 * The stack instance (i.e 'A', or 'B')
	 * 
	 * @return stack instance (i.e 'A', or 'B')
	 */
	public String getStackInstance();

	/**
	 * @return authentication service private endpoint
	 */
	public String getAuthenticationServicePrivateEndpoint();

	/**
	 * @return authentication service public endpoint
	 */
	public String getAuthenticationServicePublicEndpoint();

	/**
	 * @return repository service endpoint
	 */
	public String getRepositoryServiceEndpoint();
	
	/**
	 * Get the file service Endpoint.
	 * @return
	 */
	public String getFileServiceEndpoint();
	
	/**
	 * 
	 * @return search service endpoint
	 */
	public String getSearchServiceEndpoint();

	/**
	 * The repository Apache HttpClient connection pool properties
	 * 
	 * @return the max number of connections per route
	 */
	public int getHttpClientMaxConnsPerRoute();

	/**
	 * 
	 * @return docker service endpoint
	 */
	public String getDockerServiceEndpoint();
	
	/**
	 * 
	 * @return the endpoint for the docker registry event listener
	 */
	public String getDockerRegistryListenerEndpoint();
	

}