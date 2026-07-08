package com.asos;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies that every bundled learning module parses into valid LearningChunk
 * objects - a malformed module JSON would otherwise only fail at runtime when
 * the learner selects that course.
 */
class LearningModuleParsingTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @ParameterizedTest
    @ValueSource(strings = {
            "java-hello-world.json",
            "java-complete.json",
            "python-complete.json",
            "cpp-complete.json"
    })
    @DisplayName("All bundled learning modules parse into valid chunks")
    void moduleParsesIntoValidChunks(String moduleFile) throws Exception {
        InputStream inputStream =
                getClass().getResourceAsStream("/learning-modules/" + moduleFile);
        assertNotNull(inputStream, "Module resource missing: " + moduleFile);

        List<LearningChunk> chunks = objectMapper.readValue(
                inputStream, new TypeReference<List<LearningChunk>>() {});

        assertFalse(chunks.isEmpty(), moduleFile + " should contain at least one chunk");

        for (LearningChunk chunk : chunks) {
            assertTrue(chunk.getChunkId() > 0,
                    moduleFile + ": every chunk needs a positive chunkId");
            assertNotNull(chunk.getInstruction(),
                    moduleFile + ": chunk " + chunk.getChunkId() + " has no instruction");
            assertNotNull(chunk.getExpectedActions(),
                    moduleFile + ": chunk " + chunk.getChunkId() + " has no expected actions");
            assertFalse(chunk.getExpectedActions().isEmpty(),
                    moduleFile + ": chunk " + chunk.getChunkId() + " has empty expected actions");

            for (LearningChunk.ExpectedAction action : chunk.getExpectedActions()) {
                assertNotNull(action.getType(),
                        moduleFile + ": chunk " + chunk.getChunkId() + " has an action without a type");
                assertNotNull(action.getTarget(),
                        moduleFile + ": chunk " + chunk.getChunkId() + " has an action without a target");
            }
        }
    }
}
