"""Isolated avatar transport fixture; never connects to Pandora or stores credentials.

Run on the development host, then pass avatarFixtureUrl to instrumentation.
The real backend's storage and authorization are tested separately by its suite.
"""
import argparse
import base64
import json
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

parser = argparse.ArgumentParser()
parser.add_argument('--port', type=int, default=18766)
args = parser.parse_args()
images = {}

class Handler(BaseHTTPRequestHandler):
    protocol_version = 'HTTP/1.1'

    def log_message(self, *_):
        pass

    def reply(self, status, data=b''):
        self.send_response(status)
        self.send_header('Content-Length', str(len(data)))
        self.end_headers()
        self.wfile.write(data)
        self.wfile.flush()

    def do_GET(self):
        if self.path == '/health':
            return self.reply(200, b'avatar-fixture-only')
        if self.path != '/avatar-fixture/users/avatar-test-subject/avatar':
            return self.reply(404)
        if self.headers.get('Authorization') not in ('Bearer fixture-subject', 'Bearer fixture-manager'):
            return self.reply(403)
        image = images.get('subject')
        self.reply(200, image) if image else self.reply(404)

    def do_PUT(self):
        if self.path != '/avatar-fixture/users/avatar-test-subject/avatar':
            return self.reply(404)
        if self.headers.get('Authorization') != 'Bearer fixture-subject':
            return self.reply(403)
        value = json.loads(self.rfile.read(int(self.headers['Content-Length'])))['dataUrl']
        if not value.startswith('data:image/jpeg;base64,'):
            return self.reply(422)
        images['subject'] = base64.b64decode(value.split(',', 1)[1], validate=True)
        self.reply(204)

print(f'avatar fixture listening on {args.port}', flush=True)
ThreadingHTTPServer(('0.0.0.0', args.port), Handler).serve_forever()
