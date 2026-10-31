"""Static Compose inventory. Run from repo root; no dependencies, no app changes.

Counts declarations/call sites, never runtime pixels. Dynamic expressions remain
expressions; Material defaults and server tag colors are deliberately not guessed.
"""
import csv
import json
import re
from collections import Counter
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / 'docs/ui-audit-data'
OUT.mkdir(exist_ok=True)
files = sorted((ROOT / 'android/app/src/main').rglob('*.kt'))

def calls(text, pattern):
    for m in re.finditer(pattern + r'\s*\(', text):
        start = m.end()
        depth, quote, escape = 1, False, False
        for end in range(start, len(text)):
            c = text[end]
            if quote:
                if escape: escape = False
                elif c == '\\': escape = True
                elif c == '"': quote = False
            elif c == '"': quote = True
            elif c == '(': depth += 1
            elif c == ')':
                depth -= 1
                if not depth:
                    yield m, text[start:end]
                    break

def compact(s):
    return re.sub(r'\s+', ' ', s).strip()

def argument(text, start):
    depth, quoted, escaped = 0, False, False
    for end in range(start, len(text)):
        c = text[end]
        if quoted:
            if escaped: escaped = False
            elif c == '\\': escaped = True
            elif c == '"': quoted = False
        elif c == '"': quoted = True
        elif c in '([{': depth += 1
        elif c in ')]}':
            if depth == 0: return text[start:end]
            depth -= 1
        elif c == ',' and depth == 0: return text[start:end]
    return text[start:]

rows = {k: [] for k in ['colors', 'backgrounds', 'buttons', 'typography', 'spacing', 'shapes', 'components']}
for path in files:
    raw = path.read_text(encoding='utf-8-sig')
    # Preserve line numbers while excluding comments; sufficient for these sources.
    text = re.sub(r'/\*.*?\*/|//[^\n]*', lambda m: '\n' * m[0].count('\n'), raw, flags=re.S)
    loc = lambda m: [path.relative_to(ROOT).as_posix(), text[:m.start()].count('\n') + 1]
    for m in re.finditer(r'0x([\da-fA-F]{6,8})\b|Color\.(White|Black|Transparent|Red|Gray|LightGray|DarkGray|Blue|Green|Yellow|Cyan|Magenta)\b', text):
        value = '#' + m[1].upper() if m[1] else 'Color.' + m[2]
        rows['colors'].append(loc(m) + [value])
    for m, args in calls(text, r'\b(?:background|(?:\w+)?[Cc]olors)'):
        if m[0].startswith('background'):
            rows['backgrounds'].append(loc(m) + ['background', compact(args)])
    for m in re.finditer(r'\b(containerColor|backgroundColor)\s*=\s*', text):
        rows['backgrounds'].append(loc(m) + [m[1], compact(argument(text, m.end()))])
    for m, args in calls(text, r'\b(?:ButtonDefaults\.\w*[Cc]olors|ButtonDefaults\.buttonColors)'):
        rows['buttons'].append(loc(m) + [compact(args)])
    for m, args in calls(text, r'\bText'):
        size = re.search(r'fontSize\s*=\s*([^,\n]+)', args)
        weight = re.search(r'fontWeight\s*=\s*([^,\n]+)', args)
        style = re.search(r'(?<!\w)style\s*=\s*([^,\n]+)', args)
        rows['typography'].append(loc(m) + [compact(size[1]) if size else 'inherited', compact(weight[1]) if weight else 'inherited', compact(style[1]) if style else 'LocalTextStyle', compact(args)])
    for m, args in calls(text, r'\b(?:padding|PaddingValues|spacedBy)'):
        rows['spacing'].append(loc(m) + [m[0].split('(')[0].strip(), compact(args)])
    for m, args in calls(text, r'\bRoundedCornerShape'):
        rows['shapes'].append(loc(m) + [compact(args)])
    for m in re.finditer(r'@Composable\s+(?:private\s+|internal\s+)?fun\s+(?:<[^>]+>\s*)?([\w.]+)', text):
        rows['components'].append(loc(m) + [m[1]])

headers = {
    'colors': ['file', 'line', 'literal'],
    'backgrounds': ['file', 'line', 'role', 'expression'],
    'buttons': ['file', 'line', 'color_arguments'],
    'typography': ['file', 'line', 'size', 'weight', 'style', 'arguments'],
    'spacing': ['file', 'line', 'kind', 'arguments'],
    'shapes': ['file', 'line', 'arguments'],
    'components': ['file', 'line', 'name'],
}
for name, data in rows.items():
    with (OUT / (name + '.csv')).open('w', encoding='utf-8-sig', newline='') as f:
        writer = csv.writer(f)
        writer.writerow(headers[name])
        writer.writerows(data)
summary = {
    'scope': 'android/app/src/main/**/*.kt; comments excluded; static declarations, not runtime resolved colors',
    'kotlin_files': len(files),
    'composable_functions': len(rows['components']),
    'literal_colors': dict(sorted(Counter(r[2] for r in rows['colors']).items())),
    'background_expressions': dict(sorted(Counter(r[3] for r in rows['backgrounds']).items())),
    'button_color_argument_sets': dict(sorted(Counter(r[2] for r in rows['buttons']).items())),
    'text_size_weight_pairs': dict(sorted(Counter(r[2] + ' / ' + r[3] for r in rows['typography']).items())),
    'shape_argument_sets': dict(sorted(Counter(r[2] for r in rows['shapes']).items())),
    'spacing_numeric_dp': sorted({float(v) for r in rows['spacing'] for v in re.findall(r'(?<![\w.])(\d+(?:\.\d+)?)\.dp', r[3])}),
    'spacing_argument_sets': len({r[3] for r in rows['spacing']}),
    'calls': {k: len(v) for k, v in rows.items()},
}
# Role-use counts deliberately stay separate from theme variant values: the same
# call site resolves to a different Hex when the selected theme changes.
theme = (ROOT / 'android/app/src/main/java/com/pandora6ix/app/ui/theme/Theme.kt').read_text(encoding='utf-8')
themes = []
roles = {
    'background': r'\b(?:theme|t|LocalAppTheme\.current)\.background\b',
    'backgroundDeep': r'\b(?:theme|t|LocalAppTheme\.current)\.backgroundDeep\b',
    'dashboard': r'\bWarmDashboard\b|\bLocalAppTheme\.current\.dashboard\b',
    'card': r'\b(?:Cream|WarmCard)\b',
    'cardDeep': r'\bCreamDeep\b',
}
for match in re.finditer(r'AppTheme\(\s*"([^"]+)"\s*,\s*"([^"]+)"(.*?)\n\s*\)', theme, re.S):
    values = dict(re.findall(r'(\w+)\s*=\s*Color\(0xFF([A-Fa-f\d]{6})\)', match[3]))
    for role, pattern in roles.items():
        uses = [r for r in rows['backgrounds'] if re.search(pattern, r[3])]
        themes.append([match[1], role, '#' + values[role], len(uses), 'retain identity; consolidate background roles', '; '.join(f'{r[0]}:{r[1]}' for r in uses)])
with (OUT / 'theme-background-uses.csv').open('w', encoding='utf-8-sig', newline='') as f:
    w = csv.writer(f)
    w.writerow(['theme', 'role', 'hex', 'explicit_background_use_sites', 'decision', 'locations'])
    w.writerows(themes)
summary['base_background_hex_count'] = len({r[2] for r in themes} | {'#FFFFFF'})
(OUT / 'summary.json').write_text(json.dumps(summary, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
print(json.dumps({k: v if not isinstance(v, dict) else len(v) for k,v in summary.items()}, ensure_ascii=False, indent=2))
