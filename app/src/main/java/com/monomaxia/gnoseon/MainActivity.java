package com.monomaxia.gnoseon;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.content.ClipData;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.ToneGenerator;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.DragEvent;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.text.InputType;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {

    private static final String PREFS = "quizara_prefs";
    private static final String THEME_LIGHT = "light";
    private static final String THEME_DARK = "dark";

    private int BG;
    private int CARD;
    private int TEXT;
    private int MUTED;
    private int BORDER;
    private int BLUE;
    private int BLUE_SOFT;
    private int RED;
    private int RED_SOFT;
    private int GREEN;
    private int GREEN_SOFT;
    private int ORANGE;
    private int ORANGE_SOFT;
    private int YELLOW;
    private int YELLOW_SOFT;
    private int PURPLE;
    private int PURPLE_SOFT;
    private int CYAN;
    private int CYAN_SOFT;
    private int TEAL;
    private int TEAL_SOFT;
    private int MAGENTA;
    private int MAGENTA_SOFT;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private SharedPreferences prefs;
    private String theme = THEME_LIGHT;

    private boolean revealEnabled = true;
    private boolean earlyFinishEnabled = true;
    private boolean finalQuestionEnabled = true;
    private boolean soundEnabled = true;
    private boolean hapticsEnabled = true;
    private boolean effectsEnabled = true;
    // Can be disabled when both players can see the answer on the same device.
    private boolean classicStealEnabled = true;

    // Shared question timer for Duel and Classic. 0 = Off.
    private int questionTimerSeconds = 0;
    private CountDownTimer questionTimer;
    private long questionTimerRemainingMs = 0L;
    private boolean questionTimerExpired = false;
    private TextView questionTimerView;

    // Single source of truth for category symbols across every mode and screen.
    private static final Category[] CATEGORIES = new Category[]{
            new Category("sports", "⚽", "Αθλητικά"),
            new Category("art", "🎨", "Τέχνες"),
            new Category("science", "⚛", "Επιστήμη"),
            new Category("history", "🏛", "Ιστορία"),
            new Category("general", "🧠", "Γενικές"),
            new Category("geography", "🌐", "Γεωγραφία")
    };

    private final List<Question> questions = new ArrayList<>();
    private final List<Question> questionBank = new ArrayList<>();
    private final List<Integer> duelSelection = new ArrayList<>();
    private final Map<String, Question> questionMap = new HashMap<>();
    private final Set<String> used = new HashSet<>();
    // Sounds are prepared off the UI thread: creating a ToneGenerator/MediaPlayer can block for seconds.
    private final ExecutorService audioExecutor = Executors.newSingleThreadExecutor();
    private final List<SuddenQuestion> suddenQuestions = new ArrayList<>();

    // Duel special questions. Every enabled type contributes two selectable plays to the shared board.
    private final List<SpecialQuestion> specialQuestions = new ArrayList<>();
    private final Map<String, SpecialQuestion> specialQuestionById = new HashMap<>();
    private final Set<String> enabledSpecialTypes = new HashSet<>();
    private final Map<String, Integer> specialPlaysUsed = new HashMap<>();
    private final Set<String> usedSpecialQuestionIds = new HashSet<>();
    private String activeSpecialId = "";
    private int activeSpecialCorrectCount = 0;
    private int activeSpecialMistakes = 0;
    private int activeSpecialHintIndex = 0;
    private boolean activeSpecialJokerSelected = false;
    private boolean activeSpecialInteractionStarted = false;
    private boolean activeSpecialAnswersVisible = false;
    private final List<String> activeRankOrder = new ArrayList<>();

    private static final String DEFAULT_TEAM_A = "Ομάδα 1";
    private static final String DEFAULT_TEAM_B = "Ομάδα 2";
    private String teamA = DEFAULT_TEAM_A;
    private String teamB = DEFAULT_TEAM_B;
    // Six primary team colors: red, orange, yellow, green, blue and purple.
    private int teamColorA = 4;
    private int teamColorB = 1;
    private final LinearLayout[] liveSwatches = new LinearLayout[2];
    private final LinearLayout[] liveTeamCards = new LinearLayout[2];
    private final TextView[] liveTeamTitles = new TextView[2];
    private int scoreA = 0;
    private int scoreB = 0;
    private int currentTeam = 0;
    private int answered = 0;

    // One red x2 joker for EACH team, once per game.
    private boolean jokerAvailableA = true;
    private boolean jokerAvailableB = true;

    private int correctA = 0;
    private int correctB = 0;
    private int wrongA = 0;
    private int wrongB = 0;
    private int maxHitA = 0;
    private int maxHitB = 0;
    private final Map<String, Integer> categoryCorrectA = new HashMap<>();
    private final Map<String, Integer> categoryCorrectB = new HashMap<>();

    private boolean finalIntroShown = false;

    // Persistent match state. The app saves after every meaningful action.
    private boolean gameActive = false;
    private String activeQuestionKey = "";
    private boolean activeQuestionRevealed = false;
    private boolean activeJokerSelected = false;
    private boolean inSuddenDeath = false;
    private boolean suddenStarted = false;

    private int suddenIndex = 0;
    private int suddenRound = 1;
    private int suddenTeam = 0;
    private Boolean suddenResultA = null;
    private Boolean suddenResultB = null;
    private boolean suddenQuestionRevealed = false;
    private String suddenMode = "";
    private String activeSuddenQuestionId = "";
    private String suddenClosestInputA = "";
    private String suddenClosestInputB = "";
    private boolean suddenClosestResolved = false;
    private double suddenClosestValueA = 0d;
    private double suddenClosestValueB = 0d;
    private final Set<String> suddenUsedQuestionIds = new HashSet<>();

    // Game modes. v0.8 activates Classic while keeping Duel unchanged.
    private enum GameMode { DUEL, CLASSIC, CHEESE }
    private GameMode gameMode = GameMode.DUEL;

    // Tie behavior is chosen only when the match actually ends tied.
    private enum TieRule { SUDDEN_DEATH, DRAW }
    private TieRule tieRule = TieRule.SUDDEN_DEATH;
    private boolean tieChoicePending = false;

    // Classic mode state.
    private final List<ClassicQuestion> classicQuestions = new ArrayList<>();
    private final Map<String, ClassicQuestion> classicQuestionById = new HashMap<>();
    private final Set<String> classicUsedQuestionIds = new HashSet<>();
    private final Map<String, Integer> classicProgressA = new HashMap<>();
    private final Map<String, Integer> classicProgressB = new HashMap<>();
    private final Set<Integer> classicDifficulties = new HashSet<>();
    private final List<Integer> classicDifficultySchedule = new ArrayList<>();
    private String classicOption1 = "";
    private String classicOption2 = "";
    private String activeClassicQuestionId = "";
    private boolean classicQuestionRevealed = false;
    private boolean classicAnswerEverRevealed = false;
    // 0 = normal question, 1 = steal offer, 2 = steal attempt.
    private int classicQuestionPhase = 0;
    private boolean stealAvailableA = true;
    private boolean stealAvailableB = true;
    private boolean shuffleAvailableA = true;
    private boolean shuffleAvailableB = true;
    private boolean changeQuestionAvailableA = true;
    private boolean changeQuestionAvailableB = true;
    private int classicQuestionsPlayedA = 0;
    private int classicQuestionsPlayedB = 0;
    private boolean classicFinalChance = false;
    private int classicFinisher = -1;
    // 1 = μικρή, 2 = μεσαία, 3 = μεγάλη διάρκεια: σωστές ανά κατηγορία.
    private int classicTargetPerCategory = 2;

    private enum ScreenSource { HOME, MODE, SETUP, BOARD, CLASSIC_SETUP, CLASSIC_BOARD }
    private ScreenSource settingsSource = ScreenSource.HOME;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
            loadPreferences();
            applyTheme(theme);
            loadQuestions();
            loadSavedGame();
            showHome();
        } catch (Throwable t) {
            showFatal(t);
        }
    }

    @Override
    protected void onPause() {
        pauseQuestionTimer();
        if (gameActive) saveGameState();
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        resumeQuestionTimerIfVisible();
    }

    private void loadPreferences() {
        theme = prefs.getString("theme", THEME_LIGHT);
        // Older Minimal themes are migrated to the corresponding supported theme.
        if ("minimal".equals(theme) || "minimal_light".equals(theme)) theme = THEME_LIGHT;
        if ("minimal_dark".equals(theme)) theme = THEME_DARK;
        // Mathematical end and Classic steal are permanent game rules.
        earlyFinishEnabled = true;
        classicStealEnabled = true;
        teamColorA = Math.max(0, Math.min(5, prefs.getInt("team_color_a", 4)));
        teamColorB = Math.max(0, Math.min(5, prefs.getInt("team_color_b", 1)));
        if (teamColorA == teamColorB) teamColorB = (teamColorA + 1) % 6;
        tieRule = "draw".equals(prefs.getString("tie_rule_default", "sudden_death")) ? TieRule.DRAW : TieRule.SUDDEN_DEATH;
        questionTimerSeconds = prefs.getInt("question_timer_seconds", 0);
        if (questionTimerSeconds != 0 && questionTimerSeconds != 30 && questionTimerSeconds != 45 && questionTimerSeconds != 60) {
            questionTimerSeconds = 0;
        }
        effectsEnabled = true;
        // One master Effects switch controls all sensory/game-show feedback.
        revealEnabled = effectsEnabled;
        finalQuestionEnabled = effectsEnabled;
        soundEnabled = effectsEnabled;
        hapticsEnabled = effectsEnabled;
    }

    private void applyTheme(String selected) {
        theme = selected;
        if (THEME_DARK.equals(selected)) {
            BG = Color.rgb(13, 13, 17);
            CARD = Color.rgb(31, 29, 36);
            TEXT = Color.rgb(244, 247, 249);
            MUTED = Color.rgb(155, 168, 179);
            BORDER = Color.rgb(73, 70, 79);
            BLUE = Color.rgb(155, 187, 230);
            BLUE_SOFT = Color.rgb(34, 40, 49);
            RED = Color.rgb(220, 160, 166);
            RED_SOFT = Color.rgb(47, 35, 40);
            GREEN = Color.rgb(155, 206, 172);
            GREEN_SOFT = Color.rgb(32, 43, 38);
            ORANGE = Color.rgb(235, 187, 148);
            ORANGE_SOFT = Color.rgb(47, 39, 34);
            YELLOW = Color.rgb(222, 199, 141);
            YELLOW_SOFT = Color.rgb(46, 43, 35);
            PURPLE = Color.rgb(197, 161, 226);
            PURPLE_SOFT = Color.rgb(42, 35, 48);
            CYAN = Color.rgb(151, 204, 216);
            CYAN_SOFT = Color.rgb(34, 44, 48);
            TEAL = Color.rgb(143, 207, 194);
            TEAL_SOFT = Color.rgb(33, 44, 43);
            MAGENTA = Color.rgb(225, 168, 200);
            MAGENTA_SOFT = Color.rgb(46, 36, 44);
        } else {
            BG = Color.rgb(255, 246, 232);
            CARD = Color.rgb(255, 249, 239);
            TEXT = Color.rgb(18, 18, 17);
            MUTED = Color.rgb(143, 145, 147);
            BORDER = Color.rgb(224, 221, 217);
            BLUE = Color.rgb(0, 49, 211);
            BLUE_SOFT = Color.rgb(238, 246, 253);
            RED = Color.rgb(235, 76, 0);
            RED_SOFT = Color.rgb(253, 242, 239);
            GREEN = Color.rgb(46, 156, 92);
            GREEN_SOFT = Color.rgb(237, 249, 242);
            ORANGE = Color.rgb(255, 81, 0);
            ORANGE_SOFT = Color.rgb(254, 247, 235);
            YELLOW = Color.rgb(223, 185, 62);
            YELLOW_SOFT = Color.rgb(255, 250, 236);
            PURPLE = Color.rgb(128, 80, 190);
            PURPLE_SOFT = Color.rgb(246, 240, 252);
            CYAN = Color.rgb(38, 167, 209);
            CYAN_SOFT = Color.rgb(239, 249, 252);
            TEAL = Color.rgb(21, 154, 146);
            TEAL_SOFT = Color.rgb(236, 248, 246);
            MAGENTA = Color.rgb(198, 78, 140);
            MAGENTA_SOFT = Color.rgb(251, 240, 246);
        }
        configureSystemBarsSafely();
    }

    private void configureSystemBarsSafely() {
        try {
            getWindow().setStatusBarColor(BG);
            getWindow().setNavigationBarColor(BG);
            int flags = 0;
            if (!isDarkTheme()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            }
            getWindow().getDecorView().setSystemUiVisibility(flags);
        } catch (Throwable ignored) {
            // Cosmetic only. Never block startup.
        }
    }

    private void loadQuestions() throws Exception {
        questionBank.clear();
        questions.clear();
        questionMap.clear();
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                getAssets().open("questions.json"), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
        }
        JSONArray arr = new JSONArray(sb.toString());
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.getJSONObject(i);
            JSONArray answersJson = o.getJSONArray("answers");
            String[] answers = new String[answersJson.length()];
            for (int j = 0; j < answersJson.length(); j++) answers[j] = answersJson.getString(j);
            Question q = new Question(
                    o.getString("category"),
                    o.getInt("multiplier"),
                    o.getString("question"),
                    answers,
                    o.getInt("correctIndex"),
                    o.optString("explanation", "")
            );
            questionBank.add(q);
        }
        selectDuelQuestions();
        loadDuelSpecialQuestions();
        loadSuddenQuestions();
        loadClassicQuestions();
    }

    private void selectDuelQuestions() {
        duelSelection.clear();
        for (Category c : CATEGORIES) for (int m=1;m<=3;m++) {
            List<Integer> pool=new ArrayList<>();
            for(int i=0;i<questionBank.size();i++) {
                Question q=questionBank.get(i);
                if(q.category.equals(c.id)&&q.multiplier==m)pool.add(i);
            }
            if(pool.isEmpty())throw new IllegalStateException("Δεν υπάρχουν ερωτήσεις: "+c.name);
            Collections.shuffle(pool);duelSelection.add(pool.get(0));
        }
        rebuildDuelMap();
    }

    private void restoreDuelQuestions(JSONArray saved)throws Exception {
        if(saved.length()!=18)throw new IllegalStateException("Invalid saved question selection");
        duelSelection.clear();for(int i=0;i<saved.length();i++)duelSelection.add(saved.getInt(i));
        rebuildDuelMap();
        if(questionMap.size()!=18)throw new IllegalStateException("Invalid saved question board");
    }

    private void rebuildDuelMap() {
        questions.clear();questionMap.clear();
        for(int i:duelSelection){Question q=questionBank.get(i);questions.add(q);questionMap.put(key(q.category,q.multiplier),q);}
    }

    private void loadDuelSpecialQuestions() throws Exception {
        specialQuestions.clear();
        specialQuestionById.clear();
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                getAssets().open("duel_specials.json"), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
        }
        JSONArray arr = new JSONArray(sb.toString());
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.getJSONObject(i);
            SpecialQuestion q = SpecialQuestion.fromJson(o);
            specialQuestions.add(q);
            specialQuestionById.put(q.id, q);
        }
        String[] required = new String[]{"top5", "find3", "person_id", "country_id", "rank", "connection"};
        for (String type : required) {
            int count = 0;
            for (SpecialQuestion q : specialQuestions) if (type.equals(q.type)) count++;
            if (count < 2) throw new IllegalStateException("Χρειάζονται τουλάχιστον 2 special ερωτήσεις τύπου " + type);
        }
    }

    private void loadSuddenQuestions() throws Exception {
        suddenQuestions.clear();
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                getAssets().open("sudden_death.json"), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
        }
        JSONArray arr = new JSONArray(sb.toString());
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.getJSONObject(i);
            JSONArray optionsJson = o.optJSONArray("options");
            String[] options = new String[optionsJson == null ? 0 : optionsJson.length()];
            if (optionsJson != null) for (int j = 0; j < optionsJson.length(); j++) options[j] = optionsJson.getString(j);
            suddenQuestions.add(new SuddenQuestion(
                    o.getString("id"),
                    o.getString("type"),
                    o.getString("prompt"),
                    options,
                    o.optInt("correctIndex", -1),
                    o.optDouble("numericAnswer", Double.NaN),
                    o.optString("unit", ""),
                    o.optString("explanation", "")
            ));
        }
        String[] required = new String[]{"tf", "multiple", "over_under", "ab", "closest"};
        for (String type : required) {
            int count = 0;
            for (SuddenQuestion q : suddenQuestions) if (type.equals(q.type)) count++;
            if (count < 2) throw new IllegalStateException("Χρειάζονται περισσότερες ερωτήσεις Sudden Death τύπου " + type);
        }
    }

    private void loadClassicQuestions() throws Exception {
        classicQuestions.clear();
        classicQuestionById.clear();
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                getAssets().open("classic_questions.json"), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
        }
        JSONArray arr = new JSONArray(sb.toString());
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.getJSONObject(i);
            ClassicQuestion q = new ClassicQuestion(
                    o.getString("id"),
                    o.getString("category"),
                    o.getInt("difficulty"),
                    o.getString("question"),
                    o.getString("answer"),
                    o.optString("explanation", "")
            );
            classicQuestions.add(q);
            classicQuestionById.put(q.id, q);
        }
        if (classicQuestions.size() < 36) {
            throw new IllegalStateException("Χρειάζονται περισσότερες ερωτήσεις για το Κλασικό mode.");
        }
    }

    private CheeseGame cheese = new CheeseGame();
    private String cheeseQuestionId = "";
    private boolean cheeseRevealed = false;
    private boolean cheeseAttackSelected = false;
    private final Set<String> cheeseUsed = new HashSet<>();
    private final List<Integer> cheeseDifficultyBag = new ArrayList<>();
    private final Set<Integer> cheeseDifficulties = new HashSet<>(java.util.Arrays.asList(1,2,3));
    private static final String[] CHEESE_IDS = {"history", "geography", "science", "art", "sports", "general"};

    private void showCheeseSetup() {
        applyTheme(prefs.getString("theme",THEME_LIGHT));
        LinearLayout root=baseColumn();
        root.addView(topBar(true,false));
        root.addView(sectionTitle("ΡΥΘΜΙΣΕΙΣ ΟΜΑΔΩΝ"),fullMargins(0,12,0,12));
        EditText a=teamInput(teamA), b=teamInput(teamB);
        root.addView(teamColorSettings(a,b),fullMargins(2,0,2,20));
        root.addView(sectionTitle("Δυσκολίες παρτίδας"),fullMargins(0,12,0,7));
        final boolean[] selected={true,true,true};
        LinearLayout difficultyList=new LinearLayout(this);
        difficultyList.setOrientation(LinearLayout.VERTICAL);
        for(int i=0;i<3;i++){
            final int idx=i, level=i+1;
            Button choice=button("",CARD,TEXT);
            choice.setMinHeight(dp(54));
            styleClassicDifficultyToggle(choice,level,true);
            choice.setOnClickListener(v->{
                int count=(selected[0]?1:0)+(selected[1]?1:0)+(selected[2]?1:0);
                if(selected[idx]&&count==1){Toast.makeText(this,"Τουλάχιστον μία δυσκολία",Toast.LENGTH_SHORT).show();return;}
                selected[idx]=!selected[idx];
                styleClassicDifficultyToggle(choice,level,selected[idx]);
            });
            difficultyList.addView(choice,fullMargins(3,3,3,3));
        }
        root.addView(difficultyList);
        Button start=modeStartButton(GameMode.CHEESE);
        start.setOnClickListener(v -> {
            teamA=cleanName(a.getText().toString(),DEFAULT_TEAM_A);
            teamB=cleanName(b.getText().toString(),DEFAULT_TEAM_B);
            cheese=new CheeseGame();cheeseUsed.clear();cheeseDifficultyBag.clear();
            cheeseDifficulties.clear();for(int i=0;i<3;i++)if(selected[i])cheeseDifficulties.add(i+1);
            cheeseQuestionId="";cheeseRevealed=false;cheeseAttackSelected=false;
            gameMode=GameMode.CHEESE;gameActive=true;inSuddenDeath=false;suddenStarted=false;
            activeQuestionKey="";activeClassicQuestionId="";activeSpecialId="";
            scoreA=scoreB=0;saveGameState();showCheeseBoard();
        });
        root.addView(start,fullMargins(4,28,4,8));
        setStaticScreen(root);
    }


    private void showCheeseBoard() {
        if(cheese.phase.equals("winner")){showCheeseWinner(cheese.winner);return;}
        if(cheese.phase.equals("sudden")){currentTeam=cheese.turn;showSuddenDeathIntro();return;}
        if(cheese.phase.equals("last")){showLastDance();return;}
        applyTheme(prefs.getString("theme",THEME_LIGHT));currentTeam=cheese.turn;
        LinearLayout root=baseColumn();
        root.addView(gameTopBar(false));
        root.addView(cheeseScores(),fullMargins(4,4,4,10));
        root.addView(turnBanner(cheese.turn==0?teamA:teamB,cheese.turn),centeredWrapMargins(0,0,0,8));
        addCheeseGrid(root,false);
        Button sabotage=button(cheeseAttackSelected?"✓  ΣΑΜΠΟΤΑΖ":"ΣΑΜΠΟΤΑΖ  ·  "+(cheese.sabotage[cheese.turn]?"1":"0"),CARD,TEXT);
        sabotage.setBackground(round(CARD,modeAccent(GameMode.CHEESE),2,radius()));
        sabotage.setEnabled(cheese.sabotage[cheese.turn]);
        sabotage.setAlpha(sabotage.isEnabled()?1f:0.4f);
        sabotage.setOnClickListener(v->{cheeseAttackSelected=!cheeseAttackSelected;showCheeseBoard();});
        root.addView(sabotage,fullMargins(4,10,4,5));
        saveGameState();setStaticScreen(root);
    }


    private View cheeseScores() { return cheeseScores(-1); }

    private View cheeseScores(int winningTeam) {
        LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);
        for(int t=0;t<2;t++) {
            final int team=t;
            LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);
            card.setGravity(Gravity.CENTER);card.setPadding(dp(5),dp(7),dp(5),dp(7));
            boolean active=cheese.phase.equals("board") && t==cheese.turn;
            card.setBackground(round(active?teamTint(t):CARD,teamAccent(t),active?3:1,radius()));
            card.setElevation(dp(active?5:1));
            TextView name=text((active?"▶  ":"")+(t==0?teamA:teamB),14,teamAccent(t),true);
            applyAdaptiveText(name,10,14,true);card.addView(name);
            int count=t==winningTeam?4:cheese.count(t);
            TextView score=text(count+" / 4",26,TEXT,true);score.setGravity(Gravity.CENTER);card.addView(score);
            TextView wedges=text("△  △  △  △",13,teamAccent(t),true);
            wedges.setGravity(Gravity.CENTER);card.addView(wedges);
            row.addView(card,weightedMargins(1,t==0?0:4,0,t==0?4:0,0));
        }
        return row;
    }

    /** Consistent line icons, drawn as vectors instead of device-dependent emoji glyphs. */
    private View categoryGlyph(String id, int accent, int sizeDp) {
        View glyph = new View(this) {
            final Paint pen = new Paint(Paint.ANTI_ALIAS_FLAG);
            void line(Canvas canvas, float x1,float y1,float x2,float y2) { canvas.drawLine(x1,y1,x2,y2,pen); }
            @Override protected void onDraw(Canvas canvas) {
                super.onDraw(canvas);
                float scale=Math.min(getWidth(),getHeight())/48f;
                canvas.save();canvas.translate((getWidth()-48*scale)/2f,(getHeight()-48*scale)/2f);canvas.scale(scale,scale);
                pen.setColor(accent);pen.setStrokeWidth(2.8f);pen.setStrokeCap(Paint.Cap.ROUND);
                pen.setStrokeJoin(Paint.Join.ROUND);pen.setStyle(Paint.Style.STROKE);
                if("history".equals(id)) {
                    line(canvas,8,12,40,12);line(canvas,12,9,36,9);line(canvas,8,38,40,38);
                    for(int x=13;x<=35;x+=11){line(canvas,x,16,x,34);line(canvas,x-2,16,x+2,16);line(canvas,x-2,34,x+2,34);}
                } else if("geography".equals(id)) {
                    canvas.drawCircle(24,24,17,pen);canvas.drawOval(new RectF(17,7,31,41),pen);
                    line(canvas,8,24,40,24);line(canvas,11,16,37,16);line(canvas,11,32,37,32);
                } else if("science".equals(id)) {
                    canvas.drawOval(new RectF(6,18,42,30),pen);
                    canvas.save();canvas.rotate(60,24,24);canvas.drawOval(new RectF(6,18,42,30),pen);canvas.restore();
                    canvas.save();canvas.rotate(-60,24,24);canvas.drawOval(new RectF(6,18,42,30),pen);canvas.restore();
                    pen.setStyle(Paint.Style.FILL);canvas.drawCircle(24,24,3.5f,pen);
                } else if("art".equals(id)) {
                    canvas.drawOval(new RectF(8,10,39,39),pen);
                    pen.setStyle(Paint.Style.FILL);
                    for(int[] c:new int[][]{{18,17},{29,15},{32,25},{17,30}})canvas.drawCircle(c[0],c[1],2.5f,pen);
                    pen.setStyle(Paint.Style.STROKE);canvas.drawArc(new RectF(23,27,39,40),-30,220,false,pen);
                } else if("sports".equals(id)) {
                    canvas.drawCircle(24,24,17,pen);canvas.drawCircle(24,24,7,pen);
                    line(canvas,24,7,24,17);line(canvas,24,31,24,41);line(canvas,7,24,17,24);line(canvas,31,24,41,24);
                } else {
                    line(canvas,24,5,24,43);line(canvas,5,24,43,24);
                    line(canvas,12,12,36,36);line(canvas,36,12,12,36);
                    pen.setStyle(Paint.Style.FILL);canvas.drawCircle(24,24,4,pen);
                }
                canvas.restore();
            }
        };
        glyph.setContentDescription(categoryById(id).name);
        glyph.setLayoutParams(new LinearLayout.LayoutParams(dp(sizeDp),dp(sizeDp)));
        return glyph;
    }

    private void addCheeseGrid(LinearLayout root,boolean last) {
        GridLayout grid=new GridLayout(this);grid.setColumnCount(2);
        for(int c=0;c<6;c++){
            final int category=c;Category cat=categoryById(CHEESE_IDS[c]);
            boolean attack=last||cheeseAttackSelected;
            int owner=cheese.owner(c);int accent=categoryAccent(cat);
            LinearLayout tile=new LinearLayout(this);tile.setOrientation(LinearLayout.VERTICAL);
            tile.setPadding(dp(10),dp(8),dp(10),dp(8));
            tile.setBackground(round(CARD,accent,2,radius()));
            LinearLayout heading = new LinearLayout(this);
            heading.setGravity(Gravity.CENTER_VERTICAL);
            heading.addView(categoryGlyph(cat.id,accent,30));
            TextView name=text(cat.name.toUpperCase(java.util.Locale.forLanguageTag("el")),15,accent,true);
            name.setPadding(dp(6),0,0,0);
            heading.addView(name, new LinearLayout.LayoutParams(0,-2,1));
            tile.addView(heading);
            TextView a=text(teamA+"   "+cheese.progress[0][c]+"/2"+(owner==0?"  ◭":""),12,teamAccent(0),true);
            TextView b=text(teamB+"   "+cheese.progress[1][c]+"/2"+(owner==1?"  ◭":""),12,teamAccent(1),true);
            tile.addView(a,fullMargins(0,10,0,4));tile.addView(b);
            boolean enabled=cheese.canPick(c,attack);tile.setAlpha(enabled?1f:0.45f);
            tile.setEnabled(enabled);tile.setFocusable(enabled);
            tile.setContentDescription(cat.name+", "+teamA+" "+cheese.progress[0][c]+" από 2, "+teamB+" "+cheese.progress[1][c]+" από 2"+(owner>=0?", κλειδωμένη":""));
            tile.setOnClickListener(v->startCheeseQuestion(category,attack));attachPressAnimation(tile);
            GridLayout.LayoutParams lp=new GridLayout.LayoutParams();lp.width=0;lp.height=dp(104);
            lp.columnSpec=GridLayout.spec(GridLayout.UNDEFINED,1f);lp.setMargins(dp(4),dp(4),dp(4),dp(4));
            grid.addView(tile,lp);
        }
        root.addView(grid,new LinearLayout.LayoutParams(-1,-2));
    }

    private void startCheeseQuestion(int category,boolean attack) {
        // Draw from a shuffled bag: each block of three questions has all difficulties.
        if(cheeseDifficultyBag.isEmpty()){
            cheeseDifficultyBag.addAll(cheeseDifficulties);
            Collections.shuffle(cheeseDifficultyBag);
        }
        int difficulty=cheeseDifficultyBag.get(0);
        List<ClassicQuestion> candidates=new ArrayList<>();
        for(ClassicQuestion q:classicQuestions)if(q.category.equals(CHEESE_IDS[category])&&q.difficulty==difficulty&&!cheeseUsed.contains(q.id))candidates.add(q);
        if(candidates.isEmpty())for(ClassicQuestion q:classicQuestions)if(q.category.equals(CHEESE_IDS[category])&&q.difficulty==difficulty)candidates.add(q);
        if(candidates.isEmpty()){Toast.makeText(this,"Δεν υπάρχουν διαθέσιμες ερωτήσεις",Toast.LENGTH_SHORT).show();return;}
        Collections.shuffle(candidates);ClassicQuestion q=candidates.get(0);
        cheese.begin(category,attack);cheeseDifficultyBag.remove(0);cheeseUsed.add(q.id);
        cheeseQuestionId=q.id;cheeseRevealed=false;cheeseAttackSelected=false;currentTeam=cheese.turn;
        saveGameState();
        Category shown = categoryById(q.category);
        showQuestionTransition(shown.name, shown.id, categoryAccent(shown), this::showCheeseQuestion);
    }

    private void showCheeseQuestion() {
        ClassicQuestion q=classicQuestionById.get(cheeseQuestionId);
        if(q==null){Toast.makeText(this,"Η ερώτηση δεν είναι διαθέσιμη",Toast.LENGTH_LONG).show();return;}
        applyTheme(prefs.getString("theme",THEME_LIGHT));
        LinearLayout root=baseColumn();root.setPadding(dp(18),dp(10),dp(18),dp(12));
        root.addView(gameTopBar(false));
        if (cheese.phase.equals("last") || cheese.attacking)
            root.addView(sectionTitle(cheese.phase.equals("last")?"LAST DANCE":"ΣΑΜΠΟΤΑΖ"),fullMargins(0,2,0,6));
        root.addView(cheeseScores(),fullMargins(0,6,0,8));
        Category category=categoryById(q.category);
        TextView title=pill(category.name.toUpperCase(java.util.Locale.forLanguageTag("el")),TEXT,CARD,softOutline(categoryAccent(category)));
        title.setGravity(Gravity.CENTER);root.addView(title,fullMargins(3,0,3,10));
        LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER);card.setPadding(dp(16),dp(18),dp(16),dp(18));
        card.setMinimumHeight(dp(280));card.setBackground(round(CARD,softOutline(categoryAccent(category)),1,radius()));
        card.setCameraDistance(dp(12000));
        final boolean[] revealed={cheeseRevealed};
        renderCheeseFace(card,q,revealed[0]);
        card.setOnClickListener(v->{
            revealed[0]=!revealed[0];cheeseRevealed=revealed[0];saveGameState();
            playFlipFeedback();
            if(!effectsEnabled){renderCheeseFace(card,q,revealed[0]);return;}
            boolean showAnswer=revealed[0];
            card.animate().rotationY(90f).setDuration(140).withEndAction(()->{
                renderCheeseFace(card,q,showAnswer);card.setRotationY(-90f);
                card.animate().rotationY(0f).setDuration(140).start();
            }).start();
        });
        root.addView(card,fullMargins(2,0,2,8));
        LinearLayout actions=new LinearLayout(this);actions.setGravity(Gravity.CENTER);
        Button yes=button("✓  ΣΩΣΤΟ",CARD,TEXT),no=button("✕  ΛΑΘΟΣ",CARD,TEXT);
        yes.setBackground(round(CARD,softOutline(GREEN),1,radius()));
        no.setBackground(round(CARD,softOutline(RED),1,radius()));
        yes.setOnClickListener(v->resolveCheese(true));no.setOnClickListener(v->resolveCheese(false));
        actions.addView(yes,weightedMargins(1,2,0,5,0));
        actions.addView(no,weightedMargins(1,5,0,2,0));
        root.addView(actions,fullMargins(0,0,0,4));
        setStaticScreen(root);
    }

    private void renderCheeseFace(LinearLayout card,ClassicQuestion q,boolean answer){
        card.removeAllViews();
        if(answer){
            TextView label=text("ΑΠΑΝΤΗΣΗ",11,MUTED,true);label.setGravity(Gravity.CENTER);
            card.addView(label,fullMargins(0,0,0,12));
            TextView result=text(q.answer,24,TEXT,true);result.setGravity(Gravity.CENTER);
            card.addView(result);
            if(!q.explanation.isEmpty()){
                TextView explanation=text(q.explanation,12,MUTED,false);
                explanation.setGravity(Gravity.CENTER);
                card.addView(explanation,fullMargins(0,12,0,0));
            }
        }else{
            TextView question=text(q.question,23,TEXT,true);question.setGravity(Gravity.CENTER);
            question.setLineSpacing(dp(4),1f);card.addView(question);
        }
    }

    private void resolveCheese(boolean correct){
        if(!cheese.questionOpen)return;
        cheese.answer(correct);cheeseQuestionId="";cheeseRevealed=false;currentTeam=cheese.turn;
        scoreA=cheese.count(0);scoreB=cheese.count(1);saveGameState();
        showCheeseBoard();
    }

    private void showLastDance(){
        applyTheme(prefs.getString("theme", THEME_LIGHT));currentTeam=cheese.turn;
        LinearLayout root=baseColumn();root.setPadding(dp(20),dp(20),dp(20),dp(28));
        root.addView(gameTopBar(false));
        root.addView(text("LAST DANCE",38,modeAccent(GameMode.CHEESE),true),fullMargins(0,8,0,12));
        root.addView(cheeseScores(),fullMargins(0,0,0,20));
        addCheeseGrid(root,true);
        Button decline=button("ΠΑΡΑΙΤΗΣΗ ΑΠΟ ΤΟ LAST DANCE",CARD,MUTED);
        decline.setOnClickListener(v->new AlertDialog.Builder(this).setMessage("Ο αντίπαλος θα κερδίσει την παρτίδα. Συνέχεια;").setNegativeButton("Πίσω",null).setPositiveButton("Ναι",(d,w)->{cheese.declineLastDance();showCheeseWinner(cheese.winner);}).show());
        root.addView(decline,fullMargins(0,18,0,0));
        setScrollable(root);tensionShake(root);
    }

    private void showCheeseWinner(int team){
        showUnifiedWinner(team);
    }

    private void tensionShake(View view){
        if(!effectsEnabled)return;
        android.animation.ObjectAnimator a=android.animation.ObjectAnimator.ofFloat(view,"translationX",0,dp(2),-dp(2),dp(1),-dp(1),0);
        a.setDuration(320);a.start();
    }

    private void saveCheese(SharedPreferences.Editor e)throws Exception{
        JSONObject o=new JSONObject();
        JSONArray a=new JSONArray(),b=new JSONArray(),bag=new JSONArray();
        for(int n:cheese.progress[0])a.put(n);for(int n:cheese.progress[1])b.put(n);
        for(int n:cheeseDifficultyBag)bag.put(n);
        o.put("a",a);o.put("b",b);o.put("turn",cheese.turn);o.put("sa",cheese.sabotage[0]);o.put("sb",cheese.sabotage[1]);
        o.put("phase",cheese.phase);o.put("winner",cheese.winner);o.put("leader",cheese.lastLeader);
        o.put("open",cheese.questionOpen);o.put("attack",cheese.attacking);o.put("cat",cheese.category);
        JSONArray allowed=new JSONArray();for(int d:cheeseDifficulties)allowed.put(d);
        o.put("q",cheeseQuestionId);o.put("revealed",cheeseRevealed);o.put("bag",bag);o.put("difficulties",allowed);
        e.putString("game_cheese",o.toString());e.putStringSet("game_cheese_used",new HashSet<>(cheeseUsed));
    }

    private void loadCheese()throws Exception{
        JSONObject o=new JSONObject(prefs.getString("game_cheese","{}"));cheese=new CheeseGame();
        for(int i=0;i<6;i++){cheese.progress[0][i]=o.getJSONArray("a").getInt(i);cheese.progress[1][i]=o.getJSONArray("b").getInt(i);}
        cheese.turn=o.getInt("turn");cheese.sabotage[0]=o.getBoolean("sa");cheese.sabotage[1]=o.getBoolean("sb");
        cheese.phase=o.getString("phase");cheese.winner=o.optInt("winner",-1);cheese.lastLeader=o.optInt("leader",-1);
        cheese.questionOpen=o.getBoolean("open");cheese.attacking=o.getBoolean("attack");cheese.category=o.getInt("cat");
        cheeseQuestionId=o.getString("q");cheeseRevealed=o.getBoolean("revealed");
        cheeseDifficultyBag.clear();JSONArray bag=o.getJSONArray("bag");for(int i=0;i<bag.length();i++)cheeseDifficultyBag.add(bag.getInt(i));
        cheeseDifficulties.clear();JSONArray allowed=o.optJSONArray("difficulties");if(allowed!=null)for(int i=0;i<allowed.length();i++)cheeseDifficulties.add(allowed.getInt(i));
        if(cheeseDifficulties.isEmpty()){cheeseDifficulties.add(1);cheeseDifficulties.add(2);cheeseDifficulties.add(3);}
        cheeseUsed.clear();cheeseUsed.addAll(prefs.getStringSet("game_cheese_used",new HashSet<>()));
    }

    private void resumeCheese(){
        if(inSuddenDeath){applyTheme(prefs.getString("theme", THEME_LIGHT));
            if(!suddenStarted)showSuddenDeathIntro();
            else if(suddenMode==null||suddenMode.isEmpty())showSuddenRoulette();
            else if("closest".equals(suddenMode))showClosestSudden();
            else if(suddenResultA!=null&&suddenResultB!=null)continueSuddenAfterIndividualAnswer(suddenTeam);
            else showSuddenQuestion();return;
        }
        if(cheese.questionOpen)showCheeseQuestion();else showCheeseBoard();
    }

    private void renderConnectionSpecial(LinearLayout root,SpecialQuestion q){
        LinearLayout card=cardColumn();card.setMinimumHeight(dp(160));
        card.setBackground(round(CARD,TEAL,2,radius()));card.setCameraDistance(dp(12000));
        Runnable render=()->{
            card.removeAllViews();
            if(activeSpecialAnswersVisible){
                TextView answerLabel=text("ΑΠΑΝΤΗΣΗ",12,MUTED,true);answerLabel.setGravity(Gravity.CENTER);
                card.addView(answerLabel,fullMargins(0,0,0,8));
                for(String answer:q.answers){
                    TextView value=text(answer,20,TEXT,true);value.setGravity(Gravity.CENTER);
                    card.addView(value,fullMargins(4,4,4,4));
                }
            }else{
                TextView instruction=text("ΒΡΕΣ ΤΗ ΣΥΝΔΕΣΗ",17,TEXT,true);
                instruction.setGravity(Gravity.CENTER);card.addView(instruction);
            }
        };
        render.run();
        card.setOnClickListener(v->{
            activeSpecialAnswersVisible=!activeSpecialAnswersVisible;saveGameState();playFlipFeedback();
            if(!effectsEnabled){render.run();return;}
            card.animate().rotationY(90).setDuration(140).withEndAction(()->{
                render.run();card.setRotationY(-90f);
                card.animate().rotationY(0f).setDuration(140).start();
            }).start();
        });
        root.addView(card,fullMargins(4,4,4,12));
        Button yes=primaryButton("✓  ΣΩΣΤΟ",GREEN),no=primaryButton("✕  ΛΑΘΟΣ",RED);
        yes.setOnClickListener(v->resolveSpecialQuestion(q,2,true));no.setOnClickListener(v->resolveSpecialQuestion(q,0,false));
        root.addView(yes,fullMargins(4,0,4,8));root.addView(no,fullMargins(4,0,4,8));
    }

    // ---------- HOME / SETUP ----------

    private void showHome() {
        applyTheme(prefs.getString("theme", THEME_LIGHT));
        settingsSource = ScreenSource.HOME;
        LinearLayout root = baseColumn();
        root.setPadding(dp(16), dp(8), dp(16), dp(10));

        ImageView hero = new ImageView(this);
        hero.setImageResource(isDarkTheme() ? R.drawable.poster_home_dark : R.drawable.poster_home_light);
        hero.setScaleType(ImageView.ScaleType.FIT_CENTER);
        hero.setContentDescription("QUIZARA — ο Σκεπτόμενος");
        root.addView(hero, new LinearLayout.LayoutParams(-1, dp(245)));

        if (gameActive) {
            Button resume = button("ΣΥΝΕΧΕΙΑ  →", CARD, TEXT);
            resume.setBackground(round(CARD, isDarkTheme() ? BORDER : Color.rgb(48, 46, 42), 2, radius()));
            resume.setMinHeight(dp(52));
            resume.setOnClickListener(v -> resumeSavedGame());
            root.addView(resume, fullMargins(2, 6, 2, 8));
        }
        addHomeMode(root, "ΜΟΝΟΜΑΧΙΑ", isDarkTheme() ? Color.rgb(174, 105, 251) : Color.rgb(18, 18, 17), 0);
        addHomeMode(root, "ΚΛΑΣΙΚΟ", isDarkTheme() ? Color.rgb(255, 217, 40) : ORANGE, 1);
        addHomeMode(root, "4 ΤΥΡΙΑ", isDarkTheme() ? Color.rgb(0, 221, 208) : BLUE, 2);

        LinearLayout footer = new LinearLayout(this);
        footer.setGravity(Gravity.CENTER_VERTICAL);
        Button settings = button("⚙  Ρυθμίσεις", BG, TEXT);
        settings.setOnClickListener(v -> { settingsSource = ScreenSource.HOME; showSettings(); });
        Button info = button("ⓘ  Πληροφορίες", BG, TEXT);
        info.setOnClickListener(v -> showInstructions());
        footer.addView(settings, new LinearLayout.LayoutParams(0, dp(48), 1));
        footer.addView(info, new LinearLayout.LayoutParams(0, dp(48), 1));
        root.addView(footer, fullMargins(0, 8, 0, 0));
        setScrollable(root);
    }

    private void launchPosterMode(int mode) {
        Runnable launch = () -> {
            clearSavedGame(); resetTeamNamesToDefault();
            if (mode == 0) showSetup();
            else if (mode == 1) { classicTargetPerCategory = 2; showClassicSetup(); }
            else showCheeseSetup();
        };
        if (gameActive) confirmDiscardThen(launch); else launch.run();
    }

    /** Home menu: fixed-height poster tiles keep their labels and glyphs aligned. */
    private void addHomeMode(LinearLayout root, String title, int accent, int mode) {
        FrameLayout tile = new FrameLayout(this);
        tile.setBackground(new PosterDrawable(accent));
        tile.setClipChildren(true);

        int ink = isDarkTheme() && (mode == 0 || mode == 1 || mode == 2)
                ? Color.rgb(12, 10, 16) : Color.WHITE;
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.HORIZONTAL);
        body.setGravity(Gravity.CENTER_VERTICAL);
        // Preserve label width on narrow devices; colors do not affect sizing.
        body.setPadding(dp(18), 0, dp(14), 0);

        // A MATCH_PARENT child in a WRAP_CONTENT FrameLayout measured at zero
        // height on some devices. Use an explicit tile height and center its row.
        View icon = modeGlyph(mode, ink);
        body.addView(icon, new LinearLayout.LayoutParams(dp(36), dp(36)));

        View divider = new View(this);
        divider.setBackgroundColor(ink);
        LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(dp(2), dp(35));
        dividerParams.setMargins(dp(13), 0, dp(14), 0);
        body.addView(divider, dividerParams);

        TextView name = text(title, 28, ink, true);
        name.setTypeface(uiTypeface(true));
        name.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
        name.setSingleLine(true);
        // Scale DOWN before truncating any mode name, in both Light/Dark.
        // Android 6/7 do not have platform TextView auto-size, so use a
        // conservative fallback instead of the larger fixed 28sp size.
        name.setIncludeFontPadding(false);
        name.setEllipsize(null);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            name.setAutoSizeTextTypeUniformWithConfiguration(16, 28, 1, TypedValue.COMPLEX_UNIT_SP);
        } else {
            name.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        }
        body.addView(name, new LinearLayout.LayoutParams(0, -1, 1));
        tile.addView(body, new FrameLayout.LayoutParams(-1, -1));

        tile.setContentDescription("Παίξε " + title);
        tile.setOnClickListener(v -> launchPosterMode(mode));
        attachPressAnimation(tile);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(86));
        lp.setMargins(dp(2), 0, dp(2), dp(10));
        root.addView(tile, lp);
    }

    private View modeGlyph(int mode,int ink){
        return new View(this){
            final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
            @Override protected void onDraw(Canvas canvas){
                super.onDraw(canvas);
                float z=Math.min(getWidth(),getHeight())/48f;
                canvas.save();canvas.translate((getWidth()-48*z)/2f,(getHeight()-48*z)/2f);canvas.scale(z,z);
                p.setColor(ink);p.setStrokeWidth(3.5f);p.setStrokeCap(Paint.Cap.ROUND);
                p.setStrokeJoin(Paint.Join.ROUND);p.setStyle(Paint.Style.STROKE);
                if(mode==0){
                    canvas.drawLine(9,8,37,39,p);canvas.drawLine(37,8,9,39,p);
                    canvas.drawLine(7,18,19,7,p);canvas.drawLine(29,7,41,18,p);
                    canvas.drawLine(9,42,16,35,p);canvas.drawLine(39,42,32,35,p);
                }else if(mode==1){
                    Path cup=new Path();cup.moveTo(13,9);cup.lineTo(35,9);cup.lineTo(32,25);
                    cup.quadTo(24,35,16,25);cup.close();canvas.drawPath(cup,p);
                    canvas.drawArc(new RectF(5,11,17,25),80,200,false,p);
                    canvas.drawArc(new RectF(31,11,43,25),-100,200,false,p);
                    canvas.drawLine(24,31,24,40,p);canvas.drawLine(14,42,34,42,p);
                }else{
                    Path cheese=new Path();cheese.moveTo(6,39);cheese.lineTo(12,18);
                    cheese.lineTo(36,9);cheese.lineTo(43,38);cheese.close();canvas.drawPath(cheese,p);
                    p.setStyle(Paint.Style.FILL);for(int[] h:new int[][]{{21,25,3},{31,32,4},{30,19,2}})
                        canvas.drawCircle(h[0],h[1],h[2],p);
                }
                canvas.restore();
            }
        };
    }

    private void addPosterMode(LinearLayout root, String label, int color, Runnable action) {
        Button b = primaryButton(label, color);
        b.setTypeface(brandTypeface()); b.setTextSize(29);
        b.setBackground(new PosterDrawable(color));
        b.setPadding(dp(24), dp(12), dp(24), dp(12));
        b.setMinHeight(dp(84));
        b.setOnClickListener(v -> action.run());
        root.addView(b, fullMargins(2, 0, 2, 12));
    }

    private class PosterDrawable extends android.graphics.drawable.Drawable {
        private final int fill;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        PosterDrawable(int color) { fill = color; }
        @Override public void draw(Canvas canvas) {
            android.graphics.Rect b = getBounds();
            Path p = new Path(); float slant = dp(15);
            p.moveTo(b.left + slant, b.top); p.lineTo(b.right, b.top);
            p.lineTo(b.right - slant, b.bottom); p.lineTo(b.left, b.bottom); p.close();
            paint.setColor(fill); canvas.drawPath(p, paint);
        }
        @Override public void setAlpha(int alpha) { paint.setAlpha(alpha); }
        @Override public void setColorFilter(android.graphics.ColorFilter filter) { paint.setColorFilter(filter); }
        @Override public int getOpacity() { return android.graphics.PixelFormat.TRANSLUCENT; }
    }

    // Info is paged so no vertical scrolling is needed on a phone.
    private void showInstructions() {
        LinearLayout root=baseColumn();
        root.addView(simpleBackBar("QUIZARA",this::showHome));
        root.addView(sectionTitle("Πληροφορίες"),fullMargins(0,4,0,8));
        String[] titles={"Μονομαχία","Κλασικό","4 Τυριά"};
        int[] colors={modeAccent(GameMode.DUEL),modeAccent(GameMode.CLASSIC),modeAccent(GameMode.CHEESE)};
        for(int i=0;i<titles.length;i++){
            final int page=i;
            Button b=button(titles[i]+"   →",CARD,TEXT);
            b.setTextSize(22);b.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
            b.setPadding(dp(24),0,dp(12),0);
            b.setBackground(round(CARD,colors[i],2,radius()));
            b.setOnClickListener(v->showInformationDetail(page));
            root.addView(b,fullMargins(5,12,5,3));
        }
        setStaticScreen(root);
    }


    private void showInstructionsPage(int page) { showInformationDetail(Math.max(0,Math.min(2,page))); }


    private void showInformationDetail(int page) {
        LinearLayout root = baseColumn();
        String[] titles = {"Μονομαχία", "Κλασικό", "4 Τυριά"};
        page = Math.max(0, Math.min(2, page));
        root.addView(simpleBackBar("QUIZARA", this::showInstructions));
        root.addView(sectionTitle(titles[page]), fullMargins(0, 4, 0, 12));
        if (page == 0) {
            root.addView(instructionSection("Μονομαχία", "Έξι βασικές κατηγορίες με ερωτήσεις ×1, ×2, ×3. Οι ομάδες παίζουν εναλλάξ. Κάθε ομάδα έχει έναν Διπλασιασμό.", modeAccent(GameMode.DUEL)));
            root.addView(instructionSection("Special", "TOP 5 · ΒΡΕΣ 3 · PERSON ID · COUNTRY ID · ΒΑΛ’ ΤΑ ΣΕ ΣΕΙΡΑ · ΒΡΕΣ ΤΗ ΣΥΝΔΕΣΗ. Κάθε επιλεγμένο Special έχει δύο χρήσεις.", PURPLE));
            root.addView(instructionSection("Λήξη", "Το Μαθηματικό Τέλος ισχύει πάντα. Στην ισοπαλία παίζεται Ξαφνικός Θάνατος. Αγγίζεις την κάρτα ερώτησης για να δεις την απάντηση.", BORDER));
        } else if (page == 1) {
            root.addView(instructionSection("Κλασικό", "Δύο σωστές απαντήσεις σε καθεμία από τις έξι κατηγορίες, σύνολο 12. Σε κάθε γύρο εμφανίζονται δύο διαθέσιμες κατηγορίες.", modeAccent(GameMode.CLASSIC)));
            root.addView(instructionSection("Σειρά", "Στη σωστή απάντηση αυξάνεται η πρόοδος της κατηγορίας. Στο λάθος αλλάζει η σειρά χωρίς απώλεια προόδου.", BORDER));
            root.addView(instructionSection("Ενέργειες", "Ανακάτεμα, αλλαγή ερώτησης και Κλοπή, μία φορά ανά ομάδα. Επιλέγεις δυσκολίες πριν από την παρτίδα.", BORDER));
        } else {
            root.addView(instructionSection("4 Τυριά", "Με δύο σωστές απαντήσεις η ομάδα κατακτά την κατηγορία. Όποια ομάδα αποκτήσει τέσσερις κατηγορίες κερδίζει.", modeAccent(GameMode.CHEESE)));
            root.addView(instructionSection("Σαμποτάζ", "Κάθε ομάδα έχει μία χρήση. Μια σωστή απάντηση μειώνει κατά ένα την πρόοδο του αντιπάλου.", BORDER));
            root.addView(instructionSection("Last Dance", "Πριν από τη νίκη στα τέσσερα τυριά μπορεί να δοθεί τελευταία ευκαιρία Σαμποτάζ. Στο 3–3 ακολουθεί Ξαφνικός Θάνατος.", BORDER));
        }
        setStaticScreen(root);
    }

    private View instructionSection(String titleValue, String bodyValue, int accent) {
        LinearLayout card = cardColumn();
        card.setBackground(round(CARD, accent, 1, radius()));
        TextView title = text(titleValue, 15, accent, true);
        card.addView(title, fullMargins(0, 0, 0, 7));
        TextView body = text(bodyValue, 13, TEXT, false);
        body.setLineSpacing(dp(3), 1f);
        card.addView(body);
        return withMargins(card, 4, 0, 4, 12);
    }

    private void showModeSelect() { showHome(); }

    private View simpleBackBar(String titleValue, Runnable backAction) {
        LinearLayout line = new LinearLayout(this);
        line.setGravity(Gravity.CENTER_VERTICAL);
        line.setPadding(0, dp(2), 0, dp(12));
        View back = backArrowButton(true);
        back.setOnClickListener(v -> backAction.run());
        line.addView(back, new LinearLayout.LayoutParams(dp(48), dp(48)));
        TextView title = "QUIZARA".equals(titleValue) ? gameBrandTitle() : text(titleValue, 22, TEXT, true);
        title.setGravity(Gravity.CENTER);
        line.addView(title, new LinearLayout.LayoutParams(0, dp(48), 1));
        View spacer = new View(this);
        line.addView(spacer, new LinearLayout.LayoutParams(dp(48), dp(48)));
        return line;
    }

    private void showResumePrompt() {
        LinearLayout root = baseColumn();
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        TextView hourglass = text("⌛", 62, ORANGE, false);
        hourglass.setGravity(Gravity.CENTER);
        root.addView(hourglass, fullMargins(0, 46, 0, 10));
        TextView title = text("Η ΠΑΡΤΙΔΑ ΣΟΥ ΠΕΡΙΜΕΝΕΙ", 25, TEXT, true);
        title.setGravity(Gravity.CENTER);
        root.addView(title, fullMargins(0, 0, 0, 8));

        String modeLabel = gameMode == GameMode.CLASSIC ? "ΚΛΑΣΙΚΟ" : "ΜΟΝΟΜΑΧΙΑ";
        TextView mode = pill(modeLabel, modeAccent(gameMode), CARD, softOutline(modeAccent(gameMode)));
        mode.setGravity(Gravity.CENTER);
        root.addView(mode, centeredWrapMargins(0, 0, 0, 12));

        TextView score;
        TextView progress;
        if (gameMode == GameMode.CLASSIC) {
            score = text(teamA + "  " + classicTotal(0) + "/" + classicGoalTotal() + " — " + classicTotal(1) + "/" + classicGoalTotal() + "  " + teamB, 20, TEXT, true);
            String stealResumeA = classicStealEnabled ? (classicStealAvailable(0) ? "1" : "0") : "OFF";
            String stealResumeB = classicStealEnabled ? (classicStealAvailable(1) ? "1" : "0") : "OFF";
            progress = text("Σειρά: " + currentTeamName() + "\n" + teamA + "  🥷" + stealResumeA + "  🔀" + (shuffleAvailableA ? "1" : "0") + "  ↻" + (changeQuestionAvailableA ? "1" : "0") + "   ·   " + teamB + "  🥷" + stealResumeB + "  🔀" + (shuffleAvailableB ? "1" : "0") + "  ↻" + (changeQuestionAvailableB ? "1" : "0"), 13, MUTED, false);
        } else {
            score = text(teamA + "  " + scoreA + " — " + scoreB + "  " + teamB, 21, TEXT, true);
            progress = text("Σειρά: " + currentTeamName(), 14, MUTED, false);
        }
        score.setGravity(Gravity.CENTER);
        applyAdaptiveText(score, 11, 20, true);
        root.addView(score, fullMargins(0, 0, 0, 6));
        progress.setGravity(Gravity.CENTER);
        root.addView(progress, fullMargins(0, 0, 0, 20));

        Button resume = primaryButton("▶  ΣΥΝΕΧΕΙΑ", GREEN);
        resume.setOnClickListener(v -> resumeSavedGame());
        root.addView(resume, fullMargins(10, 0, 10, 12));
        Button fresh = button("＋  ΝΕΟ ΠΑΙΧΝΙΔΙ", CARD, TEXT);
        fresh.setBackground(round(CARD, ORANGE, 2, radius()));
        fresh.setOnClickListener(v -> confirmDiscardThen(() -> { clearSavedGame(); resetTeamNamesToDefault(); showModeSelect(); }));
        root.addView(fresh, fullMargins(10, 0, 10, 10));
        Button discard = button("Διαγραφή παρτίδας", BG, MUTED);
        discard.setOnClickListener(v -> { clearSavedGame(); showHome(); });
        root.addView(discard, fullMargins(10, 0, 10, 24));
        setStaticScreen(root);
    }

    private void resumeSavedGame() {
        if (gameMode == GameMode.CHEESE) { resumeCheese(); return; }
        if (!gameActive) { showHome(); return; }
        if (tieChoicePending) {
            showTieChoice();
            return;
        }
        if (inSuddenDeath) {
            if (!suddenStarted) showSuddenDeathIntro();
            else if (suddenMode == null || suddenMode.isEmpty()) showSuddenRoulette();
            else if (!"closest".equals(suddenMode) && suddenResultA != null && suddenResultB != null) {
                if (Boolean.TRUE.equals(suddenResultA) != Boolean.TRUE.equals(suddenResultB)) showWinner(Boolean.TRUE.equals(suddenResultA) ? 0 : 1, true, false);
                else advanceSuddenWithoutWinner();
            }
            else if ("closest".equals(suddenMode)) showClosestSudden();
            else showSuddenQuestion();
            return;
        }
        if (gameMode == GameMode.CLASSIC) {
            if (activeClassicQuestionId != null && !activeClassicQuestionId.isEmpty()) {
                ClassicQuestion q = classicQuestionById.get(activeClassicQuestionId);
                if (q != null) {
                    if (classicQuestionPhase == 1) {
                        if (!classicStealEnabled) {
                            activeClassicQuestionId = "";
                            classicQuestionPhase = 0;
                            refreshClassicCategoryOptions();
                            saveGameState();
                            showClassicBoard();
                        } else showClassicStealOffer(q, currentTeam);
                    } else openClassicQuestion(q, classicQuestionPhase == 2);
                    return;
                }
            }
            showClassicBoard();
            return;
        }

        if (activeSpecialId != null && !activeSpecialId.isEmpty()) {
            SpecialQuestion special = specialQuestionById.get(activeSpecialId);
            if (special != null) { openSpecialQuestion(special); return; }
            activeSpecialId = "";
        }
        if (answered >= totalDuelQuestions()) { showResult(); return; }
        if (earlyFinishEnabled && scoreA != scoreB) {
            int leader = scoreA > scoreB ? 0 : 1;
            int trailer = 1 - leader;
            int leaderScore = leader == 0 ? scoreA : scoreB;
            if (maxReachForTeam(trailer, currentTeam) < leaderScore) {
                showWinner(leader, false, true);
                return;
            }
        }
        if (activeQuestionKey != null && !activeQuestionKey.isEmpty()) {
            Question q = questionMap.get(activeQuestionKey);
            if (q != null) {
                Category c = categoryById(q.category);
                openQuestion(q, c);
                return;
            }
        }
        showBoard();
    }

    private void showSetup() {
        settingsSource = ScreenSource.SETUP;
        LinearLayout root = baseColumn();
        root.addView(topBar(true,false));
        root.addView(sectionTitle("ΡΥΘΜΙΣΕΙΣ ΟΜΑΔΩΝ"), fullMargins(0,4,0,12));
        EditText a = teamInput(teamA), b = teamInput(teamB);
        root.addView(teamColorSettings(a,b),fullMargins(2,0,2,12));
        root.addView(sectionTitle("SPECIAL"),fullMargins(0,8,0,6));
        final String[] types = {"top5","find3","person_id","country_id","rank","connection"};
        final Map<String,Boolean> chosen = new HashMap<>();
        GridLayout specials = new GridLayout(this); specials.setColumnCount(2);
        for(String type : types) {
            chosen.put(type,true);
            Button toggle = button("✓  " + specialTypeLabel(type), specialTypeSoft(type), specialTypeColor(type));
            toggle.setTextSize(12);toggle.setMinHeight(dp(48));
            toggle.setBackground(round(specialTypeSoft(type),specialTypeColor(type),2,radius()));
            toggle.setOnClickListener(v -> {
                boolean on = !Boolean.TRUE.equals(chosen.get(type)); chosen.put(type,on);
                toggle.setText((on?"✓  ":"○  ")+specialTypeLabel(type));
                toggle.setTextColor(on?specialTypeColor(type):MUTED);
                toggle.setBackground(round(on?specialTypeSoft(type):CARD,on?specialTypeColor(type):BORDER,on?2:1,radius()));
            });
            GridLayout.LayoutParams p = new GridLayout.LayoutParams();
            p.width=0; p.height=dp(48);p.columnSpec=GridLayout.spec(GridLayout.UNDEFINED,1f);
            p.setMargins(dp(4),dp(4),dp(4),dp(4));specials.addView(toggle,p);
        }
        root.addView(specials,fullMargins(0,0,0,10));
        Button start = modeStartButton(GameMode.DUEL);
        start.setOnClickListener(v -> {
            teamA=cleanName(a.getText().toString(),DEFAULT_TEAM_A);
            teamB=cleanName(b.getText().toString(),DEFAULT_TEAM_B);
            enabledSpecialTypes.clear();
            for(String type:types) if(Boolean.TRUE.equals(chosen.get(type))) enabledSpecialTypes.add(type);
            resetGame();saveGameState();showBoard();
        });
        root.addView(start,fullMargins(4,2,4,8));
        setStaticScreen(root);
    }


    // ---------- CLASSIC MODE ----------

    private void showClassicDurationSelect() {
        // Classic is always 2 correct answers in all 6 categories: 12 per player.
        classicTargetPerCategory = 2;
        showClassicSetup();
    }


    private Button classicDurationButton(String title, String detail, int target, int accent) {
        Button b = button(title + "\n" + detail, CARD, TEXT);
        b.setTextSize(15);
        b.setGravity(Gravity.CENTER);
        b.setMinHeight(dp(96));
        b.setBackground(round(CARD, accent, target == classicTargetPerCategory ? 2 : 1, radius()));
        return b;
    }

    private int classicGoalTotal() { return 12; }


    private void showClassicSetup() {
        settingsSource = ScreenSource.CLASSIC_SETUP;
        classicTargetPerCategory = 2;
        LinearLayout root=baseColumn();
        root.addView(topBar(true,false));
        root.addView(sectionTitle("ΡΥΘΜΙΣΕΙΣ ΟΜΑΔΩΝ"),fullMargins(0,4,0,12));
        EditText a=teamInput(teamA), b=teamInput(teamB);
        root.addView(teamColorSettings(a,b),fullMargins(2,0,2,12));
        root.addView(sectionTitle("Δυσκολίες παρτίδας"),fullMargins(0,12,0,7));
        final boolean[] selected={true,true,true};
        final Button[] buttons=new Button[3];
        LinearLayout difficultyList=new LinearLayout(this);
        difficultyList.setOrientation(LinearLayout.VERTICAL);
        for(int i=0;i<3;i++) {
            final int idx=i;final int level=i+1;
            Button choice=button("",CARD,TEXT);buttons[i]=choice;
            choice.setMinHeight(dp(54));
            styleClassicDifficultyToggle(choice,level,true);
            choice.setOnClickListener(v -> {
                int count=(selected[0]?1:0)+(selected[1]?1:0)+(selected[2]?1:0);
                if(selected[idx] && count==1) { Toast.makeText(this,"Τουλάχιστον μία δυσκολία",Toast.LENGTH_SHORT).show();return; }
                selected[idx]=!selected[idx];
                styleClassicDifficultyToggle(choice,level,selected[idx]);
            });
            difficultyList.addView(choice,fullMargins(3,3,3,3));
        }
        root.addView(difficultyList);
        Button start=modeStartButton(GameMode.CLASSIC);
        start.setOnClickListener(v -> {
            teamA=cleanName(a.getText().toString(),DEFAULT_TEAM_A);
            teamB=cleanName(b.getText().toString(),DEFAULT_TEAM_B);
            Set<Integer> diffs=new HashSet<>();
            for(int i=0;i<3;i++) if(selected[i]) diffs.add(i+1);
            resetClassicGame(diffs);saveGameState();showClassicBoard();
        });
        root.addView(start,fullMargins(4,12,4,8));
        setStaticScreen(root);
    }


    private void styleClassicDifficultyToggle(Button b, int difficulty, boolean selected) {
        int accent = difficultyColor(difficulty);
        int soft = difficultySoft(difficulty);
        String prefix = selected ? "✓  " : "○  ";
        b.setText(prefix + difficultyName(difficulty));
        b.setTextColor(selected ? accent : MUTED);
        b.setBackground(round(selected ? soft : CARD, selected ? accent : BORDER, selected ? 2 : 1, radius()));
    }

    private void resetClassicGame(Set<Integer> difficulties) {
        gameMode = GameMode.CLASSIC;
        gameActive = true;
        scoreA = scoreB = 0;
        currentTeam = 0;
        answered = 0;
        activeQuestionKey = "";
        activeQuestionRevealed = false;
        activeJokerSelected = false;
        activeSpecialId = "";
        activeSpecialCorrectCount = 0;
        activeSpecialMistakes = 0;
        activeSpecialHintIndex = 0;
        activeSpecialJokerSelected = false;
        activeSpecialInteractionStarted = false;
        activeSpecialAnswersVisible = false;
        activeRankOrder.clear();
        specialPlaysUsed.clear();
        usedSpecialQuestionIds.clear();
        enabledSpecialTypes.clear();
        clearQuestionTimerState();
        inSuddenDeath = false;
        suddenStarted = false;
        suddenIndex = 0;
        suddenRound = 1;
        suddenTeam = 0;
        suddenResultA = suddenResultB = null;
        suddenMode = "";
        activeSuddenQuestionId = "";
        suddenClosestInputA = "";
        suddenClosestInputB = "";
        suddenClosestResolved = false;
        suddenClosestValueA = 0d;
        suddenClosestValueB = 0d;
        suddenUsedQuestionIds.clear();
        tieChoicePending = false;

        classicProgressA.clear();
        classicProgressB.clear();
        classicUsedQuestionIds.clear();
        classicDifficulties.clear();
        classicDifficulties.addAll(difficulties);
        classicDifficultySchedule.clear();
        ensureClassicDifficultySchedule(40);
        classicQuestionsPlayedA = 0;
        classicQuestionsPlayedB = 0;
        stealAvailableA = true;
        stealAvailableB = true;
        shuffleAvailableA = true;
        shuffleAvailableB = true;
        changeQuestionAvailableA = true;
        changeQuestionAvailableB = true;
        classicOption1 = "";
        classicOption2 = "";
        activeClassicQuestionId = "";
        classicQuestionRevealed = false;
        classicAnswerEverRevealed = false;
        classicQuestionPhase = 0;
        classicFinalChance = false;
        classicFinisher = -1;
        refreshClassicCategoryOptions();
    }

    private void ensureClassicDifficultySchedule(int size) {
        if (classicDifficulties.isEmpty()) classicDifficulties.add(2);
        Random random = new Random();
        while (classicDifficultySchedule.size() < size) {
            List<Integer> cycle = new ArrayList<>(classicDifficulties);
            Collections.sort(cycle);
            Collections.shuffle(cycle, random);
            classicDifficultySchedule.addAll(cycle);
        }
    }

    private int classicNextDifficulty(int team) {
        int index = team == 0 ? classicQuestionsPlayedA : classicQuestionsPlayedB;
        ensureClassicDifficultySchedule(index + 1);
        return classicDifficultySchedule.get(index);
    }

    private int classicProgress(int team, String category) {
        Map<String, Integer> map = team == 0 ? classicProgressA : classicProgressB;
        return Math.min(classicTargetPerCategory, Math.max(0, countOf(map, category)));
    }

    private int classicTotal(int team) {
        int total = 0;
        for (Category c : CATEGORIES) total += classicProgress(team, c.id);
        return total;
    }

    private void addClassicProgress(int team, String category) {
        Map<String, Integer> map = team == 0 ? classicProgressA : classicProgressB;
        int now = Math.min(classicTargetPerCategory, countOf(map, category) + 1);
        map.put(category, now);
        scoreA = classicTotal(0);
        scoreB = classicTotal(1);
    }

    private boolean classicStealAvailable(int team) {
        if (!classicStealEnabled) return false;
        return team == 0 ? stealAvailableA : stealAvailableB;
    }

    private void consumeClassicSteal(int team) {
        if (team == 0) stealAvailableA = false;
        else stealAvailableB = false;
    }

    private boolean classicShuffleAvailable(int team) {
        return team == 0 ? shuffleAvailableA : shuffleAvailableB;
    }

    private void consumeClassicShuffle(int team) {
        if (team == 0) shuffleAvailableA = false;
        else shuffleAvailableB = false;
    }

    private boolean classicChangeAvailable(int team) {
        return team == 0 ? changeQuestionAvailableA : changeQuestionAvailableB;
    }

    private void consumeClassicChange(int team) {
        if (team == 0) changeQuestionAvailableA = false;
        else changeQuestionAvailableB = false;
    }

    private void refreshClassicCategoryOptions() {
        List<Category> eligible = new ArrayList<>();
        for (Category c : CATEGORIES) if (classicProgress(currentTeam, c.id) < classicTargetPerCategory) eligible.add(c);
        Collections.shuffle(eligible);
        classicOption1 = eligible.size() > 0 ? eligible.get(0).id : "";
        classicOption2 = eligible.size() > 1 ? eligible.get(1).id : "";
        saveGameState();
    }

    private void reshuffleClassicOptions() {
        if (!classicShuffleAvailable(currentTeam)) return;
        String old1 = classicOption1;
        String old2 = classicOption2;
        List<Category> eligible = new ArrayList<>();
        for (Category c : CATEGORIES) if (classicProgress(currentTeam, c.id) < classicTargetPerCategory) eligible.add(c);

        // 2 (or 1) remaining: there is no meaningful alternative, so nothing changes
        // and the one-use Ανακάτεμα is not consumed.
        if (eligible.size() <= 2) {
            Toast.makeText(this, "Δεν υπάρχουν άλλες κατηγορίες για ανακάτεμα", Toast.LENGTH_SHORT).show();
            return;
        }

        if (eligible.size() == 3) {
            // Exactly three remain: replace only ONE of the two current choices with
            // the third category, as requested.
            Category third = null;
            for (Category c : eligible) {
                if (!c.id.equals(old1) && !c.id.equals(old2)) { third = c; break; }
            }
            if (third == null) {
                Collections.shuffle(eligible);
                third = eligible.get(0);
            }
            if (new Random().nextBoolean()) classicOption1 = third.id;
            else classicOption2 = third.id;
        } else {
            // Four or more remain: BOTH visible categories must change.
            List<Category> fresh = new ArrayList<>();
            for (Category c : eligible) if (!c.id.equals(old1) && !c.id.equals(old2)) fresh.add(c);
            Collections.shuffle(fresh);
            if (fresh.size() >= 2) {
                classicOption1 = fresh.get(0).id;
                classicOption2 = fresh.get(1).id;
            } else {
                // Defensive fallback; should only be reachable with malformed state.
                Collections.shuffle(eligible);
                classicOption1 = eligible.get(0).id;
                classicOption2 = eligible.get(1).id;
            }
        }
        consumeClassicShuffle(currentTeam);
        if (hapticsEnabled) try { getWindow().getDecorView().performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP); } catch (Throwable ignored) { }
        saveGameState();
        showClassicBoard();
    }

    private void showClassicBoard() {
        settingsSource=ScreenSource.CLASSIC_BOARD;
        gameMode=GameMode.CLASSIC;
        activeClassicQuestionId="";classicQuestionRevealed=false;
        classicAnswerEverRevealed=false;classicQuestionPhase=0;
        if(classicOption1.isEmpty() && classicOption2.isEmpty()) refreshClassicCategoryOptions();
        saveGameState();
        LinearLayout root=baseColumn();
        root.addView(gameTopBar(true));
        root.addView(classicCompactScoreStrip(),fullMargins(5,3,5,9));
        root.addView(turnBanner(currentTeamName(),currentTeam),centeredWrapMargins(0,3,0,14));
        if(classicShuffleAvailable(currentTeam)) {
            Button shuffle=button("🔀  ΑΝΑΚΑΤΕΜΑ · 1",ORANGE_SOFT,ORANGE);
            shuffle.setMinHeight(dp(44));shuffle.setOnClickListener(v->reshuffleClassicOptions());
            root.addView(shuffle,centeredWrapMargins(0,0,0,10));
        }
        if(!classicOption1.isEmpty()){
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(150));
            lp.setMargins(dp(6),0,dp(6),dp(12));
            root.addView(classicCategoryChoice(classicOption1),lp);
        }
        if(!classicOption2.isEmpty()){
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(150));
            lp.setMargins(dp(6),0,dp(6),dp(6));
            root.addView(classicCategoryChoice(classicOption2),lp);
        }
        setStaticScreen(root);
    }


    private View classicToolsStatusCard(int team) {
        int accent = team == 0 ? BLUE : RED;
        int soft = team == 0 ? BLUE_SOFT : RED_SOFT;
        String name = team == 0 ? teamA : teamB;
        boolean steal = classicStealAvailable(team);
        boolean shuffle = classicShuffleAvailable(team);
        boolean change = classicChangeAvailable(team);
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER);
        card.setPadding(dp(10), dp(10), dp(10), dp(10));
        card.setBackground(round(CARD, accent, 1, radius()));
        card.setElevation(dp(5));
        TextView title = text(name, 12, accent, true);
        title.setGravity(Gravity.CENTER);
        applyAdaptiveText(title, 9, 12, true);
        card.addView(title, fullMargins(0, 0, 0, 5));
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER);
        TextView stealChip = pill(classicStealEnabled ? ("🥷 " + (steal ? "1" : "0")) : "🥷 OFF", steal ? PURPLE : MUTED, steal ? PURPLE_SOFT : CARD, steal ? PURPLE : BORDER);
        TextView shuffleChip = pill("🔀 " + (shuffle ? "1" : "0"), shuffle ? ORANGE : MUTED, shuffle ? ORANGE_SOFT : CARD, shuffle ? ORANGE : BORDER);
        TextView changeChip = pill("↻ " + (change ? "1" : "0"), change ? BLUE : MUTED, change ? BLUE_SOFT : CARD, change ? BLUE : BORDER);
        stealChip.setGravity(Gravity.CENTER);
        shuffleChip.setGravity(Gravity.CENTER);
        changeChip.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams stealLp = new LinearLayout.LayoutParams(0, dp(32), 1);
        stealLp.setMargins(0, 0, dp(3), 0);
        LinearLayout.LayoutParams shuffleLp = new LinearLayout.LayoutParams(0, dp(32), 1);
        shuffleLp.setMargins(dp(2), 0, dp(2), 0);
        LinearLayout.LayoutParams changeLp = new LinearLayout.LayoutParams(0, dp(32), 1);
        changeLp.setMargins(dp(3), 0, 0, 0);
        row.addView(stealChip, stealLp);
        row.addView(shuffleChip, shuffleLp);
        row.addView(changeChip, changeLp);
        card.addView(row);
        return card;
    }

    private View classicCompactScoreStrip() {
        LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(6),dp(7),dp(6),dp(7));
        row.setBackground(round(CARD,BORDER,1,radius()));
        for(int team=0;team<2;team++) {
            final int index=team;
            TextView label=text((currentTeam==team?"▶  ":"")+(team==0?teamA:teamB)+"  "+classicTotal(team)+"/12",16,teamAccent(team),true);
            applyAdaptiveText(label,10,16,true);
            label.setPadding(dp(3),dp(11),dp(3),dp(11));
            label.setBackground(round(currentTeam==team?teamTint(team):CARD,teamAccent(team),currentTeam==team?3:1,radius()));
            if(index==1){TextView vs=text("VS",13,MUTED,true);vs.setGravity(Gravity.CENTER);
                row.addView(vs,new LinearLayout.LayoutParams(dp(35),dp(48)));}
            row.addView(label,new LinearLayout.LayoutParams(0,dp(54),1));
        }
        return row;
    }


    private View classicProgressPanel(int team) {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(5), dp(14), dp(5), dp(14));
        int teamAccent = teamAccent(team);
        panel.setBackground(round(CARD, teamAccent, 1, radius()));
        panel.setElevation(dp(5));
        TextView head = text(team == 0 ? "Α" : "Β", 14, teamAccent, true);
        head.setGravity(Gravity.CENTER);
        panel.addView(head, fullMargins(0, 0, 0, 4));
        for (Category c : CATEGORIES) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.VERTICAL);
            row.setGravity(Gravity.CENTER);
            row.setPadding(0, dp(7), 0, dp(7));
            int accent = categoryAccent(c);
            TextView top = text(c.icon + " " + c.name, 9, accent, true);
            top.setSingleLine(true);
            top.setGravity(Gravity.CENTER);
            row.addView(top);
            LinearLayout dots = new LinearLayout(this);
            dots.setGravity(Gravity.CENTER);
            int count = classicProgress(team, c.id);
            for (int i = 0; i < classicTargetPerCategory; i++) {
                TextView dot = text(i < count ? "●" : "○", 12, i < count ? accent : MUTED, true);
                dot.setGravity(Gravity.CENTER);
                dots.addView(dot, new LinearLayout.LayoutParams(dp(17), dp(16)));
            }
            row.addView(dots);
            panel.addView(row);
        }
        return panel;
    }

    private View classicCategoryChoice(String categoryId) {
        Category c = categoryById(categoryId);
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER);
        card.setPadding(dp(12), dp(14), dp(12), dp(14));
        int accent = categoryAccent(c);
        // Same restrained category-card language used by the 4 Cheeses board.
        card.setBackground(round(CARD, softOutline(accent), 1, radius()));
        card.setElevation(0);
        card.setMinimumHeight(dp(134));
        card.addView(categoryGlyph(c.id,accent,34));
        TextView name = text(c.name.toUpperCase(java.util.Locale.forLanguageTag("el")), 20, accent, true);
        name.setSingleLine(true);
        name.setGravity(Gravity.CENTER);
        card.addView(name, fullMargins(0, 1, 0, 2));
        TextView currentProgress = text(teamA + " " + classicProgress(0, c.id) + "/2   ·   " + teamB + " " + classicProgress(1, c.id) + "/2", 11, TEXT, true);
        currentProgress.setGravity(Gravity.CENTER);
        applyAdaptiveText(currentProgress, 9, 11, true);
        card.addView(currentProgress);
        attachPressAnimation(card);
        card.setOnClickListener(v -> startClassicQuestion(c.id));
        return card;
    }

    private void startClassicQuestion(String categoryId) {
        if (classicProgress(currentTeam, categoryId) >= classicTargetPerCategory) {
            refreshClassicCategoryOptions();
            showClassicBoard();
            return;
        }
        int difficulty = classicNextDifficulty(currentTeam);
        ClassicQuestion q = pickClassicQuestion(categoryId, difficulty);
        if (q == null) {
            Toast.makeText(this, "Δεν βρέθηκε διαθέσιμη ερώτηση", Toast.LENGTH_SHORT).show();
            return;
        }
        if (currentTeam == 0) classicQuestionsPlayedA++; else classicQuestionsPlayedB++;
        classicUsedQuestionIds.add(q.id);
        classicOption1 = classicOption2 = "";
        activeClassicQuestionId = q.id;
        classicQuestionRevealed = false;
        classicAnswerEverRevealed = false;
        classicQuestionPhase = 0;
        answered++;
        prepareQuestionTimerForNewQuestion();
        saveGameState();
        showClassicQuestionLoading(q);
    }

    private ClassicQuestion pickClassicQuestion(String categoryId, int difficulty) {
        List<ClassicQuestion> candidates = new ArrayList<>();
        for (ClassicQuestion q : classicQuestions) {
            if (q.category.equals(categoryId) && q.difficulty == difficulty && !classicUsedQuestionIds.contains(q.id)) candidates.add(q);
        }
        if (candidates.isEmpty()) {
            // Starter bank fallback: allow an old question only after this bucket is exhausted.
            for (ClassicQuestion q : classicQuestions) if (q.category.equals(categoryId) && q.difficulty == difficulty) candidates.add(q);
        }
        if (candidates.isEmpty()) return null;
        Collections.shuffle(candidates);
        return candidates.get(0);
    }

    private void replaceClassicQuestion(ClassicQuestion oldQuestion) {
        if (oldQuestion == null || !classicChangeAvailable(currentTeam) || classicAnswerEverRevealed) return;
        List<ClassicQuestion> candidates = new ArrayList<>();
        for (ClassicQuestion q : classicQuestions) {
            if (q.category.equals(oldQuestion.category) && q.difficulty == oldQuestion.difficulty && !q.id.equals(oldQuestion.id) && !classicUsedQuestionIds.contains(q.id)) {
                candidates.add(q);
            }
        }
        if (candidates.isEmpty()) {
            Toast.makeText(this, "Δεν υπάρχει διαθέσιμη άλλη ερώτηση ίδιας δυσκολίας", Toast.LENGTH_SHORT).show();
            return;
        }
        Collections.shuffle(candidates);
        ClassicQuestion replacement = candidates.get(0);
        consumeClassicChange(currentTeam);
        classicUsedQuestionIds.add(replacement.id);
        activeClassicQuestionId = replacement.id;
        classicQuestionRevealed = false;
        classicAnswerEverRevealed = false;
        classicQuestionPhase = 0;
        prepareQuestionTimerForNewQuestion();
        saveGameState();
        showClassicQuestionLoading(replacement);
    }

    private void showClassicQuestionLoading(ClassicQuestion q) {
        Category c = categoryById(q.category);
        showQuestionTransition(c.name, c.id, categoryAccent(c), () -> openClassicQuestion(q, false));
    }

    private void openClassicQuestion(ClassicQuestion q, boolean stealAttempt) {
        if (q == null) { showClassicBoard(); return; }
        settingsSource = ScreenSource.CLASSIC_BOARD;
        activeClassicQuestionId = q.id;
        classicQuestionPhase = stealAttempt ? 2 : 0;
        saveGameState();

        LinearLayout root = baseColumn();
        root.addView(gameTopBar(false));
        root.addView(classicCompactScoreStrip(), fullMargins(4, 0, 4, 8));

        if (stealAttempt) {
            TextView steal = pill("🥷 ΚΛΟΠΗ · " + currentTeamName(), PURPLE, PURPLE_SOFT, PURPLE);
            steal.setGravity(Gravity.CENTER);
            root.addView(steal, centeredWrapMargins(0, 0, 0, 8));
        } else {
            TextView turn = pill("Σειρά: " + currentTeamName(), teamAccent(currentTeam), CARD, softOutline(teamAccent(currentTeam)));
            turn.setGravity(Gravity.CENTER);
            root.addView(turn, centeredWrapMargins(0, 0, 0, 8));
        }

        Category c = categoryById(q.category);
        TextView category = pill(c.icon + "  " + c.name, categoryAccent(c), categorySoft(c), categoryAccent(c));
        category.setGravity(Gravity.CENTER);
        root.addView(category, centeredWrapMargins(0, 0, 0, 8));

        addQuestionTimer(root);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER);
        card.setPadding(dp(18), dp(24), dp(18), dp(24));
        card.setMinimumHeight(dp(300));
        card.setBackground(round(CARD, categoryAccent(c), 2, radius()));
        card.setElevation(dp(5));
        card.setCameraDistance(dp(12000));
        final boolean[] revealed = {classicQuestionRevealed};
        if (revealed[0]) renderClassicAnswerFace(card, q); else renderClassicQuestionFace(card, q);
        card.setOnClickListener(v -> {
            revealed[0] = !revealed[0];
            classicQuestionRevealed = revealed[0];
            if (revealed[0]) classicAnswerEverRevealed = true;
            saveGameState();
            flipClassicCard(card, q, revealed[0]);
        });
        root.addView(card, fullMargins(4, 0, 4, 8));

        if (!stealAttempt && classicChangeAvailable(currentTeam)) {
            LinearLayout changeRow = new LinearLayout(this);
            changeRow.setGravity(Gravity.CENTER_VERTICAL);
            Button change = button("↻  ΑΛΛΑΓΗ ΕΡΩΤΗΣΗΣ · 1", BLUE_SOFT, BLUE);
            change.setTextSize(12);
            change.setMinHeight(dp(48));
            change.setOnClickListener(v -> {
                if (classicAnswerEverRevealed) {
                    Toast.makeText(this, "Η απάντηση έχει ήδη αποκαλυφθεί", Toast.LENGTH_SHORT).show();
                    return;
                }
                replaceClassicQuestion(q);
            });
            changeRow.addView(change, new LinearLayout.LayoutParams(0, dp(48), 1));
            Button changeInfo = infoButton("Αλλαγή ερώτησης", "Μία φορά ανά παίκτη. Αντικαθιστά την τρέχουσα ερώτηση με άλλη ίδιας κατηγορίας και δυσκολίας. Χρησιμοποιείται μόνο πριν αποκαλυφθεί η απάντηση.", BLUE);
            LinearLayout.LayoutParams cip = new LinearLayout.LayoutParams(dp(42), dp(42));
            cip.setMargins(dp(8), 0, 0, 0);
            changeRow.addView(changeInfo, cip);
            root.addView(changeRow, fullMargins(4, 0, 4, 9));
        }

        TextView prompt = text(stealAttempt ? "Ο κλέφτης το βρήκε;" : "Η ομάδα το βρήκε;", 15, TEXT, true);
        prompt.setGravity(Gravity.CENTER);
        root.addView(prompt, fullMargins(0, 4, 0, 7));

        LinearLayout actions = new LinearLayout(this);
        int correctAccent = GREEN;
        int wrongAccent = RED;
        int correctSoft = GREEN_SOFT;
        int wrongSoft = RED_SOFT;
        Button correct = button("✓  ΣΩΣΤΟ", correctSoft, correctAccent);
        correct.setBackground(round(correctSoft, correctAccent, 2, radius()));
        correct.setMinHeight(dp(58));
        Button wrong = button("✕  ΛΑΘΟΣ", wrongSoft, wrongAccent);
        wrong.setBackground(round(wrongSoft, wrongAccent, 2, radius()));
        wrong.setMinHeight(dp(58));
        actions.addView(correct, weightedMargins(1, 4, 0, 4, 0));
        actions.addView(wrong, weightedMargins(1, 4, 4, 0, 0));
        root.addView(actions, fullMargins(0, 0, 0, 26));

        if (stealAttempt) {
            correct.setOnClickListener(v -> resolveClassicSteal(q, true));
            wrong.setOnClickListener(v -> resolveClassicSteal(q, false));
        } else {
            correct.setOnClickListener(v -> resolveClassicAnswer(q, true));
            wrong.setOnClickListener(v -> resolveClassicAnswer(q, false));
        }
        setStaticScreen(root);
    }

    private void renderClassicQuestionFace(LinearLayout card, ClassicQuestion q) {
        card.removeAllViews();
        TextView qv = text(q.question, 22, TEXT, true);
        qv.setGravity(Gravity.CENTER);
        qv.setLineSpacing(dp(4), 1f);
        card.addView(qv);
    }

    private void renderClassicAnswerFace(LinearLayout card, ClassicQuestion q) {
        card.removeAllViews();
        TextView label = text("ΑΠΑΝΤΗΣΗ", 10, MUTED, true);
        label.setGravity(Gravity.CENTER);
        card.addView(label, fullMargins(0, 0, 0, 10));
        TextView av = text(q.answer, 24, TEXT, true);
        av.setGravity(Gravity.CENTER);
        av.setLineSpacing(dp(4), 1f);
        card.addView(av);
        if (!q.explanation.isEmpty()) {
            TextView ex = text(q.explanation, 11, MUTED, false);
            ex.setGravity(Gravity.CENTER);
            ex.setLineSpacing(dp(3), 1f);
            card.addView(ex, fullMargins(0, 12, 0, 0));
        }
    }

    private void flipClassicCard(LinearLayout card, ClassicQuestion q, boolean toAnswer) {
        playFlipFeedback();
        if (!effectsEnabled) {
            if (toAnswer) renderClassicAnswerFace(card, q); else renderClassicQuestionFace(card, q);
            return;
        }
        card.animate().rotationY(90f).setDuration(145).withEndAction(() -> {
            if (toAnswer) renderClassicAnswerFace(card, q); else renderClassicQuestionFace(card, q);
            card.setRotationY(-90f);
            card.animate().rotationY(0f).setDuration(145).start();
        }).start();
    }

    private void resolveClassicAnswer(ClassicQuestion q, boolean correct) {
        int answeringTeam = currentTeam;
        clearQuestionTimerState();
        playFeedback(correct);
        activeClassicQuestionId = q.id;
        classicQuestionRevealed = false;
        if (correct) {
            addClassicProgress(answeringTeam, q.category);
            activeClassicQuestionId = "";
            classicQuestionPhase = 0;
            if (handleClassicCompletion(answeringTeam)) return;
            // Correct answer: same player continues.
            refreshClassicCategoryOptions();
            saveGameState();
            showClassicBoard();
            return;
        }

        // During the second player's final chance, a miss ends the match immediately.
        if (classicFinalChance && answeringTeam == 1) {
            showClassicWinner(0, false);
            return;
        }

        int thief = 1 - answeringTeam;
        if (classicStealAvailable(thief) && classicProgress(thief, q.category) < classicTargetPerCategory) {
            classicQuestionPhase = 1;
            saveGameState();
            showClassicStealOffer(q, thief);
        } else {
            currentTeam = thief;
            activeClassicQuestionId = "";
            classicQuestionPhase = 0;
            refreshClassicCategoryOptions();
            saveGameState();
            showClassicBoard();
        }
    }

    private void showClassicStealOffer(ClassicQuestion q, int thief) {
        if (!classicStealEnabled) {
            currentTeam = thief;
            activeClassicQuestionId = "";
            classicQuestionPhase = 0;
            refreshClassicCategoryOptions();
            saveGameState();
            showClassicBoard();
            return;
        }
        currentTeam = thief;
        activeClassicQuestionId = q.id;
        classicQuestionPhase = 1;
        saveGameState();
        LinearLayout root = baseColumn();
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.addView(gameTopBar(false));
        TextView wrong = text("✕", 54, RED, true);
        wrong.setGravity(Gravity.CENTER);
        root.addView(wrong, fullMargins(0, 28, 0, 4));
        TextView title = text("ΛΑΘΟΣ", 24, TEXT, true);
        title.setGravity(Gravity.CENTER);
        root.addView(title);
        TextView question = text(q.question, 16, TEXT, true);
        question.setGravity(Gravity.CENTER);
        question.setLineSpacing(dp(4), 1f);
        question.setPadding(dp(14), dp(14), dp(14), dp(14));
        question.setBackground(round(CARD, categoryAccent(categoryById(q.category)), 1, radius()));
        root.addView(question, fullMargins(8, 16, 8, 16));
        TextView ask = text(teamA.equals(teamB) ? "Ο αντίπαλος έχει μία Κλοπή. Θέλει να τη χρησιμοποιήσει;" : currentTeamName() + ", θέλεις να κλέψεις την ερώτηση;", 16, TEXT, true);
        ask.setGravity(Gravity.CENTER);
        root.addView(ask, fullMargins(8, 0, 8, 16));
        Button steal = primaryButton("🥷  ΚΛΟΠΗ", PURPLE);
        steal.setOnClickListener(v -> {
            consumeClassicSteal(currentTeam);
            classicQuestionPhase = 2;
            classicQuestionRevealed = false;
            prepareQuestionTimerForNewQuestion();
            saveGameState();
            openClassicQuestion(q, true);
        });
        root.addView(steal, fullMargins(8, 0, 8, 10));
        Button pass = button("ΠΑΣΟ · Κρατάω την Κλοπή", CARD, TEXT);
        pass.setBackground(round(CARD, BORDER, 1, radius()));
        pass.setOnClickListener(v -> {
            activeClassicQuestionId = "";
            classicQuestionPhase = 0;
            refreshClassicCategoryOptions();
            saveGameState();
            showClassicBoard();
        });
        root.addView(pass, fullMargins(8, 0, 8, 24));
        setStaticScreen(root);
    }

    private void resolveClassicSteal(ClassicQuestion q, boolean correct) {
        int thief = currentTeam;
        clearQuestionTimerState();
        playFeedback(correct);
        if (correct) addClassicProgress(thief, q.category);
        activeClassicQuestionId = "";
        classicQuestionRevealed = false;
        classicQuestionPhase = 0;
        if (correct && handleClassicCompletion(thief)) return;
        // A steal never consumes the thief's normal turn; they now play normally.
        refreshClassicCategoryOptions();
        saveGameState();
        showClassicBoard();
    }

    private boolean handleClassicCompletion(int team) {
        if (classicTotal(team) < classicGoalTotal()) return false;
        scoreA = classicTotal(0);
        scoreB = classicTotal(1);
        if (classicTotal(0) == classicGoalTotal() && classicTotal(1) == classicGoalTotal()) {
            startClassicTieBreak();
            return true;
        }
        if (team == 1) {
            // ONLY Team A (the team that starts) can trigger a last equalizing
            // opportunity for Team B. If B finishes first, B wins immediately.
            showClassicWinner(1, false);
            return true;
        }
        // The FIRST/starting team (A) finished all categories. Only the SECOND
        // team (B) may keep answering to equalize, ending on its first miss.
        classicFinalChance = true;
        classicFinisher = 0;
        currentTeam = 1;
        activeClassicQuestionId = "";
        classicQuestionPhase = 0;
        refreshClassicCategoryOptions();
        saveGameState();
        showClassicBoard();
        return true;
    }

    private void startClassicTieBreak() {
        scoreA = scoreB = classicGoalTotal();
        classicFinalChance = false;
        showTieChoice();
    }

    private void showClassicWinner(int winnerTeam, boolean suddenDeath) {
        showUnifiedWinner(winnerTeam);
    }

    // ---------- BOARD ----------

    private void showBoard() {
        settingsSource=ScreenSource.BOARD;
        activeQuestionKey="";activeQuestionRevealed=false;activeJokerSelected=false;
        if(gameActive && answered>=totalDuelQuestions()){showResult();return;}
        if(gameActive)saveGameState();
        if(finalQuestionEnabled && remainingCount()==1 && !finalIntroShown){
            finalIntroShown=true;showFinalQuestionIntro();return;
        }
        LinearLayout root=baseColumn();
        root.addView(gameTopBar(false));
        root.addView(scoreStrip(),fullMargins(4,3,4,7));
        root.addView(turnBanner(currentTeamName(),currentTeam),centeredWrapMargins(0,2,0,8));
        // Choose the one-time double BEFORE opening any regular or Special question.
        // The choice is carried through the loading screen and is locked on the question.
        if (isJokerAvailable(currentTeam)) {
            Button doubleChoice = button("", CARD, TEXT);
            Runnable renderDouble = () -> {
                doubleChoice.setText(activeJokerSelected ? "⚡ ΔΙΠΛΑΣΙΑΣΜΟΣ ✓" : "⚡ ΔΙΠΛΑΣΙΑΣΜΟΣ");
                int accent = modeAccent(GameMode.DUEL);
                doubleChoice.setTextColor(activeJokerSelected ? accent : MUTED);
                doubleChoice.setBackground(round(CARD, activeJokerSelected ? softOutline(accent) : BORDER, 1, radius()));
            };
            renderDouble.run();
            doubleChoice.setOnClickListener(v -> {
                activeJokerSelected = !activeJokerSelected;
                renderDouble.run();
                saveGameState();
            });
            root.addView(doubleChoice, fullMargins(4,0,4,5));
        }
        GridLayout categories=new GridLayout(this);categories.setColumnCount(2);
        String[] ids={"history","geography","science","art","sports","general"};
        for(String id:ids){
            Category c=categoryById(id);
            GridLayout.LayoutParams lp=new GridLayout.LayoutParams();lp.width=0;lp.height=dp(96);
            lp.columnSpec=GridLayout.spec(GridLayout.UNDEFINED,1f);
            lp.setMargins(dp(3),dp(3),dp(3),dp(3));
            categories.addView(duelCategoryTile(c),lp);
        }
        root.addView(categories,fullMargins(0,0,0,5));
        if(!enabledSpecialTypes.isEmpty()){
            root.addView(sectionTitle("SPECIAL"),fullMargins(0,0,0,3));
            root.addView(duelSpecialBoardSection(),fullMargins(0,0,0,2));
        }
        setStaticScreen(root);
    }


    private View duelCategoryTile(Category c) {
        int accent=categoryAccent(c);
        LinearLayout tile=new LinearLayout(this);
        tile.setOrientation(LinearLayout.VERTICAL);tile.setGravity(Gravity.CENTER);
        tile.setPadding(dp(4),dp(5),dp(4),dp(5));
        tile.setBackground(round(categorySoft(c), softOutline(accent), 1, radius()));
        LinearLayout header=new LinearLayout(this);header.setGravity(Gravity.CENTER);
        header.addView(categoryGlyph(c.id,accent,26),new LinearLayout.LayoutParams(dp(26),dp(26)));
        TextView title=text(c.name.toUpperCase(java.util.Locale.forLanguageTag("el")),14,accent,true);
        title.setSingleLine(true);applyAdaptiveText(title,11,14,true);
        header.addView(title,new LinearLayout.LayoutParams(0,dp(28),1));
        tile.addView(header,new LinearLayout.LayoutParams(-1,dp(30)));
        LinearLayout difficulties=new LinearLayout(this);difficulties.setGravity(Gravity.CENTER);
        for(int n=1;n<=3;n++){
            final int m=n;boolean available=!used.contains(key(c.id,n));
            Button b=button("×"+n,CARD,available?accent:MUTED);b.setTextSize(13);
            b.setBackground(round(available?CARD:BG,available?softOutline(accent):BORDER,1,8));
            b.setAlpha(available?1f:0.36f);b.setEnabled(available);
            b.setOnClickListener(v->showQuestionLoading(questionMap.get(key(c.id,m)),c));
            difficulties.addView(b,weightedMargins(1,2,0,2,0));
        }
        tile.addView(difficulties,new LinearLayout.LayoutParams(-1,dp(38)));
        return tile;
    }

    private View categoryRow(Category c) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(5), dp(4), dp(5), dp(4));
        row.setBackground(round(CARD, BORDER, 1, radius()));
        row.setLayoutParams(fullMargins(4, 2, 4, 2));

        int accent = categoryAccent(c);
        TextView icon = iconCircle(c.icon, accent, categorySoft(c));
        row.addView(icon, new LinearLayout.LayoutParams(dp(40), dp(40)));

        TextView name = text(c.name, 14, TEXT, true);
        name.setSingleLine(true); // Geography must never wrap.
        name.setGravity(Gravity.CENTER_VERTICAL);
        name.setPadding(dp(7), 0, dp(4), 0);
        row.addView(name, new LinearLayout.LayoutParams(0, dp(40), 1));

        int[] order = new int[]{1, 2, 3};
        for (int m : order) {
            String k = key(c.id, m);
            int difficultyAccent = difficultyColor(m);
            Button tile = button("×" + m, CARD, difficultyAccent);
            tile.setTextSize(14);
            tile.setMinHeight(dp(40));
            tile.setBackground(round(CARD, difficultyAccent, 2, 10));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(42), dp(40));
            lp.setMargins(dp(3), 0, 0, 0);
            row.addView(tile, lp);
            if (used.contains(k)) {
                tile.setVisibility(View.INVISIBLE);
            } else {
                tile.setOnClickListener(v -> showQuestionLoading(questionMap.get(k), c));
            }
        }
        return row;
    }

    private void styleSpecialToggle(Button b, String type, boolean selected) {
        int accent = specialTypeColor(type);
        b.setText((selected ? "✓  " : "○  ") + specialTypeLabel(type));
        b.setTextSize(11);
        b.setTextColor(selected ? accent : MUTED);
        b.setGravity(Gravity.CENTER);
        b.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        b.setIncludeFontPadding(false);
        b.setPadding(dp(10), 0, dp(10), 0);
        b.setBackground(round(selected ? specialTypeSoft(type) : CARD, selected ? accent : BORDER, selected ? 2 : 1, 11));
    }

    private View duelSpecialBoardSection() {
        GridLayout grid=new GridLayout(this);grid.setColumnCount(2);
        String[] types={"top5","find3","person_id","country_id","rank","connection"};
        for(String type:types){
            if(!enabledSpecialTypes.contains(type))continue;
            int left=Math.max(0,2-countOf(specialPlaysUsed, type));
            int accent=specialTypeColor(type);
            String uses=left==1?"1 χρήση":left+" χρήσεις";
            Button b=button(specialTypeLabel(type)+"  ·  "+uses, left>0?specialTypeSoft(type):BG,left>0?accent:MUTED);
            b.setTextSize(11);b.setMinHeight(dp(44));
            b.setBackground(round(left>0?specialTypeSoft(type):BG,left>0?softOutline(accent):BORDER,1,radius()));
            b.setEnabled(left>0);
            b.setAlpha(left>0?1f:0.43f);
            b.setContentDescription(specialTypeLabel(type)+", "+(left>0?uses:"κλειδωμένο"));
            if(left>0)b.setOnClickListener(v->startSpecialQuestion(type));
            GridLayout.LayoutParams lp=new GridLayout.LayoutParams();
            lp.width=0;lp.height=dp(46);lp.columnSpec=GridLayout.spec(GridLayout.UNDEFINED,1f);
            lp.setMargins(dp(3),dp(3),dp(3),dp(3));grid.addView(b,lp);
        }
        return grid;
    }


    private String specialTypeLabel(String type) {
        if ("top5".equals(type)) return "TOP 5";
        if ("find3".equals(type)) return "ΒΡΕΣ 3";
        if ("person_id".equals(type)) return "PERSON ID";
        if ("country_id".equals(type)) return "COUNTRY ID";
        if ("rank".equals(type)) return "ΒΑΛ’ ΤΑ ΣΕ ΣΕΙΡΑ";
        if ("connection".equals(type)) return "ΒΡΕΣ ΤΗ ΣΥΝΔΕΣΗ";
        return "SPECIAL";
    }

    private String specialTypeIcon(String type) {
        if ("top5".equals(type)) return "🏆";
        if ("find3".equals(type)) return "◎";
        if ("person_id".equals(type)) return "👤";
        if ("country_id".equals(type)) return "⚑";
        if ("rank".equals(type)) return "↕";
        return "★";
    }

    private int specialTypeColor(String type) {
        // Every Special has its own stable visual identity.
        if ("top5".equals(type)) return YELLOW;
        if ("find3".equals(type)) return CYAN;
        if ("person_id".equals(type)) return PURPLE;
        if ("country_id".equals(type)) return TEAL;
        if ("rank".equals(type)) return ORANGE;
        return MAGENTA;
    }

    private int specialTypeSoft(String type) {
        if ("top5".equals(type)) return YELLOW_SOFT;
        if ("find3".equals(type)) return CYAN_SOFT;
        if ("person_id".equals(type)) return PURPLE_SOFT;
        if ("country_id".equals(type)) return TEAL_SOFT;
        if ("rank".equals(type)) return ORANGE_SOFT;
        return MAGENTA_SOFT;
    }

    private int specialMaxPoints(String type) {
        if ("person_id".equals(type) || "country_id".equals(type)) return 3;
        return 2;
    }

    private void startSpecialQuestion(String type) {
        if (!enabledSpecialTypes.contains(type) || countOf(specialPlaysUsed, type) >= 2) return;
        List<SpecialQuestion> candidates = new ArrayList<>();
        for (SpecialQuestion q : specialQuestions) {
            if (type.equals(q.type) && !usedSpecialQuestionIds.contains(q.id)) candidates.add(q);
        }
        if (candidates.isEmpty()) for (SpecialQuestion q : specialQuestions) if (type.equals(q.type)) candidates.add(q);
        if (candidates.isEmpty()) return;
        Collections.shuffle(candidates);
        SpecialQuestion q = candidates.get(0);
        activeSpecialId = q.id;
        activeSpecialCorrectCount = 0;
        activeSpecialMistakes = 0;
        activeSpecialHintIndex = 0;
        activeSpecialJokerSelected = activeJokerSelected;
        activeSpecialInteractionStarted = false;
        activeSpecialAnswersVisible = false;
        activeRankOrder.clear();
        if ("rank".equals(q.type)) {
            Collections.addAll(activeRankOrder, q.items);
            Collections.shuffle(activeRankOrder);
        }
        prepareQuestionTimerForNewQuestion();
        saveGameState();
        showQuestionTransition(specialTypeLabel(q.type), q.type, specialTypeColor(q.type), () -> openSpecialQuestion(q));
    }

    private void openSpecialQuestion(SpecialQuestion q) {
        if (q == null) { showBoard(); return; }
        activeSpecialId = q.id;
        settingsSource = ScreenSource.BOARD;
        saveGameState();

        LinearLayout root = baseColumn();
        root.addView(gameTopBar(false));
        root.addView(scoreStrip());

        TextView turn = pill("Σειρά: " + currentTeamName(), teamAccent(currentTeam), CARD, softOutline(teamAccent(currentTeam)));
        turn.setGravity(Gravity.CENTER);
        applyAdaptiveText(turn, 10, 12, true);
        root.addView(turn, centeredWrapMargins(0, 7, 0, 7));
        addQuestionTimer(root);

        int accent = specialTypeColor(q.type);
        TextView mode = pill(specialTypeIcon(q.type) + "  " + specialTypeLabel(q.type), accent, CARD, softOutline(accent));
        mode.setGravity(Gravity.CENTER);
        root.addView(mode, centeredWrapMargins(0, 0, 0, 8));

        TextView prompt = text(q.prompt, 21, TEXT, true);
        prompt.setGravity(Gravity.CENTER);
        prompt.setLineSpacing(dp(4), 1f);
        prompt.setPadding(dp(16), dp(18), dp(16), dp(18));
        prompt.setBackground(round(CARD, softOutline(accent), 1, radius()));
        prompt.setElevation(0);
        root.addView(prompt, fullMargins(4, 0, 4, 8));

        if (activeSpecialJokerSelected) {
            TextView doubled = pill("⚡ ΔΙΠΛΑΣΙΑΣΜΟΣ ΕΝΕΡΓΟΣ", modeAccent(GameMode.DUEL), CARD, BORDER);
            doubled.setGravity(Gravity.CENTER);
            root.addView(doubled, centeredWrapMargins(0,0,0,8));
        }

        if ("top5".equals(q.type)) renderTop5Special(root, q);
        else if ("find3".equals(q.type)) renderFind3Special(root, q);
        else if ("person_id".equals(q.type) || "country_id".equals(q.type)) renderIdSpecial(root, q);
        else if ("rank".equals(q.type)) renderRankSpecial(root, q);
        else if ("connection".equals(q.type)) renderConnectionSpecial(root, q);
        else renderMultipleSpecial(root, q);

        setStaticScreen(root);
    }

    private View specialJokerButton() {
        Button joker = button("", RED_SOFT, RED);
        joker.setMinHeight(dp(50));
        Runnable draw = () -> {
            if (activeSpecialInteractionStarted) {
                joker.setText(activeSpecialJokerSelected ? "⚡ ΔΙΠΛΑΣΙΑΣΜΟΣ ΚΛΕΙΔΩΜΕΝΟΣ" : "⚡ ΔΙΠΛΑΣΙΑΣΜΟΣ · ΔΕΝ ΕΝΕΡΓΟΠΟΙΗΘΗΚΕ");
                joker.setTextColor(activeSpecialJokerSelected ? Color.WHITE : MUTED);
                joker.setBackground(round(activeSpecialJokerSelected ? RED : CARD, activeSpecialJokerSelected ? RED : BORDER, 1, radius()));
                joker.setEnabled(false);
            } else if (activeSpecialJokerSelected) {
                joker.setText("⚡ ΔΙΠΛΑΣΙΑΣΜΟΣ ΕΝΕΡΓΟΣ");
                joker.setTextColor(Color.WHITE);
                joker.setBackground(round(RED, RED, 0, radius()));
                joker.setEnabled(true);
            } else {
                joker.setText("⚡ ΔΙΠΛΑΣΙΑΣΜΟΣ · ΜΙΑ ΦΟΡΑ");
                joker.setTextColor(RED);
                joker.setBackground(round(RED_SOFT, RED, 2, radius()));
                joker.setEnabled(true);
            }
        };
        draw.run();
        joker.setOnClickListener(v -> {
            if (activeSpecialInteractionStarted) return;
            activeSpecialJokerSelected = !activeSpecialJokerSelected;
            draw.run();
            saveGameState();
        });
        return joker;
    }

    private void renderTop5Special(LinearLayout root, SpecialQuestion q) {
        TextView host = text("ΕΚΦΩΝΗΤΗΣ", 12, YELLOW, true);
        host.setGravity(Gravity.CENTER);
        root.addView(host, fullMargins(4, 2, 4, 5));
        root.addView(specialHostKey(q, "ΔΕΣ ΤΙΣ 5 ΑΠΑΝΤΗΣΕΙΣ"), fullMargins(4, 0, 4, 10));


        int fivePoints = activeSpecialJokerSelected ? 4 : 2;
        int fourPoints = activeSpecialJokerSelected ? 2 : 1;

        Button allFive = primaryButton("✓  ΒΡΗΚΕ ΚΑΙ ΤΙΣ 5 · +" + fivePoints, GREEN);
        allFive.setOnClickListener(v -> {
            activeSpecialInteractionStarted = true;
            resolveSpecialQuestion(q, 2, true);
        });
        root.addView(allFive, fullMargins(4, 0, 4, 8));

        Button stoppedAtFour = button("4/5  ΣΤΑΜΑΤΗΣΕ · +" + fourPoints, YELLOW_SOFT, YELLOW);
        stoppedAtFour.setMinHeight(dp(54));
        stoppedAtFour.setBackground(round(YELLOW_SOFT, YELLOW, 2, radius()));
        stoppedAtFour.setOnClickListener(v -> {
            activeSpecialInteractionStarted = true;
            resolveSpecialQuestion(q, 1, true);
        });
        root.addView(stoppedAtFour, fullMargins(4, 0, 4, 8));

        Button lost = button("✕  ΕΧΑΣΕ · 0", RED_SOFT, RED);
        lost.setMinHeight(dp(54));
        lost.setBackground(round(RED_SOFT, RED, 2, radius()));
        lost.setOnClickListener(v -> {
            activeSpecialInteractionStarted = true;
            resolveSpecialQuestion(q, 0, false);
        });
        root.addView(lost, fullMargins(4, 0, 4, 8));
    }

    private void renderFind3Special(LinearLayout root, SpecialQuestion q) {
        root.addView(specialHostKey(q, "ΔΕΣ ΑΠΟΔΕΚΤΕΣ ΑΠΑΝΤΗΣΕΙΣ"), fullMargins(4, 2, 4, 10));
        TextView help = text("Αρκούν οποιεσδήποτε 3 σωστές από τη λίστα.", 13, MUTED, false);
        help.setGravity(Gravity.CENTER);
        root.addView(help, fullMargins(4, 0, 4, 10));
        Button won = primaryButton("✓  ΒΡΗΚΕ 3 · +" + (activeSpecialJokerSelected ? 4 : 2), GREEN);
        won.setOnClickListener(v -> { activeSpecialInteractionStarted = true; resolveSpecialQuestion(q, 2, true); });
        root.addView(won, fullMargins(4, 0, 4, 8));
        Button lost = button("✕  ΔΕΝ ΤΑ ΒΡΗΚΕ", RED_SOFT, RED);
        lost.setOnClickListener(v -> { activeSpecialInteractionStarted = true; resolveSpecialQuestion(q, 0, false); });
        root.addView(lost, fullMargins(4, 0, 4, 8));
    }

    private void renderIdSpecial(LinearLayout root, SpecialQuestion q) {
        if (!activeSpecialInteractionStarted) {
            TextView ready = text("Το πρώτο clue δεν έχει ανοίξει ακόμα. Αν θέλεις Διπλασιασμό, ενεργοποίησέ τον τώρα.", 13, MUTED, false);
            ready.setGravity(Gravity.CENTER);
            ready.setPadding(dp(12), dp(12), dp(12), dp(12));
            ready.setBackground(round(CARD, BORDER, 1, radius()));
            root.addView(ready, fullMargins(4, 2, 4, 9));
            Button start = primaryButton("ΑΝΟΙΞΕ ΠΡΩΤΟ CLUE", specialTypeColor(q.type));
            start.setOnClickListener(v -> {
                activeSpecialInteractionStarted = true;
                activeSpecialHintIndex = 0;
                saveGameState();
                openSpecialQuestion(q);
            });
            root.addView(start, fullMargins(4, 0, 4, 9));
            return;
        }

        LinearLayout hints = cardColumn();
        for (int i = 0; i <= activeSpecialHintIndex && i < q.hints.length; i++) {
            TextView h = text("CLUE " + (i + 1) + "\n" + q.hints[i], 15, TEXT, i == activeSpecialHintIndex);
            h.setLineSpacing(dp(3), 1f);
            hints.addView(h, fullMargins(0, i == 0 ? 0 : 7, 0, 0));
        }
        root.addView(hints, fullMargins(4, 0, 4, 8));
        root.addView(specialHostKey(q, "ΔΕΣ ΑΠΑΝΤΗΣΗ ΕΚΦΩΝΗΤΗ"), fullMargins(4, 0, 4, 8));

        int basePoints = Math.max(1, 3 - activeSpecialHintIndex);
        Button found = primaryButton("✓  ΤΟ ΒΡΗΚΕ · +" + (activeSpecialJokerSelected ? basePoints * 2 : basePoints), GREEN);
        found.setOnClickListener(v -> resolveSpecialQuestion(q, basePoints, true));
        root.addView(found, fullMargins(4, 0, 4, 7));
        if (activeSpecialHintIndex < q.hints.length - 1) {
            Button next = button("ΕΠΟΜΕΝΟ CLUE", specialTypeSoft(q.type), specialTypeColor(q.type));
            next.setOnClickListener(v -> {
                activeSpecialHintIndex++;
                saveGameState();
                openSpecialQuestion(q);
            });
            root.addView(next, fullMargins(4, 0, 4, 7));
        } else {
            Button fail = button("✕  ΔΕΝ ΤΟ ΒΡΗΚΕ", RED_SOFT, RED);
            fail.setOnClickListener(v -> resolveSpecialQuestion(q, 0, false));
            root.addView(fail, fullMargins(4, 0, 4, 7));
        }
    }

    private View specialHostKey(SpecialQuestion q, String closedLabel) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        Button toggle = button(activeSpecialAnswersVisible ? "ΚΡΥΨΕ ΑΠΑΝΤΗΣΕΙΣ" : closedLabel, CARD, MUTED);
        toggle.setBackground(round(CARD, BORDER, 1, radius()));
        box.addView(toggle);
        if (activeSpecialAnswersVisible) {
            StringBuilder value = new StringBuilder();
            if (q.answers.length > 0) {
                for (int i = 0; i < q.answers.length; i++) value.append(i + 1).append(". ").append(q.answers[i]).append(i + 1 < q.answers.length ? "\n" : "");
            } else if (q.explanation != null) value.append(q.explanation);
            TextView key = text(value.toString(), 13, TEXT, true);
            key.setPadding(dp(12), dp(10), dp(12), dp(10));
            key.setBackground(round(BG, BORDER, 1, radius()));
            box.addView(key, fullMargins(0, 6, 0, 0));
        }
        toggle.setOnClickListener(v -> {
            activeSpecialAnswersVisible = !activeSpecialAnswersVisible;
            saveGameState();
            openSpecialQuestion(q);
        });
        return box;
    }

    private void renderRankSpecial(LinearLayout root, SpecialQuestion q) {
        if (activeRankOrder.isEmpty()) {
            Collections.addAll(activeRankOrder, q.items);
            Collections.shuffle(activeRankOrder);
        }
        TextView help = text("Κράτησε ένα στοιχείο και σύρε το στη σωστή θέση.", 12, MUTED, false);
        help.setGravity(Gravity.CENTER);
        root.addView(help, fullMargins(4, 0, 4, 7));

        LinearLayout list = cardColumn();
        for (int i = 0; i < activeRankOrder.size(); i++) {
            final int targetIndex = i;
            LinearLayout row = new LinearLayout(this);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(7), dp(2), dp(7), dp(2));
            row.setBackground(round(CARD, BORDER, 1, 10));

            TextView handle = text("≡", 22, ORANGE, true);
            handle.setGravity(Gravity.CENTER);
            row.addView(handle, new LinearLayout.LayoutParams(dp(34), dp(46)));

            TextView n = text((i + 1) + ".", 15, ORANGE, true);
            n.setGravity(Gravity.CENTER_VERTICAL);
            row.addView(n, new LinearLayout.LayoutParams(dp(28), dp(46)));

            TextView item = text(activeRankOrder.get(i), 14, TEXT, true);
            item.setGravity(Gravity.CENTER_VERTICAL);
            item.setSingleLine(false);
            row.addView(item, new LinearLayout.LayoutParams(0, dp(46), 1));

            View.OnLongClickListener startDrag = v -> {
                activeSpecialInteractionStarted = true;
                ClipData data = ClipData.newPlainText("quizara_rank_index", String.valueOf(targetIndex));
                View.DragShadowBuilder shadow = new View.DragShadowBuilder(row);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) row.startDragAndDrop(data, shadow, targetIndex, 0);
                else row.startDrag(data, shadow, targetIndex, 0);
                if (hapticsEnabled) try { row.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS); } catch (Throwable ignored) { }
                return true;
            };
            row.setOnLongClickListener(startDrag);
            handle.setOnLongClickListener(startDrag);

            row.setOnDragListener((v, event) -> {
                switch (event.getAction()) {
                    case DragEvent.ACTION_DRAG_ENTERED:
                        v.setAlpha(0.62f);
                        return true;
                    case DragEvent.ACTION_DRAG_EXITED:
                        v.setAlpha(1f);
                        return true;
                    case DragEvent.ACTION_DROP:
                        v.setAlpha(1f);
                        Object state = event.getLocalState();
                        if (!(state instanceof Integer)) return false;
                        int from = (Integer) state;
                        int to = targetIndex;
                        if (from < 0 || from >= activeRankOrder.size() || to < 0 || to >= activeRankOrder.size()) return false;
                        if (from != to) {
                            String moved = activeRankOrder.remove(from);
                            int insertAt = Math.min(to, activeRankOrder.size());
                            activeRankOrder.add(insertAt, moved);
                            saveGameState();
                            openSpecialQuestion(q);
                        }
                        return true;
                    case DragEvent.ACTION_DRAG_ENDED:
                        v.setAlpha(1f);
                        return true;
                    default:
                        return true;
                }
            });

            list.addView(row, fullMargins(0, i == 0 ? 0 : 4, 0, 0));
        }
        root.addView(list, fullMargins(4, 0, 4, 8));
        Button check = primaryButton("ΕΛΕΓΧΟΣ ΣΕΙΡΑΣ", ORANGE);
        check.setOnClickListener(v -> {
            activeSpecialInteractionStarted = true;
            boolean correct = activeRankOrder.size() == q.correctOrder.length;
            if (correct) for (int i = 0; i < q.correctOrder.length; i++) if (!q.correctOrder[i].equals(activeRankOrder.get(i))) { correct = false; break; }
            resolveSpecialQuestion(q, correct ? 2 : 0, correct);
        });
        root.addView(check, fullMargins(4, 0, 4, 8));
    }

    private void renderMultipleSpecial(LinearLayout root, SpecialQuestion q) {
        for (int i = 0; i < q.options.length; i++) {
            final int idx = i;
            String letter = String.valueOf((char) ('A' + i));
            Button option = button(letter + "   " + q.options[i], CARD, TEXT);
            option.setGravity(Gravity.CENTER_VERTICAL);
            option.setMinHeight(dp(58));
            option.setBackground(round(CARD, BORDER, 1, radius()));
            option.setOnClickListener(v -> {
                activeSpecialInteractionStarted = true;
                boolean correct = idx == q.correctIndex;
                resolveSpecialQuestion(q, correct ? 1 : 0, correct);
            });
            root.addView(option, fullMargins(4, 3, 4, 3));
        }
    }

    private void resolveSpecialQuestion(SpecialQuestion q, int basePoints, boolean success) {
        clearQuestionTimerState();
        int answeredTeam = currentTeam;
        int points = basePoints * (activeSpecialJokerSelected ? 2 : 1);
        if (activeSpecialJokerSelected) consumeJoker(answeredTeam);
        if (success) {
            if (answeredTeam == 0) { scoreA += points; correctA++; maxHitA = Math.max(maxHitA, points); }
            else { scoreB += points; correctB++; maxHitB = Math.max(maxHitB, points); }
            playFeedback(true);
        } else {
            if (answeredTeam == 0) wrongA++; else wrongB++;
            playFeedback(false);
        }
        usedSpecialQuestionIds.add(q.id);
        specialPlaysUsed.put(q.type, countOf(specialPlaysUsed, q.type) + 1);
        answered++;
        currentTeam = 1 - currentTeam;
        activeSpecialId = "";
        activeSpecialCorrectCount = 0;
        activeSpecialMistakes = 0;
        activeSpecialHintIndex = 0;
        activeSpecialJokerSelected = false;
        activeSpecialInteractionStarted = false;
        activeSpecialAnswersVisible = false;
        activeRankOrder.clear();
        saveGameState();
        showSpecialFeedback(q, success, points, () -> finishAfterSpecial());
    }

    private void showSpecialFeedback(SpecialQuestion q, boolean success, int points, Runnable next) {
        LinearLayout root = baseColumn();
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.addView(topBar(false, false));
        int accent = success ? GREEN : RED;
        TextView icon = text(success ? "✓" : "✕", 56, accent, true);
        icon.setGravity(Gravity.CENTER);
        icon.setBackground(round(success ? GREEN_SOFT : RED_SOFT, accent, 2, 100));
        root.addView(icon, centeredSize(dp(108), dp(108), 0, 38, 0, 12));
        TextView title = text(success ? "SPECIAL ΚΕΡΔΗΘΗΚΕ" : "SPECIAL ΧΑΘΗΚΕ", 25, accent, true);
        title.setGravity(Gravity.CENTER);
        root.addView(title);
        TextView mode = text(specialTypeLabel(q.type), 16, TEXT, true);
        mode.setGravity(Gravity.CENTER);
        root.addView(mode, fullMargins(0, 5, 0, 8));
        TextView pts = text(success ? "+" + points + " πόντοι" : "0 πόντοι", 20, TEXT, true);
        pts.setGravity(Gravity.CENTER);
        root.addView(pts, fullMargins(0, 0, 0, 12));
        root.addView(feedbackAnswerCard(specialAnswerLabel(q), specialAnswerSummary(q), q.explanation),
                fullMargins(8, 0, 8, 0));
        root.addView(feedbackContinueButton(next), fullMargins(8, 18, 8, 4));
        setStaticScreen(root);
    }

    private String specialAnswerLabel(SpecialQuestion q) {
        if ("rank".equals(q.type)) return "Σωστή σειρά:";
        if ("top5".equals(q.type)) return "Οι 5 απαντήσεις:";
        if ("find3".equals(q.type)) return "Αποδεκτές απαντήσεις:";
        if ("connection".equals(q.type)) return "Η σύνδεση:";
        return "Απάντηση:";
    }

    private String specialAnswerSummary(SpecialQuestion q) {
        if ("rank".equals(q.type)) return numberedLines(q.correctOrder);
        if ("top5".equals(q.type)) return numberedLines(q.answers);
        // ΒΡΕΣ 3 lists can be long, so they wrap as one line instead of one item per row.
        if ("find3".equals(q.type)) return TextUtils.join(" · ", q.answers);
        return q.answers.length == 0 ? "—" : TextUtils.join(" / ", q.answers);
    }

    private String numberedLines(String[] items) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < items.length; i++) {
            if (i > 0) sb.append('\n');
            sb.append(i + 1).append(". ").append(items[i]);
        }
        return sb.toString();
    }

    private void finishAfterSpecial() {
        if (answered >= totalDuelQuestions()) { showResult(); return; }
        if (earlyFinishEnabled && scoreA != scoreB) {
            int leader = scoreA > scoreB ? 0 : 1;
            int trailer = 1 - leader;
            int leaderScore = leader == 0 ? scoreA : scoreB;
            if (maxReachForTeam(trailer, currentTeam) < leaderScore) { showWinner(leader, false, true); return; }
        }
        showBoard();
    }

    // ---------- LOADING TRANSITION ----------

    /** Every new question enters through the same compact category identity screen. */
    private void showQuestionTransition(String label, String id, int accent, Runnable next) {
        LinearLayout root = baseColumn();
        root.addView(topBar(false, false));
        LinearLayout center = new LinearLayout(this);
        center.setOrientation(LinearLayout.VERTICAL);
        center.setGravity(Gravity.CENTER);
        int visibleAccent = accent;
        boolean regular = false;
        for (Category c : CATEGORIES) if (c.id.equals(id)) regular = true;
        if (regular) {
            View glyph = categoryGlyph(id, visibleAccent, 88);
            center.addView(glyph, centeredSize(dp(88), dp(88), 0, 0, 0, 14));
        } else {
            String symbol = "☠";
            if ("top5".equals(id)) symbol = "★";
            else if ("find3".equals(id)) symbol = "◎";
            else if ("person_id".equals(id)) symbol = "♟";
            else if ("country_id".equals(id)) symbol = "⚑";
            else if ("rank".equals(id)) symbol = "↕";
            else if ("connection".equals(id)) symbol = "⌁";
            TextView glyph = text(symbol, 74, visibleAccent, true);
            glyph.setGravity(Gravity.CENTER);
            center.addView(glyph, centeredSize(dp(96), dp(100), 0, 0, 0, 12));
        }
        TextView title = text(label.toUpperCase(java.util.Locale.forLanguageTag("el")), 22, TEXT, true);
        title.setGravity(Gravity.CENTER);
        title.setSingleLine(true);
        applyAdaptiveText(title, 15, 22, true);
        center.addView(title);
        View smallLine = new View(this);
        smallLine.setBackgroundColor(softOutline(visibleAccent));
        center.addView(smallLine, centeredSize(dp(64), dp(2), 0, 18, 0, 0));
        root.addView(center, new LinearLayout.LayoutParams(-1, 0, 1));
        setStaticScreen(root);
        // Effects controls animation, not whether the loading identity appears.
        if (effectsEnabled) center.animate().alpha(1f).setDuration(160).start();
        handler.postDelayed(next, effectsEnabled ? 560 : 190);
    }

    private void showQuestionLoading(Question q, Category c) {
        if (q == null) return;
        activeQuestionKey = key(q.category, q.multiplier);
        activeQuestionRevealed = false;
        // Preserve the doubling choice from the board; it must not be selectable in-question.
        prepareQuestionTimerForNewQuestion();
        saveGameState();
        showQuestionTransition(c.name, c.id, categoryAccent(c), () -> openQuestion(q, c));
    }

    // ---------- QUESTION ----------

    private void openQuestion(Question q, Category c) {
        if (q == null) { showBoard(); return; }
        if (c == null) c = categoryById(q.category);
        activeQuestionKey = key(q.category, q.multiplier);
        settingsSource = ScreenSource.BOARD;
        saveGameState();

        LinearLayout root = baseColumn();
        root.addView(gameTopBar(false));
        root.addView(scoreStrip());

        TextView turn = pill("●  Σειρά: " + currentTeamName(), teamAccent(currentTeam), CARD, softOutline(teamAccent(currentTeam)));
        turn.setGravity(Gravity.CENTER);
        applyAdaptiveText(turn, 10, 12, true);
        root.addView(turn, centeredWrapMargins(0, 7, 0, 8));

        addQuestionTimer(root);

        LinearLayout meta = new LinearLayout(this);
        meta.setOrientation(LinearLayout.HORIZONTAL);
        meta.setGravity(Gravity.CENTER_VERTICAL);
        meta.setPadding(dp(10), dp(9), dp(10), dp(9));
        meta.setBackground(round(CARD, BORDER, 1, radius()));

        LinearLayout leftMeta = new LinearLayout(this);
        leftMeta.setOrientation(LinearLayout.VERTICAL);
        TextView catLabel = text("ΚΑΤΗΓΟΡΙΑ", 9, MUTED, true);
        TextView cat = text(c.name, 15, TEXT, true);
        cat.setSingleLine(true);
        leftMeta.addView(catLabel);
        leftMeta.addView(cat);
        meta.addView(leftMeta, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        LinearLayout rightMeta = new LinearLayout(this);
        rightMeta.setOrientation(LinearLayout.VERTICAL);
        rightMeta.setGravity(Gravity.END);
        TextView difLabel = text("ΔΥΣΚΟΛΙΑ", 9, MUTED, true);
        difLabel.setGravity(Gravity.END);
        TextView dif = text("×" + q.multiplier + "  " + difficultyName(q.multiplier), 14, difficultyColor(q.multiplier), true);
        dif.setGravity(Gravity.END);
        rightMeta.addView(difLabel);
        rightMeta.addView(dif);
        meta.addView(rightMeta, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        root.addView(meta, fullMargins(4, 0, 4, 10));

        LinearLayout questionCard = new LinearLayout(this);
        questionCard.setOrientation(LinearLayout.VERTICAL);
        questionCard.setPadding(dp(18), dp(20), dp(18), dp(18));
        questionCard.setGravity(Gravity.CENTER);
        questionCard.setMinimumHeight(dp(235));
        questionCard.setBackground(round(CARD, softOutline(categoryAccent(c)), 1, radius()));
        questionCard.setElevation(0);
        questionCard.setCameraDistance(dp(12000));

        TextView pointsText = text("", 13, MUTED, false);
        pointsText.setVisibility(View.GONE);
        final boolean[] answerRevealed = {activeQuestionRevealed};
        if (answerRevealed[0]) renderAnswerFace(questionCard, q, pointsText);
        else renderQuestionFace(questionCard, q, pointsText);

        questionCard.setOnClickListener(v -> {
            answerRevealed[0] = !answerRevealed[0];
            activeQuestionRevealed = answerRevealed[0];
            saveGameState();
            if (answerRevealed[0]) flipToAnswer(questionCard, q, pointsText);
            else flipToQuestion(questionCard, q, pointsText);
        });
        root.addView(questionCard, fullMargins(4, 0, 4, 10));

        // The doubling button is intentionally absent from the opened question.
        // It was selected (or left off) on the board before the loading transition.
        final boolean[] jokerActive = {activeJokerSelected};
        if (jokerActive[0]) {
            TextView doubled = pill("⚡ ΔΙΠΛΑΣΙΑΣΜΟΣ ΕΝΕΡΓΟΣ", modeAccent(GameMode.DUEL), CARD, BORDER);
            doubled.setGravity(Gravity.CENTER);
            root.addView(doubled, centeredWrapMargins(0,0,0,8));
        }

        TextView prompt = text("Η ομάδα το βρήκε;", 16, TEXT, true);
        prompt.setGravity(Gravity.CENTER);
        root.addView(prompt, fullMargins(0, 2, 0, 8));

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        Button correct = button("✓  ΣΩΣΤΟ", CARD, TEXT);
        correct.setBackground(round(CARD, softOutline(GREEN), 1, radius()));
        correct.setMinHeight(dp(56));
        Button wrong = button("✕  ΛΑΘΟΣ", CARD, TEXT);
        wrong.setBackground(round(CARD, softOutline(RED), 1, radius()));
        wrong.setMinHeight(dp(56));
        actions.addView(correct, weightedMargins(1, 4, 0, 4, 0));
        actions.addView(wrong, weightedMargins(1, 4, 4, 0, 0));
        root.addView(actions, fullMargins(0, 0, 0, 10));


        wrong.setOnClickListener(v -> resolveOpenAnswer(q, false, jokerActive[0]));
        correct.setOnClickListener(v -> resolveOpenAnswer(q, true, jokerActive[0]));
        setStaticScreen(root);
    }

    private void resolveOpenAnswer(Question q, boolean correct, boolean usedJoker) {
        String k = key(q.category, q.multiplier);
        if (used.contains(k)) return;

        clearQuestionTimerState();
        int answeredTeam = currentTeam;
        int points = correct ? q.multiplier * (usedJoker ? 2 : 1) : 0;
        if (usedJoker) consumeJoker(answeredTeam); // burned even on a wrong answer.

        if (correct) {
            if (answeredTeam == 0) {
                scoreA += points;
                correctA++;
                maxHitA = Math.max(maxHitA, points);
                incrementCategory(categoryCorrectA, q.category);
            } else {
                scoreB += points;
                correctB++;
                maxHitB = Math.max(maxHitB, points);
                incrementCategory(categoryCorrectB, q.category);
            }
            playFeedback(true);
        } else {
            if (answeredTeam == 0) {
                wrongA++;
            } else {
                wrongB++;
            }
            playFeedback(false);
        }

        used.add(k);
        answered++;
        currentTeam = 1 - currentTeam;
        activeQuestionKey = "";
        activeQuestionRevealed = false;
        activeJokerSelected = false;
        saveGameState();

        // Always shown (even with Effects off): it is where players read the correct answer.
        showAnswerFeedback(q, correct, points, usedJoker, () -> finishAfterAnswer(answeredTeam, correct));
    }

    private void finishAfterAnswer(int answeredTeam, boolean correct) {
        if (answered >= totalDuelQuestions()) {
            showResult();
            return;
        }

        if (earlyFinishEnabled && scoreA != scoreB) {
            int leader = scoreA > scoreB ? 0 : 1;
            int trailer = 1 - leader;
            int leaderScore = leader == 0 ? scoreA : scoreB;
            int trailerMax = maxReachForTeam(trailer, currentTeam);
            if (trailerMax < leaderScore) {
                showWinner(leader, false, true);
                return;
            }
        }

        showBoard();
    }

    /**
     * Best-case mathematical ceiling for a team from the CURRENT remaining board.
     * On target-team turns we give it the highest remaining question; on opponent
     * turns we assume the opponent removes the lowest remaining question and scores 0.
     * If the target still owns its ONE red x2 joker, we double the best question it
     * can still receive. This is deliberately the most favorable possible scenario
     * for the trailing team, so early finish can never fire too soon.
     */
    private int maxReachForTeam(int targetTeam, int nextTeam) {
        ArrayList<Integer> pool = new ArrayList<>();
        for (Question q : questions) {
            if (!used.contains(key(q.category, q.multiplier))) pool.add(q.multiplier);
        }
        for (String type : enabledSpecialTypes) {
            int remaining = Math.max(0, 2 - countOf(specialPlaysUsed, type));
            for (int i = 0; i < remaining; i++) pool.add(specialMaxPoints(type));
        }
        Collections.sort(pool);
        int future = 0;
        int bestTargetQuestion = 0;
        int turn = nextTeam;
        while (!pool.isEmpty()) {
            if (turn == targetTeam) {
                int value = pool.remove(pool.size() - 1);
                future += value;
                bestTargetQuestion = Math.max(bestTargetQuestion, value);
            } else {
                pool.remove(0); // most favorable removal for the target team
            }
            turn = 1 - turn;
        }
        if (isJokerAvailable(targetTeam) && bestTargetQuestion > 0) {
            future += bestTargetQuestion; // x2 adds one extra copy of that question's value
        }
        return (targetTeam == 0 ? scoreA : scoreB) + future;
    }

    // ---------- FEEDBACK / MOMENTUM ----------

    private void showAnswerFeedback(Question q, boolean correct, int points, boolean usedJoker, Runnable next) {
        LinearLayout root = baseColumn();
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.addView(topBar(false, false));

        TextView spacer = new TextView(this);
        root.addView(spacer, new LinearLayout.LayoutParams(1, dp(34)));

        String mark = correct ? "✓" : "✕";
        int accent = correct ? GREEN : RED;
        int soft = correct ? GREEN_SOFT : RED_SOFT;
        TextView icon = text(mark, 58, accent, true);
        icon.setGravity(Gravity.CENTER);
        icon.setBackground(round(soft, accent, 2, 100));
        root.addView(icon, centeredSize(dp(112), dp(112), 0, 8, 0, 14));

        TextView title = text(correct ? "ΣΩΣΤΟ!" : "ΛΑΘΟΣ!", 31, accent, true);
        title.setGravity(Gravity.CENTER);
        root.addView(title, fullMargins(0, 0, 0, 8));

        if (correct) {
            String msg = "+" + points + " πόντοι" + (usedJoker ? " · ΔΙΠΛΑΣΙΑΣΜΟΣ" : "");
            TextView pts = text(msg, 19, TEXT, true);
            pts.setGravity(Gravity.CENTER);
            root.addView(pts, fullMargins(0, 0, 0, 10));
        }
        root.addView(feedbackAnswerCard(correct ? "Σωστή απάντηση:" : "Η σωστή απάντηση ήταν:",
                q.correctAnswer(), q.explanation), fullMargins(8, 0, 8, 10));

        if (effectsEnabled) {
            TextView confetti = text(correct ? "✦  ✦  ✦" : "•  •  •", 21, accent, true);
            confetti.setGravity(Gravity.CENTER);
            root.addView(confetti);
        }

        root.addView(feedbackContinueButton(next), fullMargins(8, 18, 8, 4));
        setStaticScreen(root);
    }

    // Answer box for the Duel result screens: label, the answer(s), then the explanation if any.
    private View feedbackAnswerCard(String label, String answer, String explanation) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(14), dp(12), dp(14), dp(12));
        box.setBackground(round(CARD, BORDER, 1, radius()));
        TextView head = text(label, 13, MUTED, true);
        head.setGravity(Gravity.CENTER);
        box.addView(head);
        TextView value = text(answer, 16, TEXT, true);
        value.setGravity(Gravity.CENTER);
        value.setLineSpacing(dp(3), 1f);
        box.addView(value, fullMargins(0, 4, 0, 0));
        if (explanation != null && !explanation.isEmpty()) {
            TextView exp = text(explanation, 13, MUTED, false);
            exp.setGravity(Gravity.CENTER);
            box.addView(exp, fullMargins(0, 8, 0, 0));
        }
        return box;
    }

    // Duel result screens wait for the players instead of closing on a timer.
    private Button feedbackContinueButton(Runnable next) {
        Button b = primaryButton("ΣΥΝΕΧΕΙΑ  →", modeAccent(gameMode));
        b.setOnClickListener(v -> {
            v.setEnabled(false); // a double tap must not advance twice
            next.run();
        });
        return b;
    }

    private void showFinalQuestionIntro() {
        LinearLayout root = baseColumn();
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.addView(topBar(false, false));
        TextView swords = text("⚔", 68, PURPLE, false);
        swords.setGravity(Gravity.CENTER);
        root.addView(swords, fullMargins(0, 54, 0, 12));
        TextView title = text("ΤΕΛΕΥΤΑΙΑ ΕΡΩΤΗΣΗ!", 28, PURPLE, true);
        title.setGravity(Gravity.CENTER);
        root.addView(title, fullMargins(0, 0, 0, 8));
        TextView sub = text("Όλα θα κριθούν εδώ.", 17, TEXT, true);
        sub.setGravity(Gravity.CENTER);
        root.addView(sub);
        setStaticScreen(root);
        handler.postDelayed(this::showBoard, 900);
    }

    private void showTieChoice() {
        tieChoicePending = true;
        saveGameState();

        LinearLayout root = baseColumn();
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.addView(topBar(false, false));

        TextView equality = text("=", 58, PURPLE, true);
        equality.setGravity(Gravity.CENTER);
        equality.setBackground(round(PURPLE_SOFT, PURPLE, 2, 100));
        root.addView(equality, centeredSize(dp(108), dp(108), 0, 28, 0, 10));

        TextView title = text("ΙΣΟΠΑΛΙΑ!", 30, TEXT, true);
        title.setGravity(Gravity.CENTER);
        root.addView(title);

        String result = gameMode == GameMode.CLASSIC
                ? teamA + "  " + classicTotal(0) + "/" + classicGoalTotal() + "   —   " + classicTotal(1) + "/" + classicGoalTotal() + "  " + teamB
                : teamA + "  " + scoreA + "   —   " + scoreB + "  " + teamB;
        TextView finalScore = text(result, 19, TEXT, true);
        finalScore.setGravity(Gravity.CENTER);
        root.addView(finalScore, fullMargins(0, 6, 0, 4));

        TextView ask = text("Οι κανονικές ερωτήσεις τελείωσαν ισόπαλες. Τι θέλεις να γίνει τώρα;", 16, MUTED, false);
        ask.setGravity(Gravity.CENTER);
        root.addView(ask, fullMargins(8, 0, 8, 18));

        Button sudden = button("", CARD, TEXT);
        styleTieRuleButton(sudden, TieRule.SUDDEN_DEATH, false);
        sudden.setMinHeight(dp(104));
        sudden.setOnClickListener(v -> {
            tieRule = TieRule.SUDDEN_DEATH;
            tieChoicePending = false;
            inSuddenDeath = true;
            suddenStarted = false;
            suddenIndex = 0;
            suddenRound = 1;
            suddenTeam = 0;
            suddenResultA = null;
            suddenResultB = null;
            suddenMode = "";
            activeSuddenQuestionId = "";
            suddenUsedQuestionIds.clear();
            saveGameState();
            showSuddenDeathIntro();
        });
        root.addView(sudden, fullMargins(8, 0, 8, 12));

        Button draw = button("", CARD, TEXT);
        styleTieRuleButton(draw, TieRule.DRAW, false);
        draw.setMinHeight(dp(104));
        draw.setOnClickListener(v -> {
            tieRule = TieRule.DRAW;
            tieChoicePending = false;
            saveGameState();
            showDrawResult();
        });
        root.addView(draw, fullMargins(8, 0, 8, 24));

        setStaticScreen(root);
    }

    private void styleTieRuleButton(Button b, TieRule rule, boolean selected) {
        boolean sudden = rule == TieRule.SUDDEN_DEATH;
        int accent = (sudden ? RED : GREEN);
        int soft = (sudden ? RED_SOFT : GREEN_SOFT);
        String title = sudden ? "☠  ΞΑΦΝΙΚΟΣ ΘΑΝΑΤΟΣ" : "ΙΣΟΠΑΛΙΑ";
        String detail = sudden ? "Tie-break μέχρι να υπάρξει νικητής" : "Τέλος τώρα · χωρίς νικητή";
        b.setText((selected ? "✓  " : "") + title + "\n" + detail);
        b.setTextSize(13);
        b.setTextColor(accent);
        b.setBackground(round(selected ? soft : CARD, accent, selected ? 2 : 1, radius()));
    }

    private void showDrawResult() {
        gameActive = false;
        clearSavedGame();
        LinearLayout root = baseColumn();
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.addView(topBar(false, false));

        if (hapticsEnabled) {
            try { getWindow().getDecorView().performHapticFeedback(HapticFeedbackConstants.LONG_PRESS); } catch (Throwable ignored) { }
        }

        TextView hands = text("🤝", 72, GREEN, false);
        hands.setGravity(Gravity.CENTER);
        root.addView(hands, fullMargins(0, 34, 0, 8));

        TextView title = text("ΙΣΟΠΑΛΙΑ", 30, TEXT, true);
        title.setGravity(Gravity.CENTER);
        root.addView(title);

        String result = gameMode == GameMode.CLASSIC
                ? teamA + "  " + classicTotal(0) + "/" + classicGoalTotal() + "   —   " + classicTotal(1) + "/" + classicGoalTotal() + "  " + teamB
                : teamA + "  " + scoreA + "   —   " + scoreB + "  " + teamB;
        TextView finalScore = text(result, 19, TEXT, true);
        finalScore.setGravity(Gravity.CENTER);
        root.addView(finalScore, fullMargins(0, 8, 0, 8));

        TextView note = text("Η παρτίδα ολοκληρώθηκε χωρίς νικητή.", 14, MUTED, false);
        note.setGravity(Gravity.CENTER);
        root.addView(note, fullMargins(0, 0, 0, 18));

        if (gameMode == GameMode.CLASSIC) {
            LinearLayout progress = new LinearLayout(this);
            progress.setOrientation(LinearLayout.HORIZONTAL);
            progress.addView(classicProgressPanel(0), new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
            progress.addView(classicProgressPanel(1), new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
            root.addView(progress, fullMargins(4, 0, 4, 18));
        }

        Button again = primaryButton("ΠΑΙΞΕ ΞΑΝΑ", GREEN);
        again.setOnClickListener(v -> {
            resetTeamNamesToDefault();
            if (gameMode == GameMode.CLASSIC) showClassicDurationSelect(); else showSetup();
        });
        root.addView(again, fullMargins(8, 0, 8, 10));

        Button menu = button("Κεντρικό μενού", CARD, TEXT);
        menu.setBackground(round(CARD, BORDER, 1, radius()));
        menu.setOnClickListener(v -> showHome());
        root.addView(menu, fullMargins(8, 0, 8, 24));
        setStaticScreen(root);
    }

    // ---------- RESULT / SUDDEN DEATH ----------

    private void showResult() {
        if (scoreA == scoreB) {
            showTieChoice();
            return;
        }
        showWinner(scoreA > scoreB ? 0 : 1, false, false);
    }

    private void showWinner(int winnerTeam, boolean suddenDeath, boolean earlyFinish) {
        showUnifiedWinner(winnerTeam);
    }

    /** One winner design for Duel, Classic, and Four Cheeses. */
    private void showUnifiedWinner(int team) {
        clearSavedGame();
        applyTheme(prefs.getString("theme", THEME_LIGHT));
        playVictoryMusic();
        LinearLayout root=baseColumn();
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(dp(16),dp(10),dp(16),dp(12));
        FrameLayout artwork = new FrameLayout(this);
        ImageView hero=new ImageView(this);
        hero.setImageResource(isDarkTheme() ? R.drawable.poster_winner_dark : R.drawable.poster_winner_light);
        hero.setScaleType(ImageView.ScaleType.FIT_CENTER);
        hero.setAdjustViewBounds(true);
        hero.setContentDescription("Ο Σκεπτόμενος σηκώνει τη γροθιά του");
        root.setTag("winner_full_height");
        hero.setScaleType(ImageView.ScaleType.FIT_CENTER);
        hero.setAdjustViewBounds(false);
        artwork.addView(hero, new FrameLayout.LayoutParams(-1, -1));
        View back = backArrowButton(true);
        back.setOnClickListener(v -> showHome());
        FrameLayout.LayoutParams backParams = new FrameLayout.LayoutParams(dp(48), dp(48), Gravity.TOP | Gravity.START);
        artwork.addView(back, backParams);
        root.addView(artwork,new LinearLayout.LayoutParams(-1,0,1f));
        TextView winner=text("ΝΙΚΗΤΗΣ",34,TEXT,true);
        winner.setTypeface(brandTypeface());winner.setGravity(Gravity.CENTER);
        root.addView(winner,fullMargins(0,6,0,0));
        TextView name=text(team==0?teamA:teamB,30,teamAccent(team),true);
        name.setGravity(Gravity.CENTER);applyAdaptiveText(name,16,30,true);
        root.addView(name,fullMargins(0,3,0,15));
        Button replay=modeStartButton(gameMode);
        replay.setText("ΠΑΙΞΕ ΞΑΝΑ  →");
        replay.setOnClickListener(v->{
            resetTeamNamesToDefault();
            if(gameMode==GameMode.CHEESE)showCheeseSetup();
            else if(gameMode==GameMode.CLASSIC){classicTargetPerCategory=2;showClassicSetup();}
            else showSetup();
        });
        root.addView(replay,fullMargins(4,0,4,12));
        setStaticScreen(root);
    }

    private static final String[] SUDDEN_MODES = new String[]{"tf", "multiple", "over_under", "ab", "closest"};

    private void showSuddenDeathIntro() {
        applyTheme(prefs.getString("theme", THEME_LIGHT));
        if (!inSuddenDeath) {
            inSuddenDeath = true;
            suddenStarted = false;
            suddenIndex = 0;
            suddenRound = 1;
            suddenTeam = 0;
            suddenResultA = null;
            suddenResultB = null;
            suddenMode = "";
            activeSuddenQuestionId = "";
            suddenUsedQuestionIds.clear();
            saveGameState();
        }

        LinearLayout root = baseColumn();
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.addView(gameTopBar(false));
        TextView bolt = text("☠", 62, TEXT, false);
        bolt.setGravity(Gravity.CENTER);
        root.addView(bolt, fullMargins(0, 28, 0, 8));
        TextView title = text("ΞΑΦΝΙΚΟΣ ΘΑΝΑΤΟΣ", 28, TEXT, true);
        title.setGravity(Gravity.CENTER);
        root.addView(title);
        TextView tied = text("Ισοπαλία  " + scoreA + " — " + scoreB, 18, PURPLE, true);
        tied.setGravity(Gravity.CENTER);
        root.addView(tied, fullMargins(0, 4, 0, 14));


        Button start = primaryButton("ΣΤΗ ΡΟΥΛΕΤΑ", PURPLE);
        start.setOnClickListener(v -> {
            suddenStarted = true;
            suddenMode = "";
            activeSuddenQuestionId = "";
            suddenQuestionRevealed = false;
            clearQuestionTimerState();
            saveGameState();
            showSuddenRoulette();
        });
        root.addView(start, fullMargins(4, 0, 4, 24));
        setStaticScreen(root);
    }

    private void showSuddenRoulette() {
        applyTheme(prefs.getString("theme", THEME_LIGHT));
        suddenStarted = true;
        suddenMode = "";
        activeSuddenQuestionId = "";
        suddenClosestInputA = "";
        suddenClosestInputB = "";
        suddenClosestResolved = false;
        suddenClosestValueA = 0d;
        suddenClosestValueB = 0d;
        suddenQuestionRevealed = false;
        clearQuestionTimerState();
        saveGameState();

        LinearLayout root = baseColumn();
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.addView(gameTopBar(false));
        root.addView(gameMode == GameMode.CHEESE ? cheeseScores() : (gameMode == GameMode.CLASSIC ? classicCompactScoreStrip() : scoreStrip()), fullMargins(4, 0, 4, 8));

        TextView round = pill("Ξαφνικός Θάνατος · Γύρος " + suddenRound, PURPLE, PURPLE_SOFT, PURPLE);
        round.setGravity(Gravity.CENTER);
        root.addView(round, centeredWrapMargins(0, 6, 0, 10));

        TextView title = text("☠  ΞΑΦΝΙΚΟΣ ΘΑΝΑΤΟΣ", 20, TEXT, true);
        title.setGravity(Gravity.CENTER);
        root.addView(title, fullMargins(0, 0, 0, 8));

        RouletteView wheel = new RouletteView();
        root.addView(wheel, centeredSize(dp(286), dp(286), 0, 0, 0, 14));

        Button spin = primaryButton("ΓΥΡΙΣΕ ΤΗ ΡΟΥΛΕΤΑ", PURPLE);
        spin.setMinHeight(dp(58));
        spin.setOnClickListener(v -> {
            spin.setEnabled(false);
            spin.setText("ΓΥΡΙΖΕΙ…");
            int index = new Random().nextInt(SUDDEN_MODES.length);
            wheel.spinTo(index, () -> {
                suddenMode = SUDDEN_MODES[index];
                suddenResultA = null;
                suddenResultB = null;
                // The starting team alternates between rounds to avoid a fixed order advantage.
                suddenTeam = (suddenRound - 1) % 2;
                activeSuddenQuestionId = "";
                saveGameState();
                spin.setText("ΔΕΣ ΤΟ ΑΠΟΤΕΛΕΣΜΑ  →");
                spin.setEnabled(true);
                spin.setOnClickListener(t -> showSuddenModeReveal(suddenMode));
            });
        });
        root.addView(spin, fullMargins(8, 0, 8, 24));
        setStaticScreen(root);
    }

    private void showSuddenModeReveal(String mode) {
        LinearLayout root = baseColumn();
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.addView(gameTopBar(false));
        int accent = suddenModeColor(mode);
        TextView icon = text(suddenModeIcon(mode), 62, accent, true);
        icon.setGravity(Gravity.CENTER);
        icon.setBackground(round(suddenModeSoft(mode), accent, 2, 100));
        root.addView(icon, centeredSize(dp(118), dp(118), 0, 48, 0, 14));
        TextView picked = text("ΚΛΗΡΩΘΗΚΕ", 12, MUTED, true);
        picked.setGravity(Gravity.CENTER);
        root.addView(picked, fullMargins(0, 0, 0, 5));
        LinearLayout pickedTitleRow = new LinearLayout(this);
        pickedTitleRow.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = text(suddenModeLabel(mode), 28, accent, true);
        title.setGravity(Gravity.CENTER);
        pickedTitleRow.addView(title, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        Button modeInfoButton = infoButton(suddenModeLabel(mode), suddenModeInfo(mode), accent);
        pickedTitleRow.addView(modeInfoButton, new LinearLayout.LayoutParams(dp(40), dp(40)));
        root.addView(pickedTitleRow, fullMargins(22, 0, 22, 8));
        Button next=primaryButton("ΣΥΝΕΧΕΙΑ  →",modeAccent(gameMode));
        next.setOnClickListener(v->startCurrentSuddenRound());
        root.addView(next,fullMargins(8,18,8,22));
        setStaticScreen(root);
    }

    private void startCurrentSuddenRound() {
        if ("closest".equals(suddenMode)) showClosestSudden();
        else showSuddenQuestion();
    }

    private String suddenModeLabel(String mode) {
        if ("tf".equals(mode)) return "ΣΩΣΤΟ / ΛΑΘΟΣ";
        if ("multiple".equals(mode)) return "ΠΟΛΛΑΠΛΗΣ ΕΠΙΛΟΓΗΣ";
        if ("over_under".equals(mode)) return "OVER / UNDER";
        if ("ab".equals(mode)) return "ΔΙΑΛΕΞΕ ΤΟ ΣΩΣΤΟ";
        return "ΠΙΟ ΚΟΝΤΑ ΚΕΡΔΙΖΕΙ";
    }

    private String suddenModeWheelLabel(String mode) {
        if ("tf".equals(mode)) return "ΣΩΣΤΟ\nΛΑΘΟΣ";
        if ("multiple".equals(mode)) return "ΠΟΛΛΑΠΛΗΣ\nΕΠΙΛΟΓΗΣ";
        if ("over_under".equals(mode)) return "OVER\nUNDER";
        if ("ab".equals(mode)) return "ΔΙΑΛΕΞΕ\nΤΟ ΣΩΣΤΟ";
        return "ΠΙΟ ΚΟΝΤΑ\nΚΕΡΔΙΖΕΙ";
    }

    private String suddenModeIcon(String mode) {
        if ("tf".equals(mode)) return "✓✕";
        if ("multiple".equals(mode)) return "☑";
        if ("over_under".equals(mode)) return "↕";
        if ("ab".equals(mode)) return "⇄";
        return "◎";
    }

    private int suddenModeColor(String mode) {
        if ("tf".equals(mode)) return GREEN;
        if ("multiple".equals(mode)) return PURPLE;
        if ("over_under".equals(mode)) return BLUE;
        if ("ab".equals(mode)) return ORANGE;
        return RED;
    }

    private int suddenModeSoft(String mode) {
        if ("tf".equals(mode)) return GREEN_SOFT;
        if ("multiple".equals(mode)) return PURPLE_SOFT;
        if ("over_under".equals(mode)) return BLUE_SOFT;
        if ("ab".equals(mode)) return ORANGE_SOFT;
        return RED_SOFT;
    }

    private SuddenQuestion findSuddenQuestion(String id) {
        if (id == null || id.isEmpty()) return null;
        for (SuddenQuestion q : suddenQuestions) if (id.equals(q.id)) return q;
        return null;
    }

    private SuddenQuestion pickSuddenQuestion(String type) {
        List<SuddenQuestion> candidates = new ArrayList<>();
        for (SuddenQuestion q : suddenQuestions) if (type.equals(q.type) && !suddenUsedQuestionIds.contains(q.id)) candidates.add(q);
        if (candidates.isEmpty()) for (SuddenQuestion q : suddenQuestions) if (type.equals(q.type)) candidates.add(q);
        if (candidates.isEmpty()) return null;
        Collections.shuffle(candidates);
        return candidates.get(0);
    }

    private void showSuddenQuestion() {
        if (suddenMode == null || suddenMode.isEmpty()) { showSuddenRoulette(); return; }
        if ("closest".equals(suddenMode)) { showClosestSudden(); return; }
        SuddenQuestion q = findSuddenQuestion(activeSuddenQuestionId);
        if (q == null || !suddenMode.equals(q.type)) {
            q = pickSuddenQuestion(suddenMode);
            if (q == null) { showSuddenRoulette(); return; }
            activeSuddenQuestionId = q.id;
            prepareQuestionTimerForNewQuestion();
            saveGameState();
            showQuestionTransition("Ξαφνικός Θάνατος", "sudden", TEXT, this::showSuddenQuestion);
            return;
        }
        final SuddenQuestion current = q;
        String playingTeam = suddenTeam == 0 ? teamA : teamB;
        int teamAccent = teamAccent(suddenTeam);
        int accent = suddenModeColor(suddenMode);

        LinearLayout root = baseColumn();
        root.addView(gameTopBar(false));
        root.addView(gameMode == GameMode.CHEESE ? cheeseScores() : (gameMode == GameMode.CLASSIC ? classicCompactScoreStrip() : scoreStrip()), fullMargins(4, 0, 4, 8));
        TextView round = pill("Γύρος " + suddenRound + " · " + suddenModeLabel(suddenMode), accent, suddenModeSoft(suddenMode), accent);
        round.setGravity(Gravity.CENTER);
        root.addView(round, centeredWrapMargins(0, 4, 0, 7));
        TextView turn = pill("Σειρά: " + playingTeam, teamAccent, CARD, teamAccent);
        turn.setGravity(Gravity.CENTER);
        applyAdaptiveText(turn, 10, 13, true);
        root.addView(turn, centeredWrapMargins(0, 0, 0, 8));
        addQuestionTimer(root);

        TextView prompt = text(current.prompt, 22, TEXT, true);
        prompt.setGravity(Gravity.CENTER);
        prompt.setLineSpacing(dp(4), 1f);
        prompt.setPadding(dp(16), dp(24), dp(16), dp(24));
        prompt.setBackground(round(CARD, softOutline(accent), 1, radius()));
        prompt.setElevation(0);
        root.addView(prompt, fullMargins(4, 0, 4, 12));

        for (int i = 0; i < current.options.length; i++) {
            final int idx = i;
            Button option = button(current.options[i], CARD, TEXT);
            option.setMinHeight(dp(current.options.length > 2 ? 58 : 68));
            option.setTextSize(current.options.length > 2 ? 14 : 16);
            option.setBackground(round(CARD, accent, 2, radius()));
            option.setOnClickListener(v -> resolveSuddenChoice(current, idx));
            root.addView(option, fullMargins(5, 4, 5, 4));
        }
        setStaticScreen(root);
    }

    private void resolveSuddenChoice(SuddenQuestion q, int selectedIndex) {
        clearQuestionTimerState();
        boolean correct = selectedIndex == q.correctIndex;
        int answeringTeam = suddenTeam;
        if (answeringTeam == 0) suddenResultA = correct; else suddenResultB = correct;
        suddenUsedQuestionIds.add(q.id);
        activeSuddenQuestionId = "";
        suddenQuestionRevealed = false;
        if (suddenResultA == null || suddenResultB == null) suddenTeam = 1 - answeringTeam;
        playFeedback(correct);
        saveGameState();
        showSuddenAnswerFeedback(q, selectedIndex, correct, () -> continueSuddenAfterIndividualAnswer(answeringTeam));
    }

    private void showSuddenAnswerFeedback(SuddenQuestion q, int selectedIndex, boolean correct, Runnable next) {
        LinearLayout root = baseColumn();
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.addView(gameTopBar(false));
        int accent = correct ? GREEN : RED;
        TextView icon = text(correct ? "✓" : "✕", 56, accent, true);
        icon.setGravity(Gravity.CENTER);
        icon.setBackground(round(correct ? GREEN_SOFT : RED_SOFT, accent, 2, 100));
        root.addView(icon, centeredSize(dp(108), dp(108), 0, 40, 0, 12));
        TextView title = text(correct ? "ΣΩΣΤΟ" : "ΛΑΘΟΣ", 27, accent, true);
        title.setGravity(Gravity.CENTER);
        root.addView(title);
        if (q.correctIndex >= 0 && q.correctIndex < q.options.length) {
            TextView answer = text("Σωστή επιλογή: " + q.options[q.correctIndex], 15, TEXT, true);
            answer.setGravity(Gravity.CENTER);
            root.addView(answer, fullMargins(8, 8, 8, 5));
        }
        if (!q.explanation.isEmpty()) {
            TextView ex = text(q.explanation, 12, MUTED, false);
            ex.setGravity(Gravity.CENTER);
            root.addView(ex, fullMargins(10, 0, 10, 0));
        }
        Button continueButton=primaryButton("ΣΥΝΕΧΕΙΑ  →",modeAccent(gameMode));
        continueButton.setOnClickListener(v->next.run());
        root.addView(continueButton,fullMargins(8,24,8,24));
        setStaticScreen(root);
    }

    private void continueSuddenAfterIndividualAnswer(int answeredTeam) {
        if (suddenResultA == null || suddenResultB == null) {
            suddenTeam = 1 - answeredTeam;
            prepareQuestionTimerForNewQuestion();
            saveGameState();
            showSuddenQuestion();
            return;
        }
        if (Boolean.TRUE.equals(suddenResultA) != Boolean.TRUE.equals(suddenResultB)) {
            int winner = Boolean.TRUE.equals(suddenResultA) ? 0 : 1;
            showWinner(winner, true, false);
            return;
        }
        advanceSuddenWithoutWinner();
    }

    private void advanceSuddenWithoutWinner() {
        suddenRound++;
        suddenMode = "";
        activeSuddenQuestionId = "";
        suddenClosestResolved = false;
        suddenClosestInputA = "";
        suddenClosestInputB = "";
        suddenClosestValueA = 0d;
        suddenClosestValueB = 0d;
        suddenResultA = null;
        suddenResultB = null;
        suddenQuestionRevealed = false;
        clearQuestionTimerState();
        saveGameState();

        LinearLayout root = baseColumn();
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.addView(gameTopBar(false));
        TextView equal = text("=", 62, PURPLE, true);
        equal.setGravity(Gravity.CENTER);
        equal.setBackground(round(PURPLE_SOFT, PURPLE, 2, 100));
        root.addView(equal, centeredSize(dp(112), dp(112), 0, 52, 0, 12));
        TextView title = text("ΚΑΝΕΝΑΣ ΝΙΚΗΤΗΣ", 24, TEXT, true);
        title.setGravity(Gravity.CENTER);
        root.addView(title);
        TextView sub = text("Πάλι στη ρουλέτα.", 17, PURPLE, true);
        sub.setGravity(Gravity.CENTER);
        root.addView(sub, fullMargins(0, 8, 0, 0));
        Button next=primaryButton("ΣΥΝΕΧΕΙΑ  →",modeAccent(gameMode));
        next.setOnClickListener(v->showSuddenRoulette());
        root.addView(next,fullMargins(8,20,8,24));
        setStaticScreen(root);
    }

    private void showClosestSudden() {
        if (!"closest".equals(suddenMode)) { showSuddenRoulette(); return; }
        SuddenQuestion q = findSuddenQuestion(activeSuddenQuestionId);
        if (q == null || !"closest".equals(q.type)) {
            q = pickSuddenQuestion("closest");
            if (q == null) { showSuddenRoulette(); return; }
            activeSuddenQuestionId = q.id;
            prepareQuestionTimerForNewQuestion();
            saveGameState();
            showQuestionTransition("Ξαφνικός Θάνατος", "sudden", TEXT, this::showClosestSudden);
            return;
        }
        final SuddenQuestion current = q;
        if (suddenClosestResolved) {
            double diffA = Math.abs(suddenClosestValueA - current.numericAnswer);
            double diffB = Math.abs(suddenClosestValueB - current.numericAnswer);
            showClosestResult(current, suddenClosestValueA, suddenClosestValueB, diffA, diffB);
            return;
        }
        LinearLayout root = baseColumn();
        root.addView(gameTopBar(false));
        root.addView(gameMode == GameMode.CHEESE ? cheeseScores() : (gameMode == GameMode.CLASSIC ? classicCompactScoreStrip() : scoreStrip()), fullMargins(4, 0, 4, 8));
        TextView round = pill("Γύρος " + suddenRound + " · ΠΙΟ ΚΟΝΤΑ", PURPLE, PURPLE_SOFT, PURPLE);
        round.setGravity(Gravity.CENTER);
        root.addView(round, centeredWrapMargins(0, 4, 0, 8));
        addQuestionTimer(root);

        TextView prompt = text(current.prompt, 22, TEXT, true);
        prompt.setGravity(Gravity.CENTER);
        prompt.setLineSpacing(dp(4), 1f);
        prompt.setPadding(dp(16), dp(22), dp(16), dp(22));
        prompt.setBackground(round(CARD, softOutline(PURPLE), 1, radius()));
        prompt.setElevation(0);
        root.addView(prompt, fullMargins(4, 0, 4, 12));

        TextView host = text("", 12, MUTED, false);
        host.setGravity(Gravity.CENTER);
        root.addView(host, fullMargins(8, 0, 8, 10));

        EditText a = numericHostInput(teamA);
        EditText b = numericHostInput(teamB);
        a.setText(suddenClosestInputA);
        b.setText(suddenClosestInputB);
        bindClosestInput(a, true);
        bindClosestInput(b, false);
        root.addView(labeledInput(teamA, a, teamAccent(0)), fullMargins(5, 0, 5, 8));
        root.addView(labeledInput(teamB, b, teamAccent(1)), fullMargins(5, 0, 5, 12));

        Button reveal = primaryButton("ΕΛΕΓΧΟΣ ΑΠΑΝΤΗΣΕΩΝ", PURPLE);
        reveal.setOnClickListener(v -> {
            Double valueA = parseHostNumber(a.getText().toString());
            Double valueB = parseHostNumber(b.getText().toString());
            if (valueA == null || valueB == null) {
                Toast.makeText(this, "Γράψε και τις δύο αριθμητικές απαντήσεις", Toast.LENGTH_SHORT).show();
                return;
            }
            clearQuestionTimerState();
            suddenUsedQuestionIds.add(current.id);
            suddenClosestResolved = true;
            suddenClosestValueA = valueA;
            suddenClosestValueB = valueB;
            double diffA = Math.abs(valueA - current.numericAnswer);
            double diffB = Math.abs(valueB - current.numericAnswer);
            saveGameState();
            showClosestResult(current, valueA, valueB, diffA, diffB);
        });
        root.addView(reveal, fullMargins(5, 0, 5, 24));
        setStaticScreen(root);
    }

    private void bindClosestInput(EditText input, boolean teamAInput) {
        input.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence value, int start, int before, int count) {
                if (teamAInput) suddenClosestInputA = value == null ? "" : value.toString();
                else suddenClosestInputB = value == null ? "" : value.toString();
            }
            @Override public void afterTextChanged(Editable s) { }
        });
    }

    private EditText numericHostInput(String teamName) {
        EditText e = new EditText(this);
        e.setTextColor(TEXT);
        e.setHintTextColor(MUTED);
        e.setHint("Γράψε την απάντηση");
        e.setTextSize(18);
        e.setSingleLine(true);
        e.setGravity(Gravity.CENTER);
        e.setPadding(dp(12), dp(12), dp(12), dp(12));
        e.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_NUMBER_FLAG_SIGNED);
        e.setBackground(round(CARD, BORDER, 1, radius()));
        return e;
    }

    private View labeledInput(String label, EditText input, int accent) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(12), dp(10), dp(12), dp(12));
        card.setBackground(round(CARD, accent, 1, radius()));
        TextView name = text(label, 13, accent, true);
        name.setGravity(Gravity.CENTER);
        applyAdaptiveText(name, 10, 13, true);
        card.addView(name, fullMargins(0, 0, 0, 6));
        card.addView(input, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(54)));
        return card;
    }

    private Double parseHostNumber(String raw) {
        if (raw == null) return null;
        String value = raw.trim().replace(" ", "");
        if (value.isEmpty()) return null;
        try {
            if (value.contains(",") && value.contains(".")) value = value.replace(".", "").replace(",", ".");
            else value = value.replace(",", ".");
            return Double.parseDouble(value);
        } catch (Throwable ignored) { return null; }
    }

    private void showClosestResult(SuddenQuestion q, double valueA, double valueB, double diffA, double diffB) {
        boolean tie = Math.abs(diffA - diffB) < 0.0000001d;
        int winner = diffA < diffB ? 0 : 1;
        LinearLayout root = baseColumn();
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.addView(gameTopBar(false));
        TextView target = text("ΣΩΣΤΗ ΑΠΑΝΤΗΣΗ", 11, MUTED, true);
        target.setGravity(Gravity.CENTER);
        root.addView(target, fullMargins(0, 20, 0, 4));
        TextView answer = text(formatNumber(q.numericAnswer) + (q.unit.isEmpty() ? "" : " " + q.unit), 30, PURPLE, true);
        answer.setGravity(Gravity.CENTER);
        root.addView(answer, fullMargins(0, 0, 0, 14));

        LinearLayout results = cardColumn();
        results.addView(closestResultLine(teamA, valueA, diffA, BLUE, q.unit));
        results.addView(ruleDivider());
        results.addView(closestResultLine(teamB, valueB, diffB, RED, q.unit));
        root.addView(results, fullMargins(5, 0, 5, 14));

        if (tie) {
            TextView tied = text("ΙΔΙΑ ΑΠΟΣΤΑΣΗ", 22, PURPLE, true);
            tied.setGravity(Gravity.CENTER);
            root.addView(tied, fullMargins(0, 0, 0, 6));
            Button again = primaryButton("ΣΥΝΕΧΕΙΑ  →",modeAccent(gameMode));
            again.setOnClickListener(v->advanceSuddenWithoutWinner());
            root.addView(again,fullMargins(8,14,8,24));
        } else {
            TextView win = text("ΠΙΟ ΚΟΝΤΑ: " + (winner == 0 ? teamA : teamB), 22, winner == 0 ? BLUE : ORANGE, true);
            win.setGravity(Gravity.CENTER);
            applyAdaptiveText(win, 13, 22, true);
            root.addView(win, fullMargins(0, 0, 0, 10));
            Button finish = primaryButton("ΝΙΚΗΤΗΣ · " + (winner == 0 ? teamA : teamB), winner == 0 ? BLUE : ORANGE);
            finish.setOnClickListener(v -> showWinner(winner, true, false));
            root.addView(finish, fullMargins(8, 0, 8, 24));
        }
        setStaticScreen(root);
    }

    private View closestResultLine(String team, double value, double diff, int accent, String unit) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        TextView name = text(team, 13, accent, true);
        applyAdaptiveText(name, 9, 13, true);
        TextView guess = text(formatNumber(value), 16, TEXT, true);
        guess.setGravity(Gravity.CENTER);
        TextView distance = text("Διαφορά " + formatNumber(diff) + (unit.isEmpty() ? "" : " " + unit), 11, MUTED, true);
        distance.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        row.addView(name, new LinearLayout.LayoutParams(0, dp(48), 1));
        row.addView(guess, new LinearLayout.LayoutParams(dp(90), dp(48)));
        row.addView(distance, new LinearLayout.LayoutParams(0, dp(48), 1));
        return row;
    }

    private String formatNumber(double value) {
        if (Math.abs(value - Math.rint(value)) < 0.000001d) return String.valueOf((long) Math.rint(value));
        return String.format(java.util.Locale.US, "%.2f", value).replace('.', ',');
    }

    private class RouletteView extends View {
        private float rotation = 0f;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        RouletteView() {
            super(MainActivity.this);
            textPaint.setTextAlign(Paint.Align.CENTER);
            textPaint.setTypeface(uiTypeface(true));
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float cx = getWidth() / 2f;
            float cy = getHeight() / 2f;
            float radius = Math.min(getWidth(), getHeight()) * 0.46f;
            RectF oval = new RectF(cx - radius, cy - radius, cx + radius, cy + radius);
            float sweep = 360f / SUDDEN_MODES.length;

            canvas.save();
            canvas.rotate(rotation, cx, cy);
            for (int i = 0; i < SUDDEN_MODES.length; i++) {
                String mode = SUDDEN_MODES[i];
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(suddenModeSoft(mode));
                canvas.drawArc(oval, -90f + i * sweep, sweep, true, paint);
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(dp(2));
                paint.setColor(suddenModeColor(mode));
                canvas.drawArc(oval, -90f + i * sweep, sweep, true, paint);

                float angle = -90f + (i + 0.5f) * sweep;
                double r = Math.toRadians(angle);
                float tx = cx + (float) Math.cos(r) * radius * 0.60f;
                float ty = cy + (float) Math.sin(r) * radius * 0.60f;
                canvas.save();
                canvas.rotate(angle + 90f, tx, ty);
                textPaint.setColor(suddenModeColor(mode));
                textPaint.setTextSize(dp(("multiple".equals(mode) || "ab".equals(mode) || "closest".equals(mode)) ? 8 : 10));
                String[] lines = suddenModeWheelLabel(mode).split("\\n");
                for (int line = 0; line < lines.length; line++) {
                    canvas.drawText(lines[line], tx, ty + (line - (lines.length - 1) / 2f) * dp(12), textPaint);
                }
                canvas.restore();
            }
            canvas.restore();

            paint.setStyle(Paint.Style.FILL);
            paint.setColor(CARD);
            canvas.drawCircle(cx, cy, radius * 0.18f, paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(3));
            paint.setColor(PURPLE);
            canvas.drawCircle(cx, cy, radius * 0.18f, paint);
            textPaint.setTextSize(dp(17));
            textPaint.setColor(TEXT);
            canvas.drawText("QUIZ", cx, cy + dp(6), textPaint);

            Path pointer = new Path();
            pointer.moveTo(cx, cy - radius - dp(7));
            pointer.lineTo(cx - dp(13), cy - radius - dp(30));
            pointer.lineTo(cx + dp(13), cy - radius - dp(30));
            pointer.close();
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(RED);
            canvas.drawPath(pointer, paint);
        }

        void spinTo(int index, Runnable end) {
            float sweep = 360f / SUDDEN_MODES.length;
            float desired = -((index + 0.5f) * sweep);
            float currentMod = ((rotation % 360f) + 360f) % 360f;
            float desiredMod = ((desired % 360f) + 360f) % 360f;
            float delta = (desiredMod - currentMod + 360f) % 360f;
            float target = rotation + 360f * 7f + delta;
            ValueAnimator animator = ValueAnimator.ofFloat(rotation, target);
            animator.setDuration(7000L);
            animator.setInterpolator(new DecelerateInterpolator());
            animator.addUpdateListener(a -> {
                rotation = (float) a.getAnimatedValue();
                invalidate();
            });
            animator.addListener(new AnimatorListenerAdapter() {
                @Override public void onAnimationEnd(Animator animation) {
                    if (hapticsEnabled) try { performHapticFeedback(HapticFeedbackConstants.LONG_PRESS); } catch (Throwable ignored) { }
                    if (end != null) end.run();
                }
            });
            animator.start();
        }
    }

    private void showLiveScore() {
        LinearLayout root = baseColumn();
        Runnable back = gameMode == GameMode.CLASSIC ? this::showClassicBoard : (gameMode == GameMode.CHEESE ? this::showCheeseBoard : this::showBoard);
        root.addView(simpleBackBar("Σκορ", back));

        if (gameMode == GameMode.CHEESE) {
            root.addView(cheeseScores(),fullMargins(4,0,4,10));
            for(int i=0;i<CHEESE_IDS.length;i++){
                Category c=categoryById(CHEESE_IDS[i]);
                int owner=cheese.owner(i);
                root.addView(statLine(c.name,cheese.progress[0][i]+"/2",cheese.progress[1][i]+"/2"),fullMargins(4,2,4,2));
            }
        } else if (gameMode == GameMode.CLASSIC) {
            root.addView(classicCompactScoreStrip(), fullMargins(4, 0, 4, 10));
            LinearLayout progress = new LinearLayout(this);
            progress.setOrientation(LinearLayout.HORIZONTAL);
            progress.addView(classicProgressPanel(0), new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
            progress.addView(classicProgressPanel(1), new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
            root.addView(progress, fullMargins(4, 0, 4, 10));

            LinearLayout abilities = cardColumn();
            TextView title = text("SPECIAL ABILITIES", 12, MUTED, true);
            title.setGravity(Gravity.CENTER);
            abilities.addView(title, fullMargins(0, 0, 0, 6));
            String stealA = classicStealEnabled ? (classicStealAvailable(0) ? "1" : "0") : "OFF";
            String stealB = classicStealEnabled ? (classicStealAvailable(1) ? "1" : "0") : "OFF";
            abilities.addView(statLine("🥷 Κλοπή", stealA, stealB));
            abilities.addView(ruleDivider());
            abilities.addView(statLine("🔀 Ανακάτεμα", classicShuffleAvailable(0) ? "1" : "0", classicShuffleAvailable(1) ? "1" : "0"));
            abilities.addView(ruleDivider());
            abilities.addView(statLine("↻ Αλλαγή", classicChangeAvailable(0) ? "1" : "0", classicChangeAvailable(1) ? "1" : "0"));
            root.addView(abilities, fullMargins(4, 0, 4, 10));
        } else {
            root.addView(scoreStrip());
            LinearLayout stats = cardColumn();
            stats.addView(statLine("Σωστές", String.valueOf(correctA), String.valueOf(correctB)));
            stats.addView(ruleDivider());
            stats.addView(statLine("Λάθη", String.valueOf(wrongA), String.valueOf(wrongB)));
            stats.addView(ruleDivider());
            stats.addView(statLine("Διπλασιασμός", jokerAvailableA ? "1" : "0", jokerAvailableB ? "1" : "0"));
            root.addView(stats, fullMargins(4, 0, 4, 10));
        }

        Button resume = primaryButton("ΕΠΙΣΤΡΟΦΗ ΣΤΟ ΠΑΙΧΝΙΔΙ", GREEN);
        resume.setOnClickListener(v -> back.run());
        root.addView(resume, fullMargins(8, 0, 8, 12));
        setStaticScreen(root);
    }

    // ---------- SETTINGS ----------

    private void showSettings() {
        LinearLayout root=baseColumn();
        root.addView(settingsTopBar());
        root.addView(sectionTitle("Θέμα"),fullMargins(0,5,0,6));
        LinearLayout themes=cardColumn();
        GridLayout grid=new GridLayout(this);grid.setColumnCount(2);
        addThemeGridButton(grid,themeButton("LIGHT",THEME_LIGHT));
        addThemeGridButton(grid,themeButton("DARK",THEME_DARK));
        themes.addView(grid);
        root.addView(themes,fullMargins(4,0,4,12));
        root.addView(sectionTitle("Χρόνος"),fullMargins(0,0,0,6));
        LinearLayout choices=cardColumn();
        LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);
        row.addView(timerChoiceButton("OFF",0),weightedMargins(1,2,0,2,0));
        row.addView(timerChoiceButton("30″",30),weightedMargins(1,2,0,2,0));
        row.addView(timerChoiceButton("45″",45),weightedMargins(1,2,0,2,0));
        row.addView(timerChoiceButton("60″",60),weightedMargins(1,2,0,2,0));
        choices.addView(row);
        root.addView(choices,fullMargins(4,0,4,12));
        setStaticScreen(root);
    }


    private void addThemeGridButton(GridLayout grid, Button button) {
        GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
        lp.width = 0;
        lp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        lp.setMargins(dp(3), dp(3), dp(3), dp(3));
        grid.addView(button, lp);
    }

    private Button themeButton(String label, String value) {
        boolean selected = value.equals(theme);
        int accent = selected ? PURPLE : BORDER;
        Button b = button(label, selected ? PURPLE_SOFT : CARD, selected ? PURPLE : TEXT);
        b.setTextSize(12);
        b.setBackground(round(selected ? PURPLE_SOFT : CARD, accent, selected ? 2 : 1, 11));
        b.setOnClickListener(v -> {
            prefs.edit().putString("theme", value).apply();
            applyTheme(value);
            showSettings();
        });
        return b;
    }

    private Button timerChoiceButton(String label, int seconds) {
        boolean selected = questionTimerSeconds == seconds;
        int accent = ORANGE;
        int soft = ORANGE_SOFT;
        Button b = button((selected ? "✓ " : "") + label, selected ? soft : CARD, selected ? accent : TEXT);
        b.setTextSize(12);
        b.setMinHeight(dp(44));
        b.setBackground(round(selected ? soft : CARD, selected ? accent : BORDER, selected ? 2 : 1, 10));
        b.setOnClickListener(v -> {
            questionTimerSeconds = seconds;
            prefs.edit().putInt("question_timer_seconds", seconds).apply();
            if (gameActive) saveGameState();
            showSettings();
        });
        return b;
    }

    private View settingSwitch(String title, String description, boolean checked, BoolConsumer consumer) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(8), 0, dp(8));
        LinearLayout copy = new LinearLayout(this);
        copy.setOrientation(LinearLayout.VERTICAL);
        TextView t = text(title, 14, TEXT, true);
        TextView d = text(description, 11, MUTED, false);
        d.setPadding(0, dp(2), dp(8), 0);
        copy.addView(t);
        if (!description.isEmpty()) copy.addView(d);
        row.addView(copy, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        Switch toggle = new Switch(this);
        toggle.setChecked(checked);
        toggle.setOnCheckedChangeListener((buttonView, isChecked) -> consumer.accept(isChecked));
        row.addView(toggle);
        return row;
    }

    // ---------- UI COMPONENTS ----------

    private View gameTopBar(boolean showScore) {
        LinearLayout line = new LinearLayout(this);
        line.setGravity(Gravity.CENTER_VERTICAL);
        line.setPadding(0, dp(2), 0, dp(10));

        View back = backArrowButton(true);
        back.setOnClickListener(v -> confirmExitGame());
        line.addView(back, new LinearLayout.LayoutParams(dp(66), dp(48)));

        TextView title = gameBrandTitle();
        line.addView(title, new LinearLayout.LayoutParams(0, dp(48), 1));

        if (showScore) {
            Button score = button("ΣΚΟΡ", CARD, ORANGE);
            score.setTextSize(10);
            score.setPadding(dp(3), 0, dp(3), 0);
            score.setBackground(round(CARD, ORANGE, 1, 10));
            score.setOnClickListener(v -> showLiveScore());
            line.addView(score, new LinearLayout.LayoutParams(dp(66), dp(42)));
        } else {
            View spacer = new View(this);
            line.addView(spacer, new LinearLayout.LayoutParams(dp(66), dp(48)));
        }
        return line;
    }

    private TextView gameBrandTitle() {
        TextView logo = text("QUIZARA", 28, TEXT, true);
        logo.setGravity(Gravity.CENTER);
        logo.setTypeface(brandTypeface());
        logo.setSingleLine(true);
        logo.setIncludeFontPadding(true);
        logo.setPadding(dp(4), dp(2), dp(4), dp(2));
        applyAdaptiveText(logo, 20, 28, true);
        SpannableString wordmark = new SpannableString("QUIZARA");
        int highlight = isDarkTheme() ? Color.rgb(168, 0, 255) : Color.rgb(255, 81, 0);
        wordmark.setSpan(new ForegroundColorSpan(highlight), 4, 7, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        logo.setText(wordmark);
        return logo;
    }


    private void confirmExitGame() {
        pauseQuestionTimer();
        if (gameActive) saveGameState();
        try {
            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setTitle("Αποχώρηση από το παιχνίδι;")
                    .setMessage("Η πρόοδός σου θα αποθηκευτεί και μπορείς να συνεχίσεις αργότερα.")
                    .setNegativeButton("Παραμονή", (d, which) -> resumeQuestionTimerIfVisible())
                    .setPositiveButton("Αποχώρηση", (d, which) -> {
                        pauseQuestionTimer();
                        questionTimerView = null;
                        if (gameActive) saveGameState();
                        showHome();
                    })
                    .create();
            dialog.setOnCancelListener(d -> resumeQuestionTimerIfVisible());
            dialog.show();
        } catch (Throwable t) {
            resumeQuestionTimerIfVisible();
        }
    }

    private void prepareQuestionTimerForNewQuestion() {
        pauseQuestionTimer();
        questionTimerView = null;
        questionTimerExpired = false;
        questionTimerRemainingMs = questionTimerSeconds > 0 ? questionTimerSeconds * 1000L : 0L;
    }

    private void clearQuestionTimerState() {
        pauseQuestionTimer();
        questionTimerView = null;
        questionTimerExpired = false;
        questionTimerRemainingMs = 0L;
    }

    private void addQuestionTimer(LinearLayout root) {
        if (questionTimerSeconds <= 0) {
            questionTimerView = null;
            return;
        }
        long configuredMs = questionTimerSeconds * 1000L;
        if (questionTimerRemainingMs <= 0L && !questionTimerExpired) {
            questionTimerRemainingMs = configuredMs;
        } else if (!questionTimerExpired && questionTimerRemainingMs > configuredMs) {
            questionTimerRemainingMs = configuredMs;
        }
        int accent = ORANGE;
        int soft = ORANGE_SOFT;
        TextView timer = pill(formatQuestionTimer(), accent, soft, accent);
        timer.setGravity(Gravity.CENTER);
        questionTimerView = timer;
        root.addView(timer, centeredWrapMargins(0, 0, 0, 9));
        if (questionTimerExpired) {
            timer.setText("⏱  ΤΕΛΟΣ ΧΡΟΝΟΥ");
        } else {
            startQuestionTimer(timer);
        }
    }

    private void startQuestionTimer(TextView timerView) {
        pauseQuestionTimer();
        if (questionTimerSeconds <= 0 || questionTimerExpired || questionTimerRemainingMs <= 0L) return;
        questionTimerView = timerView;
        questionTimer = new CountDownTimer(questionTimerRemainingMs, 250L) {
            @Override
            public void onTick(long millisUntilFinished) {
                questionTimerRemainingMs = millisUntilFinished;
                if (questionTimerView != null) questionTimerView.setText(formatQuestionTimer());
            }

            @Override
            public void onFinish() {
                questionTimer = null;
                questionTimerRemainingMs = 0L;
                questionTimerExpired = true;
                if (questionTimerView != null) {
                    questionTimerView.setText("⏱  ΤΕΛΟΣ ΧΡΟΝΟΥ");
                    pulse(questionTimerView);
                }
                if (soundEnabled) playTone(ToneGenerator.TONE_PROP_BEEP, 70, 18);
                if (gameActive) saveGameState();
            }
        };
        questionTimer.start();
    }

    private void pauseQuestionTimer() {
        if (questionTimer != null) {
            try { questionTimer.cancel(); } catch (Throwable ignored) { }
            questionTimer = null;
        }
    }

    private void resumeQuestionTimerIfVisible() {
        if (questionTimerView != null && questionTimerSeconds > 0 && !questionTimerExpired && questionTimerRemainingMs > 0L && questionTimer == null) {
            startQuestionTimer(questionTimerView);
        }
    }

    private String formatQuestionTimer() {
        long totalSeconds = Math.max(0L, (questionTimerRemainingMs + 999L) / 1000L);
        long minutes = totalSeconds / 60L;
        long seconds = totalSeconds % 60L;
        return String.format("⏱  %02d:%02d", minutes, seconds);
    }

    private View topBar(boolean showBack, boolean showSettings) {
        LinearLayout line = new LinearLayout(this);
        line.setGravity(Gravity.CENTER_VERTICAL);
        line.setPadding(0, dp(2), 0, dp(10));

        View left = backArrowButton(showBack);
        if (showBack) left.setOnClickListener(v -> {
            if (settingsSource == ScreenSource.SETUP || settingsSource == ScreenSource.CLASSIC_SETUP || !gameActive) showModeSelect();
            else if (settingsSource == ScreenSource.CLASSIC_BOARD || gameMode == GameMode.CLASSIC) showClassicBoard();
            else if (gameMode == GameMode.CHEESE) showCheeseBoard();
            else showBoard();
        });
        line.addView(left, new LinearLayout.LayoutParams(dp(48), dp(48)));

        TextView title = gameBrandTitle();
        line.addView(title, new LinearLayout.LayoutParams(0, dp(48), 1));

        // Settings intentionally exist only on the Home screen.
        View spacer = new View(this);
        line.addView(spacer, new LinearLayout.LayoutParams(dp(48), dp(48)));
        return line;
    }

    private View settingsTopBar() {
        LinearLayout line = new LinearLayout(this);
        line.setGravity(Gravity.CENTER_VERTICAL);
        line.setPadding(0, dp(2), 0, dp(10));
        View back = backArrowButton(true);
        back.setOnClickListener(v -> showHome());
        line.addView(back, new LinearLayout.LayoutParams(dp(48), dp(48)));
        TextView title = gameBrandTitle();
        title.setGravity(Gravity.CENTER);
        line.addView(title, new LinearLayout.LayoutParams(0, dp(48), 1));
        View spacer = new View(this);
        line.addView(spacer, new LinearLayout.LayoutParams(dp(48), dp(48)));
        return line;
    }

    private View scoreStrip() {
        LinearLayout row=new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.addView(scoreCard(teamA,scoreA,teamAccent(0),0),new LinearLayout.LayoutParams(0,dp(88),1));
        TextView vs=text("VS",14,MUTED,true);vs.setGravity(Gravity.CENTER);
        row.addView(vs,new LinearLayout.LayoutParams(dp(40),dp(48)));
        row.addView(scoreCard(teamB,scoreB,teamAccent(1),1),new LinearLayout.LayoutParams(0,dp(88),1));
        return row;
    }


    private View scoreCard(String name, int score, int accent, int team) {
        LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER);card.setPadding(dp(5),dp(8),dp(5),dp(8));
        boolean active=currentTeam==team;
        card.setBackground(round(active?teamTint(team):CARD,teamAccent(team),active?3:1,radius()));
        card.setElevation(dp(active?5:1));
        TextView title=text((active?"▶  ":"")+name,14,teamAccent(team),true);
        title.setSingleLine(true);applyAdaptiveText(title,10,14,true);card.addView(title);
        card.addView(text(String.valueOf(score),30,TEXT,true));
        TextView joker=text(isJokerAvailable(team)?"⚡ ΔΙΠΛ.":"ΔΙΠΛ. ✓",10,teamAccent(team),true);
        joker.setGravity(Gravity.CENTER);card.addView(joker);
        return card;
    }


    private View turnDots(int played, int accent) {
        LinearLayout dots = new LinearLayout(this);
        dots.setGravity(Gravity.CENTER);
        int slots = Math.max(9, (int) Math.ceil(totalDuelQuestions() / 2.0));
        int width = slots > 12 ? 9 : 14;
        int size = slots > 12 ? 7 : 10;
        for (int i = 0; i < slots; i++) {
            TextView d = text(i < played ? "●" : "○", size, i < played ? accent : MUTED, true);
            d.setGravity(Gravity.CENTER);
            dots.addView(d, new LinearLayout.LayoutParams(dp(width), dp(16)));
        }
        return dots;
    }

    /** Subtle accent strokes on gameplay surfaces; full color stays in Home and category identity. */
    private int softOutline(int accent) {
        float weight = isDarkTheme() ? 0.40f : 0.42f;
        return Color.rgb(
                Math.round(Color.red(CARD) * (1 - weight) + Color.red(accent) * weight),
                Math.round(Color.green(CARD) * (1 - weight) + Color.green(accent) * weight),
                Math.round(Color.blue(CARD) * (1 - weight) + Color.blue(accent) * weight));
    }

    private int teamAccent(int team) {
        int idx = team == 0 ? teamColorA : teamColorB;
        final int[] normal = {0xFFE53935, 0xFFFF6500, 0xFFF3BC24, 0xFF159B55, 0xFF0645D9, 0xFF913FE0};
        final int[] dark = {0xFFE8A3A5, 0xFFE2AF8C, 0xFFDACB93, 0xFFA0D4B8, 0xFFA4BAE8, 0xFFC5A4E7};
        return (isDarkTheme() ? dark : normal)[Math.max(0, Math.min(5, idx))];
    }

    private int teamTint(int team) {
        int c=teamAccent(team);
        float a = isDarkTheme() ? 0.18f : 0.075f;
        return Color.rgb((int)(Color.red(c)*a + Color.red(CARD)*(1-a)),
                (int)(Color.green(c)*a + Color.green(CARD)*(1-a)),
                (int)(Color.blue(c)*a + Color.blue(CARD)*(1-a)));
    }

    /** One reusable team-settings design shared by Duel, Classic and Four Cheeses. */
    private View teamColorSettings(EditText a, EditText b) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.addView(teamColorSetupCard(0, a), weightedMargins(1, 0, 0, 4, 0));
        row.addView(teamColorSetupCard(1, b), weightedMargins(1, 4, 0, 0, 0));
        return row;
    }

    private View teamColorSetupCard(int team, EditText input) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(7), dp(9), dp(7), dp(9));
        card.setBackground(round(CARD, teamAccent(team), 2, radius()));
        TextView title = text(team==0 ? "ΟΜΑΔΑ Α" : "ΟΜΑΔΑ Β", 14, teamAccent(team), true);
        title.setGravity(Gravity.CENTER);
        card.addView(title, fullMargins(0,0,0,7));
        input.setTextSize(15);
        card.addView(input, new LinearLayout.LayoutParams(-1, dp(44)));
        LinearLayout swatches = new LinearLayout(this);
        swatches.setGravity(Gravity.CENTER);
        for (int i = 0; i < 6; i++) {
            final int selected = i;
            int color = teamSwatchColor(i);
            View square = new View(this);
            square.setBackground(round(color, i == (team == 0 ? teamColorA : teamColorB) ? TEXT : color,
                    i == (team == 0 ? teamColorA : teamColorB) ? 3 : 1, 4));
            square.setContentDescription("Χρώμα " + (i + 1) + " για ομάδα " + (team == 0 ? "Α" : "Β"));
            LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(0, dp(25), 1);
            sp.setMargins(dp(2), 0, dp(2), 0);
            swatches.addView(square, sp);
            square.setOnClickListener(v -> {
                // Both teams must always have different colors.
                int previous = team == 0 ? teamColorA : teamColorB;
                if (team == 0) {
                    if (teamColorB == selected) teamColorB = previous;
                    teamColorA = selected;
                } else {
                    if (teamColorA == selected) teamColorA = previous;
                    teamColorB = selected;
                }
                prefs.edit().putInt("team_color_a",teamColorA).putInt("team_color_b",teamColorB).apply();
                refreshTeamColorCards();
            });
        }
        card.addView(swatches, fullMargins(0, 10, 0, 0));
        liveSwatches[team] = swatches;
        liveTeamCards[team] = card;
        liveTeamTitles[team] = title;
        return card;
    }

    private void refreshTeamColorCards() {
        for (int team=0;team<2;team++) {
            LinearLayout row=liveSwatches[team];
            LinearLayout card=liveTeamCards[team];
            TextView title=liveTeamTitles[team];
            if (row==null || card==null || title==null) continue;
            int selected=team==0?teamColorA:teamColorB;
            card.setBackground(round(CARD,teamAccent(team),2,radius()));
            title.setTextColor(teamAccent(team));
            for (int j=0;j<row.getChildCount();j++) {
                View chip=row.getChildAt(j);
                int color=teamSwatchColor(j);
                chip.setBackground(round(color,j==selected?TEXT:color,j==selected?3:1,4));
            }
        }
    }

    private int teamSwatchColor(int index) {
        return new int[]{0xFFE53935, 0xFFFF6500, 0xFFF3C623, 0xFF159B55, 0xFF0645D9, 0xFF913FE0}[index];
    }

    private int modeAccent(GameMode mode) {
        if (mode == GameMode.DUEL) return isDarkTheme() ? Color.rgb(174, 105, 251) : Color.rgb(19, 19, 18);
        if (mode == GameMode.CLASSIC) return isDarkTheme() ? Color.rgb(255, 214, 0) : Color.rgb(255, 81, 0);
        return isDarkTheme() ? Color.rgb(0, 221, 208) : Color.rgb(3, 55, 216);
    }

    private Button modeStartButton(GameMode mode) {
        int accent=modeAccent(mode);
        int ink = isDarkTheme() ? Color.BLACK : Color.WHITE;
        Button start = button("ΕΝΑΡΞΗ  →",accent,ink);
        start.setMinHeight(dp(62));
        start.setBackground(new PosterDrawable(accent));
        start.setTypeface(uiTypeface(true));
        start.setTextSize(23);
        return start;
    }

    private TextView turnBanner(String name, int team) {
        int accent = teamAccent(team);
        TextView t = pill("ΠΑΙΖΟΥΝ ΟΙ " + name.toUpperCase(java.util.Locale.forLanguageTag("el")),
                accent, teamTint(team), softOutline(accent));
        t.setPadding(dp(12),dp(7),dp(12),dp(7));
        t.setGravity(Gravity.CENTER);
        applyAdaptiveText(t, 10, 14, true);
        return t;
    }

    private View teamSetupWideCard(String label, String iconText, EditText input, int accent, int soft) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(10), dp(9), dp(10), dp(9));
        card.setBackground(round(CARD, accent, 1, radius()));
        card.setElevation(dp(3));

        TextView icon = iconCircle(iconText, accent, soft);
        card.addView(icon, new LinearLayout.LayoutParams(dp(46), dp(46)));

        TextView team = text(label, 14, accent, true);
        team.setGravity(Gravity.CENTER_VERTICAL);
        team.setPadding(dp(9), 0, dp(8), 0);
        card.addView(team, new LinearLayout.LayoutParams(dp(86), dp(46)));

        LinearLayout.LayoutParams inputLp = new LinearLayout.LayoutParams(0, dp(48), 1);
        card.addView(input, inputLp);
        return card;
    }

    private View teamSetupCard(String label, String iconText, EditText input, int accent, int soft) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(10), dp(10), dp(10), dp(10));
        int actualAccent = accent;
        int actualTextAccent = accent;
        int actualSoft = soft;
        card.setBackground(round(CARD, actualAccent, 1, radius()));
        TextView icon = iconCircle(iconText, actualTextAccent, actualSoft);
        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.addView(icon, new LinearLayout.LayoutParams(dp(52), dp(52)));
        TextView team = text(label, 16, actualTextAccent, true);
        team.setPadding(dp(8), 0, 0, 0);
        top.addView(team, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        card.addView(top);
        card.addView(input, fullMargins(0, 10, 0, 0));
        return card;
    }

    private EditText teamInput(String value) {
        EditText input = new EditText(this);
        input.setText(value);
        input.setTextColor(TEXT);
        input.setTextSize(16);
        input.setTypeface(uiTypeface(false));
        input.setSingleLine(true);
        input.setPadding(dp(11), dp(9), dp(11), dp(9));
        input.setBackground(round(CARD, BORDER, 1, 11));
        return input;
    }

    private View backArrowButton(boolean visible) {
        View back = new View(this) {
            private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            @Override protected void onDraw(Canvas canvas) {
                super.onDraw(canvas);
                paint.setColor(TEXT);
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(dp(2));
                paint.setStrokeCap(Paint.Cap.ROUND);
                paint.setStrokeJoin(Paint.Join.ROUND);
                float cx = getWidth() / 2f;
                float cy = getHeight() / 2f;
                float half = dp(9);
                canvas.drawLine(cx + half * 0.55f, cy, cx - half * 0.55f, cy, paint);
                canvas.drawLine(cx - half * 0.55f, cy, cx - half * 0.05f, cy - half * 0.5f, paint);
                canvas.drawLine(cx - half * 0.55f, cy, cx - half * 0.05f, cy + half * 0.5f, paint);
            }
        };
        back.setBackground(round(CARD, BORDER, 1, 14));
        back.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
        back.setSoundEffectsEnabled(false);
        attachPressAnimation(back);
        return back;
    }

    private Button iconButton(String label, boolean visible) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextSize(22);
        b.setTextColor(TEXT);
        b.setAllCaps(false);
        b.setStateListAnimator(null);
        b.setPadding(0, 0, 0, 0);
        b.setMinWidth(0);
        b.setMinimumWidth(0);
        b.setMinHeight(0);
        b.setMinimumHeight(0);
        b.setIncludeFontPadding(false);
        b.setGravity(Gravity.CENTER);
        b.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        b.setBackground(round(CARD, BORDER, 1, 14));
        b.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
        b.setSoundEffectsEnabled(false);
        attachPressAnimation(b);
        return b;
    }

    private LinearLayout cardColumn() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(14), dp(14), dp(14));
        card.setBackground(round(CARD, BORDER, 1, radius()));
        card.setElevation(0);
        card.setGravity(Gravity.CENTER_HORIZONTAL);
        return card;
    }

    private TextView iconCircle(String value, int accent, int soft) {
        TextView t = text(value, 22, accent, false);
        t.setGravity(Gravity.CENTER);
        t.setBackground(round(soft, soft, 0, 100));
        return t;
    }

    private TextView pill(String value, int color, int fill, int stroke) {
        TextView t = text(value, 12, color, true);
        t.setPadding(dp(12), dp(6), dp(12), dp(6));
        t.setBackground(round(fill, stroke, 1, 100));
        return t;
    }

    private TextView sectionTitle(String value) {
        TextView t = text(value, 19, TEXT, true);
        t.setGravity(Gravity.CENTER);
        return t;
    }

    private View ruleRow(String iconText, String value, int accent, int soft) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(6), 0, dp(6));
        TextView icon = iconCircle(iconText, accent, soft);
        row.addView(icon, new LinearLayout.LayoutParams(dp(36), dp(36)));
        TextView t = text(value, 13, TEXT, false);
        t.setPadding(dp(9), 0, 0, 0);
        row.addView(t, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        return row;
    }

    private View ruleDivider() {
        View v = new View(this);
        v.setBackgroundColor(BORDER);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1));
        lp.setMargins(dp(4), 0, dp(4), 0);
        v.setLayoutParams(lp);
        return v;
    }

    private View statLine(String label, String a, String b) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(7), 0, dp(7));
        TextView left = text(a, 13, BLUE, true);
        left.setGravity(Gravity.CENTER);
        row.addView(left, new LinearLayout.LayoutParams(dp(72), LinearLayout.LayoutParams.WRAP_CONTENT));
        TextView center = text(label, 12, MUTED, false);
        center.setGravity(Gravity.CENTER);
        row.addView(center, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        TextView right = text(b, 13, RED, true);
        right.setGravity(Gravity.CENTER);
        row.addView(right, new LinearLayout.LayoutParams(dp(72), LinearLayout.LayoutParams.WRAP_CONTENT));
        return row;
    }

    private void showInfoDialog(String titleValue, String bodyValue) {
        pauseQuestionTimer();
        try {
            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setTitle(titleValue)
                    .setMessage(bodyValue)
                    .setPositiveButton("ΟΚ", (d, which) -> resumeQuestionTimerIfVisible())
                    .create();
            dialog.setOnCancelListener(d -> resumeQuestionTimerIfVisible());
            dialog.show();
        } catch (Throwable ignored) {
            resumeQuestionTimerIfVisible();
            Toast.makeText(this, bodyValue, Toast.LENGTH_LONG).show();
        }
    }

    private Button infoButton(String titleValue, String bodyValue, int accent) {
        Button info = button("ⓘ", CARD, accent);
        info.setTextSize(18);
        info.setPadding(0, 0, 0, 0);
        info.setMinWidth(0);
        info.setMinimumWidth(0);
        info.setMinHeight(0);
        info.setMinimumHeight(0);
        info.setElevation(0);
        info.setBackground(round(CARD, accent, 1, 100));
        info.setContentDescription("Πληροφορίες: " + titleValue);
        info.setOnClickListener(v -> showInfoDialog(titleValue, bodyValue));
        return info;
    }

    private String modeInfo(String mode) {
        if ("duel".equals(mode)) return "Δύο ομάδες παίζουν εναλλάξ στις 6 βασικές κατηγορίες. Κάθε κατηγορία έχει ×1, ×2 και ×3. Μπορείς να ενεργοποιήσεις Special κατηγορίες και κάθε ομάδα έχει έναν Διπλασιασμό για μία ερώτηση.";
        return "Σε κάθε γύρο επιλέγεις μία από δύο διαθέσιμες κατηγορίες. Στόχος: δύο σωστές σε καθεμία από τις έξι κατηγορίες.";
    }

    private String specialInfo(String type) {
        if ("top5".equals(type)) return "Ο εκφωνητής αποκαλύπτει τη λίστα των 5 σωστών απαντήσεων. 5/5 δίνει 2 πόντους, 4/5 και σταμάτησε δίνει 1 πόντο, αποτυχία 0.";
        if ("find3".equals(type)) return "Η ομάδα πρέπει να βρει οποιεσδήποτε 3 αποδεκτές απαντήσεις. Επιτυχία: 2 πόντοι.";
        if ("person_id".equals(type)) return "Αναγνώρισε το πρόσωπο από τα clues. Με το 1ο clue αξίζει 3 πόντους, με το 2ο 2 πόντους και με το 3ο 1 πόντο.";
        if ("country_id".equals(type)) return "Αναγνώρισε τη χώρα από τα clues. Με το 1ο clue αξίζει 3 πόντους, με το 2ο 2 πόντους και με το 3ο 1 πόντο.";
        if ("rank".equals(type)) return "Ο εκφωνητής κάνει drag & drop τα στοιχεία μέχρι να μπουν στη σωστή σειρά. Πλήρως σωστή σειρά: 2 πόντοι.";
        return "Special γύρος.";
    }

    private String suddenModeInfo(String mode) {
        if ("tf".equals(mode)) return "Κάθε ομάδα παίρνει διαφορετική πρόταση και απαντά Σωστό ή Λάθος. Αν μόνο μία ομάδα είναι σωστή, κερδίζει.";
        if ("multiple".equals(mode)) return "Κάθε ομάδα παίρνει διαφορετική ερώτηση πολλαπλής επιλογής. Αν μόνο μία απαντήσει σωστά, κερδίζει.";
        if ("over_under".equals(mode)) return "Κάθε ομάδα κρίνει αν η σωστή αριθμητική τιμή είναι πάνω ή κάτω από το όριο που εμφανίζεται.";
        if ("ab".equals(mode)) return "Κάθε ομάδα επιλέγει ανάμεσα σε δύο πιθανές απαντήσεις. Αν μόνο μία είναι σωστή, κερδίζει.";
        return "Κοινή αριθμητική ερώτηση. Και οι δύο δίνουν εκτίμηση και κερδίζει όποιος βρεθεί πιο κοντά στη σωστή τιμή.";
    }

    private View withInfoControl(View main, int heightDp, String titleValue, String bodyValue, int accent) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.addView(main, new LinearLayout.LayoutParams(0, dp(heightDp), 1));
        Button info = infoButton(titleValue, bodyValue, accent);
        LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(dp(42), dp(42));
        ip.setMargins(dp(8), 0, 0, 0);
        row.addView(info, ip);
        return row;
    }

    private Button primaryButton(String label, int accent) {
        int ink = isDarkTheme() && accent==ORANGE ? Color.rgb(21,17,30) : Color.WHITE;
        Button b = button(label, accent, ink);
        b.setMinHeight(dp(58));
        b.setTextSize(17);
        b.setBackground(new PosterDrawable(accent));
        b.setElevation(0);
        return b;
    }

    private Button button(String value, int bgColor, int textColor) {
        Button b = new Button(this);
        b.setText(value);
        b.setTextColor(textColor);
        b.setTextSize(15);
        b.setTypeface(uiTypeface(true));
        b.setGravity(Gravity.CENTER);
        b.setAllCaps(false);
        b.setPadding(dp(6), 0, dp(6), 0);
        b.setBackground(round(bgColor, bgColor, 0, radius()));
        b.setMinHeight(dp(48));
        b.setStateListAnimator(null);
        b.setElevation(0);
        b.setSoundEffectsEnabled(false);
        attachPressAnimation(b);
        return b;
    }

    private void applyAdaptiveText(TextView view, int minSp, int maxSp, boolean singleLine) {
        view.setGravity(Gravity.CENTER);
        view.setSingleLine(singleLine);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            view.setAutoSizeTextTypeUniformWithConfiguration(minSp, maxSp, 1, TypedValue.COMPLEX_UNIT_SP);
        } else {
            String value = view.getText() == null ? "" : view.getText().toString();
            int len = value.length();
            float size = maxSp;
            if (len > 24) size = Math.max(minSp, maxSp - 4);
            else if (len > 18) size = Math.max(minSp, maxSp - 3);
            else if (len > 12) size = Math.max(minSp, maxSp - 2);
            else if (len > 8) size = Math.max(minSp, maxSp - 1);
            view.setTextSize(size);
        }
    }

    private Typeface uiTypeface(boolean bold) {
        try { return Typeface.create("sans-serif-condensed", bold ? Typeface.BOLD : Typeface.NORMAL); }
        catch (Throwable ignored) { return Typeface.create(Typeface.DEFAULT, bold ? Typeface.BOLD : Typeface.NORMAL); }
    }


    private Typeface brandTypeface() {
        try {
            return Typeface.create("sans-serif-condensed", Typeface.BOLD_ITALIC);
        } catch (Throwable ignored) {
            return Typeface.DEFAULT_BOLD;
        }
    }

    private TextView text(String value, int sp, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextColor(color);
        t.setTextSize(sp);
        t.setTypeface(uiTypeface(bold));
        return t;
    }


    private LinearLayout baseColumn() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(10), dp(6), dp(10), dp(14));
        root.setBackgroundColor(BG);
        return root;
    }

    private void setStaticScreen(LinearLayout content) {
        if (inSuddenDeath && !suddenStarted) tensionShake(content);
        FitScreenLayout frame = new FitScreenLayout();
        frame.setBackgroundColor(BG);
        frame.setClipChildren(true);
        frame.setClipToPadding(false);
        frame.addView(content, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT));
        frame.setOnApplyWindowInsetsListener((v, insets) -> {
            try {
                int left = insets.getSystemWindowInsetLeft();
                int top = insets.getSystemWindowInsetTop();
                int right = insets.getSystemWindowInsetRight();
                int bottom = insets.getSystemWindowInsetBottom();
                frame.setPadding(left, top + dp(6), right, bottom + dp(8));
            } catch (Throwable ignored) {
                frame.setPadding(0, dp(22), 0, dp(18));
            }
            frame.requestLayout();
            return insets;
        });
        setContentView(frame);
        frame.requestApplyInsets();
    }

    private class FitScreenLayout extends FrameLayout {
        FitScreenLayout() { super(MainActivity.this); }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            int width = MeasureSpec.getSize(widthMeasureSpec);
            int height = MeasureSpec.getSize(heightMeasureSpec);
            setMeasuredDimension(width, height);
            if (getChildCount() == 0) return;
            View child = getChildAt(0);
            int availableWidth = Math.max(1, width - getPaddingLeft() - getPaddingRight());
            int childWidthSpec = MeasureSpec.makeMeasureSpec(availableWidth, MeasureSpec.EXACTLY);
            int availableHeight = Math.max(1, height - getPaddingTop() - getPaddingBottom());
            int childHeightSpec = "winner_full_height".equals(child.getTag())
                    ? MeasureSpec.makeMeasureSpec(availableHeight, MeasureSpec.EXACTLY)
                    : MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
            child.measure(childWidthSpec, childHeightSpec);
        }

        @Override
        protected void onLayout(boolean changed, int l, int t, int r, int b) {
            if (getChildCount() == 0) return;
            View child = getChildAt(0);
            int left = getPaddingLeft();
            int top = getPaddingTop();
            int childWidth = child.getMeasuredWidth();
            int childHeight = child.getMeasuredHeight();
            child.layout(left, top, left + childWidth, top + childHeight);

            int availableHeight = Math.max(1, getHeight() - getPaddingTop() - getPaddingBottom());
            float scale = childHeight <= availableHeight ? 1f : (availableHeight / (float) childHeight);
            child.setPivotX(childWidth / 2f);
            child.setPivotY(0f);
            child.setScaleX(scale);
            child.setScaleY(scale);
        }
    }

    // No vertical scrolling anywhere in the game. The existing FitScreenLayout
    // fits content to the viewport, keeping controls reachable on small phones.
    private void setScrollable(LinearLayout content) {
        setStaticScreen(content);
    }

    private GradientDrawable round(int fill, int stroke, int strokeDp, int radiusDp) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(fill);
        d.setCornerRadius(dp(radiusDp));
        if (strokeDp > 0) d.setStroke(dp(strokeDp), stroke);
        return d;
    }

    private boolean isDarkTheme() {
        return THEME_DARK.equals(theme);
    }

    private int radius() {
        return 3;
    }

    // ---------- GAME LOGIC HELPERS ----------

    private boolean isJokerAvailable(int team) {
        return team == 0 ? jokerAvailableA : jokerAvailableB;
    }

    private void consumeJoker(int team) {
        if (team == 0) jokerAvailableA = false;
        else jokerAvailableB = false;
    }

    private int totalDuelQuestions() {
        return 18 + enabledSpecialTypes.size() * 2;
    }

    private int specialRemainingCount() {
        int remaining = 0;
        for (String type : enabledSpecialTypes) remaining += Math.max(0, 2 - countOf(specialPlaysUsed, type));
        return remaining;
    }

    private int remainingCount() {
        return (18 - used.size()) + specialRemainingCount();
    }

    private String matchPressureText() {
        if (!earlyFinishEnabled || scoreA == scoreB || answered == 0) return "";
        int leader = scoreA > scoreB ? 0 : 1;
        int trailer = 1 - leader;
        int leaderScore = leader == 0 ? scoreA : scoreB;
        int trailerScore = trailer == 0 ? scoreA : scoreB;
        int trailerMax = maxReachForTeam(trailer, currentTeam);
        if (trailerMax == leaderScore) {
            return "⚡ " + (trailer == 0 ? teamA : teamB) + " μπορούν πλέον μόνο να ισοφαρίσουν";
        }
        int possibleGain = trailerMax - trailerScore;
        int needed = leaderScore - trailerScore;
        if (needed > 0 && possibleGain <= needed + 3) {
            return "• " + (trailer == 0 ? teamA : teamB) + " χρειάζονται " + needed + " πόντους για να φτάσουν το σκορ";
        }
        return "";
    }

    private String difficultyName(int multiplier) {
        if (multiplier == 1) return "Εύκολη";
        if (multiplier == 2) return "Μεσαία";
        return "Δύσκολη";
    }

    private int difficultyColor(int multiplier) {
        if (multiplier == 1) return GREEN;
        if (multiplier == 2) return ORANGE;
        return RED;
    }

    private int difficultySoft(int multiplier) {
        if (multiplier == 1) return GREEN_SOFT;
        if (multiplier == 2) return ORANGE_SOFT;
        return RED_SOFT;
    }

    private int categoryAccent(Category c) {
        // One stable category identity everywhere in the app.
        switch (c.id) {
            case "sports": return ORANGE;
            case "art": return PURPLE;
            case "science": return GREEN;
            case "history": return YELLOW;
            case "general": return RED;
            case "geography": return BLUE;
            default: return BLUE;
        }
    }

    private int categorySoft(Category c) {
        switch (c.id) {
            case "sports": return ORANGE_SOFT;
            case "art": return PURPLE_SOFT;
            case "science": return GREEN_SOFT;
            case "history": return YELLOW_SOFT;
            case "general": return RED_SOFT;
            case "geography": return BLUE_SOFT;
            default: return BLUE_SOFT;
        }
    }

    private void playFeedback(boolean correct) {
        if (hapticsEnabled) {
            try {
                getWindow().getDecorView().performHapticFeedback(correct ? HapticFeedbackConstants.CONFIRM : HapticFeedbackConstants.REJECT);
            } catch (Throwable ignored) {
                try { getWindow().getDecorView().performHapticFeedback(correct ? HapticFeedbackConstants.KEYBOARD_TAP : HapticFeedbackConstants.LONG_PRESS); } catch (Throwable ignoredAgain) { }
            }
        }
        if (soundEnabled) {
            playRawSound(correct ? R.raw.sfx_correct : R.raw.sfx_wrong, correct ? 0.22f : 0.18f);
        }
    }

    private void playRawSound(int resourceId, float volume) {
        if (!soundEnabled) return;
        audioExecutor.execute(() -> {
            try {
                MediaPlayer player = MediaPlayer.create(this, resourceId);
                if (player == null) return;
                float v = Math.max(0f, Math.min(1f, volume));
                player.setVolume(v, v);
                player.setOnCompletionListener(mp -> {
                    try { mp.release(); } catch (Throwable ignored) { }
                });
                player.setOnErrorListener((mp, what, extra) -> {
                    try { mp.release(); } catch (Throwable ignored) { }
                    return true;
                });
                player.start();
            } catch (Throwable ignored) { }
        });
    }

    private void playVictoryMusic() {
        if (!soundEnabled) return;
        playRawSound(R.raw.music_victory, 0.34f);
    }

    private void playTone(int tone, int durationMs, int volume) {
        if (!soundEnabled) return;
        audioExecutor.execute(() -> {
            try {
                ToneGenerator tg = new ToneGenerator(AudioManager.STREAM_MUSIC, Math.max(0, Math.min(100, volume)));
                tg.startTone(tone, durationMs);
                handler.postDelayed(() -> audioExecutor.execute(tg::release), durationMs + 90L);
            } catch (Throwable ignored) { }
        });
    }

    private void playUiTick() {
        if (soundEnabled) playTone(ToneGenerator.TONE_PROP_BEEP, 30, 10);
    }

    private void playFlipFeedback() {
        if (hapticsEnabled) {
            try { getWindow().getDecorView().performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP); } catch (Throwable ignored) { }
        }
        if (soundEnabled) playTone(ToneGenerator.TONE_PROP_BEEP, 42, 14);
    }

    private void attachPressAnimation(View v) {
        v.setOnTouchListener((view, event) -> {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    if (effectsEnabled && hapticsEnabled) {
                        try { view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP); } catch (Throwable ignored) { }
                    }
                    if (effectsEnabled) {
                        playUiTick();
                        float pressedScale = 0.955f;
                        view.animate().cancel();
                        view.animate().scaleX(pressedScale).scaleY(pressedScale).alpha(0.88f).translationY(dp(1)).setDuration(65).start();
                    }
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    if (effectsEnabled) {
                        view.animate().cancel();
                        view.animate().scaleX(1f).scaleY(1f).alpha(1f).translationY(0f).setDuration(115).start();
                    }
                    break;
            }
            return false;
        });
    }

    private void renderQuestionFace(LinearLayout card, Question q, TextView pointsText) {
        card.removeAllViews();
        TextView label = text("ΕΡΩΤΗΣΗ", 11, difficultyColor(q.multiplier), true);
        label.setGravity(Gravity.CENTER);
        card.addView(label, fullMargins(0, 0, 0, 8));
        TextView question = text(q.question, 22, TEXT, true);
        question.setGravity(Gravity.CENTER);
        question.setLineSpacing(dp(3), 1f);
        question.setPadding(dp(2), dp(6), dp(2), dp(14));
        card.addView(question);
        
        pointsText.setTextColor(activeJokerSelected ? RED : MUTED);
        pointsText.setGravity(Gravity.CENTER);
        card.addView(pointsText);
    }

    private void renderAnswerFace(LinearLayout card, Question q, TextView pointsText) {
        card.removeAllViews();
        TextView label = text("ΑΠΑΝΤΗΣΗ", 12, difficultyColor(q.multiplier), true);
        label.setGravity(Gravity.CENTER);
        card.addView(label, fullMargins(0, 0, 0, 8));
        String answerValue = q.correctIndex >= 0 && q.correctIndex < q.answers.length ? q.answers[q.correctIndex] : "—";
        TextView answer = text(answerValue, 25, TEXT, true);
        answer.setGravity(Gravity.CENTER);
        answer.setLineSpacing(dp(3), 1f);
        card.addView(answer, fullMargins(0, 0, 0, q.explanation.isEmpty() ? 10 : 8));
        if (!q.explanation.isEmpty()) {
            View divider = new View(this);
            divider.setBackgroundColor(BORDER);
            card.addView(divider, new LinearLayout.LayoutParams(dp(110), dp(1)));
            TextView explanation = text(q.explanation, 12, MUTED, false);
            explanation.setGravity(Gravity.CENTER);
            card.addView(explanation, fullMargins(0, 9, 0, 10));
        }
        
        pointsText.setTextColor(activeJokerSelected ? RED : MUTED);
        pointsText.setGravity(Gravity.CENTER);
        card.addView(pointsText);
    }

    private void flipToAnswer(LinearLayout card, Question q, TextView pointsText) {
        playFlipFeedback();
        if (!effectsEnabled) { renderAnswerFace(card, q, pointsText); return; }
        card.animate().rotationY(90f).setDuration(160).withEndAction(() -> {
            card.setRotationY(-90f);
            renderAnswerFace(card, q, pointsText);
            card.animate().rotationY(0f).setDuration(180).start();
        }).start();
    }

    private void flipToQuestion(LinearLayout card, Question q, TextView pointsText) {
        playFlipFeedback();
        if (!effectsEnabled) { renderQuestionFace(card, q, pointsText); return; }
        card.animate().rotationY(-90f).setDuration(160).withEndAction(() -> {
            card.setRotationY(90f);
            renderQuestionFace(card, q, pointsText);
            card.animate().rotationY(0f).setDuration(180).start();
        }).start();
    }

    private void pulse(View v) {
        if (!effectsEnabled) return;
        v.animate().scaleX(1.04f).scaleY(1.04f).setDuration(100).withEndAction(() ->
                v.animate().scaleX(1f).scaleY(1f).setDuration(120).start()).start();
    }

    private void incrementCategory(Map<String, Integer> map, String category) {
        map.put(category, map.containsKey(category) ? map.get(category) + 1 : 1);
    }

    private String bestCategory(Map<String, Integer> map) {
        if (map.isEmpty()) return "—";
        String best = null;
        int max = -1;
        for (Map.Entry<String, Integer> e : map.entrySet()) {
            if (e.getValue() > max) {
                max = e.getValue();
                best = e.getKey();
            }
        }
        for (Category c : CATEGORIES) if (c.id.equals(best)) return c.name;
        return "—";
    }

    private void resetGame() {
        selectDuelQuestions();
        gameMode = GameMode.DUEL;
        gameActive = true;
        activeQuestionKey = "";
        activeQuestionRevealed = false;
        activeJokerSelected = false;
        activeSpecialId = "";
        activeSpecialCorrectCount = 0;
        activeSpecialMistakes = 0;
        activeSpecialHintIndex = 0;
        activeSpecialJokerSelected = false;
        activeSpecialInteractionStarted = false;
        activeSpecialAnswersVisible = false;
        activeRankOrder.clear();
        specialPlaysUsed.clear();
        usedSpecialQuestionIds.clear();
        clearQuestionTimerState();
        inSuddenDeath = false;
        suddenStarted = false;
        tieChoicePending = false;
        scoreA = 0;
        scoreB = 0;
        currentTeam = 0;
        answered = 0;
        jokerAvailableA = true;
        jokerAvailableB = true;
        correctA = correctB = 0;
        wrongA = wrongB = 0;
        maxHitA = maxHitB = 0;
        categoryCorrectA.clear();
        categoryCorrectB.clear();
        finalIntroShown = false;
        suddenIndex = 0;
        suddenRound = 1;
        suddenTeam = 0;
        suddenResultA = null;
        suddenResultB = null;
        suddenMode = "";
        activeSuddenQuestionId = "";
        suddenClosestInputA = "";
        suddenClosestInputB = "";
        suddenClosestResolved = false;
        suddenClosestValueA = 0d;
        suddenClosestValueB = 0d;
        suddenUsedQuestionIds.clear();
        used.clear();
    }

    private String currentTeamName() {
        return currentTeam == 0 ? teamA : teamB;
    }

    private String cleanName(String name, String fallback) {
        if (name == null) return fallback;
        String v = name.trim();
        return v.isEmpty() ? fallback : v;
    }

    private void resetTeamNamesToDefault() {
        teamA = DEFAULT_TEAM_A;
        teamB = DEFAULT_TEAM_B;
    }

    // Map.getOrDefault needs Android 7 (API 24); minSdk is 23.
    private static int countOf(Map<String, Integer> map, String key) {
        Integer value = map.get(key);
        return value == null ? 0 : value;
    }

    private String key(String category, int multiplier) {
        return category + "_" + multiplier;
    }

    private void confirmDiscardThen(Runnable action) {
        try {
            new AlertDialog.Builder(this)
                    .setTitle("Νέο παιχνίδι;")
                    .setMessage("Θέλεις να διαγράψεις την τρέχουσα παρτίδα;")
                    .setNegativeButton("Ακύρωση", null)
                    .setPositiveButton("Ναι, νέο παιχνίδι", (dialog, which) -> action.run())
                    .show();
        } catch (Throwable t) { action.run(); }
    }

    private Category categoryById(String id) {
        for (Category c : CATEGORIES) if (c.id.equals(id)) return c;
        return CATEGORIES[0];
    }

    private void saveGameState() {
        if (prefs == null || !gameActive) return;
        try {
            SharedPreferences.Editor e = prefs.edit();
            e.putBoolean("game_active", true);
            e.putString("game_duel_selection", new JSONArray(duelSelection).toString());
            e.putString("game_mode", gameMode == GameMode.CHEESE ? "cheese" : gameMode == GameMode.CLASSIC ? "classic" : "duel");
            if (gameMode == GameMode.CHEESE) saveCheese(e);
            e.putString("game_tie_rule", tieRule == TieRule.DRAW ? "draw" : "sudden_death");
            e.putBoolean("game_tie_choice_pending", tieChoicePending);
            e.putString("game_team_a", teamA);
            e.putString("game_team_b", teamB);
            e.putInt("game_score_a", scoreA);
            e.putInt("game_score_b", scoreB);
            e.putInt("game_current_team", currentTeam);
            e.putInt("game_answered", answered);
            e.putLong("game_timer_remaining_ms", questionTimerRemainingMs);
            e.putBoolean("game_timer_expired", questionTimerExpired);

            // Duel state.
            e.putBoolean("game_joker_a", jokerAvailableA);
            e.putBoolean("game_joker_b", jokerAvailableB);
            e.putInt("game_correct_a", correctA);
            e.putInt("game_correct_b", correctB);
            e.putInt("game_wrong_a", wrongA);
            e.putInt("game_wrong_b", wrongB);
            e.putInt("game_max_hit_a", maxHitA);
            e.putInt("game_max_hit_b", maxHitB);
            e.putStringSet("game_used", new HashSet<>(used));
            e.putString("game_cat_a", mapToJson(categoryCorrectA));
            e.putString("game_cat_b", mapToJson(categoryCorrectB));
            e.putBoolean("game_final_intro", finalIntroShown);
            e.putString("game_active_question", activeQuestionKey == null ? "" : activeQuestionKey);
            e.putBoolean("game_active_revealed", activeQuestionRevealed);
            e.putBoolean("game_active_joker_selected", activeJokerSelected);
            e.putStringSet("game_special_enabled", new HashSet<>(enabledSpecialTypes));
            e.putString("game_special_plays", mapToJson(specialPlaysUsed));
            e.putStringSet("game_special_used_ids", new HashSet<>(usedSpecialQuestionIds));
            e.putString("game_active_special", activeSpecialId == null ? "" : activeSpecialId);
            e.putInt("game_special_correct_count", activeSpecialCorrectCount);
            e.putInt("game_special_mistakes", activeSpecialMistakes);
            e.putInt("game_special_hint", activeSpecialHintIndex);
            e.putBoolean("game_special_joker_selected", activeSpecialJokerSelected);
            e.putBoolean("game_special_interaction", activeSpecialInteractionStarted);
            e.putBoolean("game_special_answers_visible", activeSpecialAnswersVisible);
            e.putString("game_special_rank_order", stringListToJson(activeRankOrder));

            // Classic state.
            e.putString("game_classic_progress_a", mapToJson(classicProgressA));
            e.putString("game_classic_progress_b", mapToJson(classicProgressB));
            e.putStringSet("game_classic_used", new HashSet<>(classicUsedQuestionIds));
            Set<String> difficultyStrings = new HashSet<>();
            for (Integer d : classicDifficulties) difficultyStrings.add(String.valueOf(d));
            e.putStringSet("game_classic_difficulties", difficultyStrings);
            e.putString("game_classic_schedule", intListToCsv(classicDifficultySchedule));
            e.putString("game_classic_option1", classicOption1 == null ? "" : classicOption1);
            e.putString("game_classic_option2", classicOption2 == null ? "" : classicOption2);
            e.putString("game_classic_active_question", activeClassicQuestionId == null ? "" : activeClassicQuestionId);
            e.putBoolean("game_classic_revealed", classicQuestionRevealed);
            e.putBoolean("game_classic_ever_revealed", classicAnswerEverRevealed);
            e.putInt("game_classic_phase", classicQuestionPhase);
            e.putBoolean("game_classic_steal_a", stealAvailableA);
            e.putBoolean("game_classic_steal_b", stealAvailableB);
            e.putBoolean("game_classic_shuffle_a", shuffleAvailableA);
            e.putBoolean("game_classic_shuffle_b", shuffleAvailableB);
            e.putBoolean("game_classic_change_a", changeQuestionAvailableA);
            e.putBoolean("game_classic_change_b", changeQuestionAvailableB);
            e.putInt("game_classic_played_a", classicQuestionsPlayedA);
            e.putInt("game_classic_played_b", classicQuestionsPlayedB);
            e.putBoolean("game_classic_final_chance", classicFinalChance);
            e.putInt("game_classic_finisher", classicFinisher);
            e.putInt("game_classic_target_per_category", classicTargetPerCategory);

            // Shared Sudden Death state.
            e.putBoolean("game_in_sudden", inSuddenDeath);
            e.putBoolean("game_sudden_started", suddenStarted);
            e.putInt("game_sudden_index", suddenIndex);
            e.putInt("game_sudden_round", suddenRound);
            e.putInt("game_sudden_team", suddenTeam);
            e.putInt("game_sudden_result_a", nullableBooleanToInt(suddenResultA));
            e.putInt("game_sudden_result_b", nullableBooleanToInt(suddenResultB));
            e.putBoolean("game_sudden_revealed", suddenQuestionRevealed);
            e.putString("game_sudden_mode", suddenMode == null ? "" : suddenMode);
            e.putString("game_sudden_active_question", activeSuddenQuestionId == null ? "" : activeSuddenQuestionId);
            e.putString("game_sudden_closest_a", suddenClosestInputA == null ? "" : suddenClosestInputA);
            e.putString("game_sudden_closest_b", suddenClosestInputB == null ? "" : suddenClosestInputB);
            e.putBoolean("game_sudden_closest_resolved", suddenClosestResolved);
            e.putString("game_sudden_closest_value_a", Double.toString(suddenClosestValueA));
            e.putString("game_sudden_closest_value_b", Double.toString(suddenClosestValueB));
            e.putStringSet("game_sudden_used_ids", new HashSet<>(suddenUsedQuestionIds));
            e.apply();
        } catch (Throwable ignored) { }
    }

    private boolean loadSavedGame() {
        if (prefs == null || !prefs.getBoolean("game_active", false)) return false;
        try {
            gameActive = true;
            gameMode = "cheese".equals(prefs.getString("game_mode", "duel")) ? GameMode.CHEESE : "classic".equals(prefs.getString("game_mode", "duel")) ? GameMode.CLASSIC : GameMode.DUEL;
            if (gameMode == GameMode.CHEESE) loadCheese();
            if (gameMode == GameMode.DUEL && prefs.contains("game_duel_selection")) restoreDuelQuestions(new JSONArray(prefs.getString("game_duel_selection", "[]")));
            tieRule = "draw".equals(prefs.getString("game_tie_rule", "sudden_death")) ? TieRule.DRAW : TieRule.SUDDEN_DEATH;
            tieChoicePending = prefs.getBoolean("game_tie_choice_pending", false);
            teamA = prefs.getString("game_team_a", DEFAULT_TEAM_A);
            teamB = prefs.getString("game_team_b", DEFAULT_TEAM_B);
            scoreA = prefs.getInt("game_score_a", 0);
            scoreB = prefs.getInt("game_score_b", 0);
            currentTeam = prefs.getInt("game_current_team", 0);
            answered = prefs.getInt("game_answered", 0);
            questionTimerRemainingMs = Math.max(0L, prefs.getLong("game_timer_remaining_ms", 0L));
            questionTimerExpired = prefs.getBoolean("game_timer_expired", false);
            questionTimerView = null;
            pauseQuestionTimer();

            jokerAvailableA = prefs.getBoolean("game_joker_a", true);
            jokerAvailableB = prefs.getBoolean("game_joker_b", true);
            correctA = prefs.getInt("game_correct_a", 0);
            correctB = prefs.getInt("game_correct_b", 0);
            wrongA = prefs.getInt("game_wrong_a", 0);
            wrongB = prefs.getInt("game_wrong_b", 0);
            maxHitA = prefs.getInt("game_max_hit_a", 0);
            maxHitB = prefs.getInt("game_max_hit_b", 0);
            used.clear();
            Set<String> savedUsed = prefs.getStringSet("game_used", null);
            if (savedUsed != null) used.addAll(new HashSet<>(savedUsed));
            categoryCorrectA.clear();
            categoryCorrectB.clear();
            jsonToMap(prefs.getString("game_cat_a", "{}"), categoryCorrectA);
            jsonToMap(prefs.getString("game_cat_b", "{}"), categoryCorrectB);
            finalIntroShown = prefs.getBoolean("game_final_intro", false);
            activeQuestionKey = prefs.getString("game_active_question", "");
            activeQuestionRevealed = prefs.getBoolean("game_active_revealed", false);
            activeJokerSelected = prefs.getBoolean("game_active_joker_selected", false);
            enabledSpecialTypes.clear();
            Set<String> savedSpecialTypes = prefs.getStringSet("game_special_enabled", null);
            if (savedSpecialTypes != null) enabledSpecialTypes.addAll(new HashSet<>(savedSpecialTypes));
            // v0.10.3 migration: Multiple Choice no longer exists as a Duel Special.
            enabledSpecialTypes.remove("multiple");
            specialPlaysUsed.clear();
            jsonToMap(prefs.getString("game_special_plays", "{}"), specialPlaysUsed);
            usedSpecialQuestionIds.clear();
            Set<String> savedSpecialIds = prefs.getStringSet("game_special_used_ids", null);
            if (savedSpecialIds != null) usedSpecialQuestionIds.addAll(new HashSet<>(savedSpecialIds));
            activeSpecialId = prefs.getString("game_active_special", "");
            SpecialQuestion restoredSpecial = specialQuestionById.get(activeSpecialId);
            if (restoredSpecial != null && "multiple".equals(restoredSpecial.type)) activeSpecialId = "";
            activeSpecialCorrectCount = Math.max(0, prefs.getInt("game_special_correct_count", 0));
            activeSpecialMistakes = Math.max(0, prefs.getInt("game_special_mistakes", 0));
            activeSpecialHintIndex = Math.max(0, prefs.getInt("game_special_hint", 0));
            activeSpecialJokerSelected = prefs.getBoolean("game_special_joker_selected", false);
            activeSpecialInteractionStarted = prefs.getBoolean("game_special_interaction", false);
            activeSpecialAnswersVisible = prefs.getBoolean("game_special_answers_visible", false);
            activeRankOrder.clear();
            jsonToStringList(prefs.getString("game_special_rank_order", "[]"), activeRankOrder);

            classicProgressA.clear();
            classicProgressB.clear();
            jsonToMap(prefs.getString("game_classic_progress_a", "{}"), classicProgressA);
            jsonToMap(prefs.getString("game_classic_progress_b", "{}"), classicProgressB);
            classicUsedQuestionIds.clear();
            Set<String> classicUsed = prefs.getStringSet("game_classic_used", null);
            if (classicUsed != null) classicUsedQuestionIds.addAll(new HashSet<>(classicUsed));
            classicDifficulties.clear();
            Set<String> ds = prefs.getStringSet("game_classic_difficulties", null);
            if (ds != null) {
                for (String value : ds) {
                    try { int d = Integer.parseInt(value); if (d >= 1 && d <= 3) classicDifficulties.add(d); } catch (Throwable ignored) { }
                }
            }
            if (classicDifficulties.isEmpty()) { classicDifficulties.add(1); classicDifficulties.add(2); classicDifficulties.add(3); }
            classicDifficultySchedule.clear();
            csvToIntList(prefs.getString("game_classic_schedule", ""), classicDifficultySchedule);
            if (classicDifficultySchedule.isEmpty()) ensureClassicDifficultySchedule(40);
            classicOption1 = prefs.getString("game_classic_option1", "");
            classicOption2 = prefs.getString("game_classic_option2", "");
            activeClassicQuestionId = prefs.getString("game_classic_active_question", "");
            classicQuestionRevealed = prefs.getBoolean("game_classic_revealed", false);
            classicAnswerEverRevealed = prefs.getBoolean("game_classic_ever_revealed", classicQuestionRevealed);
            classicQuestionPhase = prefs.getInt("game_classic_phase", 0);
            stealAvailableA = prefs.getBoolean("game_classic_steal_a", true);
            stealAvailableB = prefs.getBoolean("game_classic_steal_b", true);
            shuffleAvailableA = prefs.getBoolean("game_classic_shuffle_a", true);
            shuffleAvailableB = prefs.getBoolean("game_classic_shuffle_b", true);
            changeQuestionAvailableA = prefs.getBoolean("game_classic_change_a", true);
            changeQuestionAvailableB = prefs.getBoolean("game_classic_change_b", true);
            classicTargetPerCategory = 2;
            classicQuestionsPlayedA = Math.max(0, prefs.getInt("game_classic_played_a", 0));
            classicQuestionsPlayedB = Math.max(0, prefs.getInt("game_classic_played_b", 0));
            classicFinalChance = prefs.getBoolean("game_classic_final_chance", false);
            classicFinisher = prefs.getInt("game_classic_finisher", -1);

            inSuddenDeath = prefs.getBoolean("game_in_sudden", false);
            suddenStarted = prefs.getBoolean("game_sudden_started", false);
            suddenIndex = prefs.getInt("game_sudden_index", 0);
            suddenRound = Math.max(1, prefs.getInt("game_sudden_round", 1));
            suddenTeam = prefs.getInt("game_sudden_team", 0);
            suddenResultA = intToNullableBoolean(prefs.getInt("game_sudden_result_a", -1));
            suddenResultB = intToNullableBoolean(prefs.getInt("game_sudden_result_b", -1));
            suddenQuestionRevealed = prefs.getBoolean("game_sudden_revealed", false);
            suddenMode = prefs.getString("game_sudden_mode", "");
            activeSuddenQuestionId = prefs.getString("game_sudden_active_question", "");
            suddenClosestInputA = prefs.getString("game_sudden_closest_a", "");
            suddenClosestInputB = prefs.getString("game_sudden_closest_b", "");
            suddenClosestResolved = prefs.getBoolean("game_sudden_closest_resolved", false);
            try { suddenClosestValueA = Double.parseDouble(prefs.getString("game_sudden_closest_value_a", "0")); } catch (Throwable ignored) { suddenClosestValueA = 0d; }
            try { suddenClosestValueB = Double.parseDouble(prefs.getString("game_sudden_closest_value_b", "0")); } catch (Throwable ignored) { suddenClosestValueB = 0d; }
            suddenUsedQuestionIds.clear();
            Set<String> suddenUsed = prefs.getStringSet("game_sudden_used_ids", null);
            if (suddenUsed != null) suddenUsedQuestionIds.addAll(new HashSet<>(suddenUsed));

            if (currentTeam < 0 || currentTeam > 1) { clearSavedGame(); return false; }
            if (gameMode == GameMode.DUEL && (answered < 0 || answered > totalDuelQuestions())) { clearSavedGame(); return false; }
            if (gameMode == GameMode.CLASSIC) {
                scoreA = classicTotal(0);
                scoreB = classicTotal(1);
            }
            return true;
        } catch (Throwable t) {
            clearSavedGame();
            return false;
        }
    }

    private void clearSavedGame() {
        clearQuestionTimerState();
        gameActive = false;
        activeQuestionKey = "";
        activeQuestionRevealed = false;
        activeJokerSelected = false;
        inSuddenDeath = false;
        suddenStarted = false;
        tieChoicePending = false;
        if (prefs == null) return;
        try {
            SharedPreferences.Editor e = prefs.edit();
            for (String k : prefs.getAll().keySet()) if (k.startsWith("game_")) e.remove(k);
            e.apply();
        } catch (Throwable ignored) { }
    }

    private String mapToJson(Map<String, Integer> map) {
        JSONObject o = new JSONObject();
        try { for (Map.Entry<String, Integer> entry : map.entrySet()) o.put(entry.getKey(), entry.getValue()); }
        catch (Throwable ignored) { }
        return o.toString();
    }

    private void jsonToMap(String json, Map<String, Integer> out) {
        try {
            JSONObject o = new JSONObject(json == null ? "{}" : json);
            java.util.Iterator<String> keys = o.keys();
            while (keys.hasNext()) {
                String k = keys.next();
                out.put(k, o.optInt(k, 0));
            }
        } catch (Throwable ignored) { }
    }

    private String intListToCsv(List<Integer> values) {
        StringBuilder out = new StringBuilder();
        for (Integer value : values) {
            if (out.length() > 0) out.append(',');
            out.append(value == null ? 0 : value);
        }
        return out.toString();
    }

    private void csvToIntList(String csv, List<Integer> out) {
        if (csv == null || csv.trim().isEmpty()) return;
        for (String part : csv.split(",")) {
            try {
                int value = Integer.parseInt(part.trim());
                if (value >= 1 && value <= 3) out.add(value);
            } catch (Throwable ignored) { }
        }
    }

    private String stringListToJson(List<String> values) {
        JSONArray a = new JSONArray();
        try { for (String value : values) a.put(value); } catch (Throwable ignored) { }
        return a.toString();
    }

    private void jsonToStringList(String json, List<String> out) {
        try {
            JSONArray a = new JSONArray(json == null ? "[]" : json);
            for (int i = 0; i < a.length(); i++) out.add(a.getString(i));
        } catch (Throwable ignored) { }
    }

    private int nullableBooleanToInt(Boolean value) {
        if (value == null) return -1;
        return value ? 1 : 0;
    }

    private Boolean intToNullableBoolean(int value) {
        if (value < 0) return null;
        return value == 1;
    }

    private void showFatal(Throwable e) {
        TextView t = new TextView(this);
        t.setText("Δεν μπόρεσε να φορτώσει το ΚΟΥΙΖΑΡΑ.\n\n" + e.getClass().getSimpleName() + ": " + e.getMessage());
        t.setTextSize(16);
        t.setTextColor(TEXT == 0 ? Color.DKGRAY : TEXT);
        t.setPadding(dp(24), dp(50), dp(24), dp(24));
        t.setBackgroundColor(BG == 0 ? Color.WHITE : BG);
        setContentView(t);
    }

    // ---------- LAYOUT PARAM HELPERS ----------

    private View withMargins(View view, int l, int t, int r, int b) {
        view.setLayoutParams(fullMargins(l, t, r, b));
        return view;
    }

    private LinearLayout.LayoutParams fullMargins(int l, int t, int r, int b) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(dp(l), dp(t), dp(r), dp(b));
        return lp;
    }

    private LinearLayout.LayoutParams weightedMargins(float weight, int l, int t, int r, int b) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, weight);
        lp.setMargins(dp(l), dp(t), dp(r), dp(b));
        return lp;
    }

    private LinearLayout.LayoutParams centeredWrapMargins(int l, int t, int r, int b) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.gravity = Gravity.CENTER_HORIZONTAL;
        lp.setMargins(dp(l), dp(t), dp(r), dp(b));
        return lp;
    }

    private LinearLayout.LayoutParams centeredSize(int w, int h, int l, int t, int r, int b) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(w, h);
        lp.gravity = Gravity.CENTER_HORIZONTAL;
        lp.setMargins(dp(l), dp(t), dp(r), dp(b));
        return lp;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    // ---------- DATA TYPES ----------

    private interface BoolConsumer { void accept(boolean value); }

    private static class Category {
        final String id;
        final String icon;
        final String name;
        Category(String id, String icon, String name) {
            this.id = id;
            this.icon = icon;
            this.name = name;
        }
    }

    private static class ClassicQuestion {
        final String id;
        final String category;
        final int difficulty;
        final String question;
        final String answer;
        final String explanation;
        ClassicQuestion(String id, String category, int difficulty, String question, String answer, String explanation) {
            this.id = id;
            this.category = category;
            this.difficulty = difficulty;
            this.question = question;
            this.answer = answer;
            this.explanation = explanation;
        }
    }

    private static class SpecialQuestion {
        final String id;
        final String type;
        final String title;
        final String prompt;
        final String[] answers;
        final String[] hints;
        final String[] items;
        final String[] correctOrder;
        final String[] options;
        final int correctIndex;
        final String explanation;

        SpecialQuestion(String id, String type, String title, String prompt, String[] answers, String[] hints,
                        String[] items, String[] correctOrder, String[] options, int correctIndex, String explanation) {
            this.id = id;
            this.type = type;
            this.title = title;
            this.prompt = prompt;
            this.answers = answers;
            this.hints = hints;
            this.items = items;
            this.correctOrder = correctOrder;
            this.options = options;
            this.correctIndex = correctIndex;
            this.explanation = explanation;
        }

        static SpecialQuestion fromJson(JSONObject o) throws Exception {
            return new SpecialQuestion(
                    o.getString("id"), o.getString("type"), o.optString("title", "SPECIAL"), o.getString("prompt"),
                    jsonStrings(o.optJSONArray("answers")), jsonStrings(o.optJSONArray("hints")),
                    jsonStrings(o.optJSONArray("items")), jsonStrings(o.optJSONArray("correctOrder")),
                    jsonStrings(o.optJSONArray("options")), o.optInt("correctIndex", -1), o.optString("explanation", "")
            );
        }

        private static String[] jsonStrings(JSONArray a) throws Exception {
            if (a == null) return new String[0];
            String[] out = new String[a.length()];
            for (int i = 0; i < a.length(); i++) out[i] = a.getString(i);
            return out;
        }
    }

    private static class SuddenQuestion {
        final String id;
        final String type;
        final String prompt;
        final String[] options;
        final int correctIndex;
        final double numericAnswer;
        final String unit;
        final String explanation;
        SuddenQuestion(String id, String type, String prompt, String[] options, int correctIndex, double numericAnswer, String unit, String explanation) {
            this.id = id;
            this.type = type;
            this.prompt = prompt;
            this.options = options;
            this.correctIndex = correctIndex;
            this.numericAnswer = numericAnswer;
            this.unit = unit;
            this.explanation = explanation;
        }
    }

    private static class Question {
        final String category;
        final int multiplier;
        final String question;
        final String[] answers;
        final int correctIndex;
        final String explanation;
        Question(String category, int multiplier, String question, String[] answers, int correctIndex, String explanation) {
            this.category = category;
            this.multiplier = multiplier;
            this.question = question;
            this.answers = answers;
            this.correctIndex = correctIndex;
            this.explanation = explanation;
        }
        String correctAnswer() {
            if (correctIndex < 0 || correctIndex >= answers.length) return "—";
            return answers[correctIndex];
        }
    }
}
