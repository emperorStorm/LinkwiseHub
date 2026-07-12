package com.linkwisehub.modules.ai.document.service.impl;

import com.linkwisehub.modules.ai.document.entity.AiDocumentChunk;
import com.linkwisehub.modules.ai.document.entity.AiDocumentSplitConfig;
import com.linkwisehub.modules.ai.document.service.TextSplitService;

import java.util.ArrayList;
import java.util.List;

/**
 * 文本分片服务，按段落聚合成适合预览和后续检索的 Chunk。
 * 已由 DocumentRagIndexServiceImpl 接管运行时切片流程，仅保留作为旧逻辑参考。
 */
public class TextSplitServiceImpl implements TextSplitService {

    private static final int TARGET_CHUNK_LENGTH = 800;

    @Override
    public List<AiDocumentChunk> split(Long documentId, String fileName, String fileType, String text, AiDocumentSplitConfig config) {
        List<AiDocumentChunk> chunks = new ArrayList<>();
        if (text == null || text.trim().isEmpty()) {
            return chunks;
        }
        int targetLength = getTargetLength(config);
        boolean splitByBlankLine = config == null || Boolean.TRUE.equals(config.getSplitByBlankLine());
        boolean preserveMarkdownTitle = config == null || Boolean.TRUE.equals(config.getPreserveMarkdownTitle());

        if (!splitByBlankLine) {
            splitByFixedLength(documentId, fileName, fileType, text.trim(), chunks, targetLength, preserveMarkdownTitle);
            return chunks;
        }

        String currentTitle = fileName;
        StringBuilder buffer = new StringBuilder();
        int chunkIndex = 1;
        int paragraphIndex = 0;
        int chunkStartParagraph = 1;

        String[] paragraphs = text.split("\\n\\s*\\n|\\n");
        for (String rawParagraph : paragraphs) {
            String paragraph = rawParagraph.trim();
            if (paragraph.isEmpty()) {
                continue;
            }
            paragraphIndex++;

            // Markdown 标题作为后续 Chunk 的来源标题，同时保留在正文里。
            if (preserveMarkdownTitle && "md".equals(fileType) && paragraph.startsWith("#")) {
                currentTitle = paragraph.replaceFirst("^#+\\s*", "").trim();
                if (currentTitle.isEmpty()) {
                    currentTitle = fileName;
                }
            }

            if (buffer.length() > 0 && buffer.length() + paragraph.length() + 2 > targetLength) {
                chunks.add(buildChunk(documentId, chunkIndex++, buffer.toString(), currentTitle, chunkStartParagraph, fileName, fileType));
                buffer.setLength(0);
                chunkStartParagraph = paragraphIndex;
            }

            if (buffer.length() > 0) {
                buffer.append("\n\n");
            }
            buffer.append(paragraph);
        }

        if (buffer.length() > 0) {
            chunks.add(buildChunk(documentId, chunkIndex, buffer.toString(), currentTitle, chunkStartParagraph, fileName, fileType));
        }
        return chunks;
    }

    /**
     * 不按段落切分时，直接按目标长度固定截断，适合对纯文本做稳定预览。
     */
    private void splitByFixedLength(Long documentId, String fileName, String fileType, String text,
                                    List<AiDocumentChunk> chunks, int targetLength, boolean preserveMarkdownTitle) {
        String currentTitle = fileName;
        int chunkIndex = 1;
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + targetLength, text.length());
            String content = text.substring(start, end).trim();
            if (!content.isEmpty()) {
                if (preserveMarkdownTitle && "md".equals(fileType)) {
                    currentTitle = findLatestMarkdownTitle(content, currentTitle, fileName);
                }
                chunks.add(buildChunk(documentId, chunkIndex++, content, currentTitle, chunkIndex - 1, fileName, fileType));
            }
            start = end;
        }
    }

    /**
     * 查找当前片段内最后一个 Markdown 标题，作为后续固定长度片段的来源标题。
     */
    private String findLatestMarkdownTitle(String content, String currentTitle, String fileName) {
        String latestTitle = currentTitle;
        String[] lines = content.split("\\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("#")) {
                latestTitle = trimmed.replaceFirst("^#+\\s*", "").trim();
                if (latestTitle.isEmpty()) {
                    latestTitle = fileName;
                }
            }
        }
        return latestTitle;
    }

    /**
     * 获取目标长度，防止异常配置影响分片稳定性。
     */
    private int getTargetLength(AiDocumentSplitConfig config) {
        if (config == null || config.getTargetChunkLength() == null) {
            return TARGET_CHUNK_LENGTH;
        }
        return Math.max(200, Math.min(2000, config.getTargetChunkLength()));
    }

    /**
     * 构建 Chunk 实体，集中维护来源信息和元数据。
     */
    private AiDocumentChunk buildChunk(Long documentId, Integer chunkIndex, String content, String sourceTitle,
                                       Integer sourceParagraph, String fileName, String fileType) {
        AiDocumentChunk chunk = new AiDocumentChunk();
        chunk.setDocumentId(documentId);
        chunk.setChunkIndex(chunkIndex);
        chunk.setContent(content);
        chunk.setContentLength(content.length());
        chunk.setSourceTitle(sourceTitle);
        chunk.setSourcePage(1);
        chunk.setSourceParagraph(sourceParagraph);
        chunk.setMetadataJson("{\"fileName\":\"" + escapeJson(fileName) + "\",\"fileType\":\"" + escapeJson(fileType) + "\"}");
        chunk.setStatus(1);
        return chunk;
    }

    /**
     * 简单转义元数据中的 JSON 特殊字符。
     */
    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
