package org.sagebionetworks.repo.manager.agent.handler.grid;

import org.sagebionetworks.repo.manager.agent.handler.HttpCode;
import org.sagebionetworks.repo.manager.agent.handler.HttpMethod;
import org.sagebionetworks.repo.manager.agent.handler.OpenApiReturnControlHandler;
import org.sagebionetworks.repo.manager.agent.handler.ReturnControlEvent;
import org.sagebionetworks.repo.manager.grid.GridManager;
import org.sagebionetworks.repo.manager.schema.JsonSchemaManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.agent.GridAgentSessionContext;
import org.sagebionetworks.repo.model.grid.GridSession;
import org.sagebionetworks.util.JsonEntityUtils;
import org.springframework.stereotype.Service;

@Service
public class GetGridSchemaHandler implements OpenApiReturnControlHandler {

	private final GridManager gridManager;
	private final JsonSchemaManager jsonSchemaManager;

	public GetGridSchemaHandler(GridManager gridManager, JsonSchemaManager jsonSchemaManager) {
		super();
		this.gridManager = gridManager;
		this.jsonSchemaManager = jsonSchemaManager;
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

		// NOTE Here we do not need to know the user's 'realm'
		String userRealm = null;
		UserInfo userInfo = new UserInfo(false, event.getRunAsUserId(), userRealm);
		GridSession session = gridManager.getGridSession(userInfo, context.getGridSessionId());
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
