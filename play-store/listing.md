# Google Play: store listing and forms

Everything to paste or choose in Play Console for Quizara. Default store language: **Ελληνικά (el-GR)**.

## Store listing (Κύρια καταχώριση)

**App name** (max 30):

```
Quizara – Κουίζ για παρέες
```

**Short description** (max 80):

```
Παιχνίδι γνώσεων για δύο ομάδες σε ένα κινητό. Χωρίς internet και διαφημίσεις.
```

**Full description** (max 4000):

```
Το Quizara είναι ένα παιχνίδι γνώσεων για παρέες. Δύο ομάδες παίζουν σε ένα κινητό, συνήθως με έναν παρουσιαστή που διαβάζει τις ερωτήσεις δυνατά. Ιδανικό για βραδιές με φίλους, οικογενειακές συγκεντρώσεις και ταξίδια.

ΠΑΝΩ ΑΠΟ 9.000 ΕΡΩΤΗΣΕΙΣ ΣΤΑ ΕΛΛΗΝΙΚΑ
Έξι κατηγορίες: Αθλητικά, Τέχνη, Επιστήμη, Ιστορία, Γενικές γνώσεις και Γεωγραφία, σε τρεις δυσκολίες.

ΤΕΣΣΕΡΙΣ ΤΡΟΠΟΙ ΠΑΙΧΝΙΔΙΟΥ
• Σπέσιαλ: ταμπλό με 18 ερωτήσεις, από μία ανά κατηγορία για ×1, ×2 και ×3 πόντους, και προαιρετικοί ειδικοί γύροι: TOP 5, Βρες 3, Person ID, Country ID, Βάλ' τα σε σειρά και Βρες τη σύνδεση. Κάθε ομάδα μπορεί μία φορά να διπλασιάσει τους πόντους μιας ερώτησης.
• Κλασικό: δύο σωστές απαντήσεις σε κάθε κατηγορία για τη νίκη. Κάθε ομάδα έχει μία Κλοπή, ένα Ανακάτεμα και μία Αλλαγή ερώτησης.
• 4 Τυριά: κέρδισε το «τυρί» κάθε κατηγορίας με δύο σωστές απαντήσεις. Πρώτη στα τέσσερα κερδίζει. Με το Σαμποτάζ πισωγυρίζεις τον αντίπαλο.
• Αιφνίδιος Θάνατος: σε ισοπαλία, μια ρουλέτα από γρήγορες δοκιμασίες (Σωστό/Λάθος, πολλαπλής επιλογής, Πάνω/Κάτω, Διάλεξε, Πιο κοντά) βγάζει τον νικητή.

ΕΠΙΣΗΣ
• Ονόματα και χρώματα ομάδων, προαιρετικό χρονόμετρο (30, 45 ή 60 δευτερόλεπτα).
• Φωτεινό και σκοτεινό θέμα.
• Το παιχνίδι αποθηκεύεται μόνο του και συνεχίζεις από εκεί που σταμάτησες.
• Πρότεινε τις δικές σου ερωτήσεις: οι καλύτερες μπαίνουν στο παιχνίδι.

ΧΩΡΙΣ INTERNET, ΧΩΡΙΣ ΔΙΑΦΗΜΙΣΕΙΣ
Το Quizara δεν χρειάζεται σύνδεση, λογαριασμό ή άδειες στο κινητό, και δεν έχει διαφημίσεις.
```

**App category**: Παιχνίδια → **Trivia** (Κουίζ). Tags: Trivia, Party, Board-style.

**Contact details**: email `geconstantos@gmail.com` (shown publicly), website optional.

**Privacy policy URL**:

```
https://gkonstantos.github.io/QUIZARA-APP/privacy/
```

**Graphics** (files in this folder):

| Asset | Size | File |
|---|---|---|
| App icon | 512 × 512 PNG | `icon-512.png` |
| Feature graphic | 1024 × 500 PNG/JPG | `feature-graphic.png` |
| Phone screenshots | at least 2, 9:16 portrait, 1080 × 1920 or similar | take them on a phone (home screen, a question, a Special round, the winner screen) |

## App content (Περιεχόμενο εφαρμογής)

| Form | Answer |
|---|---|
| Privacy policy | the URL above |
| Ads | **No, my app does not contain ads** |
| App access | **All functionality is available without special access** (no login) |
| Content rating | Category **Game** → trivia/quiz. Answer **No** to violence, sexual content, profanity, drugs, gambling, user interaction/sharing, location sharing, digital purchases. Expected rating: PEGI 3 / Everyone. |
| Target audience | **13–15, 16–17, 18 and over**. Not 12 or under. "Could the app appeal to children?" → No (text-only trivia for groups, adult topics). |
| News app | No |
| COVID-19 apps | No |
| Data safety | see below |
| Government app | No |
| Financial features | None |
| Health | None |

### Data safety

The app itself sends nothing: it has no internet permission. The suggestion button opens a
pre-filled Google Form in the browser and the player submits it there. To be safe, declare that form:

- Does your app collect or share any of the required user data types? **Yes**
- Is all user data encrypted in transit? **Yes** (Google Forms uses HTTPS)
- Do you provide a way for users to request that their data be deleted? **Yes** (by email, as the
  privacy policy says)
- Data types:
  - **Personal info → Name**: collected, not shared, **optional**, purpose **App functionality**
    (the optional name or nickname on a suggestion).
  - **App activity → Other user-generated content**: collected, not shared, **optional**, purpose
    **App functionality** (the suggested question and answers).
- Processed ephemerally? **No**. Collection required? **No, users can choose**.

## Testing before going public

A new personal developer account must run a **closed test with at least 12 testers for 14 days in
a row** before it can apply for production. Add 15–20 testers to be safe (their Google account
emails, in an email list on the closed testing track), send them the opt-in link, and ask them to
keep the app installed for the whole 14 days.
