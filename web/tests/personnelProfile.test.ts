import assert from 'node:assert/strict'
import test from 'node:test'
import { maskIdentity, validatePersonnelProfile } from '../src/data/personnelProfile.ts'

test('profile requires gender, identity and phone and masks identity in details', () => {
  // Synthetic format-only fixture; no real employee identity.
  const profile = { gender: 'FEMALE' as const, idNumber: '00000020000101000X', phoneNumber: '00000000000' }
  assert.doesNotThrow(() => validatePersonnelProfile(profile))
  assert.throws(() => validatePersonnelProfile({ ...profile, gender: null }), /性别/)
  assert.throws(() => validatePersonnelProfile({ ...profile, idNumber: '123' }), /身份证/)
  assert.throws(() => validatePersonnelProfile({ ...profile, phoneNumber: 'abc123' }), /电话/)
  assert.equal(maskIdentity(profile.idNumber), '000***********000X')
  assert.equal(maskIdentity(null), '未填写')
})

test('legacy profile patches validate only supplied values while creation still requires all fields', () => {
  const partial = { gender: null, idNumber: null, phoneNumber: '+86 (010) 12345678' }
  assert.doesNotThrow(() => validatePersonnelProfile(partial, true))
  assert.throws(() => validatePersonnelProfile(partial), /性别/)
  assert.doesNotThrow(() => validatePersonnelProfile({ gender: 'FEMALE', idNumber: null, phoneNumber: null }, true))
  assert.throws(() => validatePersonnelProfile({ ...partial, phoneNumber: '12-34' }, true), /电话/)
  assert.throws(() => validatePersonnelProfile({ ...partial, idNumber: '' }, true), /身份证/)
})
