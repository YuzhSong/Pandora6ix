package com.pandora6ix.backend.controller;

import com.pandora6ix.backend.api.Contracts;
import com.pandora6ix.backend.service.DirectoryService;
import com.pandora6ix.backend.service.IdentityService;

import jakarta.validation.Valid;

import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

/**
 * 用户头像：客户端压缩后的图片以 data URL 存取（服务端不落地文件）。
 * 读取要求同企业（审批详情需要显示请求人/审批人/执行人的真实头像）；写入限本人或管理员。
 */
@RestController
@RequestMapping("/api/v1")
public class AvatarController {
    private final IdentityService identity;
    private final DirectoryService directory;

    public AvatarController(IdentityService identity, DirectoryService directory) {
        this.identity = identity;
        this.directory = directory;
    }

    /** 未设置头像返回 404，客户端回退到姓名首字占位 */
    @GetMapping("/users/{id}/avatar")
    public ResponseEntity<byte[]> avatar(@PathVariable String id) {
        var image = directory.avatar(identity.current(), id);
        if (image == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok()
                .contentType(MediaType.valueOf(String.valueOf(image.get("mime"))))
                .cacheControl(CacheControl.maxAge(Duration.ofHours(1)))
                .body((byte[]) image.get("bytes"));
    }

    @PutMapping("/users/{id}/avatar")
    public ResponseEntity<Void> save(
            @PathVariable String id, @Valid @RequestBody Contracts.UserAvatar c) {
        directory.saveAvatar(identity.current(), id, c);
        return ResponseEntity.noContent().build();
    }
}
