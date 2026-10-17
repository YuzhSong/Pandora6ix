import assert from 'node:assert/strict'
import test from 'node:test'
import { ItemOrderSaveError, moveCompanyItem, saveCompanyItemOrder, sortCompanyItems } from '../src/data/companyItemOrder.ts'
import type { AdminData, CompanyImportantItem } from '../src/data/types.ts'
import { createSeed } from '../src/data/seed.ts'

const items: CompanyImportantItem[] = [1, 2, 3].map(index => ({ id: `item-${index}`, title: `事项 ${index}`, content: `正文 ${index}`, displayOrder: index, publisherUserId: 'admin', version: index + 10 }))

test('move-to-top changes only draft position and saving assigns contiguous orders with original content/versions', async () => {
  const draft = moveCompanyItem(items, 'item-3', 0)
  assert.deepEqual(draft.map(item => item.id), ['item-3', 'item-1', 'item-2'])
  assert.deepEqual(items.map(item => item.id), ['item-1', 'item-2', 'item-3'])
  const calls: unknown[] = []
  let state: AdminData = { ...createSeed(), companyItems: [...items] }
  await saveCompanyItemOrder({ saveCompanyItem: async (input, id, version) => {
    calls.push({ input, id, version })
    const old = items.find(item => item.id === id)!
    assert.equal(version, old.version)
    assert.equal(input.title, old.title); assert.equal(input.content, old.content)
    state = { ...state, companyItems: state.companyItems.map(item => item.id === id ? { ...item, ...input, version: item.version + 1 } : item) }
    return state
  } }, draft)
  assert.equal(calls.length, 3)
  assert.deepEqual(sortCompanyItems(state.companyItems).map(item => [item.id, item.displayOrder]), [['item-3', 1], ['item-1', 2], ['item-2', 3]])
})

test('unchanged items are not written and a partial failure stops without retrying', async () => {
  let calls = 0
  assert.equal(await saveCompanyItemOrder({ saveCompanyItem: async () => { calls++; return createSeed() } }, items), null)
  assert.equal(calls, 0)
  const conflict = new Error('版本冲突')
  await assert.rejects(saveCompanyItemOrder({ saveCompanyItem: async () => {
    calls++
    if (calls === 2) throw conflict
    return createSeed()
  } }, moveCompanyItem(items, 'item-3', 0)), (reason: unknown) => reason instanceof ItemOrderSaveError && reason.completed === 1 && reason.cause === conflict)
  assert.equal(calls, 2)
})

test('moving across a page boundary uses full-list indices and stable sorting does not mutate input', () => {
  const all = Array.from({ length: 12 }, (_, index) => ({ ...items[0], id: `long-${index}`, displayOrder: index + 1 }))
  const moved = moveCompanyItem(all, 'long-10', 0)
  assert.equal(moved[0].id, 'long-10')
  assert.equal(moved.length, 12)
  assert.equal(all[10].id, 'long-10')
  assert.equal(moveCompanyItem(all, 'missing', 0), all)
  assert.equal(moveCompanyItem(all, 'long-0', -1), all)
  const reversed = [...items].reverse()
  assert.deepEqual(sortCompanyItems(reversed), items)
  assert.equal(reversed[0].id, 'item-3')
})
