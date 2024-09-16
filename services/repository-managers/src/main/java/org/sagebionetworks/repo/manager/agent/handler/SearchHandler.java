package org.sagebionetworks.repo.manager.agent.handler;

import java.util.List;

import org.sagebionetworks.repo.manager.agent.parameter.ParameterUtils;
import org.sagebionetworks.repo.model.search.SearchResults;
import org.sagebionetworks.repo.model.search.query.SearchQuery;
import org.sagebionetworks.repo.service.SearchService;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SearchHandler implements ReturnControlHandler {
	
	private static final int MAX_NUM_CHARS = 200;
	private final SearchService serchService;
	
	@Autowired
	public SearchHandler(SearchService serchService) {
		super();
		this.serchService = serchService;
	}

	@Override
	public String getActionGroup() {
		return "org_sage_zero";
	}

	@Override
	public String getFunction() {
		return "org_sage_zero_search";
	}

	@Override
	public String handleEvent(ReturnControlEvent event) throws Exception {
		String term = ParameterUtils.extractParameter(String.class, "term", event.getParameters())
				.orElseThrow(() -> new IllegalArgumentException("Parameter 'term' of type string is required"));
		SearchResults results = this.serchService.proxySearch(event.getRunAsUserId(), new SearchQuery().setQueryTerm(List.of(term)));
		results.getHits().forEach(h->{
			if(h.getDescription() != null && h.getDescription().length() > MAX_NUM_CHARS) {
				h.setDescription(String.format("%s --truncated--", h.getDescription().subSequence(0, MAX_NUM_CHARS)));
			}
		});	
		return EntityFactory.createJSONStringForEntity(results);
	}

	@Override
	public boolean needsWriteAccess() {
		return false;
	}

}
