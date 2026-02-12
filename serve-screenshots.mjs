import http from 'node:http';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const DIST = path.join(__dirname, 'frontend', 'dist');
const PORT = 4173;
const BACKEND = 'http://localhost:8080';

const MIME = {
  '.html': 'text/html',
  '.js': 'application/javascript',
  '.css': 'text/css',
  '.json': 'application/json',
  '.png': 'image/png',
  '.jpg': 'image/jpeg',
  '.svg': 'image/svg+xml',
  '.woff': 'font/woff',
  '.woff2': 'font/woff2',
  '.ttf': 'font/ttf',
  '.eot': 'application/vnd.ms-fontobject',
  '.webmanifest': 'application/manifest+json',
};

const server = http.createServer((req, res) => {
  // Proxy API and actuator calls to backend
  if (req.url.startsWith('/api/') || req.url.startsWith('/actuator/') || req.url.startsWith('/ws/')) {
    const opts = {
      hostname: '127.0.0.1',
      port: 8080,
      path: req.url,
      method: req.method,
      headers: { ...req.headers, host: '127.0.0.1:8080' },
    };
    const proxy = http.request(opts, (proxyRes) => {
      res.writeHead(proxyRes.statusCode, proxyRes.headers);
      proxyRes.pipe(res);
    });
    proxy.on('error', () => {
      res.writeHead(502);
      res.end('Backend unavailable');
    });
    req.pipe(proxy);
    return;
  }

  // Serve static files with path traversal protection
  const safePath = path.normalize(req.url === '/' ? 'index.html' : req.url).replace(/^(\.\.(\/|\\|$))+/, '');
  let filePath = path.join(DIST, safePath);

  // Ensure resolved path is within DIST directory
  if (!filePath.startsWith(DIST)) {
    res.writeHead(403);
    res.end('Forbidden');
    return;
  }

  if (!fs.existsSync(filePath) || fs.statSync(filePath).isDirectory()) {
    // SPA fallback: serve index.html for all routes
    filePath = path.join(DIST, 'index.html');
  }

  const ext = path.extname(filePath);
  const contentType = MIME[ext] || 'application/octet-stream';

  try {
    const data = fs.readFileSync(filePath);
    res.writeHead(200, { 'Content-Type': contentType });
    res.end(data);
  } catch {
    res.writeHead(404);
    res.end('Not Found');
  }
});

server.listen(PORT, () => {
  console.log(`Static server with API proxy on http://localhost:${PORT}`);
});
