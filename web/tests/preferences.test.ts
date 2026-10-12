import assert from 'node:assert/strict'
import test from 'node:test'
import { createPreferenceStore, defaultPreferences } from '../src/data/preferences.ts'

test('theme and font persist per account without mixing another account', () => {
  const values = new Map<string, string>()
  const storage = { getItem: (key: string) => values.get(key) ?? null, setItem: (key: string, value: string) => { values.set(key, value) } }
  const first = createPreferenceStore(storage)
  first.save('admin-a', { theme: 'dark', fontSize: 'large' })
  const reloaded = createPreferenceStore(storage)
  assert.deepEqual(reloaded.load('admin-a'), { theme: 'dark', fontSize: 'large' })
  assert.deepEqual(reloaded.load('admin-b'), defaultPreferences)
})

test('preference write failure does not change existing persisted settings', () => {
  const store = createPreferenceStore({ getItem: () => JSON.stringify(defaultPreferences), setItem: () => { throw new Error('quota') } })
  assert.throws(() => store.save('admin', { theme: 'dark', fontSize: 'large' }), /保存失败/)
  assert.deepEqual(store.load('admin'), defaultPreferences)
})

test('invalid stored preferences report failure instead of affecting CSS attributes', () => {
  const store = createPreferenceStore({ getItem: () => '{"theme":"invalid","fontSize":"giant"}', setItem: () => {} })
  assert.throws(() => store.load('admin'), /无法读取/)
})
