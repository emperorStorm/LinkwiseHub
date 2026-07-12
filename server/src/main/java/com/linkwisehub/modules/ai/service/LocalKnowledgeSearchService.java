package com.linkwisehub.modules.ai.service;

import java.util.List;

public interface LocalKnowledgeSearchService {

    List<SearchResult> search(String question);

    class SearchResult {
        private final Long chunkId;
        private final String title;
        private final String content;

        public SearchResult(String title, String content) {
            this(null, title, content);
        }

        public SearchResult(Long chunkId, String title, String content) {
            this.chunkId = chunkId;
            this.title = title;
            this.content = content;
        }

        public Long getChunkId() {
            return chunkId;
        }

        public String getTitle() {
            return title;
        }

        public String getContent() {
            return content;
        }
    }
}
