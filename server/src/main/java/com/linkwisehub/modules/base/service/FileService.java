package com.linkwisehub.modules.base.service;

import com.linkwisehub.modules.base.entity.File;
import java.util.List;

public interface FileService {
    List<File> getAllFiles();
    List<File> getFilesByModule(String module);
    File getFileById(Long id);
    Long saveFile(File file);
    void updateStatus(Long id, Integer status);
    void deleteFile(Long id);
}
