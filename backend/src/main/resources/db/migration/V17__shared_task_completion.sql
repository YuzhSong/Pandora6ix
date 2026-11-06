ALTER TABLE task ADD COLUMN completion_status VARCHAR(24) NOT NULL DEFAULT 'NOT_COMPLETED'
    CHECK (completion_status IN ('NOT_COMPLETED','AWAITING_ACCEPTANCE','COMPLETED'));

UPDATE task t SET completion_status = CASE
    WHEN EXISTS (SELECT 1 FROM task_assignment a WHERE a.enterprise_id=t.enterprise_id AND a.task_id=t.id AND a.current_member=TRUE AND a.execution_status='COMPLETED') THEN 'COMPLETED'
    WHEN EXISTS (SELECT 1 FROM task_assignment a WHERE a.enterprise_id=t.enterprise_id AND a.task_id=t.id AND a.current_member=TRUE AND a.execution_status='AWAITING_ACCEPTANCE') THEN 'AWAITING_ACCEPTANCE'
    ELSE 'NOT_COMPLETED' END;

-- Preserve actual completion attribution; never manufacture a new verifier or timestamp.
UPDATE task_assignment a SET
    completion_source=(SELECT x.completion_source FROM task_assignment x WHERE x.enterprise_id=a.enterprise_id AND x.task_id=a.task_id AND x.current_member=TRUE AND x.execution_status='COMPLETED' ORDER BY x.completed_at,x.id FETCH FIRST 1 ROW ONLY),
    completed_by_user_id=(SELECT x.completed_by_user_id FROM task_assignment x WHERE x.enterprise_id=a.enterprise_id AND x.task_id=a.task_id AND x.current_member=TRUE AND x.execution_status='COMPLETED' ORDER BY x.completed_at,x.id FETCH FIRST 1 ROW ONLY),
    completed_at=(SELECT x.completed_at FROM task_assignment x WHERE x.enterprise_id=a.enterprise_id AND x.task_id=a.task_id AND x.current_member=TRUE AND x.execution_status='COMPLETED' ORDER BY x.completed_at,x.id FETCH FIRST 1 ROW ONLY),
    execution_status='COMPLETED',work_status='COMPLETED',version=version+1
WHERE a.current_member=TRUE AND a.execution_status<>'COMPLETED' AND EXISTS
    (SELECT 1 FROM task t WHERE t.enterprise_id=a.enterprise_id AND t.id=a.task_id AND t.completion_status='COMPLETED');

UPDATE task_assignment a SET execution_status='AWAITING_ACCEPTANCE',work_status='COMPLETED',version=version+1
WHERE a.current_member=TRUE AND EXISTS
    (SELECT 1 FROM task t WHERE t.enterprise_id=a.enterprise_id AND t.id=a.task_id AND t.completion_status='AWAITING_ACCEPTANCE');

-- Old pending requests on a task already accepted inherit its existing verified result.
UPDATE task_completion_request r SET decision='ACCEPTED',
    decided_by_user_id=(SELECT a.completed_by_user_id FROM task_assignment a WHERE a.enterprise_id=r.enterprise_id AND a.id=r.assignment_id),
    decided_at=(SELECT a.completed_at FROM task_assignment a WHERE a.enterprise_id=r.enterprise_id AND a.id=r.assignment_id),
    decision_note='按任务既有完成结果统一，保留原申请记录',version=version+1
WHERE r.decision='PENDING' AND EXISTS
    (SELECT 1 FROM task_assignment a JOIN task t ON t.enterprise_id=a.enterprise_id AND t.id=a.task_id
     WHERE a.enterprise_id=r.enterprise_id AND a.id=r.assignment_id AND a.current_member=TRUE AND t.completion_status='COMPLETED');
