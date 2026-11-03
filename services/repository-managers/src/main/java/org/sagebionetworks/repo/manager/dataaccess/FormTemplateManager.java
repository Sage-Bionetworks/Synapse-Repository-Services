package org.sagebionetworks.repo.manager.dataaccess;

import java.util.List;

import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.NextPageToken;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dataaccess.schema.FormTemplate;
import org.sagebionetworks.repo.model.dataaccess.schema.FormTemplateSearchRequest;
import org.sagebionetworks.repo.model.dataaccess.schema.FormTemplateSearchResponse;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.FormTemplateDao;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.FormTemplateInfoForUpdate;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.stereotype.Service;

/**
 * Manages the templates that describe how a JSON Schema is presented as a data access request form.
 * Every version of a template is immutable, so each update publishes a new version and leaves the
 * access requirements that reference an earlier version untouched.
 */
@Service
public class FormTemplateManager {

	private final FormTemplateDao formTemplateDao;
	private final FormTemplateValidator formTemplateValidator;
	private final AuthorizationManager authorizationManager;

	public FormTemplateManager(FormTemplateDao formTemplateDao, FormTemplateValidator formTemplateValidator,
			AuthorizationManager authorizationManager) {
		this.formTemplateDao = formTemplateDao;
		this.formTemplateValidator = formTemplateValidator;
		this.authorizationManager = authorizationManager;
	}

	/**
	 * Create the first version of a new template. Only the ACT may create templates.
	 *
	 * @return The created template.
	 */
	@WriteTransaction
	public FormTemplate create(UserInfo userInfo, FormTemplate template) {
		ValidateArgument.required(userInfo, "userInfo");
		ValidateArgument.required(template, "template");
		verifyACTMember(userInfo);
		formTemplateValidator.validate(template);

		return formTemplateDao.create(userInfo.getId(), template);
	}

	/**
	 * Publish a new version of an existing template. Only the ACT may update templates.
	 *
	 * @param template The new body of the template, carrying the etag of the version it was derived from.
	 * @return The new version of the template.
	 * @throws ConflictingUpdateException If the template changed since the given etag was read.
	 */
	@WriteTransaction
	public FormTemplate createNewVersion(UserInfo userInfo, FormTemplate template) {
		ValidateArgument.required(userInfo, "userInfo");
		ValidateArgument.required(template, "template");
		ValidateArgument.requiredNotBlank(template.getId(), "template.id");
		ValidateArgument.requiredNotBlank(template.getEtag(), "template.etag");
		verifyACTMember(userInfo);
		formTemplateValidator.validate(template);

		Long id = Long.parseLong(template.getId());
		FormTemplateInfoForUpdate current = formTemplateDao.getForUpdate(id)
				.orElseThrow(() -> new NotFoundException("A form template with the id '" + id + "' does not exist."));
		if (!current.etag().equals(template.getEtag())) {
			throw new ConflictingUpdateException(
					"The form template was updated since you last fetched it, retrieve it again and reapply the update.");
		}

		return formTemplateDao.createNewVersion(userInfo.getId(),
				template.setVersionNumber(current.currentVersionNumber() + 1));
	}

	/**
	 * @return The latest version of the given template.
	 */
	public FormTemplate getLatestVersion(String id) {
		ValidateArgument.requiredNotBlank(id, "id");
		return formTemplateDao.getLatestVersion(Long.parseLong(id))
				.orElseThrow(() -> new NotFoundException("A form template with the id '" + id + "' does not exist."));
	}

	/**
	 * @return The given version of the given template.
	 */
	public FormTemplate getVersion(String id, Long versionNumber) {
		ValidateArgument.requiredNotBlank(id, "id");
		ValidateArgument.required(versionNumber, "versionNumber");
		return formTemplateDao.getVersion(Long.parseLong(id), versionNumber).orElseThrow(() -> new NotFoundException(
				"Version " + versionNumber + " of the form template with the id '" + id + "' does not exist."));
	}

	/**
	 * Search the latest version of each template. Deprecated templates are excluded unless the
	 * request asks for them.
	 */
	public FormTemplateSearchResponse search(FormTemplateSearchRequest request) {
		ValidateArgument.required(request, "request");

		NextPageToken nextPageToken = new NextPageToken(request.getNextPageToken());
		List<FormTemplate> page = formTemplateDao.searchLatestVersions(request.getName(),
				Boolean.TRUE.equals(request.getIncludeDeprecated()), nextPageToken.getLimitForQuery(),
				nextPageToken.getOffset());

		return new FormTemplateSearchResponse().setNextPageToken(nextPageToken.getNextPageTokenForCurrentResults(page))
				.setResults(page);
	}

	private void verifyACTMember(UserInfo userInfo) {
		if (!authorizationManager.isACTTeamMemberOrAdmin(userInfo)) {
			throw new UnauthorizedException("Only ACT members may create or update a form template.");
		}
	}
}
