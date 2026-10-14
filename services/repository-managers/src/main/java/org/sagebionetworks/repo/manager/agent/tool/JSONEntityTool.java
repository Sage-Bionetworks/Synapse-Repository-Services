/**
 * 
 */
package org.sagebionetworks.repo.manager.agent.tool;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.sagebionetworks.schema.adapter.JSONEntity;

/**
 * Make a method as as an agent tool with a {@link JSONEntity} as a request
 * parameter.
 */
@Target({ ElementType.METHOD, ElementType.ANNOTATION_TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface JSONEntityTool {
	/**
	 * The name of the tool. If not provided, the method name will be used.
	 * <p>
	 * For maximum compatibility across different LLMs, it is recommended to use
	 * only alphanumeric characters, underscores, hyphens, and dots in tool names.
	 * Using spaces or special characters may cause issues with some LLMs (e.g.,
	 * OpenAI).
	 * </p>
	 * <p>
	 * Examples of recommended names: "get_weather", "search-docs", "tool.v1"
	 * </p>
	 * <p>
	 * Examples of names that may cause compatibility issues: "get weather"
	 * (contains space), "tool()" (contains parentheses)
	 * </p>
	 */
	String name() default "";

	/**
	 * The description of the tool. If not provided, the method name will be used.
	 */
	String description() default "";

	/**
	 * Whether the tool result should be returned directly or passed back to the
	 * model.
	 */
	boolean returnDirect() default false;
}
