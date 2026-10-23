package com.pandora6ix.backend.controller;
import com.pandora6ix.backend.service.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import java.time.OffsetDateTime;
@RestController @RequestMapping("/api/v1/admin/operation-logs")
public class OperationLogController {
 private final IdentityService identity; private final OperationLogService logs;
 public OperationLogController(IdentityService identity, OperationLogService logs){this.identity=identity;this.logs=logs;}
 @GetMapping public Object list(@RequestParam(required=false) String keyword,@RequestParam(required=false) String module,@RequestParam(required=false) String action,@RequestParam(required=false) String source,@RequestParam(required=false) String result,@RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,@RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,@RequestParam(required=false) String cursor,@RequestParam(required=false) String snapshotToken,@RequestParam(defaultValue="20") int limit){return logs.list(identity.current(),keyword,module,action,source,result,from,to,cursor,snapshotToken,limit);}
 @GetMapping("/{id}") public Object detail(@PathVariable String id){return logs.detail(identity.current(),id);}
}
