package com.linkwisehub.modules.ai.document.service.impl;

import com.linkwisehub.modules.ai.document.dto.AiDocumentSplitConfigRespDto;
import com.linkwisehub.modules.ai.document.dto.AiDocumentSplitConfigSaveReqDto;
import com.linkwisehub.modules.ai.document.entity.AiDocumentSplitConfig;
import com.linkwisehub.modules.ai.document.mapper.AiDocumentSplitConfigMapper;
import com.linkwisehub.modules.ai.document.service.DocumentSplitConfigService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 文档分片配置服务，统一维护全局默认规则。
 */
@Service
public class DocumentSplitConfigServiceImpl implements DocumentSplitConfigService {

    private static final int DEFAULT_TARGET_CHUNK_LENGTH = 800;
    private static final boolean DEFAULT_SPLIT_BY_BLANK_LINE = true;
    private static final boolean DEFAULT_PRESERVE_MARKDOWN_TITLE = true;

    private final AiDocumentSplitConfigMapper splitConfigMapper;

    public DocumentSplitConfigServiceImpl(AiDocumentSplitConfigMapper splitConfigMapper) {
        this.splitConfigMapper = splitConfigMapper;
    }

    @Override
    public AiDocumentSplitConfigRespDto getConfig() {
        return toRespDto(getEffectiveConfig());
    }

    @Override
    @Transactional
    public AiDocumentSplitConfig getEffectiveConfig() {
        AiDocumentSplitConfig config = splitConfigMapper.selectActive();
        if (config != null) {
            return fillDefaults(config);
        }
        AiDocumentSplitConfig defaultConfig = buildDefaultConfig();
        splitConfigMapper.insertDefault(defaultConfig);
        return fillDefaults(splitConfigMapper.selectActive());
    }

    @Override
    @Transactional
    public AiDocumentSplitConfigRespDto saveConfig(AiDocumentSplitConfigSaveReqDto reqDto) {
        AiDocumentSplitConfig config = getEffectiveConfig();
        config.setTargetChunkLength(reqDto.getTargetChunkLength() == null ? DEFAULT_TARGET_CHUNK_LENGTH : reqDto.getTargetChunkLength());
        config.setSplitByBlankLine(reqDto.getSplitByBlankLine() == null ? DEFAULT_SPLIT_BY_BLANK_LINE : reqDto.getSplitByBlankLine());
        config.setPreserveMarkdownTitle(reqDto.getPreserveMarkdownTitle() == null ? DEFAULT_PRESERVE_MARKDOWN_TITLE : reqDto.getPreserveMarkdownTitle());
        splitConfigMapper.update(config);
        return toRespDto(fillDefaults(splitConfigMapper.selectActive()));
    }

    /**
     * 构建默认配置，保证没有初始化数据时接口仍可用。
     */
    private AiDocumentSplitConfig buildDefaultConfig() {
        AiDocumentSplitConfig config = new AiDocumentSplitConfig();
        config.setId(1L);
        config.setTargetChunkLength(DEFAULT_TARGET_CHUNK_LENGTH);
        config.setSplitByBlankLine(DEFAULT_SPLIT_BY_BLANK_LINE);
        config.setPreserveMarkdownTitle(DEFAULT_PRESERVE_MARKDOWN_TITLE);
        config.setStatus(1);
        return config;
    }

    /**
     * 兜底填充默认值，防止旧数据缺字段导致分片逻辑空指针。
     */
    private AiDocumentSplitConfig fillDefaults(AiDocumentSplitConfig config) {
        if (config == null) {
            return buildDefaultConfig();
        }
        if (config.getTargetChunkLength() == null) {
            config.setTargetChunkLength(DEFAULT_TARGET_CHUNK_LENGTH);
        }
        if (config.getSplitByBlankLine() == null) {
            config.setSplitByBlankLine(DEFAULT_SPLIT_BY_BLANK_LINE);
        }
        if (config.getPreserveMarkdownTitle() == null) {
            config.setPreserveMarkdownTitle(DEFAULT_PRESERVE_MARKDOWN_TITLE);
        }
        return config;
    }

    /**
     * 实体转响应对象，只返回前端配置页面需要的字段。
     */
    private AiDocumentSplitConfigRespDto toRespDto(AiDocumentSplitConfig config) {
        AiDocumentSplitConfigRespDto dto = new AiDocumentSplitConfigRespDto();
        dto.setId(config.getId());
        dto.setTargetChunkLength(config.getTargetChunkLength());
        dto.setSplitByBlankLine(config.getSplitByBlankLine());
        dto.setPreserveMarkdownTitle(config.getPreserveMarkdownTitle());
        dto.setUpdateTime(config.getUpdateTime());
        return dto;
    }
}
