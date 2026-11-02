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
# 2026-11 统一化后：五主题值定义在 ui/theme/Color.kt 的 PandoraColorTokens，Theme.kt 只按 key 绑定。
theme_dir = ROOT / 'android/app/src/main/java/com/pandora6ix/app/ui/theme'
color_kt = (theme_dir / 'Color.kt').read_text(encoding='utf-8')
theme_kt = (theme_dir / 'Theme.kt').read_text(encoding='utf-8')
token_sets = {m[1]: dict(re.findall(r'(\w+)\s*=\s*(?:Color\(0xFF([A-Fa-f\d]{6})\)|ElevatedWhite)', m[2]))
              for m in re.finditer(r'val (\w+Colors) = PandoraColorTokens\((.*?)\n\)', color_kt, re.S)}
for name, values in token_sets.items():
    for role in list(values):
        if values[role] == '':
            values[role] = 'FFFFFF'
themes = []
roles = {
    'appBackground': r'\b(?:colors|PandoraTheme\.colors|theme\.colors)\.appBackground\b',
    'surface': r'\b(?:colors|PandoraTheme\.colors|c)\.surface\b',
    'elevatedSurface': r'\b(?:colors|PandoraTheme\.colors|c)\.(?:elevatedSurface|panelSurface)\b',
    'primaryContainer': r'\b(?:colors|PandoraTheme\.colors|c)\.primaryContainer\b',
}
for key, label, token_name in re.findall(r'AppTheme\("([^"]+)", "([^"]+)", (\w+Colors)\)', theme_kt):
    values = token_sets[token_name]
    for role, pattern in roles.items():
        uses = [r for r in rows['backgrounds'] if re.search(pattern, r[3])]
        themes.append([key, role, '#' + values[role].upper(), len(uses), 'semantic token; selected theme maps this role', '; '.join(f'{r[0]}:{r[1]}' for r in uses)])
with (OUT / 'theme-background-uses.csv').open('w', encoding='utf-8-sig', newline='') as f:
    w = csv.writer(f)
    w.writerow(['theme', 'role', 'hex', 'explicit_background_use_sites', 'decision', 'locations'])
    w.writerows(themes)
summary['base_background_hex_count'] = len({r[2] for r in themes} | {'#FFFFFF'})

# 字面量颜色分类：Token 定义 / 组件 / 业务色板（头像、新建标签候选色）/ 页面硬编码 / 透明
def color_class(row):
    file, line, value = row[0], int(row[1]), row[2]
    if value == 'Color.Transparent':
        return 'transparent'
    if '/ui/theme/' in file:
        return 'token_definition'
    if '/ui/components/' in file:
        return 'component'
    src = (ROOT / file).read_text(encoding='utf-8-sig').split('\n')
    window = '\n'.join(src[max(0, line - 40):line])
    if re.search(r'val (AvatarPalette|TagColorPalette) = listOf\((?:(?!\n\)).)*$', window, re.S):
        return 'business_palette'
    return 'page_hardcoded'
summary['literal_color_classes'] = dict(sorted(Counter(color_class(r) for r in rows['colors']).items()))
summary['page_hardcoded_colors'] = [f'{r[0]}:{r[1]} {r[2]}' for r in rows['colors'] if color_class(r) == 'page_hardcoded']
summary['text_style_tokens'] = dict(sorted(Counter(r[4] for r in rows['typography'] if r[4].startswith('PandoraTypography')).items()))
summary['text_literal_font_sizes'] = sum(1 for r in rows['typography'] if r[2] != 'inherited')
(OUT / 'summary.json').write_text(json.dumps(summary, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
print(json.dumps({k: v if not isinstance(v, dict) else len(v) for k,v in summary.items()}, ensure_ascii=False, indent=2))
