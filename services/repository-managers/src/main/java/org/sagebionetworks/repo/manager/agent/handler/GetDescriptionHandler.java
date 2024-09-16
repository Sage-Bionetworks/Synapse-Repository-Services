package org.sagebionetworks.repo.manager.agent.handler;

import org.sagebionetworks.repo.manager.agent.parameter.ParameterUtils;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.dao.WikiPageKey;
import org.sagebionetworks.repo.model.wiki.WikiPage;
import org.sagebionetworks.repo.service.WikiService;
import org.springframework.stereotype.Service;

import com.google.gson.JsonObject;

@Service
public class GetDescriptionHandler implements ReturnControlHandler {

	private final WikiService wikiService;

	public GetDescriptionHandler(WikiService wikiService) {
		super();
		this.wikiService = wikiService;
	}

	@Override
	public String getActionGroup() {
		return "org_sage_zero";
	}

	@Override
	public String getFunction() {
		return "org_sage_zero_get_description";
	}

	@Override
	public boolean needsWriteAccess() {
		return false;
	}

	@Override
	public String handleEvent(ReturnControlEvent event) throws Exception {
		String synId = ParameterUtils.extractParameter(String.class, "synId", event.getParameters())
				.orElseThrow(() -> new IllegalArgumentException("Parameter 'synId' of type string is required"));

		StringBuilder builder = new StringBuilder();
		var offset = 0L;
		var limit = 5L;
		var headers = wikiService.getWikiHeaderTree(event.getRunAsUserId(), synId, ObjectType.ENTITY, limit, offset)
				.getResults();
		if (headers != null) {
			for (var h : headers) {
				WikiPage wp = wikiService.getWikiPage(event.getRunAsUserId(), new WikiPageKey().setOwnerObjectId(synId)
						.setOwnerObjectType(ObjectType.ENTITY).setWikiPageId(h.getId()), null);
				if (wp.getTitle() != null) {
					builder.append(wp.getTitle());
					builder.append("\n");
				}
				if (wp.getMarkdown() != null) {
					builder.append(wp.getMarkdown());
					builder.append("\n");
				}
				builder.append("\n");
			}
		}
		JsonObject object = new JsonObject();
		object.addProperty("description", builder.toString());
		return object.toString();
	}

}
