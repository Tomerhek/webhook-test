package serverPackage;
import java.io.*;
import java.net.*;
import java.util.ArrayList;

/**
 * The Server class,
 * Contain all server parameters
 * @author Shahar & Tomer
 *
 */
public class Server implements Runnable
{
	int serverPort = 45000; // The port that this server is listening on
	ServerSocket serverSocket = null;  // Server sock that will listen for incoming connections
	Thread runningThread = null;
	boolean isStopped = false;
	ArrayList<ClientMirror> ClientsMirror= new ArrayList<ClientMirror>();
	int size=0;
	String namec;   // name of clients
	ArrayList<String> listnamec=new ArrayList<String>();
	private javax.swing.JTextArea ServerTXTA;
	private final String TO="to: ";
	private final String ALL="ALL";

	/**
	 * Server contractor
	 * @param port The server port number
	 * @param ServerTXTA The server text area pointer
	 */
	public Server(int port, javax.swing.JTextArea ServerTXTA)
	{
		this.serverPort = port;
		this.ServerTXTA= ServerTXTA;
	}
	
	/**
	 * The server running implementation for the thread 
	 */
	public void run()
	{
		synchronized (this)
		{
			this.runningThread = Thread.currentThread();
		}

		try
		{
			serverSocket = new ServerSocket(serverPort);   
		} catch (IOException e)
		{
			ServerTXTA.append("THIS PORT IS TAKEN");
			System.exit(1);
			
			//System.err.println("Cannot listen on this port.\n" + e.getMessage());
			//System.exit(1);
		}
		while (!isStopped())
		{
			Socket clientSocket = null;  // socket created by accept
			try
			{	
				clientSocket = serverSocket.accept(); // wait for a client to connect 
				namec=new BufferedReader(new InputStreamReader (clientSocket.getInputStream())).readLine();
				if(listnamec.contains(namec))
				{
					PrintWriter out =new PrintWriter(clientSocket.getOutputStream());
					out.print("server:Name is taken.\n".toUpperCase());
					out.close();
					clientSocket.close();
				}
				else
				{
				ClientMirror temp=new ClientMirror( namec ,clientSocket);
				ClientsMirror.add(temp);
				size++;
				listnamec.add(temp.name);
				Thread listener =new Thread (temp);
				listener.start();
				}

			} catch (IOException e)
			{
				if (isStopped())
				{
					ServerTXTA.setText("Server Stopped.");
					return;
				}
				throw new RuntimeException("Error accepting client connection", e);    //Accept failed
			}

		}
		ServerTXTA.append("Server Stopped.");
	}

	/**
	 * This method is for sending message to all clients
	 * @param msg The message 
	 * @param from The sender
	 * @param to The receiver
	 */
	public void broadcast (String msg, String from, String to)
	{
		ClientMirror temp;
		if(to==ALL)
		{
			for (int i = 0; i < size; i++) 
			{
				temp= (ClientMirror) (ClientsMirror.get(i));
				temp.out.println(from+": "+msg);
				temp.out.flush();		
			}
		}
		else
		{
			for (int i = 0; i < size; i++) 
			{
				temp= (ClientMirror) (ClientsMirror.get(i));
				if(temp.name.equals(to))
				{
					temp.out.println("<Private Message> from " + from+ ": " + msg);
					temp.out.flush();	
				}
				else if(temp.name.equals(from))
				{
					temp.out.println("<Private Message> to "+ to +": "+msg);
					temp.out.flush();
				}
			}
		}
	}
	/**
	 * This method updates the "online users" list in the GUI
	 */
	public void showConnected()
	{
		ClientMirror temp;
		String lnc=listnamec.toString();
		for (int i = 0; i < size; i++) 
		{
			temp= (ClientMirror) (ClientsMirror.get(i));
			temp.out.println("names:"+lnc);
			temp.out.flush();
		}
	}

	/**
	 * Checking if the server stopped working
	 * @return boolean
	 */
	private synchronized boolean isStopped()
	{
		return this.isStopped;
	}

	/**
	 * This method closing the server and all the connections to it
	 */
	public synchronized void stop()
	{
		this.isStopped = true;
		try
		{
			ServerTXTA.setText("Server Stopped.");
			this.serverSocket.close();
			ClientMirror temp;
			for (int i = 0; i < size; i++)
			{
				temp= (ClientMirror) (ClientsMirror.get(i));
				temp.out.close();
				temp.br.close();
				temp.clientSocket.close();
			}
		} catch (IOException e)
		{
			throw new RuntimeException("Error closing server", e);
		}
	}

	/**
	 * This class purpose is to open a listening thread for every new client
	 * <br>Every new thread will received and transmit the data for a specific client
	 * @author Tomer
	 *
	 */
	public class ClientMirror implements Runnable
	{
		private String name;
		private PrintWriter out;
		private Socket clientSocket;
		private BufferedReader br; 

		/**
		 * ClientMirror contractor
		 * @param name The client user name
		 * @param clientSocket The client socket
		 */
		public ClientMirror(String name, Socket clientSocket) 
		{
			this.name = name;
			this.clientSocket=clientSocket;
			
			try {
				this.out = new PrintWriter(clientSocket.getOutputStream());
				InputStreamReader in =new InputStreamReader(clientSocket.getInputStream());
				br=new BufferedReader(in);
			} catch (IOException e) 
			{
				// TODO Auto-generated catch block
				ServerTXTA.append("Input/Output Error");
			}
		}

		@Override
		public String toString() 
		{
			return "ClientMirror [name=" + name + ", out=" + out
					+ ", clientSocket=" + clientSocket + "]";
		}

		/**
		 * return the clientSocket
		 * @return return the clientSocket
		 */
		public Socket getClientSocket() 
		{
			return clientSocket;
		}

		public void setClientSocket(Socket clientSocket) 
		{
			this.clientSocket = clientSocket;
		}

		public String getName() 
		{
			return name;
		}
		public void setName(String name) 
		{
			this.name = name;
		}
		public PrintWriter getOut() 
		{
			return out;
		}
		public void setOut(PrintWriter out) 
		{
			this.out = out;
		}
		
		/**
		 * The ClientMirror running implementation for the thread
		 */
		@Override
		public void run() 
		{
			// TODO Auto-generated method stub
			showConnected();
			broadcast(this.name+" Connected!", "SERVER",ALL);
			ServerTXTA.append(this.name+" Connected! \n");
			String msg="";
			try {
				while ((msg=br.readLine())!=null)
				{
					if(msg.startsWith("Disconnect: "))
					{
						ServerTXTA.append(name+" left! \n");
						ClientsMirror.remove(this);
						size--;
						listnamec.remove(name); 
						showConnected();
						broadcast(this.name+" left!", "SERVER",ALL);
					}

					else
					{
						if (msg.contains(TO))
						{
							String msgd="",to="";
							msgd=msg.substring(0, msg.indexOf(TO));
							to=msg.substring(msg.indexOf(TO)+TO.length(),msg.length());
							broadcast(msgd,this.name,to);
						}
						else
						{
							broadcast(msg,this.name,ALL);
						}
					}
				}
			} catch (IOException e) 
			{
				// TODO Auto-generated catch block    
				ServerTXTA.append(name+" left!\n");
				ClientsMirror.remove(this);
				size--;
				listnamec.remove(name); 
				showConnected();
				broadcast(this.name+" left!", "SERVER",ALL);
			}
		}
	}
}
