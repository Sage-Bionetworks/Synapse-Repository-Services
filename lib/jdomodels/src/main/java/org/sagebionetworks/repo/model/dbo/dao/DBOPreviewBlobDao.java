package org.sagebionetworks.repo.model.dbo.dao;

import java.io.IOException;
import java.io.InputStream;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.web.NotFoundException;

public interface DBOPreviewBlobDao {

	/**
	 * Create a new preview from the passed input stream for the owner and token.
	 * @param in
	 * @param owner
	 * @param token
	 * @throws IOException 
	 * @throws DatastoreException 
	 */
	public void createNewPreview(InputStream in, Long owner, Long token) throws IOException, DatastoreException;
	
	/**
	 * Get the bytes of a preview for a given owner and token
	 * @param owner
	 * @param token
	 * @return
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	public byte[] getPreview(Long owner, Long token) throws DatastoreException, NotFoundException;
}
