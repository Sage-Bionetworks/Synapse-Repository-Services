package org.sagebionetworks.samples;
import java.util.List;

import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.file.CompleteAllChunksRequest;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.UploadDaemonStatus;
import org.sagebionetworks.repo.model.IdList;
import org.sagebionetworks.repo.model.ServiceConstants;
import org.sagebionetworks.repo.model.discussion.DiscussionFilter;
import org.sagebionetworks.repo.model.discussion.DiscussionThreadBundle;
import org.sagebionetworks.repo.model.discussion.DiscussionThreadOrder;
import org.sagebionetworks.repo.model.migration.MigrationTypeList;
import org.sagebionetworks.repo.model.wiki.WikiPage;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.UrlHelpers;
import org.sagebionetworks.repo.web.rest.doc.ControllerInfo;
import org.sagebionetworks.javadoc.testclasses.GenericList;
import org.sagebionetworks.reflection.model.PaginatedResults;
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
 * Proin ornare ligula eu tellus tempus elementum. Aenean bibendum iaculis mi,
 * nec blandit lacus interdum vitae. Vestibulum non nibh risus, a scelerisque
 * purus. Ut vel arcu ac tortor adipiscing hendrerit vel sed massa. Fusce sem
 * libero, lacinia vulputate interdum non, porttitor non quam. Aliquam sed felis
 * ligula. Duis non nulla magna.
 * 
 */
@ControllerInfo(displayName="Example Service", path="example/v1")
@Controller
public class ExampleController {

	/**
	 * Etiam aliquam sem ac velit feugiat elementum. Nunc eu elit velit, nec
	 * vestibulum nibh. Curabitur ultrices, diam non ullamcorper blandit, nunc
	 * lacus ornare nisi, egestas rutrum magna est id nunc. Pellentesque
	 * imperdiet malesuada quam, et rhoncus eros auctor eu. Nullam vehicula
	 * metus ac lacus rutrum nec fermentum urna congue. Vestibulum et risus at
	 * mi ultricies sagittis quis nec ligula. Suspendisse dignissim dignissim
	 * luctus. Duis ac dictum nibh. Etiam id massa magna. Morbi molestie posuere
	 * posuere.
	 * 
	 * Etiam aliquam sem ac velit feugiat elementum. Nunc eu elit velit, nec
	 * vestibulum nibh. Curabitur ultrices, diam non ullamcorper blandit, nunc
	 * lacus ornare nisi, egestas rutrum magna est id nunc. Pellentesque
	 * imperdiet malesuada quam, et rhoncus eros auctor eu. Nullam vehicula
	 * metus ac lacus rutrum nec fermentum urna congue. Vestibulum et risus at
	 * mi ultricies sagittis quis nec ligula. Suspendisse dignissim dignissim
	 * luctus. Duis ac dictum nibh. Etiam id massa magna. Morbi molestie posuere
	 * posuere.
	 * 
	 * @param userId
	 *            - the user's id.
	 * @param ownerId
	 *            The ID of the object that owns the wiki page.
	 * @param toCreate
	 *            - The wiki page to create.
	 * @return -
	 * @throws DatastoreException
	 *             - Synapse error.
	 * @throws NotFoundException
	 *             - returned if the user or owner does not exist.
	 */
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = "/entity/{ownerId}/wiki", method = RequestMethod.POST)
	public @ResponseBody
	WikiPage createEntityWikiPage(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@PathVariable String ownerId, @RequestBody WikiPage toCreate)
			throws DatastoreException, NotFoundException {
		return null;
	}

	/**
	 * Maecenas eu placerat ante. Fusce ut neque justo, et aliquet enim. In hac
	 * habitasse platea dictumst. Nullam commodo neque erat, vitae facilisis
	 * erat. Cras at mauris ut tortor vestibulum fringilla vel sed metus. Donec
	 * interdum purus a justo feugiat rutrum. Sed ac neque ut neque dictum
	 * accumsan. Cras lacinia rutrum risus, id viverra metus dictum sit amet.
	 * Fusce venenatis, urna eget cursus placerat, dui nisl fringilla purus, nec
	 * tincidunt sapien justo ut nisl. Curabitur lobortis semper neque et
	 * varius. Etiam eget lectus risus, a varius orci. Nam placerat mauris at
	 * dolor imperdiet at aliquet lectus ultricies. Duis tincidunt mi at quam
	 * condimentum lobortis.
	 * 
	 * @param userId
	 * @param ownerId
	 *            - The ID of the object that owns the wiki page.
	 * @param toCreate
	 *            - The body
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = "/evaluation/{ownerId}/wiki", method = RequestMethod.POST)
	public @ResponseBody
	WikiPage createCompetitionWikiPage(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@PathVariable String ownerId, @RequestBody WikiPage toCreate)
			throws DatastoreException, NotFoundException {
		return null;
	}

	/**
	 * Lorem Ipsum is simply dummy text of the printing and typesetting
	 * industry. Lorem Ipsum has been the industry's standard dummy text ever
	 * since the 1500s, when an unknown printer took a galley of type and
	 * scrambled it to make a type specimen book. It has survived not only five
	 * centuries, but also the leap into electronic typesetting, remaining
	 * essentially unchanged. It was popularised in the 1960s with the release
	 * of Letraset sheets containing Lorem Ipsum passages, and more recently
	 * with desktop publishing software like Aldus PageMaker including versions
	 * of Lorem Ipsum.
	 * 
	 * @param userId
	 * @param cacf
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = "/startCompleteUploadDaemon", method = RequestMethod.POST)
	public @ResponseBody
	UploadDaemonStatus startCompleteUploadDaemon(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) String userId,
			@RequestBody CompleteAllChunksRequest cacf)
			throws DatastoreException, NotFoundException {
		return null;
	}

	/**
	 * In facilisis scelerisque dui vel dignissim. Sed nunc orci, ultricies
	 * congue vehicula quis, facilisis a orci. In aliquet facilisis condimentum.
	 * Donec at orci orci, a dictum justo. Sed a nunc non lectus fringilla
	 * suscipit. Vivamus pretium sapien sit amet mauris aliquet eleifend vel
	 * vitae arcu. Fusce pharetra dignissim nisl egestas pretium.
	 * 
	 * @param userId
	 * @param type The MigrationType.name()
	 * @param limit Limit the number of results resturned.
	 * @param offset The offest from zero of the page.
	 * @param bar The parameter named foo.
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = "/multiple/params", method = RequestMethod.GET)
	public @ResponseBody
	MigrationTypeList getRowMetadataDelta(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = true) String userId,
			@RequestParam(required = true) String type,
			@RequestParam(required = false) String limit,
			@RequestParam(required = false) String offset,
			@RequestParam(value = "foo") String bar,
			@RequestBody IdList request) throws DatastoreException,
			NotFoundException {
		if (request == null)
			throw new IllegalArgumentException("Request cannot be null");
		return null;
	}
	
	/**
	 * In facilisis scelerisque dui vel dignissim. Sed nunc orci, ultricies
	 * congue vehicula quis, facilisis a orci. In aliquet facilisis condimentum.
	 * Donec at orci orci, a dictum justo. Sed a nunc non lectus fringilla
	 * suscipit. Vivamus pretium sapien sit amet mauris aliquet eleifend vel
	 * vitae arcu. Fusce pharetra dignissim nisl egestas pretium.
	 * 
	 * @param pathVar An exmaple path variable
	 * @param type The MigrationType.name()
	 * @param limit Limit the number of results resturned.
	 * @param offset The offest from zero of the page.
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = "/multiple/{pathVar}", method = RequestMethod.GET)
	public @ResponseBody
	MigrationTypeList noAuthPathVariable(
			@PathVariable(required = true) String pathVar,
			@RequestParam(required = false) String limit,
			@RequestParam(required = false) String offset,
			@RequestBody IdList request) throws DatastoreException,
			NotFoundException {
		if (request == null)
			throw new IllegalArgumentException("Request cannot be null");
		return null;
	}
	

	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = "/missing/descriptions", method = RequestMethod.GET)
	public @ResponseBody
	MigrationTypeList missingDescriptions(
			@PathVariable(required = true) String pathVar,
			@RequestParam(required = false) String limit,
			@RequestParam(required = false) String offset,
			@RequestBody IdList request) throws DatastoreException,
			NotFoundException {
		if (request == null)
			throw new IllegalArgumentException("Request cannot be null");
		return null;
	}
	
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = "/nonJson", method = RequestMethod.GET)
	public @ResponseBody
	String nonJsonEntity(@RequestBody String request) {
		return null;
	}
	
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = "/void", method = RequestMethod.GET)
	public @ResponseBody
	void noParamsOrReturn() {
		return null;
	}

	/**
	 * Get getting an object that is an interface
	 * Link to this controller <a href="${org.sagebionetworks.samples.ExampleController}">Example Controller</a>
	 * @return
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = "/some/interface", method = RequestMethod.GET)
	public @ResponseBody
	FileHandle getInterface(@RequestBody Annotations annos) {
		return null;
	}
	
	/**
	 * This method is depricated and should not be included.
	 * @return
	 */
	@Deprecated
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = "/some/depricated", method = RequestMethod.GET)
	public @ResponseBody
	FileHandle someDepricated(@RequestBody Annotations annos) {
		return null;
	}
	
	/**
	 * returning a generic
	 * @param annos the list of annotations
	 * @return the list of strings
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = "/some/generic", method = RequestMethod.GET)
	public @ResponseBody
	GenericList<Entity> someGenericReturn() {
		return null;
	}
	
	/**
	 * passing a generic
	 * @param annos the list of annotations
	 * @return the list of strings
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = "/some/generic", method = RequestMethod.POST)
	public @ResponseBody
	Long someGenericParam(@RequestBody GenericList<Annotations> annos) {
		return null;
	}

	/**
	 * using enum in the param.
	 * Available filter options: <a href="${org.sagebionetworks.repo.model.discussion.DiscussionFilter}">DiscussionFilter</a>.
	 * 
	 * @param filter - Available options: <a href="${org.sagebionetworks.repo.model.discussion.DiscussionFilter}">DiscussionFilter</a>.
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = "/some/enum", method = RequestMethod.GET)
	public @ResponseBody Long enumParam(
			@RequestParam(value = ServiceConstants.DISCUSSION_FILTER_PARAM) DiscussionFilter filter) {
		return null;
	}
}
