package com.linkwisehub.modules.base.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Organization {
    private Long id;
    private String name;
    private Long parentId;
    private Integer level;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
