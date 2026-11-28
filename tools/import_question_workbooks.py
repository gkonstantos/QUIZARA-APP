# -*- coding: utf-8 -*-
r"""Build the four question files from the reviewed question workbooks.

Usage (from the project root):
    python tools/import_question_workbooks.py --classic "OLES OI KLASIKES KATHGORIES.xlsm" \
        --sudden "QUIZARA_Xafnikos_Thanatos_...xlsx" --specials "QUIZARA_SPECIAL_6_KATIGORIES_...xlsx" \
        [--suggestions "Quizara προτάσεις (Responses).xlsx"]
    python tools/import_question_workbooks.py --update --suggestions "Quizara προτάσεις (Responses).xlsx"

Each workbook is the complete set for its modes; it replaces what is in
app/src/main/assets. Workbooks can be given on their own (e.g. only --specials): the modes without
a workbook keep the app's current questions. Approved player suggestions already in a file are kept
when it is rebuilt without --suggestions.
  --classic   6 category sheets      -> classic_questions.json and questions.json (Σπέσιαλ board)
  --sudden    5 Sudden Death sheets  -> sudden_death.json
  --specials  6 Special sheets       -> duel_specials.json
  --suggestions (optional) the players' suggestions sheet downloaded from Google Sheets (.xlsx or
              .csv); rows with "ναι" in the "Εγκρίθηκε" column are added to the classic questions
              or, for the other kinds, to the Specials.
  --update    no workbooks needed: keep the questions already in the assets and only replace the
              suggested ones (ids "suggestion_N", N = sheet row) with the sheet's approved rows.
              The GitHub job .github/workflows/import-suggestions.yml runs this every 5 days.

Rows the reviewers took out are skipped (the specials workbook's "ΑΔΙΚΑ_ΔΥΣΚΟΛΕΣ_ΕΛΕΓΧΟΣ" list and
TOP 5 rows "moved out of the pool"). Repeated questions are dropped: same text and answer, or nearly
the same text with the same answer. Questions that only share a template ("capital of X?") are kept.
The script stops if a mode would be left without the questions the app needs to start.
Requires: pip install openpyxl
"""
import argparse
import collections
import difflib
import io
import json
import os
import re
import sys
import unicodedata

import openpyxl

sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ASSETS = os.path.join(ROOT, 'app', 'src', 'main', 'assets')

CLASSIC_SHEETS = {'Γεωγραφία': 'geography', 'Ιστορία': 'history', 'Τέχνη': 'art',
                  'Αθλητικά': 'sports', 'Γενικές': 'general', 'Επιστήμη': 'science'}
SUDDEN_SHEETS = {'ΣΩΣΤΟ_ΛΑΘΟΣ': 'tf', 'ΠΟΛΛΑΠΛΗΣ': 'multiple', 'ΠΑΝΩ_ΚΑΤΩ': 'over_under',
                 'ΔΙΑΛΕΞΕ': 'ab', 'ΠΙΟ_ΚΟΝΤΑ': 'closest'}
SPECIAL_TYPES = ('top5', 'find3', 'person_id', 'country_id', 'rank', 'connection')
# Category names as players see them in the app (Τέχνες) and as the workbooks name them (Τέχνη).
SUGGESTION_CATEGORIES = {'αθλητικα': 'sports', 'τεχνεσ': 'art', 'τεχνη': 'art', 'επιστημη': 'science',
                         'ιστορια': 'history', 'γενικεσ': 'general', 'γεωγραφια': 'geography'}
# Question types as the form names them ("Είδος ερώτησης"); rows without a type are classic.
SUGGESTION_TYPES = {'κλασικη': 'classic', 'top 5': 'top5', 'βρεσ 3': 'find3', 'βαλ τα σε σειρα': 'rank',
                    'βρεσ τη συνδεση': 'connection', 'person id': 'person_id', 'country id': 'country_id'}
APPROVED = {'ναι', 'ν', 'yes', 'y', 'x', 'ok', 'οκ', '1', 'true', '✓', '✔'}
DIFFICULTY = {'ευκολη': 1, 'ευκολο': 1, 'μεσαια': 2, 'μετρια': 2, 'δυσκολη': 3, 'δυσκολο': 3}
CONNECTION_PROMPT = 'Τι συνδέει τα παρακάτω;\n\n'
SAME_TEXT = 0.86          # at or above: lightly edited version of the same question
# Explanation cells that are editors' notes rather than text for players.
REVIEW_NOTE = re.compile(r'προτεινόμεν|έλεγχ|επανεξέταση|επιμέλεια', re.IGNORECASE)

warnings = []


# ---------- helpers ----------

def text(v):
    """Cell value as text; whole numbers lose Excel's trailing .0 (26.0 -> 26)."""
    if v is None:
        return ''
    if isinstance(v, float) and v.is_integer():
        return str(int(v))
    return str(v).strip()


def norm(v):
    t = unicodedata.normalize('NFD', text(v))
    t = ''.join(c for c in t if unicodedata.category(c) != 'Mn').lower().replace('ς', 'σ')
    return re.sub(r'\s+', ' ', re.sub(r'[^0-9a-zα-ω]+', ' ', t)).strip()


def sheet_rows(path, sheet):
    ws = openpyxl.load_workbook(path, read_only=True, data_only=True)[sheet]
    data = list(ws.iter_rows(values_only=True))
    hdr = [text(h) for h in data[0]]
    out = []
    for n, r in enumerate(data[1:], start=2):
        if any(text(c) for c in r):
            out.append(dict({'_row': n}, **{h: (r[i] if i < len(r) else None) for i, h in enumerate(hdr) if h}))
    return out


def difficulty(raw, where):
    key = norm(raw)
    if key in DIFFICULTY:
        return DIFFICULTY[key]
    warnings.append('%s: difficulty %r unknown, using medium' % (where, text(raw)))
    return 2


def similar(a, b):
    return difflib.SequenceMatcher(None, a, b, autojunk=False).ratio()


def different_subject(a, b):
    """Near-identical wording that still asks about something else: other numbers, «quoted» terms or names."""
    for pattern in (r'\d+', r'«([^»]+)»'):
        x, y = {norm(m) for m in re.findall(pattern, a)}, {norm(m) for m in re.findall(pattern, b)}
        if x and y and x != y:
            return True
    def names(t):
        return {norm(w) for w in re.findall(r'\w+', t)[1:] if w[0].isupper()}
    x, y = names(a), names(b)
    return bool(x - y) and bool(y - x)


def same_answer(a, b):
    a, b = norm(a), norm(b)
    return a == b or (a and b and similar(a, b) >= 0.9)


class Dedupe:
    """Keeps the first of repeated questions: same text, or nearly the same text with the same answer."""

    def __init__(self, label):
        self.label = label
        self.seen = []                      # (normalised text, answer key, description, original text)
        self.words = collections.defaultdict(list)
        self.dropped = []
        self.exact = {}

    def add(self, question, answer_key, description):
        q = norm(question)
        if (q, answer_key) in self.exact:
            self.dropped.append((description, self.exact[(q, answer_key)]))
            return False
        toks = {w for w in q.split() if len(w) >= 4}
        cands = collections.Counter(i for w in toks for i in self.words[w])
        for i, _ in cands.most_common(40):
            q2, a2, d2, raw2 = self.seen[i]
            if a2 == answer_key and similar(q2, q) >= SAME_TEXT and not different_subject(raw2, question):
                self.dropped.append((description, d2))
                return False
        for w in toks:
            self.words[w].append(len(self.seen))
        self.exact[(q, answer_key)] = description
        self.seen.append((q, answer_key, description, question))
        return True


def unique_id(base, used):
    uid, n = base, 1
    while uid in used:
        n += 1
        uid = '%s_%d' % (base, n)
    used.add(uid)
    return uid


# ---------- classic ----------

def suggestion_rows(path):
    """Approved rows of the suggestions sheet (Google Forms responses), .xlsx or .csv."""
    if path.lower().endswith('.csv'):
        import csv
        with open(path, encoding='utf-8-sig', newline='') as f:
            rows = [dict(r, _row=n) for n, r in enumerate(csv.DictReader(f), start=2)]
    else:
        wb = openpyxl.load_workbook(path, read_only=True, data_only=True)
        rows = sheet_rows(path, wb.sheetnames[0])
    if rows and not any(text(k) == 'Εγκρίθηκε' for k in rows[0]):
        sys.exit('ERROR: %s has no "Εγκρίθηκε" column; is it the form\'s responses sheet?' % path)
    out = []
    for r in rows:
        r = {text(k): v for k, v in r.items()}
        mark = text(r.get('Εγκρίθηκε'))
        if mark.lower() in APPROVED or norm(mark) in APPROVED:
            r['_type'] = SUGGESTION_TYPES.get(norm(r.get('Είδος ερώτησης')), 'classic')
            r['_lines'] = [x.strip() for k, v in r.items() if text(k).startswith('Απαντήσεις')
                           for x in text(v).split('\n') if x.strip()]
            out.append(r)
    return out


def add_special_suggestions(out, used, suggestions):
    """Add approved special-question suggestions, dropping ones that repeat a special already in the file."""
    def key(kind, prompt, items, answer):
        if kind in ('top5', 'find3'):
            return prompt, tuple(sorted(map(norm, items)))
        if kind == 'rank':
            return prompt + ' ' + ' '.join(items), tuple(map(norm, items))
        if kind == 'connection':
            return ' '.join(items), tuple(sorted(map(norm, items)))
        return ' '.join(items), (norm(answer),) + tuple(map(norm, items))

    dd = collections.defaultdict(lambda: None)
    for q in out:
        kind = q['type']
        if dd[kind] is None:
            dd[kind] = Dedupe(kind)
        if kind == 'connection':
            items = q['prompt'].split('\n\n', 1)[-1].split(' · ')
        elif kind in ('person_id', 'country_id'):
            items = q['hints']
        else:
            items = q.get('correctOrder') or q.get('answers') or []
        dd[kind].add(*key(kind, q['prompt'], items, (q.get('answers') or [''])[0]), description=q['id'])

    added = 0
    for r in suggestions:
        kind = r['_type']
        if kind == 'classic':
            continue
        where = 'suggestion row %d' % r['_row']
        prompt, answer, lines = text(r.get('Ερώτηση')), text(r.get('Απάντηση')), r['_lines']
        need = {'top5': len(lines) == 5 and prompt, 'find3': len(lines) >= 3 and prompt,
                'rank': 4 <= len(lines) <= 5 and prompt, 'connection': 3 <= len(lines) <= 5 and answer,
                'person_id': len(lines) == 3 and answer, 'country_id': len(lines) == 3 and answer}
        if not need.get(kind):
            warnings.append(where + ': %s suggestion is missing its question, answer or lines, skipped' % kind)
            continue
        if dd[kind] is None:
            dd[kind] = Dedupe(kind)
        if not dd[kind].add(*key(kind, prompt, lines, answer), description=where):
            continue
        uid = unique_id('suggestion_%d' % r['_row'], used)
        title = {'top5': 'TOP 5', 'find3': 'ΒΡΕΣ 3', 'rank': 'ΒΑΛ’ ΤΑ ΣΕ ΣΕΙΡΑ', 'connection': 'ΒΡΕΣ ΤΗ ΣΥΝΔΕΣΗ',
                 'person_id': 'PERSON ID', 'country_id': 'COUNTRY ID'}[kind]
        entry = {'id': uid, 'type': kind, 'title': title, 'prompt': prompt, 'explanation': ''}
        if kind in ('top5', 'find3'):
            entry['answers'] = lines
        elif kind == 'rank':
            entry['items'], entry['correctOrder'] = list(lines), list(lines)
        elif kind == 'connection':
            entry['prompt'] = CONNECTION_PROMPT + ' · '.join(lines)
            entry['answers'] = [answer]
        else:
            entry['prompt'] = 'Ποιο πρόσωπο ψάχνουμε;' if kind == 'person_id' else 'Ποια χώρα ψάχνουμε;'
            entry['hints'], entry['answers'] = lines, [answer]
        out.append(entry)
        added += 1
    print('approved special suggestions added: %d' % added)


def build_classic(path, suggestions=None):
    classic, used, dd = [], set(), Dedupe('classic')
    for sheet, cat in CLASSIC_SHEETS.items():
        for r in sheet_rows(path, sheet):
            q, a = text(r.get('Ερώτηση')), text(r.get('Απάντηση'))
            where = '%s row %d' % (sheet, r['_row'])
            if not q or not a:
                warnings.append(where + ': empty question or answer, skipped')
                continue
            if not dd.add(q, norm(a), where + ': ' + q[:60]):
                continue
            src = text(r.get('ID') or r.get('Κλειδί'))
            base = src if re.match(r'^[A-Za-z]+_', src) else ('%s_%s' % (cat, src) if src else '%s_row%d' % (cat, r['_row']))
            classic.append({'id': unique_id(base, used), 'category': cat,
                            'difficulty': difficulty(r.get('Δυσκολία'), where),
                            'question': q, 'answer': a, 'explanation': ''})
    add_classic_suggestions(classic, used, dd, suggestions)
    return classic, duel_of(classic), dd.dropped


def duel_of(classic):
    """The Σπέσιαλ board's questions.json, made from the classic questions."""
    return [{'category': c['category'], 'multiplier': c['difficulty'], 'question': c['question'],
             'answers': [c['answer']], 'correctIndex': 0, 'explanation': ''} for c in classic]


def add_classic_suggestions(classic, used, dd, suggestions):
    added = 0
    for r in suggestions or []:
        if r['_type'] != 'classic':
            continue
        q, a = text(r.get('Ερώτηση')), text(r.get('Απάντηση'))
        where = 'suggestion row %d' % r['_row']
        cat = SUGGESTION_CATEGORIES.get(norm(r.get('Κατηγορία')))
        if not q or not a or not cat:
            warnings.append(where + ': needs a question, an answer and a known category, skipped')
            continue
        if not dd.add(q, norm(a), where + ': ' + q[:60]):
            continue
        classic.append({'id': unique_id('suggestion_%d' % r['_row'], used), 'category': cat,
                        'difficulty': difficulty(r.get('Δυσκολία'), where),
                        'question': q, 'answer': a, 'explanation': ''})
        added += 1
    if suggestions:
        print('approved suggestions added: %d' % added)


# ---------- sudden death ----------

def number(v, where):
    if isinstance(v, (int, float)):
        return float(v)
    t = text(v).replace(' ', '')
    if re.fullmatch(r'-?\d+', t):
        return float(t)
    if re.fullmatch(r'-?\d+[.,]\d+', t):
        if re.fullmatch(r'-?\d{1,3}[.,]\d{3}', t):
            warnings.append('%s: number %r could be a decimal or a thousands separator; read as decimal' % (where, t))
        return float(t.replace(',', '.'))
    raise ValueError('%s: cannot read number %r' % (where, text(v)))


def build_sudden(path):
    out, used = [], set()
    for sheet, kind in SUDDEN_SHEETS.items():
        dd = Dedupe(kind)
        for r in sheet_rows(path, sheet):
            prompt = text(r.get('Ερώτηση'))
            where = '%s row %d' % (sheet, r['_row'])
            if not prompt:
                continue
            entry = {'id': unique_id(text(r.get('ID')) or '%s_row%d' % (kind, r['_row']), used),
                     'type': kind, 'prompt': prompt}
            if kind == 'closest':
                entry['numericAnswer'] = number(r.get('Αριθμητική απάντηση'), where)
                entry['unit'] = text(r.get('Μονάδα'))
                key = entry['numericAnswer']
            else:
                options = [text(r.get('Επιλογή ' + x)) for x in 'ΑΒΓΔ' if text(r.get('Επιλογή ' + x))]
                # Exact text first: options can differ only by an accent (Μαντίλι / Μαντιλί) or be symbols (@).
                correct = text(r.get('Σωστή απάντηση'))
                hits = [i for i, o in enumerate(options) if o == correct]
                hits = hits or [i for i, o in enumerate(options) if norm(o) == norm(correct)]
                if len(hits) != 1:
                    warnings.append(where + ': correct answer not found exactly once among options, skipped')
                    used.discard(entry['id'])
                    continue
                entry['options'] = options
                entry['correctIndex'] = hits[0]
                key = correct.lower()
            if not dd.add(prompt, key, where):
                used.discard(entry['id'])
                continue
            entry['explanation'] = text(r.get('Αιτιολόγηση'))
            out.append(entry)
    return out


# ---------- specials ----------

def build_specials(path, suggestions=None):
    excluded = collections.defaultdict(set)
    # Optional: a reviewed workbook may have dropped the list once nothing is left out of the game.
    if 'ΑΔΙΚΑ_ΔΥΣΚΟΛΕΣ_ΕΛΕΓΧΟΣ' in openpyxl.load_workbook(path, read_only=True).sheetnames:
        for r in sheet_rows(path, 'ΑΔΙΚΑ_ΔΥΣΚΟΛΕΣ_ΕΛΕΓΧΟΣ'):
            excluded[text(r.get('Κατηγορία'))].add(text(r.get('ID')))
    out, used, skipped = [], set(), collections.Counter()

    def explanation(v):
        t = text(v)
        return '' if REVIEW_NOTE.search(t) else t

    def rows_of(sheet, id_col):
        for r in sheet_rows(path, sheet):
            rid = text(r.get(id_col))
            if not rid:                     # section title rows ("ΝΕΕΣ ΕΡΩΤΗΣΕΙΣ — ...")
                continue
            if rid in excluded[sheet]:
                skipped[sheet + ' (ΕΚΤΟΣ ΠΑΙΧΝΙΔΙΟΥ)'] += 1
                continue
            yield rid, r

    dd = Dedupe('top5')
    for rid, r in rows_of('Top 5', 'ID'):
        if 'Μετακινήθηκε στο τέλος' in text(r.get('Νέα απόφαση')):
            skipped['Top 5 (moved out of the pool)'] += 1
            continue
        prompt = text(r.get('Ερώτηση'))
        answers = [text(r.get('Απάντηση %d' % i)) for i in range(1, 6) if text(r.get('Απάντηση %d' % i))]
        if not prompt or len(answers) != 5:
            warnings.append('Top 5 %s: needs a prompt and 5 answers, skipped' % rid)
            continue
        if dd.add(prompt, tuple(sorted(map(norm, answers))), 'Top 5 ' + rid):
            out.append({'id': unique_id('top5_' + rid, used), 'type': 'top5', 'title': 'TOP 5', 'prompt': prompt,
                        'answers': answers, 'explanation': explanation(r.get('Επεξήγηση / Κριτήριο'))})

    dd = Dedupe('find3')
    for rid, r in rows_of('Βρες 3', 'ID'):
        prompt = text(r.get('Ερώτηση'))
        answers = [a.strip() for a in text(r.get('Όλες οι αποδεκτές απαντήσεις')).split('•') if a.strip()]
        if not prompt or len(answers) < 3:
            warnings.append('Βρες 3 %s: needs a prompt and at least 3 answers, skipped' % rid)
            continue
        if dd.add(prompt, tuple(sorted(map(norm, answers))), 'Βρες 3 ' + rid):
            out.append({'id': unique_id('find3_' + rid, used), 'type': 'find3', 'title': 'ΒΡΕΣ 3', 'prompt': prompt,
                        'answers': answers, 'explanation': explanation(r.get('Επεξήγηση / σημείωση'))})

    dd = Dedupe('rank')
    for rid, r in rows_of('Βάλτα σε σειρά', 'ID'):
        prompt = text(r.get('Ερώτηση'))
        items = [text(r.get('Στοιχείο %d' % i)) for i in range(1, 6) if text(r.get('Στοιχείο %d' % i))]
        order = [x.strip() for x in text(r.get('Σωστή σειρά')).split('→') if x.strip()]
        if not prompt or len(order) < 2 or sorted(map(norm, order)) != sorted(map(norm, items)):
            warnings.append('Βάλτα σε σειρά %s: correct order must list exactly the items, skipped' % rid)
            continue
        if dd.add(prompt + ' ' + ' '.join(order), tuple(map(norm, order)), 'Βάλτα σε σειρά ' + rid):
            # The app shuffles items for play and checks positions against correctOrder.
            out.append({'id': unique_id('rank_' + rid, used), 'type': 'rank', 'title': 'ΒΑΛ’ ΤΑ ΣΕ ΣΕΙΡΑ',
                        'prompt': prompt, 'items': list(order), 'correctOrder': list(order),
                        'explanation': explanation(r.get('Τεκμηρίωση'))})

    dd = Dedupe('connection')
    for rid, r in rows_of('Βρες τη σύνδεση', 'ID'):
        items = [x.strip() for x in text(r.get('Στοιχεία')).split('•') if x.strip()]
        answer = text(r.get('Απάντηση'))
        if len(items) < 2 or not answer:
            warnings.append('Βρες τη σύνδεση %s: needs items and an answer, skipped' % rid)
            continue
        if dd.add(' '.join(items), tuple(sorted(map(norm, items))), 'Βρες τη σύνδεση ' + rid):
            out.append({'id': unique_id('connection_' + rid, used), 'type': 'connection', 'title': 'ΒΡΕΣ ΤΗ ΣΥΝΔΕΣΗ',
                        'prompt': CONNECTION_PROMPT + ' · '.join(items), 'answers': [answer], 'explanation': ''})

    for sheet, kind, title, prompt in (('Person ID', 'person_id', 'PERSON ID', 'Ποιο πρόσωπο ψάχνουμε;'),
                                       ('Country ID', 'country_id', 'COUNTRY ID', 'Ποια χώρα ψάχνουμε;')):
        seen = set()
        for rid, r in rows_of(sheet, 'Α/Α'):
            answer = text(r.get('Απάντηση'))
            # Fact 1 is the hardest: the first clue is worth the most points.
            hints = [text(r.get(c)) for c in ('Fact 1 — δύσκολο', 'Fact 2 — μεσαίο', 'Fact 3 — εύκολο')]
            if not answer or not all(hints):
                warnings.append('%s %s: needs an answer and 3 clues, skipped' % (sheet, rid))
                continue
            key = (norm(answer),) + tuple(map(norm, hints))
            if key in seen:
                continue
            seen.add(key)
            out.append({'id': unique_id('%s_%s' % (kind, rid), used), 'type': kind, 'title': title,
                        'prompt': prompt, 'hints': hints, 'answers': [answer], 'explanation': ''})
    if suggestions:
        add_special_suggestions(out, used, suggestions)
    return out, skipped


# ---------- checks + output ----------

def check(classic, duel, sudden, specials):
    problems = []
    if len(classic) < 36:
        problems.append('Classic needs at least 36 questions')
    buckets = {(q['category'], q['multiplier']) for q in duel}
    for cat in CLASSIC_SHEETS.values():
        for diff in (1, 2, 3):
            if (cat, diff) not in buckets:
                problems.append('Σπέσιαλ board has no %s question of difficulty %d' % (cat, diff))
    for kind in SUDDEN_SHEETS.values():
        if not any(q['type'] == kind for q in sudden):
            problems.append('Sudden Death has no "%s" questions' % kind)
    for kind in SPECIAL_TYPES:
        if sum(1 for q in specials if q['type'] == kind) < 2:
            problems.append('need at least 2 "%s" specials' % kind)
    for name, items in (('classic', classic), ('sudden', sudden), ('specials', specials)):
        ids = collections.Counter(q['id'] for q in items)
        if any(c > 1 for c in ids.values()):
            problems.append(name + ' has repeated ids')
    return problems


def load_asset(name):
    with open(os.path.join(ASSETS, name), encoding='utf-8') as f:
        return json.load(f)


def suggested(q):
    return q['id'].startswith('suggestion_')


def kept_classic(suggestions):
    """Classic questions kept from the app's file. With a suggestions sheet, the suggested ones are
    replaced by the sheet's approved rows (so edited rows change and rows no longer approved go away)."""
    classic = load_asset('classic_questions.json')
    if suggestions is None:
        return classic, duel_of(classic), []
    classic = [q for q in classic if not suggested(q)]
    dd = Dedupe('classic')
    for q in classic:
        dd.add(q['question'], norm(q['answer']), q['id'])
    add_classic_suggestions(classic, {q['id'] for q in classic}, dd, suggestions)
    return classic, duel_of(classic), dd.dropped


def kept_specials(suggestions):
    """Specials kept from the app's file, with the suggested ones refreshed as in kept_classic."""
    specials = load_asset('duel_specials.json')
    if suggestions is None:
        return specials
    specials = [q for q in specials if not suggested(q)]
    add_special_suggestions(specials, {q['id'] for q in specials}, suggestions)
    return specials


def with_kept_suggestions(built, name):
    """A file rebuilt from its workbook without a suggestions sheet keeps the suggestions already in it."""
    return built + [q for q in load_asset(name) if suggested(q)]


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument('--classic')
    ap.add_argument('--sudden')
    ap.add_argument('--specials')
    ap.add_argument('--suggestions')
    ap.add_argument('--update', action='store_true',
                    help='only refresh the suggested questions in the existing files (needs --suggestions)')
    args = ap.parse_args()
    workbooks = args.classic or args.sudden or args.specials
    if args.update and (workbooks or not args.suggestions):
        ap.error('--update takes only --suggestions')
    if not workbooks and not args.suggestions:
        ap.error('give at least one of --classic, --sudden, --specials (or --update --suggestions)')

    # Each mode comes from its workbook when one is given; otherwise the app's current file is kept.
    suggestions = suggestion_rows(args.suggestions) if args.suggestions else None
    if args.classic:
        classic, duel, classic_dropped = build_classic(args.classic, suggestions)
        if suggestions is None:
            classic = with_kept_suggestions(classic, 'classic_questions.json')
            duel = duel_of(classic)
    else:
        classic, duel, classic_dropped = kept_classic(suggestions)
    sudden = build_sudden(args.sudden) if args.sudden else load_asset('sudden_death.json')
    if args.specials:
        specials, skipped = build_specials(args.specials, suggestions)
        if suggestions is None:
            specials = with_kept_suggestions(specials, 'duel_specials.json')
    else:
        specials, skipped = kept_specials(suggestions), {}
    problems = check(classic, duel, sudden, specials)
    if problems:
        sys.exit('ERROR:\n  ' + '\n  '.join(problems))

    for name, data in (('classic_questions.json', classic), ('questions.json', duel),
                       ('sudden_death.json', sudden), ('duel_specials.json', specials)):
        with open(os.path.join(ASSETS, name), 'w', encoding='utf-8', newline='\n') as f:
            json.dump(data, f, ensure_ascii=False, indent=2)
        print('%-24s %5d' % (name, len(data)))
    print('classic by category:', dict(collections.Counter(q['category'] for q in classic)))
    print('sudden by type:     ', dict(collections.Counter(q['type'] for q in sudden)))
    print('specials by type:   ', dict(collections.Counter(q['type'] for q in specials)))
    print('skipped specials:   ', dict(skipped))
    print('repeated classic questions dropped: %d' % len(classic_dropped))
    for kept_as, first in classic_dropped:
        print('  - %s  (same as %s)' % (kept_as, first))
    print('warnings: %d' % len(warnings))
    for w in warnings:
        print('  -', w)


if __name__ == '__main__':
    main()
