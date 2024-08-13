package org.sagebionetworks.repo.model.dbo.migration;

import java.io.IOException;
import java.lang.reflect.Field;

import org.sagebionetworks.repo.model.UnmodifiableXStream;
import org.sagebionetworks.repo.model.dbo.DatabaseObject;
import org.sagebionetworks.repo.model.jdo.JDOSecondaryPropertyUtils;
import org.sagebionetworks.schema.adapter.JSONEntity;
import org.sagebionetworks.util.ValidateArgument;

public class XStreamToJsonTranslator {

	private final UnmodifiableXStream xStream;
	private final Field from;
	private final Field to;

	private XStreamToJsonTranslator(UnmodifiableXStream xstream, Class<? extends JSONEntity> dtoType,
			Class<? extends DatabaseObject<?>> dboType, String fromName, String toName) {
		ValidateArgument.required(dboType, "dtoType");
		ValidateArgument.required(dboType, "dboType");
		ValidateArgument.required(fromName, "fromName");
		ValidateArgument.required(toName, "toName");
		try {
			this.xStream = xstream != null ? xstream : UnmodifiableXStream.builder().allowTypes(dtoType).build();
			this.from = dboType.getDeclaredField(fromName);
			this.from.setAccessible(true);
			this.to = dboType.getDeclaredField(toName);
			this.to.setAccessible(true);
		} catch (NoSuchFieldException | SecurityException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Apply the translation on the provide database object.
	 * 
	 * @param backup
	 */
	public void translate(DatabaseObject<?> backup) {
		try {
			byte[] oldValue = (byte[]) from.get(backup);
			String newValue = (String) to.get(backup);
			if (oldValue != null) {
				if (newValue != null) {
					throw new IllegalArgumentException(
							String.format("Both '%s' and '%s' are not null", from.getName(), to.getName()));
				}
				JSONEntity entity = (JSONEntity) JDOSecondaryPropertyUtils.decompressObject(xStream, oldValue);
				newValue = JDOSecondaryPropertyUtils.createJSONFromObject(entity);
				to.set(backup, newValue);
				from.set(backup, null);
			}
		} catch (SecurityException | IllegalArgumentException | IllegalAccessException | IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {
		private Class<? extends JSONEntity> dtoType;
		private Class<? extends DatabaseObject<?>> dboType;
		private String fromName;
		private String toName;
		private UnmodifiableXStream xstream;

		public Builder setDtoType(Class<? extends JSONEntity> dtoType) {
			this.dtoType = dtoType;
			return this;
		}

		public Builder setDboType(Class<? extends DatabaseObject<?>> dboType) {
			this.dboType = dboType;
			return this;
		}

		public Builder setFromName(String fromName) {
			this.fromName = fromName;
			return this;
		}

		public Builder setToName(String toName) {
			this.toName = toName;
			return this;
		}

		public Builder setXStream(UnmodifiableXStream xstream) {
			this.xstream = xstream;
			return this;
		}

		public XStreamToJsonTranslator build() {
			return new XStreamToJsonTranslator(xstream, dtoType, dboType, fromName, toName);
		}
	}
}
