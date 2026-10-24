package com.pandora6ix.backend.application.dashboard;

public interface DashboardUseCases {
    java.util.Map<String, Object> dashboard(
            com.pandora6ix.backend.service.IdentityService.Actor actor);

    java.util.Map<String, Object> statistics(
            com.pandora6ix.backend.service.IdentityService.Actor actor);

    java.util.Map<String, Object> items(
            com.pandora6ix.backend.service.IdentityService.Actor actor, String cursor, int limit);

    java.util.Map<String, Object> item(
            com.pandora6ix.backend.service.IdentityService.Actor actor, String id);
}
