package org.sagebionetworks.repo.manager.search;

import java.util.Arrays;

import org.json.JSONObject;
import org.opensearch.client.opensearch._types.analysis.Analyzer;
import org.opensearch.client.opensearch._types.analysis.TokenFilter;
import org.opensearch.client.opensearch._types.analysis.TokenFilterDefinition;
import org.opensearch.client.opensearch.indices.IndexSettingsAnalysis;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.schema.SynapseSchemaBootstrap;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dbo.search.TextAnalyzerDao;
import org.sagebionetworks.repo.model.schema.Organization;
import org.sagebionetworks.repo.model.search.table.TextAnalyzer;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;

@Service
@DependsOn({"realmDao", "teamManager"})
public class TextAnalyzerBootstrapper implements TextAnalyzerBootstrap {

	public static final long SCIENTIFIC_ID = 1L;
	public static final long STANDARD_ID = 2L;
	public static final long IDENTIFIER_ID = 3L;
	public static final long KEYWORD_ID = 4L;
	public static final long AUTOCOMPLETE_ID = 5L;

	// Each bootstrapped analyzer's settings is built with the OpenSearch Java client's
	// typed builders, then serialized to JSON for the existing string-typed DAO column.
	// The persisted shape is identical to a hand-typed OpenSearch settings.analysis block;
	// the typed builders just give us compile-time checking on filter type names and
	// parameters, plus IDE autocomplete. Bootstrapped analyzers never carry $ref entries —
	// users who want synonyms compose their own TextAnalyzers.

	/**
	 * Stop-token-filter type discriminator. The OpenSearch native form
	 * {@code "stopwords": "_english_"} is a single-string convenience that the typed
	 * {@code StopTokenFilter.stopwords(List)} setter accepts as a one-element list — the
	 * serializer collapses it back to the same string at the wire boundary.
	 */
	private static final String ENGLISH_STOPWORDS = "_english_";

	private static final IndexSettingsAnalysis SCIENTIFIC_SETTINGS = IndexSettingsAnalysis.of(a -> a
			.filter("sci_word_delimiter", wordDelimiterGraph(true))
			.filter("english_stop", filter(b -> b.stop(s -> s.stopwords(ENGLISH_STOPWORDS))))
			.filter("english_stemmer", filter(b -> b.stemmer(s -> s.language("english"))))
			.analyzer("default", customAnalyzer("standard",
					Arrays.asList("sci_word_delimiter", "lowercase", "english_stop", "english_stemmer")))
	);

	private static final IndexSettingsAnalysis STANDARD_SETTINGS = IndexSettingsAnalysis.of(a -> a
			.filter("std_word_delimiter", wordDelimiterGraph(true))
			.analyzer("default", customAnalyzer("standard",
					Arrays.asList("std_word_delimiter", "lowercase")))
	);

	private static final IndexSettingsAnalysis IDENTIFIER_SETTINGS = IndexSettingsAnalysis.of(a -> a
			.filter("id_word_delimiter", wordDelimiterGraph(false))
			.analyzer("default", customAnalyzer("whitespace",
					Arrays.asList("id_word_delimiter", "lowercase")))
	);

	private static final IndexSettingsAnalysis KEYWORD_SETTINGS = IndexSettingsAnalysis.of(a -> a
			.analyzer("default", customAnalyzer("keyword", null))
	);

	// AUTOCOMPLETE pairs an index-time edge_ngram chain with a non-ngram search-time chain in
	// one record. The index chain uses the legacy non-graph word_delimiter because
	// word_delimiter_graph emits multi-position graph tokens that edge_ngram cannot consume.
	private static final IndexSettingsAnalysis AUTOCOMPLETE_SETTINGS = IndexSettingsAnalysis.of(a -> a
			.filter("ac_word_delimiter", filter(b -> b.wordDelimiter(w -> w
					.preserveOriginal(true)
					.splitOnCaseChange(true)
					.splitOnNumerics(true)
					.catenateWords(true)
					.catenateNumbers(false)
					.stemEnglishPossessive(true))))
			.filter("edge_ngram_filter", filter(b -> b.edgeNgram(e -> e.minGram(2).maxGram(20))))
			.filter("acs_word_delimiter", wordDelimiterGraph(true))
			.analyzer("default", customAnalyzer("standard",
					Arrays.asList("ac_word_delimiter", "lowercase", "edge_ngram_filter")))
			.analyzer("default_search", customAnalyzer("standard",
					Arrays.asList("lowercase", "acs_word_delimiter")))
	);

	/**
	 * Standard-shaped {@code word_delimiter_graph} filter used by SCIENTIFIC, STANDARD,
	 * IDENTIFIER, and AUTOCOMPLETE.default_search. Only {@code stem_english_possessive}
	 * varies (false for IDENTIFIER, true for everyone else), so it's the one parameter
	 * the caller supplies.
	 */
	private static TokenFilter wordDelimiterGraph(boolean stemEnglishPossessive) {
		return filter(b -> b.wordDelimiterGraph(w -> w
				.preserveOriginal(true)
				.splitOnCaseChange(true)
				.splitOnNumerics(true)
				.catenateWords(true)
				.catenateNumbers(false)
				.stemEnglishPossessive(stemEnglishPossessive)));
	}

	/**
	 * Build a {@code custom} analyzer with the given tokenizer and (optional) filter chain.
	 * A null/empty {@code filterChain} produces a tokenizer-only analyzer (used by KEYWORD).
	 */
	private static Analyzer customAnalyzer(String tokenizer, java.util.List<String> filterChain) {
		return Analyzer.of(a -> a.custom(c -> {
			c.tokenizer(tokenizer);
			if (filterChain != null && !filterChain.isEmpty()) {
				c.filter(filterChain);
			}
			return c;
		}));
	}

	/**
	 * Wrap a TokenFilterDefinition variant ({@code stop}, {@code stemmer},
	 * {@code wordDelimiter}, etc.) in a {@link TokenFilter} that the
	 * {@link IndexSettingsAnalysis.Builder#filter(String, TokenFilter)} setter expects.
	 */
	private static TokenFilter filter(java.util.function.Function<TokenFilterDefinition.Builder,
			org.opensearch.client.util.ObjectBuilder<TokenFilterDefinition>> def) {
		TokenFilterDefinition d = TokenFilterDefinition.of(def);
		return TokenFilter.of(f -> f.definition(d));
	}

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

		textAnalyzerDao.createOrUpdateSystemAnalyzerForBootstrapOnly(SCIENTIFIC_ID, buildAnalyzer(
				"SCIENTIFIC",
				"English stemming, stop words, lowercase. Best for scientific metadata.",
				SCIENTIFIC_SETTINGS
		), organizationName, adminUserId);

		textAnalyzerDao.createOrUpdateSystemAnalyzerForBootstrapOnly(STANDARD_ID, buildAnalyzer(
				"STANDARD",
				"OpenSearch standard analyzer. Unicode segmentation with lowercase. General-purpose.",
				STANDARD_SETTINGS
		), organizationName, adminUserId);

		textAnalyzerDao.createOrUpdateSystemAnalyzerForBootstrapOnly(IDENTIFIER_ID, buildAnalyzer(
				"IDENTIFIER",
				"Preserves punctuation. Whitespace tokenization plus lowercase. Suitable for DOIs, RRIDs, PMIDs.",
				IDENTIFIER_SETTINGS
		), organizationName, adminUserId);

		textAnalyzerDao.createOrUpdateSystemAnalyzerForBootstrapOnly(KEYWORD_ID, buildAnalyzer(
				"KEYWORD",
				"No tokenization. Entire value is a single token. Suitable for facet and filter fields.",
				KEYWORD_SETTINGS
		), organizationName, adminUserId);

		textAnalyzerDao.createOrUpdateSystemAnalyzerForBootstrapOnly(AUTOCOMPLETE_ID, buildAnalyzer(
				"AUTOCOMPLETE",
				"Edge n-gram (2-20 chars) for type-ahead, with a non-ngram analyzer.default_search for search time.",
				AUTOCOMPLETE_SETTINGS
		), organizationName, adminUserId);
	}

	/**
	 * Serialize a typed analyzer to its persisted JSON form using
	 * {@link IndexSettingsAnalysis#toJsonString()} (default method on
	 * {@code PlainJsonSerializable}), then wrap as a {@link JSONObject} so the
	 * opaque-Object {@code settings} field carries a JSON object &mdash; not an encoded
	 * scalar string.
	 */
	private TextAnalyzer buildAnalyzer(String name, String description, IndexSettingsAnalysis settings) {
		return new TextAnalyzer()
				.setName(name)
				.setDescription(description)
				.setSettings(new JSONObject(settings.toJsonString()));
	}

}
