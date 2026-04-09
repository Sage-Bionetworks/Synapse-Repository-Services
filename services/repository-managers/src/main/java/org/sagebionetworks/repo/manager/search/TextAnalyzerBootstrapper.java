package org.sagebionetworks.repo.manager.search;

import java.util.Arrays;

import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.schema.SynapseSchemaBootstrap;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dbo.search.TextAnalyzerDao;
import org.sagebionetworks.repo.model.schema.Organization;
import org.sagebionetworks.repo.model.search.table.TextAnalyzer;
import org.sagebionetworks.repo.model.search.table.TextAnalyzerSettings;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;

@Service
@DependsOn({"updateDefaultRealm", "teamManager"})
public class TextAnalyzerBootstrapper implements TextAnalyzerBootstrap {

	public static final long SCIENTIFIC_ID = 1L;
	public static final long STANDARD_ID = 2L;
	public static final long IDENTIFIER_ID = 3L;
	public static final long KEYWORD_ID = 4L;
	public static final long AUTOCOMPLETE_ID = 5L;
	public static final long AUTOCOMPLETE_SEARCH_ID = 6L;

	private final TextAnalyzerDao textAnalyzerDao;
	private final SynapseSchemaBootstrap synapseSchemaBootstrap;
	private final UserManager userManager;

	public TextAnalyzerBootstrapper(TextAnalyzerDao textAnalyzerDao,
			SynapseSchemaBootstrap synapseSchemaBootstrap, UserManager userManager) {
		this.textAnalyzerDao = textAnalyzerDao;
		this.synapseSchemaBootstrap = synapseSchemaBootstrap;
		this.userManager = userManager;
		bootstrapSystemAnalyzers();
	}

	@Override
	public void bootstrapSystemAnalyzers() {
		UserInfo adminUser = userManager.getUserInfo(AuthorizationConstants.BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		Organization organization = synapseSchemaBootstrap.createOrganizationIfDoesNotExist(adminUser);
		Long adminUserId = AuthorizationConstants.BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();
		String organizationName = organization.getName();

		// 1. SCIENTIFIC: English stemming, stop words, lowercase, synonym expansion
		textAnalyzerDao.createOrUpdateSystemAnalyzerForBootstrapOnly(SCIENTIFIC_ID, buildAnalyzer(
				"SCIENTIFIC",
				"English stemming, stop words, lowercase, synonym expansion. Best for scientific metadata.",
				buildScientificSettings()
		), organizationName, adminUserId);

		// 2. STANDARD: Standard tokenizer with lowercase
		textAnalyzerDao.createOrUpdateSystemAnalyzerForBootstrapOnly(STANDARD_ID, buildAnalyzer(
				"STANDARD",
				"OpenSearch standard analyzer. Unicode segmentation with lowercase. General-purpose.",
				buildStandardSettings()
		), organizationName, adminUserId);

		// 3. IDENTIFIER: Whitespace tokenizer with lowercase
		textAnalyzerDao.createOrUpdateSystemAnalyzerForBootstrapOnly(IDENTIFIER_ID, buildAnalyzer(
				"IDENTIFIER",
				"Preserves punctuation. Whitespace tokenization plus lowercase. Suitable for DOIs, RRIDs, PMIDs.",
				buildIdentifierSettings()
		), organizationName, adminUserId);

		// 4. KEYWORD: Built-in keyword analyzer
		textAnalyzerDao.createOrUpdateSystemAnalyzerForBootstrapOnly(KEYWORD_ID, buildAnalyzer(
				"KEYWORD",
				"No tokenization. Entire value is a single token. Suitable for facet and filter fields.",
				buildKeywordSettings()
		), organizationName, adminUserId);

		// 5. AUTOCOMPLETE: Edge n-gram for type-ahead
		textAnalyzerDao.createOrUpdateSystemAnalyzerForBootstrapOnly(AUTOCOMPLETE_ID, buildAnalyzer(
				"AUTOCOMPLETE",
				"Edge n-gram (2-20 chars) for type-ahead. Paired with AUTOCOMPLETE_SEARCH at search time.",
				buildAutocompleteSettings()
		), organizationName, adminUserId);

		// 6. AUTOCOMPLETE_SEARCH: Standard tokenizer with lowercase (search-time pair for AUTOCOMPLETE)
		textAnalyzerDao.createOrUpdateSystemAnalyzerForBootstrapOnly(AUTOCOMPLETE_SEARCH_ID, buildAnalyzer(
				"AUTOCOMPLETE_SEARCH",
				"Search-time analyzer paired with AUTOCOMPLETE. Standard tokenizer with lowercase and synonyms.",
				buildAutocompleteSearchSettings()
		), organizationName, adminUserId);
	}

	private TextAnalyzer buildAnalyzer(String name, String description, TextAnalyzerSettings settings) {
		TextAnalyzer analyzer = new TextAnalyzer();
		analyzer.setName(name);
		analyzer.setDescription(description);
		analyzer.setSettings(settings);
		return analyzer;
	}

	private TextAnalyzerSettings buildScientificSettings() {
		TextAnalyzerSettings settings = new TextAnalyzerSettings();
		settings.setTokenizer("standard");
		settings.setTokenFilters("{"
				+ "\"sci_word_delimiter\":{\"type\":\"word_delimiter_graph\",\"preserve_original\":true,"
				+ "\"split_on_case_change\":true,\"split_on_numerics\":true,"
				+ "\"catenate_words\":true,\"catenate_numbers\":false,"
				+ "\"stem_english_possessive\":true},"
				+ "\"english_stop\":{\"type\":\"stop\",\"stopwords\":\"_english_\"},"
				+ "\"english_stemmer\":{\"type\":\"stemmer\",\"language\":\"english\"}"
				+ "}");
		settings.setFilterOrder(Arrays.asList(
				"sci_word_delimiter", "lowercase", "english_stop", "english_stemmer"));
		settings.setSynonymAware(true);
		return settings;
	}

	private TextAnalyzerSettings buildStandardSettings() {
		TextAnalyzerSettings settings = new TextAnalyzerSettings();
		settings.setTokenizer("standard");
		settings.setTokenFilters("{"
				+ "\"std_word_delimiter\":{\"type\":\"word_delimiter_graph\",\"preserve_original\":true,"
				+ "\"split_on_case_change\":true,\"split_on_numerics\":true,"
				+ "\"catenate_words\":true,\"catenate_numbers\":false,"
				+ "\"stem_english_possessive\":true}"
				+ "}");
		settings.setFilterOrder(Arrays.asList("std_word_delimiter", "lowercase"));
		settings.setSynonymAware(true);
		return settings;
	}

	private TextAnalyzerSettings buildIdentifierSettings() {
		TextAnalyzerSettings settings = new TextAnalyzerSettings();
		settings.setTokenizer("whitespace");
		settings.setTokenFilters("{"
				+ "\"id_word_delimiter\":{\"type\":\"word_delimiter_graph\",\"preserve_original\":true,"
				+ "\"split_on_case_change\":true,\"split_on_numerics\":true,"
				+ "\"catenate_words\":true,\"catenate_numbers\":false,"
				+ "\"stem_english_possessive\":false}"
				+ "}");
		settings.setFilterOrder(Arrays.asList("id_word_delimiter", "lowercase"));
		settings.setSynonymAware(true);
		return settings;
	}

	private TextAnalyzerSettings buildKeywordSettings() {
		TextAnalyzerSettings settings = new TextAnalyzerSettings();
		settings.setTokenizer("keyword");
		return settings;
	}

	private TextAnalyzerSettings buildAutocompleteSettings() {
		TextAnalyzerSettings settings = new TextAnalyzerSettings();
		settings.setTokenizer("standard");
		settings.setTokenFilters("{"
				+ "\"ac_word_delimiter\":{\"type\":\"word_delimiter_graph\",\"preserve_original\":true,"
				+ "\"split_on_case_change\":true,\"split_on_numerics\":true,"
				+ "\"catenate_words\":true,\"catenate_numbers\":false,"
				+ "\"stem_english_possessive\":true},"
				+ "\"edge_ngram_filter\":{\"type\":\"edge_ngram\",\"min_gram\":2,\"max_gram\":20}"
				+ "}");
		settings.setFilterOrder(Arrays.asList("ac_word_delimiter", "lowercase", "edge_ngram_filter"));
		settings.setSynonymAware(false);
		return settings;
	}

	private TextAnalyzerSettings buildAutocompleteSearchSettings() {
		TextAnalyzerSettings settings = new TextAnalyzerSettings();
		settings.setTokenizer("standard");
		settings.setTokenFilters("{"
				+ "\"acs_word_delimiter\":{\"type\":\"word_delimiter_graph\",\"preserve_original\":true,"
				+ "\"split_on_case_change\":true,\"split_on_numerics\":true,"
				+ "\"catenate_words\":true,\"catenate_numbers\":false,"
				+ "\"stem_english_possessive\":true}"
				+ "}");
		settings.setFilterOrder(Arrays.asList("acs_word_delimiter", "lowercase"));
		settings.setSynonymAware(true);
		return settings;
	}

}
