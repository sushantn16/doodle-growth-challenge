import { getJson } from './http'

export interface Poll {
  id: string
  title: string
  /** ISO 8601, e.g. "2017-01-27T12:52:49.056Z" */
  createdAt: string
}

/** All polls, newest first. */
export function fetchPolls(): Promise<Poll[]> {
  return getJson<Poll[]>('/api/polls')
}
