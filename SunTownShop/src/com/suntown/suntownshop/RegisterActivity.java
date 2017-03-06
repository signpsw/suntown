package com.suntown.suntownshop;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.nostra13.universalimageloader.cache.disc.naming.Md5FileNameGenerator;
import com.suntown.suntownshop.model.Goods;
import com.suntown.suntownshop.runnable.GetJsonRunnable;
import com.suntown.suntownshop.utils.FormatValidation;
import com.suntown.suntownshop.utils.JsonParser;
import com.suntown.suntownshop.utils.Md5Manager;
import com.suntown.suntownshop.utils.SmsContent;
import com.suntown.suntownshop.widget.ConfirmDialog;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
/**
 * ע��
 *
 * @author Ǯ��
 * @version 2015��2��21�� ����9:47:03
 *
 */
public class RegisterActivity extends Activity {
	private EditText etUsername;
	private EditText etPass;
	private EditText etCheckCode;
	private ImageView ivUserCheck;
	private CheckBox cbShowPass;
	private View viewCheckCode;
	private TextView tvUserErr;
	private TextView tvPassErr;
	private TextView tvCheckcodeErr;
	private Button btnCheckCode;
	private Button btnRegister;
	private String username;
	private String password;
	private String mCheckcode;
	private int mode;
	private final static int MODE_PHONE = 1;
	private final static int MODE_EMAIL = 2;
	private SmsContent smsContent;
	private boolean isUsernameOk = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.myaccount_register);
		etUsername = (EditText) findViewById(R.id.et_telphone);
		etPass = (EditText) findViewById(R.id.et_pass);
		etCheckCode = (EditText) findViewById(R.id.et_checkcode);
		ivUserCheck = (ImageView) findViewById(R.id.iv_checkusername);
		cbShowPass = (CheckBox) findViewById(R.id.cb_showpass);

		viewCheckCode = findViewById(R.id.view_checkcode);
		tvUserErr = (TextView) findViewById(R.id.tv_username_err);
		tvPassErr = (TextView) findViewById(R.id.tv_pass_err);
		tvCheckcodeErr = (TextView) findViewById(R.id.tv_checkcode_err);
		btnCheckCode = (Button) findViewById(R.id.btn_checkcode);
		btnRegister = (Button) findViewById(R.id.btn_register);

		etUsername.setOnFocusChangeListener(userFocusChange);
		etPass.setOnFocusChangeListener(passFocusChange);
		smsContent = new SmsContent(RegisterActivity.this, new Handler(),
				etCheckCode);
		ivUserCheck.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				if (!isUsernameOk) {
					etUsername.setText("");
					etUsername.setFocusable(true);
				}
			}
		});
		cbShowPass.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				// TODO Auto-generated method stub
				int sel = etPass.getSelectionStart();
				if (isChecked) {
					etPass.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
				} else {
					etPass.setInputType(InputType.TYPE_CLASS_TEXT
							| InputType.TYPE_TEXT_VARIATION_PASSWORD);
				}
				etPass.setSelection(sel);
			}
		});
		etCheckCode.setOnFocusChangeListener(new OnFocusChangeListener() {

			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				// TODO Auto-generated method stub
				if (hasFocus) {
					tvCheckcodeErr.setVisibility(View.GONE);
				} else {
					checkCheckcode();
				}
			}
		});

		btnCheckCode.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				getCheckCode();
			}
		});

		btnRegister.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				register();
			}
		});
	}

	private OnFocusChangeListener passFocusChange = new OnFocusChangeListener() {

		@Override
		public void onFocusChange(View v, boolean hasFocus) {
			// TODO Auto-generated method stub
			if (hasFocus) {
				tvPassErr.setVisibility(View.GONE);
			} else {
				checkPassword();
			}
		}
	};

	private OnFocusChangeListener userFocusChange = new OnFocusChangeListener() {

		@Override
		public void onFocusChange(View v, boolean hasFocus) {
			// TODO Auto-generated method stub
			ivUserCheck.setVisibility(View.VISIBLE);
			if (hasFocus) {
				ivUserCheck.setImageResource(R.drawable.icon_clean);
				tvUserErr.setVisibility(View.GONE);
			} else {
				checkUsername();
			}
		}
	};

	private boolean checkCheckcode() {
		String checkcode = etCheckCode.getText().toString();
		if (checkcode == null || "".equals(checkcode)) {
			tvCheckcodeErr.setText("��֤�벻��Ϊ��!");
		} else if (checkcode.length() != 6
				|| !FormatValidation.isNumeric(checkcode)) {
			tvCheckcodeErr.setText("������6λ������֤��!");
		} else {
			mCheckcode = checkcode;
			return true;
		}
		tvCheckcodeErr.setVisibility(View.VISIBLE);
		return false;
	}

	private boolean checkPassword() {
		String pass = etPass.getText().toString();
		if (pass == null || "".equals(pass)) {
			tvPassErr.setText("���벻��Ϊ��!");
		} else if (pass.length() < 6 || pass.length() > 20) {
			tvPassErr.setText("�������Ϊ6-20λ!");
		} else if (FormatValidation.isCharacter(pass)) {
			tvPassErr.setText("���벻��ȫΪ��ĸ!");
		} else if (FormatValidation.isNumeric(pass)) {
			tvPassErr.setText("���벻��ȫΪ����!");
		} else if (FormatValidation.isSymbol(pass)) {
			tvPassErr.setText("���벻��ȫΪ����!");
		} else {
			password = pass;
			return true;
		}
		tvPassErr.setVisibility(View.VISIBLE);
		return false;
	}

	private boolean checkUsername() {
		String uname = etUsername.getText().toString();
		isUsernameOk = false;
		if (uname == null || "".equals(uname)) {
			tvUserErr.setText("�ֻ��Ų���Ϊ��!");
		} else if (FormatValidation.isMobileNO(uname)) {
			mode = MODE_PHONE;
			ivUserCheck.setImageResource(R.drawable.icon_ok);
			isUsernameOk = true;
			viewCheckCode.setVisibility(View.VISIBLE);
			username = uname;
			return true;
		} /*else if (FormatValidation.isEmail(uname)) {
			mode = MODE_EMAIL;
			ivUserCheck.setImageResource(R.drawable.icon_ok);
			isUsernameOk = true;
			viewCheckCode.setVisibility(View.GONE);
			username = uname;
			return true;
		}*/ else {
			tvUserErr.setText("�ֻ��Ų���ȷ!");
		}
		ivUserCheck.setImageResource(R.drawable.icon_no);
		tvUserErr.setVisibility(View.VISIBLE);
		return false;
	}

	private boolean checkRegister() {
		if (!checkUsername() || !checkPassword()
				|| (mode == MODE_PHONE && !checkCheckcode())) {
			return false;
		}
		return true;
	}

	private void getCheckCode() {
		String phone = etUsername.getText().toString();
		if (!FormatValidation.isMobileNO(phone)) {
			tvUserErr.setText("�ֻ����벻�Ϸ�!");
		} else {

			// ע����ű仯����
			this.getContentResolver().registerContentObserver(
					Uri.parse("content://sms/"), true, smsContent);

			username = phone;
			showProgress(true);
			GetJsonRunnable getJsonRunnable = new GetJsonRunnable(
					URL_GETCHECKCODE + username, MSG_GETCHECKCODE, handler);
			new Thread(getJsonRunnable).start();
		}
	}

	private void register() {

		if (checkRegister()) {
			try {
				password = Md5Manager.md5(password);
			} catch (Exception e) {
				// TODO: handle exception
				System.out.println("err------>" + e.getMessage());
				e.printStackTrace();
			}
			System.out.println("password------>" + password);
			if (password == null || "".equals(password)) {
				Toast.makeText(getApplicationContext(), "ע��ʧ�ܣ����ܳ���",
						Toast.LENGTH_SHORT).show();
				return;
			}
			if (mode == MODE_PHONE) {
				showProgress(true);
				GetJsonRunnable getJsonRunnable;
				try {
					getJsonRunnable = new GetJsonRunnable(URL_REGISTER
							+ username + "&code=" + mCheckcode + "&pwd="
							+ URLEncoder.encode(password, "UTF-8"),
							MSG_REGISTER, handler);
					new Thread(getJsonRunnable).start();
				} catch (UnsupportedEncodingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			} else if (mode == MODE_EMAIL) {
				showProgress(true);
				GetJsonRunnable getJsonRunnable;
				try {
					getJsonRunnable = new GetJsonRunnable(URL_REGISTER_EMAIL
							+ username + "&pwd="
							+ URLEncoder.encode(password, "UTF-8"),
							MSG_REGISTER, handler);
					new Thread(getJsonRunnable).start();
				} catch (UnsupportedEncodingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}
		}

	}

	private ProgressDialog mPDialog;

	public void showProgress(final boolean show) {
		if (show) {
			mPDialog = new ProgressDialog(RegisterActivity.this);
			// ʵ����
			mPDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			// ���ý�������񣬷��ΪԲ�Σ���ת��
			// pDialog.setTitle("Google");
			// ����ProgressDialog ����
			mPDialog.setMessage(getString(R.string.wait_a_minute));
			// ����ProgressDialog ��ʾ��Ϣ
			// pDialog.setIcon(R.drawable.ic_launcher);
			// ����ProgressDialog ����ͼ��
			// mypDialog.setButton();
			// ����ProgressDialog ��һ��Button
			mPDialog.setIndeterminate(false);
			// ����ProgressDialog �Ľ������Ƿ���ȷ
			mPDialog.setCancelable(false);
			// ����ProgressDialog �Ƿ���԰��˻ذ���ȡ��
			mPDialog.show();
		} else {
			if (mPDialog != null && mPDialog.isShowing()) {
				mPDialog.dismiss();
				mPDialog = null;
			}
		}
	}

	private void login(String userId, String phone, String voucher) {
		SharedPreferences mSharedPreferences = getSharedPreferences(
				"suntownshop", 0);
		SharedPreferences.Editor mEditor = mSharedPreferences.edit();
		mEditor.putString("userId", userId);
		mEditor.putString("m_name", "");
		mEditor.putString("m_cardno", "");
		mEditor.putString("m_age", "");
		mEditor.putString("m_address", "");
		mEditor.putInt("m_sex", 0);
		mEditor.putString("m_voucher", voucher);
		mEditor.putString("username", phone);
		mEditor.putString("showname", phone);
		if (mode == MODE_PHONE) {
			mEditor.putString("mobile", phone);
		}
		mEditor.putString("password", "");
		mEditor.putBoolean("isremember", false);
		mEditor.putBoolean("islogin", true);
		mEditor.putInt("logintype", 0);
		mEditor.commit();
		// ˢ�¹��ﳵ
		sendBroadcast(new Intent(Constants.ACTION_SHOPCART_CHANGED));

		hideInput();
		Intent intent = new Intent(RegisterActivity.this, MainTabActivity.class);
		Bundle b = new Bundle();
		b.putInt("gototab", 3);
		b.putBoolean("login", true);
		intent.putExtras(b);
		startActivity(intent);
		finish();

	}

	private final static int MSG_GETCHECKCODE = 1;
	private final static int MSG_REGISTER = 2;
	private final static int MSG_ERROR = -1;
	private final static String URL_GETCHECKCODE = Constants.DOMAIN_NAME
			+ "axis2/services/sunteslwebservice/checkCodeSend?moblie=";
	private final static String URL_REGISTER = Constants.DOMAIN_NAME
			+ "axis2/services/sunteslwebservice/mobileCodeBack?moblie=";
	private final static String URL_REGISTER_EMAIL = Constants.DOMAIN_NAME
			+ "axis2/services/sunteslwebservice/mailRegister?mail=";
	private Handler handler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			// TODO Auto-generated method stub
			showProgress(false);
			Bundle bundle = msg.getData();
			String strMsg;
			JSONObject jsonObj;
			switch (msg.what) {
			case MSG_REGISTER:
				strMsg = bundle.getString("MSG_JSON");
				try {
					jsonObj = new JSONObject(strMsg);
					int sendState = jsonObj.getInt("RELSUT");
					if (sendState == 0) {
						String userId = jsonObj.getString("ID");
						String voucher = jsonObj.getString("LOGINTOKEN");
						if (userId != null && !"".equals(userId)) {
							Toast.makeText(getApplicationContext(), "ע��ɹ�",
									Toast.LENGTH_LONG).show();
							login(userId, username, voucher);

						}
					} else if (sendState == 1) {
						tvCheckcodeErr.setVisibility(View.VISIBLE);
						if (mode == MODE_PHONE) {

							tvCheckcodeErr.setText("��֤����ʧЧ�������»�ȡ��֤��!");
						} else {

							tvCheckcodeErr.setText("�������Ѿ���ע�ᣬ�������������!");
						}
					} else {
						tvCheckcodeErr.setVisibility(View.VISIBLE);
						if (mode == MODE_PHONE) {
							tvCheckcodeErr.setText("��֤�����!");
						} else {
							tvCheckcodeErr.setText("ע��ʧ��!");
						}
					}
				} catch (Exception e) {
					// TODO Auto-generated catch block
					Toast.makeText(getApplicationContext(),
							"ERROR:ע������������:" + e.getMessage(),
							Toast.LENGTH_SHORT).show();
					e.printStackTrace();
				}
				break;
			case MSG_GETCHECKCODE:
				strMsg = bundle.getString("MSG_JSON");
				try {
					jsonObj = new JSONObject(strMsg);
					int sendState = jsonObj.getInt("RESULT");
					if (sendState == 0) {
						Toast.makeText(getApplicationContext(),
								"�ѷ�����֤�뵽�ֻ�����" + username, Toast.LENGTH_LONG)
								.show();
						btnCheckCode.setEnabled(false);
						etCheckCode.requestFocus();
						countdown = 60;
						btnCheckCode.setText(countdown + "����ط�");
						handler.postDelayed(runnableTimer, 1000);
					} else if (sendState == 1) {
						tvUserErr.setVisibility(View.VISIBLE);
						tvUserErr.setText("���ֻ������Ѿ���ע�ᣬ������ֻ�����!");
						ivUserCheck.setImageResource(R.drawable.icon_no);
					} else {
						Toast.makeText(getApplicationContext(),
								"��֤�뷢��ʧ�ܣ����Ժ�����", Toast.LENGTH_LONG).show();
					}
				} catch (Exception e) {
					// TODO Auto-generated catch block
					Toast.makeText(getApplicationContext(),
							"ERROR:��֤���������:" + e.getMessage(),
							Toast.LENGTH_SHORT).show();
					e.printStackTrace();
				}

				break;
			case MSG_ERROR:
				strMsg = bundle.getString("MSG_ERR");
				Toast.makeText(getApplicationContext(), "���ӳ�ʱ�����Ժ�����...",
						Toast.LENGTH_LONG).show();
				break;
			}
			showProgress(false);
			super.handleMessage(msg);
		}

	};

	private void hideInput() {
		// �������뷨
		InputMethodManager imm = (InputMethodManager) getApplicationContext()
				.getSystemService(Context.INPUT_METHOD_SERVICE);
		// ��ʾ�����������뷨
		imm.hideSoftInputFromWindow(RegisterActivity.this.getCurrentFocus()
				.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
	}

	private int countdown = 60;

	Runnable runnableTimer = new Runnable() {
		@Override
		public void run() {
			countdown--;
			if (countdown > 0) {
				btnCheckCode.setText(countdown + "����ط�");
				handler.postDelayed(this, 1000);
			} else {
				btnCheckCode.setText(getString(R.string.getcheckcode));
				btnCheckCode.setEnabled(true);
			}
		}
	};

	public void close(View v) {
		hideInput();
		finish();
	}
}