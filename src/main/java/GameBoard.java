public class GameBoard {
    private int[][] board;
    private boolean lastMoveWasCapture;
    private boolean lastMoveWasKing;


    public GameBoard(){
        board = new int[8][8];
        lastMoveWasCapture = false;
        lastMoveWasKing = false;
        initializeBoard();
    }

    public void initializeBoard(){
        for(int i = 0; i < 8; i++){// row
            for(int j = 0; j < 8; j++){ // column
                board[i][j] = 0;
            }
        }

        for(int i = 0; i < 3; i++){
            for(int j = 0; j < 8; j++){
                if((i + j) % 2 == 1){
                    board[i][j] = 2;
                }
            }
        }

        for(int i = 5; i < 8; i++){
            for(int j = 0; j < 8; j++){
                if((i + j) % 2 == 1){
                    board[i][j] = 1;
                }
            }
        }
    }

    public int[][] getBoard(){
        return board;
    }

    public int piece(int row, int col){
        if(!inBounds(row, col)){
            return -1;
        }
        return board[row][col];
    }

    public boolean valid(int fRow, int fCol, int tRow, int tCol, String color){
        if(!inBounds(fRow, fCol) || !inBounds(tRow, tCol)){
            return false;
        }

        int cPiece = board[fRow][fCol];
        int destination = board[tRow][tCol];

        if(cPiece == 0 || destination != 0){
            return false;
        }

        if((tRow + tCol) % 2 == 0){
            return false;
        }

        if(!belongsToColor(cPiece, color)){
            return false;
        }

        int rDiff = tRow - fRow;
        int cDiff = Math.abs(tCol - fCol);

        if(Math.abs(rDiff) == 1 && cDiff == 1){
            if(anyCaptureAvailable(color)){
                return false;
            }
            return validRegularMove(cPiece, rDiff);
        }

        if(Math.abs(rDiff) == 2 && cDiff == 2){
            int mRow = (fRow + tRow) / 2;
            int mCol = (fCol + tCol) / 2;
            int jumpPiece = board[mRow][mCol];

            if(jumpPiece == 0){
                return false;
            }

            if(sameTeam(cPiece, jumpPiece)){
                return false;
            }

            return validCaptureMove(cPiece, rDiff);
        }
        return false;
    }

    public boolean movePiece(int fRow, int fCol, int tRow, int tCol, String color){
        lastMoveWasCapture = false;
        lastMoveWasKing = false;
        if(!valid(fRow, fCol, tRow, tCol, color)){
            return false;
        }

        int mPiece = board[fRow][fCol];
        if(Math.abs(tRow - fRow) == 2){
            int mRow = (fRow + tRow) / 2;
            int mCol = (fCol + tCol) / 2;
            board[mRow][mCol] = 0;
            lastMoveWasCapture = true;
        }

        board[tRow][tCol] = mPiece;
        board[fRow][fCol] = 0;
        kingPiece(tRow, tCol);

        return true;
    }

    public String status(){
        int rCount = 0;
        int bCount = 0;

        for(int i = 0; i < 8; i++) {
            for(int j = 0; j < 8; j++) {
                if(board[i][j] == 1 || board[i][j] == 3) {
                    rCount++;
                } else if(board[i][j] == 2 || board[i][j] == 4) {
                    bCount++;
                }
            }
        }

        if(rCount == 0) {
            return "BLACK_WINS";
        }
        if(bCount == 0) {
            return "RED_WINS";
        }

        boolean redCanMove = hasAnyMove("red");
        boolean blackCanMove = hasAnyMove("black");

        if(!redCanMove && !blackCanMove) {
            return "DRAW";
        }
        if(!redCanMove) {
            return "BLACK_WINS";
        }
        if(!blackCanMove) {
            return "RED_WINS";
        }

        return "ONGOING";
    }

    private boolean hasAnyMove(String color) {
        for(int i = 0; i < 8; i++) {
            for(int j = 0; j < 8; j++) {
                int piece = board[i][j];

                if(!belongsToColor(piece, color)) {
                    continue;
                }

                if(canPieceMove(i, j, color)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean canPieceMove(int row, int col, String color) {
        int piece = board[row][col];

        if(piece == 1) { // red regular
            return valid(row, col, row - 1, col - 1, color) ||
                    valid(row, col, row - 1, col + 1, color) ||
                    valid(row, col, row - 2, col - 2, color) ||
                    valid(row, col, row - 2, col + 2, color);
        }

        if(piece == 2) { // black regular
            return valid(row, col, row + 1, col - 1, color) ||
                    valid(row, col, row + 1, col + 1, color) ||
                    valid(row, col, row + 2, col - 2, color) ||
                    valid(row, col, row + 2, col + 2, color);
        }

        if(piece == 3 || piece == 4) { // kings
            return valid(row, col, row - 1, col - 1, color) ||
                    valid(row, col, row - 1, col + 1, color) ||
                    valid(row, col, row + 1, col - 1, color) ||
                    valid(row, col, row + 1, col + 1, color) ||
                    valid(row, col, row - 2, col - 2, color) ||
                    valid(row, col, row - 2, col + 2, color) ||
                    valid(row, col, row + 2, col - 2, color) ||
                    valid(row, col, row + 2, col + 2, color);
        }

        return false;
    }

    private boolean inBounds(int row, int col){
        return row >= 0 && row < 8 && col >= 0 && col < 8;
    }

    private boolean belongsToColor(int piece, String color){
        if(color == null){
            return false;
        }

        if(color.equalsIgnoreCase("red")){
            return piece == 1 || piece == 3;
        }

        if(color.equalsIgnoreCase("black")){
            return piece == 2 || piece == 4;
        }

        return false;
    }

    private boolean sameTeam(int p1, int p2){
        boolean p1Red = (p1 == 1 || p1 == 3);
        boolean p2Red = (p2 == 1 || p2 == 3);
        boolean p1Black = (p1 == 2 || p1 == 4);
        boolean p2Black = (p2 == 2 || p2 == 4);

        return(p1Red && p2Red) || (p1Black && p2Black);
    }

    private boolean validRegularMove(int piece, int rDiff){
        if(piece == 1){
            return rDiff == -1;
        }
        if(piece == 2){
            return rDiff == 1;
        }
        return Math.abs(rDiff) == 1;
    }

    private boolean validCaptureMove(int piece, int rDiff){
        if(piece == 1){
            return rDiff == -2;
        }
        if(piece == 2){
            return rDiff == 2;
        }
        return Math.abs(rDiff) == 2;
    }

    private void kingPiece(int row, int col){
        if(board[row][col] == 1 && row == 0){
            board[row][col] = 3;
            lastMoveWasKing = true;
        }else if(board[row][col] == 2 && row == 7){
            board[row][col] = 4;
            lastMoveWasKing = true;
        }
    }

    public String boardString(){
        StringBuilder sb = new StringBuilder();

        for(int i = 0; i < 8; i++){
            for(int j = 0; j < 8; j++){
                sb.append(board[i][j]).append(" ");
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    public boolean wasLastMoveCapture(){
        return lastMoveWasCapture;
    }

    public boolean wasLastMoveKing(){
        return lastMoveWasKing;
    }

    public boolean canContinueCapture(int row, int col, String color){
        int piece = board[row][col];

        if(!belongsToColor(piece, color)){
            return false;
        }

        if(piece == 1){
            return valid(row, col, row - 2, col - 2, color) ||
                    valid(row, col, row - 2, col + 2, color);
        }

        if(piece == 2){
            return valid(row, col, row + 2, col - 2, color) ||
                    valid(row, col, row + 2, col + 2, color);
        }

        if(piece == 3 || piece == 4){
            return valid(row, col, row - 2, col - 2, color) ||
                    valid(row, col, row - 2, col + 2, color) ||
                    valid(row, col, row + 2, col - 2, color) ||
                    valid(row, col, row + 2, col + 2, color);
        }

        return false;
    }

    private boolean pieceHasCapture(int row, int col, String color){
        int piece = board[row][col];

        if(!belongsToColor(piece, color)){
            return false;
        }

        if(piece == 1){ // red regular
            return validCaptureOnly(row, col, row - 2, col - 2, color) ||
                    validCaptureOnly(row, col, row - 2, col + 2, color);
        }

        if(piece == 2){ // black regular
            return validCaptureOnly(row, col, row + 2, col - 2, color) ||
                    validCaptureOnly(row, col, row + 2, col + 2, color);
        }

        if(piece == 3 || piece == 4){ // kings
            return validCaptureOnly(row, col, row - 2, col - 2, color) ||
                    validCaptureOnly(row, col, row - 2, col + 2, color) ||
                    validCaptureOnly(row, col, row + 2, col - 2, color) ||
                    validCaptureOnly(row, col, row + 2, col + 2, color);
        }

        return false;
    }

    private boolean anyCaptureAvailable(String color){
        for(int i = 0; i < 8; i++){
            for(int j = 0; j < 8; j++){
                if(pieceHasCapture(i, j, color)){
                    return true;
                }
            }
        }
        return false;
    }

    private boolean validCaptureOnly(int fRow, int fCol, int tRow, int tCol, String color){
        if(!inBounds(fRow, fCol) || !inBounds(tRow, tCol)){
            return false;
        }

        int cPiece = board[fRow][fCol];
        int destination = board[tRow][tCol];

        if(cPiece == 0 || destination != 0){
            return false;
        }

        if((tRow + tCol) % 2 == 0){
            return false;
        }

        if(!belongsToColor(cPiece, color)){
            return false;
        }

        int rDiff = tRow - fRow;
        int cDiff = Math.abs(tCol - fCol);

        if(Math.abs(rDiff) != 2 || cDiff != 2){
            return false;
        }

        int mRow = (fRow + tRow) / 2;
        int mCol = (fCol + tCol) / 2;
        int jumpPiece = board[mRow][mCol];

        if(jumpPiece == 0){
            return false;
        }

        if(sameTeam(cPiece, jumpPiece)){
            return false;
        }

        return validCaptureMove(cPiece, rDiff);
    }
}
