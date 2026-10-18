package com.pandora6ix.app.ui.calendar

import com.pandora6ix.app.data.RecipientDto

internal data class RecipientOrganizationGroup(
    val key: String,
    val name: String,
    val members: List<RecipientDto>,
    val children: List<RecipientOrganizationGroup>
) {
    val memberCount: Int get() = members.size + children.sumOf { it.memberCount }
    val groupKeys: Set<String> get() = setOf(key) + children.flatMap { it.groupKeys }
}

/** Reconstruct only relationships supplied by the candidate API; names never identify a node. */
internal fun recipientOrganizationTree(candidates: List<RecipientDto>): List<RecipientOrganizationGroup> {
    class Node(val key: String, val name: String) {
        val members = mutableListOf<RecipientDto>()
        val children = linkedMapOf<String, Node>()
        fun freeze(): RecipientOrganizationGroup = RecipientOrganizationGroup(
            key, name, members.toList(), children.values.map { it.freeze() }
        )
    }
    val roots = linkedMapOf<String, Node>()
    for (person in candidates.distinctBy { it.id }) {
        val departmentId = person.departmentId?.takeIf { it.isNotBlank() }
        val organizationId = person.orgUnitId?.takeIf { it.isNotBlank() }
        val departmentName = person.departmentName?.takeIf { it.isNotBlank() }
        val organizationName = person.organizationName?.takeIf { it.isNotBlank() }
        if (departmentId != null) {
            val departmentKey = "department:$departmentId"
            val department = roots.getOrPut(departmentKey) {
                Node(departmentKey, departmentName ?: organizationName?.takeIf { organizationId == departmentId } ?: "未命名部门")
            }
            if (organizationId != null && organizationId != departmentId) {
                val teamKey = "team:$organizationId"
                department.children.getOrPut(teamKey) {
                    Node(teamKey, organizationName ?: "未命名团队")
                }.members += person
            } else {
                department.members += person
            }
        } else {
            val key = organizationId?.let { "organization:$it" } ?: "unassigned"
            roots.getOrPut(key) {
                Node(key, organizationName ?: departmentName ?: "未分配组织")
            }.members += person
        }
    }
    return roots.values.map { it.freeze() }
}
