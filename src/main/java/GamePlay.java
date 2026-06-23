public class GamePlay {
    private GameBoard board;
    private Player p1;
    private Player p2;
    private Player cTurn;
    private int movesWithoutProgress;
    private int forcedJumpRow;
    private int forcedJumpCol;
    private boolean resultRecorded;



    public GamePlay(Player p1, Player p2){
        this.board = new GameBoard();
        this.p1 = p1;
        this.p2 = p2;
        this.cTurn = p1;
        this.movesWithoutProgress = 0;
        this.resultRecorded = false;
        this.forcedJumpRow = -1;
        this.forcedJumpCol = -1;
    }

    public GameBoard getBoard(){
        return board;
    }

    public Player getP1(){
        return p1;
    }

    public Player getP2(){
        return p2;
    }

    public Player getCurrentTurn(){
        return cTurn;
    }

    public boolean isResultRecorded(){
        return resultRecorded;
    }

    public void setResultRecorded(boolean value){
        resultRecorded = value;
    }

    public boolean isForcedJumpActive(){
        return forcedJumpRow != -1 && forcedJumpCol != -1;
    }

    public int getForcedJumpRow(){
        return forcedJumpRow;
    }

    public int getForcedJumpCol(){
        return forcedJumpCol;
    }

    public String gameStatus(){
        if(movesWithoutProgress >= 40){
            return "DRAW";
        }
        return board.status();
    }

    public boolean makeMove(String username, int fRow, int fCol, int tRow, int tCol){
        if(!cTurn.getUsername().equals(username)){
            return false;
        }

        if(isForcedJumpActive()){
            if(fRow != forcedJumpRow || fCol != forcedJumpCol){
                return false;
            }
        }

        boolean moved = board.movePiece(fRow, fCol, tRow, tCol, cTurn.getColor());

        if(moved){
            if(board.wasLastMoveCapture() || board.wasLastMoveKing()){
                movesWithoutProgress = 0;
            } else {
                movesWithoutProgress++;
            }


            if(board.wasLastMoveCapture() && board.canContinueCapture(tRow, tCol, cTurn.getColor())){
                forcedJumpRow = tRow;
                forcedJumpCol = tCol;
            } else {
                forcedJumpRow = -1;
                forcedJumpCol = -1;

                if(cTurn == p1){
                    cTurn = p2;
                } else {
                    cTurn = p1;
                }
            }
        }

        return moved;
    }


}
