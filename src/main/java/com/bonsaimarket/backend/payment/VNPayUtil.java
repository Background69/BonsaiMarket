package com.bonsaimarket.backend.payment;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class VNPayUtil {
    private VNPayUtil() {}

    public static String hmacSHA512(String key, String data) {
        try {
            Mac hmac = Mac.getInstance("HmacSHA512");
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
            hmac.init(secretKey);
            byte[] hash = hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(2 * hash.length);
            for (byte b : hash) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        } catch (Exception ex) {
            throw new IllegalStateException("Không thể tạo mã băm HMAC-SHA512", ex);
        }
    }

    public static String buildQueryUrl(Map<String, String> params, String secretKey, String baseUrl) {
        List<String> fieldNames = new ArrayList<>(params.keySet());
        Collections.sort(fieldNames);

        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();

        for (int i = 0; i < fieldNames.size(); i++) {
            String fieldName = fieldNames.get(i);
            String fieldValue = params.get(fieldName);
            if (fieldValue != null && !fieldValue.isEmpty()) {
                // Build hash data
                hashData.append(fieldName);
                hashData.append('=');
                hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));

                // Build query string
                query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII));
                query.append('=');
                query.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));

                if (i < fieldNames.size() - 1) {
                    query.append('&');
                    hashData.append('&');
                }
            }
        }

        String secureHash = hmacSHA512(secretKey, hashData.toString());
        return baseUrl + "?" + query.toString() + "&vnp_SecureHash=" + secureHash;
    }

    public static boolean verifyCallback(Map<String, String> fields, String secretKey) {
        String secureHash = fields.get("vnp_SecureHash");
        if (secureHash == null || secureHash.isEmpty()) {
            return false;
        }

        List<String> fieldNames = new ArrayList<>(fields.keySet());
        Collections.sort(fieldNames);

        StringBuilder hashData = new StringBuilder();
        for (int i = 0; i < fieldNames.size(); i++) {
            String fieldName = fieldNames.get(i);
            String fieldValue = fields.get(fieldName);
            if (!"vnp_SecureHash".equals(fieldName) && !"vnp_SecureHashType".equals(fieldName)
                    && fieldValue != null && !fieldValue.isEmpty()) {
                hashData.append(fieldName).append('=').append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));
                if (i < fieldNames.size() - 1) {
                    hashData.append('&');
                }
            }
        }

        // Strip any trailing &
        String raw = hashData.toString();
        if (raw.endsWith("&")) {
            raw = raw.substring(0, raw.length() - 1);
        }

        String calculatedHash = hmacSHA512(secretKey, raw);
        return calculatedHash.equalsIgnoreCase(secureHash);
    }
}
