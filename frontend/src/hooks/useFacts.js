import { useQuery } from '@tanstack/react-query';
import { factApi } from '../api/factApi';
import { FALLBACK_FACTS } from '../utils/constants';

/** Sustainability facts from the DB; falls back to built-in facts if the API fails. */
export function useFacts() {
  const query = useQuery({ queryKey: ['facts'], queryFn: factApi.list, staleTime: 10 * 60 * 1000 });
  const facts = query.data?.length ? query.data.map((f) => f.factText) : FALLBACK_FACTS;
  return { facts, isLoading: query.isLoading };
}
