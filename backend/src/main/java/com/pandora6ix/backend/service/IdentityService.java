package com.pandora6ix.backend.service;

import static com.pandora6ix.backend.repository.BusinessDb.*;

import com.pandora6ix.backend.api.*;
import com.pandora6ix.backend.repository.BusinessDb;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.*;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.util.*;

@Service
public class IdentityService
        implements com.pandora6ix.backend.application.identity.IdentityUseCases {
    public static final String ACTOR_ATTRIBUTE = IdentityService.class.getName() + ".actor";
    private final BusinessDb db;
    private final CredentialHandoffService handoffs;
    private final String bootstrap;
    private final tools.jackson.databind.json.JsonMapper json;
    private final BCryptPasswordEncoder passwords = new BCryptPasswordEncoder(12);
    private final SecureRandom random = new SecureRandom();
    // Equal-cost verification for unknown usernames prevents the cheap account-existence branch.
    private final String dummyHash = passwords.encode(UUID.randomUUID().toString());

    public record Actor(
            String id, String enterpriseId, String orgUnitId, Set<String> roles, String tokenHash) {
        public boolean has(String role) {
            return roles.contains(role);
        }

        public int businessRank() {
            if (has("FOUNDER")) return 0;
            if (has("DEPARTMENT_MANAGER")) return 1;
            if (has("TEAM_LEAD")) return 2;
            if (has("EMPLOYEE")) return 3;
            return 99;
        }

        public void business() {
            if (businessRank() == 99) throw BusinessException.forbidden();
        }

        public void admin() {
            if (!has("ADMIN")) throw BusinessException.forbidden();
        }

        public void manager() {
            business();
            if (businessRank() >= 3) throw BusinessException.forbidden();
        }
    }

    public IdentityService(
            BusinessDb db,
            CredentialHandoffService handoffs,
            tools.jackson.databind.json.JsonMapper json,
            @Value("${pandora.bootstrap-token:${PANDORA_BOOTSTRAP_TOKEN:}}") String bootstrap) {
        this.db = db;
        this.handoffs = handoffs;
        this.bootstrap = bootstrap;
        this.json = json;
    }

    public static String hash(String value) {
        try {
            return HexFormat.of()
                    .formatHex(
                            MessageDigest.getInstance("SHA-256")
                                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public Actor current() {
        var attributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        var actor = (Actor) attributes.getRequest().getAttribute(ACTOR_ATTRIBUTE);
        if (actor == null)
            throw new BusinessException(401, "UNAUTHENTICATED", "A bearer session is required.");
        return actor;
    }

    public Actor authenticate(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ") || header.length() > 512)
            throw new BusinessException(401, "UNAUTHENTICATED", "A bearer session is required.");
        String digest = hash(header.substring(7));
        var session =
                db.optional(
                        "SELECT s.*,u.org_unit_id,u.status AS user_status,e.status AS"
                            + " enterprise_status FROM auth_session s JOIN app_user u ON"
                            + " u.enterprise_id=s.enterprise_id AND u.id=s.user_id JOIN enterprise"
                            + " e ON e.id=s.enterprise_id WHERE s.token_hash=?",
                        digest);
        if (session.isEmpty())
            throw new BusinessException(401, "UNAUTHENTICATED", "The session is invalid.");
        var row = session.get();
        boolean repeatedLogout =
                request.getRequestURI().equals("/api/v1/auth/logout")
                        && request.getMethod().equals("POST");
        if (Boolean.TRUE.equals(row.get("revoked")) && !repeatedLogout
                || !((OffsetDateTime) row.get("expires_at")).toInstant().isAfter(Instant.now())
                || !"ACTIVE".equals(row.get("user_status"))
                || !"ACTIVE".equals(row.get("enterprise_status")))
            throw new BusinessException(
                    401, "SESSION_REVOKED", "The session has expired or is revoked.");
        return actor(
                str(row, "user_id"), str(row, "enterprise_id"), str(row, "org_unit_id"), digest);
    }

    public Actor actor(String userId, String enterpriseId, String orgId, String digest) {
        Set<String> roles = new LinkedHashSet<>();
        db.rows(
                        "SELECT role_code FROM user_role WHERE enterprise_id=? AND user_id=? ORDER"
                                + " BY role_code",
                        enterpriseId,
                        userId)
                .forEach(r -> roles.add(str(r, "role_code")));
        return new Actor(userId, enterpriseId, orgId, Set.copyOf(roles), digest);
    }

    @Transactional
    public Map<String, Object> login(Contracts.Login command) {
        if (command.password().getBytes(StandardCharsets.UTF_8).length > 72)
            throw BusinessException.invalid("Passwords must fit the 72-byte BCrypt input bound.");
        String domain = command.enterpriseDomain();
        String loginName = command.loginName();
        if (command.accountIdentifier() != null && !command.accountIdentifier().isBlank()) {
            if (domain != null || loginName != null)
                throw BusinessException.invalid("Use accountIdentifier or the legacy enterpriseDomain/loginName pair.");
            int separator = command.accountIdentifier().lastIndexOf('@');
            if (separator <= 0 || separator == command.accountIdentifier().length() - 1) {
                passwords.matches(command.password(), dummyHash);
                throw new BusinessException(401, "INVALID_CREDENTIALS", "Account identifier or credentials are invalid.");
            }
            loginName = command.accountIdentifier().substring(0, separator);
            domain = command.accountIdentifier().substring(separator + 1);
        }
        if (domain == null || domain.isBlank() || loginName == null || loginName.isBlank()) {
            passwords.matches(command.password(), dummyHash);
            throw new BusinessException(401, "INVALID_CREDENTIALS", "Account identifier or credentials are invalid.");
        }
        var user =
                db.optional(
                        "SELECT u.* FROM app_user u JOIN enterprise e ON e.id=u.enterprise_id WHERE"
                                + " e.login_domain=? AND e.status='ACTIVE' AND u.login_name=? AND"
                                + " u.status='ACTIVE'",
                        domain,
                        loginName);
        boolean valid =
                passwords.matches(
                        command.password(),
                        user.map(r -> str(r, "password_hash")).orElse(dummyHash));
        if (!valid || user.isEmpty())
            throw new BusinessException(
                    401, "INVALID_CREDENTIALS", "Enterprise or credentials are invalid.");
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Instant expires = Instant.now().plus(Duration.ofHours(12));
        var u = user.get();
        db.update(
                "INSERT INTO auth_session(token_hash,enterprise_id,user_id,expires_at)"
                        + " VALUES(?,?,?,?)",
                hash(token),
                u.get("enterprise_id"),
                u.get("id"),
                OffsetDateTime.ofInstant(expires, ZoneOffset.UTC));
        return fields(
                "accessToken",
                token,
                "tokenType",
                "Bearer",
                "expiresAt",
                expires.toString(),
                "user",
                profile(
                        actor(
                                str(u, "id"),
                                str(u, "enterprise_id"),
                                str(u, "org_unit_id"),
                                hash(token))));
    }

    public Map<String, Object> profile(Actor actor) {
        var user = view(db.entity("app_user", actor.enterpriseId(), actor.id()));
        user.remove("loginName");
        user.put("name", user.get("displayName"));
        user.put("roles", actor.roles());
        user.put(
                "capabilities",
                fields(
                        "admin",
                        actor.has("ADMIN"),
                        "business",
                        actor.businessRank() != 99,
                        "manage",
                        actor.businessRank() < 3,
                        "createPersonalTasks",
                        actor.businessRank() != 99,
                        "createDelegatedTasks",
                        actor.businessRank() < 3,
                        "reviewTasks",
                        actor.businessRank() < 3,
                        "createTags",
                        actor.has("FOUNDER")
                                || actor.has("DEPARTMENT_MANAGER"),
                        "publishCompanyItems",
                        actor.has("ADMIN") || actor.has("FOUNDER")));
        var enterprise = db.one("SELECT * FROM enterprise WHERE id=?", actor.enterpriseId());
        user.put("enterprise", view(enterprise));
        user.put("businessTimezone", enterprise.get("business_timezone"));
        user.put(
                "organization",
                actor.orgUnitId() == null
                        ? null
                        : view(db.entity("org_unit", actor.enterpriseId(), actor.orgUnitId())));
        user.put("department", department(actor));
        // 直属上级展示名：供客户端"我的"页展示真实上级；未配置或上级已被删除时为 null
        var supervisorId = user.get("supervisorUserId");
        user.put(
                "supervisorName",
                supervisorId == null || supervisorId.toString().isBlank()
                        ? null
                        : db.optional(
                                        "SELECT display_name FROM app_user WHERE enterprise_id=? AND id=?",
                                        actor.enterpriseId(),
                                        supervisorId.toString())
                                .map(row -> str(row, "display_name"))
                                .orElse(null));
        return user;
    }

    private String department(Actor actor) {
        String org = actor.orgUnitId();
        Set<String> seen = new HashSet<>();
        while (org != null && seen.add(org)) {
            var row = db.entity("org_unit", actor.enterpriseId(), org);
            if ("DEPARTMENT".equals(row.get("kind"))) return str(row, "name");
            org = str(row, "parent_id");
        }
        return null;
    }

    @Transactional
    public void logout(Actor actor) {
        db.update("UPDATE auth_session SET revoked=TRUE WHERE token_hash=?", actor.tokenHash());
    }

    @Transactional
    public void password(Actor actor, Contracts.Password command) {
        db.one("SELECT id FROM enterprise WHERE id=? FOR UPDATE", actor.enterpriseId());
        var user = db.lock("app_user", actor.enterpriseId(), actor.id());
        if (!passwords.matches(command.currentPassword(), str(user, "password_hash")))
            throw new BusinessException(
                    403, "CURRENT_PASSWORD_INVALID", "The current password is invalid.");
        db.update(
                "UPDATE app_user SET password_hash=?,version=version+1 WHERE enterprise_id=? AND"
                        + " id=?",
                encode(command.newPassword()),
                actor.enterpriseId(),
                actor.id());
        handoffs.changed(actor.enterpriseId(), actor.id());
        logout(actor);
    }

    public String encode(String password) {
        if (password.getBytes(StandardCharsets.UTF_8).length > 72)
            throw BusinessException.invalid("Passwords must fit the 72-byte BCrypt input bound.");
        return passwords.encode(password);
    }

    public void validateZone(String zone) {
        if (zone != null)
            try {
                if (zone.isBlank() || !ZoneId.getAvailableZoneIds().contains(zone))
                    throw new DateTimeException("invalid");
                ZoneId.of(zone);
            } catch (DateTimeException ex) {
                throw BusinessException.invalid("businessTimezone must be an IANA timezone.");
            }
    }

    public ZoneId zone(Actor actor) {
        String zone =
                str(
                        db.one(
                                "SELECT business_timezone FROM enterprise WHERE id=?",
                                actor.enterpriseId()),
                        "business_timezone");
        if (zone == null)
            throw new BusinessException(
                    409,
                    "BUSINESS_TIMEZONE_NOT_CONFIGURED",
                    "An administrator must configure the enterprise business timezone.");
        return ZoneId.of(zone);
    }

    public LocalDate today(Actor actor) {
        return LocalDate.now(zone(actor));
    }

    @Transactional
    @SuppressWarnings("unchecked")
    public Map<String, Object> initialize(
            Contracts.EnterpriseCreate command, String provided, String key) {
        boolean bootstrapValid =
                !bootstrap.isBlank()
                        && provided != null
                        && MessageDigest.isEqual(
                                bootstrap.getBytes(StandardCharsets.UTF_8),
                                provided.getBytes(StandardCharsets.UTF_8));
        String replayScope;
        if (!bootstrapValid) {
            current(); // Anonymous callers retain 401; a company's authenticated ADMIN gets 403.
            throw BusinessException.forbidden();
        }
        replayScope = hash("BOOTSTRAP:" + bootstrap);
        if (key != null && (key.isBlank() || key.length() > 128))
            throw BusinessException.invalid("Idempotency-Key must contain 1–128 characters.");
        db.one("SELECT id FROM enterprise_initialization_lock WHERE id=1 FOR UPDATE");
        String requestHash = hash(json.writeValueAsString(command));
        if (key != null) {
            var replay =
                    db.optional(
                            "SELECT * FROM enterprise_initialization_replay WHERE scope_hash=? AND"
                                    + " idempotency_key=?",
                            replayScope,
                            key);
            if (replay.isPresent()) {
                if (!requestHash.equals(replay.get().get("request_hash")))
                    throw new BusinessException(
                            409,
                            "IDEMPOTENCY_CONFLICT",
                            "The initialization key was used for different input.");
                return json.readValue(str(replay.get(), "response_json"), Map.class);
            }
        }
        validateZone(command.businessTimezone());
        String enterprise = id(), root = id(), admin = id();
        db.update(
                "INSERT INTO enterprise(id,login_domain,name,business_timezone) VALUES(?,?,?,?)",
                enterprise,
                command.loginDomain(),
                command.name(),
                command.businessTimezone());
        db.update(
                "INSERT INTO org_unit(id,enterprise_id,name,kind) VALUES(?,?,?,'COMPANY')",
                root,
                enterprise,
                command.name());
        db.update(
                "INSERT INTO"
                    + " app_user(id,enterprise_id,login_name,password_hash,display_name,org_unit_id)"
                    + " VALUES(?,?,?,?,?,?)",
                admin,
                enterprise,
                command.adminLoginName(),
                encode(command.adminPassword()),
                command.adminDisplayName(),
                root);
        db.update("INSERT INTO user_role VALUES(?,?,'ADMIN')", enterprise, admin);
        var result =
                fields(
                        "id",
                        enterprise,
                        "loginDomain",
                        command.loginDomain(),
                        "name",
                        command.name(),
                        "rootOrgUnitId",
                        root,
                        "adminUserId",
                        admin,
                        "businessTimezone",
                        command.businessTimezone(),
                        "version",
                        0);
        if (key != null)
            db.update(
                    "INSERT INTO"
                        + " enterprise_initialization_replay(scope_hash,idempotency_key,request_hash,response_json)"
                        + " VALUES(?,?,?,?)",
                    replayScope,
                    key,
                    requestHash,
                    json.writeValueAsString(result));
        return result;
    }
}
