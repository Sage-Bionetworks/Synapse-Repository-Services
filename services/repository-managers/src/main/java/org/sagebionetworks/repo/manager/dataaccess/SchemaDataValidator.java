package org.sagebionetworks.repo.manager.dataaccess;

import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;
import org.sagebionetworks.repo.manager.schema.JsonSchemaManager;
import org.sagebionetworks.repo.manager.schema.JsonSchemaValidatorFactory;
import org.sagebionetworks.repo.model.JsonSchemaAccessRequirement;
import org.sagebionetworks.repo.model.dataaccess.Renewal;
import org.sagebionetworks.repo.model.dataaccess.RequestInterface;
import org.sagebionetworks.repo.model.dataaccess.schema.FormTemplate;
import org.sagebionetworks.repo.model.dataaccess.schema.FormTemplateReference;
import org.sagebionetworks.repo.model.dbo.search.OpaqueJsonColumnCodecUtil;
import org.sagebionetworks.repo.model.schema.JsonSchema;
import org.sagebionetworks.repo.model.schema.JsonSchemaConstants;
import org.sagebionetworks.repo.web.AccessRequirementVersionOutOfDateException;
import org.sagebionetworks.repo.web.SubmissionContextMismatchException;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.stereotype.Service;

/**
 * Validates the answers a data access request carries against the JSON Schema that the Access
 * Requirement it answers is bound to.
 */
@Service
public class SchemaDataValidator {

	private static final String SCHEMA_DATA = "Request.schemaData";

	private final JsonSchemaManager jsonSchemaManager;
	private final FormTemplateManager formTemplateManager;
	private final JsonSchemaValidatorFactory validatorFactory;

	public SchemaDataValidator(JsonSchemaManager jsonSchemaManager, FormTemplateManager formTemplateManager,
			JsonSchemaValidatorFactory validatorFactory) {
		this.jsonSchemaManager = jsonSchemaManager;
		this.formTemplateManager = formTemplateManager;
		this.validatorFactory = validatorFactory;
	}

	/**
	 * Validate the answers the given request carries against the schema that the given requirement
	 * is bound to.
	 *
	 * @return The answers to record on the submission, carrying the submission context that they
	 *         were validated under.
	 * @throws AccessRequirementVersionOutOfDateException If the request was last saved against a
	 *                                                    version of the requirement that is no
	 *                                                    longer current.
	 * @throws SubmissionContextMismatchException         If the answers declare a submission
	 *                                                    context that contradicts the request.
	 * @throws IllegalArgumentException                   If the answers do not satisfy the schema.
	 *                                                    The message reports every problem found.
	 */
	public JSONObject validate(JsonSchemaAccessRequirement accessRequirement, RequestInterface request) {
		ValidateArgument.required(accessRequirement, "accessRequirement");
		ValidateArgument.required(request, "request");

		requireCurrentVersion(accessRequirement, request);

		JSONObject answers = applySubmissionContext(request);
		JsonSchema schema = getBoundSchema(accessRequirement);

		try {
			validatorFactory.buildValidator(schema, false).validate(answers);
		} catch (org.everit.json.schema.ValidationException e) {
			// Every message is reported rather than only the first, since structured per field
			// reporting is the client's job and it builds that from the same schema.
			throw new IllegalArgumentException(
					"The request data is invalid: " + String.join(" ", e.getAllMessages()));
		}

		return answers;
	}

	/**
	 * Find the file handles that the given answers reference, through the properties that the
	 * schema the given requirement is bound to declares with the
	 * {@link FormTemplateValidator#FILE_HANDLE_FORMAT} format. The answers need not be valid, since
	 * a draft is saved long before it is complete.
	 *
	 * @return The distinct file handle IDs the answers reference, empty if there are no answers.
	 * @throws IllegalArgumentException If the answers are not a JSON object.
	 */
	public List<String> getFileHandleIds(JsonSchemaAccessRequirement accessRequirement, Object schemaData) {
		ValidateArgument.required(accessRequirement, "accessRequirement");
		if (schemaData == null) {
			return List.of();
		}
		JSONObject answers = copyAnswers(schemaData);
		return SchemaDataUtils.findFileHandleIds(getBoundSchema(accessRequirement), answers);
	}

	/**
	 * An Access Requirement states the terms of access, so a submission is only ever validated
	 * against its current version. The request records the version its answers were last saved
	 * against, which lags behind once the requirement is updated and catches up when the requester
	 * answers the current form again.
	 */
	private static void requireCurrentVersion(JsonSchemaAccessRequirement accessRequirement,
			RequestInterface request) {
		Long savedAgainst = request.getAccessRequirementVersionNumber();
		if (!accessRequirement.getVersionNumber().equals(savedAgainst)) {
			throw new AccessRequirementVersionOutOfDateException("This request was last saved against version "
					+ savedAgainst + " of the access requirement, which is now on version "
					+ accessRequirement.getVersionNumber()
					+ ". Answer the current form and save the request before submitting it.");
		}
	}

	/**
	 * The answers to validate, which are the request's own with the submission context they are
	 * being submitted under.
	 */
	private static JSONObject applySubmissionContext(RequestInterface request) {
		String context = request instanceof Renewal ? JsonSchemaConstants.SUBMISSION_CONTEXT_RENEWAL
				: JsonSchemaConstants.SUBMISSION_CONTEXT_REQUEST;
		JSONObject answers = copyAnswers(request.getSchemaData());

		// No form collects the context, so the request will usually not carry one and the server
		// supplies it. A client that sends one anyway must not be able to claim a context that the
		// request is not in, since the schema can condition what it requires on the context.
		if (!answers.isNull(JsonSchemaConstants.SUBMISSION_CONTEXT_PROPERTY)
				&& !context.equals(answers.get(JsonSchemaConstants.SUBMISSION_CONTEXT_PROPERTY))) {
			throw new SubmissionContextMismatchException("This request is a " + context
					+ ", so its '" + JsonSchemaConstants.SUBMISSION_CONTEXT_PROPERTY + "' cannot be '"
					+ answers.get(JsonSchemaConstants.SUBMISSION_CONTEXT_PROPERTY) + "'.");
		}
		answers.put(JsonSchemaConstants.SUBMISSION_CONTEXT_PROPERTY, context);

		return answers;
	}

	/**
	 * The answers as a mutable JSON object, leaving the request's own copy as it was.
	 */
	private static JSONObject copyAnswers(Object schemaData) {
		if (schemaData == null) {
			return new JSONObject();
		}
		// The answers are opaque JSON, so they arrive as whichever shape produced them: a
		// JSONObject when read back from the database, a JSONObjectAdapter when taken off the wire.
		// Rendering them through the same codec that persists them accepts every such shape.
		String json = OpaqueJsonColumnCodecUtil.serialize(schemaData, SCHEMA_DATA);
		try {
			return new JSONObject(json);
		} catch (JSONException e) {
			throw new IllegalArgumentException("Request.schemaData must be a JSON object.", e);
		}
	}

	/**
	 * The schema that the requirement is bound to, which it names transitively through the version
	 * of the form template that renders it.
	 */
	private JsonSchema getBoundSchema(JsonSchemaAccessRequirement accessRequirement) {
		FormTemplateReference reference = accessRequirement.getFormTemplateRef();
		ValidateArgument.required(reference, "accessRequirement.formTemplateRef");

		FormTemplate template = formTemplateManager.getVersion(reference.getTemplateId(),
				reference.getTemplateVersionNumber());

		return jsonSchemaManager.getValidationSchema(template.getSchema$id());
	}
}
