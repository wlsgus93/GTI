/**
 * Spring `@RestControllerAdvice` ProblemDetail (RFC 7807) 매핑.
 *
 * - 401: 토큰 만료 / 미인증
 * - 422: 리소스 미존재 등 도메인 예외
 * - 5xx: 서버 장애
 */

export class ApiError extends Error {
  readonly status: number;
  readonly detail: string | undefined;
  readonly title: string | undefined;

  constructor(status: number, message: string, opts?: { detail?: string; title?: string }) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.detail = opts?.detail;
    this.title = opts?.title;
  }

  get isUnauthorized(): boolean {
    return this.status === 401;
  }

  get isNotFound(): boolean {
    return this.status === 404 || this.status === 422;
  }

  get isServer(): boolean {
    return this.status >= 500;
  }
}

type ProblemDetail = {
  type?: string;
  title?: string;
  status?: number;
  detail?: string;
  instance?: string;
};

export async function toApiError(response: Response): Promise<ApiError> {
  let body: ProblemDetail | { message?: string } | null = null;
  try {
    body = (await response.json()) as ProblemDetail | { message?: string };
  } catch {
    body = null;
  }

  const detail =
    (body && "detail" in body && body.detail) ||
    (body && "message" in body && body.message) ||
    response.statusText ||
    "요청 실패";

  const title = body && "title" in body ? body.title : undefined;
  return new ApiError(response.status, String(detail), { detail: String(detail), title });
}
