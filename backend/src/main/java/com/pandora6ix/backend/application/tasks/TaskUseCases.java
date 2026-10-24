package com.pandora6ix.backend.application.tasks;

public interface TaskUseCases {
    java.util.Map<String, Object> create(
            com.pandora6ix.backend.service.IdentityService.Actor actor,
            com.pandora6ix.backend.api.Contracts.TaskCreate command);

    java.util.Map<String, Object> detail(
            com.pandora6ix.backend.service.IdentityService.Actor actor, String id);

    java.util.Map<String, Object> progress(
            com.pandora6ix.backend.service.IdentityService.Actor actor,
            String id,
            com.pandora6ix.backend.api.Contracts.Progress command);

    java.util.Map<String, Object> complete(
            com.pandora6ix.backend.service.IdentityService.Actor actor,
            String id,
            com.pandora6ix.backend.api.Contracts.Complete command);

    java.util.Map<String, Object> requestCompletion(
            com.pandora6ix.backend.service.IdentityService.Actor actor,
            String id,
            com.pandora6ix.backend.api.Contracts.CompletionRequest command);

    java.util.Map<String, Object> decide(
            com.pandora6ix.backend.service.IdentityService.Actor actor,
            String id,
            com.pandora6ix.backend.api.Contracts.Decision command);
}
