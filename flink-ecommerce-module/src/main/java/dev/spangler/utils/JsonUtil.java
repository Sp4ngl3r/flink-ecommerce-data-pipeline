package dev.spangler.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.spangler.dto.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JsonUtil {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final Logger logger = LoggerFactory.getLogger(JsonUtil.class);

    public static String convertTransactionToJson(Transaction transaction) {
        try {
            return objectMapper.writeValueAsString(transaction);
        } catch (JsonProcessingException jpe) {
            logger.error("Failed to convert transaction into json", jpe);
            return null;
        }
    }
}
