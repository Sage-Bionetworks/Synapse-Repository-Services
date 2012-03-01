package org.sagebionetworks.workflow;

import org.apache.http.client.HttpClient;
import org.sagebionetworks.TemplatedConfiguration;
import org.sagebionetworks.client.Synapse;
import org.sagebionetworks.client.exceptions.SynapseException;

import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflow;
import com.amazonaws.services.sns.AmazonSNS;

/**
 * An extension of TemplatedConfiguration that includes configuration relevant
 * to all workflows
 * 
 * @author deflaux
 * 
 */
public interface WorkflowTemplatedConfiguration extends TemplatedConfiguration {

	/**
	 * @return the endpoint for the Simple Workflow Service
	 */
	public String getSwfEndpoint();

	/**
	 * Create a new synchronous Simple Workflow Framework (SWF) Client configured
	 * with the default socket timeout and the AWS IAM credentials of this
	 * workflow
	 * 
	 * @return the SWF client 
	 */
	public AmazonSimpleWorkflow createSWFClient();

	/**
	 * Create a new synchronous Simple Workflow Framework (SWF) Client and the AWS
	 * IAM credentials of this workflow
	 * 
	 * @param socketTimeoutSeconds
	 * 
	 * @return the SWF client 
	 */
	public AmazonSimpleWorkflow createSWFClient(int socketTimeoutSeconds);
	
	/**
	 * Get the shared synchronous Simple Workflow Framework (SWF) Client configured
	 * with the default socket timeout and the AWS IAM credentials of this
	 * workflow
	 * 
	 * @return the SWF client 
	 */
	public AmazonSimpleWorkflow getSWFClient();

	/**
	 * @return the endpoint for the Simple Notification Service
	 */
	public String getSnsEndpoint();

	/**
	 * Create a new synchronous Simple Notification Service (SNS) client configured
	 * with the AWS IAM credentials of this workflow
	 * 
	 * @return the SNS Client
	 */
	public AmazonSNS createSNSClient();
	
	/**
	 * Get the shared synchronous Simple Notification Service (SNS) client configured
	 * with the AWS IAM credentials of this workflow
	 * 
	 * @return the SNS Client
	 */
	public AmazonSNS getSNSClient();

	/**
	 * @return the Synapse username to log in as for this workflow
	 */
	public String getSynapseUsername();

	/**
	 * @return the Synapse password to log in with for this workflow
	 */
	public String getSynapsePassword();

	/**
	 * @return the Synapse secret key to log in with for this workflow
	 */
	public String getSynapseSecretKey();

	/**
	 * Create a new Synapse client configured with the Synapse credentials of
	 *         this workflow
	 *         
	 * @return the Synapse client configured with the Synapse credentials of
	 *         this workflow
	 * @throws SynapseException
	 */
	public Synapse createSynapseClient() throws SynapseException;

	/**
	 * Get the shared Synapse client configured with the Synapse credentials of
	 *         this workflow
	 *         
	 * @return the Synapse client configured with the Synapse credentials of
	 *         this workflow
	 * @throws SynapseException
	 */
	public Synapse getSynapseClient() throws SynapseException;
	
	/**
	 * Create a new HttpClient configured to use ThreadSafeClientConnManager with a
	 * modified default number of connections per host
	 * 
	 * @return the HttpClient
	 */
	public HttpClient createHttpClient();
	
	/**
	 * Get the shared HttpClient configured to use ThreadSafeClientConnManager with a
	 * modified default number of connections per host
	 * 
	 * @return the HttpClient
	 */
	public HttpClient getHttpClient();

	/**
	 * @return the path to the rscript executable
	 */
	public String getRScriptPath();
}