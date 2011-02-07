package edu.illinois.CS598rhk.services;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import android.app.Notification;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import edu.illinois.CS598rhk.interfaces.IBluetoothMessage;
import edu.illinois.CS598rhk.interfaces.IBluetoothService;
import edu.illinois.CS598rhk.models.BluetoothMessage;
import edu.illinois.CS598rhk.models.BluetoothNeighbor;
import edu.illinois.CS598rhk.models.ElectionMessage;
import edu.illinois.CS598rhk.models.WifiNeighbor;

public class BluetoothService extends Service implements IBluetoothService {	
	public static final String DISCOVERED_OVER_BLUETOOTH = "Wifi neighbor found over bluetooth";
	public static final String INTENT_TO_ADD_BLUETOOTH_NEIGHBOR = "add bluetooth neighbor";
	public static final String BLUETOOTH_NEIGHBOR_DATA = "bluetooth neighbor data";
	
	public static final String ACTION_ELECTED_FOR_WIFI_DISCOVERY = "action elected for wifi discovery";
	public static final String DELY_UNTIL_STARTING_WIFI_DISCOVERY = "delay until starting wifi discovery";
	
	final IBinder mBinder = new BluetoothBinder();
    
	@Override
    public IBinder onBind(Intent arg0) {
        return mBinder;
    }
    
    public class BluetoothBinder extends Binder {
        public IBluetoothService getService() {
            return BluetoothService.this;
        }
    }
    
    private BluetoothNeighbor myContactInfo;

    private ElectionHandler electionHandler;
    
    private List<IBluetoothMessage> messages;
    public static Set<BluetoothDevice> neighbors;
    private Iterator<BluetoothDevice> nextNeighbor;
    private BluetoothDevice currentNeighbor;
    private IBluetoothMessage currentMessage;
    
    private volatile boolean updatingNeighbors;
    
    
    private BluetoothAcceptThread acceptThread;
    private BluetoothControllerThread controllerThread;
    
    private volatile boolean controllerReady;
    private Object controllerMonitor;
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
    	// We could use setName() here to allow the user to change the device name
    	Notification notification = new Notification();
    	notification.tickerText = "BluetoothService";
    	startForeground(2, notification);
    	
    	myContactInfo = new BluetoothNeighbor();
    	messages = new ArrayList<IBluetoothMessage>();
    	electionHandler = new ElectionHandler();
    	
    	myContactInfo.name = BluetoothAdapter.getDefaultAdapter().getName();
    	myContactInfo.address = BluetoothAdapter.getDefaultAdapter().getAddress();
    	myContactInfo.progress = 0;
    	
    	sendToLogger("BluetoothService:" 
    			+ "\n\tName: " + myContactInfo.name
    			+ "\n\tAddress: " + myContactInfo.address);
    	
    	updatingNeighbors = false;
    	
    	neighbors = BluetoothAdapter.getDefaultAdapter().getBondedDevices();
    	nextNeighbor = neighbors.iterator();
    	
    	currentNeighbor = null;
    	currentMessage = null;
    	
    	acceptThread = new BluetoothAcceptThread();
    	controllerThread = new BluetoothControllerThread();
    	
    	controllerReady = false;
    	controllerMonitor = new Object();
    	
    	acceptThread.start();
    	
    	synchronized(controllerMonitor) {
    		controllerThread.start();
    		try {
    			while(!controllerReady)
    				controllerMonitor.wait();
    		} catch (InterruptedException e) {
    			// Do nothing
    		}
    	}
    	updateNeighbors();
    	
    	return START_STICKY;
    }
    
    @Override
    public void onDestroy() {
    	stopForeground(true);
		super.onDestroy();
    }
    
    public void updateScheduleProgress(long progress) {
    	synchronized(myContactInfo) {
    		sendToLogger("BluetoothService:"
    				+ "\n\tReceived progress update from " + myContactInfo.progress + " to " + progress);
    		myContactInfo.progress = progress;
    	}
    }
    
	@Override
	public void updateNeighborCount(int neighborCount) {
			sendToLogger("BluetoothService:" + "Updating neighborCount to "
					+ neighborCount);
			synchronized (myContactInfo) {
				myContactInfo.neighborCount = neighborCount;
			}
	}
    
	public void hostWifiDiscoveryElection() {
		electionHandler.hostWifiDiscoveryElection();
	}
	
    public synchronized void updateNeighbors() {
		if (!updatingNeighbors) {
			updatingNeighbors = true;
			broadcast(myContactInfo);
		}
    }
    
    public void broadcast(IBluetoothMessage message) {
    	sendToLogger("BluetooothService:"
    			+ "\n\tReceived message to broadcast"
    			+ "\n\tMessage:" + message);

    	messages.add(message);
    	synchronized(controllerMonitor) {
    		try {
    			while(!controllerReady)
    				controllerMonitor.wait(); // Might be problematic ... blocking calling thread
    		} catch(InterruptedException e) {
    			// Do nothing
    		}
    		
    		controllerMonitor.notify();
    	}
    }
    
    private synchronized IBluetoothMessage processNextMessage() {
    	IBluetoothMessage lastMessage = null;
    	if (currentMessage == null) {
    		if (!messages.isEmpty()) {
    			currentMessage = messages.remove(0);
    		}
    		else {
    			return null;
    		}
    	}
    	
    	if (nextNeighbor.hasNext()){
    		currentNeighbor = nextNeighbor.next();
    	}
    	else {
    		nextNeighbor = neighbors.iterator();
    		lastMessage = currentMessage;
    		currentMessage = null;
    		currentNeighbor = null;
    	}
    	return lastMessage;
    }
    
    private synchronized void handleLastBroadcastMessage(IBluetoothMessage lastMessage) {
    	if (lastMessage != null) {
			switch (lastMessage.getMessageType()) {
			case BluetoothMessage.BLUETOOTH_NEIGHBOR_HEADER:
				updatingNeighbors = false;
				break;
			case BluetoothMessage.INITIATE_ELECTION_HEADER:
				electionHandler.determineElectionWinner();
				break;
			case BluetoothMessage.ELECTION_WINNER_ANNOUNCEMENT_HEADER:
				electionHandler.announceElectionWinner();
				break;
			default:
				break;
			}
			handleLastBroadcastMessage(processNextMessage());
    	}
    }
    
    private synchronized void sendMessage(OutputStream outStream, IBluetoothMessage message) {
	    	sendToLogger("BluetoothService:"
	    			+ "\n\tConnected to Neighbor:\n\t" + currentNeighbor
	    			+ "\n\tSending message: " + message);
	    	writeBluetoothMessage(outStream, message);
    }
    
    public synchronized void newBluetoothNeighbor(BluetoothNeighbor neighbor) {
    	Intent i = new Intent(INTENT_TO_ADD_BLUETOOTH_NEIGHBOR);
    	i.putExtra(BLUETOOTH_NEIGHBOR_DATA, neighbor.pack());
    	sendBroadcast(i);
    	sendToLogger("BluetoothService:"
    			+ "\n\tFound Bluetooth Nieghbor:"
    			+ "\n\t" + neighbor);
    }
    
    public synchronized void newWifiNeighbor(WifiNeighbor neihgbor) {
    	sendToLogger("BluetoothService:"
    			+ "\n\tFound Wifi Nieghbor:"
    			+ "\n\t" + neihgbor);
    	Intent i = new Intent(WifiService.INTENT_TO_ADD_WIFI_NEIGHBOR);
    	i.putExtra(WifiService.WIFI_NEIGHBOR_DATA, neihgbor.pack());
    	i.putExtra(WifiService.INTENT_TO_ADD_WIFI_NEIGHBOR_SOURCE, DISCOVERED_OVER_BLUETOOTH);
    	sendBroadcast(i);
    }
	
	public synchronized void sendToLogger(String message) {
		   	Log.d(TAG,message);
			Intent intentToLog = new Intent(PowerManagement.ACTION_LOG_UPDATE);
			intentToLog.putExtra(PowerManagement.LOG_MESSAGE, message
					+ "\n\t-- [" + new Date().toGMTString() + "]");
			sendBroadcast(intentToLog);
	}

    //
    // Code taken BluetoothChatService API Demo
    //
    
 // Debugging
    private static final String TAG = "AdHocBluetoothService";

    // Name for the SDP record when creating server socket
    private static final String NAME = "BluetoothNeighborFinder";

    // Unique UUID for this application
    private static final UUID MY_UUID = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");

    // Member fields
    private final BluetoothAdapter mAdapter = BluetoothAdapter.getDefaultAdapter();

    /**
     * Indicate that the connection attempt failed and notify the UI Activity.
     */
    private synchronized void connectionFailed() {
    	sendToLogger("BluetoothService:"
        		+ "\n\tConnection Failed..."
        		+ "\n");    	
    }

    /**
     * Indicate that the connection was lost and notify the UI Activity.
     */
    private synchronized void connectionLost() {
    	sendToLogger("BluetoothService:"
        		+ "\n\tConnection Lost...");
    }

    public synchronized IBluetoothMessage handleReceivedMessage(IBluetoothMessage message) {
    	IBluetoothMessage response = null;
    	
    	if (message instanceof BluetoothNeighbor) {
			newBluetoothNeighbor((BluetoothNeighbor)message);
			response = myContactInfo;
    	}
		else if (message instanceof ElectionMessage) {
			ElectionMessage electionMessage = (ElectionMessage) message;
			switch (message.getMessageType()) {
			case BluetoothMessage.INITIATE_ELECTION_HEADER:
				response = electionHandler.getElectionResponse();
				break;
			case BluetoothMessage.ELECTION_WINNER_ANNOUNCEMENT_HEADER:
				response = electionHandler.getElectionAcknowledgement(electionMessage);
				break;
			}
		}	
		return response;
	}
    
    public synchronized void handleResponseMessage(IBluetoothMessage message) {
		
    	if (message instanceof BluetoothNeighbor) {
			newBluetoothNeighbor((BluetoothNeighbor)message);
    	}
    	else if (message instanceof WifiNeighbor) {
			newWifiNeighbor((WifiNeighbor)message);
		}
		else if (message instanceof ElectionMessage) {
			ElectionMessage electionMessage = (ElectionMessage)message;
			switch(message.getMessageType()) {
    		case BluetoothMessage.ELECTION_RESPONSE_HEADER:
    			electionHandler.handleElectionResponse(electionMessage);
    			break;
    		case BluetoothMessage.ACKNOWLEDGE_ELECTION_WINNER:
    			electionHandler.handleElectionAcknowledgement(electionMessage);
    			break;
    		}
		}
	}
    
    private class BluetoothAcceptThread extends Thread {
    	private final BluetoothServerSocket serverSocket;
    	
        public BluetoothAcceptThread() {
            BluetoothServerSocket tmp = null;

            // Create a new listening server socket
            try {
                tmp = mAdapter.listenUsingRfcommWithServiceRecord(NAME, MY_UUID);
            } catch (IOException e) {
                sendToLogger("BluetoothService:"
                		+ "\n\tFailed to construct acceptThread"
                		+ "\n");
                Log.e(TAG, "BluetoothAcceptThread() failed", e);
            }
            serverSocket = tmp;
        }
    	
    	@Override
		public void run() {
			while (true) {
				BluetoothSocket socket = null;
				
				try {
					socket = serverSocket.accept();
				} catch (IOException e) {
					sendToLogger("BluetoothService:" + "\n\tFailed in accept()");
					Log.e(TAG, "accept() failed", e);
				}

				// If a connection was accepted
				if (socket != null) {
					InputStream inStream = null;
					try {
						inStream = socket.getInputStream();
					} catch (IOException e) {
						sendToLogger("BluetoothService:" + "\n\tgetInputStream failed");
						Log.e(TAG, "getInputStream() failed", e);
					}
					
					if (inStream != null) {
						IBluetoothMessage message = readBluetoothMessage(inStream);
						
	            		sendToLogger("BluetoothService:"
	            				+ "\n\tReceived message from Neighbor:" 
	            				+ "\n\tName: " + socket.getRemoteDevice().getName()
	            				+ "\n\tAddress: " + socket.getRemoteDevice().getAddress()
	            				+ "\n\tMessage: " + message);
	            		
	            		
						IBluetoothMessage response = handleReceivedMessage(message);
						
						OutputStream outStream = null;
						try {
							outStream = socket.getOutputStream();
						} catch (IOException e) {
							sendToLogger("BluetoothService:" + "\n\tgetOutputStream failed");
							Log.e(TAG, "getOutputStream() failed", e);
						}
						
						if (outStream != null) {
							writeBluetoothMessage(outStream, response);
						}	
					}
				}
			}
		}
    }
    
    private class BluetoothAcceptHandlerThread extends Thread {
    	
    }
    
    private IBluetoothMessage readBluetoothMessage(InputStream inStream) {        
    	IBluetoothMessage message = null;

        byte[] buffer = new byte[4];

        boolean receivingMessage = false;
        int messageLength = 4;
        int bytesRead = 0;
        int bytes = 0;
        
    	while (true) {            
            try {
                // Read from the InputStream
                bytes = inStream.read(buffer, bytesRead, messageLength-bytesRead);

                if (bytes > 0) {
                	bytesRead += bytes;
                	if (!receivingMessage && bytesRead == 4) {
                		receivingMessage = true;
                		messageLength = ByteBuffer.wrap(buffer,0,4).getInt();
                		byte[] newBuffer = new byte[messageLength];
                		System.arraycopy(buffer, 0, newBuffer, 0, buffer.length);
                		buffer = newBuffer;
                	}
                	else if (receivingMessage && bytesRead == messageLength){
                		
                		receivingMessage = false;
                		messageLength = 4;
                		bytesRead = 0;
                		bytes = 0;
                		
                		message = BluetoothMessage.parse(buffer);
            			break;
                	}
                }
            } catch (IOException e) {
				sendToLogger("BleutoothService:"
						+ "\n\tDisconnected, read interrupted");
				Log.e(TAG, "disconnected", e);
				connectionLost();
                break;
            }
        }
    	
    	return message;
    }
    
    private void writeBluetoothMessage(OutputStream outStream, IBluetoothMessage message) {
    	try {
    		outStream.write((new BluetoothMessage(message)).getMessageWithHeader());
        } catch (IOException e) {
			Log.e(TAG, "Exception during write", e);
        }
    }
    
    private class BluetoothControllerThread extends Thread {
    	@Override
    	public void run() {
    		synchronized(controllerMonitor) {
    			controllerReady = true;
    			controllerMonitor.notify();
    		}
    		while(true) {
    			
    			// Do something based on state ? broadcasting or not?
    			
    			// if messages to send, connect and send message, wait for next signal ?
    			
				handleLastBroadcastMessage(processNextMessage());
				if (currentNeighbor != null) {
					BluetoothSocket socket = null;
    				try {
    					socket = currentNeighbor.createRfcommSocketToServiceRecord(MY_UUID);
					} catch (IOException e) {
                		Log.e(TAG, "createRFcommSocketServiceRecord() failed", e);
            		}	
   			
					mAdapter.cancelDiscovery();
					
					try {
						socket.connect();
					} catch (IOException e) {
						try {
							socket.close();
						} catch (IOException e2) {
							Log.e(TAG, "unable to close() socket during connection failure", e2);
						}
						connectionFailed();
						continue;
					} 
					
					if (socket != null) {    			
						sendToLogger("BluetoothService:"
							+ "\n\tBroadcasting current message");
						
    					sendToLogger("BluetoothService:"
							+ "\n\tRESPONSE TIMEOUT SET");
						
    					OutputStream outStream = null;
    					try {
    						outStream = socket.getOutputStream();
    					} catch(IOException e) {
    						Log.e(TAG, "getOutputStream() failed", e);
    					}

						if (outStream != null) {
    						sendMessage(outStream, currentMessage);
    						
    						InputStream inStream = null;
    						try {
    							inStream = socket.getInputStream();
    						} catch (IOException e) {
    							sendToLogger("BluetoothService:" + "\n\tgetInputStream failed");
    							Log.e(TAG, "getInputStream() failed", e);
    						}
    						
    						if (inStream != null) {
    							IBluetoothMessage message = readBluetoothMessage(inStream);
        						
        	            		sendToLogger("BluetoothService:"
        	            				+ "\n\tReceived message from Neighbor:" 
        	            				+ "\n\tName: " + socket.getRemoteDevice().getName()
        	            				+ "\n\tAddress: " + socket.getRemoteDevice().getAddress()
        	            				+ "\n\tMessage: " + message);
        	            		
        	            		
        						handleResponseMessage(message);
    						}	
			    		}
					}
				}
    			else {
					sendToLogger("BluetoothService:"
						+ "\n\tDone Broadcasting");
				}
    		}
    	}
    }

    private class ElectionHandler {
        private boolean hostingElection;
        private int myElectionResponseWindow;
        private List<Pair<BluetoothDevice, Integer>> electionResponses;
        
        private List<IBluetoothMessage> electionAnnouncements;
        private Iterator<IBluetoothMessage> nextAnnouncement;
        private boolean winnerAcknowledgedElection;
        
        public ElectionHandler() {
        	hostingElection = false;
        	myElectionResponseWindow = 1024;
        }
        
    	public synchronized void hostWifiDiscoveryElection() {
        	if (!hostingElection) {
        		hostingElection = true;
        		sendToLogger("BluetoothService:"
        				+ "\n\tStarting new Wifi discovery election!");
        		
        		electionResponses = new ArrayList<Pair<BluetoothDevice, Integer>>();
        		broadcast(new ElectionMessage(BluetoothMessage.INITIATE_ELECTION_HEADER));
        	}
        	else {
        		sendToLogger("BluetoothService:"
        				+ "\n\tAlready hosting election, request ignored.");
        	}
        }
    	
        public synchronized IBluetoothMessage getElectionResponse() {
        	Random rand = new Random();
        	int value = rand.nextInt(myElectionResponseWindow);
        	
        	sendToLogger("BluetoothService:"
        			+ "\n\tResponding to election with value " + String.valueOf(value));
        	
        	return new ElectionMessage(value);
        }
        
    	public synchronized void handleElectionResponse(ElectionMessage message) {
			synchronized (currentNeighbor) {
				sendToLogger("BluetoothService:" + "\n\tReceived value "
						+ message.value + " from neighbor "
						+ currentNeighbor.getAddress());
				electionResponses.add(new Pair<BluetoothDevice, Integer>(
						currentNeighbor, message.value));
			}
        }
        
        public synchronized void determineElectionWinner() {
    		sendToLogger("BluetoothService:"
    				+ "\n\tFinalizing Election...");
    		Collections.sort(electionResponses);
    		electionAnnouncements = new ArrayList<IBluetoothMessage>();
    		
    		long delay = 0;
    		synchronized(myContactInfo) {
    			delay = myContactInfo.progress;
    		}
    		for (Pair<BluetoothDevice, Integer> response : electionResponses) {
    			electionAnnouncements.add(new ElectionMessage(response.first.getAddress(), delay));
    		}

    		electionResponses = null;
    		nextAnnouncement = electionAnnouncements.iterator();
    		winnerAcknowledgedElection = false;
    		announceElectionWinner();
    	}
    	
    	private synchronized void announceElectionWinner() {
    		sendToLogger("BluetoothService:"
    				+ "\n\tAnnouncing Election Results...");

    		if (!winnerAcknowledgedElection) {
				if (nextAnnouncement.hasNext()) {
					broadcast(nextAnnouncement.next());
				} else {
					winnerElected();
				}
			}
    		else {
    			winnerElected();
    		}
    	}
    	
        public synchronized ElectionMessage getElectionAcknowledgement(ElectionMessage result) {
        	ElectionMessage response = new ElectionMessage(BluetoothMessage.ACKNOWLEDGE_ELECTION_WINNER);
        	
        	
        	String myAddress = "";
        	synchronized(myContactInfo) {
        		myAddress = myContactInfo.address;
        	}
        	if (myAddress.equals(result.winnerAddress)) {
        		response.value = 1;
        		myElectionResponseWindow = 1024;
        		
        		Intent i = new Intent(ACTION_ELECTED_FOR_WIFI_DISCOVERY);
    			i.putExtra(DELY_UNTIL_STARTING_WIFI_DISCOVERY, result.delayUntilWinnerStarts);
    			sendBroadcast(i);
    			
    			sendToLogger("BluetoothService:"
    					+ "\n\tResetting election window to 1024");
    		}
    		else {
    			response.value = 0;
    			myElectionResponseWindow = Math.max(64, myElectionResponseWindow / 2);
    			
    			sendToLogger("BluetoothService:"
    					+ "\n\tDecreasing electionWindow to " + myElectionResponseWindow);
    		}
    		return response;
    	}

		public synchronized void handleElectionAcknowledgement(ElectionMessage electionMessage) {
			if (electionMessage.value == 1) {
				winnerAcknowledgedElection = true;
			}
		}
    	
    	private synchronized void winnerElected() {    		
    		Intent i = new Intent(ACTION_ELECTED_FOR_WIFI_DISCOVERY);
    		
    		long delay = 0;
    		synchronized(myContactInfo) {
    			 delay = myContactInfo.progress;
    		}
    		if (winnerAcknowledgedElection) {
    			delay *= -1;
    		}
    		i.putExtra(DELY_UNTIL_STARTING_WIFI_DISCOVERY, delay);
    		sendBroadcast(i);
    		
    		hostingElection = false;
    		electionAnnouncements = null;
    		nextAnnouncement = null;
    	}
    }
    
    
    
    private class Pair<T,E extends Comparable<E>> implements Comparable<Pair<T,E>> {
    	public T first;
    	public E second;
    	
    	public Pair(T t, E e) {
    		this.first = t;
    		this.second = e;
    	}

		@Override
		public int compareTo(Pair<T,E> another) {
			return second.compareTo(another.second);
		}
    }

	@Override
	public void updateScheduleInfo(String[] schedule) {
		// TODO Auto-generated method stub
		
	}
}