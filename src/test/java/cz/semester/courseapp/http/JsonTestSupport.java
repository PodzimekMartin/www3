package cz.semester.courseapp.http;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

final class JsonTestSupport {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private JsonTestSupport() {
    }

    static long idFrom(String json) {
        try {
            JsonNode node = OBJECT_MAPPER.readTree(json);
            return node.get("id").asLong();
        } catch (Exception exception) {
            throw new IllegalArgumentException("Cannot read id from JSON: " + json, exception);
        }
    }

    static String tokenFrom(String json) {
        try {
            JsonNode node = OBJECT_MAPPER.readTree(json);
            return node.get("token").asText();
        } catch (Exception exception) {
            throw new IllegalArgumentException("Cannot read token from JSON: " + json, exception);
        }
    }
}
