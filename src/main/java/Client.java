import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.Socket;
import java.util.function.Consumer;



public class Client extends Thread{

	
	Socket socketClient;
	
	ObjectOutputStream out;
	ObjectInputStream in;
	
	private Consumer<Serializable> callback;
	
	Client(Consumer<Serializable> call){
	
		callback = call;
	}
	
	public void run() {
		
		try {
		socketClient= new Socket("127.0.0.1",5555);
		socketClient.setTcpNoDelay(true);

	    out = new ObjectOutputStream(socketClient.getOutputStream());
	    in = new ObjectInputStream(socketClient.getInputStream());
		}
		catch(Exception e) {
			System.out.println("Client failed to connect.");
			e.printStackTrace();
			return;
		}
		
		while(true) {
			 
			try {
				Message message = (Message) in.readObject();
				callback.accept(message);
			}
			catch(Exception e) {
				e.printStackTrace();
				break;
			}
		}
	
    }
	
	public void send(Message message) {
		
		try {
			out.writeObject(message);
			out.flush();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}


}
