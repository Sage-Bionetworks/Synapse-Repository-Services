package org.sagebionetworks.repo.manager.agent.specialist.tablequery;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.manager.agent.CodeInterpreterTools;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Service;

import au.com.bytecode.opencsv.CSVReader;

/**
 * Factory for creating {@link TableQuerySpecialist} instances. Each instance gets a fresh
 * conversation memory and a pre-rendered system prompt that includes the current SQL reference.
 */
@Service
public class TableQuerySpecialistFactory {

	static final String PROMPT_TEMPLATE = "prompts/table-query-specialist.vtp";
	static final String SQL_SPEC_CSV = "SQLSpecification2.csv";

	private final ChatModel chatModel;
	private final StackConfiguration stackConfig;
	private final TableQueryTools tableQueryTools;
	private final CodeInterpreterTools codeInterpreterTools;
	private final String renderedSystemPrompt;

	public TableQuerySpecialistFactory(ChatModel chatModel, StackConfiguration stackConfig,
			TableQueryTools tableQueryTools, CodeInterpreterTools codeInterpreterTools) {
		this.chatModel = chatModel;
		this.stackConfig = stackConfig;
		this.tableQueryTools = tableQueryTools;
		this.codeInterpreterTools = codeInterpreterTools;
		this.renderedSystemPrompt = renderSystemPrompt();
	}

	public TableQuerySpecialist create() {
		return new TableQuerySpecialist(chatModel, stackConfig, tableQueryTools, codeInterpreterTools, renderedSystemPrompt);
	}

	String renderSystemPrompt() {
		VelocityEngine engine = new VelocityEngine();
		engine.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath");
		engine.setProperty("classpath.resource.loader.class", ClasspathResourceLoader.class.getName());
		engine.setProperty("runtime.references.strict", true);

		List<SqlExample> sqlExamples = loadSqlExamples();

		VelocityContext context = new VelocityContext();
		context.put("sqlExamples", sqlExamples);

		Template template = engine.getTemplate(PROMPT_TEMPLATE);
		StringWriter writer = new StringWriter();
		template.merge(context, writer);
		return writer.toString();
	}

	List<SqlExample> loadSqlExamples() {
		InputStream in = getClass().getClassLoader().getResourceAsStream(SQL_SPEC_CSV);
		if (in == null) {
			throw new IllegalStateException("Cannot find " + SQL_SPEC_CSV + " on the classpath");
		}
		try (CSVReader reader = new CSVReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
			List<String[]> rows = reader.readAll();
			List<SqlExample> examples = new ArrayList<>(rows.size());
			for (String[] row : rows) {
				if (row != null && row.length >= 3) {
					examples.add(new SqlExample(row[0], row[1], row[2]));
				}
			}
			return examples;
		} catch (IOException e) {
			throw new RuntimeException("Failed to load SQL examples from " + SQL_SPEC_CSV, e);
		}
	}

	/**
	 * A single SQL example entry from the specification CSV.
	 */
	public static class SqlExample {
		private final String category;
		private final String description;
		private final String sql;

		public SqlExample(String category, String description, String sql) {
			this.category = category;
			this.description = description;
			this.sql = sql;
		}

		public String getCategory() {
			return category;
		}

		public String getDescription() {
			return description;
		}

		public String getSql() {
			return sql;
		}
	}
}
