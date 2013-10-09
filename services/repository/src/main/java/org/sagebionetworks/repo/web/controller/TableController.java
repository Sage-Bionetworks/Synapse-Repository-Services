package org.sagebionetworks.repo.web.controller;

import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.UrlHelpers;
import org.sagebionetworks.repo.web.rest.doc.ControllerInfo;
import org.sagebionetworks.repo.web.service.ServiceProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * <p>
 * The Synapse <a
 * href="${org.sagebionetworks.repo.model.table.TableEntity}">TableEntity</a>
 * model object represents the metadata of a relational-like table.
 * </p>
 * 
 */
@ControllerInfo(displayName = "Table Services", path = "repo/v1")
@Controller
public class TableController extends BaseController {

	@Autowired
	ServiceProvider serviceProvider;

	/**
	 * Create a <a
	 * href="${org.sagebionetworks.repo.model.table.ColumnModel}">ColumnModel
	 * </a> that can be used as a column of a <a
	 * href="${org.sagebionetworks.repo.model.table.TableEntity}"
	 * >TableEntity</a>.
	 * 
	 * @param userId
	 *            The user's id.
	 * @param ownerId
	 *            The ID of the owner Entity.
	 * @param toCreate
	 *            The WikiPage to create.
	 * @return -
	 * @throws DatastoreException
	 *             - Synapse error.
	 * @throws NotFoundException
	 *             - returned if the user or owner does not exist.
	 */
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = UrlHelpers.COLUMN, method = RequestMethod.POST)
	public @ResponseBody
	ColumnModel createColumnModel(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@RequestBody ColumnModel toCreate) throws DatastoreException,
			NotFoundException {
		return serviceProvider.getTableServices().createColumnModel(userId,
				toCreate);
	}
}
