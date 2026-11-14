import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import DishCard from './DishCard';

const dish = { id: 3, name: 'Paneer Butter Masala', calories: 320, proteinG: 14, carbsG: 12, fatG: 24, vegetarian: true, ingredients: ['Paneer', 'Tomato'] };

describe('DishCard', () => {
  it('shows nutrition info but no add button before attendance is confirmed', () => {
    render(<DishCard dish={dish} canAdd={false} inPlate={false} onToggle={() => {}} />);
    expect(screen.getByText('Paneer Butter Masala')).toBeInTheDocument();
    expect(screen.getByText('320 kcal')).toBeInTheDocument();
    expect(screen.queryByRole('button')).not.toBeInTheDocument();
  });
  it('calls onToggle with the dish id when Add to plate is clicked', async () => {
    const onToggle = vi.fn();
    render(<DishCard dish={dish} canAdd inPlate={false} onToggle={onToggle} />);
    await userEvent.click(screen.getByRole('button', { name: 'Add to plate' }));
    expect(onToggle).toHaveBeenCalledWith(3);
  });
  it('reflects the in-plate state', () => {
    render(<DishCard dish={dish} canAdd inPlate onToggle={() => {}} />);
    expect(screen.getByRole('button', { name: 'Added ✓' })).toHaveAttribute('aria-pressed', 'true');
  });
});
