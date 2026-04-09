package org.sagebionetworks.repo.manager.agent.handler.grid;

import java.util.Optional;

import org.sagebionetworks.repo.manager.agent.handler.HttpCode;
import org.sagebionetworks.repo.manager.agent.handler.HttpMethod;
import org.sagebionetworks.repo.manager.agent.handler.OpenApiReturnControlHandler;
import org.sagebionetworks.repo.manager.agent.handler.ReturnControlEvent;
import org.sagebionetworks.repo.manager.schema.JsonSchemaManager;
import org.sagebionetworks.repo.model.agent.GridAgentSessionContext;
import org.sagebionetworks.repo.model.dbo.grid.GridDao;
import org.sagebionetworks.repo.model.grid.GridSession;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.JsonEntityUtils;
import org.springframework.stereotype.Service;

@Service
public class GetGridSchemaHandler implements OpenApiReturnControlHandler {

	private final JsonSchemaManager jsonSchemaManager;
	private final GridDao gridDao;

	public GetGridSchemaHandler(JsonSchemaManager jsonSchemaManager, GridDao gridDao) {
		super();
		this.jsonSchemaManager = jsonSchemaManager;
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

		GridAgentSessionContext context = event.getSessionContext(GridAgentSessionContext.class)
				.orElseThrow(() -> new IllegalArgumentException("GridAgentSessionContext cannot be null"));

		GridSession session = gridDao.getGridSession(context.getGridSessionId())
				.orElseThrow((() -> new NotFoundException("Grid session no longer exists")));
		if (session.getGridJsonSchema$Id() == null) {
			return "{}";
		}
		return JsonEntityUtils.toJsonString(jsonSchemaManager.getValidationSchema(session.getGridJsonSchema$Id()));
	}

	@Override
	public String getPath() {
		return "/repo/v1/grid/json-schema";
	}

	@Override
	public HttpMethod getHttpMethod() {
		return HttpMethod.get;
	}

	@Override
	public HttpCode getSuccessHttpCode() {
		return HttpCode.ok;
	}

}
