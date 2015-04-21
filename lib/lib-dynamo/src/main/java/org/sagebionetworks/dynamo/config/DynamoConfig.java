package org.sagebionetworks.dynamo.config;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.reflections.Reflections;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.dynamo.DynamoTable;
import org.sagebionetworks.dynamo.config.DynamoTableConfig.DynamoKey;
import org.sagebionetworks.dynamo.config.DynamoTableConfig.DynamoKeySchema;
import org.sagebionetworks.dynamo.config.DynamoTableConfig.DynamoThroughput;
import org.sagebionetworks.util.Pair;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig.ConsistentReads;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig.SaveBehavior;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig.TableNameOverride;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBRangeKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;

public class DynamoConfig {

	private static final DynamoThroughput DEV_THROUGHPUT = new DynamoThroughput(10L, 5L);

	private final List<DynamoTableConfig> tableList;

	DynamoConfig() {

		List<DynamoTableConfig> tableList = new ArrayList<DynamoTableConfig>();

		// scan all child packages
		Reflections reflections = new Reflections(DynamoTable.class.getPackage().getName());
		for (Class<?> clazz : reflections.getTypesAnnotatedWith(DynamoDBTable.class)) {
			String tableName = clazz.getAnnotation(DynamoDBTable.class).tableName();
			if (tableName == null) {
				throw new IllegalArgumentException(clazz.getName() + " has no table name defined");
			}

			Pair<DynamoDBHashKey, ScalarAttributeType> hashAnnotationAndType = getMethodAnnotation(clazz, DynamoDBHashKey.class);
			if (hashAnnotationAndType == null) {
				throw new IllegalArgumentException(clazz.getName() + " has no hash key defined");
			}
			String hashKeyName = hashAnnotationAndType.getFirst().attributeName();
			if (hashKeyName == null) {
				throw new IllegalArgumentException(clazz.getName() + " has no hashKey name defined in the @DynamoDBHashKey annotation");
			}
			DynamoKey hashKey = new DynamoKey(hashKeyName, KeyType.HASH, hashAnnotationAndType.getSecond());

			DynamoKey rangeKey = null;
			Pair<DynamoDBRangeKey, ScalarAttributeType> rangeAnnotationAndType = getMethodAnnotation(clazz, DynamoDBRangeKey.class);
			if (rangeAnnotationAndType != null) {
				String rangeKeyName = rangeAnnotationAndType.getFirst().attributeName();
				if (rangeKeyName == null) {
					throw new IllegalArgumentException(clazz.getName() + " has no rangeKey name defined in the @DynamoDBRangeKey annotation");
				}
				rangeKey = new DynamoKey(rangeKeyName, KeyType.RANGE, rangeAnnotationAndType.getSecond());
			}

			DynamoKeySchema keySchema = new DynamoKeySchema(hashKey, rangeKey);
			DynamoThroughput throughput = StackConfiguration.isProductionStack() ? new DynamoThroughput(75L, 75L) : DEV_THROUGHPUT;
			DynamoTableConfig table = new DynamoTableConfig(tableName, keySchema, throughput);
			tableList.add(table);
		}
		this.tableList = Collections.unmodifiableList(tableList);
	}

	Iterable<DynamoTableConfig> listTables() {
		return this.tableList;
	}

	public static DynamoDBMapperConfig getDynamoDBMapperConfigFor(Class<? extends DynamoTable> clazz) {
		SaveBehavior saveBehavior = SaveBehavior.CLOBBER;
		ConsistentReads consistentReads = ConsistentReads.EVENTUAL;
		DynamoBehavior hashKeyAnnotation = clazz.getAnnotation(DynamoBehavior.class);
		if (hashKeyAnnotation != null) {
			saveBehavior = hashKeyAnnotation.saveBehavior();
			consistentReads = hashKeyAnnotation.consistentReads();
		}
		String tableName = clazz.getAnnotation(DynamoDBTable.class).tableName();
		if (tableName == null) {
			throw new IllegalArgumentException(clazz.getName() + " has no table name defined");
		}
		String stackPrefix = StackConfiguration.getStack() + "-" + StackConfiguration.getStackInstance() + "-";
		String tableNameOverride = stackPrefix + tableName;
		return new DynamoDBMapperConfig(saveBehavior, consistentReads, new TableNameOverride(tableNameOverride));
	}

	private static <T extends Annotation> Pair<T, ScalarAttributeType> getMethodAnnotation(Class<?> clazz, Class<T> annotationType) {
		T annotation = null;
		ScalarAttributeType type = ScalarAttributeType.S;
		Method[] methods = clazz.getDeclaredMethods();
		for (Method method : methods) {
			T foundAnnotation = method.getAnnotation(annotationType);
			if (foundAnnotation != null) {
				if (annotation != null) {
					throw new IllegalArgumentException(clazz.getName() + " has more than one annotation of type " + annotationType.getName());
				}
				annotation = foundAnnotation;
				type = guessType(method.getReturnType());
			}
		}
		return annotation == null ? null : new Pair<T, ScalarAttributeType>(annotation, type);
	}

	private static ScalarAttributeType guessType(Class<?> returnType) {
		if (returnType == String.class) {
			return ScalarAttributeType.S;
		}
		if (Number.class.isAssignableFrom(returnType)) {
			return ScalarAttributeType.N;
		}
		if (byte[].class.isAssignableFrom(returnType)) {
			return ScalarAttributeType.B;
		}
		throw new IllegalArgumentException("Cannot guess key type from return type " + returnType.getName());
	}
}
