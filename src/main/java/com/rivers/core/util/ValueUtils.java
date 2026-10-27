package com.rivers.core.util;

/**
 * <p>
 * 空值默认值工具：null 转为对应类型的零值/空串
 * </p>
 *
 * @author rivers
 * @since 2026-10-20
 */
public final class ValueUtils {

    private ValueUtils() {
    }

    public static String defaultString(String value) {
        if (value == null) {
            return "";
        }
        return value;
    }

    public static int defaultInt(Integer value) {
        if (value == null) {
            return 0;
        }
        return value;
    }

    public static long defaultLong(Long value) {
        if (value == null) {
            return 0L;
        }
        return value;
    }

    public static double defaultDouble(Double value) {
        if (value == null) {
            return 0.0;
        }
        return value;
    }
}
