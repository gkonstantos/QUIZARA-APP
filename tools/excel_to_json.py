# -*- coding: utf-8 -*-
"""Convert the ΚΟΥΙΖΑΡΑ question Excel files into the app's asset JSON files.

Usage (from the project root):
    python tools/excel_to_json.py "<folder with the .xlsx files>"

Writes app/src/main/assets/{classic_questions,questions,duel_specials,sudden_death}.json.
If there is no CONNECTION file, the ΒΡΕΣ ΤΗ ΣΥΝΔΕΣΗ questions already in duel_specials.json are kept.
Requires: pip install openpyxl
"""
import collections
import io
import json
import os
import sys
import unicodedata

import openpyxl

sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ASSETS = os.path.join(ROOT, 'app', 'src', 'main', 'assets')

# Category files: filename keyword -> app category id (the 6 ids are hard-coded in CATEGORIES).
CATEGORY_FILES = [
    ('SPORT', 'sports'),
    ('GEOGRAP', 'geography'),
    ('HISTOR', 'history'),
    ('SCIENCE', 'science'),
    ('GENERAL', 'general'),
    ('ART', 'art'),
]
# Special / sudden-death files: filename keyword -> builder key.
SPECIAL_FILES = {'TOP5': 'top5', 'FIND3': 'find3', 'PERSON_ID': 'person_id',
                 'COUNTRY_ID': 'country_id', 'RANK': 'rank', 'CONNECTION': 'connection'}
SPECIAL_TYPES = ('top5', 'find3', 'person_id', 'country_id', 'rank', 'connection')
CONNECTION_PROMPT = 'Τι συνδέει τα παρακάτω;\n\n'
SUDDEN_FILES = {'TF_SUDDEN': 'tf', 'MC_SUDDEN': 'multiple', 'OU_SUDDEN': 'over_under',
                'AB_SUDDEN': 'ab', 'CLOSEST_SUDDEN': 'closest'}

DIFFICULTY = {'ευκολη': 1, 'ευκολο': 1, 'μεσαια': 2, 'μετρια': 2, 'δυσκολη': 3, 'δυσκολο': 3}

warnings = []


def s(v):
    return '' if v is None else str(v).strip()


def norm(v):
    t = unicodedata.normalize('NFD', s(v))
    return ''.join(c for c in t if unicodedata.category(c) != 'Mn').lower()


def rows(path):
    ws = openpyxl.load_workbook(path, read_only=True, data_only=True).worksheets[0]
    data = list(ws.iter_rows(values_only=True))
    return [r for r in data[1:] if any(s(c) for c in r)]


def cell(r, i):
    return s(r[i]) if i < len(r) else ''


def classify(name):
    up = name.upper()
    for kw, key in SUDDEN_FILES.items():
        if kw in up:
            return 'sudden', key
    for kw, key in SPECIAL_FILES.items():
        if kw in up:
            return 'special', key
    for kw, cat in CATEGORY_FILES:
        if kw in up:
            return 'category', cat
    return None, None


def build_category(path, cat, classic, duel, seen):
    for r in rows(path):
        key, question, answer = cell(r, 0), cell(r, 3), cell(r, 4)
        if not question or not answer:
            warnings.append('%s %s: empty question/answer, skipped' % (cat, key))
            continue
        diff = DIFFICULTY.get(norm(r[1]) if len(r) > 1 else '')
        if diff is None:
            warnings.append('%s %s: unknown difficulty %r, skipped' % (cat, key, cell(r, 1)))
            continue
        uid = unique('%s_%s' % (cat, key or 'row'), seen)
        classic.append({'id': uid, 'category': cat, 'difficulty': diff,
                        'question': question, 'answer': answer, 'explanation': ''})
        duel.append({'category': cat, 'multiplier': diff, 'question': question,
                     'answers': [answer], 'correctIndex': 0, 'explanation': ''})


def build_special(path, kind, out):
    for r in rows(path):
        sid, prompt, expl = cell(r, 0), cell(r, 1), s(r[-1])
        if kind in ('top5', 'find3'):
            answers = [cell(r, i) for i in range(2, len(r) - 1) if cell(r, i)]
            need = 5 if kind == 'top5' else 3
            if (kind == 'top5' and len(answers) != 5) or len(answers) < need:
                warnings.append('%s %s: %d answers' % (kind, sid, len(answers)))
            out.append({'id': sid, 'type': kind, 'title': 'TOP 5' if kind == 'top5' else 'ΒΡΕΣ 3',
                        'prompt': prompt, 'answers': answers, 'explanation': expl})
        elif kind in ('person_id', 'country_id'):
            hints = [cell(r, i) for i in (2, 3, 4) if cell(r, i)]
            answer = cell(r, 5)
            if len(hints) != 3 or not answer:
                warnings.append('%s %s: %d clues, answer=%r' % (kind, sid, len(hints), answer))
            out.append({'id': sid, 'type': kind,
                        'title': 'PERSON ID' if kind == 'person_id' else 'COUNTRY ID',
                        'prompt': prompt, 'hints': hints, 'answers': [answer] if answer else [],
                        'explanation': cell(r, 6)})
        elif kind == 'rank':
            items = [cell(r, i) for i in range(2, len(r) - 1) if cell(r, i)]
            if len(items) < 2 or len(set(items)) != len(items):
                warnings.append('rank %s: %d items (duplicates?)' % (sid, len(items)))
            out.append({'id': sid, 'type': 'rank', 'title': 'ΒΑΛΤΑ ΣΕ ΣΕΙΡΑ', 'prompt': prompt,
                        'items': list(items), 'correctOrder': list(items), 'explanation': expl})
        elif kind == 'connection':
            # Columns: Κλειδί | Στοιχεία (e.g. "Ερμής · Αφροδίτη · Γη") | Σύνδεση | Επεξήγηση
            answer = cell(r, 2)
            if not prompt or not answer:
                warnings.append('connection %s: missing items or answer' % sid)
            out.append({'id': sid, 'type': 'connection', 'title': 'ΒΡΕΣ ΤΗ ΣΥΝΔΕΣΗ',
                        'prompt': CONNECTION_PROMPT + prompt, 'answers': [answer] if answer else [],
                        'explanation': cell(r, 3)})


def build_sudden(path, kind, out):
    for r in rows(path):
        sid, prompt = cell(r, 0), cell(r, 1)
        if kind == 'tf':
            c = cell(r, 2).upper()
            ci = 0 if 'ΣΩΣΤ' in c else 1 if 'ΛΑΘ' in c else -1
            entry = {'options': ['ΣΩΣΤΟ', 'ΛΑΘΟΣ'], 'correctIndex': ci, 'explanation': cell(r, 3)}
        elif kind == 'multiple':
            opts = [cell(r, i) for i in range(2, 6) if cell(r, i)]
            try:
                ci = int(float(cell(r, 6))) - 1
            except ValueError:
                ci = -1
            if not 0 <= ci < len(opts):
                ci = -1
            entry = {'options': opts, 'correctIndex': ci, 'explanation': cell(r, 7)}
        elif kind in ('over_under', 'ab'):
            c = cell(r, 4).upper()
            if kind == 'over_under':
                ci = 0 if c.startswith('O') or 'ΠΑΝ' in c else 1 if c.startswith('U') or 'ΚΑΤ' in c else -1
            else:
                ci = 0 if c in ('A', 'Α') else 1 if c in ('B', 'Β') else -1  # latin or greek
            entry = {'options': [cell(r, 2), cell(r, 3)], 'correctIndex': ci, 'explanation': cell(r, 5)}
        else:  # closest
            entry = {'unit': cell(r, 3), 'explanation': cell(r, 4)}
            try:
                entry['numericAnswer'] = float(cell(r, 2).replace(',', '.'))
            except ValueError:
                warnings.append('closest %s: bad number %r' % (sid, cell(r, 2)))
        if entry.get('correctIndex') == -1:
            warnings.append('%s %s: could not read the correct answer' % (kind, sid))
        out.append(dict({'id': sid, 'type': kind, 'prompt': prompt}, **entry))


def unique(base, seen):
    uid, n = base, 1
    while uid in seen:
        n += 1
        uid = '%s_%d' % (base, n)
    seen.add(uid)
    return uid


def dedupe_ids(items):
    seen = set()
    for e in items:
        new = unique(e['id'] or e['type'], seen)
        if new != e['id']:
            warnings.append('duplicate id %s -> %s' % (e['id'], new))
        e['id'] = new


def main():
    if len(sys.argv) != 2:
        sys.exit(__doc__)
    src = sys.argv[1]
    classic, duel, specials, sudden, ids = [], [], [], [], set()
    for name in sorted(os.listdir(src)):
        if not name.lower().endswith('.xlsx') or name.startswith('~$'):
            continue
        group, key = classify(name)
        path = os.path.join(src, name)
        if group == 'category':
            build_category(path, key, classic, duel, ids)
        elif group == 'special':
            build_special(path, key, specials)
        elif group == 'sudden':
            build_sudden(path, key, sudden)
        else:
            warnings.append('ignored file: ' + name)
    if not any(e['type'] == 'connection' for e in specials):
        current = os.path.join(ASSETS, 'duel_specials.json')
        if os.path.exists(current):
            with open(current, encoding='utf-8') as f:
                kept = [e for e in json.load(f) if e.get('type') == 'connection']
            specials.extend(kept)
            warnings.append('no CONNECTION file: kept the %d connection questions already in the app' % len(kept))
    dedupe_ids(specials)
    dedupe_ids(sudden)

    # The app refuses to start without these minimums.
    by_special = collections.Counter(e['type'] for e in specials)
    for t in SPECIAL_TYPES:
        if by_special[t] < 2:
            sys.exit('ERROR: need at least 2 "%s" specials, found %d' % (t, by_special[t]))
    if len(classic) < 36:
        sys.exit('ERROR: Classic needs at least 36 questions, found %d' % len(classic))
    buckets = {(q['category'], q['multiplier']) for q in duel}
    if len(buckets) < 18:
        sys.exit('ERROR: Duel needs a question for every category x difficulty (have %d of 18)' % len(buckets))

    for name, data in (('classic_questions.json', classic), ('questions.json', duel),
                       ('duel_specials.json', specials), ('sudden_death.json', sudden)):
        with open(os.path.join(ASSETS, name), 'w', encoding='utf-8', newline='\n') as f:
            json.dump(data, f, ensure_ascii=False, indent=2)
        print('%-24s %5d' % (name, len(data)))
    print('specials by type:', dict(by_special))
    print('sudden by type:  ', dict(collections.Counter(e['type'] for e in sudden)))
    print('warnings: %d' % len(warnings))
    for w in warnings:
        print('  -', w)


if __name__ == '__main__':
    main()
