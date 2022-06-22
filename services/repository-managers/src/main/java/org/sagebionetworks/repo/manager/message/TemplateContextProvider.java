package org.sagebionetworks.repo.manager.message;

import java.util.Map;

@FunctionalInterface
public interface TemplateContextProvider {

	/**
	 * The provided context map will be used to replace each variable in the
	 * template body text with the mapped value.
	 * 
	 * @param userNameProvider
	 * @return
	 */
	Map<String, Object> getTemplateContext(UserNameProvider userNameProvider);
}
