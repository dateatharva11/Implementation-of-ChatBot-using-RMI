package server;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.server.RemoteRef;
import java.rmi.server.UnicastRemoteObject;
import java.util.Date;
import java.util.Vector;

import client.ChatClientIF;

public class ChatServer extends UnicastRemoteObject implements ChatServerIF {
	String line = "---------------------------------------------\n";
	private Vector<Chatter> chatters;
	private static final long serialVersionUID = 1L;
	
	//Constructor
	public ChatServer() throws RemoteException {
		super();
		chatters = new Vector<Chatter>(10, 1);
	}
	
	public static void main(String[] args) {
		startRMIRegistry();	
		String hostName = "localhost";
		String serviceName = "ChatBotService";
		
		if(args.length == 2){
			hostName = args[0];
			serviceName = args[1];
		}
		
		try{
			ChatServerIF hello = new ChatServer();
			Naming.rebind("rmi://" + hostName + "/" + serviceName, hello);
			System.out.println("ChatBot RMI Server is running...");
		}
		catch(Exception e){
			System.out.println("ChatBot had problems starting");
		}	
	}

	//start rmi registry
	public static void startRMIRegistry() {
		try{
			java.rmi.registry.LocateRegistry.createRegistry(1099);
			System.out.println("RMI Server ready");
		}
		catch(RemoteException e) {
			e.printStackTrace();
		}
	}
		
	//Remote methods
	
	//Return a message to client
	public String sayHello(String ClientName) throws RemoteException {
		System.out.println(ClientName + " sent a message");
		return "Hello " + ClientName + " from chatbot";
	}
	
	//Send message to client
	public void updateChat(String name, String nextPost) throws RemoteException {
		String message =  name + " : " + nextPost + "\n";
		sendToAll(message);
		String msg="";
		String x= nextPost.toLowerCase();
	    // System.out.println(x); 
	    if(x.contains("rider") && x.contains("benefits"))
	    {
	      msg ="[Server] : Rider Benefits make your life insurance policy more flexible either by adding useful features or extending coverage to situations that a standard policy might not allow. For example, an accelerated death benefit rider will let you take some of your death benefit early if you're diagnosed with a terminal illness.\n"; 
	    }
	    else if(x.contains("death") && x.contains("benefits"))
	    {
	    	msg ="[Server] : Life insurance death benefit is the amount of money the insurance company pays the designated beneficiaries upon the insured’s death, provided the policy was in force at the time of the incident.\n";
	    }	
	    else if(x.contains("policies")) 
	    {
	    	msg ="[Server] : Here are a few types of policies we offer \n Type 1: LIC's Bima Jyoti \n Type 2: LIC's Bachat Plus \n Type 3: LIC's New Endowment Plan \n Type 4: LIC's New Jeevan Anand \n Type 5: LIC's New Bima Bachat \n Type 6: LIC's Single Premium Endowment Plan \n Type 7: LIC's Jeevan Lakshya \n Type 8: LIC's Jeevan Labh \n Type 9: LIC's Aadhaar Stambh \n Type 10: LIC's Aadhaar Shila \n";
	    }
	    else if ((x.contains("process") || x.contains("steps"))&& x.contains("claim")) 
	    {
	    	msg ="[Server] : Step 1: OTP Authentication \n Step 2: Select type of claim \n Step 3: Upload required documents \n Step 4: Facial Recognition \n Step 5: Electronic Signature \n Step 6: Personalised Desk Verification \n Step 7: Money Transfered\n";
	    }
	    else {
	    	msg ="[Server] : Sorry I didn't understand, Please try again with a different question\n";
	    }
	    sendToAll(msg);
	}
	
	//Receive a new client remote reference
	@Override
	public void passIDentity(RemoteRef ref) throws RemoteException {	
		//System.out.println("\n" + ref.remoteToString() + "\n");
		try{
			System.out.println(line + ref.toString());
		}catch(Exception e){
			e.printStackTrace();
		}
	}

	
	//Receive a new client and display details to the console send on to register method
	@Override
	public void registerListener(String[] details) throws RemoteException {	
		System.out.println(new Date(System.currentTimeMillis()));
		System.out.println(details[0] + " has joined the chatbot server");
		System.out.println(details[0] + "'s hostname : " + details[1]);
		System.out.println(details[0] + "'s RMI service : " + details[2]);
		registerChatter(details);
	}

	
	//register the clients interface and store it in a reference for 
	//future messages to be sent to, ie other members messages of the chat session.
	//send a test message for confirmation / test connection
	private void registerChatter(String[] details){		
		try{
			ChatClientIF nextClient = ( ChatClientIF )Naming.lookup("rmi://" + details[1] + "/" + details[2]);
			
			chatters.addElement(new Chatter(details[0], nextClient));
			
			nextClient.messageFromServer("[Server] : Hello " + details[0] + " you are now connected to chatbot.\n");
			
			//sendToAll("[Server] : " + details[0] + " is connected to ChatBot.\n");
			
			updateUserList();		
		}
		catch(RemoteException | MalformedURLException | NotBoundException e){
			e.printStackTrace();
		}
	}
	
	//Update clients by remotely invoking its updateUserList RMI method
	private void updateUserList() {
		String[] currentUser = getUserList();	
		for(Chatter c : chatters){
			try {
				c.getClient().updateUserList(currentUser);
			} 
			catch (RemoteException e) {
				e.printStackTrace();
			}
		}	
	}
	

	//generate an array of users
	private String[] getUserList(){
		// generate an array of current users
		String[] allUsers = new String[chatters.size()];
		for(int i = 0; i< allUsers.length; i++){
			allUsers[i] = chatters.elementAt(i).getName();
		}
		return allUsers;
	}
	

	//Send a message
	public void sendToAll(String newMessage){	
		for(Chatter c : chatters){
			try {
				c.getClient().messageFromServer(newMessage);
			} 
			catch (RemoteException e) {
				e.printStackTrace();
			}
		}	
	}

	//remove a client from the list, notify everyone
	@Override
	public void leaveChat(String userName) throws RemoteException{
		
		for(Chatter c : chatters){
			if(c.getName().equals(userName)){
				System.out.println(line + userName + " left the chat session");
				System.out.println(new Date(System.currentTimeMillis()));
				chatters.remove(c);
				break;
			}
		}		
		if(!chatters.isEmpty()){
			updateUserList();
		}			
	}
	
	 //A method to send a private message to selected clients
	 //The integer array holds the indexes (from the chatters vector) 
	 //of the clients to send the message to 
	@Override
	public void sendPM(int[] privateGroup, String privateMessage) throws RemoteException{
		Chatter pc;
		for(int i : privateGroup){
			pc= chatters.elementAt(i);
			pc.getClient().messageFromServer(privateMessage);
		}
	}
	
}
