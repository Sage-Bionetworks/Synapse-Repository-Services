package org.sagebionetworks.repo.manager.table;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.sagebionetworks.repo.model.FacetPostProcessingAlgorithm;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Resolves the {@link FacetPostProcessor} bound to a {@link FacetPostProcessingAlgorithm}. Spring
 * supplies every registered processor bean as a {@link List}, which is indexed by algorithm here so
 * a new algorithm is wired simply by adding a {@link FacetPostProcessor} bean.
 */
@Service
public class FacetPostProcessorProvider {

	private final Map<FacetPostProcessingAlgorithm, FacetPostProcessor> processors;

	@Autowired
	public FacetPostProcessorProvider(List<FacetPostProcessor> processors) {
		this.processors = processors.stream()
				.collect(Collectors.toMap(FacetPostProcessor::getAlgorithm, Function.identity()));
	}

	/**
	 * @param algorithm The algorithm to resolve a processor for.
	 * @return The processor that realizes the given algorithm.
	 * @throws IllegalArgumentException If no processor is registered for the algorithm.
	 */
	public FacetPostProcessor getProcessor(FacetPostProcessingAlgorithm algorithm) {
		ValidateArgument.required(algorithm, "algorithm");
		FacetPostProcessor processor = processors.get(algorithm);
		ValidateArgument.requirement(processor != null, "No FacetPostProcessor is registered for algorithm: " + algorithm);
		return processor;
	}

}
