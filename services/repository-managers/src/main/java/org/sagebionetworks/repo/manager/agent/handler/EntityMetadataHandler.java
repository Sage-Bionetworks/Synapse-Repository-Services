package org.sagebionetworks.repo.manager.agent.handler;

import org.sagebionetworks.repo.manager.agent.parameter.ParameterUtils;
import org.sagebionetworks.repo.model.entitybundle.v2.EntityBundleRequest;
import org.sagebionetworks.repo.service.EntityBundleService;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.springframework.stereotype.Service;

@Service
public class EntityMetadataHandler implements ReturnControlHandler {

	private final EntityBundleService entityBundleService;

	public EntityMetadataHandler(EntityBundleService entityBundleService) {
		super();
		this.entityBundleService = entityBundleService;
	}

	@Override
	public String getActionGroup() {
		return "org_sage_zero";
	}

	@Override
	public String getFunction() {
		return "org_sage_zero_get_entity_metadata";
	}

	@Override
	public boolean needsWriteAccess() {
		return false;
	}

	@Override
	public String handleEvent(ReturnControlEvent event) throws Exception {
		String synId = ParameterUtils.extractParameter(String.class, "synId", event.getParameters())
				.orElseThrow(() -> new IllegalArgumentException("Parameter 'synId' of type string is required"));

		var results = entityBundleService.getEntityBundle(event.getRunAsUserId(), synId,
				new EntityBundleRequest().setIncludeAccessControlList(true).setIncludeEntity(true)
						.setIncludeAnnotations(true).setIncludeEntityPath(true).setIncludeHasChildren(true)
						.setIncludePermissions(true).setIncludeTableBundle(true));
		return EntityFactory.createJSONStringForEntity(results);
	}

}
