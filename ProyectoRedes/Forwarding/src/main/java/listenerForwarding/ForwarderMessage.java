package listenerForwarding;

public class ForwarderMessage {
    String from;
    String to;
    String message;

    public ForwarderMessage(String from, String to, String message){
        this.from = from;
        this.to = to;
        this.message = message;
    }

    @Override
    public String toString() {
        return "From:" + from + "\nTo:" + to + "\nMsg:"+ message +"\n" + "EOF"+"\n";
     }
}
