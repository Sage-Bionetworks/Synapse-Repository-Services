package org.sagebionetworks.repo.model.dbo.search;

import java.util.List;
import java.util.Optional;

import org.sagebionetworks.repo.model.table.search.TextAnalyzer;

public interface TextAnalyzerDao {

	TextAnalyzer create(TextAnalyzer analyzer, Long userId);

	Optional<TextAnalyzer> get(Long id);

	TextAnalyzer update(TextAnalyzer analyzer, Long userId);

	void delete(Long id);

	List<TextAnalyzer> listByOrganization(String organizationName, long limit, long offset);

	List<TextAnalyzer> listAll(long limit, long offset);

	boolean exists(Long id);

	void createOrUpdateSystemAnalyzerForBootstrapOnly(Long id, TextAnalyzer analyzer, String organizationName, Long userId);

	void truncateAll();
}
