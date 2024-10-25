package org.sagebionetworks.repo.manager.agent.handler;

import java.util.List;
import java.util.stream.Collectors;

import org.sagebionetworks.repo.manager.agent.parameter.ParameterUtils;
import org.sagebionetworks.repo.model.EntityChildrenRequest;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.entity.Direction;
import org.sagebionetworks.repo.model.entity.SortBy;
import org.sagebionetworks.repo.service.EntityService;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class GetEntityChildrenHandler implements ReturnControlHandler {

	private final EntityService entityService;

	@Autowired
	public GetEntityChildrenHandler(EntityService entityService) {
		super();
		this.entityService = entityService;
	}

	@Override
	public String getActionGroup() {
		return "org_sage_zero";
	}

	@Override
	public String getFunction() {
		return "org_sage_zero_get_entity_children";
	}

	@Override
	public boolean needsWriteAccess() {
		return false;
	}

	@Override
	public String handleEvent(ReturnControlEvent event) throws Exception {

		String synId = ParameterUtils.extractParameter(String.class, "synId", event.getParameters())
				.orElseThrow(() -> new IllegalArgumentException("Parameter 'synId' of type string is required"));

		String nextPageToken = ParameterUtils.extractParameter(String.class, "nextPageToken", event.getParameters())
				.orElse(null);

		List<EntityType> entityTypes = (List<EntityType>) ParameterUtils.extractParameter(List.class, "entityType", event.getParameters())
				.orElseGet(() -> List.of(EntityType.values())).stream()
				.map(e -> EntityType.valueOf(e.toString()))
				.collect(Collectors.toList());

		return EntityFactory.createJSONStringForEntity(entityService.getChildren(event.getRunAsUserId(),
				new EntityChildrenRequest().setParentId(synId).setIncludeSumFileSizes(true)
						.setIncludeTotalChildCount(true).setSortBy(SortBy.MODIFIED_ON).setSortDirection(Direction.DESC)
						.setIncludeTypes(entityTypes).setNextPageToken(nextPageToken)));
	}
}
