package org.sagebionetworks.repo.manager.agent;

import org.json.JSONException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.manager.agent.parameter.ListParser;
import org.sagebionetworks.repo.model.EntityType;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ListParserTest {

    ListParser listParser;

    @BeforeEach
    public void before() {
        listParser = new ListParser();
    }

    @Test
    public void parseInvalidJsonArrayString() {
        String jsonArrayString = "\"folder\",\"file\"]";
        String resultMessage = assertThrows(JSONException.class, () -> {
            listParser.parse(jsonArrayString);
        }).getMessage();
        System.out.println(resultMessage);
        assertTrue(resultMessage.startsWith("A JSONArray text must start with"));
    }


    @Test
    public void parseJsonArrayString() {
        String jsonArrayString = "[\"folder\",\"file\"]";

        List<Object> result = listParser.parse(jsonArrayString);
        assertEquals(List.of(EntityType.folder, EntityType.file),
                result.stream().map(Objects::toString).map(EntityType::valueOf).collect(Collectors.toList()));
    }
}
