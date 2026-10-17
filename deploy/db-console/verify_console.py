"""Validate H2 Console HTTP using private in-process credentials, never print passwords or rows."""
import re
import urllib.parse
import urllib.request


def read(origin, path, fields=None):
    request = urllib.request.Request(origin + path,
        data=urllib.parse.urlencode(fields).encode() if fields is not None else None)
    with urllib.request.urlopen(request, timeout=15) as response:
        return response.read().decode('utf-8')


def login(origin, jdbc_url, user, password):
    html = read(origin, '/')
    match = re.search(r'jsessionid=([a-zA-Z0-9]+)', html)
    if not match:
        raise AssertionError('Console session ID absent')
    session = match[1]
    html = read(origin, '/login.do?jsessionid=' + session,
                {'language': 'en', 'driver': 'org.h2.Driver', 'url': jdbc_url,
                 'user': user, 'password': password})
    return session, html


def verify(origin, jdbc_url, password):
    session, html = login(origin, jdbc_url, 'PANDORA_VIEWER', password)
    assert 'frame.jsp' in html or 'query.jsp' in html, 'Reader console login failed'
    def query(sql):
        return read(origin, '/query.do?jsessionid=' + session, {'sql': sql, 'rowcount': '100'})
    try:
        result = query('SELECT COUNT(*) AS PEOPLE_COUNT FROM PANDORA_VIEW.APP_USER')
        assert 'PEOPLE_COUNT' in result and '90096' not in result, 'Business view SELECT failed'
        count_match = re.search(r'<td[^>]*>([0-9]+)</td>', result)
        assert count_match, 'Count result absent'
        count = int(count_match[1])
        columns = query("SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA='PANDORA_VIEW' AND TABLE_NAME='APP_USER'")
        assert 'PASSWORD_HASH' not in columns, 'Password hash exposed in reader view'
        for sql in ['SELECT * FROM PUBLIC.AUTH_SESSION',
                    'SELECT PASSWORD_HASH FROM PUBLIC.APP_USER',
                    'UPDATE PUBLIC.APP_USER SET display_name=display_name WHERE 1=0',
                    'DELETE FROM PUBLIC.APP_USER WHERE 1=0']:
            assert '90096' in query(sql), 'Expected permission denial through the HTTP console'
    finally:
        read(origin, '/logout.do?jsessionid=' + session)
    bad_session, bad_login = login(origin, jdbc_url, 'sa', '')
    assert '28000' in bad_login, 'Blank administrator login was not rejected'
    return {'readerLogin': True, 'peopleCount': count, 'forbiddenQueries': 4,
            'blankAdminRejected': True, 'passwordHashHidden': True}
