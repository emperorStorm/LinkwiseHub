package com.linkwisehub.modules.ai.document.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Qdrant 向量索引重建结果。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VectorIndexRebuildRespDto {
    private int indexedCount;
    private int skippedCount;
}
