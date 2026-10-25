/**
 * Vercel Serverless Function - API Proxy
 * 프론트엔드 /api/* 요청을 Ktor 백엔드(BACKEND_API_URL)로 그대로 전달한다.
 * 본문은 application/x-protobuf 바이트이므로 파싱하지 않고 원본 스트림을 그대로 보낸다.
 */
const FORWARDED_REQUEST_HEADERS = ['accept', 'accept-language', 'authorization', 'content-type', 'grpc-timeout', 'user-agent'];
const FORWARDED_RESPONSE_HEADERS = ['cache-control', 'content-type', 'etag', 'last-modified', 'www-authenticate'];
const UPSTREAM_TIMEOUT_MS = 10_000;

function readRawBody(req) {
  return new Promise((resolve, reject) => {
    const chunks = [];
    req.on('data', (chunk) => chunks.push(chunk));
    req.on('end', () => resolve(Buffer.concat(chunks)));
    req.on('error', reject);
  });
}

// '//evil.com/x' 같은 프로토콜 상대·절대 URL이 base를 무시하지 않도록 origin과 경로를 검증한다.
function resolveTarget(reqUrl, base) {
  const target = new URL(reqUrl, base);
  if (target.origin !== base.origin || !target.pathname.startsWith('/api/')) {
    return null;
  }
  return target;
}

function pickHeaders(source, names) {
  const picked = {};
  for (const name of names) {
    if (source[name]) picked[name] = source[name];
  }
  return picked;
}

async function handler(req, res) {
  const backendUrl = process.env.BACKEND_API_URL;
  if (!backendUrl) {
    return res.status(503).json({ error: 'Service Unavailable', message: 'Backend is not configured' });
  }

  const base = URL.canParse(backendUrl) ? new URL(backendUrl) : null;
  if (base?.protocol !== 'https:') {
    console.error('API Proxy: BACKEND_API_URL must be a valid https URL');
    return res.status(503).json({ error: 'Service Unavailable', message: 'Backend is misconfigured' });
  }

  const target = resolveTarget(req.url, base);
  if (!target) {
    return res.status(400).json({ error: 'Bad Request' });
  }

  const headers = pickHeaders(req.headers, FORWARDED_REQUEST_HEADERS);
  const clientIp = req.headers['x-vercel-forwarded-for'] || req.socket?.remoteAddress;
  if (clientIp) headers['x-forwarded-for'] = clientIp;

  try {
    const fetchOptions = {
      method: req.method,
      headers,
      signal: AbortSignal.timeout(UPSTREAM_TIMEOUT_MS),
    };
    if (req.method !== 'GET' && req.method !== 'HEAD') {
      fetchOptions.body = await readRawBody(req);
    }

    const response = await fetch(target, fetchOptions);

    res.status(response.status);
    for (const name of FORWARDED_RESPONSE_HEADERS) {
      const value = response.headers.get(name);
      if (value) res.setHeader(name, value);
    }

    const buffer = await response.arrayBuffer();
    return res.send(Buffer.from(buffer));
  } catch (error) {
    console.error('API Proxy error:', error);
    return res.status(502).json({ error: 'Bad Gateway' });
  }
}

module.exports = handler;
module.exports.resolveTarget = resolveTarget;
module.exports.pickHeaders = pickHeaders;
module.exports.FORWARDED_REQUEST_HEADERS = FORWARDED_REQUEST_HEADERS;
