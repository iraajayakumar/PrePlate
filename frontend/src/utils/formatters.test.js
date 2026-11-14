import { describe, expect, it } from 'vitest';
import { firstName, formatCountdown, formatDateTimeLong, formatMealDate, sumNutrition, toIsoDate } from './formatters';

describe('formatters', () => {
  it('formats a countdown as HH : MM : SS', () => {
    expect(formatCountdown((1 * 3600 + 20 * 60 + 57) * 1000)).toBe('01 : 20 : 57');
  });
  it('clamps negative countdowns to zero', () => {
    expect(formatCountdown(-5000)).toBe('00 : 00 : 00');
  });
  it('formats long date-time with ordinal suffix', () => {
    expect(formatDateTimeLong(new Date(2026, 10, 14, 12, 40))).toBe('14th November 2026, 12:40 pm');
    expect(formatDateTimeLong(new Date(2026, 10, 1, 9, 5))).toBe('1st November 2026, 9:05 am');
    expect(formatDateTimeLong(new Date(2026, 10, 12, 9, 5))).toContain('12th');
  });
  it('builds local ISO dates without timezone shift', () => {
    expect(toIsoDate(new Date(2026, 10, 14, 0, 5))).toBe('2026-11-14');
  });
  it('formats meal dates', () => {
    expect(formatMealDate('2026-11-14')).toContain('14');
  });
  it('extracts the first name with a safe fallback', () => {
    expect(firstName('Aarav Mehta')).toBe('Aarav');
    expect(firstName('')).toBe('there');
  });
  it('sums nutrition of dishes', () => {
    const total = sumNutrition([{ calories: 200, proteinG: 5, carbsG: 30, fatG: 2 }, { calories: 100, proteinG: 3, carbsG: 10, fatG: 1 }]);
    expect(total).toEqual({ calories: 300, proteinG: 8, carbsG: 40, fatG: 3 });
  });
});
