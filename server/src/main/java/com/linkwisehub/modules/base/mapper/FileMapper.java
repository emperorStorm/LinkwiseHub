package com.linkwisehub.modules.base.mapper;

import com.linkwisehub.modules.base.entity.File;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface FileMapper {
    List<File> selectAll();
    List<File> selectByModule(@Param("module") String module);
    File selectById(@Param("id") Long id);
    int insert(File file);
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);
    int delete(@Param("id") Long id);
}
