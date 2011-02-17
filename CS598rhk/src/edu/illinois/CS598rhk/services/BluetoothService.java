package edu.illinois.CS598rhk.services;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
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

	public static Set<BluetoothDevice> potentialNeighbors;
	public static Set<BluetoothDevice> activeNeighbors;
	private static Set<BluetoothDevice> intermediateNeighbors;

	private BluetoothAcceptThread acceptThread;

	private volatile boolean controllerReady;
	private Object controllerMonitor;

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		// We could use setName() here to allow the user to change the device
		// name
		Notification notification = new Notification();
		notification.tickerText = "BluetoothService";
		startForeground(2, notification);

		myContactInfo = new BluetoothNeighbor();
		electionHandler = new ElectionHandler();

		myContactInfo.name = BluetoothAdapter.getDefaultAdapter().getName();
		myContactInfo.address = BluetoothAdapter.getDefaultAdapter()
				.getAddress();
		myContactInfo.progress = 0;

		potentialNeighbors = BluetoothAdapter.getDefaultAdapter()
				.getBondedDevices();
		activeNeighbors = new HashSet<BluetoothDevice>();

		acceptThread = new BluetoothAcceptThread();

		controllerReady = false;

		acceptThread.start();

		return START_STICKY;
	}

	@Override
	public void onDestroy() {
		stopForeground(true);
		super.onDestroy();
	}

	public void updateScheduleProgress(long progress) {
		myContactInfo.progress = progress;

	}

	public void hostWifiDiscoveryElection() {
		electionHandler.hostWifiDiscoveryElection();
	}

	public void broadcast(IBluetoothMessage message) {
		(new BluetoothBroadcastThread(message)).start();
	}

	private synchronized void handleLastBroadcastMessage(
			IBluetoothMessage lastMessage) {
		if (lastMessage != null) {
			switch (lastMessage.getMessageType()) {
			case BluetoothMessage.INITIATE_ELECTION_HEADER:
				electionHandler.determineElectionWinner();
				break;
			case BluetoothMessage.ELECTION_WINNER_ANNOUNCEMENT_HEADER:
				electionHandler.announceElectionWinner();
				break;
			default:
				break;
			}
		}
	}

	public synchronized void newBluetoothNeighbor(BluetoothNeighbor neighbor) {
		Intent i = new Intent(INTENT_TO_ADD_BLUETOOTH_NEIGHBOR);
		i.putExtra(BLUETOOTH_NEIGHBOR_DATA, neighbor.pack());
		sendBroadcast(i);

	}

	public synchronized void newWifiNeighbor(WifiNeighbor neihgbor) {
		Intent i = new Intent(WifiService.INTENT_TO_ADD_WIFI_NEIGHBOR);
		i.putExtra(WifiService.WIFI_NEIGHBOR_DATA, neihgbor.pack());
		i.putExtra(WifiService.INTENT_TO_ADD_WIFI_NEIGHBOR_SOURCE,
				DISCOVERED_OVER_BLUETOOTH);
		sendBroadcast(i);
	}

	public synchronized void sendToLoggers(String message) {
		Log.d(TAG, message);
		Intent intentToLog = new Intent(PowerManagement.ACTION_LOG_UPDATE);
		intentToLog.putExtra(PowerManagement.LOG_MESSAGE, message + "\n\t-- ["
				+ new Date().toGMTString() + "]");
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
	private static final UUID MY_UUID = UUID
			.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");

	// Member fields
	private final BluetoothAdapter mAdapter = BluetoothAdapter
			.getDefaultAdapter();
	public static int errorCount;

	/**
	 * Indicate that the connection attempt failed and notify the UI Activity.
	 */
	private synchronized void connectionFailed(BluetoothDevice who) {
		intermediateNeighbors.remove(who);
		errorCount++;
	}

	/**
	 * Indicate that the connection was lost and notify the UI Activity.
	 */
	private synchronized void connectionLost() {
		errorCount++;
	}

	public synchronized IBluetoothMessage handleReceivedMessage(
			IBluetoothMessage message) {
		IBluetoothMessage response = null;

		if (message instanceof BluetoothNeighbor) {
			newBluetoothNeighbor((BluetoothNeighbor) message);
			response = myContactInfo;
		} else if (message instanceof ElectionMessage) {
			ElectionMessage electionMessage = (ElectionMessage) message;
			switch (message.getMessageType()) {
			case BluetoothMessage.INITIATE_ELECTION_HEADER:
				response = electionHandler.getElectionResponse();
				break;
			case BluetoothMessage.ELECTION_WINNER_ANNOUNCEMENT_HEADER:
				response = electionHandler
						.getElectionAcknowledgement(electionMessage);
				break;
			}
		}
		return response;
	}

	public synchronized void handleResponseMessage(IBluetoothMessage message,
			BluetoothDevice currentNeighbor) {

		if (message instanceof BluetoothNeighbor) {
			newBluetoothNeighbor((BluetoothNeighbor) message);
		} else if (message instanceof WifiNeighbor) {
			newWifiNeighbor((WifiNeighbor) message);
		} else if (message instanceof ElectionMessage) {
			ElectionMessage electionMessage = (ElectionMessage) message;
			switch (message.getMessageType()) {
			case BluetoothMessage.ELECTION_RESPONSE_HEADER:
				electionHandler.handleElectionResponse(electionMessage,
						currentNeighbor);
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
				tmp = mAdapter
						.listenUsingRfcommWithServiceRecord(NAME, MY_UUID);
			} catch (IOException e) {

				Log.e(TAG, "BluetoothAcceptThread() failed", e);
			}
			serverSocket = tmp;
		}

		@Override
		public void run() {
			setName("Accept");
			while (true) {
				BluetoothSocket socket = null;

				try {
					socket = serverSocket.accept();
				} catch (IOException e) {
					Log.e(TAG, "accept() failed", e);
				}

				(new BluetoothAcceptHandlerThread(socket)).start();
			}
		}
	}

	private class BluetoothBroadcastThread extends Thread {
		IBluetoothMessage message;

		public BluetoothBroadcastThread(IBluetoothMessage message) {
			this.message = message;
		}

		@Override
		public void run() {
			setName("Broadcast");
			List<Thread> threads = new ArrayList<Thread>();
			intermediateNeighbors = new HashSet<BluetoothDevice>(
					activeNeighbors);
			synchronized (activeNeighbors) {
				for (BluetoothDevice neighbor : activeNeighbors) {
					Thread tempThread = new BluetoothConnectThread(message,
							neighbor);
					threads.add(tempThread);
					tempThread.start();

					while (tempThread.isAlive())
						try {
							Thread.sleep(50);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
				}

				activeNeighbors = new HashSet<BluetoothDevice>(
						intermediateNeighbors);
			}

			handleLastBroadcastMessage(message);
		}
	}

	private class BluetoothAcceptHandlerThread extends Thread {
		private BluetoothSocket mSocket;

		public BluetoothAcceptHandlerThread(BluetoothSocket socket) {
			mSocket = socket;
		}

		@Override
		public void run() {
			setName("Accept Handler");
			if (mSocket != null) {
				InputStream inStream = null;
				try {
					inStream = mSocket.getInputStream();
				} catch (IOException e) {

					Log.e(TAG, "getInputStream() failed", e);
				}

				if (inStream != null) {
					IBluetoothMessage message = readBluetoothMessage(inStream);

					IBluetoothMessage response = handleReceivedMessage(message);

					OutputStream outStream = null;
					try {
						outStream = mSocket.getOutputStream();
					} catch (IOException e) {
						Log.e(TAG, "getOutputStream() failed", e);
					}

					if (outStream != null) {
						writeBluetoothMessage(outStream, response);
					}
				}
			}
		}
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
				bytes = inStream.read(buffer, bytesRead, messageLength
						- bytesRead);

				if (bytes > 0) {
					bytesRead += bytes;
					if (!receivingMessage && bytesRead == 4) {
						receivingMessage = true;
						messageLength = ByteBuffer.wrap(buffer, 0, 4).getInt();
						byte[] newBuffer = new byte[messageLength];
						System.arraycopy(buffer, 0, newBuffer, 0, buffer.length);
						buffer = newBuffer;
					} else if (receivingMessage && bytesRead == messageLength) {

						receivingMessage = false;
						messageLength = 4;
						bytesRead = 0;
						bytes = 0;

						message = BluetoothMessage.parse(buffer);
						break;
					}
				}
			} catch (IOException e) {
				Log.e(TAG, "disconnected", e);
				connectionLost();
				break;
			}
		}

		return message;
	}

	private void writeBluetoothMessage(OutputStream outStream,
			IBluetoothMessage message) {
		try {
			outStream.write((new BluetoothMessage(message))
					.getMessageWithHeader());
		} catch (IOException e) {
			Log.e(TAG, "Exception during write", e);
		}
	}

	private class BluetoothConnectThread extends Thread {
		IBluetoothMessage currentMessage;
		BluetoothDevice currentNeighbor;

		public BluetoothConnectThread(IBluetoothMessage message,
				BluetoothDevice device) {
			currentMessage = message;
			currentNeighbor = device;
		}

		@Override
		public void run() {
			setName("Connect");
			if (currentNeighbor != null) {
				BluetoothSocket socket = null;
				try {
					socket = currentNeighbor
							.createRfcommSocketToServiceRecord(MY_UUID);
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
						Log.e(TAG,
								"unable to close() socket during connection failure",
								e2);
					}
					Log.e(TAG,
							e.getMessage() + " DEVICE:"
									+ currentNeighbor.getName(), e);
					connectionFailed(currentNeighbor);
					return;
				}

				if (socket != null) {

					OutputStream outStream = null;
					try {
						outStream = socket.getOutputStream();
					} catch (IOException e) {
						Log.e(TAG, "getOutputStream() failed", e);
					}

					if (outStream != null) {
						writeBluetoothMessage(outStream, currentMessage);

						InputStream inStream = null;
						try {
							inStream = socket.getInputStream();
						} catch (IOException e) {

							Log.e(TAG, "getInputStream() failed", e);
						}

						if (inStream != null) {
							IBluetoothMessage message = readBluetoothMessage(inStream);

							handleResponseMessage(message, currentNeighbor);
						}
					}
				}
				try {
					socket.close();
				} catch (IOException e) {

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

				electionResponses = new ArrayList<Pair<BluetoothDevice, Integer>>();
				broadcast(new ElectionMessage(
						BluetoothMessage.INITIATE_ELECTION_HEADER));
			}
		}

		public synchronized IBluetoothMessage getElectionResponse() {
			Random rand = new Random();
			int value = rand.nextInt(myElectionResponseWindow);

			return new ElectionMessage(value);
		}

		public synchronized void handleElectionResponse(
				ElectionMessage message, BluetoothDevice currentNeighbor) {

			electionResponses.add(new Pair<BluetoothDevice, Integer>(
					currentNeighbor, message.value));
		}

		public synchronized void determineElectionWinner() {

			Collections.sort(electionResponses);
			electionAnnouncements = new ArrayList<IBluetoothMessage>();

			long delay = 0;
			delay = myContactInfo.progress;

			for (Pair<BluetoothDevice, Integer> response : electionResponses) {
				electionAnnouncements.add(new ElectionMessage(response.first
						.getAddress(), delay));
			}

			electionResponses = null;
			nextAnnouncement = electionAnnouncements.iterator();
			winnerAcknowledgedElection = false;
			announceElectionWinner();
		}

		private synchronized void announceElectionWinner() {

			if (!winnerAcknowledgedElection) {
				if (nextAnnouncement.hasNext()) {
					broadcast(nextAnnouncement.next());
				} else {
					winnerElected();
				}
			} else {
				winnerElected();
			}
		}

		public synchronized ElectionMessage getElectionAcknowledgement(
				ElectionMessage result) {
			ElectionMessage response = new ElectionMessage(
					BluetoothMessage.ACKNOWLEDGE_ELECTION_WINNER);

			String myAddress = "";
			synchronized (myContactInfo) {
				myAddress = myContactInfo.address;
			}
			if (myAddress.equals(result.winnerAddress)) {
				response.value = 1;
				myElectionResponseWindow = 1024;

				Intent i = new Intent(ACTION_ELECTED_FOR_WIFI_DISCOVERY);
				i.putExtra(DELY_UNTIL_STARTING_WIFI_DISCOVERY,
						result.delayUntilWinnerStarts);
				sendBroadcast(i);
			} else {
				response.value = 0;
				myElectionResponseWindow = Math.max(64,
						myElectionResponseWindow / 2);

				Intent i = new Intent(ACTION_ELECTED_FOR_WIFI_DISCOVERY);
				i.putExtra(DELY_UNTIL_STARTING_WIFI_DISCOVERY, 0);
				sendBroadcast(i);
			}

			return response;
		}

		public synchronized void handleElectionAcknowledgement(
				ElectionMessage electionMessage) {
			if (electionMessage.value == 1) {
				winnerAcknowledgedElection = true;
			}
		}

		private synchronized void winnerElected() {
			Intent i = new Intent(ACTION_ELECTED_FOR_WIFI_DISCOVERY);

			long delay = 0;
			synchronized (myContactInfo) {
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

	private class Pair<T, E extends Comparable<E>> implements
			Comparable<Pair<T, E>> {
		public T first;
		public E second;

		public Pair(T t, E e) {
			this.first = t;
			this.second = e;
		}

		@Override
		public int compareTo(Pair<T, E> another) {
			return second.compareTo(another.second);
		}
	}

	@Override
	public void startDiscovery() {
		mAdapter.startDiscovery();
	}
	
	@Override
	public void stopDiscovery() {
		mAdapter.cancelDiscovery();
	}
}