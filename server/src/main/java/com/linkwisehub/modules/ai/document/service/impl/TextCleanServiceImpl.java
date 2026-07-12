package com.linkwisehub.modules.ai.document.service.impl;

import com.linkwisehub.modules.ai.document.service.TextCleanService;
import org.springframework.stereotype.Service;

/**
 * 文本清洗服务，保留 Markdown 结构但压缩无意义空白。
 */
@Service
public class TextCleanServiceImpl implements TextCleanService {

    @Override
    public String clean(String text) {
        if (text == null) {
            return "";
        }
        String normalized = text
                .replace("\uFEFF", "")
                .replace("\r\n", "\n")
                .replace('\r', '\n')
                .replace('\u00A0', ' ');
        // 保留段落边界，避免多空行造成空 Chunk。
        return normalized
                .replaceAll("[\\t ]+", " ")
                .replaceAll("(?m)^\\s+$", "")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }
}
