import Modal from '../common/Modal';
import { MEAL_LABELS, RESPONSE_STATUS } from '../../utils/constants';

const MESSAGES = {
  [RESPONSE_STATUS.ATTENDING]: (meal) => `You are confirming your attendance for ${meal}`,
  [RESPONSE_STATUS.NOT_ATTENDING]: (meal) => `You are letting the kitchen know you won't attend ${meal}`,
  [RESPONSE_STATUS.NOT_SURE]: (meal) => `You are marking ${meal} as "not sure yet"`,
};

/** Confirmation dialog shown before any attendance response is submitted. */
export default function ResponseModal({ status, mealType, busy, onCancel, onConfirm }) {
  const meal = mealType ? MEAL_LABELS[mealType] : '';
  return (
    <Modal open={Boolean(status)} onClose={onCancel} label="Confirm your response">
      <p className="text-center text-xl font-medium">{status ? MESSAGES[status](meal) : ''}</p>
      <div className="mt-6 flex justify-center gap-6">
        <button type="button" onClick={onCancel} className="border-2 border-tangerine px-6 py-2 text-lg font-medium text-tangerine hover:bg-butter">✕ Cancel</button>
        <button type="button" onClick={onConfirm} disabled={busy} className="bg-tangerine px-6 py-2 text-lg font-medium text-white hover:brightness-105 disabled:opacity-60">✓ Confirm</button>
      </div>
    </Modal>
  );
}
