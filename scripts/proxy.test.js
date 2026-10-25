// api/proxy.js 단위 테스트. 실행: node --test scripts/
// api/ 아래 파일은 Vercel 함수로 배포되므로 테스트는 scripts/에 둔다.
const test = require('node:test');
const assert = require('node:assert');
const { resolveTarget, pickHeaders, FORWARDED_REQUEST_HEADERS } = require('../api/proxy.js');

const base = new URL('https://backend.example.com');

test('resolveTarget keeps /api path and query on backend origin', () => {
  const target = resolveTarget('/api/events?from=1', base);
  assert.strictEqual(target.toString(), 'https://backend.example.com/api/events?from=1');
});

test('resolveTarget rejects protocol-relative and absolute URLs', () => {
  assert.strictEqual(resolveTarget('//evil.com/api/events', base), null);
  assert.strictEqual(resolveTarget('https://evil.com/api/events', base), null);
});

test('resolveTarget rejects paths outside /api/', () => {
  assert.strictEqual(resolveTarget('/internal/admin', base), null);
  assert.strictEqual(resolveTarget('/api/../internal', base), null);
});

test('pickHeaders drops host, hop-by-hop, length and vercel headers', () => {
  const picked = pickHeaders(
    {
      host: 'app.vercel.app',
      connection: 'keep-alive',
      'content-length': '10',
      'content-type': 'application/x-protobuf',
      'x-vercel-oidc-token': 'secret',
      'x-forwarded-for': '1.2.3.4',
      authorization: 'Bearer t',
    },
    FORWARDED_REQUEST_HEADERS,
  );
  assert.deepStrictEqual(picked, { 'content-type': 'application/x-protobuf', authorization: 'Bearer t' });
});
