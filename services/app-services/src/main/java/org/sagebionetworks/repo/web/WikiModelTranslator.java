package org.sagebionetworks.repo.web;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.v2.wiki.V2WikiPage;
import org.sagebionetworks.repo.model.wiki.WikiPage;

/**
 * Abstraction for converting between the WikiPage models
 * @author hso
 *
 */
public interface WikiModelTranslator {
	/**
	 * Convert from a WikiPage to a V2WikiPage. Zips up the markdown string into a file and stores the file handle id.
	 * 
	 * @param from
	 * @param userInfo TODO
	 * @return
	 * @throws NotFoundException
	 * @throws DatastoreException
	 */
	public V2WikiPage convertToV2WikiPage(WikiPage from, UserInfo userInfo) throws IOException, DatastoreException, NotFoundException;
	
	/**
	 * Converts a V2WikiPage to a WikiPage. Gets the file and reads its
	 * contents into the string field for the WikiPage.
	 * @param from
	 * @return
	 * @throws NotFoundException
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public WikiPage convertToWikiPage(V2WikiPage from) throws NotFoundException, FileNotFoundException, IOException ;
}
