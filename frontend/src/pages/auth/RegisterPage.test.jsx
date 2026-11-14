import { describe, expect, it } from 'vitest';
import { validateRegistration } from './RegisterPage';

const valid = { registrationNumber: 'RA2111003010099', fullName: 'Test User', department: 'CSE', hostelName: 'Kaveri', hostelRoom: '101', password: 'secret1', confirmPassword: 'secret1' };

describe('validateRegistration', () => {
  it('accepts a complete form', () => { expect(validateRegistration(valid)).toEqual({}); });
  it('flags missing fields and short passwords', () => {
    const errors = validateRegistration({ ...valid, fullName: ' ', password: '123', confirmPassword: '123' });
    expect(errors.fullName).toBeDefined();
    expect(errors.password).toBeDefined();
  });
  it('flags mismatched passwords', () => {
    expect(validateRegistration({ ...valid, confirmPassword: 'other' }).confirmPassword).toBeDefined();
  });
});
