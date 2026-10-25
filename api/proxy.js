/**
 * Vercel Serverless Function - API Proxy
 * 프론트엔드 (/api/*) 요청을 Ktor 백엔드 서버(BACKEND_API_URL 또는 로컬/원격 Ktor 인스턴스)로 프록시하거나 상태를 반환합니다.
 */
module.exports = async (req, res) => {
  const backendUrl = process.env.BACKEND_API_URL;

  if (!backendUrl) {
    // 백엔드 URL이 별도로 설정되지 않은 경우 기본 상태 응답
    return res.status(200).json({
      status: "ok",
      message: "Kotlin Calendar Vercel Serverless API Gateway",
      timestamp: new Date().toISOString(),
      path: req.url,
      method: req.method,
      configuredBackend: false
    });
  }

  try {
    const targetUrl = new URL(req.url, backendUrl).toString();

    // Headers forwarding
    const headers = { ...req.headers };
    delete headers.host;

    const fetchOptions = {
      method: req.method,
      headers: headers,
    };

    if (req.method !== 'GET' && req.method !== 'HEAD' && req.body) {
      fetchOptions.body = typeof req.body === 'string' ? req.body : JSON.stringify(req.body);
    }

    const response = await fetch(targetUrl, fetchOptions);
    const contentType = response.headers.get('content-type') || 'application/octet-stream';

    res.status(response.status);
    res.setHeader('content-type', contentType);

    const buffer = await response.arrayBuffer();
    return res.send(Buffer.from(buffer));
  } catch (error) {
    console.error('API Proxy error:', error);
    return res.status(502).json({
      error: "Bad Gateway",
      message: error.message || "Failed to reach backend server"
    });
  }
};
