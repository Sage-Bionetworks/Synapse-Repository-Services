package org.sagebionetworks.samples;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.file.CompleteAllChunksRequest;
import org.sagebionetworks.repo.model.file.UploadDaemonStatus;
import org.sagebionetworks.repo.model.migration.IdList;
import org.sagebionetworks.repo.model.migration.RowMetadataResult;
import org.sagebionetworks.repo.model.wiki.WikiPage;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * This is an example controller that is used to test javadoc generation.
 * @author jmhill
 *
 */
@Controller
public class ExampleController {


	/**
	 * Create a wiki page with an entity owner.
	 * 
	 * @param userId - the user's id.
	 * @param ownerId - the ID of thw owner object.
	 * @param toCreate - the WikiPage to create.s
	 * @return - 
	 * @throws DatastoreException - Synapse error.
	 * @throws NotFoundException - returned if the user or owner does not exist.
	 */
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = "/entity/{ownerId}/wiki", method = RequestMethod.POST)
	public @ResponseBody
	WikiPage createEntityWikiPage(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@PathVariable String ownerId,
			@RequestBody WikiPage toCreate
			) throws DatastoreException, NotFoundException{
		return null;
	}
	
	/**
	 * Create a wiki page with a evaluation owner.
	 * 
	 * @param userId
	 * @param ownerId
	 * @param toCreate
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = "/evaluation/{ownerId}/wiki", method = RequestMethod.POST)
	public @ResponseBody
	WikiPage createCompetitionWikiPage(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@PathVariable String ownerId,
			@RequestBody WikiPage toCreate
			) throws DatastoreException, NotFoundException{
		return null;
	}
	
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value ="/startCompleteUploadDaemon" , method = RequestMethod.POST)
	public @ResponseBody UploadDaemonStatus startCompleteUploadDaemon(@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) String userId,
			@RequestBody CompleteAllChunksRequest cacf) throws DatastoreException, NotFoundException{
		return null;
	}
	
	/**
	 * This method is called on the destination stack to compare compare its
	 * metadata with the source stack metadata
	 * 
	 * @param userId
	 * @param type
	 * @param limit
	 * @param offset
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = "/migration/delata", method = RequestMethod.GET)
	public @ResponseBody
	RowMetadataResult getRowMetadataDelta(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = true) String userId,
			@RequestParam(required = true) String type,
			@RequestBody IdList request) throws DatastoreException,
			NotFoundException {
		if (request == null)
			throw new IllegalArgumentException("Request cannot be null");
		return null;
	}
	
}
