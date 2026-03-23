package org.sagebionetworks.repo.manager.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.schema.SynapseSchemaBootstrap;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dbo.schema.OrganizationDao;
import org.sagebionetworks.repo.model.dbo.search.TextAnalyzerDao;
import org.sagebionetworks.repo.model.schema.Organization;
import org.sagebionetworks.repo.model.table.search.TextAnalyzer;
import org.sagebionetworks.repo.model.table.search.TextAnalyzerSettings;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class TextAnalyzerBootstrapperTest {

	private static final String TEST_ORG_NAME = "sage.bionetworks";

	@Mock
	private TextAnalyzerDao textAnalyzerDao;

	@Mock
	private OrganizationDao organizationDao;

	@Mock
	private SynapseSchemaBootstrap synapseSchemaBootstrap;

	@Mock
	private UserManager userManager;

	private TextAnalyzerBootstrapper bootstrapper;

	@Captor
	private ArgumentCaptor<TextAnalyzer> analyzerCaptor;

	private void setupOrgMock() {
		Organization org = new Organization();
		org.setId("100");
		org.setName(TEST_ORG_NAME);
		when(synapseSchemaBootstrap.createOrganizationIfDoesNotExist(any())).thenReturn(org);
		when(userManager.getUserInfo(AuthorizationConstants.BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId()))
				.thenReturn(new UserInfo(true));
		bootstrapper = new TextAnalyzerBootstrapper(textAnalyzerDao, synapseSchemaBootstrap, userManager);
	}

	@Test
	public void testBootstrapCreatesAllSixSystemAnalyzers() {
		setupOrgMock();

		verify(textAnalyzerDao).createOrUpdateSystemAnalyzerForBootstrapOnly(eq(TextAnalyzerBootstrapper.SCIENTIFIC_ID), analyzerCaptor.capture(), eq(TEST_ORG_NAME), any(Long.class));
		verify(textAnalyzerDao).createOrUpdateSystemAnalyzerForBootstrapOnly(eq(TextAnalyzerBootstrapper.STANDARD_ID), analyzerCaptor.capture(), eq(TEST_ORG_NAME), any(Long.class));
		verify(textAnalyzerDao).createOrUpdateSystemAnalyzerForBootstrapOnly(eq(TextAnalyzerBootstrapper.IDENTIFIER_ID), analyzerCaptor.capture(), eq(TEST_ORG_NAME), any(Long.class));
		verify(textAnalyzerDao).createOrUpdateSystemAnalyzerForBootstrapOnly(eq(TextAnalyzerBootstrapper.KEYWORD_ID), analyzerCaptor.capture(), eq(TEST_ORG_NAME), any(Long.class));
		verify(textAnalyzerDao).createOrUpdateSystemAnalyzerForBootstrapOnly(eq(TextAnalyzerBootstrapper.AUTOCOMPLETE_ID), analyzerCaptor.capture(), eq(TEST_ORG_NAME), any(Long.class));
		verify(textAnalyzerDao).createOrUpdateSystemAnalyzerForBootstrapOnly(eq(TextAnalyzerBootstrapper.AUTOCOMPLETE_SEARCH_ID), analyzerCaptor.capture(), eq(TEST_ORG_NAME), any(Long.class));
	}

	@Test
	public void testScientificAnalyzerHasWordDelimiterGraph() {
		setupOrgMock();

		verify(textAnalyzerDao).createOrUpdateSystemAnalyzerForBootstrapOnly(eq(TextAnalyzerBootstrapper.SCIENTIFIC_ID), analyzerCaptor.capture(), eq(TEST_ORG_NAME), any(Long.class));
		TextAnalyzerSettings settings = analyzerCaptor.getValue().getSettings();

		assertEquals("standard", settings.getTokenizer());
		assertWordDelimiterGraphFilter(settings.getTokenFilters(), "sci_word_delimiter");
		assertEquals(Arrays.asList("sci_word_delimiter", "lowercase", "english_stop", "english_stemmer"),
				settings.getFilterOrder());
		assertTrue(settings.getSynonymAware());
	}

	@Test
	public void testStandardAnalyzerHasWordDelimiterGraph() {
		setupOrgMock();

		verify(textAnalyzerDao).createOrUpdateSystemAnalyzerForBootstrapOnly(eq(TextAnalyzerBootstrapper.STANDARD_ID), analyzerCaptor.capture(), eq(TEST_ORG_NAME), any(Long.class));
		TextAnalyzerSettings settings = analyzerCaptor.getValue().getSettings();

		assertEquals("standard", settings.getTokenizer());
		assertWordDelimiterGraphFilter(settings.getTokenFilters(), "std_word_delimiter");
		assertEquals(Arrays.asList("std_word_delimiter", "lowercase"), settings.getFilterOrder());
		assertTrue(settings.getSynonymAware());
	}

	@Test
	public void testIdentifierAnalyzerHasWordDelimiterGraph() {
		setupOrgMock();

		verify(textAnalyzerDao).createOrUpdateSystemAnalyzerForBootstrapOnly(eq(TextAnalyzerBootstrapper.IDENTIFIER_ID), analyzerCaptor.capture(), eq(TEST_ORG_NAME), any(Long.class));
		TextAnalyzerSettings settings = analyzerCaptor.getValue().getSettings();

		assertEquals("whitespace", settings.getTokenizer());
		assertWordDelimiterGraphFilter(settings.getTokenFilters(), "id_word_delimiter");
		assertEquals(Arrays.asList("id_word_delimiter", "lowercase"), settings.getFilterOrder());
		assertTrue(settings.getSynonymAware());
	}

	@Test
	public void testAutocompleteAnalyzerHasWordDelimiterGraph() {
		setupOrgMock();

		verify(textAnalyzerDao).createOrUpdateSystemAnalyzerForBootstrapOnly(eq(TextAnalyzerBootstrapper.AUTOCOMPLETE_ID), analyzerCaptor.capture(), eq(TEST_ORG_NAME), any(Long.class));
		TextAnalyzerSettings settings = analyzerCaptor.getValue().getSettings();

		assertEquals("standard", settings.getTokenizer());
		assertWordDelimiterGraphFilter(settings.getTokenFilters(), "ac_word_delimiter");
		List<String> filterOrder = settings.getFilterOrder();
		assertEquals("ac_word_delimiter", filterOrder.get(0));
		assertEquals("lowercase", filterOrder.get(1));
		assertEquals("edge_ngram_filter", filterOrder.get(2));
	}

	@Test
	public void testAutocompleteSearchAnalyzerHasWordDelimiterGraph() {
		setupOrgMock();

		verify(textAnalyzerDao).createOrUpdateSystemAnalyzerForBootstrapOnly(eq(TextAnalyzerBootstrapper.AUTOCOMPLETE_SEARCH_ID), analyzerCaptor.capture(), eq(TEST_ORG_NAME), any(Long.class));
		TextAnalyzerSettings settings = analyzerCaptor.getValue().getSettings();

		assertEquals("standard", settings.getTokenizer());
		assertWordDelimiterGraphFilter(settings.getTokenFilters(), "acs_word_delimiter");
		assertEquals(Arrays.asList("acs_word_delimiter", "lowercase"), settings.getFilterOrder());
		assertTrue(settings.getSynonymAware());
	}

	@Test
	public void testWordDelimiterGraphComesBeforeLowercaseInAllAnalyzers() {
		setupOrgMock();

		// Verify that word_delimiter_graph is always first in filter order (before lowercase)
		// so that case-change boundaries can be detected
		List<Long> idsWithWordDelimiter = Arrays.asList(
				TextAnalyzerBootstrapper.SCIENTIFIC_ID,
				TextAnalyzerBootstrapper.STANDARD_ID,
				TextAnalyzerBootstrapper.IDENTIFIER_ID,
				TextAnalyzerBootstrapper.AUTOCOMPLETE_ID,
				TextAnalyzerBootstrapper.AUTOCOMPLETE_SEARCH_ID
		);

		for (Long id : idsWithWordDelimiter) {
			verify(textAnalyzerDao).createOrUpdateSystemAnalyzerForBootstrapOnly(eq(id), analyzerCaptor.capture(), eq(TEST_ORG_NAME), any(Long.class));
			TextAnalyzerSettings settings = analyzerCaptor.getValue().getSettings();
			List<String> order = settings.getFilterOrder();
			int wdIdx = -1;
			int lcIdx = -1;
			for (int i = 0; i < order.size(); i++) {
				if (order.get(i).contains("word_delimiter")) wdIdx = i;
				if ("lowercase".equals(order.get(i))) lcIdx = i;
			}
			assertTrue(wdIdx >= 0, "word_delimiter_graph filter missing for analyzer ID " + id);
			assertTrue(lcIdx >= 0, "lowercase filter missing for analyzer ID " + id);
			assertTrue(wdIdx < lcIdx, "word_delimiter_graph must come before lowercase for analyzer ID " + id);
		}
	}

	private void assertWordDelimiterGraphFilter(String tokenFiltersJson, String filterName) {
		assertNotNull(tokenFiltersJson, "tokenFilters should not be null");
		assertTrue(tokenFiltersJson.contains("\"" + filterName + "\""),
				"Expected token filter '" + filterName + "' not found in JSON");
		assertTrue(tokenFiltersJson.contains("\"type\":\"word_delimiter_graph\""),
				"Filter should be of type word_delimiter_graph");
		assertTrue(tokenFiltersJson.contains("\"preserve_original\":true"),
				"Filter should have preserve_original=true");
		assertTrue(tokenFiltersJson.contains("\"split_on_case_change\":true"),
				"Filter should have split_on_case_change=true");
		assertTrue(tokenFiltersJson.contains("\"catenate_words\":true"),
				"Filter should have catenate_words=true");
	}
}
