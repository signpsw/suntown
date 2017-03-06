package com.suntown.suntownshop.service;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.suntown.suntownshop.Constants;
import com.suntown.suntownshop.MainTabActivity;
import com.suntown.suntownshop.R;
import com.suntown.suntownshop.SpecialGoodsListActivity;
import com.suntown.suntownshop.adapter.GridGoodsListAdapter;
import com.suntown.suntownshop.asynctask.PostAsyncTask;
import com.suntown.suntownshop.asynctask.PostAsyncTask.OnCompleteCallback;
import com.suntown.suntownshop.db.BeaconDb;
import com.suntown.suntownshop.db.ShopCartDb;
import com.suntown.suntownshop.ibeacon.IBeaconClass;
import com.suntown.suntownshop.ibeacon.IBeaconClass.IBeacon;
import com.suntown.suntownshop.model.Beacon;
import com.suntown.suntownshop.model.CartGoods;
import com.suntown.suntownshop.model.Goods;
import com.suntown.suntownshop.model.NotificationInfo;
import com.suntown.suntownshop.model.ParcelableGoods;
import com.suntown.suntownshop.receiver.NotificationCanceledBroadcastReceiver;
import com.suntown.suntownshop.runnable.GetJsonRunnable;
import com.suntown.suntownshop.utils.JsonBuilder;
import com.suntown.suntownshop.utils.MyMath;
import com.suntown.suntownshop.utils.WifiAdmin;
import com.suntown.suntownshop.utils.XmlParser;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.Notification.BigTextStyle;
import android.app.Notification.Style;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources.NotFoundException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

/**
 * ��̨����
 * 
 * @author Ǯ��
 *
 */
@SuppressLint("NewApi")
public class LocalService extends Service {

	private BluetoothAdapter mBluetoothAdapter;
	private boolean mScanning;

	private static final int REQUEST_ENABLE_BT = 2;
	// 2���ֹͣ��������.
	private static final long SCAN_PERIOD = 2000;

	private int loopInterval = 100;
	private final static long TIME_INTERVAL_CHECKAGAIN = 60 * 1000;
	private final static long TIME_INTERVAL_RUN = 10 * 1000;
	private final static long TIME_ONE_DAY = 24 * 60 * 60 * 1000;
	private final static int CHECK_UPDATE_INFO = 0;
	private final static int DOWNLOAD_ERR = 1;
	private final static int DOWNLOAD_SUSS = 2;
	private final static int DOWNLOAD_CANCEL = 3;
	private final static int MSG_NEWORDER = 4;
	private final static int MSG_PULLMSG_COMPLETE = 5;
	private final static int MSG_UPDATE_DA_COMPLETE = 6;
	private final static int NETWORK_ERR = -1;
	// ��IBeacon�ľ��룬С�ڸ�ֵ��������Ϣ
	private final static double DIS_PULLMSG = 5.0;
	// private final static String URL_CHECK_ORDER =
	// "http://192.168.0.78:8080/axis2/services/suntesl2webservice/rebackRadio?type=2";
	private final static String NOTIFICATION_DELETED_ACTION = "com.suntown.suntownshop.action.NOTIFICATION_DELETED_ACTION";
	private final static String URL = Constants.DOMAIN_NAME
			+ "axis2/services/sunteslwebservice/firstIosIbeacon";
	private boolean isOnUpdate = false;
	private boolean isOrderActivity = false;
	private boolean isCheckNewOrder = false;
	private boolean isShowNotification = false;
	
	private long enterTime;
	private WifiAdmin wifiAdmin;
	private boolean isBleOn = false;
	private boolean isOnCheck = false;
	boolean isMsgPush = true;
	private OnBeaconFoundListener foundListener;
	// IBeacon��Ϣ�����߳�������
	private final static int THREAD_MSGPUSH_MAX = 5;
	// ��ǰIBeacon��Ϣ�����߳���
	private int curThreadMsgPush = 0;
	
	private String title,content,url;//֪ͨ�������Լ�����
//	private final static int MSG_UPDATE_DA_COMPLETE = 6;

	/**
	 * ����RSSI������룬
	 * 
	 * @param txPower
	 *            ����Ϊ1��ʱ��RSSIֵ
	 * @param rssi
	 *            ��ǰRSSIֵ
	 * @return ����
	 */
	protected static double calculateAccuracy(int txPower, double rssi) {
		if (rssi == 0) {
			return -1.0; // if we cannot determine accuracy, return -1.
		}
		double ratio = rssi * 1.0 / txPower;
        if (ratio < 1.0) {
            return Math.pow(ratio, 10);
        } else {
            double accuracy = (0.89976) * Math.pow(ratio, 7.7095) + 0.111;
            return accuracy;
        }


//		return Math.pow(10, (txPower - rssi) / (10 * 4.1));
		/*
		 * double ratio = rssi * 1.0 / txPower; if (ratio < 1.0) { return
		 * Math.pow(ratio, 10); } else { double accuracy = (0.89976) *
		 * Math.pow(ratio, 7.7095) + 0.111;
		 * 
		 * return accuracy; }
		 */
	}

	public boolean isCheckNewOrder() {
		return isCheckNewOrder;
	}

	public void setCheckNewOrder(boolean isCheckNewOrder) {
		SharedPreferences mSharedPreferences = getSharedPreferences(
				"suntownhuilive", 0);
		SharedPreferences.Editor mEditor = mSharedPreferences.edit();
		mEditor.putBoolean("checkneworder", true);
		mEditor.commit();
		this.isCheckNewOrder = isCheckNewOrder;
	}

	public class LocalBinder extends Binder {
		public LocalService getService() {
			// Toast.makeText(LocalService.this, "getService...",
			// Toast.LENGTH_SHORT).show();
			return LocalService.this.getService();
		}
	}

	// ���÷���BLE�豸�ļ����ӿ�
	public void setOnBeaconFoundListener(OnBeaconFoundListener listener) {
		this.foundListener = listener;
	}

	// ȡ��BLE�豸�ļ����ӿ�
	public void unSetOnBeaconFoundListener() {
		foundListener = null;
	}

	public LocalService getService() {
		return this;
	}

	// ��ʼ��BLE
	private void initBLE() {
		/*
		 * IntentFilter filter = new IntentFilter(
		 * BluetoothDevice.ACTION_ACL_CONNECTED);
		 * this.registerReceiver(mReceiver, filter);
		 */
		
		// ��鵱ǰ�ֻ��Ƿ�֧��ble ����,�����֧���˳�����
		if (!getPackageManager().hasSystemFeature(
				PackageManager.FEATURE_BLUETOOTH_LE)) {
			Toast.makeText(this, "���豸��֧��BLE", Toast.LENGTH_SHORT).show();
			System.out.println("���豸��֧��BLE");

			return;
		}

		// ��ʼ�� Bluetooth adapter, ͨ�������������õ�һ���ο�����������(API����������android4.3�����ϺͰ汾)
		final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
		mBluetoothAdapter = bluetoothManager.getAdapter();

		// ����豸���Ƿ�֧������
		if (mBluetoothAdapter == null) {
			Toast.makeText(this, "���豸��֧������", Toast.LENGTH_SHORT).show();
			System.out.println("���豸��֧������");
			return;
		}

		isBleOn = true;
	}

	private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			System.out.println("Receiverd");
			// ���豸��ʼɨ��ʱ��
			Log.i("fc", "�豸��ʼɨ��");
			if (BluetoothDevice.ACTION_FOUND.equals(action)) {
				// ��Intent�õ�blueDevice����
				BluetoothDevice device = intent
						.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

				if (device.getBondState() != BluetoothDevice.BOND_BONDED) {

					// �ź�ǿ�ȡ�
					short rssi = intent.getExtras().getShort(
							BluetoothDevice.EXTRA_RSSI);
					Log.i("fc", "�ź�ǿ�ȣ�"+rssi);
					Toast.makeText(getApplicationContext(), "RSSI:" + rssi,
							Toast.LENGTH_SHORT).show();
				}

			}
		}
	};

	private void scanLeDevice(final boolean enable) {
		final BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {

			@Override
			public synchronized void onLeScan(final BluetoothDevice device,
					final int rssi, byte[] scanRecord) {
				if (curThreadMsgPush < THREAD_MSGPUSH_MAX) {
					String address = device.getAddress();
					double dis = Math.pow(10, (RSSIA - rssi) / (10 * RSSIN));
					StringBuilder sb = new StringBuilder();
					for (int i = 0; i < scanRecord.length; i++) {
						sb.append(scanRecord[i]);
					}

					//System.out.println("�����豸:" + device.getName() + "��ַ:"
						//	+ address + " ����:" + dis + " ����:" + sb.toString());
					Log.i("fc", "�����豸:" + device.getName() + "��ַ:"
							+ address + " ����:" + dis + " ����:" + sb.toString());
					//����Ƿ�ΪIBeacon
					IBeacon iBeacon = IBeaconClass.fromScanData(device, rssi,
							scanRecord);
					if (iBeacon != null) {
						address = iBeacon.proximityUuid+iBeacon.major+iBeacon.minor;
						System.out.println("����IBeacon:" + iBeacon.name + "��ַ:"
								+ iBeacon.bluetoothAddress + " UUID:"
								+ iBeacon.proximityUuid + " Major:"
								+ iBeacon.major + " Minor:" + iBeacon.minor);
						Log.i("fc", "����IBeacon:" + iBeacon.name + "��ַ:"
								+ iBeacon.bluetoothAddress + " UUID:"
								+ iBeacon.proximityUuid + " Major:"
								+ iBeacon.major + " Minor:" + iBeacon.minor);
					}
					BeaconDb db = new BeaconDb(LocalService.this);
					Beacon beacon = db.getBeacon(address);
					db.Close();
					// ֪ͨǰ̨����BLE�豸
					if (foundListener != null) {
						foundListener.OnFound(address);
				
					}
					
					boolean isPullMsg = false;
					if (dis < DIS_PULLMSG && isMsgPush) {
						System.out.println("Ibeacon�ھ�����"+address);
						Log.i("fc", "Ibeacon����:"+address);
						if (beacon == null) {
							isPullMsg = true;
						} else {
							SimpleDateFormat format = new SimpleDateFormat(
									"yyyy-MM-dd");
							String lastDay = format.format(beacon.getDate());
							String today = format.format(new Date());
							System.out.println("today:" + today + " lastday:"
									+ lastDay);
							if (!lastDay.equals(today)
									|| (lastDay.equals(today) && beacon
											.getOnpull() == 0)) {
								isPullMsg = true;
							}
						}
					}
					
					
					System.out.println("Ibeacon�Ƿ�����"+isPullMsg);
					isPullMsg = true;
					if (isPullMsg&&iBeacon!=null) {
						db = new BeaconDb(LocalService.this);
						beacon = new Beacon(address, new Date(), 1);
						db.insertBeacon(beacon);
						db.Close();
						SharedPreferences mSharedPreferences = getSharedPreferences(
								"suntownshop", 0);
						String userId = mSharedPreferences.getString("userId","");
						Log.i("fc", "����Ibeacon��Ϣ:"+URL + "?memid=" + userId + "&uuid="
								+ iBeacon.proximityUuid+"&major="+iBeacon.major+"&minor="+iBeacon.minor);
						GetJsonRunnable getJsonRunnable = new GetJsonRunnable(
								URL + "?memid=" + userId + "&uuid="
										+ iBeacon.proximityUuid+"&major="+iBeacon.major+"&minor="+iBeacon.major, MSG_PULLMSG_COMPLETE,
								address, mHandler);
						Log.i("fc", "GetJsonRunnable11111");
						new Thread(getJsonRunnable).start();
						Log.i("fc", "GetJsonRunnable33333");
						curThreadMsgPush++;
						System.out.println(URL + "?memid=" + userId
								+ "&ibeacon=" + address);
					}

				}
			}

		};
		
		if (enable && !mScanning) {
			// Stops scanning after a pre-defined scan period.
			mHandler.postDelayed(new Runnable() {
				@Override
				public void run() {
					mScanning = false;
					/*
					 * BluetoothLeScanner scanner = mBluetoothAdapter
					 * .getBluetoothLeScanner(); scanner.stopScan(scanCallback);
					 */
					mBluetoothAdapter.stopLeScan(mLeScanCallback);

				}
			}, SCAN_PERIOD);
			mScanning = true;

			/*
			 * BluetoothLeScanner scanner = mBluetoothAdapter
			 * .getBluetoothLeScanner();
			 * 
			 * ScanSettings settings = new ScanSettings.Builder()
			 * .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build();
			 * 
			 * List<ScanFilter> filters = new ArrayList<ScanFilter>();
			 * scanner.startScan(filters, settings, scanCallback);
			 */

			mBluetoothAdapter.startLeScan(mLeScanCallback);
		} else if (mScanning) {
			mScanning = false;
			/*
			 * BluetoothLeScanner scanner = mBluetoothAdapter
			 * .getBluetoothLeScanner();
			 * 
			 * scanner.stopScan(scanCallback);
			 */

			mBluetoothAdapter.stopLeScan(mLeScanCallback);
		}

	}

	private double RSSIA = -60;
	private double RSSIN = 4.1;

	// Device scan callback.

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return new LocalBinder();
	}

	@Override
	public boolean onUnbind(Intent intent) {
		// TODO Auto-generated method stub
		isOrderActivity = false;
		return super.onUnbind(intent);
	}

	@Override
	public void onCreate() {
		// TODO Auto-generated method stub
		// Notification notification = new Notification();
		// startForeground(R.string.service_foreground, notification);
		// Toast.makeText(this, "��ݳ��з���ʼ��...", Toast.LENGTH_SHORT).show();

		wifiAdmin = new WifiAdmin(this);
		// �Ǽ���Ϣ�����ͣ������ظ�����
		SharedPreferences mSharedPreferences = getSharedPreferences(
				"suntownshop", 0);
		SharedPreferences.Editor mEditor = mSharedPreferences.edit();
		mEditor.putBoolean("ismsgpushshow", false);
		mEditor.commit();
		initBLE();
		checkShopcart();
		scanLeDevice(true);
		showNotification(R.drawable.ic_launcher,"��Ʒ��Ϣ","Ŀǰ��17����Ʒ���ڴ���");
		new Thread(checkIBeaconRunnable).start();
	}

	@Override
	public void onStart(Intent intent, int startId) {
		// TODO Auto-generated method stub
		Log.i("fc", "����onStart");
		super.onStart(intent, startId);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		// TODO Auto-generated method stub
		flags = START_STICKY;
		return super.onStartCommand(intent, flags, startId);
	}

	@Override
	public void onDestroy() {
		// TODO Auto-generated method stub
		// stopForeground(true);
		Log.i("fc", "����onDestroy");
		System.out.println("��ݳ��з��������");
		Toast.makeText(this, "��ݳ��з��������...", Toast.LENGTH_SHORT).show();
	}

	// ��������ź��߳�
	private Runnable checkIBeaconRunnable = new Runnable() {

		@Override
		public void run() {
			// TODO Auto-generated method stub
			while (true) {
				try {
					SharedPreferences mSharedPreferences = getSharedPreferences(
							"suntownshop", 0);
					isMsgPush = mSharedPreferences.getBoolean("msgpush", true);
					if (isBleOn && (isMsgPush || foundListener != null)) {
						if (!mBluetoothAdapter.isEnabled()) {
							mBluetoothAdapter.enable();
						}
						scanLeDevice(true); // ����BLE�豸
					} /*
					 * else { scanWifi(); //����WIFI���� }
					 */
					Thread.sleep(TIME_INTERVAL_RUN);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}

	};

	// ����WIFI
	private void scanWifi() {
		wifiAdmin.startScan();
		if (wifiAdmin.checkApStats("14:75:90:e0:b8:54")) {
			isShowNotification = true;
			enterTime = System.currentTimeMillis();
			SharedPreferences mSharedPreferences = getSharedPreferences(
					"suntownshop", 0);
			SharedPreferences.Editor mEditor = mSharedPreferences.edit();
			mEditor.putLong("entertime", enterTime);
			mEditor.commit();
//			 showNotification(R.drawable.ic_launcher, "�ѽ��볬��", "�ѽ��볬�з�Χ");
		}
	}

	public void readMessage(boolean isRead) {
		if (isRead) {
			isShowNotification = false;
			cancelUpdateNotification();
		}
	}

	public void setOrderActivity(boolean isOrder) {
		this.isOrderActivity = isOrder;
		if (isOrder) {
			cancelUpdateNotification();
		}
	}

	private Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			// TODO Auto-generated method stub
			Bundle bundle;
			String strMsg;
			JSONObject jsonObj;
			JSONArray jsonArray;
			switch (msg.what) {
			case MSG_PULLMSG_COMPLETE:
				Log.i("fc", "@@@@MSG_PULLMSG_COMPLETE");
				
				curThreadMsgPush--;
				
				bundle = msg.getData();
				strMsg = bundle.getString("MSG_JSON");
				try {
					jsonObj = new JSONObject(strMsg);
					int state = jsonObj.getInt("RESULT");
					String address = "";
					if (bundle.containsKey("MSG_PLUS")) {
						address = bundle.getString("MSG_PLUS");
					}
//					String address = jsonObj.getString("IBEACON");
					BeaconDb db = new BeaconDb(LocalService.this);
					Beacon beacon = new Beacon(address, new Date(), 2);
					db.insertBeacon(beacon);
					db.Close();
  					
					if (state == 0) {
						String title = jsonObj.getString("TITLE");
						String content = jsonObj.getString("CONTEXT");
						String url = jsonObj.getString("URL");
						sendLiveNotification(R.drawable.ic_launcher, title,
								content, url, address);
						// sendLiveNotification(R.drawable.ic_launcher,
						// "��Ʒ�ٱ�֪ͨ",
						// "���������������9����Ʒ���ٽ������ڣ��뾡�촦����\n����鿴���顣", url, address);
					}
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					if (bundle.containsKey("MSG_PLUS")) {
						String msgPlus = bundle.getString("MSG_PLUS");
						BeaconDb db = new BeaconDb(LocalService.this);
						db.updateBeacon(msgPlus, 0);
						db.Close();
					}
					System.out.println("֪ͨ��Ϣ��ȡ����");
					e.printStackTrace();
				}
				break;
			case MSG_UPDATE_DA_COMPLETE:
				Log.i("fc", "####MSG_UPDATE_DA_COMPLETE");
				bundle = msg.getData();
				strMsg = bundle.getString("MSG_JSON");
				try {
					jsonObj = new JSONObject(strMsg);
					int state = jsonObj.getInt("RESULT");
					if (state == 0) {
						SharedPreferences mSharedPreferences = getSharedPreferences(
								"suntownshop", 0);
						SharedPreferences.Editor mEditor = mSharedPreferences
								.edit();
						SimpleDateFormat format = new SimpleDateFormat(
								"yyyy-MM-dd");
						String now = format.format(new Date());
						mEditor.putString("shopcartcheckdate", now);
						mEditor.commit();
						isOnCheck = false;
					}
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					System.out.println(e.getMessage());
					e.printStackTrace();
				}
				break;
			case MSG_NEWORDER:
				break;
			case CHECK_UPDATE_INFO:
				break;
			case DOWNLOAD_ERR:
				break;
			case DOWNLOAD_SUSS:
				break;
			case DOWNLOAD_CANCEL:
				break;
			case NETWORK_ERR:
				System.out.println("�������");
				bundle = msg.getData();
				// �����IBeacon��ȡ������Ϣ�������򽫸�IBeacon����Ϊ������Ϣδ����״̬
				if (bundle.containsKey("MSG_PLUS")) {
					curThreadMsgPush--;
					String msgPlus = bundle.getString("MSG_PLUS");
					BeaconDb db = new BeaconDb(LocalService.this);
					db.updateBeacon(msgPlus, 0);
					db.Close();
				}
				break;
			}

			super.handleMessage(msg);
		}
	};

	private boolean mIsUpdateCancel = false;

	public void updateCancel() {
		mIsUpdateCancel = true;
	}

	public boolean isOnUpdate() {
		return isOnUpdate;
	}

	private boolean mIsUpdateInBack = false;

	private void cancelUpdateNotification() {
		NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		mNotificationManager.cancel(R.string.app_name);

	}

	private void refreshUpdateNotification(int max, int current) {
		if (mIsUpdateInBack) {
			NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(
					getApplicationContext())
					// .setSmallIcon(R.drawable.ic_launcher)
					.setContentTitle("�Ż�ȯ").setContentText("���³�������")
					.setOngoing(true).setProgress(max, current, false);
			// Intent resultIntent = new
			// Intent(getApplicationContext(),DoUpdateActivity.class);

			TaskStackBuilder stackBuilder = TaskStackBuilder
					.create(getApplicationContext());
			// stackBuilder.addParentStack(DoUpdateActivity.class);
			// stackBuilder.addNextIntent(resultIntent);
			PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(
					0, PendingIntent.FLAG_UPDATE_CURRENT);
			mBuilder.setContentIntent(resultPendingIntent);
			NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
			// mNotificationManager.notify(R.string.update_in_back,mBuilder.build());
		}
	}

	public void updateInBack() {
		mIsUpdateInBack = true;
	}

	public void updateInFront() {
		mIsUpdateInBack = false;
		cancelUpdateNotification();
	}

	protected void installApk(File file) {
		Intent intent = new Intent();
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		// ִ�ж���
		intent.setAction(Intent.ACTION_VIEW);
		// ִ�е���������
		intent.setDataAndType(Uri.fromFile(file),
				"application/vnd.android.package-archive");// ���߰����˴�AndroidӦΪandroid��������ɰ�װ����
		startActivity(intent);
	}

	private void sendLiveNotification(int icon, String title, String content,
			String url, String address) {
		Intent intent = new Intent(this, SpecialGoodsListActivity.class);
		Bundle bundle = new Bundle();
		int noId = MyMath.getRandom(100000000, 999999999);
		bundle.putBoolean("fromnotification", true);
		url = url.replace("\u003d", "=");
		if (url.indexOf("http://") < 0) {
			url = "http://" + url;
		}
		bundle.putString("url", url);
		bundle.putInt("noid", noId);
		bundle.putString("title", title);
		bundle.putString("address", address);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		intent.putExtras(bundle);

		// ��Ҫ�����õ��֪ͨʱ��ʾ���ݵ���
		PendingIntent m_PendingIntent = PendingIntent.getActivity(this, noId,
				intent, 0);
		NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(
				this);

		mBuilder.setContentTitle(title);
		mBuilder.setTicker(title);
		mBuilder.setContentText(content);
		mBuilder.setLargeIcon(BitmapFactory.decodeResource(getResources(),
				R.drawable.ic_launcher));
		mBuilder.setSmallIcon(R.drawable.ic_launcher);
		mBuilder.setDefaults(Notification.DEFAULT_ALL);
		mBuilder.setWhen(System.currentTimeMillis());
		mBuilder.setContentIntent(m_PendingIntent);
		intent = new Intent(NOTIFICATION_DELETED_ACTION);
		intent.putExtras(bundle);
		mBuilder.setDeleteIntent(PendingIntent.getBroadcast(this, noId, intent,
				0));
		NotificationCompat.BigTextStyle textStyle = new NotificationCompat.BigTextStyle();
		textStyle.setBigContentTitle(title).bigText(content);
		// .setSummaryText(title);
		mBuilder.setStyle(textStyle);
		// ����֪ͨ����Ϣ��������
		NotificationManager mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		mNotificationManager.notify(noId, mBuilder.build());
		
		System.out.println("֪ͨ��Ϣ��url="+url+", title:"+title);
		Log.i("fc", "֪ͨ��Ϣ��url="+url+", title:"+title);
	}

	private void sendLiveNotification(int icon, String tickertext,
			String content, ArrayList<ParcelableGoods> goodsList) {
		Intent intent = new Intent(this, SpecialGoodsListActivity.class);

		Bundle bundle = new Bundle();
		bundle.putBoolean("fromnotification", true);
		bundle.putParcelableArrayList("goodslist", goodsList);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		intent.putExtras(bundle);
		// ��Ҫ�����õ��֪ͨʱ��ʾ���ݵ���
		PendingIntent m_PendingIntent = PendingIntent.getActivity(this,
				R.string.app_name, intent, 0);
		NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(
				this);

		mBuilder.setContentTitle(tickertext);
		mBuilder.setTicker(tickertext);
		mBuilder.setContentText(content);
		mBuilder.setLargeIcon(BitmapFactory.decodeResource(getResources(),
				R.drawable.ic_launcher));
		mBuilder.setSmallIcon(R.drawable.ic_launcher);
		mBuilder.setDefaults(Notification.DEFAULT_ALL);
		mBuilder.setWhen(System.currentTimeMillis());
		mBuilder.setContentIntent(m_PendingIntent);
		mBuilder.setDeleteIntent(PendingIntent.getBroadcast(this,
				R.string.app_name, new Intent(NOTIFICATION_DELETED_ACTION), 0));
		// ����֪ͨ����Ϣ��������
		NotificationManager mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		mNotificationManager.notify(R.string.app_name, mBuilder.build());
		// �Ǽ���Ϣ�����ͣ������ظ�����
		SharedPreferences mSharedPreferences = getSharedPreferences(
				"suntownshop", 0);
		SharedPreferences.Editor mEditor = mSharedPreferences.edit();
		mEditor.putBoolean("ismsgpushshow", true);
		mEditor.commit();
	}

	@SuppressWarnings("deprecation")
	public void showNotification(int icon, String tickertext, String content
			) {
		ArrayList<ParcelableGoods> goodsList = null;
		sendBroadcast(new Intent(NOTIFICATION_DELETED_ACTION));
		// ����֪ͨ����Ϣ��������
		NotificationManager m_NotificationManager;
		Intent m_Intent;
		PendingIntent m_PendingIntent;
		// ����Notification����
		Notification m_Notification;
		// ����֪ͨ
		m_NotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		// ���֪ͨʱת������
		m_Intent = new Intent(LocalService.this, SpecialGoodsListActivity.class);

		Bundle bundle = new Bundle();
		bundle.putBoolean("fromnotification", true);
		bundle.putParcelableArrayList("goodslist", goodsList);
		m_Intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		m_Intent.putExtras(bundle);
		// ��Ҫ�����õ��֪ͨʱ��ʾ���ݵ���
		m_PendingIntent = PendingIntent.getActivity(LocalService.this,
				R.string.app_name, m_Intent, 0);

		// Ҫ��������Բ�ͬ����Ҫ���ID��ͬ���ڹȸ�����������������ܻ�������Ϊ�����������ν��һֱʹ��0
		// ����Notification����
		m_Notification = new Notification();
		//

		m_Notification.contentIntent = m_PendingIntent;
		Intent deleteIntent = new Intent(NOTIFICATION_DELETED_ACTION);
		m_Notification.deleteIntent = PendingIntent.getBroadcast(
				LocalService.this, R.string.app_name, deleteIntent, 0);
		// ������Զ������ͼʾ��ֻ���������ú�����ڵ�����Զ�ɾ��

		m_Notification.flags = Notification.FLAG_ONLY_ALERT_ONCE;
		// ����֪ͨ��״̬����ʾ��ͼ��
		m_Notification.icon = R.drawable.ic_launcher;
		// �����ǵ��֪ͨʱ��ʾ������
		m_Notification.tickerText = tickertext;

		// ֪ͨʱ����Ĭ�ϵ�����
		m_Notification.defaults = Notification.DEFAULT_ALL;
		// ����֪ͨ��ʾ�Ĳ���
		m_Notification.setLatestEventInfo(LocalService.this, tickertext,
				content, m_PendingIntent);
		// ��������Ϊִ�����֪ͨ
		m_NotificationManager.notify(R.string.app_name, m_Notification);

		// ͬ����IDӦ�ò�ͬ���������˵����������ҵ����˵��

	}

	private final static String URL_UPDATE_AD = Constants.DOMAIN_NAME
			+ "axis2/services/sunteslwebservice/getda";

	private void checkShopcart() {
		if (!isOnCheck) {
			SharedPreferences mSharedPreferences = getSharedPreferences(
					"suntownshop", 0);
			String userId = mSharedPreferences.getString("userId", "");
			String checkDate = mSharedPreferences.getString(
					"shopcartcheckdate", "");
			SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
			Date now = new Date();
			Calendar calendar = Calendar.getInstance();
			calendar.setTime(now);
			calendar.add(Calendar.MONTH, -1);
			String strNow = format.format(calendar.getTime());
			System.out.println("checkDate:" + checkDate + " now:" + strNow);
			if (!strNow.equals(checkDate)) {
				ShopCartDb db = new ShopCartDb(getApplicationContext(), userId);
				ArrayList<CartGoods> list = db.getGoodsByTime(strNow);
				db.Close();
				System.out.println("goods:" + list.size());
				if (list.size() > 0) {
					try {
						String strJson = JsonBuilder
								.makeDAJson(0, userId, list);
						System.out.println(strJson);
						Log.i("fc", "Json���ݣ�"+strJson);
						HashMap<String, String> params = new HashMap<String, String>();
						params.put("strMsg", strJson);
						isOnCheck = true;
						PostAsyncTask postAsyncTask = new PostAsyncTask(
								URL_UPDATE_AD, upADCallback);
						postAsyncTask.execute(params);
					} catch (JSONException e) {
						// TODO Auto-generated catch block
						System.out.println(e.getMessage());
						e.printStackTrace();
					}
				} else {

					SharedPreferences.Editor mEditor = mSharedPreferences
							.edit();
					mEditor.putString("shopcartcheckdate", strNow);
					mEditor.commit();
				}
			}
		}
	}

	private OnCompleteCallback upADCallback = new OnCompleteCallback() {

		@Override
		public void onComplete(boolean isOk, String msg) {
			// TODO Auto-generated method stub
			isOnCheck = false;
			if (isOk) {
				JSONObject jsonObj;
				try {
					msg = XmlParser.parse(msg, "UTF-8", "return");
					jsonObj = new JSONObject(msg);
					int sendState = jsonObj.getInt("RESULT");
					if (sendState == 0) {
						SharedPreferences mSharedPreferences = getSharedPreferences(
								"suntownshop", 0);
						String userId = mSharedPreferences.getString("userId",
								"");
						SharedPreferences.Editor mEditor = mSharedPreferences
								.edit();
						SimpleDateFormat format = new SimpleDateFormat(
								"yyyy-MM-dd");
						String now = format.format(new Date());
						mEditor.putString("shopcartcheckdate", now);
						mEditor.commit();
						ShopCartDb db = new ShopCartDb(getApplicationContext(),
								userId);
						db.deleteGoodsByTime(now);
						db.Close();
					}
				} catch (Exception e) {
					// Toast.makeText(getApplicationContext(),
					// "���������ش������Ժ�����...",Toast.LENGTH_SHORT).show();

					e.printStackTrace();
				}
			} else {
				// Toast.makeText(getApplicationContext(),
				// "���ӳ�ʱ�����Ժ�����...",Toast.LENGTH_SHORT).show();
			}

		}
	};

	public interface OnBeaconFoundListener {
		public void OnFound(String name);
	}
	
	/**
	 * ͨ��Ibeacon��Ϣ���������������
	 
	public void downLoadIbeacon(String ibeaconUrl){
		HttpURLConnection conn = null; //���Ӷ���  
		InputStream is = null;  
		String resultData = "";  
		try {
			URL url = new URL(ibeaconUrl);
			conn = (HttpURLConnection)url.openConnection(); //ʹ��URL��һ������  
			conn = (HttpURLConnection)url.openConnection(); //ʹ��URL��һ������  
			conn.setDoInput(true); //����������������������  
			conn.setDoOutput(true); //������������������ϴ�  
			conn.setUseCaches(false); //��ʹ�û���  
			conn.setRequestMethod("GET"); //ʹ��get����  
			is = conn.getInputStream();   //��ȡ����������ʱ��������������  
			InputStreamReader isr = new InputStreamReader(is);  
			BufferedReader bufferReader = new BufferedReader(isr);  
			String inputLine  = "";  
			while((inputLine = bufferReader.readLine()) != null){  
			resultData += inputLine + "\n";  
			}  

		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		finally{  
			       if(is != null){  
			            try {  
			               is.close();  
			             } catch (IOException e) {  
			         // TODO Auto-generated catch block  
			                  e.printStackTrace();  
			             }  
			        }  
			       if(conn != null){  
			     conn.disconnect();  
			}  
		}		
	}*/
}