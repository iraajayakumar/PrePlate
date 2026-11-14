import { useEffect, useState } from 'react';
import ShadowCard from '../common/ShadowCard';
import { useFacts } from '../../hooks/useFacts';

export default function FoodForThought({ intervalMs = 6000 }) {
  const { facts } = useFacts();
  const [index, setIndex] = useState(0);

  useEffect(() => {
    const id = setInterval(() => setIndex((i) => (i + 1) % facts.length), intervalMs);
    return () => clearInterval(id);
  }, [facts.length, intervalMs]);

  const current = index % facts.length;
  return (
    <ShadowCard tone="sage" className="p-5">
      <h2 className="text-outline-shadow text-2xl font-bold uppercase tracking-wide text-white sm:text-3xl">Food for thought</h2>
      <p key={index} className="pp-pop mt-2 min-h-[4.5rem] text-lg font-medium leading-snug text-butter">{facts[current]}</p>
      <div className="mt-3 flex justify-center gap-2" role="tablist" aria-label="Choose fact">
        {facts.slice(0, 6).map((_, i) => (
          <button key={i} type="button" role="tab" aria-selected={i === current} aria-label={`Fact ${i + 1}`}
            onClick={() => setIndex(i)} className={`h-3 w-3 rounded-full ${i === current ? 'bg-white' : 'bg-white/50'}`} />
        ))}
      </div>
    </ShadowCard>
  );
}
