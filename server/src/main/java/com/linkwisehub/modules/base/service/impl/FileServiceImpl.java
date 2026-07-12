package com.linkwisehub.modules.base.service.impl;

import com.linkwisehub.modules.base.entity.File;
import com.linkwisehub.modules.base.mapper.FileMapper;
import com.linkwisehub.modules.base.service.FileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class FileServiceImpl implements FileService {

    @Autowired
    private FileMapper fileMapper;

    @Override
    public List<File> getAllFiles() {
        return fileMapper.selectAll();
    }

    @Override
    public List<File> getFilesByModule(String module) {
        return fileMapper.selectByModule(module);
    }

    @Override
    public File getFileById(Long id) {
        return fileMapper.selectById(id);
    }

    @Override
    public Long saveFile(File file) {
        if (file.getStatus() == null) {
            file.setStatus(1);
        }
        fileMapper.insert(file);
        return file.getId();
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        fileMapper.updateStatus(id, status);
    }

    @Override
    public void deleteFile(Long id) {
        fileMapper.updateStatus(id, 0);
    }
}
