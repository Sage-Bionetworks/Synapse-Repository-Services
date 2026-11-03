package org.sagebionetworks.repo.model.dbo.dao.dataaccess;

import java.util.List;
import java.util.Optional;

import org.sagebionetworks.repo.model.dataaccess.schema.FormTemplate;

/**
 * Persistence of form templates. Each template is an append-only series of
 * immutable versions; the first version of a new template is version one.
 */
public interface FormTemplateDao {

	/**
	 * Create a new template with a single version.
	 *
	 * @param userId   The user creating the template.
	 * @param template The template to create. The id, etag and version number are assigned by this call.
	 * @return The created template, with the first version number.
	 * @throws IllegalArgumentException If a template with the same name already exists.
	 */
	FormTemplate create(Long userId, FormTemplate template);

	/**
	 * Add a new version to an existing template. The caller must set the version
	 * number of the new version on the given template.
	 *
	 * @param userId   The user creating the new version.
	 * @param template The body of the new version, including its id and version number.
	 * @return The new version of the template, with a freshly assigned etag.
	 * @throws IllegalArgumentException If another template already has the same name.
	 */
	FormTemplate createNewVersion(Long userId, FormTemplate template);

	/**
	 * @return The latest version of the template with the given id, or empty if no such template exists.
	 */
	Optional<FormTemplate> getLatestVersion(Long id);

	/**
	 * @return The requested version of the template, or empty if either the template or the version does not exist.
	 */
	Optional<FormTemplate> getVersion(Long id, Long versionNumber);

	/**
	 * Lock the owner row of the given template and return the state needed to apply an update.
	 *
	 * @return Empty if no such template exists.
	 */
	Optional<FormTemplateInfoForUpdate> getForUpdate(Long id);

	/**
	 * Search the latest version of each template.
	 *
	 * @param nameFilter        When not null, only templates whose name contains this value are returned.
	 * @param includeDeprecated When false, templates whose latest version is deprecated are excluded.
	 * @param limit             The maximum number of templates to return.
	 * @param offset            The number of templates to skip.
	 * @return The matching templates, in a deterministic order.
	 */
	List<FormTemplate> searchLatestVersions(String nameFilter, boolean includeDeprecated, long limit, long offset);

	/**
	 * Delete all templates and all of their versions. For testing only.
	 */
	void truncateAll();
}
