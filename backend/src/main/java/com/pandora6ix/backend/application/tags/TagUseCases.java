package com.pandora6ix.backend.application.tags;

public interface TagUseCases {
    java.util.Map<String, Object> list(
            com.pandora6ix.backend.service.IdentityService.Actor actor,
            String name,
            String cursor,
            int limit);

    java.util.Map<String, Object> create(
            com.pandora6ix.backend.service.IdentityService.Actor actor,
            com.pandora6ix.backend.api.Contracts.TagCreate command);

    java.util.Map<String, Object> patch(
            com.pandora6ix.backend.service.IdentityService.Actor actor,
            String id,
            com.pandora6ix.backend.api.Contracts.TagPatch command);

    java.util.Map<String, Object> delete(
            com.pandora6ix.backend.service.IdentityService.Actor actor,
            String id);
}
