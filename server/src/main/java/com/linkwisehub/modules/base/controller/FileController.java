package com.linkwisehub.modules.base.controller;

import com.linkwisehub.modules.base.entity.File;
import com.linkwisehub.modules.base.service.FileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/base/file")
public class FileController {

    @Autowired
    private FileService fileService;

    @GetMapping("/list")
    public List<File> list(@RequestParam(required = false) String module) {
        if (module != null && !module.isEmpty()) {
            return fileService.getFilesByModule(module);
        }
        return fileService.getAllFiles();
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        File file = fileService.getFileById(id);
        if (file == null) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "文件不存在");
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(file);
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        try {
            fileService.deleteFile(id);
            result.put("success", true);
            result.put("message", "删除成功");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "删除失败：" + e.getMessage());
        }
        return result;
    }
}
