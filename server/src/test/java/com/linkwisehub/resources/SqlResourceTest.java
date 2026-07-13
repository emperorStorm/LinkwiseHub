package com.linkwisehub.resources;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SqlResourceTest {

    private static final Path RESOURCE_DIR = Path.of("src/main/resources");

    @Test
    void onlyKeepsInitAndTestDataSqlScripts() throws IOException {
        List<String> sqlFiles;
        try (var paths = Files.walk(RESOURCE_DIR)) {
            sqlFiles = paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".sql"))
                    .map(RESOURCE_DIR::relativize)
                    .map(Path::toString)
                    .sorted()
                    .toList();
        }

        assertEquals(List.of("init.sql", "test_data.sql"), sqlFiles);
    }

    @Test
    void initSqlContainsKnowledgeCategoryTable() throws IOException {
        String initSql = Files.readString(RESOURCE_DIR.resolve("init.sql"));

        assertTrue(initSql.contains("CREATE TABLE IF NOT EXISTS lwh_ai_knowledge_category"));
        assertTrue(initSql.contains("CREATE TABLE IF NOT EXISTS lwh_ai_document"));
        assertTrue(initSql.contains("CREATE TABLE IF NOT EXISTS lwh_ai_document_chunk"));
        assertFalse(initSql.contains("INSERT INTO lwh_ai_knowledge_category"));
    }

    @Test
    void testDataSqlContainsKnowledgeCategorySeedData() throws IOException {
        String testDataSql = Files.readString(RESOURCE_DIR.resolve("test_data.sql"));

        assertTrue(testDataSql.contains("INSERT IGNORE INTO lwh_ai_knowledge_category"));
        assertFalse(testDataSql.contains("CREATE TABLE"));
    }
}
