package com.linkwisehub.modules.ai.document.support;

import com.linkwisehub.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentFileTypeTest {

    @Test
    void resolveSupportedTypes() {
        String[] fileTypes = {"txt", "md", "doc", "docx", "ppt", "pptx", "xls", "xlsx", "pdf"};
        for (String fileType : fileTypes) {
            assertEquals(fileType, DocumentFileType.resolveFileType("sample." + fileType));
            assertTrue(DocumentFileType.isSupported(fileType));
        }
        assertEquals("docx", DocumentFileType.resolveFileType("SAMPLE.DOCX"));
    }

    @Test
    void rejectUnsupportedTypes() {
        assertThrows(BusinessException.class, () -> DocumentFileType.resolveFileType("sample.exe"));
        assertThrows(BusinessException.class, () -> DocumentFileType.resolveFileType("sample"));
        assertThrows(BusinessException.class, () -> DocumentFileType.resolveFileType(null));
    }
}
