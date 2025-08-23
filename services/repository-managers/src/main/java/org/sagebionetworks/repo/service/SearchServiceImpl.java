package org.sagebionetworks.repo.service;

import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.search.SearchManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.search.SearchResults;
import org.sagebionetworks.repo.model.search.query.SearchQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * CloudSearch search service.
 * @author deflaux
 * 
 */
@Service
public class SearchServiceImpl implements SearchService {
	@Autowired
	SearchManager searchManager;

	@Autowired
	org.sagebionetworks.repo.manager.search.oss.SearchManager ossSearchManager;

	@Autowired
	UserManager userManager;


	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.web.service.SearchService#proxySearch(java.lang.String, java.lang.String, javax.servlet.http.HttpServletRequest)
	 */
	@Override
	public @ResponseBody
	SearchResults proxySearch(Long userId, boolean isOpenSearchEnable, SearchQuery searchQuery) {
		UserInfo userInfo = userManager.getUserInfo(userId);

		if (isOpenSearchEnable) {
			return ossSearchManager.search(userInfo, searchQuery);
		}

		return searchManager.proxySearch(userInfo, searchQuery);
	}



}
