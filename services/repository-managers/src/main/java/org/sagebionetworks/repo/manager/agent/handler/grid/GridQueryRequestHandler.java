package org.sagebionetworks.repo.manager.agent.handler.grid;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.repo.manager.agent.handler.HttpCode;
import org.sagebionetworks.repo.manager.agent.handler.HttpMethod;
import org.sagebionetworks.repo.manager.agent.handler.OpenApiReturnControlHandler;
import org.sagebionetworks.repo.manager.agent.handler.ReturnControlEvent;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.GridHeader;
import org.sagebionetworks.repo.manager.grid.internal.replica.view.GridReplicaViewManager;
import org.sagebionetworks.repo.manager.grid.internal.replica.view.query.QueryElement;
import org.sagebionetworks.repo.model.agent.GridAgentSessionContext;
import org.sagebionetworks.repo.model.dbo.grid.GridDao;
import org.sagebionetworks.repo.model.grid.EventSource;
import org.sagebionetworks.repo.model.grid.GridConnectionInfo;
import org.sagebionetworks.repo.model.grid.query.QueryRequest;
import org.sagebionetworks.repo.model.grid.query.result.QueryResult;
import org.sagebionetworks.repo.model.jdo.JDOSecondaryPropertyUtils;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.stereotype.Service;

@Service
public class GridQueryRequestHandler implements OpenApiReturnControlHandler {

	private static final Logger log = LogManager.getLogger(GridQueryRequestHandler.class);

	private final GridReplicaViewManager viewManager;
	private final GridDao gridDao;

	public GridQueryRequestHandler(GridReplicaViewManager viewManager, GridDao gridDao) {
		super();
		this.viewManager = viewManager;
		this.gridDao = gridDao;
	}

	@Override
	public String getActionGroup() {
		return "org_sage_grid_one";
	}

	@Override
	public boolean needsWriteAccess() {
		return false;
	}

	@Override
	public String handleEvent(ReturnControlEvent event) throws Exception {
		log.info("request body: {}", event.getRequestBody().get());
		QueryRequest request = JDOSecondaryPropertyUtils.createObjectFromJSON(QueryRequest.class,
				event.getRequestBody().get());
		ValidateArgument.required(request.getQuery(), "request.query");
		QueryElement element = new QueryElement(request.getQuery());

		GridAgentSessionContext context = event.getSessionContext(GridAgentSessionContext.class)
				.orElseThrow(() -> new IllegalArgumentException("GridAgentSessionContext cannot be null"));
		GridConnectionInfo connection = gridDao.getSingletonConnection(context.getGridSessionId(), EventSource.INTERNAL)
				.orElseThrow(() -> new IllegalArgumentException("Cannot get a grid connection."));

		GridHeader header = viewManager
				.readHeader(context.getGridSessionId(), connection.getReplicaId(), context.getUsersReplicaId())
				.orElseThrow(() -> new IllegalArgumentException("Grid session does not exist"));
		QueryResult results = viewManager.querySinglePageAsQueryResult(header, element);
		String json = JDOSecondaryPropertyUtils.createJSONFromObject(results);
		log.info("response JSON: {}", json);
		return json;
	}

	@Override
	public String getPath() {
		return "/repo/v1/grid/query";
	}

	@Override
	public HttpMethod getHttpMethod() {
		return HttpMethod.put;
	}

	@Override
	public HttpCode getSuccessHttpCode() {
		return HttpCode.ok;
	}

}
