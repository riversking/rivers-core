package com.rivers.core.constant;

import java.time.Duration;

/**
 * 统一会话协议常量 — 跨服务契约（user-server 创建，gateway-server 轮换 / 宽限 / 吊销）
 * <p>
 * session:{sid}                  TTL=30d → 会话 JSON（结构见 {@link com.rivers.core.entity.SessionInfo}）
 * session:family:{familyId}      TTL=30d → 当前有效 sid（家族指针，只由墓碑赢家写入）
 * rotated:{oldSid}               TTL=30d → 墓碑（值为 familyId），旧 sid 重放检测
 * session:last:{sid}             TTL=2d  → 活跃窗口，每请求续期；未命中视为不活跃登出
 * session:grace:{sid}:{oldSid}   TTL=2d  → 宽限计数窗口，首次计数设置 TTL，窗口内限次补发
 *
 * @author riversking
 */
public final class SessionConstant {

    /**
     * 会话绝对期限（30 天，不续期）
     */
    public static final Duration SESSION_TTL = Duration.ofDays(30);
    /**
     * 活跃窗口期限：2 天不活跃视为休眠，重放自愈路径拒绝
     */
    public static final Duration ACTIVE_TTL = Duration.ofDays(2);
    /**
     * 会话轮换周期：rotateAt 到期后以"墓碑 SETNX"抢占轮换权
     */
    public static final Duration ROTATE_INTERVAL = Duration.ofHours(12);
    /**
     * 重发窗口：落后不超过 REISSUE_WINDOW 代的陈旧 sid 可经宽限补发自愈；
     * 超出窗口只拒绝、不整族吊销。窗口同时也是被盗旧令牌的"升级上限"，
     * 追求更严防盗用可调小（1 代 = 旧行为），追求多端自愈可保持 3。
     */
    public static final int REISSUE_WINDOW = 3;
    public static final String BEARER_PREFIX = "Bearer ";
    public static final String NEW_SESSION_HEADER = "X-New-Session";
    public static final String ACTIVE_VALUE = "1";

    private SessionConstant() {
    }

    public static String session(String sessionId) {
        return "session:" + sessionId;
    }

    public static String active(String sessionId) {
        return "session:last:" + sessionId;
    }

    public static String family(String familyId) {
        return "session:family:" + familyId;
    }

    public static String rotated(String sessionId) {
        return "rotated:" + sessionId;
    }

    public static String grace(String currentSid, String oldSid) {
        return "session:grace:" + currentSid + ":" + oldSid;
    }
}
