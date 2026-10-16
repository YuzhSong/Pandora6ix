package com.pandora6ix.backend.application.identity;

public interface IdentityUseCases {
    java.util.Map<String, Object> login(com.pandora6ix.backend.api.Contracts.Login command);

    java.util.Map<String, Object> profile(
            com.pandora6ix.backend.service.IdentityService.Actor actor);

    void password(
            com.pandora6ix.backend.service.IdentityService.Actor actor,
            com.pandora6ix.backend.api.Contracts.Password command);

    void logout(com.pandora6ix.backend.service.IdentityService.Actor actor);
}
