import java.io.Serializable;

public class WinLossRecord implements Serializable{
    private static final long serialVersionUID = 1L;

    private int wins;
    private int losses;
    private int draws;

    public WinLossRecord(){
        wins = 0;
        losses = 0;
        draws = 0;
    }

    public void addWin(){
        wins++;
    }

    public int getWins(){
        return wins;
    }

    public void addLoss(){
        losses++;
    }

    public int getLosses(){
        return losses;
    }

    public void addDraw(){
        draws++;
    }

    public int getDraws(){
        return draws;
    }


    @Override
    public String toString(){
        return "Record: W - " + wins + " L - " + losses + " D - " + draws;
    }

}
