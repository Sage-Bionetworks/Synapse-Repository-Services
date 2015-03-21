package org.sagebionetworks.dynamo.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig;

/**
 * Annotation for marking a property as the hash key for a modeled class. Applied to the getter method for a hash key
 * property.
 * <p>
 * This annotation is required.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface DynamoBehavior {

	/**
	 * Optional parameter for the SaveBehavior
	 */
	DynamoDBMapperConfig.SaveBehavior saveBehavior() default DynamoDBMapperConfig.SaveBehavior.CLOBBER;

	/**
	 * Optional parameter for the ConsistentReads
	 */
	DynamoDBMapperConfig.ConsistentReads consistentReads() default DynamoDBMapperConfig.ConsistentReads.EVENTUAL;
}
