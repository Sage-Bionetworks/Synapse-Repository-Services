package org.sagebionetworks.evaluation.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.function.Function;

import org.sagebionetworks.evaluation.model.SubmissionStatusEnum;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.table.AnnotationType;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.DefaultField;
import org.sagebionetworks.repo.model.table.FacetType;
import org.sagebionetworks.util.EnumUtils;

/**
 * Enumeration of the fields that are exposed to the submission views
 * 
 * @author Marco Marasca
 */
public enum SubmissionField implements DefaultField {

	// The status of the submission
	status				("STATUS", 			ColumnType.STRING, 		 null, FacetType.enumeration, 	SubmissionStatusEnum.class, AnnotationType.STRING, 	false),
	// The id of the evaluation that the submission belongs to
	evaluationid		("EVALUATION_ID", 	ColumnType.EVALUATIONID, null, FacetType.enumeration, 	null, 						AnnotationType.LONG,	false),
	evaluationroundid 	("EVALUATION_ROUND_ID",	ColumnType.INTEGER,  null, FacetType.enumeration, 	null,  						AnnotationType.LONG, 	true),
	// The id of the submitter, can be the user or the team id
	submitterid			("SUBMITTER_ID", 	ColumnType.USERID,		 null, FacetType.enumeration, 	null, 						AnnotationType.LONG, 	false),
	// The optional submitter alias
	submitteralias		("SUBMITTER_ALIAS", ColumnType.STRING, 		 500L, FacetType.enumeration, 	null, 						AnnotationType.STRING, 	true),
	// The id of the submitted entity
	entityid			("ENTITY_ID", 		ColumnType.ENTITYID, 	 null, FacetType.enumeration, 	null, 						AnnotationType.STRING, 	false),
	// The version of the submitted entity
	entityversion		("ENTITY_VERSION",	ColumnType.INTEGER, 	 null, FacetType.enumeration, 	null, 						AnnotationType.LONG, 	false),
	// The docker repository name for a docker submission
	dockerrepositoryname("DOCKER_REPO", 	ColumnType.STRING, 		 400L, null, 					null, 						AnnotationType.STRING, 	true),
	// The digest for a docker submission
	dockerdigest		("DOCKER_DIGEST", 	ColumnType.STRING, 		 200L, null, 					null, 						AnnotationType.STRING, 	true);

	// Note: The canCancel and cancellationRequests are stored in the serialized version DTO,
	// we can consider including them in the index if we normalize the fields into the table
	// or alternatively deserialize the COL_SUBSTATUS_SERIALIZED_ENTITY blob to read the attributes

	private String columnAlias;
	private ColumnType columnType;
	private Long maximumSize;
	private FacetType facetType;
	private List<String> enumValues;
	private AnnotationType annotationType;
	private boolean isNullable;
	private Function<ResultSet, String> valueExtractor;

	private SubmissionField(String columnAlias, ColumnType columnType, Long maximumSize, FacetType facetType,
			Class<? extends Enum<?>> enumValues, AnnotationType annotationType, boolean isNullable) {
		this.columnAlias = columnAlias;
		this.columnType = columnType;
		this.maximumSize = maximumSize;
		this.facetType = facetType;
		this.enumValues = enumValues == null ? null : EnumUtils.names(enumValues);
		this.annotationType = annotationType;
		this.isNullable = isNullable;
		if (enumValues == null) {
			this.valueExtractor = stringExtractor(columnAlias);
		} else {
			this.valueExtractor = enumExtractor(columnAlias, enumValues.getEnumConstants());
		}
	}

	/**
	 * @return The alias used when querying from the database
	 */
	public String getColumnAlias() {
		return columnAlias;
	}

	@Override
	public String getColumnName() {
		return name();
	}

	@Override
	public ColumnType getColumnType() {
		return columnType;
	}

	@Override
	public Long getMaximumSize() {
		return maximumSize;
	}

	@Override
	public FacetType getFacetType() {
		return facetType;
	}

	@Override
	public List<String> getEnumValues() {
		return enumValues;
	}

	public AnnotationType getAnnotationType() {
		return annotationType;
	}
	
	public boolean isNullable() {
		return isNullable;
	}

	public String getValue(ResultSet rs) {
		return valueExtractor.apply(rs);
	}

	public static Function<ResultSet, String> stringExtractor(String columnAlias) {
		return (ResultSet rs) -> {
			try {
				return rs.getString(columnAlias);
			} catch (SQLException e) {
				throw new DatastoreException(
						"Could not extract value from alias " + columnAlias + ": " + e.getMessage(), e);
			}
		};
	}
	
	public static Function<ResultSet, String> enumExtractor(String columnAlias, Enum<?>[] enumValues) {
		return (ResultSet rs) -> {
			try {
				return enumValues[rs.getInt(columnAlias)].name();
			} catch (SQLException e) {
				throw new DatastoreException(
						"Could not extract value from alias " + columnAlias + ": " + e.getMessage(), e);
			}
		};
	}

}
