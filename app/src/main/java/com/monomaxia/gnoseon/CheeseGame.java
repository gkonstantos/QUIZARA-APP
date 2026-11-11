package com.monomaxia.gnoseon;

/** Android-independent rules for 4 Τυριά. Progress belongs to each team. */
public final class CheeseGame {
    public final int[][] progress = new int[2][6];
    public final boolean[] sabotage = {true, true};
    public int turn = 0;
    public int winner = -1;
    public int lastLeader = -1;
    public String phase = "board";
    public boolean questionOpen, attacking;
    public int category = -1;
    public int count(int team) { int n=0; for (int p:progress[team]) if(p==2)n++; return n; }
    public int owner(int c) { return progress[0][c]==2 ? 0 : progress[1][c]==2 ? 1 : -1; }
    public boolean canPick(int c, boolean attack) {
        if(c<0||c>=6||winner>=0||questionOpen||phase.equals("sudden"))return false;
        if(phase.equals("last"))return attack && sabotage[turn] && progress[lastLeader][c]==2;
        return attack ? sabotage[turn] && progress[1-turn][c]>0 : owner(c)<0;
    }
    public void begin(int c, boolean attack) {
        if(!canPick(c,attack))throw new IllegalStateException("Unavailable category");
        category=c; attacking=attack; questionOpen=true;
        if(attack)sabotage[turn]=false;
    }
    public void answer(boolean correct) {
        if(!questionOpen)throw new IllegalStateException("No open question");
        questionOpen=false;
        if(attacking) {
            if(correct)progress[1-turn][category]--;
            if(phase.equals("last")) {
                if(!correct){winner=lastLeader;phase="winner";return;}
                lastLeader=-1;phase="board";
            }
            turn=1-turn;
        } else {
            int answeringTeam=turn;
            if(correct)progress[answeringTeam][category]++;
            if(count(answeringTeam)>=4) {
                if(sabotage[1-answeringTeam]) { lastLeader=answeringTeam;turn=1-answeringTeam;phase="last"; }
                else {winner=answeringTeam;phase="winner";}
            } else {
                // Alternate teams after every normal question, even a correct answer.
                turn=1-answeringTeam;
            }
        }
        if(count(0)==3 && count(1)==3)phase="sudden";
        category=-1;attacking=false;
    }
    public void declineLastDance() {
        if(!phase.equals("last")||questionOpen)throw new IllegalStateException();
        winner=lastLeader;phase="winner";
    }
}
