package org.sagebionetworks.repo.manager.agent.tool;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import org.json.JSONObject;
import org.sagebionetworks.repo.manager.agent.specialist.ToolResponse;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.EntityInstanceFactory;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.springframework.ai.chat.model.ToolContext;

/**
 * Test fixture exercising {@link JSONEntityToolBase}: a typed {@link Entity} tool whose
 * {@code concreteType}-discriminated argument the base deserializes, and a raw-{@code String} tool
 * whose input schema is generated from a declared {@link JSONEntityToolParam#schemaType()} while the
 * untouched payload is passed through.
 */
public class ExampleJSONEntityTool extends JSONEntityToolBase {

	private Entity entity;
	private String rawPayload;
	private JSONObject rawObject;
	private Long count;
	private String label;
	private boolean noArgCalled;
	private ToolContext context;

	public ExampleJSONEntityTool() {
		super();
	}

	@Override
	protected Collection<Iterator<String>> getPolymorphicImplementerSeeds() {
		return List.of(EntityInstanceFactory.singleton().getKeySetIterator());
	}

	@JSONEntityTool(name = "get_entity_file_handle", description = "This is the method description",
			returnDirect = true)
	public ToolResponse<FileHandle> getEntityFileHandle(
			@JSONEntityToolParam(description = "this is the parameter description") Entity entity,
			ToolContext context) {
		this.entity = entity;
		this.context = context;
		return new ToolResponse<FileHandle>(new S3FileHandle().setId("123"));
	}

	@JSONEntityTool(description = "Consumes the raw request payload untouched")
	public ToolResponse<FileHandle> getRawPayload(
			@JSONEntityToolParam(schemaType = Entity.class, description = "raw entity payload") String payload,
			ToolContext context) {
		this.rawPayload = payload;
		this.context = context;
		return new ToolResponse<FileHandle>(new S3FileHandle().setId("456"));
	}

	@JSONEntityTool(description = "Consumes the parsed-but-untyped request payload")
	public ToolResponse<FileHandle> getRawObject(
			@JSONEntityToolParam(schemaType = Entity.class, description = "raw entity object") JSONObject payload,
			ToolContext context) {
		this.rawObject = payload;
		this.context = context;
		return new ToolResponse<FileHandle>(new S3FileHandle().setId("789"));
	}

	@JSONEntityTool(name = "sumScalars", description = "Consumes scalar arguments bound by name")
	public ToolResponse<FileHandle> sumScalars(
			@JSONEntityToolParam(description = "how many", required = true) Long count,
			@JSONEntityToolParam(description = "an optional label", required = false) String label,
			ToolContext context) {
		this.count = count;
		this.label = label;
		this.context = context;
		return new ToolResponse<FileHandle>(new S3FileHandle().setId("count-" + count));
	}

	@JSONEntityTool(name = "ping", description = "Takes no argument")
	public ToolResponse<FileHandle> ping(ToolContext context) {
		this.noArgCalled = true;
		this.context = context;
		return new ToolResponse<FileHandle>(new S3FileHandle().setId("pong"));
	}

	public Long getCount() {
		return count;
	}

	public String getLabel() {
		return label;
	}

	public boolean isNoArgCalled() {
		return noArgCalled;
	}

	public Entity getEntity() {
		return entity;
	}

	public String getRawPayload() {
		return rawPayload;
	}

	public JSONObject getRawObject() {
		return rawObject;
	}

	public ToolContext getContext() {
		return context;
	}

	@Override
	public int hashCode() {
		return Objects.hash(context, entity, rawPayload);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ExampleJSONEntityTool other = (ExampleJSONEntityTool) obj;
		return Objects.equals(context, other.context) && Objects.equals(entity, other.entity)
				&& Objects.equals(rawPayload, other.rawPayload);
	}

}
