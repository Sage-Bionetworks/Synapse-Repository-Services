package org.sagebionetworks.repo.manager.agent.handler.grid;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;
import org.sagebionetworks.repo.manager.agent.handler.HttpCode;
import org.sagebionetworks.repo.manager.agent.handler.HttpMethod;
import org.sagebionetworks.repo.manager.agent.handler.OpenApiReturnControlHandler;
import org.sagebionetworks.repo.manager.agent.handler.ReturnControlEvent;
import org.springframework.stereotype.Service;

@Service
public class GridFeatureRequestHandler implements OpenApiReturnControlHandler {
	
	private static final Logger log = LogManager.getLogger(GridFeatureRequestHandler.class);

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
		log.info("Event: "+event);
		event.getRequestBody().ifPresent(b->{
			JSONObject body = new JSONObject(b);
			log.info(body.toString(5));
		});
		return "";
	}

	@Override
	public String getPath() {
		return "/repo/v1/grid/feature/request";
	}

	@Override
	public HttpMethod getHttpMethod() {
		return HttpMethod.put;
	}

	@Override
	public HttpCode getSuccessHttpCode() {
		return HttpCode.no_content;
	}

}
