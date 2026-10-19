package com.pandora6ix.backend.controller;

import com.pandora6ix.backend.service.IdentityService;
import com.pandora6ix.backend.service.NotificationService;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class NotificationController {
    private final IdentityService identity;
    private final NotificationService notifications;

    public NotificationController(IdentityService identity, NotificationService notifications) {
        this.identity = identity;
        this.notifications = notifications;
    }

    /** 本人提醒列表；游标绑定操作者与企业，倒序返回 */
    @GetMapping("/notifications")
    public Object list(
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int limit) {
        return notifications.list(identity.current(), cursor, limit);
    }

    @GetMapping("/notifications/unread-count")
    public Object unreadCount() {
        return notifications.unreadCount(identity.current());
    }

    /** 幂等标记已读；非本人与其他企业返回 404 */
    @PostMapping("/notifications/{id}/read")
    public Object read(@PathVariable String id) {
        return notifications.read(identity.current(), id);
    }
}
