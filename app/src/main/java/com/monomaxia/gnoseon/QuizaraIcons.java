package com.monomaxia.gnoseon;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ReplacementSpan;
import android.widget.TextView;

/**
 * Line icons for the Special and Sudden Death modes, drawn on a canvas instead of emoji so they
 * look the same on every device. Each icon travels inside text as one private-use character
 * (FIRST + index) and is painted by IconSpan.
 */
public final class QuizaraIcons {
    private static final char FIRST = '';

    // Same order as LABELS: index i is drawn by case i in draw().
    private static final String[] IDS = {"top5", "find3", "person_id", "country_id", "rank", "connection",
            "tf", "multiple", "over_under", "ab", "closest"};
    private static final String[] LABELS = {"TOP 5", "ΒΡΕΣ 3", "PERSON ID", "COUNTRY ID", "ΒΑΛ’ ΤΑ ΣΕ ΣΕΙΡΑ",
            "ΒΡΕΣ ΤΗ ΣΥΝΔΕΣΗ", "ΣΩΣΤΟ / ΛΑΘΟΣ", "ΠΟΛΛΑΠΛΗΣ ΕΠΙΛΟΓΗΣ", "OVER / UNDER", "ΔΙΑΛΕΞΕ ΤΟ ΣΩΣΤΟ",
            "ΠΙΟ ΚΟΝΤΑ ΚΕΡΔΙΖΕΙ"};

    private QuizaraIcons() { }

    /** The icon character for a Special or Sudden Death mode id, or "" if there is none. */
    public static String symbol(String id) {
        for (int i = 0; i < IDS.length; i++) {
            if (IDS[i].equals(id)) return String.valueOf((char) (FIRST + i));
        }
        return "";
    }

    private static boolean isIcon(char c) {
        return c >= FIRST && c < FIRST + IDS.length;
    }

    /**
     * Sets text, drawing any icon characters. Text without icons that starts with a known mode label
     * (optionally after "✓  ", "✕  " or "Γύρος N · ") gets that mode's icon in front of the label.
     */
    public static void setText(TextView view, CharSequence value) {
        if (value == null) {
            view.setText(null);
            return;
        }
        String s = value.toString();
        boolean hasIcon = false;
        for (int i = 0; i < s.length() && !hasIcon; i++) hasIcon = isIcon(s.charAt(i));
        if (!hasIcon) {
            for (int i = 0; i < LABELS.length; i++) {
                int at = s.indexOf(LABELS[i]);
                if (at < 0) continue;
                String before = s.substring(0, at);
                String after = s.substring(at + LABELS[i].length());
                boolean prefixOk = before.isEmpty() || before.equals("✓  ") || before.equals("✕  ")
                        || (before.startsWith("Γύρος ") && before.endsWith(" · "));
                boolean suffixOk = after.isEmpty() || after.startsWith("  ·") || after.startsWith(" ·");
                if (prefixOk && suffixOk) {
                    s = before + (char) (FIRST + i) + "  " + LABELS[i] + after;
                    break;
                }
            }
            // The closest-number round pill uses a shorter label than LABELS.
            if (s.startsWith("Γύρος ") && s.endsWith(" · ΠΙΟ ΚΟΝΤΑ")) {
                s = s.substring(0, s.length() - "ΠΙΟ ΚΟΝΤΑ".length()) + symbol("closest") + "  ΠΙΟ ΚΟΝΤΑ";
            }
        }
        SpannableString spannable = new SpannableString(s);
        for (int i = 0; i < s.length(); i++) {
            if (isIcon(s.charAt(i))) {
                spannable.setSpan(new IconSpan(s.charAt(i) - FIRST), i, i + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
        view.setText(spannable);
    }

    /** Canvas.drawText that draws a lone icon character as its icon (used by the roulette wheel). */
    public static void drawText(Canvas canvas, String text, float x, float y, Paint paint) {
        if (text.length() == 1 && isIcon(text.charAt(0))) {
            float size = paint.getTextSize() * 1.65f;
            float left = x;
            if (paint.getTextAlign() == Paint.Align.CENTER) left = x - size / 2f;
            else if (paint.getTextAlign() == Paint.Align.RIGHT) left = x - size;
            draw(canvas, text.charAt(0) - FIRST, left, y + paint.ascent(), size, paint.getColor());
            return;
        }
        canvas.drawText(text, x, y, paint);
    }

    /** Replaces one icon character in a TextView with the drawn icon, in the text colour. */
    public static final class IconSpan extends ReplacementSpan {
        final int id;

        public IconSpan(int id) {
            this.id = id;
        }

        @Override
        public int getSize(Paint paint, CharSequence text, int start, int end, Paint.FontMetricsInt fm) {
            if (fm != null) paint.getFontMetricsInt(fm);
            return (int) Math.ceil(paint.getTextSize() * 1.12f);
        }

        @Override
        public void draw(Canvas canvas, CharSequence text, int start, int end, float x, int top, int y,
                         int bottom, Paint paint) {
            float size = paint.getTextSize() * 1.12f;
            QuizaraIcons.draw(canvas, id, x, y + (paint.ascent() + paint.descent() - size) / 2f, size, paint.getColor());
        }
    }

    private static void lines(Canvas canvas, Paint paint, float... xy) {
        for (int i = 0; i < xy.length; i += 4) canvas.drawLine(xy[i], xy[i + 1], xy[i + 2], xy[i + 3], paint);
    }

    private static void polyline(Canvas canvas, Paint paint, float... xy) {
        Path path = new Path();
        path.moveTo(xy[0], xy[1]);
        for (int i = 2; i < xy.length; i += 2) path.lineTo(xy[i], xy[i + 1]);
        canvas.drawPath(path, paint);
    }

    /** Draws icon {@code id} in a size x size box whose top-left corner is (left, top); designed on a 24-unit grid. */
    public static void draw(Canvas canvas, int id, float left, float top, float size, int color) {
        int save = canvas.save();
        canvas.translate(left, top);
        float scale = size / 24f;
        canvas.scale(scale, scale);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(color);
        paint.setStrokeWidth(1.9f);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStyle(Paint.Style.STROKE);
        switch (id) {
            case 0: // TOP 5
            case 1: // ΒΡΕΣ 3: the number in a circle
                paint.setStrokeWidth(1.6f);
                canvas.drawCircle(12f, 12f, 10.8f, paint);
                paint.setStyle(Paint.Style.FILL);
                paint.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
                paint.setTextAlign(Paint.Align.CENTER);
                paint.setTextSize(19f);
                canvas.drawText(id == 0 ? "5" : "3", 12f, 12f - (paint.ascent() + paint.descent()) / 2f, paint);
                break;
            case 2: // PERSON ID: head and shoulders
                paint.setStyle(Paint.Style.FILL);
                canvas.drawCircle(12f, 7f, 4f, paint);
                canvas.drawRoundRect(new RectF(4f, 13f, 20f, 23f), 7f, 7f, paint);
                break;
            case 3: { // COUNTRY ID: flag
                lines(canvas, paint, 4f, 3f, 4f, 22f);
                Path flag = new Path();
                flag.moveTo(4f, 4f);
                flag.cubicTo(10f, 0f, 14f, 8f, 21f, 4f);
                flag.lineTo(21f, 14f);
                flag.cubicTo(14f, 18f, 10f, 10f, 4f, 14f);
                flag.close();
                canvas.drawPath(flag, paint);
                break;
            }
            case 4: // ΒΑΛ’ ΤΑ ΣΕ ΣΕΙΡΑ
            case 8: // OVER / UNDER: up and down arrows
                polyline(canvas, paint, 3f, 7f, 7f, 3f, 11f, 7f);
                lines(canvas, paint, 7f, 3f, 7f, 21f, 17f, 3f, 17f, 21f);
                polyline(canvas, paint, 13f, 17f, 17f, 21f, 21f, 17f);
                break;
            case 5: { // ΒΡΕΣ ΤΗ ΣΥΝΔΕΣΗ: puzzle piece
                Path piece = new Path();
                piece.moveTo(3f, 9f);
                piece.lineTo(9f, 9f);
                piece.lineTo(9f, 6f);
                piece.cubicTo(4f, 3f, 13f, -1f, 15f, 4f);
                piece.lineTo(15f, 9f);
                piece.lineTo(21f, 9f);
                piece.lineTo(21f, 14f);
                piece.cubicTo(14f, 10f, 14f, 22f, 21f, 18f);
                piece.lineTo(21f, 22f);
                piece.lineTo(3f, 22f);
                piece.close();
                canvas.drawPath(piece, paint);
                break;
            }
            case 6: // ΣΩΣΤΟ / ΛΑΘΟΣ: tick and cross
                polyline(canvas, paint, 2f, 8f, 5f, 11f, 10f, 4f);
                lines(canvas, paint, 15f, 14f, 22f, 21f, 22f, 14f, 15f, 21f);
                break;
            case 7: // ΠΟΛΛΑΠΛΗΣ ΕΠΙΛΟΓΗΣ: ticked box
                canvas.drawRoundRect(new RectF(3f, 3f, 21f, 21f), 2f, 2f, paint);
                polyline(canvas, paint, 7f, 12f, 10f, 15f, 17f, 8f);
                break;
            case 9: // ΔΙΑΛΕΞΕ ΤΟ ΣΩΣΤΟ: double tick
                polyline(canvas, paint, 2f, 12f, 6f, 16f, 13f, 7f);
                polyline(canvas, paint, 11f, 16f, 15f, 20f, 23f, 10f);
                break;
            case 10: // ΠΙΟ ΚΟΝΤΑ: target
                canvas.drawCircle(12f, 12f, 10f, paint);
                canvas.drawCircle(12f, 12f, 5f, paint);
                paint.setStyle(Paint.Style.FILL);
                canvas.drawCircle(12f, 12f, 1.7f, paint);
                break;
            default:
                break;
        }
        canvas.restoreToCount(save);
    }
}
