package org.sagebionetworks.repo.web.controller;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.ServiceConstants;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.PaginatedColumnModels;
import org.sagebionetworks.repo.model.table.Query;
import org.sagebionetworks.repo.model.table.RowReference;
import org.sagebionetworks.repo.model.table.RowReferenceSet;
import org.sagebionetworks.repo.model.table.RowSet;
import org.sagebionetworks.repo.model.table.TableFileHandleResults;
import org.sagebionetworks.repo.model.table.TableUnavilableException;
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
 * href="${org.sagebionetworks.repo.model.table.ColumnModel}">ColumnModel</a>
 * IDs. Use <a href="${POST.column}">POST /column</a> to create new ColumnModle
 * objects. Each ColumnModel object is immutable, so to change a column of a
 * table a new column must be added and the old column must be removed.
 * TableEntities can be created, updated, read and deleted like any other
 * entity:
 * <ul>
 * <li><a href="${POST.entity}">POST /entity</a></li>
 * <li><a href="${GET.entity.id}">GET /entity/{id}</a></li>
 * <li><a href="${PUT.entity.id}">PUT /entity/{id}</a></li>
 * <li><a href="${DELETE.entity.id}">DELETE /entity/{id}</a></li>
 * </ul>
 * </p>
 * <p>
 * <p>
 * All ColumnModel objects are publicly viewable and usable. Since each
 * ColumnModel is immutable it is safe to re-use ColumModels created by other
 * users. Use the <a href="${GET.column}">GET /column</a> services to list all
 * of the existing ColumnModels that are currently in use.
 * </p>
 * 
 * Once the columns for a TableEntity have been created and assigned to the
 * TableEntity, rows can be added to the table using <a
 * href="${POST.entity.id.table}">POST /entity/{id}/table</a>. Each <a
 * href="${org.sagebionetworks.repo.model.table.Row}">Row</a> appended to the
 * table will automatically be assigned a rowId and a versionNumber and can be
 * found in the resulting <a
 * href="${org.sagebionetworks.repo.model.table.RowReferenceSet}"
 * >RowReferenceSet</a>. To update a row, simply include the row's rowId in the
 * passed <a href="${org.sagebionetworks.repo.model.table.RowSet}">RowSet</a>.
 * Any row without a rowId will be treated as a new row. When a row is updated a
 * new versionNumber will automatically be assigned the Row. While previous
 * versions of any row are kept, only the current version of any row will appear
 * in the table index used to support the query service: <a
 * href="${POST.table.query}">POST /table/query</a> </p>
 * <p>
 * Use the <a href="${POST.table.query}">POST /table/query</a> services to query
 * for the current rows of a table. The returned <a
 * href="${org.sagebionetworks.repo.model.table.RowSet}">RowSet</a> of the table
 * query can be modified and returned to update the rows of a table using <a
 * href="${POST.entity.id.table}">POST /entity/{id}/table</a>.
 * </p>
 */
@ControllerInfo(displayName = "Table Services", path = "repo/v1")
@Controller
@RequestMapping(UrlHelpers.REPO_PATH)
public class TableController extends BaseController {

	@Autowired
	ServiceProvider serviceProvider;

	/**
	 * Create a <a
	 * href="${org.sagebionetworks.repo.model.table.ColumnModel}">ColumnModel
	 * </a> that can be used as a column of a <a
	 * href="${org.sagebionetworks.repo.model.table.TableEntity}"
	 * >TableEntity</a>. Unlike other objects in Synapse ColumnModels are
	 * immutable and reusable and do not have an "owner" or "creator". This
	 * method is idempotent, so if the same ColumnModel is passed multiple time
	 * a new ColumnModel will not be created. Instead the existing ColumnModel
	 * will be returned. This also means if two users create identical
	 * ColumnModels for their tables they will both receive the same
	 * ColumnModel.
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
	 * >ColumnModels</a> that are currently assigned to the table.
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
	 * <p>
	 * List all of the <a
	 * href="${org.sagebionetworks.repo.model.table.ColumnModel}"
	 * >ColumnModels</a> that have been created in Synapse.
	 * </p>
	 * Since each ColumnModel is immutable it is safe to re-use ColumModels
	 * created by other users.
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
	 * <p>
	 * This method is used to both add and update rows to a TableEntity. The
	 * passed RowSet will contain all data for the rows to be added or updated.
	 * The RowSet.rows is a list of Rows, one of each row to add or update. If
	 * the Row.rowId is null, then a row will be added for that request, if a
	 * rowId is provided then the row with that ID will be updated (a 400 will
	 * be returned if a row ID is provided that does not actually exist). The
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
	 * transaction, meaning either all of the rows will be added/updated or none
	 * of the rows will be added/updated. If this web-services fails for any
	 * reason all changes will be "rolled back".
	 * </p>
	 * <p>
	 * There is a limit to the size of a RowSet that can be passed in a single
	 * web-services call. Currently, that limit is set to a maximum size of 2 MB
	 * per call. The maximum size is calculated based on the maximum possible
	 * size of a the ColumModel definition, NOT on the size of the actual passed
	 * data. For example, the maximum size of an integer column is 20
	 * characters. Since each integer is represented as a UTF-8 string (not a
	 * binary representation) with 1 byte per character (for numbers), a single
	 * integer has a maximum size of 20 bytes (20 chars * 1 bytes/char). Since
	 * the page size limits are based on the maximum size and not the actual
	 * size of the data it will be consistent from page to page. This means a
	 * valid page size will work for a all pages even if some pages have more
	 * data that others.
	 * </p>
	 * <p>
	 * Note: The caller must have the <a
	 * href="${org.sagebionetworks.repo.model.ACCESS_TYPE}"
	 * >ACCESS_TYPE.UPDATE</a> permission on the TableEntity to make this call.
	 * </p>
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
		if (id == null)
			throw new IllegalArgumentException("{id} cannot be null");
		rows.setTableId(id);
		return serviceProvider.getTableServices().appendRows(userId, rows);
	}

	/**
	 * <p>
	 * This method is used to delete rows in a TableEntity. The rows in the passed in RowReferenceSet will be deleted if
	 * they exists (a 400 will be returned if a row ID is provided that does not actually exist). A single POST to this
	 * services will be treated as a single transaction, meaning either all of the rows will be deleted or none of the
	 * rows will be deleted. If this web-services fails for any reason all changes will be "rolled back".
	 * </p>
	 * <p>
	 * Note: The caller must have the <a href="${org.sagebionetworks.repo.model.ACCESS_TYPE}" >ACCESS_TYPE.UPDATE</a>
	 * permission on the TableEntity to make this call.
	 * </p>
	 * 
	 * @param userId
	 * @param id The ID of the TableEntity to append rows to.
	 * @param rows The set of rows to add/update.
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws IOException
	 */
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = UrlHelpers.ENTITY_TABLE_DELETE_ROWS, method = RequestMethod.POST)
	public @ResponseBody
	RowReferenceSet deleteRows(@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId, @PathVariable String id,
			@RequestBody RowReferenceSet rowsToDelete) throws DatastoreException, NotFoundException, IOException {
		if (id == null)
			throw new IllegalArgumentException("{id} cannot be null");
		rowsToDelete.setTableId(id);
		return serviceProvider.getTableServices().deleteRows(userId, rowsToDelete);
	}

	/**
	 * <p>
	 * This method is used to get file handle information for rows in a TableEntity. The columns in the passed in
	 * RowReferenceSet need to be FILEHANDLEID columns and the rows in the passed in RowReferenceSet need to exists (a
	 * 400 will be returned if a row ID is provided that does not actually exist). The order of the returned rows of
	 * file handles is the same as the order of the rows requested, and the order of the file handles in each row is the
	 * same as the order of the columns requested.
	 * </p>
	 * <p>
	 * Note: The caller must have the <a href="${org.sagebionetworks.repo.model.ACCESS_TYPE}" >ACCESS_TYPE.READ</a>
	 * permission on the TableEntity to make this call.
	 * </p>
	 * 
	 * @param userId
	 * @param id The ID of the TableEntity to append rows to.
	 * @param rows The set of rows and columns for which to return the file handles.
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws IOException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.ENTITY_TABLE_FILE_HANDLES, method = RequestMethod.POST)
	public @ResponseBody
	TableFileHandleResults getFileHandles(@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId, @PathVariable String id,
			@RequestBody RowReferenceSet fileHandlesToFind) throws DatastoreException, NotFoundException, IOException {
		if (id == null)
			throw new IllegalArgumentException("{id} cannot be null");
		fileHandlesToFind.setTableId(id);
		return serviceProvider.getTableServices().getFileHandles(userId, fileHandlesToFind);
	}

	/**
	 * Get the actual URL of the file associated with a specific version of a row and file handle column.
	 * <p>
	 * Note: This call will result in a HTTP temporary redirect (307), to the actual file URL if the caller meets all of
	 * the download requirements.
	 * </p>
	 * 
	 * @param userId
	 * @param id The ID of the FileEntity to get.
	 * @param columnId
	 * @param rowId
	 * @param versionNumber
	 * @param redirect When set to false, the URL will be returned as text/plain instead of redirecting.
	 * @param response
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws IOException
	 */
	@RequestMapping(value = UrlHelpers.ENTITY_TABLE_FILE, method = RequestMethod.GET)
	public @ResponseBody
	void fileRedirectURLForRow(@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId, @PathVariable String id,
			@PathVariable String columnId, @PathVariable Long rowId, @PathVariable Long versionNumber,
			@RequestParam(required = false) Boolean redirect, HttpServletResponse response) throws DatastoreException, NotFoundException,
			IOException {
		// Get the redirect url
		RowReference ref = new RowReference();
		ref.setRowId(rowId);
		ref.setVersionNumber(versionNumber);
		URL redirectUrl = serviceProvider.getTableServices().getFileRedirectURL(userId, id, ref, columnId);
		RedirectUtils.handleRedirect(redirect, redirectUrl, response);
	}

	/**
	 * Get the preview URL of the file associated with a specific version of a row and file handle column.
	 * <p>
	 * Note: This call will result in a HTTP temporary redirect (307), to the actual file URL if the caller meets all of
	 * the download requirements.
	 * </p>
	 * 
	 * @param userId
	 * @param id The ID of the FileEntity to get.
	 * @param columnId
	 * @param rowId
	 * @param versionNumber
	 * @param redirect When set to false, the URL will be returned as text/plain instead of redirecting.
	 * @param response
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws IOException
	 */
	@RequestMapping(value = UrlHelpers.ENTITY_TABLE_FILE_PREVIEW, method = RequestMethod.GET)
	public @ResponseBody
	void filePreviewRedirectURLForRow(@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId, @PathVariable String id,
			@PathVariable String columnId, @PathVariable Long rowId, @PathVariable Long versionNumber,
			@RequestParam(required = false) Boolean redirect, HttpServletResponse response) throws DatastoreException, NotFoundException,
			IOException {
		// Get the redirect url
		RowReference ref = new RowReference();
		ref.setRowId(rowId);
		ref.setVersionNumber(versionNumber);
		URL redirectUrl = serviceProvider.getTableServices().getFilePreviewRedirectURL(userId, id, ref, columnId);
		RedirectUtils.handleRedirect(redirect, redirectUrl, response);
	}
	/**
	 * <p>
	 * Using a 'SQL like' syntax, query the current version of the rows in a
	 * single table. The following pseudo-syntax is the basic supported format:
	 * </p>
	 * SELECT <br>
	 * [ALL | DISTINCT] select_expr [, select_expr ...] <br>
	 * FROM table_references <br>
	 * [WHERE where_condition] <br>
	 * [GROUP BY {col_name [, [col_name * ...] } <br>
	 * [ORDER BY {col_name [ [ASC | DESC] [, col_name [ [ASC | DESC]]}<br>
	 * [LIMIT row_count [ OFFSET offset ]]<br>
	 * <p>
	 * Please see the following for samples: <a
	 * href="${org.sagebionetworks.repo.web.controller.TableExamples}">Table SQL
	 * Examples</a>
	 * </p>
	 * <p>
	 * Note: Sub-queries and joining tables is not supported.
	 * </p>
	 * <p>
	 * This services depends on an index that is created/update asynchronously
	 * from table creation and update events. This means there could be short
	 * window of time when the index is inconsistent with the true state of the
	 * table. When a query is run with the isConsistent parameter set to true
	 * (the default) and the index is out-of-sych, then a status code of 202
	 * (ACCEPTED) will be returned and the response body will be a <a
	 * href="${org.sagebionetworks.repo.model.table.TableStatus}"
	 * >TableStatus</a> object. The TableStatus will indicates the current
	 * status of the index including how much work is remaining until the index
	 * is consistent with the truth of the table.
	 * </p>
	 * <p>
	 * Note: The caller must have the <a
	 * href="${org.sagebionetworks.repo.model.ACCESS_TYPE}"
	 * >ACCESS_TYPE.READ</a> permission on the TableEntity to make this call.
	 * </p>
	 * 
	 * @param userId
	 * @param query
	 * @param isConsistent
	 *            Defaults to true. When true, a query will be run only if the
	 *            index is up-to-date with all changes to the table and a
	 *            read-lock is successfully acquired on the index. When set to
	 *            false, the query will be run against the index regardless of
	 *            the state of the index and without attempting to acquire a
	 *            read-lock. When isConsistent is set to false the query results
	 *            will not contain an etag so the results cannot be used as
	 *            input to a table update.
	 * @param countOnly
	 *            When this parameter is included and set to 'true', the passed
	 *            query will be converted into a count query. This means the
	 *            passed select clause will be replaced with 'count(*)' and
	 *            pagination, order by, and group by will all be ignored. This
	 *            can be used to to setup client-side paging.
	 * 
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws IOException
	 * @throws TableUnavilableException
	 */
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = UrlHelpers.TABLE_QUERY, method = RequestMethod.POST)
	public @ResponseBody
	RowSet query(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestBody Query query,
			@RequestParam(value = ServiceConstants.IS_CONSISTENT, required = false) Boolean isConsistent,
			@RequestParam(value = ServiceConstants.COUNT_ONLY, required = false) Boolean countOnly)
			throws DatastoreException, NotFoundException, IOException,
			TableUnavilableException {
		// By default isConsistent is true.
		boolean isConsistentValue = true;
		if (isConsistent != null) {
			isConsistentValue = isConsistent;
		}
		// Count only is false by default
		boolean countOnlyValue = false;
		if (countOnly != null) {
			countOnlyValue = countOnly;
		}
		return serviceProvider.getTableServices().query(userId, query,
				isConsistentValue, countOnlyValue);
	}

}
