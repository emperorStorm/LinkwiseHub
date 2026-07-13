package com.linkwisehub.modules.ai.document.enums;

import com.linkwisehub.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentParseStrategyTest {

    @Test
    void autoRoutesModernDocumentsAndImagesToMineru() {
        assertTrue(DocumentParseStrategy.AUTO.usesMineru("pdf"));
        assertTrue(DocumentParseStrategy.AUTO.usesMineru("DOCX"));
        assertTrue(DocumentParseStrategy.AUTO.usesMineru("png"));
        assertFalse(DocumentParseStrategy.AUTO.usesMineru("txt"));
        assertFalse(DocumentParseStrategy.LEGACY.usesMineru("pdf"));
    }

    @Test
    void resolveRejectsUnknownStrategy() {
        assertThrows(BusinessException.class, () -> DocumentParseStrategy.resolve("unknown", DocumentParseStrategy.LEGACY));
    }
}
