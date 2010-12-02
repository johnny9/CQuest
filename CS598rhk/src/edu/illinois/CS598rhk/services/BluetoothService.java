package edu.illinois.CS598rhk.services;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

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
import edu.illinois.CS598rhk.models.DiscoveryElectionMessage;
import edu.illinois.CS598rhk.models.Neighbor;

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
    
    private List<BluetoothMessage> messages;
    private Set<BluetoothDevice> neighbors;
    private Iterator<BluetoothDevice> nextNeighbor;
    private BluetoothDevice currentNeighbor;
    private boolean broadcasting;
    
    private boolean hostingElection;
    private int myElectionResponseWindow;
    private Map<BluetoothDevice, Integer> electionResponses;
    
    @Override
    public void onCreate() {
    	super.onCreate();
    	myContactInfo = new BluetoothNeighbor();
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
    	// TODO: This dies if Bluetooth is not enabled at this point!
    	
    	// We could use setName() here to allow the user to change the name we use
    	
    	mStateString = "STATE_INIT";
    	myElectionResponseWindow = 1024;
    	
    	myContactInfo.name = BluetoothAdapter.getDefaultAdapter().getName();
    	myContactInfo.address = BluetoothAdapter.getDefaultAdapter().getAddress();
    	myContactInfo.progress = 0;
    	sendToLogger("Bluetooth name: " + myContactInfo.name + " Bluetooth address: " + myContactInfo.address + "\n");
    	
    	broadcasting = false;
    	neighbors = BluetoothAdapter.getDefaultAdapter().getBondedDevices();
    	nextNeighbor = neighbors.iterator();
    	
    	messages = new ArrayList<BluetoothMessage>();
    	updateNeighbors();
    	return START_STICKY;
    }
    
    public void updateScheduleProgress(long progress) {
    	synchronized(myContactInfo) {
    		sendToLogger("Bluetooth received progress update from " + myContactInfo.progress + " to " + progress);
    		myContactInfo.progress = progress;
    	}
    }
    
	@Override
	public void updateNeighborCount(int neighborCount) {
		synchronized(myContactInfo) {
			myContactInfo.neighborCount = neighborCount;
		}
	}
    
    public void updateNeighbors() {
    	sendToLogger("Bluetooth: Begin updating neighbors\n");
    	broadcast(new BluetoothMessage(myContactInfo.getMessageType(), myContactInfo));
    }
    
    public void broadcast(IBluetoothMessage message) {
    	sendToLogger("Bluetoooth: Received message to broadcast\n\tMessage: " + message + "\n");
    	broadcast(new BluetoothMessage(message.getMessageType() ,message));
    }
    
    private void broadcast(BluetoothMessage message) {
    	synchronized(messages) {
	    	messages.add(message);
	    	if (!broadcasting) {
	    		stop();
				broadcasting = true;
				processNextMessage();
			}
	    	else {
	    		sendToLogger("Bluetooth: Currently broadcasting message\n\tMessage: " + messages.get(0)
	    				+ "\n\tQueuing message: " + message + "\n");
	    	}
    	}
    }
    
    private void processNextMessage() {
    	BluetoothMessage lastMessage = null;
    	synchronized(messages) {
	     	if (nextNeighbor.hasNext()) {
				currentNeighbor = nextNeighbor.next();
				sendToLogger("Bluetooth: Attempting to connect to Neighbor:\n\t" + currentNeighbor.getName() 
						+ " with address " + currentNeighbor.getAddress()
						+ "\n\tand message: " + messages.get(0) + "\n");
				connect(currentNeighbor);
			}
	    	else {	    		
	    		lastMessage = messages.remove(0);
	    		nextNeighbor = neighbors.iterator();
	    		if (!messages.isEmpty()) {
	    			processNextMessage();
	    		}
	    		else {
	    			broadcasting = false;
	    			sendToLogger("Bluetooth: Done Broadcasting\n");
	    			start();
	    		}
	    	}
    	}
		if ( lastMessage != null && lastMessage.getMessageType() == BluetoothMessage.WIFI_ELECTION_HEADER && hostingElection) {
			finalizeElection();
		}
    }
    
    private void sendMessageToNeighbor(BluetoothMessage message) {
    	synchronized(messages) {
	    	if (message == null) {
	    		message = messages.get(0);
	    	}
	    	sendToLogger("Bluetooth: Connected to Neighbor:\n\t" + currentNeighbor.getName() 
	    			+ " with address " + currentNeighbor.getAddress()
	    			+ "\n\tSending message: " + message + "\n");
	    	write(message.getMessageWithHeader());
    	}
    }
    
    public void newBluetoothNeighbor(byte[] message) {
    	Intent i = new Intent(INTENT_TO_ADD_BLUETOOTH_NEIGHBOR);
    	i.putExtra(BLUETOOTH_NEIGHBOR_DATA, BluetoothMessage.stripHeader(message));
    	sendBroadcast(i);
    	sendToLogger("Bluetooth: Found Bluetooth Nieghbor:\n\t" + new String(message) + "\n");
    }
    
    public void newWifiNeighbor(byte[] message) {
    	Intent i = new Intent(WifiService.INTENT_TO_ADD_WIFI_NEIGHBOR);
    	i.putExtra(WifiService.WIFI_NEIGHBOR_DATA, BluetoothMessage.stripHeader(message));
    	i.putExtra(WifiService.INTENT_TO_ADD_WIFI_NEIGHBOR_SOURCE, DISCOVERED_OVER_BLUETOOTH);
    	sendBroadcast(i);
    	sendToLogger("Bluetooth: Found Wifi Nieghbor:\n\t" + message + "\n");
    }
    
    public void hostWifiDiscoveryElection() {
    	if (!hostingElection) {
    		sendToLogger("BluetoothService: Starting new Wifi discovery election!\n");
    		hostingElection = true;
    		electionResponses = new HashMap<BluetoothDevice, Integer>();
    		BluetoothMessage election = new BluetoothMessage(BluetoothMessage.WIFI_ELECTION_HEADER, new DiscoveryElectionMessage(BluetoothMessage.WIFI_ELECTION_HEADER));
    		broadcast(election);
    	}
    	else {
    		sendToLogger("BluetoothService: Already hosting election, request ignored.\n");
    	}
    }
    
    private BluetoothMessage getElectionResponse() {
    	Random rand = new Random();
    	int value = rand.nextInt(myElectionResponseWindow);
    	
    	sendToLogger("BluetoothService: Responding to election with value " + String.valueOf(value) + "\n");
    	
    	return new BluetoothMessage(BluetoothMessage.WIFI_ELECTION_RESPONSE_HEADER, new DiscoveryElectionMessage(value));
    }
    
	private void handleElectionResults(byte[] result) {
		int addressLength = myContactInfo.address.getBytes().length;
		byte[] winnerAddressBytes = new byte[addressLength];
		System.arraycopy(result, 5, winnerAddressBytes, 0, addressLength);

		String winnerAddress = new String(winnerAddressBytes);
		if (myContactInfo.address.equals(winnerAddress)) {
			byte[] temp = new byte[8];
			System.arraycopy(result, 1 + addressLength, temp, 0, 8);
			long delay = ByteBuffer.wrap(temp).getLong();

			Intent i = new Intent(ACTION_ELECTED_FOR_WIFI_DISCOVERY);
			i.putExtra(DELY_UNTIL_STARTING_WIFI_DISCOVERY, delay);
			sendBroadcast(i);
			
			myElectionResponseWindow = 1024;
		}
		else {
			myElectionResponseWindow = Math.max(64, myElectionResponseWindow / 2);
		}
	}
    
    private void handleElectionResponse(byte[] message, BluetoothDevice neighbor) {
    	byte[] temp = new byte[4];
		System.arraycopy(message, 5, temp, 0, 4);
		int value = ByteBuffer.wrap(temp).getInt();
		synchronized(electionResponses) {
			electionResponses.put(neighbor, value);
		}
    }
    
	private void finalizeElection() {
		BluetoothDevice winner = null;
		Integer currentBest = 1024;

		Set<BluetoothDevice> neighbors = electionResponses.keySet();
		for (BluetoothDevice neighbor : neighbors) {
			Integer value = electionResponses.get(neighbor);
			if (value <= currentBest) {
				currentBest = value;
				winner = neighbor;
			}
		}
		electionResponses = null;
		hostingElection = false;

		if (winner != null) {
			byte[] winnerAddress = winner.getAddress().getBytes();
			int msgLength = winnerAddress.length + 8;
			byte[] electionResult = new byte[msgLength];
			System.arraycopy(winnerAddress, 0, electionResult, 0,
					winnerAddress.length);

			byte[] delayUntilWinnerDiscovery = ByteBuffer.allocate(8)
					.putLong(myContactInfo.progress).array();
			System.arraycopy(electionResult, winnerAddress.length,
					delayUntilWinnerDiscovery, 0, 8);

			broadcast(new BluetoothMessage(
					BluetoothMessage.WIFI_ELECTION_RESULTS_HEADER,
					new DiscoveryElectionMessage(electionResult)));
		} else {
			Intent i = new Intent(ACTION_ELECTED_FOR_WIFI_DISCOVERY);
			synchronized (myContactInfo) {
				i.putExtra(DELY_UNTIL_STARTING_WIFI_DISCOVERY,
						myContactInfo.progress);
			}
			sendBroadcast(i);
		}
	}
	
	public void sendToLogger(String message) {
		Intent intentToLog = new Intent(PowerManagement.ACTION_LOG_UPDATE);
		intentToLog.putExtra(PowerManagement.LOG_MESSAGE, message);
		sendBroadcast(intentToLog);
	}
    
    //
    //
    //
    // Code taken BluetoothChatService API Demo
    //
    //
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
//    private final Handler mHandler;
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
     * Constructor. Prepares a new BluetoothChat session.
     * @param context  The UI Activity Context
     * @param handler  A Handler to send messages back to the UI Activity
     */
//    public BluetoothService(Context context, Handler handler) {
//        mAdapter = BluetoothAdapter.getDefaultAdapter();
//        mState = STATE_NONE;
//        mHandler = handler;
//    }

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
        
        sendToLogger(log);
        // Give the new state to the Handler so the UI Activity can update
//        mHandler.obtainMessage(BluetoothChat.MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
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
        if (D) Log.d(TAG, "start");

        // Cancel any thread attempting to make a connection
        if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}

        // Start the thread to listen on a BluetoothServerSocket
        if (mAcceptThread == null) {
            mAcceptThread = new AcceptThread();
            mAcceptThread.start();
        }
        setState(STATE_LISTEN);
    }

    /**
     * Start the ConnectThread to initiate a connection to a remote device.
     * @param device  The BluetoothDevice to connect
     */
    public synchronized void connect(BluetoothDevice device) {
        if (D) Log.d(TAG, "connect to: " + device);

        // Cancel any thread attempting to make a connection
        if (mState == STATE_CONNECTING) {
            if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}

        // Start the thread to connect with the given device
        mConnectThread = new ConnectThread(device);
        mConnectThread.start();
        setState(STATE_CONNECTING);
    }

    /**
     * Start the ConnectedThread to begin managing a Bluetooth connection
     * @param socket  The BluetoothSocket on which the connection was made
     * @param device  The BluetoothDevice that has been connected
     */
    public synchronized void connected(BluetoothSocket socket, BluetoothDevice device) {
        if (D) Log.d(TAG, "connected");

        // Cancel the thread that completed the connection
        if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}

        // Cancel the accept thread because we only want to connect to one device
        if (mAcceptThread != null) {mAcceptThread.cancel(); mAcceptThread = null;}

        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = new ConnectedThread(socket);
        mConnectedThread.start();

        setState(STATE_CONNECTED);
        if (broadcasting) {
        	sendMessageToNeighbor(null);
        }
    }

    /**
     * Stop all threads
     */
    public synchronized void stop() {
        if (D) Log.d(TAG, "stop");
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
        // Create temporary object
        ConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (mState != STATE_CONNECTED) return;
            r = mConnectedThread;
        }
        // Perform the write unsynchronized
        r.write(out);
    }

    /**
     * Indicate that the connection attempt failed and notify the UI Activity.
     */
    private void connectionFailed() {
    	//setState(STATE_LISTEN);
    	setState(STATE_NONE);
        
    	if (broadcasting) {
    		processNextMessage();
    	}
    	else {
    		start();
    	}
    }

    /**
     * Indicate that the connection was lost and notify the UI Activity.
     */
    private void connectionLost() {
    	//setState(STATE_LISTEN);
    	setState(STATE_NONE);
        
    	if (broadcasting) {
    		processNextMessage();
    	}
    	else {
    		start();
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
                Log.e(TAG, "listen() failed", e);
            }
            mmServerSocket = tmp;
        }

        public void run() {
            if (D) Log.d(TAG, "BEGIN mAcceptThread" + this);
            setName("AcceptThread");
            BluetoothSocket socket = null;

            // Listen to the server socket if we're not connected
            while (mState != STATE_CONNECTED) {
                try {
                    // This is a blocking call and will only return on a
                    // successful connection or an exception
                    socket = mmServerSocket.accept();
                } catch (IOException e) {
                    Log.e(TAG, "accept() failed", e);
                    break;
                }

                // If a connection was accepted
                if (socket != null) {
                    synchronized (BluetoothService.this) {
                        switch (mState) {
                        case STATE_LISTEN:
                        case STATE_CONNECTING:
                            // Situation normal. Start the connected thread.
                            connected(socket, socket.getRemoteDevice());
                            break;
                        case STATE_NONE:
                        case STATE_CONNECTED:
                            // Either not ready or already connected. Terminate new socket.
                            try {
                                socket.close();
                            } catch (IOException e) {
                                Log.e(TAG, "Could not close unwanted socket", e);
                            }
                            break;
                        }
                    }
                }
            }
            if (D) Log.i(TAG, "END mAcceptThread");
        }

        public void cancel() {
            if (D) Log.d(TAG, "cancel " + this);
            try {
                mmServerSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of server failed", e);
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
            Log.i(TAG, "BEGIN mConnectThread");
            setName("ConnectThread");

            // Always cancel discovery because it will slow down a connection
            mAdapter.cancelDiscovery();

            // Make a connection to the BluetoothSocket
            try {
                // This is a blocking call and will only return on a
                // successful connection or an exception
                mmSocket.connect();
            } catch (IOException e) {
                connectionFailed();
                // Close the socket
                try {
                    mmSocket.close();
                } catch (IOException e2) {
                    Log.e(TAG, "unable to close() socket during connection failure", e2);
                }
                // Start the service over to restart listening mode
                //BluetoothService.this.start();
                return;
            }

            // Reset the ConnectThread because we're done
            synchronized (BluetoothService.this) {
                mConnectThread = null;
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
            Log.d(TAG, "create ConnectedThread");
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
        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectedThread");
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
                    		System.arraycopy(buffer, 0, message, 0, bytesRead);
                    		receivingMessage = false;
                    		bytesRead = 0;
                    		messageLength = 4;
                    		
                    		sendToLogger("Bluetooth: Received message\n\t"
                    				+ "Message: " + new String(message)
                    				+ "\n\tfrom Neighbor: " + mmSocket.getRemoteDevice().getName()
                    				+ " with address " + mmSocket.getRemoteDevice().getAddress() + "\n");
                    		
                    		switch(message[BluetoothMessage.INDEX_OF_HEADER]) {
                    		case BluetoothMessage.BLUETOOTH_NEIGHBOR_HEADER:
                    			newBluetoothNeighbor(message);
                    			break;
                    		case BluetoothMessage.WIFI_NEIGHBOR_HEADER:
                    			newWifiNeighbor(message);
                    			break;
                    		case BluetoothMessage.WIFI_ELECTION_HEADER:
                    			sendMessageToNeighbor(getElectionResponse());
                    			break;
                    		case BluetoothMessage.WIFI_ELECTION_RESULTS_HEADER:
                    			handleElectionResults(message);
                    			break;
                    		case BluetoothMessage.WIFI_ELECTION_RESPONSE_HEADER:
                    			handleElectionResponse(message, currentNeighbor);
                    			break;
                    		}
                    		
							if (!broadcasting) {
								sendMessageToNeighbor(new BluetoothMessage(
										myContactInfo.getMessageType(),
										myContactInfo));
							} else {
								processNextMessage();
							}
                    	}
                    }
                } catch (IOException e) {
                    Log.e(TAG, "disconnected", e);
                    connectionLost();
                    break;
                }
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
}