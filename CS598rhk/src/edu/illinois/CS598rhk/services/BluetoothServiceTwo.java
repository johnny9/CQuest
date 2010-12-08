package edu.illinois.CS598rhk.services;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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

public class BluetoothServiceTwo extends Service implements IBluetoothService {	
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
            return BluetoothServiceTwo.this;
        }
    }
    
    private BluetoothNeighbor myContactInfo;

    private ElectionHandler electionHandler;
    
    private List<BluetoothMessage> messages;
    private Set<BluetoothDevice> neighbors;
    private Iterator<BluetoothDevice> nextNeighbor;
    private BluetoothDevice currentNeighbor;
    private BluetoothMessage currentMessage;
    
    private boolean broadcasting;
    private boolean updatingNeighbors;
    
    private static final long TIMEOUT_INTERVAL = 5000;
    private Timer timer;
    private BluetoothResponseTimeout responseTimeout;
    private BluetoothConnectingTimeout connectingTimeout;
    
    private BluetoothControllerThread controllerThread;
    private boolean otherThreadAlive;
    private Object threadLock;
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
    	// We could use setName() here to allow the user to change the device name
    	Notification notification = new Notification();
    	notification.tickerText = "BluetoothService";
    	startForeground(2, notification);
    	
    	myContactInfo = new BluetoothNeighbor();
    	messages = new ArrayList<BluetoothMessage>();
    	electionHandler = new ElectionHandler();
    	timer = new Timer();
    	
    	myContactInfo.name = BluetoothAdapter.getDefaultAdapter().getName();
    	myContactInfo.address = BluetoothAdapter.getDefaultAdapter().getAddress();
    	myContactInfo.progress = 0;
    	
    	sendToLogger("BluetoothService:" 
    			+ "\n\tName: " + myContactInfo.name
    			+ "\n\tAddress: " + myContactInfo.address
    			+ "\n");
    	
    	broadcasting = false;
    	updatingNeighbors = false;
    	
    	neighbors = BluetoothAdapter.getDefaultAdapter().getBondedDevices();
    	nextNeighbor = neighbors.iterator();
    	
    	currentNeighbor = null;
    	currentMessage = null;
    	
    	mStateString = "STATE_INIT";
    	threadLock = new Object();
    	otherThreadAlive = false;
    	controllerThread = new BluetoothControllerThread();
    	controllerThread.start();
    	
    	return START_STICKY;
    }
    
    @Override
    public void onDestroy() {
    	stopForeground(true);
		super.onDestroy();
    }
    
    public synchronized void updateScheduleProgress(long progress) {
    	synchronized(myContactInfo) {
    		sendToLogger("BluetoothService:"
    				+ "\n\tReceived progress update from " + myContactInfo.progress + " to " + progress
    				+ "\n");
    		myContactInfo.progress = progress;
    	}
    }
    
	@Override
	public synchronized void updateNeighborCount(int neighborCount) {
			sendToLogger("BluetoothService:" + "Updating neighborCount to "
					+ neighborCount + "\n");
			synchronized (myContactInfo) {
				myContactInfo.neighborCount = neighborCount;
			}
	}
    
	public synchronized void hostWifiDiscoveryElection() {
		electionHandler.hostWifiDiscoveryElection();
		if (mAcceptThread != null) { mAcceptThread.cancel(); mAcceptThread = null; }
	}
	
    public synchronized void updateNeighbors() {
		if (!updatingNeighbors) {
			updatingNeighbors = true;
			broadcast(myContactInfo);
			if (mAcceptThread != null) {mAcceptThread.cancel(); mAcceptThread = null;}
		}
    }
    
    public synchronized void broadcast(IBluetoothMessage message) {
    	sendToLogger("BluetooothService:"
    			+ "\n\tReceived message to broadcast"
    			+ "\n\tMessage:" + message
    			+ "\n");
    	broadcast(new BluetoothMessage(message));
    }
    
    private synchronized void broadcast(BluetoothMessage message) {
    	broadcasting = true;
    	synchronized(messages) {
	    	messages.add(message);
    	}
    	if (mAcceptThread != null) {mAcceptThread.cancel(); mAcceptThread = null;}
    }
    
    private synchronized BluetoothMessage processNextMessage() {
    	BluetoothMessage lastMessage = null;
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
    
    private synchronized void handleLastBroadcastMessage(BluetoothMessage lastMessage) {
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
    
    private synchronized void sendMessage(BluetoothMessage message) {
    	synchronized(messages) {
	    	if (message == null) {
	    		if (messages.isEmpty()) {
	    			sendToLogger("BluetoothService:"
	    					+ "\n\tTried to sendMessage on empty queue ... aborting send"
	    					+ "\n");
	    			return;
	    		}
	    		message = messages.get(0);
	    	}
	    	sendToLogger("BluetoothService:"
	    			+ "\n\tConnected to Neighbor:\n\t" + currentNeighbor
	    			+ "\n\tSending message: " + message
	    			+ "\n");
	    	write(message.getMessageWithHeader());
    	}
    }
    
    public synchronized void newBluetoothNeighbor(BluetoothNeighbor neighbor) {
    	Intent i = new Intent(INTENT_TO_ADD_BLUETOOTH_NEIGHBOR);
    	i.putExtra(BLUETOOTH_NEIGHBOR_DATA, neighbor.pack());
    	sendBroadcast(i);
    	sendToLogger("BluetoothService:"
    			+ "\n\tFound Bluetooth Nieghbor:"
    			+ "\n\t" + neighbor
    			+ "\n");
    }
    
    public synchronized void newWifiNeighbor(WifiNeighbor neihgbor) {
    	sendToLogger("BluetoothService:"
    			+ "\n\tFound Wifi Nieghbor:"
    			+ "\n\t" + neihgbor
    			+ "\n");
    	Intent i = new Intent(WifiService.INTENT_TO_ADD_WIFI_NEIGHBOR);
    	i.putExtra(WifiService.WIFI_NEIGHBOR_DATA, neihgbor.pack());
    	i.putExtra(WifiService.INTENT_TO_ADD_WIFI_NEIGHBOR_SOURCE, DISCOVERED_OVER_BLUETOOTH);
    	sendBroadcast(i);
    }
	
	public synchronized void sendToLogger(String message) {
			Intent intentToLog = new Intent(PowerManagement.ACTION_LOG_UPDATE);
			intentToLog.putExtra(PowerManagement.LOG_MESSAGE, message + "\t-- ["
					+ new Date().toGMTString() + "] --\n");
			sendBroadcast(intentToLog);
	}
	
	private synchronized void signalController(int state) {
		setState(state);
		otherThreadAlive = false;
		threadLock.notifyAll();
	}

    //
    // Code taken BluetoothChatService API Demo
    //
    
 // Debugging
    private static final String TAG = "BluetoothService";
    private static final boolean D = true;

    // Name for the SDP record when creating server socket
    private static final String NAME = "BluetoothNeighborFinder";

    // Unique UUID for this application
    private static final UUID MY_UUID = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");

    // Member fields
    private final BluetoothAdapter mAdapter = BluetoothAdapter.getDefaultAdapter();
    private AcceptThread mAcceptThread;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;
    private int mState;
    private String mStateString;

    // Constants that indicate the current connection state
    public static final int STATE_NONE = 0;       // we're doing nothing
    public static final int STATE_LISTEN = 1;     // now listening for incoming connections
    public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 3;  // now connected to a remote device

    /**
     * Set the current state of the chat connection
     * @param state  An integer defining the current connection state
     */
    private synchronized void setState(int state) { 
        String log = "";
        switch(state) {
        case 0:
        	log = "setState() " + mStateString + " -> STATE_NONE";
        	mStateString = "STATE_NONE";
        	break;
        case 1:
        	log = "setState() " + mStateString + " -> STATE_LISTEN";
        	mStateString = "STATE_LISTEN";
        	break;
        case 2:
        	log = "setState() " + mStateString + " -> STATE_CONNECTING";
        	mStateString = "STATE_CONNECTING";
        	break;
        case 3:
        	log = "setState() " + mStateString + " -> STATE_CONNECTED";
        	mStateString = "STATE_CONNECTED";
        	break;
        default:
        	log = "setState() " + mStateString + " -> STATE_UNKNOWN";
        	mStateString = "STATE_UNKNOWN";
        	break;
        }
        
        mState = state;
        
        sendToLogger("BluetoothService:\n\t" + log + "\n");
    }

    /**
     * Return the current connection state. */
    public synchronized int getState() {
        return mState;
    }

    /**
     * Start the chat service. Specifically start AcceptThread to begin a
     * session in listening (server) mode. Called by the Activity onResume() */
    public synchronized void start() {
			if (D)
				sendToLogger("BluetoothService:" + "\n\tStart" + "\n");

			// Cancel any thread attempting to make a connection
			if (mConnectThread != null) {
				mConnectThread.cancel();
				mConnectThread = null;
			}

			// Cancel any thread currently running a connection
			if (mConnectedThread != null) {
				mConnectedThread.cancel();
				mConnectedThread = null;
			}

			// Start the thread to listen on a BluetoothServerSocket
			if (mAcceptThread == null) {
				mAcceptThread = new AcceptThread();
				signalController(STATE_LISTEN);
			}
    }

    /**
     * Start the ConnectThread to initiate a connection to a remote device.
     * @param device  The BluetoothDevice to connect
     */
    public synchronized void connect(BluetoothDevice device) {
        if (D) sendToLogger("BluetoothService:"
        		+ "\n\tConnect to: "
        		+ device
        		+ "\n");

        // Cancel any thread attempting to make a connection
        if (mState == STATE_CONNECTING) {
            if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}

        // Start the thread to connect with the given device
        mConnectThread = new ConnectThread(device);
        signalController(STATE_CONNECTING);
    }

    /**
     * Start the ConnectedThread to begin managing a Bluetooth connection
     * @param socket  The BluetoothSocket on which the connection was made
     * @param device  The BluetoothDevice that has been connected
     */
    public synchronized void connected(BluetoothSocket socket, BluetoothDevice device) {
        if (connectingTimeout != null) {
        	connectingTimeout.cancel();
        	connectingTimeout = null;
            sendToLogger("BluetoothService:"
            		+ "\n\tCONNECTING TIMEOUT CLEARED (connected)"
            		+ "\n");
        }
    	
    	if (D) sendToLogger("BluetoothService:"
        		+ "\n\tConnected to: "
        		+ device
        		+ "\n");

        // Cancel the thread that completed the connection
        if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}

        // Cancel the accept thread because we only want to connect to one device
        if (mAcceptThread != null) {mAcceptThread.cancel(); mAcceptThread = null;}

        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = new ConnectedThread(socket);
        signalController(STATE_CONNECTED);
    }

    /**
     * Stop all threads
     */
    public synchronized void stop() {
        if (D) sendToLogger("BluetoothService:"
        		+ "\n\tStop()"
        		+ "\n");
        if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}
        if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}
        if (mAcceptThread != null) {mAcceptThread.cancel(); mAcceptThread = null;}
        setState(STATE_NONE);
    }

    /**
     * Write to the ConnectedThread in an unsynchronized manner
     * @param out The bytes to write
     * @see ConnectedThread#write(byte[])
     */
    public void write(byte[] out) {
    	sendToLogger("BluetoothService:"
        		+ "\n\t(In write())"
        		+ "\n");
        // Create temporary object
        ConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (mState != STATE_CONNECTED) {
            	sendToLogger("BluetoothService:"
                		+ "\n\t(In write())"
                		+ "\n\tWrite called with no connection, aborting..."
                		+ "\n");
            	return;
            }
            r = mConnectedThread;
        }
        // Perform the write unsynchronized
        r.write(out);
    }

    /**
     * Indicate that the connection attempt failed and notify the UI Activity.
     */
    private synchronized void connectionFailed() {
    	sendToLogger("BluetoothService:"
        		+ "\n\tConnection Failed..."
        		+ "\n");
    	if (connectingTimeout != null) {
    		connectingTimeout.cancel();
    		connectingTimeout = null;
    		sendToLogger("BluetoothService:"
            		+ "\n\tCONNECTING TIMEOUT CLEARED (connectionFailed)"
            		+ "\n");
    	}
    }

    /**
     * Indicate that the connection was lost and notify the UI Activity.
     */
    private synchronized void connectionLost() {
    	sendToLogger("BluetoothService:"
        		+ "\n\tConnection Lost..."
        		+ "\n");
    	if (responseTimeout != null) {
    		responseTimeout.cancel();
    		responseTimeout = null;
    		sendToLogger("BluetoothService:"
            		+ "\n\tRESPONSE TIMEOUT CLEARED (connectionLost)"
            		+ "\n");
    	}
    }

    public synchronized void handleReceivedMessage(IBluetoothMessage message) {
		if (message instanceof BluetoothNeighbor) {
			if (responseTimeout != null) {
				responseTimeout.cancel();
				responseTimeout = null;
				sendToLogger("BluetoothService:"
	            		+ "\n\tRESPONSE TIMEOUT CLEARED (BluetoothNeighbor)"
	            		+ "\n");
			}
			newBluetoothNeighbor((BluetoothNeighbor)message);
			if (!broadcasting) {
				sendMessage(new BluetoothMessage(myContactInfo));
			}
		}
		else if (message instanceof WifiNeighbor) {
			newWifiNeighbor((WifiNeighbor)message);
		}
		else if (message instanceof ElectionMessage) {
			ElectionMessage electionMessage = (ElectionMessage)message;
			switch(message.getMessageType()) {
    		case BluetoothMessage.INITIATE_ELECTION_HEADER:
    			sendMessage(new BluetoothMessage(electionHandler.getElectionResponse()));
    			break;
    		case BluetoothMessage.ELECTION_RESPONSE_HEADER:
    			if (responseTimeout != null) {
    				responseTimeout.cancel();
    				responseTimeout = null;
    				sendToLogger("BluetoothService:"
    	            		+ "\n\tRESPONSE TIMEOUT CLEARED (RESPONSE)"
    	            		+ "\n");
    			}
    			electionHandler.handleElectionResponse(electionMessage);
    			break;
    		case BluetoothMessage.ELECTION_WINNER_ANNOUNCEMENT_HEADER:
    			sendMessage(new BluetoothMessage(electionHandler.getElectionAcknowledgement(electionMessage)));
    			break;
    		case BluetoothMessage.ACKNOWLEDGE_ELECTION_WINNER:
    			if (responseTimeout != null) {
    				responseTimeout.cancel();
    				responseTimeout = null;
    				sendToLogger("BluetoothService:"
    	            		+ "\n\tRESPONSE TIMEOUT CLEARED (ACKKNOWLEDGE)"
    	            		+ "\n");
    			}
    			electionHandler.handleElectionAcknowledgement(electionMessage);
    			break;
    		}
		}
	}
    
    private class BluetoothControllerThread extends Thread {
    	@Override
    	public void run() {
    		while(true) {
    			synchronized (BluetoothServiceTwo.this) {
					switch (mState) {
					case STATE_NONE:
						handleLastBroadcastMessage(processNextMessage());
						if (currentNeighbor != null) {
							connect(currentNeighbor);
						} else {
							broadcasting = false;
			    			sendToLogger("BluetoothService:"
			    					+ "\n\tDone Broadcasting"
			    					+ "\n");
							BluetoothServiceTwo.this.start();
						}
						break;
					case STATE_LISTEN:
						mAcceptThread.start();
						break;
					case STATE_CONNECTING:
						mConnectThread.start();
						connectingTimeout = new BluetoothConnectingTimeout();
				        timer.schedule(connectingTimeout, TIMEOUT_INTERVAL);
				        sendToLogger("BluetoothService:"
				        		+ "\n\tCONNECTING TIMEOUT SET"
				         		+ "\n");
						break;
					case STATE_CONNECTED:
						mConnectedThread.start();
						if (broadcasting) {
							sendToLogger("BluetoothService:"
									+ "\n\tBroadcasting is true, sending current message"
									+ "\n");
							responseTimeout = new BluetoothResponseTimeout();
							timer.schedule(responseTimeout, TIMEOUT_INTERVAL);
							sendToLogger("BluetoothService:"
									+ "\n\tRESPONSE TIMEOUT SET"
									+ "\n");
							sendMessage(currentMessage);
						}
						break;
					}
					otherThreadAlive = true;
    			}
    			
    			while(otherThreadAlive) {
    				try { threadLock.wait(); } catch (InterruptedException e) {}
    			}
    		}
    	}
    }
    
    /**
     * This thread runs while listening for incoming connections. It behaves
     * like a server-side client. It runs until a connection is accepted
     * (or until cancelled).
     */
    private class AcceptThread extends Thread {
        // The local server socket
        private final BluetoothServerSocket mmServerSocket;

        public AcceptThread() {
            BluetoothServerSocket tmp = null;

            // Create a new listening server socket
            try {
                tmp = mAdapter.listenUsingRfcommWithServiceRecord(NAME, MY_UUID);
            } catch (IOException e) {
                sendToLogger("BluetoothService:"
                		+ "Accept failed in constructor"
                		+ "\n");
                Log.e(TAG, "accept() failed error message", e);
            }
            mmServerSocket = tmp;
        }

        public void run() {
            if (D) sendToLogger("BluetoothService:"
            		+ "\n\tBegin acceptThread"
            		+ this
            		+ "\n");
            setName("AcceptThread");
            BluetoothSocket socket = null;

            boolean connected = false;
			try {
				// This is a blocking call and will only return on a
				// successful connection or an exception
				socket = mmServerSocket.accept();
			} catch (IOException e) {
				sendToLogger("BluetoothService:" + "Accept failed in run()"
						+ "\n");
				Log.e(TAG, "accept() failed error message", e);
			}

			// If a connection was accepted
			if (socket != null) {
				synchronized (BluetoothServiceTwo.this) {
					switch (mState) {
					case STATE_LISTEN:
					case STATE_CONNECTING:
						// Situation normal. Start the connected thread.
						connected(socket, socket.getRemoteDevice());
						connected = true;
						break;
					case STATE_NONE:
					case STATE_CONNECTED:
						// Either not ready or already connected. Terminate new
						// socket.
						try {
							sendToLogger("BluetoothService:"
									+ "\n\tClosing unwanted socket because already connected or not ready to connect..."
									+ "\n");
							socket.close();
						} catch (IOException e) {
							Log.e(TAG, "Could not close unwanted socket", e);
						}
						break;
					}
				}
            }
            if (D) sendToLogger("BluetoothService:"
            		+ "\n\tEnd of acceptThread run()"
            		+ "\n");
            if (!connected) {
            	synchronized(BluetoothServiceTwo.this) {
            		mAcceptThread.cancel();
            		mAcceptThread = null;
            	}
				signalController(STATE_NONE);
            }
        }

        public void cancel() {
            if (D) sendToLogger("BluetoothService:"
            		+ "Cancel acceptThread..."
            		+ "\n");
            try {
                mmServerSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of server failed", e);
            }
        }
    }
    
    /**
     * This thread runs during a connection with a remote device.
     * It handles all incoming and outgoing transmissions.
     */
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        
        boolean receivingMessage;
        byte[] message;
        int messageLength;
        int bytesRead;

        public ConnectedThread(BluetoothSocket socket) {
        	sendToLogger("BluetoothService:"
            		+ "\n\tCreating connectedThread..."
            		+ "\n");
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            receivingMessage = false;
            message = null;
            messageLength = 4;
            bytesRead = 0;
            
            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "temp sockets not created", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
            sendToLogger("BluetoothService:"
            		+ "\n\tDone creating connectedThread..."
            		+ "\n");
        }

        public void run() {
        	sendToLogger("BluetoothService:"
            		+ "\n\tBegin connectedThread run()"
            		+ "\n");
            byte[] buffer = new byte[1024];
            int bytes;
            
            // Keep listening to the InputStream while connected
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer, bytesRead, messageLength-bytesRead);

                    if (bytes > 0) {
                    	bytesRead += bytes;
                    	if (!receivingMessage && bytesRead >= 4) {
                    		receivingMessage = true;
                    		messageLength = ByteBuffer.wrap(buffer,0,4).getInt();
                    		message = new byte[messageLength];
                    	}
                    	else if (receivingMessage && bytesRead == messageLength){
                    		receivingMessage = false;
                    		System.arraycopy(buffer, 0, message, 0, bytesRead);

                    		bytesRead = 0;
                    		messageLength = 4;
                    		
                    		IBluetoothMessage tempMessage = BluetoothMessage.parse(message);
                    		sendToLogger("BluetoothService:"
                    				+ "\n\tReceived message from Neighbor:" 
                    				+ "\n\tName: " + mmSocket.getRemoteDevice().getName()
                    				+ "\n\tAddress: " + mmSocket.getRemoteDevice().getAddress()
                    				+ "\n\tMessage: " + tempMessage
                    				+ "\n");
                    		
                    		handleReceivedMessage(tempMessage);
                			break;
                    	}
                    }
                } catch (IOException e) {
                	sendToLogger("BleutoothService:"
                			+ "\n\tDisconnected, read interrupted, calling connectionLost()..."
                			+ "\n");
                    Log.e(TAG, "disconnected", e);
                    connectionLost();
                    break;
                }
            	sendToLogger("BluetoothService:"
                		+ "\n\tEnd of connectedThread run()"
                		+ "\n");
            	synchronized(BluetoothServiceTwo.this) {
        			mConnectedThread.cancel();
        			mConnectedThread = null;
        		}
            	signalController(STATE_NONE);
            }
        }

        /**
         * Write to the connected OutStream.
         * @param buffer  The bytes to write
         */
        public void write(byte[] buffer) {
            try {
                mmOutStream.write(buffer);
            } catch (IOException e) {
                Log.e(TAG, "Exception during write", e);
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }
    
    /**
     * This thread runs while attempting to make an outgoing connection
     * with a device. It runs straight through; the connection either
     * succeeds or fails.
     */
    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            mmDevice = device;
            BluetoothSocket tmp = null;

            // Get a BluetoothSocket for a connection with the
            // given BluetoothDevice
            try {
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
                Log.e(TAG, "create() failed", e);
            }
            mmSocket = tmp;
        }

        public void run() {
            sendToLogger("BluetoothService:"
            		+ "Begin connectThread run()"
            		+ "\n");
            setName("ConnectThread");

            // Always cancel discovery because it will slow down a connection
            mAdapter.cancelDiscovery();

            // Make a connection to the BluetoothSocket
            try {
                // This is a blocking call and will only return on a
                // successful connection or an exception
                mmSocket.connect();
            } catch (IOException e) {
                // Close the socket
                try {
                    mmSocket.close();
                } catch (IOException e2) {
                    Log.e(TAG, "unable to close() socket during connection failure", e2);
                }
                
                connectionFailed();
                synchronized(BluetoothServiceTwo.this) {
                	mConnectThread.cancel();
                	mConnectThread = null;
                }
                signalController(STATE_NONE);
                return;
            }

            // Start the connected thread
            connected(mmSocket, mmDevice);
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
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
        				+ "\n\tStarting new Wifi discovery election!"
        				+ "\n");
        		
        		electionResponses = new ArrayList<Pair<BluetoothDevice, Integer>>();
        		broadcast(new ElectionMessage(BluetoothMessage.INITIATE_ELECTION_HEADER));
        	}
        	else {
        		sendToLogger("BluetoothService:"
        				+ "\n\tAlready hosting election, request ignored."
        				+ "\n");
        	}
        }
    	
        public synchronized IBluetoothMessage getElectionResponse() {
        	Random rand = new Random();
        	int value = rand.nextInt(myElectionResponseWindow);
        	
        	sendToLogger("BluetoothService:"
        			+ "\n\tResponding to election with value " + String.valueOf(value)
        			+ "\n");
        	
        	return new ElectionMessage(value);
        }
        
    	public synchronized void handleElectionResponse(ElectionMessage message) {
			synchronized (currentNeighbor) {
				sendToLogger("BluetoothService:" + "\n\tReceived value "
						+ message.value + " from neighbor "
						+ currentNeighbor.getAddress() + "\n");
				electionResponses.add(new Pair<BluetoothDevice, Integer>(
						currentNeighbor, message.value));
			}
        }
        
        public synchronized void determineElectionWinner() {
    		sendToLogger("BluetoothService:"
    				+ "\n\tFinalizing Election...\n");
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
    				+ "\n\tAnnouncing Election Results...\n");

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
    					+ "\n\tResetting election window to 1024"
    					+ "\n");
    		}
    		else {
    			response.value = 0;
    			myElectionResponseWindow = Math.max(64, myElectionResponseWindow / 2);
    			
    			sendToLogger("BluetoothService:"
    					+ "\n\tDecreasing electionWindow to " + myElectionResponseWindow
    					+ "\n");
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
    
	private class BluetoothConnectingTimeout extends TimerTask {
		@Override
		public void run() {
			sendToLogger("BluetoothService:"
					+ "\n\tTimeout while connecting..."
					+ "\n");
			connectionFailed();
		}
	}
    
	private class BluetoothResponseTimeout extends TimerTask {
		@Override
		public void run() {
			sendToLogger("BluetoothService:"
					+ "\n\tTimeout while waiting for response..."
					+ "\n");
			processNextMessage();
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
}