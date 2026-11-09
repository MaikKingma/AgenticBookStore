import type { Problem } from './types'

// Both content types are accepted on purpose: the bookstore answers failures it maps itself as problem+json and
// everything else as plain JSON.
const ACCEPT = 'application/json, application/problem+json'

// Every failed request ends here, whatever its cause. The normalised problem is what the pages show.
export class ApiError extends Error {
  readonly problem: Problem

  constructor(problem: Problem) {
    super(problem.title)
    this.name = 'ApiError'
    this.problem = problem
  }
}

export function getJson<T>(path: string): Promise<T> {
  return request<T>('GET', path)
}

// The payload is sent as it is: no trimming, no case change, no validation. The server is the authority.
export function postJson<T>(path: string, payload: unknown): Promise<T> {
  return request<T>('POST', path, payload)
}

// One attempt, no retry, no cache. The path is always same-origin and starts with a slash.
async function request<T>(method: 'GET' | 'POST', path: string, payload?: unknown): Promise<T> {
  const headers: Record<string, string> = { Accept: ACCEPT }
  const init: RequestInit = { method, headers }
  if (payload !== undefined) {
    headers['Content-Type'] = 'application/json'
    init.body = JSON.stringify(payload)
  }

  let response: Response
  try {
    response = await fetch(path, init)
  } catch {
    throw new ApiError({ status: 0, title: 'Cannot reach the bookstore', details: [] })
  }

  const mediaType = mediaTypeOf(response)
  const json = await readJson(response, mediaType)
  if (response.ok && json !== undefined) {
    return json.value as T
  }
  throw new ApiError(toProblem(response, mediaType, json?.value))
}

// The media type of the answer without its parameters, for example "application/json" for "application/json;charset=UTF-8".
function mediaTypeOf(response: Response): string {
  return (response.headers.get('Content-Type') ?? '').split(';')[0].trim().toLowerCase()
}

// Undefined means: the body is not JSON by its content type, is empty, is malformed or could not be read.
async function readJson(response: Response, mediaType: string): Promise<{ value: unknown } | undefined> {
  if (mediaType !== 'application/json' && !mediaType.endsWith('+json')) {
    return undefined
  }
  try {
    return { value: JSON.parse(await response.text()) }
  } catch {
    return undefined
  }
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value)
}

function nonEmptyText(value: unknown): string | undefined {
  return typeof value === 'string' && value !== '' ? value : undefined
}

// A body is a ProblemDetail when it says so by its content type or carries a code. Any other body (the plain JSON of
// the framework error page, an HTML page of a proxy, nothing at all) gets a title from the error field, the status
// text or the status, so that every failure can be shown.
function toProblem(response: Response, mediaType: string, parsed: unknown): Problem {
  const status = response.status
  const fallbackTitle = response.statusText !== '' ? response.statusText : `Request failed (${status})`
  if (!isRecord(parsed)) {
    return { status, title: fallbackTitle, details: [] }
  }

  const isProblem = mediaType === 'application/problem+json' || typeof parsed.code === 'string'
  if (!isProblem) {
    return { status, title: nonEmptyText(parsed.error) ?? fallbackTitle, details: [] }
  }

  const problem: Problem = {
    status,
    title: nonEmptyText(parsed.title) ?? fallbackTitle,
    details: Array.isArray(parsed.details) ? parsed.details : [],
  }
  if (typeof parsed.detail === 'string') {
    problem.detail = parsed.detail
  }
  if (typeof parsed.code === 'string') {
    problem.code = parsed.code
  }
  return problem
}
