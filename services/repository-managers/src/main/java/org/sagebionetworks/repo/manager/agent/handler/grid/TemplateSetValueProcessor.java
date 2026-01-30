package org.sagebionetworks.repo.manager.agent.handler.grid;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONObject;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.RowView;
import org.sagebionetworks.repo.model.grid.patch.ConType;
import org.sagebionetworks.repo.model.grid.patch.ConValue;
import org.sagebionetworks.repo.model.grid.update.OnMatchFailure;
import org.sagebionetworks.repo.model.grid.update.OnMissingValue;
import org.sagebionetworks.repo.model.grid.update.TemplateSetValue;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.stereotype.Service;

@Service
public class TemplateSetValueProcessor implements SetValueProcessor<TemplateSetValue> {

	private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{([^}]+)\\}");

	@Override
	public Optional<ConValue> createConValue(RowView row, TemplateSetValue sv, JSONObject rawSetValue) {
		ValidateArgument.required(row, "row");
		ValidateArgument.required(sv, "TemplateSetValue");
		ValidateArgument.required(sv.getSourceTemplate(), "TemplateSetValue.sourceTemplate");

		JSONObject rowValue = row.getRowObject().getData().getRowJsonDocument();

		Matcher matcher = PLACEHOLDER_PATTERN.matcher(sv.getSourceTemplate());
		StringBuffer result = new StringBuffer();

		while (matcher.find()) {
			String columnName = matcher.group(1);
			if (!rowValue.isNull(columnName)) {
				matcher.appendReplacement(result, Matcher.quoteReplacement(rowValue.get(columnName).toString()));
			} else {
				OnMissingValue onMissing = sv.getOnMissingValue() != null ? sv.getOnMissingValue()
						: OnMissingValue.SET_NULL;

				switch (onMissing) {
				case SET_NULL:
					return Optional.of(new ConValue(ConType.NULL, null));
				case SET_UNDEFINED:
					return Optional.of(new ConValue(ConType.UNDEFINED, null));
				case SKIP_UPDATE:
					return Optional.empty();
				case USE_EMPTY_STRING:
					matcher.appendReplacement(result, "");
					break;
				}
			}
		}
		matcher.appendTail(result);
		String replacedTemplate = result.toString();

		if (sv.getPattern() != null) {
			Matcher patternMatcher = Pattern.compile(sv.getPattern()).matcher(replacedTemplate);
			String replacement = sv.getReplacement() != null ? sv.getReplacement() : "$1";

			if (patternMatcher.find()) {
				StringBuffer patternResult = new StringBuffer();
				patternMatcher.appendReplacement(patternResult, replacement);
				patternMatcher.appendTail(patternResult);
				replacedTemplate = patternResult.toString();
			} else {
				// Pattern did not match
				OnMatchFailure onFailure = sv.getOnMatchFailure() != null ? sv.getOnMatchFailure()
						: OnMatchFailure.SET_NULL;

				switch (onFailure) {
				case SET_NULL:
					return Optional.of(new ConValue(ConType.NULL, null));
				case SET_UNDEFINED:
					return Optional.of(new ConValue(ConType.UNDEFINED, null));
				case SKIP_UPDATE:
					return Optional.empty();
				}
			}
		}

		return Optional.of(ConValue.fromString(replacedTemplate));
	}

	@Override
	public Class<? extends TemplateSetValue> getSetValueClass() {
		return TemplateSetValue.class;
	}
}
