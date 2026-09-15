package com.rivers.core.entity;

import com.rivers.core.constant.SessionConstant;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * 会话信息：直接存 Redis session:{sid}（无 JWT），键名与 TTL 见 {@link SessionConstant}。
 * 宽限额度不在 JSON 中，由独立计数键（session:grace:...）维护。
 * <p>
 * 跨服务契约：user-server 登录时 {@link #create} 写入，网关读取并推动轮换（{@link #rotate}）；
 * 字段直接按名反序列化，变更需两侧同步，勿改字段名。
 *
 * @author riversking
 */
public record SessionInfo(String userId, String username, String familyId,
                          long createdAt, long rotateAt, String prevSid,
                          // 旧格式会话 JSON 缺失该字段，Jackson 反序列化会绑定为 null；
                          // 显式声明可空使静态契约与运行期一致（勿删调用处判空）
                          @Nullable List<String> prevSids) {

    /**
     * 登录时创建首个会话：rotateAt = now + 轮换周期，prevSid 为空串（兼容旧约定）
     */
    public static SessionInfo create(String userId, String username, String familyId) {
        var now = System.currentTimeMillis();
        return new SessionInfo(userId, username, familyId, now,
                now + SessionConstant.ROTATE_INTERVAL.toMillis(), "", null);
    }

    public LoginUser loginUser() {
        var u = new LoginUser();
        u.setUserId(userId);
        u.setUsername(username);
        return u;
    }

    /**
     * 轮换生成下一代会话：把被轮换掉的 sid 滚入代际窗口头部，截断到 REISSUE_WINDOW。
     * prevSids 为 null（旧格式会话首次轮换）时窗口退化为仅含该 sid。
     */
    public SessionInfo rotate(String prevSid) {
        var window = new ArrayList<String>(SessionConstant.REISSUE_WINDOW);
        window.add(prevSid);
        if (prevSids != null) {
            for (var sid : prevSids) {
                if (window.size() >= SessionConstant.REISSUE_WINDOW) {
                    break;
                }
                window.add(sid);
            }
        }
        return new SessionInfo(userId, username, familyId, createdAt,
                System.currentTimeMillis() + SessionConstant.ROTATE_INTERVAL.toMillis(),
                prevSid, List.copyOf(window));
    }

    /**
     * 本会话能否为 oldSid 担保（即在重发窗口内）。
     * prevSid 单独保留：一是兼容旧格式数据（prevSids 缺失时自动退化为旧行为），
     * 二是"落后恰好一代"是最高频场景，判定最直接。
     */
    public boolean vouchesFor(String oldSid) {
        return oldSid.equals(prevSid)
                || (prevSids != null && prevSids.contains(oldSid));
    }
}
