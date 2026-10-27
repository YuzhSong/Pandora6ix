package com.pandora6ix.backend.api.error;

import com.pandora6ix.backend.service.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.Set;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Logs no raw request/response bodies, query values, SQL or foreign object names. */
@Component
public class FailedOperationAuditFilter extends OncePerRequestFilter {
    private final OperationLogService logs;
    public FailedOperationAuditFilter(OperationLogService logs) { this.logs=logs; }
    @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain) throws ServletException,IOException {
        chain.doFilter(request,response);
        if(response.getStatus()<400 || !Set.of("POST","PATCH","PUT","DELETE").contains(request.getMethod())) return;
        Object principal=request.getAttribute(IdentityService.ACTOR_ATTRIBUTE);
        if(!(principal instanceof IdentityService.Actor actor)) return;
        String path=request.getRequestURI();
        String prefix=path.startsWith("/api/v1/")?path.substring(8):"";
        String[] parts=prefix.split("/");
        if(parts.length==0) return;
        String type,module;
        switch(parts[0]) {
            case "tasks" -> {type="TASK";module="TASK";}
            case "work-logs" -> {type="WORK_LOG";module="WORK_LOG";}
            case "task-assignments" -> {type="TASK_ASSIGNMENT";module="TASK";}
            case "task-reviews" -> {type="TASK_REVIEW";module="TASK";}
            case "task-completion-requests" -> {type="COMPLETION_REQUEST";module="TASK";}
            default -> {return;}
        }
        String objectId=parts.length>1&&parts[1].matches("[A-Za-z0-9-]{1,36}")?parts[1]:null;
        String action="DELETE".equals(request.getMethod())?"DELETE":parts.length==1&&"POST".equals(request.getMethod())?"CREATE":"UPDATE";
        Object error=request.getAttribute("pandora.errorCode");
        try {
            String source=request.getHeader("X-Client-Source");
            if(!"WEB".equals(source)&&!"ANDROID".equals(source))source="SYSTEM";
            logs.failure(actor,objectId,type,module,action,source,(String)request.getAttribute(RequestIdFilter.ATTRIBUTE_NAME),response.getStatus(),error instanceof String code?code:null);
        } catch(RuntimeException ex) {
            LoggerFactory.getLogger(getClass()).warn("operation_failure_audit_unavailable requestId={}",request.getAttribute(RequestIdFilter.ATTRIBUTE_NAME));
        }
    }
}
