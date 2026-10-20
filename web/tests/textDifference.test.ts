import test from 'node:test'
import assert from 'node:assert/strict'
import { textDifference } from '../src/data/textDifference.ts'

test('unchanged text stays uncolored while modified date characters are highlighted', () => {
  const equal = textDifference('准备验收材料', '准备验收材料')
  assert.deepEqual(equal.before, [{ text: '准备验收材料', changed: false }])
  const diff = textDifference('2026-10-21 18:00', '2026-10-22 18:00')
  assert.equal(diff.before.filter(part => part.changed).map(part => part.text).join(''), '1')
  assert.equal(diff.after.filter(part => part.changed).map(part => part.text).join(''), '2')
})

test('added lines, removed text and multiple edits retain complete readable versions', () => {
  for (const [left, right] of [['整理材料\n核对附件', '整理材料\n核对附件\n补充验收记录'], ['删除的正文', ''], ['', '新正文'], ['甲乙丙丁', '甲改丙新丁'], ['备注为空', '备注已有内容']]) {
    const diff = textDifference(left, right)
    assert.equal(diff.before.map(part => part.text).join(''), left)
    assert.equal(diff.after.map(part => part.text).join(''), right)
    assert.ok([...diff.before, ...diff.after].some(part => part.changed))
  }
  const appended = textDifference('原内容', '原内容\n新增内容')
  assert.equal(appended.before.some(part => part.changed), false)
  assert.deepEqual(appended.after.at(-1), { text: '\n新增内容', changed: true })
})

test('emoji are not split into surrogate fragments and markup remains literal text', () => {
  const diff = textDifference('成员😀<script>', '成员🚀<script>')
  assert.deepEqual(diff.before.filter(part => part.changed), [{ text: '😀', changed: true }])
  assert.deepEqual(diff.after.filter(part => part.changed), [{ text: '🚀', changed: true }])
  assert.equal(diff.after.map(part => part.text).join(''), '成员🚀<script>')
})

test('large unrelated texts use bounded comparison while preserving all content', () => {
  const left = `标题\n${'甲'.repeat(2000)}\n结尾`, right = `标题\n${'乙'.repeat(2000)}\n结尾`
  const diff = textDifference(left, right)
  assert.equal(diff.before.map(part => part.text).join(''), left)
  assert.equal(diff.after.map(part => part.text).join(''), right)
  assert.deepEqual(diff.after.filter(part => part.changed), [{ text: '乙'.repeat(2000), changed: true }])
})
