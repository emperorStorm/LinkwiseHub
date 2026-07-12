package com.linkwisehub.modules.ai.service.impl;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class DeepSeekProviderTest {

    private final DeepSeekProvider provider = new DeepSeekProvider();

    @Test
    void parseStreamDataSupportsSseDataLineAndPlainJson() throws Exception {
        assertEquals("你好", parseSSEData("data: {\"choices\":[{\"delta\":{\"content\":\"你好\"}}]}"));
        assertEquals("世界", parseSSEData("{\"choices\":[{\"delta\":{\"content\":\"世界\"}}]}"));
        assertEquals("完成", parseSSEData("{\"choices\":[{\"message\":{\"content\":\"完成\"}}]}"));
        assertNull(parseSSEData("data: [DONE]"));
        assertNull(parseSSEData(": keep-alive"));
    }

    @Test
    void appendStreamChunkAccumulatesPlainJsonChunks() throws Exception {
        String chunk = """
            {"choices":[{"delta":{"content":"第一段"}}]}
            {"choices":[{"delta":{"content":"第二段"}}]}
            """;
        StringBuilder fullContent = new StringBuilder();
        List<String> callbacks = new ArrayList<>();

        Method method = DeepSeekProvider.class.getDeclaredMethod(
            "appendStreamChunk",
            String.class,
            StringBuilder.class,
            Consumer.class
        );
        method.setAccessible(true);
        method.invoke(provider, chunk, fullContent, (Consumer<String>) callbacks::add);

        assertEquals("第一段第二段", fullContent.toString());
        assertEquals(List.of("第一段", "第一段第二段"), callbacks);
    }

    private String parseSSEData(String line) throws Exception {
        Method method = DeepSeekProvider.class.getDeclaredMethod("parseSSEData", String.class);
        method.setAccessible(true);
        return (String) method.invoke(provider, line);
    }
}
