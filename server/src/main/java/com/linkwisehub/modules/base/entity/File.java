package com.linkwisehub.modules.base.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class File {
    private Long id;
    private String originalName;
    private String fileName;
    private String fileSuffix;
    private Long fileSize;
    private String contentType;
    private String filePath;
    private String module;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
