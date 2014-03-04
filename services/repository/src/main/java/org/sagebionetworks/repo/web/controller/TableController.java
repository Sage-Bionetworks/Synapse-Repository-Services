package org.sagebionetworks.repo.web.controller;

import java.io.IOException;

import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.ServiceConstants;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.PaginatedColumnModels;
import org.sagebionetworks.repo.model.table.RowReferenceSet;
import org.sagebionetworks.repo.model.table.RowSet;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.UrlHelpers;
import org.sagebionetworks.repo.web.rest.doc.ControllerInfo;
import org.sagebionetworks.repo.web.service.ServiceProvider;
import org.springframework.beans.factory.annotation.Autowired;
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
 * <p>
 * A Synapse <a
 * href="${org.sagebionetworks.repo.model.table.TableEntity}">TableEntity</a>
 * model object represents the metadata of a table. Each TableEntity is defined
 * by a list of <a
 * href="${org.sagebionetworks.repo.model.table.ColumnModel}">ColumnModels</a>
 * IDs.
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
	 * >TableEntity</a>. ColumnModels are immutable and reusable.
	 * 
	 * @param userId
	 *            The user's id.
	 * @param toCreate
	 *            The ColumnModel to create.
	 * @return -
	 * @throws DatastoreException
	 *             - Synapse error.
	 */
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = UrlHelpers.COLUMN, method = RequestMethod.POST)
	public @ResponseBody
	ColumnModel createColumnModel(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestBody ColumnModel toCreate) throws DatastoreException,
			NotFoundException {
		return serviceProvider.getTableServices().createColumnModel(userId,
				toCreate);
	}

	/**
	 * Get a <a
	 * href="${org.sagebionetworks.repo.model.table.ColumnModel}">ColumnModel
	 * </a> using its ID.
	 * 
	 * @param userId
	 * @param columnId
	 *            The ID of the ColumnModel to get.
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.COLUMN_ID, method = RequestMethod.GET)
	public @ResponseBody
	ColumnModel getColumnModel(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String columnId) throws DatastoreException,
			NotFoundException {
		return serviceProvider.getTableServices().getColumnModel(userId,
				columnId);
	}

	/**
	 * Given the ID of a <a
	 * href="${org.sagebionetworks.repo.model.table.TableEntity}"
	 * >TableEntity</a>, get its list of <a
	 * href="${org.sagebionetworks.repo.model.table.ColumnModel}"
	 * >ColumnModels</a>.
	 * 
	 * @param userId
	 * @param id
	 *            The ID of the TableEntity to get the ColumnModels for.
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.ENTITY_COLUMNS, method = RequestMethod.GET)
	public @ResponseBody
	PaginatedColumnModels getColumnForTable(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String id) throws DatastoreException,
			NotFoundException {
		return serviceProvider.getTableServices()
				.getColumnModelsForTableEntity(userId, id);
	}

	/**
	 * List all of the a
	 * href="${org.sagebionetworks.repo.model.table.ColumnModel}"
	 * >ColumnModels</a> in Synapse.
	 * 
	 * @param userId
	 * @param prefix
	 *            When included, only columns with a name that starts with this
	 *            prefix will be returned.
	 * @param offset
	 *            The index of the pagination offset. For a page size of 10, the
	 *            first page would be at offset = 0, and the second page would
	 *            be at offset = 10.
	 * @param limit
	 *            Limits the size of the page returned. For example, a page size
	 *            of 10 require limit = 10. The maximum Limit for this call is
	 *            100. The default Limit is 10;
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.COLUMN, method = RequestMethod.GET)
	public @ResponseBody
	PaginatedColumnModels listColumnModels(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestParam(required = false) String prefix,
			@RequestParam(value = ServiceConstants.PAGINATION_LIMIT_PARAM, required = false) Long limit,
			@RequestParam(value = ServiceConstants.PAGINATION_OFFSET_PARAM, required = false) Long offset)
			throws DatastoreException, NotFoundException {
		return serviceProvider.getTableServices().listColumnModels(userId,
				prefix, limit, offset);
	}

	/**
	 * This method is used to both add and update rows to a TableEntity. The
	 * passed RowSet will contain all data for the rows to add or update. The
	 * RowSet.rows is a list of Rows, one of each row to add or update. If the
	 * Row.rowId is null, then a row will be added for that request, if a rowId
	 * is provided then the row with that ID will be updated (a 400 will be
	 * returned if a row ID is provided that does not actually exist). The
	 * Row.values list should contain a value for each column of the row. The
	 * RowSet.headers identifies the columns (by ID) that are to be updated by
	 * this request. Each Row.value list must be the same size as the
	 * RowSet.headers, as each value is mapped to a column by the index of these
	 * two arrays. When a row is added it will be issued both a rowId and a
	 * version number. When a row is updated it will be issued a new version
	 * number (each row version is immutable). The resulting TableRowReference
	 * will enumerate all rowIds and versionNumbers for this update. The
	 * resulting RowReferecnes will be listed in the same order as the passed
	 * result set. A single POST to this services will be treated as a single
	 * transaction, meaning all of the rows will be added/updated or none of the
	 * rows will be added/updated. If this web-services fails for any reason all
	 * changes will be "rolled back".
	 * 
	 * @param userId
	 * @param id
	 *            The ID of the TableEntity to append rows to.
	 * @param rows
	 *            The set of rows to add/update.
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws IOException
	 */
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = UrlHelpers.ENTITY_TABLE, method = RequestMethod.POST)
	public @ResponseBody
	RowReferenceSet appendRows(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String id, @RequestBody RowSet rows)
			throws DatastoreException, NotFoundException, IOException {
		if(id == null) throw new IllegalArgumentException("{id} cannot be null");
		rows.setTableId(id);
		return serviceProvider.getTableServices().appendRows(userId, rows);
	}

}
