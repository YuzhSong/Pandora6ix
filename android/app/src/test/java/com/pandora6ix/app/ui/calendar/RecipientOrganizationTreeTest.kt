package com.pandora6ix.app.ui.calendar

import com.pandora6ix.app.data.RecipientDto
import org.junit.Assert.*
import org.junit.Test

class RecipientOrganizationTreeTest {
    private fun person(id: String, department: String?, org: String?, deptName: String? = "研发部", orgName: String? = "开发组") =
        RecipientDto(id, displayName = id, departmentId = department, departmentName = deptName, orgUnitId = org, organizationName = orgName)

    @Test fun departmentContainsItsDirectPeopleAndSeparateChildTeams() {
        val roots = recipientOrganizationTree(listOf(
            person("direct", "d1", "d1", orgName = "研发部"),
            person("android", "d1", "t1", orgName = "Android组"),
            person("web", "d1", "t2", orgName = "Web组")
        ))
        assertEquals(1, roots.size)
        val department = roots.single()
        assertEquals(listOf("direct"), department.members.map { it.id })
        assertEquals(listOf("Android组", "Web组"), department.children.map { it.name })
        assertEquals(listOf("android", "web"), department.children.flatMap { it.members }.map { it.id })
        assertEquals(3, department.memberCount)
        assertEquals(setOf("department:d1", "team:t1", "team:t2"), department.groupKeys)
    }

    @Test fun identicalDepartmentNamesDoNotMergePeopleOrExpansionKeys() {
        val roots = recipientOrganizationTree(listOf(person("one", "d1", "d1"), person("two", "d2", "d2")))
        assertEquals(listOf("研发部", "研发部"), roots.map { it.name })
        assertEquals(2, roots.map { it.key }.toSet().size)
        assertEquals(listOf(listOf("one"), listOf("two")), roots.map { it.members.map { p -> p.id } })
    }

    @Test fun identicalTeamNamesWithinADepartmentRemainIndependent() {
        val department = recipientOrganizationTree(listOf(person("one", "d1", "t1"), person("two", "d1", "t2"))).single()
        assertEquals(listOf("开发组", "开发组"), department.children.map { it.name })
        assertEquals(2, department.children.map { it.key }.toSet().size)
        assertEquals(listOf(listOf("one"), listOf("two")), department.children.map { it.members.map { p -> p.id } })
    }

    @Test fun legacyOrganizationsUseIdsWithoutGuessingAParentByName() {
        val roots = recipientOrganizationTree(listOf(person("one", null, "t1"), person("two", null, "t2")))
        assertEquals(2, roots.size)
        assertTrue(roots.all { it.children.isEmpty() && it.memberCount == 1 })
        assertEquals(setOf("organization:t1", "organization:t2"), roots.map { it.key }.toSet())
    }

    @Test fun missingOrganizationStillKeepsEveryKnownOrUnassignedPerson() {
        val roots = recipientOrganizationTree(listOf(
            person("department-member", "d1", null),
            person("unknown", null, null, null, null)
        ))
        assertEquals("department-member", roots.first().members.single().id)
        assertEquals("unassigned", roots.last().key)
        assertEquals("未分配组织", roots.last().name)
        assertEquals("unknown", roots.last().members.single().id)
    }

    @Test fun duplicatePeopleDoNotInflateCountsOrAppearInMultipleBranches() {
        val entry = person("one", "d1", "t1")
        val department = recipientOrganizationTree(listOf(entry, entry, person("two", "d1", "t1"))).single()
        assertEquals(2, department.memberCount)
        assertEquals(listOf("one", "two"), department.children.single().members.map { it.id })
        assertTrue(recipientOrganizationTree(emptyList()).isEmpty())
    }
}
