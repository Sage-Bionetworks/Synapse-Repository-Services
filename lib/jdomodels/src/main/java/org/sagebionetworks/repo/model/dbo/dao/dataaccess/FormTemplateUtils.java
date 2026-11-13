package org.sagebionetworks.repo.model.dbo.dao.dataaccess;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.repo.model.dataaccess.schema.FormTemplate;
import org.sagebionetworks.repo.model.jdo.JDOSecondaryPropertyUtils;

public class FormTemplateUtils {

	private static final Logger LOG = LogManager.getLogger(FormTemplateUtils.class);

	/**
	 * @return The attachments of the given template; a template without attachments has none.
	 */
	public static List<String> getAttachments(FormTemplate template) {
		return template.getAttachments() == null ? List.of() : template.getAttachments();
	}

	/**
	 * @param templateJson The serialized body of a template version, as stored in FORM_TEMPLATE_REVISION.TEMPLATE_JSON.
	 * @return The ids of the file handles listed in the attachments of the version. An id that is not a
	 *         number is skipped, so that a single bad version cannot fail a scan of all versions.
	 */
	public static Set<Long> extractAttachmentFileHandleIds(String templateJson) {
		Set<Long> fileHandleIds = new HashSet<>();
		FormTemplate template = JDOSecondaryPropertyUtils.createObjectFromJSON(FormTemplate.class, templateJson);
		if (template == null) {
			return fileHandleIds;
		}
		for (String attachment : getAttachments(template)) {
			try {
				fileHandleIds.add(Long.valueOf(attachment));
			} catch (NumberFormatException e) {
				LOG.warn("Malformed attachment file handle id {} in form template {}", attachment, template.getId());
			}
		}
		return fileHandleIds;
	}

}
