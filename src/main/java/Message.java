import java.io.Serializable;

public class Message implements Serializable {
    static final long serialVersionUID = 42L;

    private String sender;
    private String recipient;
    private String content;
    private String type;

    public Message(){
    }

    public Message(String sender, String recipient, String content, String type){
        this.sender = sender;
        this.recipient = recipient;
        this.content = content;
        this.type = type;
    }

    public String getSender(){
        return sender;
    }

    public String getRecipient(){
        return recipient;
    }

    public String getContent(){
        return content;
    }

    public String getType(){
        return type;
    }

    public void setSender(String sender){
        this.sender = sender;
    }

    public void setRecipient(String recipient){
        this.recipient = recipient;
    }

    public void setContent(String content){
        this.content = content;
    }

    public void setType(String type){
        this.type = type;
    }

    @Override
    public String toString(){
        if ("CHAT".equals(type)) {
            return sender + ": " + content;
        } else if ("MOVE".equals(type)) {
            return sender + " moved: " + content;
        } else if ("LOGIN".equals(type)) {
            return sender + " joined";
        } else if ("ERROR".equals(type)) {
            return "ERROR: " + content;
        } else if ("MATCH".equals(type)) {
            return "Match started: " + content;
        } else if ("GAME_STATUS".equals(type)) {
            return "Game Status: " + content;
        }
        return sender + " [" + type + "]: " + content;
    }
}
