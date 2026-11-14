import { Cell, Pie, PieChart, ResponsiveContainer } from 'recharts';
import { MEAL_LABELS } from '../../utils/constants';

const COLORS = ['#7D9B65', '#FFF3A1'];

/** Compact doughnut: meals attended out of meals available for one meal category. */
export default function ParticipationDonut({ mealType, available, selected, attended }) {
  const hasData = available > 0;
  const data = hasData
    ? [{ name: 'Attended', value: attended }, { name: 'Not attended', value: Math.max(available - attended, 0) }]
    : [{ name: 'none', value: 1 }];

  return (
    <div className="border-2 border-forest bg-white p-3 text-center shadow-[4px_4px_0_0_var(--color-tangerine)]">
      <h3 className="font-semibold text-forest">{MEAL_LABELS[mealType]}</h3>
      <div className="relative">
        <ResponsiveContainer width="100%" height={140}>
          <PieChart>
            <Pie data={data} dataKey="value" innerRadius={40} outerRadius={58} startAngle={90} endAngle={-270}
              stroke="#1F4B33" strokeWidth={1} isAnimationActive={false}>
              {data.map((_, i) => <Cell key={i} fill={hasData ? COLORS[i] : '#eee'} />)}
            </Pie>
          </PieChart>
        </ResponsiveContainer>
        <div className="pointer-events-none absolute inset-0 flex items-center justify-center text-lg font-semibold">
          {hasData ? `${attended}/${available}` : '--'}
        </div>
      </div>
      <p className="text-sm">Attended <strong>{attended}</strong> of {available} meals</p>
      <p className="text-xs text-ink/60">Selected: {selected}</p>
    </div>
  );
}
