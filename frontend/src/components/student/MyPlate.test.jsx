import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import MyPlate from './MyPlate';

const baseMeal = { mealId: 1, mealType: 'DINNER', response: null };
const renderPlate = (props = {}) =>
  render(<MyPlate meal={baseMeal} msToDeadline={4857000} plateDishes={[]} onRespond={() => {}} onRemoveDish={() => {}} {...props} />);

describe('MyPlate', () => {
  it('asks the student to confirm attendance and shows the countdown', () => {
    renderPlate();
    expect(screen.getByText('Waiting for your response !')).toBeInTheDocument();
    expect(screen.getByText('01 : 20 : 57')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Confirm attendance' })).toBeInTheDocument();
  });
  it('requests an ATTENDING response when Confirm attendance is clicked', async () => {
    const onRespond = vi.fn();
    renderPlate({ onRespond });
    await userEvent.click(screen.getByRole('button', { name: 'Confirm attendance' }));
    expect(onRespond).toHaveBeenCalledWith('ATTENDING');
  });
  it('shows the empty plate once attendance is confirmed', () => {
    renderPlate({ meal: { ...baseMeal, response: { status: 'ATTENDING' } } });
    expect(screen.getByText('Your plate is empty !')).toBeInTheDocument();
  });
  it('lists dishes and estimated nutrition for the plate', () => {
    renderPlate({ meal: { ...baseMeal, response: { status: 'ATTENDING' } }, plateDishes: [{ id: 7, name: 'Dal Tadka', calories: 180, proteinG: 9, carbsG: 20, fatG: 6 }] });
    expect(screen.getByText('Dal Tadka')).toBeInTheDocument();
    expect(screen.getByLabelText('Estimated nutrition of your plate')).toHaveTextContent('180 kcal');
  });
  it('lets a student who declined change their mind', () => {
    renderPlate({ meal: { ...baseMeal, response: { status: 'NOT_ATTENDING' } } });
    expect(screen.getByText(/You won't be having Dinner/)).toBeInTheDocument();
  });
});
