package com.linkwisehub.modules.ai.document.service;

import java.io.InputStream;

public interface DocumentParseService {
    String parse(InputStream inputStream, String fileType);
}
