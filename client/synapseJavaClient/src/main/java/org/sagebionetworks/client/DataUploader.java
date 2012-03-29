package org.sagebionetworks.client;

import java.io.File;

import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.repo.model.S3Token;
import org.sagebionetworks.repo.model.S3TokenBase;

/**
 * Interface for data upload functionality for synapse
 * 
 * @author deflaux
 */
public interface DataUploader {
	
	/**
	 * @param progressListener 
	 */
	public void setProgressListener(ProgressListener progressListener);
	
	/**
	 * @param s3Token
	 * @param dataFile
	 * @throws SynapseException
	 */
	public void uploadDataSingle(S3TokenBase s3Token, File dataFile) throws SynapseException;
	
	/**
	 * @param s3Token
	 * @param dataFile
	 * @throws SynapseException
	 */
	public void uploadDataMultiPart(S3Token s3Token, File dataFile) throws SynapseException;
}
