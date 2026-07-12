package com.linkwisehub.modules.ai.document.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ES 稀疏索引重建结果。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SparseIndexRebuildRespDto {
    private boolean enabled;
    private int indexedCount;
}
