// Copyright [2011]-[2011] Amazon.com, Inc. or its affiliates. All Rights Reserved.
// Licensed under the Amazon Web Services Customer Agreement (the “License”).
// You may not use this file except in compliance with the License. A copy of the License is located at
//     http://aws.amazon.com/agreement/
// or in the “license” file accompanying this file.
// This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
// either express or implied. See the License for the specific language governing permissions and limitations under the License.
package org.sagebionetworks.workflow.curation;

import java.io.File;
import java.io.FileInputStream;
import java.net.URL;
import java.util.Properties;

import org.sagebionetworks.client.Synapse;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflow;
import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflowClient;

/**
 * Configuration Helper to used to create SWF and S3 clients
 */
public class ConfigHelper {

	private static final String PROPERTIES_FILENAME = "workflow.properties";

	private static final String SYNAPSE_SERVICE_URL_KEY = "synapse.url";
	private static final String SWF_SERVICE_URL_KEY = "service.url";
	private static final String SWF_ACCESS_ID_KEY = "AWS_ACCESS_KEY_ID";
	private static final String SWF_SECRET_KEY_KEY = "AWS_SECRET_KEY";

	private static final String S3_ACCESS_ID_KEY = "AWS.Access.ID";
	private static final String S3_SECRET_KEY_KEY = "AWS.Secret.Key";
	private static final String S3_BUCKET_NAME_KEY = "S3.Bucket.Name";

	private String synapseServiceUrl;

	private String swfServiceUrl;
	private String swfAccessId;
	private String swfSecretKey;

	private String s3AccessId;
	private String s3SecretKey;
	private String s3BucketName;

	private volatile static ConfigHelper theInstance = null;

	private ConfigHelper() throws Exception {

		URL url = ClassLoader.getSystemResource(PROPERTIES_FILENAME);
		if (null == url) {
			throw new Exception("unable to find in classpath "
					+ PROPERTIES_FILENAME);
		}
		Properties serviceProperties = new Properties();
		serviceProperties.load(new FileInputStream(new File(url.getFile())));

		this.synapseServiceUrl = serviceProperties
				.getProperty(SYNAPSE_SERVICE_URL_KEY);
		this.swfServiceUrl = serviceProperties.getProperty(SWF_SERVICE_URL_KEY);
		this.swfAccessId = System.getProperty(SWF_ACCESS_ID_KEY); // serviceProperties.getProperty(SWF_ACCESS_ID_KEY);
		this.swfSecretKey = System.getProperty(SWF_SECRET_KEY_KEY); // serviceProperties.getProperty(SWF_SECRET_KEY_KEY);

		this.s3AccessId = System.getProperty(SWF_ACCESS_ID_KEY); // serviceProperties.getProperty(S3_ACCESS_ID_KEY);
		this.s3SecretKey = System.getProperty(SWF_SECRET_KEY_KEY); // serviceProperties.getProperty(S3_SECRET_KEY_KEY);
		this.s3BucketName = serviceProperties.getProperty(S3_BUCKET_NAME_KEY);
	}

	public static ConfigHelper createConfig() throws Exception {
		if (null == theInstance) {
			synchronized (ConfigHelper.class) {
				if (null == theInstance) {
					theInstance = new ConfigHelper();
				}
			}
		}
		return theInstance;
	}

	public AmazonSimpleWorkflow createSWFClient() {
		ClientConfiguration config = new ClientConfiguration()
				.withSocketTimeout(70 * 1000);
		AWSCredentials awsCredentials = new BasicAWSCredentials(
				this.swfAccessId, this.swfSecretKey);
		AmazonSimpleWorkflow client = new AmazonSimpleWorkflowClient(
				awsCredentials, config);
		client.setEndpoint(this.swfServiceUrl);
		return client;
	}

	public AmazonS3 createS3Client() {
		AWSCredentials s3AWSCredentials = new BasicAWSCredentials(
				this.s3AccessId, this.s3SecretKey);
		AmazonS3 client = new AmazonS3Client(s3AWSCredentials);
		return client;
	}

	public Synapse createSynapseClient() {
		Synapse synapse = new Synapse(this.synapseServiceUrl);
		return synapse;
	}

	public String getS3BucketName() {
		return s3BucketName;
	}
}
