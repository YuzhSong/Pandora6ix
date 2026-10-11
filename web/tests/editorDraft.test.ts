import assert from 'node:assert/strict'
import test from 'node:test'
import { createEditorDraft } from '../src/data/editorDraft.ts'
import { createSeed } from '../src/data/seed.ts'

test('selected department proposes a team; selected company proposes a department', () => {
  const data = createSeed()
  const team = createEditorDraft({ kind: 'unit', parentId: 'org-product' }, data)
  assert.equal(team.kind, 'TEAM')
  assert.equal(team.parentId, 'org-product')
  const department = createEditorDraft({ kind: 'unit', parentId: 'org-company' }, data)
  assert.equal(department.kind, 'DEPARTMENT')
  assert.equal(department.parentId, 'org-company')
})

test('person creation proposes selected organization without inventing a supervisor', () => {
  const data = createSeed()
  const draft = createEditorDraft({ kind: 'person', orgUnitId: 'org-web' }, data)
  assert.equal(draft.orgUnitId, 'org-web')
  assert.equal(draft.supervisorId, '')
  assert.deepEqual(draft.roles, ['EMPLOYEE'])
})

test('invalid context does not invent relationships, and existing records retain original values', () => {
  const data = createSeed()
  assert.equal(createEditorDraft({ kind: 'unit', parentId: 'org-web' }, data).kind, 'DEPARTMENT')
  assert.equal(createEditorDraft({ kind: 'person', orgUnitId: 'missing' }, data).orgUnitId, data.units[0].id)
  const person = data.people.find(item => item.id === 'u-cy')!
  const draft = createEditorDraft({ kind: 'person', item: person, orgUnitId: 'org-company' }, data)
  assert.equal(draft.orgUnitId, person.orgUnitId)
  assert.equal(draft.supervisorId, person.supervisorId)
})
