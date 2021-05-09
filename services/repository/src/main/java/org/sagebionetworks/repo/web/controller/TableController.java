package org.sagebionetworks.repo.web.controller;

import static org.sagebionetworks.repo.model.oauth.OAuthScope.download;
import static org.sagebionetworks.repo.model.oauth.OAuthScope.modify;
import static org.sagebionetworks.repo.model.oauth.OAuthScope.view;

import java.io.IOException;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.sagebionetworks.repo.model.AsynchJobFailedException;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.ListWrapper;
import org.sagebionetworks.repo.model.NotReadyException;
import org.sagebionetworks.repo.model.ServiceConstants;
import org.sagebionetworks.repo.model.asynch.AsyncJobId;
import org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus;
import org.sagebionetworks.repo.model.table.AppendableRowSetRequest;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnModelPage;
import org.sagebionetworks.repo.model.table.DownloadFromTableRequest;
import org.sagebionetworks.repo.model.table.DownloadFromTableResult;
import org.sagebionetworks.repo.model.table.PaginatedColumnModels;
import org.sagebionetworks.repo.model.table.QueryBundleRequest;
import org.sagebionetworks.repo.model.table.QueryNextPageToken;
import org.sagebionetworks.repo.model.table.QueryResult;
import org.sagebionetworks.repo.model.table.QueryResultBundle;
import org.sagebionetworks.repo.model.table.RowReference;
import org.sagebionetworks.repo.model.table.RowReferenceSet;
import org.sagebionetworks.repo.model.table.RowReferenceSetResults;
import org.sagebionetworks.repo.model.table.RowSelection;
import org.sagebionetworks.repo.model.table.SnapshotRequest;
import org.sagebionetworks.repo.model.table.SnapshotResponse;
import org.sagebionetworks.repo.model.table.SqlTransformRequest;
import org.sagebionetworks.repo.model.table.SqlTransformResponse;
import org.sagebionetworks.repo.model.table.TableFailedException;
import org.sagebionetworks.repo.model.table.TableFileHandleResults;
import org.sagebionetworks.repo.model.table.TableUnavailableException;
import org.sagebionetworks.repo.model.table.TableUpdateTransactionRequest;
import org.sagebionetworks.repo.model.table.TableUpdateTransactionResponse;
import org.sagebionetworks.repo.model.table.UploadToTablePreviewRequest;
import org.sagebionetworks.repo.model.table.UploadToTablePreviewResult;
import org.sagebionetworks.repo.model.table.UploadToTableRequest;
import org.sagebionetworks.repo.model.table.UploadToTableResult;
import org.sagebionetworks.repo.model.table.ViewColumnModelRequest;
import org.sagebionetworks.repo.model.table.ViewColumnModelResponse;
import org.sagebionetworks.repo.model.table.ViewEntityType;
import org.sagebionetworks.repo.model.table.ViewScope;
import org.sagebionetworks.repo.model.table.ViewType;
import org.sagebionetworks.repo.model.table.ViewTypeMask;
import org.sagebionetworks.repo.web.DeprecatedServiceException;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.RequiredScope;
import org.sagebionetworks.repo.web.UrlHelpers;
import org.sagebionetworks.repo.web.rest.doc.ControllerInfo;
import org.sagebionetworks.repo.web.service.ServiceProvider;
import org.sagebionetworks.table.cluster.utils.TableModelUtils;
import org.sagebionetworks.table.query.ParseException;
import org.sagebionetworks.util.ValidateArgument;
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
 * A Synapse
 * <a href="${org.sagebionetworks.repo.model.table.TableEntity}">TableEntity</a>
 * model object represents the metadata of a table. Each TableEntity is defined
 * by a list of
 * <a href="${org.sagebionetworks.repo.model.table.ColumnModel}">ColumnModel</a>
 * IDs. Use <a href="${POST.column}">POST /column</a> to create new ColumnModel
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
 * ColumnModel is immutable it is safe to re-use ColumnModels created by other
 * users. Use the <a href="${GET.column}">GET /column</a> services to list all
 * of the existing ColumnModels that are currently in use.
 * </p>
 * 
 * Once the columns for a TableEntity have been created and assigned to the
 * TableEntity, rows can be added to the table using
 * <a href="${POST.entity.id.table.transaction.async.start}">POST
 * /entity/{id}/table/transaction/async/start</a>. Each
 * <a href="${org.sagebionetworks.repo.model.table.Row}">Row</a> appended to the
 * table will automatically be assigned a rowId and a versionNumber and can be
 * found in the resulting
 * <a href="${org.sagebionetworks.repo.model.table.RowReferenceSet}"
 * >RowReferenceSet</a>. To update a row, simply include the row's rowId in the
 * passed <a href="${org.sagebionetworks.repo.model.table.RowSet}">RowSet</a>.
 * Any row without a rowId will be treated as a new row. When a row is updated a
 * new versionNumber will automatically be assigned the Row. While previous
 * versions of any row are kept, only the current version of any row will appear
 * in the table index used to support the query service:
 * <a href="${POST.entity.id.table.query.async.start}">POST
 * /entity/{id}/table/query/async/start</a>
 * </p>
 * <p>
 * Use the <a href="${POST.entity.id.table.query.async.start}">POST
 * /entity/{id}/table/query/async/start</a> services to query for the current
 * rows of a table. The returned
 * <a href="${org.sagebionetworks.repo.model.table.RowSet}">RowSet</a> of the
 * table query can be modified and returned to update the rows of a table using
 * <a href="${POST.entity.id.table.transaction.async.start}">POST
 * /entity/{id}/table/transaction/async/start</a>.
 * </p>
 * <p>
 * There is also an <a href=
 * "${org.sagebionetworks.repo.web.controller.AsynchronousJobController}"
 * >asynchronous service</a> to
 * <a href="${org.sagebionetworks.repo.model.table.UploadToTableRequest}"
 * >upload</a> and
 * <a href="${org.sagebionetworks.repo.model.table.DownloadFromTableRequest}"
 * >download</a> csv files, suitable for large datasets.
 * </p>
 * <p>
 * <b>Table Service Limits</b>
 * <table border="1">
 * <tr>
 * <th>resource</th>
 * <th>limit</th>
 * <th>notes</th>
 * </tr>
 * <tr>
 * <td>Maximum size of column names</td>
 * <td>256 characters</td>
 * <td></td>
 * </tr>
 * <tr>
 * <td>Maximum number of enumeration values for a single column</td>
 * <td>100</td>
 * <td></td>
 * </tr>
 * <tr>
 * <td>Maximum number of columns per table/view</td>
 * <td>152</td>
 * <td></td>
 * </tr>
 * <tr>
 * <td>The maximum possible width of a table/view</td>
 * <td>64 KB</td>
 * <td>Each
 * <a href="${org.sagebionetworks.repo.model.table.ColumnType}" >ColumnType</a>
 * has a maximum possible size. The total width of a table/view is the sum of
 * the maximum size of each of its columns</td>
 * </tr>
 * <tr>
 * <td>The maximum number of LARG_TEXT columns per table/view</td>
 * <td>30</td>
 * <td></td>
 * </tr>
 * <td>Maximum table size</td>
 * <td>~146 GB</td>
 * <td>All row changes applied to a table are automatically batched into changes
 * sets with a maximum size of 5242880 bytes (5 MB). Currently, there is a limit
 * of 30,000 change sets per table. Therefore, the theoretical maximum size of
 * table is 5242880 bytes * 30,000 = ~ 146 GB.</td>
 * </tr>
 * <tr>
 * <td>The maximum number of projects/folder per view scope</td>
 * <td>20 K</td>
 * <td>Recursive sub-folders count towards this limit. For example, if a project
 * contains more than 20 K sub-folders then it cannot be included in a view's
 * scope.</td>
 * </tr>
 * <tr>
 * <td>The maximum number of rows per view</td>
 * <td>200 M</td>
 * <td>A single folder cannot contain more then 10 K files/folders. Since a
 * view's scope is limited to 20 K project/folders, the maximum number of rows
 * per view is 10 K * 20 K = 200 M.</td>
 * </tr>
 * <tr>
 * <td>The maximum file size of a CSV that can be appended to a table</td>
 * <td>1 GB</td>
 * <td></td>
 * </tr>
 * <tr>
 * <td>The maximum size of a single query result</td>
 * <td>512000 bytes</td>
 * <td></td>
 * </tr>
 * <tr>
 * <td>Entity View only: The maximum total character length for a STRING or STRING_LIST <a href="${org.sagebionetworks.repo.model.table.ColumnType}" >ColumnType</a></td>
 * <td>500 characters</td>
 * <td>Entity Views ONLY! This follows limitations placed on Annotations. For the type STRING_LIST, the total character count is the cumulative length of all string contained in the list.</td>
 * </tr>
 * <tr>
 * <td>Entity View only: The maximum list length for "_LIST" suffixed <a href="${org.sagebionetworks.repo.model.table.ColumnType}" >ColumnType</a></td>
 * <td>100 values</td>
 * <td>Entity Views ONLY! This follows limitations placed on Annotations.</td>
 * </tr>
 * </table>
 * 
 * 
 */
@ControllerInfo(displayName = "Table Services", path = "repo/v1")
@Controller
@RequestMapping(UrlHelpers.REPO_PATH)
public class TableController {

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
	@RequiredScope({view,modify})
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
	 * Create a batch of <a
	 * href="${org.sagebionetworks.repo.model.table.ColumnModel}">ColumnModel
	 * </a> that can be used as columns of a <a
	 * href="${org.sagebionetworks.repo.model.table.TableEntity}"
	 * >TableEntity</a>. Unlike other objects in Synapse ColumnModels are
	 * immutable and reusable and do not have an "owner" or "creator". This
	 * method is idempotent, so if the same ColumnModel is passed multiple time
	 * a new ColumnModel will not be created. Instead the existing ColumnModel
	 * will be returned. This also means if two users create identical
	 * ColumnModels for their tables they will both receive the same
	 * ColumnModel.
	 * 
	 * This call will either create all column models or create none
	 * 
	 * @param userId
	 *            The user's id.
	 * @param toCreate
	 *            The ColumnModel to create.
	 * @return -
	 * @throws DatastoreException
	 *             - Synapse error.
	 */
	@RequiredScope({view,modify})
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = UrlHelpers.COLUMN_BATCH, method = RequestMethod.POST)
	public @ResponseBody
	ListWrapper<ColumnModel> createColumnModels(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestBody ListWrapper<ColumnModel> toCreate)
			throws DatastoreException, NotFoundException {
		List<ColumnModel> results = serviceProvider.getTableServices()
				.createColumnModels(userId, toCreate.getList());
		return ListWrapper.wrap(results, ColumnModel.class);
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
	@RequiredScope({view})
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
	 * <p>
	 * <b>Service Limits</b>
	 * <table border="1">
	 * <tr>
	 * <th>resource</th>
	 * <th>limit</th>
	 * </tr>
	 * <tr>
	 * <td>The maximum frequency this method can be called</td>
	 * <td>6 calls per minute</td>
	 * </tr>
	 * </table>
	 * </p>
	 * @param userId
	 * @param id
	 *            The ID of the TableEntity to get the ColumnModels for.
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	@RequiredScope({view})
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
	 * Since each ColumnModel is immutable it is safe to re-use ColumnModels
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
	@RequiredScope({view})
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
	 * Get the list of default
	 * <a href="${org.sagebionetworks.repo.model.table.ColumnModel}">ColumnModels
	 * </a> that are available based on the types included in the view.
	 * 
	 * @param viewtype
	 *            Deprecated. Use: 'viewTypeMask'. Must be a value from <a href=
	 *            "${org.sagebionetworks.repo.model.table.ViewType}">ViewType </a>
	 *            enumeration. 
	 * @return -
	 * @throws DatastoreException
	 *             - Synapse error.
	 *  
	 */
	@Deprecated
	@RequiredScope({view})
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.COLUMN_TABLE_VIEW_DEFAULT_TYPE, method = RequestMethod.GET)
	public @ResponseBody
	ListWrapper<ColumnModel> getDefaultColumnsForViewType(
			@PathVariable String viewtype)
			throws DatastoreException, NotFoundException {
		ViewType type = ViewType.valueOf(viewtype);
		Long viewTypeMaks = ViewTypeMask.getMaskForDepricatedType(type);
		List<ColumnModel> results = serviceProvider.getTableServices()
				.getDefaultViewColumnsForType(ViewEntityType.entityview, viewTypeMaks);
		return ListWrapper.wrap(results, ColumnModel.class);
	}
	
	/**
	 * Get the list of default
	 * <a href="${org.sagebionetworks.repo.model.table.ColumnModel}">ColumnModels
	 * </a> for the given <a href=
	 * "${org.sagebionetworks.repo.model.table.ViewEntityType}">viewEntityType</a>
	 * and viewTypeMask.
	 * 
	 * @param viewEntityType The <a href=
	 *                       "${org.sagebionetworks.repo.model.table.ViewEntityType}">entity
	 *                       type</a> of the view, if omitted use entityview
	 * @param viewTypeMask   Bit mask representing the types to include in the view.
	 *                       Not required for a submission view. For an entity view
	 *                       following are the possible types: (type=<mask_hex>):
	 *                       File=0x01, Project=0x02, Table=0x04, Folder=0x08,
	 *                       View=0x10, Docker=0x20.
	 * 
	 * @return -
	 * @throws DatastoreException - Synapse error.
	 */
	@RequiredScope({view})
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.COLUMN_TABLE_VIEW_DEFAULT, method = RequestMethod.GET)
	public @ResponseBody
	ListWrapper<ColumnModel> getDefaultColumnsForViewType(
			@RequestParam(value = "viewEntityType", required = false, defaultValue = "entityview") ViewEntityType viewEntityType,
			@RequestParam(value = "viewTypeMask", required = false) Long viewTypeMask)
			throws DatastoreException, NotFoundException {
		List<ColumnModel> results = serviceProvider.getTableServices().getDefaultViewColumnsForType(viewEntityType, viewTypeMask);
		return ListWrapper.wrap(results, ColumnModel.class);
	}
	
	/**
	 * Start a table update job that will attempt to make all of the requested changes in
	 * a single transaction. All updates will either succeed or fail as a unit.  All update
	 * requests must be for the same table.
	 * <p>
	 * Note: The caller must have the <a
	 * href="${org.sagebionetworks.repo.model.ACCESS_TYPE}"
	 * >ACCESS_TYPE.UPDATE</a> permission on the TableEntity to make this call.
	 * </p>
	 * <p>
	 * <b>Service Limits</b>
	 * <table border="1">
	 * <tr>
	 * <th>resource</th>
	 * <th>limit</th>
	 * </tr>
	 * <tr>
	 * <td>The maximum size of a PartialRow change </td>
	 * <td>2 MB</td>
	 * </tr>
	 * <tr>
	 * <td>The maximum size of a CSV that can be appended to a table</td>
	 * <td>1 GB</td>
	 * </tr>
	 * </table>
	 * </p>
	 * @param userId
	 * @param id
	 *            The ID of the TableEntity to update.
	 * @param request
	 *            List of table update requests to be applied as a single transaction.
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws IOException
	 */
	@RequiredScope({view,modify})
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = UrlHelpers.ENTITY_TABLE_TRANSACTION_ASYNC_START, method = RequestMethod.POST)
	public @ResponseBody
	AsyncJobId startTableTransactionJob(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String id, @RequestBody TableUpdateTransactionRequest request)
			throws DatastoreException, NotFoundException, IOException {
		ValidateArgument.required(id, "{id}");
		ValidateArgument.required(request, "TableUpdateTransactionRequest");
		request.setEntityId(id);
		AsynchronousJobStatus job = serviceProvider
				.getAsynchronousJobServices().startJob(userId, request);
		AsyncJobId asyncJobId = new AsyncJobId();
		asyncJobId.setToken(job.getJobId());
		return asyncJobId;
	}

	/**
	 * Asynchronously get the results of a table update transaction started with
	 * <a href="${POST.entity.id.table.transaction.async.start}">POST
	 * /entity/{id}/table/transaction/async/start</a>
	 * <p>
	 * Note: When the result is not ready yet, this method will return a status
	 * code of 202 (ACCEPTED) and the response body will be a <a
	 * href="${org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus}"
	 * >AsynchronousJobStatus</a> object.
	 * </p>
	 * 
	 * @param userId
	 * @param asyncToken
	 * @param id The ID of the table entity.
	 * @param asyncToken The token returned when the job was started.
	 * @return
	 * @throws NotReadyException
	 * @throws NotFoundException
	 * @throws AsynchJobFailedException
	 */
	@RequiredScope({view})
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = UrlHelpers.ENTITY_TABLE_TRANSACTION_ASYNC_GET, method = RequestMethod.GET)
	public @ResponseBody
	TableUpdateTransactionResponse getTableTransactionResult(@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String id, @PathVariable String asyncToken) throws Throwable {
		if (id == null)
			throw new IllegalArgumentException("{id} cannot be null");
		AsynchronousJobStatus jobStatus = serviceProvider
				.getAsynchronousJobServices().getJobStatusAndThrow(userId,
						asyncToken);
		return (TableUpdateTransactionResponse) jobStatus.getResponseBody();
	}


	/**
	 * <p>
	 * Asynchronously start a job to append row data to a table. This method is
	 * used to both add and update rows to a TableEntity. This method accepts
	 * either a <a href="${org.sagebionetworks.repo.model.table.RowSet}"
	 * >RowSet</a> for setting an entire row or a <a
	 * href="${org.sagebionetworks.repo.model.table.PartialRowSet}"
	 * >PartialRowSet</a> for setting a sub-set of the cells of a row.
	 * </p>
	 * <p>
	 * <B>PartialRowSet:</B> The passed PartialRowSet will contain some or all data for
	 * the rows to be added or updated. The PartialRowSet.rows is a list of
	 * PartialRows, one of each row to add or update. If the PartialRow.rowId is
	 * null, then a row will be added for that request, if a rowId is provided
	 * then the row with that ID will be updated (a 400 will be returned if a
	 * row ID is provided that does not actually exist). For inserts, the
	 * PartialRow.values should contain all the values the user wants to set
	 * explicitly. A null value will be replaced with the default value if
	 * appropriate. For updates, only the columns represented in
	 * PartialRow.values will be updated. Updates will always overwrite the
	 * current value of the cell. A null value for a column that has a default
	 * value, will be changed to the default value. A PartialRow.values
	 * identifies the column by ID in the key. When a row is added it will be
	 * issued both a rowId and a version number. When a row is updated it will
	 * be issued a new version number (each row version is immutable). If
	 * PartialRow.values is null, the corresponding row will be deleted. If
	 * PartialRow.values is an empty map, then no change will be made to that
	 * row.
	 * </p>
	 * <p>
	 * <B>RowSet</B>: The passed RowSet will contain all data for the rows to be added
	 * or updated. The RowSet.rows is a list of Rows, one of each row to add or
	 * update. If the Row.rowId is null, then a row will be added for that
	 * request, if a rowId is provided then the row with that ID will be updated
	 * (a 400 will be returned if a row ID is provided that does not actually
	 * exist). The Row.values list should contain a value for each column of the
	 * row. The RowSet.headers identifies the columns (by ID) that are to be
	 * updated by this request. Each Row.value list must be the same size as the
	 * RowSet.headers, as each value is mapped to a column by the index of these
	 * two arrays. When a row is added it will be issued both a rowId and a
	 * version number. When a row is updated it will be issued a new version
	 * number (each row version is immutable). The resulting RowReferenceSet
	 * will enumerate all rowIds and versionNumbers for this update. The
	 * resulting RowReferences will be listed in the same order as the passed
	 * result set. A single POST to this service will be treated as a single
	 * transaction, meaning either all of the rows will be added/updated or none
	 * of the rows will be added/updated. If this web-services fails for any
	 * reason all changes will be "rolled back".
	 * </p>
	 * <p>
	 * The resulting RowReferenceSet will enumerate all rowIds and
	 * versionNumbers for this update. The resulting RowReferences will be
	 * listed in the same order as the passed result set. A single POST to this
	 * services will be treated as a single transaction, meaning either all of
	 * the rows will be added/updated or none of the rows will be added/updated.
	 * If this web-services fails for any reason all changes will be
	 * "rolled back".
	 * </p>
	 * <p>
	 * There is a limit to the size of a request that can be passed in a single
	 * web-services call. Currently, that limit is set to a maximum size of 2 MB
	 * per call. The maximum size is calculated based on the maximum possible
	 * size of the ColumnModel definition, NOT on the size of the actual passed
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
	 * @param request
	 *            Contains the set of rows to add/update.
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws IOException
	 */
	@Deprecated
	@RequiredScope({view,modify})
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = UrlHelpers.ENTITY_TABLE_APPEND_ROW_ASYNC_START, method = RequestMethod.POST)
	public @ResponseBody
	AsyncJobId startAppendRowsJob(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String id, @RequestBody AppendableRowSetRequest request)
			throws DatastoreException, NotFoundException, IOException {
		ValidateArgument.required(id, "{id}");
		ValidateArgument.required(request, "AppendableRowSetRequest");
		request.setEntityId(id);
		// wrap the job as a transaction
		TableUpdateTransactionRequest trasnactionRequest = TableModelUtils.wrapInTransactionRequest(request);
		AsynchronousJobStatus job = serviceProvider
				.getAsynchronousJobServices().startJob(userId, trasnactionRequest);
		AsyncJobId asyncJobId = new AsyncJobId();
		asyncJobId.setToken(job.getJobId());
		return asyncJobId;
	}

	/**
	 * Asynchronously get the results of a PartialRowSet update to a table
	 * started with <a href="${POST.entity.id.table.transaction.async.start}">POST
	 * /entity/{id}/table/transaction/async/start</a>
	 * <p>
	 * Note: When the result is not ready yet, this method will return a status
	 * code of 202 (ACCEPTED) and the response body will be a <a
	 * href="${org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus}"
	 * >AsynchronousJobStatus</a> object.
	 * </p>
	 * 
	 * @param userId
	 * @param asyncToken
	 * @return
	 * @throws NotReadyException
	 * @throws NotFoundException
	 * @throws AsynchJobFailedException
	 */
	@Deprecated
	@RequiredScope({view})
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = UrlHelpers.ENTITY_TABLE_APPEND_ROW_ASYNC_GET, method = RequestMethod.GET)
	public @ResponseBody
	RowReferenceSetResults getAppendRowsResult(@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String id, @PathVariable String asyncToken) throws Throwable {
		if (id == null)
			throw new IllegalArgumentException("{id} cannot be null");
		AsynchronousJobStatus jobStatus = serviceProvider
				.getAsynchronousJobServices().getJobStatusAndThrow(userId,
						asyncToken);
		return TableModelUtils.extractResponseFromTransaction(jobStatus.getResponseBody(), RowReferenceSetResults.class) ;
	}

	@Deprecated
	@RequiredScope({view,modify})
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = UrlHelpers.ENTITY_TABLE_DELETE_ROWS, method = RequestMethod.POST)
	public @ResponseBody
	RowReferenceSet deleteRows(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String id, @RequestBody RowSelection rowsToDelete)
			throws DatastoreException, NotFoundException, IOException {
		if (id == null)
			throw new IllegalArgumentException("{id} cannot be null");
		rowsToDelete.setTableId(id);
		return serviceProvider.getTableServices().deleteRows(userId,
				rowsToDelete);
	}

	/**
	 * <p>
	 * This method is used to get file handle information for rows in a
	 * TableEntity. The columns in the passed in RowReferenceSet need to be
	 * FILEHANDLEID columns and the rows in the passed in RowReferenceSet need
	 * to exists (a 400 will be returned if a row ID is provided that does not
	 * actually exist). The order of the returned rows of file handles is the
	 * same as the order of the rows requested, and the order of the file
	 * handles in each row is the same as the order of the columns requested.
	 * </p>
	 * <p>
	 * Note: The caller must have the <a
	 * href="${org.sagebionetworks.repo.model.ACCESS_TYPE}"
	 * >ACCESS_TYPE.READ</a> permission on the TableEntity to make this call.
	 * </p>
	 * <p>
	 * <b>Service Limits</b>
	 * <table border="1">
	 * <tr>
	 * <th>resource</th>
	 * <th>limit</th>
	 * </tr>
	 * <tr>
	 * <td>The maximum frequency this method can be called</td>
	 * <td>1 calls per second</td>
	 * </tr>
	 * </table>
	 * </p>
	 * @param userId
	 * @param id
	 *            The ID of the TableEntity to append rows to.
	 * @param rows
	 *            The set of rows and columns for which to return the file
	 *            handles.
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws IOException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequiredScope({view})
	@RequestMapping(value = UrlHelpers.ENTITY_TABLE_FILE_HANDLES, method = RequestMethod.POST)
	public @ResponseBody
	TableFileHandleResults getFileHandles(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String id,
			@RequestBody RowReferenceSet fileHandlesToFind)
			throws DatastoreException, NotFoundException, IOException {
		if (id == null)
			throw new IllegalArgumentException("{id} cannot be null");
		fileHandlesToFind.setTableId(id);
		return serviceProvider.getTableServices().getFileHandles(userId,
				fileHandlesToFind);
	}

	/**
	 * Get the actual URL of the file associated with a specific version of a
	 * row and file handle column.
	 * <p>
	 * Note: This call will result in a HTTP temporary redirect (307), to the
	 * actual file URL if the caller meets all of the download requirements.
	 * </p>
	 * 
	 * @param userId
	 * @param id
	 *            The ID of the FileEntity to get.
	 * @param columnId
	 * @param rowId
	 * @param versionNumber
	 * @param redirect
	 *            When set to false, the URL will be returned as text/plain
	 *            instead of redirecting.
	 * @param response
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws IOException
	 */
	@RequiredScope({download})
	@RequestMapping(value = UrlHelpers.ENTITY_TABLE_FILE, method = RequestMethod.GET)
	public void fileRedirectURLForRow(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String id, @PathVariable String columnId,
			@PathVariable Long rowId, @PathVariable Long versionNumber,
			@RequestParam(required = false) Boolean redirect,
			HttpServletResponse response) throws DatastoreException,
			NotFoundException, IOException {
		// Get the redirect url
		RowReference ref = new RowReference();
		ref.setRowId(rowId);
		ref.setVersionNumber(versionNumber);
		String redirectUrl = serviceProvider.getTableServices()
				.getFileRedirectURL(userId, id, ref, columnId);
		RedirectUtils.handleRedirect(redirect, redirectUrl, response);
	}

	/**
	 * Get the preview URL of the file associated with a specific version of a
	 * row and file handle column.
	 * <p>
	 * Note: This call will result in a HTTP temporary redirect (307), to the
	 * actual file URL if the caller meets all of the download requirements.
	 * </p>
	 * 
	 * @param userId
	 * @param id
	 *            The ID of the FileEntity to get.
	 * @param columnId
	 * @param rowId
	 * @param versionNumber
	 * @param redirect
	 *            When set to false, the URL will be returned as text/plain
	 *            instead of redirecting.
	 * @param response
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws IOException
	 */
	@RequiredScope({download})
	@RequestMapping(value = UrlHelpers.ENTITY_TABLE_FILE_PREVIEW, method = RequestMethod.GET)
	public void filePreviewRedirectURLForRow(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String id, @PathVariable String columnId,
			@PathVariable Long rowId, @PathVariable Long versionNumber,
			@RequestParam(required = false) Boolean redirect,
			HttpServletResponse response) throws DatastoreException,
			NotFoundException, IOException {
		// Get the redirect url
		RowReference ref = new RowReference();
		ref.setRowId(rowId);
		ref.setVersionNumber(versionNumber);
		String redirectUrl = serviceProvider.getTableServices()
				.getFilePreviewRedirectURL(userId, id, ref, columnId);
		RedirectUtils.handleRedirect(redirect, redirectUrl, response);
	}

	/**
	 * Asynchronously start a query. Use the returned job id and <a
	 * href="${GET.entity.id.table.query.async.get.asyncToken}">GET
	 * /entity/{id}/table/query/async/get</a> to get the results of the query
	 * <p>
	 * Using a 'SQL like' syntax, query the current version of the rows in a
	 * single table. The following pseudo-syntax is the basic supported format:
	 * </p>
	 * SELECT <br>
	 * [ALL | DISTINCT] select_expr [, select_expr ...] <br>
	 * FROM synapse_table_id <br>
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
	 * table. When the index is out-of-synch, then a status code of 202
	 * (ACCEPTED) will be returned and the response body will be a <a
	 * href="${org.sagebionetworks.repo.model.table.TableStatus}"
	 * >TableStatus</a> object. The TableStatus will indicates the current
	 * status of the index including how much work is remaining until the index
	 * is consistent with the truth of the table.
	 * </p>
	 * <p>
	 * The 'partsMask' is an integer "mask" that can be combined into to request
	 * any desired part. As of this writing, the mask is defined as follows (see <a href="${org.sagebionetworks.repo.model.table.QueryBundleRequest}">QueryBundleRequest</a>):
	 * <ul>
	 * <li>Query Results <i>(queryResults)</i> = 0x1</li>
	 * <li>Query Count <i>(queryCount)</i> = 0x2</li>
	 * <li>Select Columns <i>(selectColumns)</i> = 0x4</li>
	 * <li>Max Rows Per Page <i>(maxRowsPerPage)</i> = 0x8</li>
	 * <li>The Table Columns <i>(columnModels)</i> = 0x10</li>
	 * <li>Facet statistics for each faceted column <i>(facetStatistics)</i> = 0x20</li>
	 * <li>The sum of the file sizes <i>(sumFileSizesBytes)</i> = 0x40</li>
	 * </ul>
	 * </p>
	 * <p>
	 * For example, to request all parts, the request mask value should be: <br>
	 * 0x1 OR 0x2 OR 0x4 OR 0x8 OR 0x10 OR 0x20 OR 0x40 = 0x7F.
	 * </p>
	 * <p>
	 * Note: The caller must have the <a
	 * href="${org.sagebionetworks.repo.model.ACCESS_TYPE}"
	 * >ACCESS_TYPE.READ</a> permission on the TableEntity to make this call.
	 * </p>
	 * 
	 * @param userId
	 * @param id
	 *            The ID of the TableEntity.
	 * @param query
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
	 * @throws TableUnavailableException
	 * @throws TableFailedException
	 */
	@RequiredScope({view,download})
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = UrlHelpers.ENTITY_TABLE_QUERY_ASYNC_START, method = RequestMethod.POST)
	public @ResponseBody
	AsyncJobId queryAsyncStart(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String id, @RequestBody QueryBundleRequest query)
			throws DatastoreException, NotFoundException, IOException {
		if (id == null)
			throw new IllegalArgumentException("{id} cannot be null");
		AsynchronousJobStatus job = serviceProvider
				.getAsynchronousJobServices().startJob(userId, query);
		AsyncJobId asyncJobId = new AsyncJobId();
		asyncJobId.setToken(job.getJobId());
		return asyncJobId;
	}

	/**
	 * Asynchronously get the results of a query started with <a
	 * href="${POST.entity.id.table.query.async.start}">POST /entity/{id}/table/query/async/start</a>.
	 * 
	 * <p>
	 * Note: When the result is not ready yet, this method will return a status
	 * code of 202 (ACCEPTED) and the response body will be a <a
	 * href="${org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus}"
	 * >AsynchronousJobStatus</a> object.
	 * </p>
	 * 
	 * @param userId
	 * @param id
	 *            The ID of the TableEntity.
	 * @param asyncToken
	 * @return
	 * @throws NotReadyException
	 *             when the result is not ready yet
	 * @throws NotFoundException
	 * @throws AsynchJobFailedException
	 *             when the asynchronous job failed
	 */
	@RequiredScope({view,download})
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = UrlHelpers.ENTITY_TABLE_QUERY_ASYNC_GET, method = RequestMethod.GET)
	public @ResponseBody
	QueryResultBundle queryAsyncGet(@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId, @PathVariable String id,
			@PathVariable String asyncToken) throws Throwable {
		if (id == null)
			throw new IllegalArgumentException("{id} cannot be null");
		AsynchronousJobStatus jobStatus = serviceProvider
				.getAsynchronousJobServices().getJobStatusAndThrow(userId,
						asyncToken);
		return (QueryResultBundle) jobStatus.getResponseBody();
	}

	/**
	 * Asynchronously get a next page of a query. Use the returned job id and <a
	 * href="${POST.entity.id.table.query.nextPage.async.start}">POST
	 * /entity/{id}/table/query/nextPage/async/start</a> to get the results of the query.
	 * The page token comes from the query result of a <a
	 * href="${GET.entity.id.table.query.async.get.asyncToken}">GET
	 * /entity/{id}/table/query/async/get</a>.
	 * 
	 * @param userId
	 * @param id
	 *            The ID of the TableEntity.
	 * @param nextPageToken
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws IOException
	 */
	@Deprecated
	@RequiredScope({view,download})
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = UrlHelpers.ENTITY_TABLE_QUERY_NEXT_PAGE_ASYNC_START, method = RequestMethod.POST)
	public @ResponseBody
	AsyncJobId queryNextPageAsyncStart(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String id, @RequestBody QueryNextPageToken nextPageToken)
			throws DatastoreException, NotFoundException, IOException {
		if (id == null)
			throw new IllegalArgumentException("{id} cannot be null");
		AsynchronousJobStatus job = serviceProvider
				.getAsynchronousJobServices().startJob(userId, nextPageToken);
		AsyncJobId asyncJobId = new AsyncJobId();
		asyncJobId.setToken(job.getJobId());
		return asyncJobId;
	}

	/**
	 * Asynchronously get the results of a nextPage query started with <a
	 * href="${POST.entity.id.table.query.nextPage.async.start}">POST
	 * /entity/{id}/table/query/nextPage/async/start</a>
	 * 
	 * <p>
	 * Note: When the result is not ready yet, this method will return a status
	 * code of 202 (ACCEPTED) and the response body will be a <a
	 * href="${org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus}"
	 * >AsynchronousJobStatus</a> object.
	 * </p>
	 * 
	 * @param userId
	 * @param id
	 *            The ID of the TableEntity.
	 * @param asyncToken
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws IOException
	 * @throws AsynchJobFailedException
	 * @throws NotReadyException
	 */
	@Deprecated
	@RequiredScope({view,download})
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = UrlHelpers.ENTITY_TABLE_QUERY_NEXT_PAGE_ASYNC_GET, method = RequestMethod.GET)
	public @ResponseBody
	QueryResult queryNextPageAsyncGet(@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId, @PathVariable String id,
			@PathVariable String asyncToken) throws Throwable {
		if (id == null)
			throw new IllegalArgumentException("{id} cannot be null");
		AsynchronousJobStatus jobStatus = serviceProvider
				.getAsynchronousJobServices().getJobStatusAndThrow(userId,
						asyncToken);
		return (QueryResult) jobStatus.getResponseBody();
	}

	@Deprecated
	@RequiredScope({view,download})
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = UrlHelpers.TABLE_DOWNLOAD_CSV_ASYNC_START, method = RequestMethod.POST)
	public @ResponseBody
	AsyncJobId csvDownloadAsyncStart(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestBody DownloadFromTableRequest downloadRequest)
			throws DeprecatedServiceException {
		throw new DeprecatedServiceException("Please update your client to use the new API.");
	}

	@Deprecated
	@RequiredScope({view,download})
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = UrlHelpers.TABLE_DOWNLOAD_CSV_ASYNC_GET, method = RequestMethod.GET)
	public @ResponseBody
	DownloadFromTableResult csvDownloadAsyncGet(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String asyncToken)
			throws DeprecatedServiceException {
		throw new DeprecatedServiceException("Please update your client to use the new API.");
	}

	/**
	 * Asynchronously start a csv download. Use the returned job id and <a
	 * href="${GET.entity.id.table.download.csv.async.get.asyncToken}">GET
	 * /entity/{id}/table/download/csv/async/get</a> to get the results of the query
	 * 
	 * @param userId
	 * @param id
	 *            The ID of the TableEntity.
	 * @param downloadRequest
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws IOException
	 */
	@RequiredScope({view,download})
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = UrlHelpers.ENTITY_TABLE_DOWNLOAD_CSV_ASYNC_START, method = RequestMethod.POST)
	public @ResponseBody
	AsyncJobId csvDownloadAsyncStart(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String id, @RequestBody DownloadFromTableRequest downloadRequest)
			throws DatastoreException, NotFoundException, IOException {
		if (id == null)
			throw new IllegalArgumentException("{id} cannot be null");
		AsynchronousJobStatus job = serviceProvider
				.getAsynchronousJobServices().startJob(userId, downloadRequest);
		AsyncJobId asyncJobId = new AsyncJobId();
		asyncJobId.setToken(job.getJobId());
		return asyncJobId;
	}

	/**
	 * Asynchronously get the results of a csv download started with <a
	 * href="${POST.entity.id.table.download.csv.async.start}">POST
	 * /entity/{id}/table/download/csv/async/start</a>
	 * 
	 * <p>
	 * Note: When the result is not ready yet, this method will return a status
	 * code of 202 (ACCEPTED) and the response body will be a <a
	 * href="${org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus}"
	 * >AsynchronousJobStatus</a> object.
	 * </p>
	 * 
	 * @param userId
	 * @param id
	 *            The ID of the TableEntity.
	 * @param asyncToken
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws IOException
	 * @throws AsynchJobFailedException
	 * @throws NotReadyException
	 */
	@RequiredScope({view,download})
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = UrlHelpers.ENTITY_TABLE_DOWNLOAD_CSV_ASYNC_GET, method = RequestMethod.GET)
	public @ResponseBody
	DownloadFromTableResult csvDownloadAsyncGet(@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String id, @PathVariable String asyncToken) throws Throwable {
		if (id == null)
			throw new IllegalArgumentException("{id} cannot be null");
		AsynchronousJobStatus jobStatus = serviceProvider
				.getAsynchronousJobServices().getJobStatusAndThrow(userId,
						asyncToken);
		return (DownloadFromTableResult) jobStatus.getResponseBody();
	}

	/**
	 * <p>
	 * The method can be used to test both the parameters for reading an upload
	 * CSV file and the required table schema. The caller can then adjust both
	 * parameters and schema before applying the CSV to that table.
	 * </p>
	 * Asynchronously start a csv upload preview. Use the returned job id and <a
	 * href="${GET.table.upload.csv.preview.async.get.asyncToken}">GET
	 * /table/upload/csv/preview/async/get/{asyncToken}</a> to get the results.
	 * 
	 * @param userId
	 * @param uploadRequest
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws IOException
	 */
	@RequiredScope({view,modify})
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = UrlHelpers.TABLE_UPLOAD_CSV_PREVIEW_ASYNC_START, method = RequestMethod.POST)
	public @ResponseBody
	AsyncJobId csvUploadPreviewAsyncStart(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestBody UploadToTablePreviewRequest uploadRequest)
			throws DatastoreException, NotFoundException, IOException {
		AsynchronousJobStatus job = serviceProvider
				.getAsynchronousJobServices().startJob(userId, uploadRequest);
		AsyncJobId asyncJobId = new AsyncJobId();
		asyncJobId.setToken(job.getJobId());
		return asyncJobId;
	}	

	/**
	 * Asynchronously get the results of a csv upload preview started with <a
	 * href="${POST.table.upload.csv.preview.async.start}">POST
	 * /table/upload/csv/async/start</a>
	 * 
	 * <p>
	 * Note: When the result is not ready yet, this method will return a status
	 * code of 202 (ACCEPTED) and the response body will be a <a
	 * href="${org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus}"
	 * >AsynchronousJobStatus</a> object.
	 * </p>
	 * 
	 * @param userId
	 * @param asyncToken
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws IOException
	 * @throws AsynchJobFailedException
	 * @throws NotReadyException
	 */
	@RequiredScope({view})
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = UrlHelpers.TABLE_UPLOAD_CSV_PREVIEW_ASYNC_GET, method = RequestMethod.GET)
	public @ResponseBody
	UploadToTablePreviewResult csvUploadPreviewAsyncGet(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String asyncToken) throws Throwable {
		AsynchronousJobStatus jobStatus = serviceProvider
				.getAsynchronousJobServices().getJobStatusAndThrow(userId,
						asyncToken);
		return (UploadToTablePreviewResult) jobStatus.getResponseBody();
	}

	@Deprecated
	@RequiredScope({view,modify})
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = UrlHelpers.TABLE_UPLOAD_CSV_ASYNC_START, method = RequestMethod.POST)
	public @ResponseBody
	AsyncJobId csvUploadAsyncStart(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestBody UploadToTableRequest uploadRequest)
			throws DeprecatedServiceException {
		throw new DeprecatedServiceException("Please update your client to use the new API.");
	}

	@Deprecated
	@RequiredScope({view})
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = UrlHelpers.TABLE_UPLOAD_CSV_ASYNC_GET, method = RequestMethod.GET)
	public @ResponseBody
	UploadToTableResult csvUploadAsyncGet(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String asyncToken)
			throws DeprecatedServiceException {
		throw new DeprecatedServiceException("Please update your client to use the new API.");
	}

	/**
	 * <p>
	 * Asynchronously start a csv upload. Use the returned job id and
	 * <a href="${GET.entity.id.table.upload.csv.async.get.asyncToken}">GET
	 * /entity/{id}/table/upload/csv/async/get</a> to get the results of the query
	 * </p>
	 * <p>
	 * <b>Service Limits</b>
	 * <table border="1">
	 * <tr>
	 * <th>resource</th>
	 * <th>limit</th>
	 * </tr>
	 * <tr>
	 * <td>The maximum size of a CSV that can be appended to a table</td>
	 * <td>1 GB</td>
	 * </tr>
	 * </table>
	 * </p>
	 * 
	 * @param userId
	 * @param id            The ID of the TableEntity.
	 * @param uploadRequest
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws IOException
	 */
	@ResponseStatus(HttpStatus.CREATED)
	@RequiredScope({view,modify})
	@RequestMapping(value = UrlHelpers.ENTITY_TABLE_UPLOAD_CSV_ASYNC_START, method = RequestMethod.POST)
	public @ResponseBody
	AsyncJobId csvUploadAsyncStart(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String id, @RequestBody UploadToTableRequest uploadRequest)
			throws DatastoreException, NotFoundException, IOException {
		ValidateArgument.required(id, "{id}");
		ValidateArgument.required(uploadRequest, "UploadToTableRequest");
		uploadRequest.setEntityId(id);
		// wrap the job as a transaction
		TableUpdateTransactionRequest request = TableModelUtils.wrapInTransactionRequest(uploadRequest);
		AsynchronousJobStatus job = serviceProvider
				.getAsynchronousJobServices().startJob(userId, request);
		AsyncJobId asyncJobId = new AsyncJobId();
		asyncJobId.setToken(job.getJobId());
		return asyncJobId;
	}

	/**
	 * Asynchronously get the results of a csv upload started with <a
	 * href="${POST.entity.id.table.upload.csv.async.start}">POST
	 * /entity/{id}/table/upload/csv/async/start</a>
	 * 
	 * <p>
	 * Note: When the result is not ready yet, this method will return a status
	 * code of 202 (ACCEPTED) and the response body will be a <a
	 * href="${org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus}"
	 * >AsynchronousJobStatus</a> object.
	 * </p>
	 * 
	 * @param userId
	 * @param id
	 *            The ID of the TableEntity.
	 * @param asyncToken
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws IOException
	 * @throws AsynchJobFailedException
	 * @throws NotReadyException
	 */
	@ResponseStatus(HttpStatus.CREATED)
	@RequiredScope({view})
	@RequestMapping(value = UrlHelpers.ENTITY_TABLE_UPLOAD_CSV_ASYNC_GET, method = RequestMethod.GET)
	public @ResponseBody
	UploadToTableResult csvUploadAsyncGet(@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId, @PathVariable String id,
			@PathVariable String asyncToken) throws Throwable {
		if (id == null)
			throw new IllegalArgumentException("{id} cannot be null");
		AsynchronousJobStatus jobStatus = serviceProvider
				.getAsynchronousJobServices().getJobStatusAndThrow(userId,
						asyncToken);
		// This job is wrapped in a transaction.
		return TableModelUtils.extractResponseFromTransaction(jobStatus.getResponseBody(), UploadToTableResult.class);
	}
	
	/**
	 * Get the possible ColumnModel definitions based on annotation within a
	 * given scope.
	 * 
	 * @param viewScope
	 *            List of parent IDs that define the scope.
	 * @param nextPageToken
	 *            Optional: When the results include a next page token, the
	 *            token can be provided to get subsequent pages.
	 * 
	 * @return A ColumnModel for each distinct annotation for the given scope. A returned nextPageToken can be used to get subsequent pages
	 * of ColumnModels for the given scope.  The nextPageToken will be null when there are no more pages of results.
	 * 
	 */
	@ResponseStatus(HttpStatus.CREATED)
	@RequiredScope({view})
	@RequestMapping(value = UrlHelpers.TABLE_COLUMNS_OF_SCOPE, method = RequestMethod.POST)
	@Deprecated
	public @ResponseBody
	ColumnModelPage getPossibleColumnModelsForView(
			@RequestBody ViewScope viewScope,
			@RequestParam(value = UrlHelpers.NEXT_PAGE_TOKEN_PARAM, required = false) String nextPageToken) {
		ValidateArgument.required(viewScope, "viewScope");
		return serviceProvider.getTableServices()
				.getPossibleColumnModelsForScopeIds(viewScope,
						nextPageToken);
	}
	
	/**
	 * Starts an asynchronous job to compute a page of the possible <a href="${org.sagebionetworks.repo.model.table.ColumnModel}">ColumnModels</a>
	 * based on the annotations within the provided scope. The result of the job can be fetched using the
	 * <a href="${GET.column.view.scope.async.get.asyncToken}">GET /column/view/scope/async/get</a> service with the job token returned by this request.
	 * 
	 * @param request The request specifies the scope to compute the model against as well as the optional nextPageToken used to fetch subsequent pages
	 * @return An object containing the id of the asynchronous job whose results can be fetched using the 
	 * <a href="${GET.column.view.scope.async.get.asyncToken}">GET /column/view/scope/async/get</a> service
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws IOException
	 */
	@RequiredScope({view})
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = UrlHelpers.VIEW_COLUMNS_FROM_SCOPE_ASYNC_START, method = RequestMethod.POST)
	public @ResponseBody AsyncJobId getViewScopeColumnsAsyncStart(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId, @RequestBody ViewColumnModelRequest request)
			throws DatastoreException, NotFoundException, IOException {
		AsynchronousJobStatus job = serviceProvider .getAsynchronousJobServices().startJob(userId, request);
		AsyncJobId asyncJobId = new AsyncJobId();
		asyncJobId.setToken(job.getJobId());
		return asyncJobId;
	}
	
	/**
	 * Fetches the result of the <a href="${POST.column.view.scope.async.start}">POST /column/view/scope/async/start</a> service that'll contain
	 * a page of possible <a href="${org.sagebionetworks.repo.model.table.ColumnModel}">ColumnModels</a> within the scope supplied in the original request.
	 * 
	 * <p>
	 * Note: When the result is not ready yet, this method will return a status
	 * code of 202 (ACCEPTED) and the response body will be a <a
	 * href="${org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus}"
	 * >AsynchronousJobStatus</a> object.
	 * </p>
	 * @param asyncToken
	 * @return
	 * @throws Throwable
	 */
	@RequiredScope({view})
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = UrlHelpers.VIEW_COLUMNS_FROM_SCOPE_ASYNC_GET, method = RequestMethod.GET)
	public @ResponseBody
	ViewColumnModelResponse getViewScopeColumnsAsyncGet(@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId, @PathVariable String asyncToken) throws Throwable {
		AsynchronousJobStatus jobStatus = serviceProvider.getAsynchronousJobServices().getJobStatusAndThrow(userId, asyncToken);
		return (ViewColumnModelResponse) jobStatus.getResponseBody();
	}

	/**
	 * Request to transform the provided SQL based on the request parameters. For
	 * example, a <a href=
	 * "${org.sagebionetworks.repo.model.table.TransformSqlWithFacetsRequest}"
	 * >TransformSqlWithFacetsRequest</a> can be used to alter the where clause
	 * of the provided SQL based on the provided selected facets.
	 * 
	 * @param request
	 * @return
	 * @throws ParseException 
	 */
	@ResponseStatus(HttpStatus.CREATED)
	@RequiredScope({})
	@RequestMapping(value = UrlHelpers.TABLE_SQL_TRANSFORM, method = RequestMethod.POST)
	public @ResponseBody SqlTransformResponse transformSqlRequest(@RequestBody SqlTransformRequest request) throws ParseException {
		return serviceProvider.getTableServices().transformSqlRequest(request);
	}
	
	/**
	 * Request to create a new snapshot of a table. The provided comment, label, and
	 * activity ID will be applied to the current version thereby creating a
	 * snapshot and locking the current version. After the snapshot is created a new
	 * version will be started with an 'in-progress' label.
	 * <p>
	 * NOTE: This service is for
	 * <a href= "${org.sagebionetworks.repo.model.table.TableEntity}"
	 * >TableEntities</a> only. Snapshots of
	 * <a href= "${org.sagebionetworks.repo.model.table.EntityView}"
	 * >EntityViews</a> require asynchronous processing and can be created via:
	 * <a href="${POST.entity.id.table.transaction.async.start}">POST
	 * /entity/{id}/table/transaction/async/start</a>
	 * </p>
	 * 
	 * @param userId
	 * @param id
	 * @param request
	 * @return
	 */
	@ResponseStatus(HttpStatus.CREATED)
	@RequiredScope({view,modify})
	@RequestMapping(value = UrlHelpers.TABLE_SNAPSHOT, method = RequestMethod.POST)
	public @ResponseBody SnapshotResponse createSnapshot(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId, @PathVariable String id,
			@RequestBody SnapshotRequest request) {
		return serviceProvider.getTableServices().createTableSnapshot(userId, id, request);
	}
}
