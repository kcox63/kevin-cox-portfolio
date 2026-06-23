public class Player {
    private String color;
    private String username;

    public Player(String username, String color){
        this.username = username;
        this.color = color;
    }

    public String getColor(){
        return color;
    }

    public String getUsername(){
        return username;
    }

    public void setUsername(String username){
        this.username = username;
    }

    public void setColor(String color){
        this.color = color;
    }

    @Override
    public String toString(){
        return username + " (" + color + ")";
    }
}
