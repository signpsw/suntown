package com.suntown.suntownshop;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.esri.core.renderer.RGBRenderer;
import com.handmark.pulltorefresh.library.PullToRefreshGridView;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;
import com.suntown.suntownshop.adapter.GridGoodsListAdapter;
import com.suntown.suntownshop.adapter.PrepareToBuyAdapter;
import com.suntown.suntownshop.asynctask.PostAsyncTask;
import com.suntown.suntownshop.asynctask.PostAsyncTask.OnCompleteCallback;
import com.suntown.suntownshop.db.FavoriteDb;
import com.suntown.suntownshop.db.ShopCartDb;
import com.suntown.suntownshop.model.Evaluate;
import com.suntown.suntownshop.model.Goods;
import com.suntown.suntownshop.model.ParcelableGoods;
import com.suntown.suntownshop.runnable.GetJsonRunnable;
import com.suntown.suntownshop.utils.FormatValidation;
import com.suntown.suntownshop.utils.InputStreamUtils;
import com.suntown.suntownshop.utils.IsChineseOrNot;
import com.suntown.suntownshop.utils.JsonParser;
import com.suntown.suntownshop.utils.MyMath;
import com.suntown.suntownshop.utils.XmlParser;
import com.suntown.suntownshop.widget.ConfirmDialog;
import com.suntown.suntownshop.widget.GoodsViewGroup;
import com.suntown.zxing.activity.CaptureActivity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.ImageSpan;
import android.text.style.StyleSpan;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.webkit.WebSettings.LayoutAlgorithm;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RatingBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.CompoundButton.OnCheckedChangeListener;
/**
 * ��Ʒ����
 *
 * @author Ǯ��
 * @version 2015��1��21�� ����9:40:18
 *
 */
public class GoodsDetailActivity extends Activity implements
		android.widget.RadioGroup.OnCheckedChangeListener {
	Button mBtnRecomd;
	Button mBtnGoodsInfo;
	Button mBtnApris;
	Button mBtnAddToCart;
	LinearLayout mViewRecomd;
	LinearLayout mViewGoodsInfo;
	LinearLayout mViewApris;
	String mBarCode;
	GoodsViewGroup goodsViewGroup;
	Goods goods;
	double curPrice;
	boolean isInFavorite = false;
	private View mLoadingView;
	private View mContentView;
	private WebView mWvDetail;
	private RadioGroup mRgDetail;
	private ImageView ivFavoriteAdd;
	private View mErrorView;
	private GridView gridView;
	private ArrayList<Goods> list;
	private GridGoodsListAdapter adapter;
	private GetJsonRunnable mGetDetailRunnable;
	private GetJsonRunnable mGetRecomdRunnable;
	private String URL = Constants.DOMAIN_NAME
			+ "axis2/services/sunteslwebservice/Getgoods_info?Barcode=";
	private String URL_RECOMD = Constants.DOMAIN_NAME
			+ "axis2/services/sunteslwebservice/Getalikegoods_info?type=0&kid=0&startIndex=1&length=6&gname=";
	private final static String URL_EVALUATE = Constants.DOMAIN_NAME
			+ "axis2/services/sunteslwebservice/getEvabyBarcode?Barcode=";
	private final static int MSG_GETGOODDETAIL_COMPLETE = 1;
	private final static int MSG_GETRECOMD_COMPLETE = 2;
	private final static int MSG_GETRECOMD_IMGPATH_COMPLETE = 3;
	private final static int MSG_GET_LOCATION_GOODS = 4;
	private final static int MSG_ERR_NETWORKERR = -1;
	private final static int MSG_GETEVA_COMPLETE = 5;
	private String userId;
	private String mLoginToken;
	private PopupWindow pw;
	private ImageView ivRoute;
	private HashMap<String, String> detailPathMap;
	private String mDetailPath;
	private View viewEva;
	private TextView tvEvaText;
	private ImageView ivAvatar;
	private RatingBar rbRate;
	private TextView tvEvaDate;
	private TextView tvNickname;
	private TextView tvEvaTitle;
	/**
	 * imageloader���
	 */
	DisplayImageOptions options;
	protected ImageLoader imageLoader = ImageLoader.getInstance();

	private void initOptions() {
		options = new DisplayImageOptions.Builder()
				.showImageOnLoading(R.drawable.picture_loading_200x200)
				.showImageForEmptyUri(R.drawable.picture_noimg_200x200)
				.showImageOnFail(R.drawable.picture_holder_200x200)
				.cacheInMemory(true).cacheOnDisk(true)
				// .considerExifParams(true)
				// .displayer(new SimpleBitmapDisplayer())
				.build();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		initOptions();
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_goods_detail);
		detailPathMap = new HashMap<String, String>();
		detailPathMap.put("2148270400002",
				"file:///android_asset/detail/devondale.html");
		detailPathMap.put("6901424334228",
				"file:///android_asset/detail/wanglaojiliulianbao.html");
		detailPathMap.put("6909931502116",
				"file:///android_asset/detail/duoliyumiyou5l.html");
		detailPathMap.put("6946985000243",
				"file:///android_asset/detail/zhimayou.html");
		detailPathMap.put("6920177944034",
				"file:///android_asset/detail/haiermian.html");
		detailPathMap.put("6956056600913",
				"file:///android_asset/detail/xiangmi.html");
		detailPathMap.put("6951019600610",
				"file:///android_asset/detail/yayao.html");
		detailPathMap.put("6935357302474",
				"file:///android_asset/detail/shupian.html");
		detailPathMap.put("2329789000000",
				"file:///android_asset/detail/zhurou.html");
		detailPathMap.put("6938522000441",
				"file:///android_asset/detail/naifen.html");
		detailPathMap.put("2113898850010",
				"file:///android_asset/detail/xueyu.html");
		SharedPreferences mSharedPreferences = getSharedPreferences(
				"suntownshop", 0);
		userId = mSharedPreferences.getString("userId", "");
		mLoginToken = mSharedPreferences.getString("m_voucher", "");
		mLoadingView = findViewById(R.id.loading);
		mErrorView = findViewById(R.id.error);
		mContentView = findViewById(R.id.content);
		mErrorView.setVisibility(View.GONE);
		mContentView.setVisibility(View.GONE);
		mLoadingView.setVisibility(View.VISIBLE);
		gridView = (GridView) findViewById(R.id.gv_goodslist);
		ivRoute = (ImageView) findViewById(R.id.iv_route);
		mBtnAddToCart = (Button) findViewById(R.id.btn_goods_detail_addtocart);
		Button btnAddToPrepare = (Button) findViewById(R.id.btn_goods_detail_addtoprepare);
		btnAddToPrepare.setOnClickListener(mBtnAddToPrepareClick);
		ivFavoriteAdd = (ImageView) findViewById(R.id.iv_goods_detail_storeup);
		mViewRecomd = (LinearLayout) findViewById(R.id.view_goods_detail_recomd);
		mViewGoodsInfo = (LinearLayout) findViewById(R.id.view_goods_detail_info);
		mViewApris = (LinearLayout) findViewById(R.id.view_goods_detail_apris);
		mWvDetail = (WebView) findViewById(R.id.wv_detail);
		mRgDetail = (RadioGroup) findViewById(R.id.rg_detail);

		viewEva = findViewById(R.id.view_evaluate);
		tvEvaText = (TextView) findViewById(R.id.tv_evatext);
		ivAvatar = (ImageView) findViewById(R.id.iv_avatar);
		rbRate = (RatingBar) findViewById(R.id.rb_evarate);
		tvEvaDate = (TextView) findViewById(R.id.tv_evadate);
		tvNickname = (TextView) findViewById(R.id.tv_nickname);
		tvEvaTitle = (TextView) findViewById(R.id.tv_evaluate_title);
		mRgDetail.setOnCheckedChangeListener(this);
		// mWvDetail.getSettings().setSupportZoom(true);
		// mWvDetail.getSettings().setBuiltInZoomControls(true);
		mWvDetail.getSettings().setUseWideViewPort(true);
		mWvDetail.getSettings().setLayoutAlgorithm(
				LayoutAlgorithm.SINGLE_COLUMN);
		mWvDetail.getSettings().setLoadWithOverviewMode(true);
		mWvDetail.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
		mBtnAddToCart.setOnClickListener(mBtnAddToCartClick);

		ivFavoriteAdd.setOnClickListener(mFavoriteAddinClick);

		Button btnAdd = (Button) findViewById(R.id.btn_goods_detail_add);
		btnAdd.setOnClickListener(mBtnAddClick);
		Button btnSub = (Button) findViewById(R.id.btn_goods_detail_sub);
		btnSub.setOnClickListener(mBtnSubClick);

		Intent intent = getIntent();
		if (intent.hasExtra("barCode")) {
			Bundle b = intent.getExtras();
			mBarCode = b.getString("barCode");
			mGetDetailRunnable = new GetJsonRunnable(URL + mBarCode,
					MSG_GETGOODDETAIL_COMPLETE, handler);
			new Thread(mGetDetailRunnable).start();

			GetJsonRunnable getJsonRunnable = new GetJsonRunnable(URL_EVALUATE
					+ mBarCode + "&startIndex=1&length=1", MSG_GETEVA_COMPLETE,
					handler);
			new Thread(getJsonRunnable).start();

		} else {
			Toast.makeText(this, "�������!", Toast.LENGTH_SHORT).show();
			finish();
		}

	}

	public void goCart(View v) {
		Intent intent = new Intent(GoodsDetailActivity.this,
				MainTabActivity.class);
		Bundle b = new Bundle();
		b.putInt("gototab", 2);
		intent.putExtras(b);
		startActivity(intent);
		finish();
	}

	/*
	 * private class GetJsonRunnable implements Runnable { String url; int
	 * msg_complete; int pos;
	 * 
	 * public GetJsonRunnable(String url, int msg) { // TODO Auto-generated
	 * constructor stub this.url = url; this.msg_complete = msg; }
	 * 
	 * public GetJsonRunnable(String url, int msg, int pos) { this.url = url;
	 * this.msg_complete = msg; this.pos = pos; }
	 * 
	 * @Override public void run() { // TODO Auto-generated method stub try {
	 * InputStream is = InputStreamUtils.getInputStream(url); String result =
	 * XmlParser.parse(is, "UTF-8", "return"); Message msg = new Message();
	 * Bundle bundle = new Bundle(); bundle.putString("MSG_JSON", result);
	 * bundle.putInt("MSG_POS", pos); msg.what = msg_complete;
	 * msg.setData(bundle); handler.sendMessage(msg); } catch (Exception e) { //
	 * TODO Auto-generated catch block Message msg = new Message(); Bundle
	 * bundle = new Bundle(); bundle.putString("MSG_ERR", e.getMessage());
	 * msg.what = MSG_ERR_NETWORKERR; msg.setData(bundle);
	 * handler.sendMessage(msg); e.printStackTrace(); } }
	 * 
	 * }
	 */

	private Handler handler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			// TODO Auto-generated method stub
			Bundle bundle;
			String strMsg;
			showProgress(false);
			switch (msg.what) {
			case MSG_GETEVA_COMPLETE:
				bundle = msg.getData();
				strMsg = bundle.getString("MSG_JSON");
				try {
					JSONObject jsonObj = new JSONObject(strMsg);
					int sendState = jsonObj.getInt("RESULT");
					if (sendState == 0) {
						JSONArray jsonArray = jsonObj.getJSONArray("RECORD");
						ArrayList<Evaluate> listOnce = JsonParser
								.evaluateParse(jsonArray);
						if (listOnce.size() > 0) {
							Evaluate evaluate = listOnce.get(0);
							viewEva.setVisibility(View.VISIBLE);
							String avatar = evaluate.getAvatar();
							if (avatar != null && !"".equals(avatar)) {
								if (avatar.indexOf("http://") < 0) {
									avatar += "http://";
								}
								imageLoader.displayImage(avatar, ivAvatar);
							}
							String nickname = evaluate.getNickname();
							String evaText = evaluate.getEvaText();

							if (nickname != null && !"".equals(nickname)) {
								tvNickname.setText(nickname);

								// SpannableString spanText = new
								// SpannableString(nickname);
								// spanText.setSpan(new
								// StyleSpan(Typeface.BOLD), 0,
								// spanText.length(),
								// Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
								// spanText.setSpan(new RelativeSizeSpan(1.5f),
								// 0, spanText.length(),
								// Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
								// spanText.setSpan(new
								// ForegroundColorSpan(Color.BLACK), 0,
								// spanText.length(),
								// Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
								// holder.tvEvaText.append(spanText);
							} else {
								tvNickname.setText("�ǳ�");
							}
							if (evaText == null || "".equals(evaText)) {
								evaText = "���û�������ʲô��û������";
							}
							SimpleDateFormat formatDate = new SimpleDateFormat(
									"yyyy-MM-dd HH:mm:ss");
							String date;

							date = formatDate.format(formatDate.parse(evaluate
									.getEvaDate()));
							tvEvaDate.setText(date);
							tvEvaText.setText(evaText);
							rbRate.setRating(evaluate.getRate());
							tvEvaTitle.setText("��Ʒ����(25��)");
							tvEvaTitle.setTextColor(GoodsDetailActivity.this
									.getResources().getColor(R.color.red));
						} else {
							tvEvaTitle.setText("��Ʒ����(0��)");
							tvEvaTitle.setTextColor(GoodsDetailActivity.this
									.getResources().getColor(R.color.greyfont));
							viewEva.setVisibility(View.GONE);
						}
					} else {
						tvEvaTitle.setText("��Ʒ����(0��)");
						tvEvaTitle.setTextColor(GoodsDetailActivity.this
								.getResources().getColor(R.color.greyfont));
						viewEva.setVisibility(View.GONE);
					}
				} catch (Exception e) {
					// TODO Auto-generated catch block
					Toast.makeText(getApplicationContext(),
							"ERROR:" + e.getMessage(), Toast.LENGTH_SHORT)
							.show();
					e.printStackTrace();
				}
				break;
			case MSG_GET_LOCATION_GOODS:
				bundle = msg.getData();
				strMsg = bundle.getString("MSG_JSON");
				try {
					JSONObject jsonObj = new JSONObject(strMsg);
					JSONArray jsonArray = jsonObj.getJSONArray("INFO");
					if (jsonArray.length() > 0) {
						jsonObj = (JSONObject) jsonArray.opt(0);
						String barCode = jsonObj.getString("BARCODE");
						String gName = jsonObj.getString("GNAME");
						String shelfId = jsonObj.getString("SFID");
						String floorName = jsonObj.getString("FLOORNAME");
						ConfirmDialog dialog = new ConfirmDialog(
								GoodsDetailActivity.this, "ȷ��Ҫ��" + gName
										+ "������ʼ������?",
								getString(R.string.tips_text),
								getString(R.string.confirm_text),
								getString(R.string.cancel_text));
						if (dialog.ShowDialog()) {
							navigate(gName, shelfId, floorName);
						}

					} else {
						Toast.makeText(getApplicationContext(),
								getString(R.string.qrcode_cantfind),
								Toast.LENGTH_SHORT).show();
					}
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					Toast.makeText(getApplicationContext(),
							"ERROR:" + e.getMessage(), Toast.LENGTH_SHORT)
							.show();
					e.printStackTrace();
				}
				break;
			case MSG_GETGOODDETAIL_COMPLETE:
				bundle = msg.getData();
				strMsg = bundle.getString("MSG_JSON");
				try {
					JSONObject jsonObj = new JSONObject(strMsg);
					JSONArray jsonArray = jsonObj.getJSONArray("INFO");
					System.out.println("jsonArray------>" + jsonArray.length());
					if (jsonArray.length() <= 0) {
						showError(getString(R.string.qrcode_cantfind));
						break;
					}
					jsonObj = (JSONObject) jsonArray.opt(0);

					String barCode = jsonObj.getString("BARCODE");
					String gCode = jsonObj.getString("GCODE");
					String gName = jsonObj.getString("GNAME");
					String gKind = jsonObj.getString("KIND");
					String gUnit = jsonObj.getString("UNIT");
					String gOriPrice = jsonObj.getString("ORIPRICE");
					String gMemPrice = jsonObj.getString("MEMPRICE");
					String gUptPrice = jsonObj.getString("UPTPRICE");
					String gSpec = jsonObj.getString("SPEC");
					String gClass = jsonObj.getString("GCLASS");
					String gProvider = jsonObj.getString("PROVIDER");
					String gBrand = jsonObj.getString("BRAND");
					String gOrigin = jsonObj.getString("ORIGIN");
					String gImgPath = jsonObj.getString("IMGPATH");
					int priceType = jsonObj.getInt("PRICETYPE");
					double evaluate = jsonObj.getDouble("AVERAGE");
					int deliverType = jsonObj.getInt("DELIVERYMODE");
					String shelfId = jsonObj.getString("SFID");
					String floorNo = jsonObj.getString("FLOORNO");
					String floorName = jsonObj.getString("FLOORNAME");
					goods = new Goods(barCode, gCode, gName, gKind, gUnit,
							gOriPrice, gMemPrice, gUptPrice, gSpec, gClass,
							gProvider, gBrand, gOrigin, gImgPath, priceType,
							deliverType, evaluate, shelfId, floorNo, floorName);
					if (shelfId == null || floorNo == null || floorName == null
							|| "".equals(floorName) || "".equals(floorNo)
							|| "".equals(shelfId)) {
						ivRoute.setVisibility(View.GONE);
					}
					// ��ѯ�ؼ�����ʱ�� Ʒ����
					mGetRecomdRunnable = new GetJsonRunnable(URL_RECOMD
							+ URLEncoder.encode(gBrand, "UTF-8"),
							MSG_GETRECOMD_COMPLETE, handler);
					new Thread(mGetRecomdRunnable).start();
					initGoods(goods);
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					Toast.makeText(getApplicationContext(), "ERROR:��Ʒ��������",
							Toast.LENGTH_SHORT).show();
					e.printStackTrace();
				} catch (UnsupportedEncodingException e) {
					Toast.makeText(getApplicationContext(), "ERROR:��Ʒ��������",
							Toast.LENGTH_SHORT).show();
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (Exception e) {
					Toast.makeText(getApplicationContext(), "ERROR:��Ʒ��������",
							Toast.LENGTH_SHORT).show();
					e.printStackTrace();
				}
				mContentView.setVisibility(View.VISIBLE);
				mLoadingView.setVisibility(View.GONE);
				// Toast.makeText(getApplicationContext(), strMsg,
				// Toast.LENGTH_SHORT).show();
				break;
			case MSG_GETRECOMD_COMPLETE:
				bundle = msg.getData();
				strMsg = bundle.getString("MSG_JSON");
				try {
					JSONObject jsonObj = new JSONObject(strMsg);
					JSONArray jsonArray = jsonObj.getJSONArray("RECORD");
					int rows = jsonArray.length();
					rows = rows > 6 ? 6 : rows;
					if (rows > 0) {
						list = new ArrayList<Goods>();

						for (int i = 0; i < rows; i++) {
							jsonObj = (JSONObject) jsonArray.opt(i);
							String barCode = jsonObj.getString("BARCODE");
							String gCode = jsonObj.getString("GCODE");
							String gName = jsonObj.getString("GNAME");
							String gKind = jsonObj.getString("KIND");
							String gUnit = jsonObj.getString("UNIT");
							String gOriPrice = jsonObj.getString("ORIPRICE");
							String gMemPrice = jsonObj.getString("MEMPRICE");
							String gUptPrice = jsonObj.getString("UPTPRICE");
							String gSpec = jsonObj.getString("SPEC");
							String gClass = jsonObj.getString("GCLASS");
							String gProvider = jsonObj.getString("PROVIDER");
							String gBrand = jsonObj.getString("BRAND");
							String gOrigin = jsonObj.getString("ORIGIN");
							String gImgPath = jsonObj.getString("IMGPATH");
							int priceType = jsonObj.getInt("PRICETYPE");
							int deliverType = jsonObj.getInt("DELIVERYMODE");
							Goods goods = new Goods(barCode, gCode, gName,
									gKind, gUnit, gOriPrice, gMemPrice,
									gUptPrice, gSpec, gClass, gProvider,
									gBrand, gOrigin, gImgPath, priceType,
									deliverType);
							list.add(goods);
						}
						adapter = new GridGoodsListAdapter(
								GoodsDetailActivity.this, list);
						gridView.setAdapter(adapter);
						int totalHeight = 0;
						int div = 0;
						View listItem = adapter.getView(0, null, gridView);
						listItem.measure(0, 0);
						if (rows > 3) {
							totalHeight = listItem.getMeasuredHeight()
									* 2
									+ MyMath.dip2px(GoodsDetailActivity.this, 2);

						} else {
							totalHeight = listItem.getMeasuredHeight();
						}

						ViewGroup.LayoutParams params = gridView
								.getLayoutParams();
						params.height = totalHeight;

						gridView.setLayoutParams(params);
					}

				} catch (JSONException e) {
					// TODO Auto-generated catch block
					Toast.makeText(getApplicationContext(),
							"ERROR:�Ƽ���Ʒ��������:" + e.getMessage(),
							Toast.LENGTH_SHORT).show();
					e.printStackTrace();
				}
				// Toast.makeText(getApplicationContext(), strMsg,
				// Toast.LENGTH_SHORT).show();
				break;
			case MSG_ERR_NETWORKERR:
				bundle = msg.getData();
				strMsg = bundle.getString("MSG_ERR");
				Toast.makeText(getApplicationContext(), "���ӳ�ʱ�����Ժ�����...",
						Toast.LENGTH_LONG).show();
				mContentView.setVisibility(View.GONE);
				mLoadingView.setVisibility(View.GONE);
				mErrorView.setVisibility(View.VISIBLE);
				break;
			case MSG_GETRECOMD_IMGPATH_COMPLETE:
				bundle = msg.getData();
				strMsg = bundle.getString("MSG_JSON");
				int pos = bundle.getInt("MSG_POS");
				JSONObject jsonObj;
				try {
					jsonObj = new JSONObject(strMsg);
					JSONArray jsonArray = jsonObj.getJSONArray("INFO");
					jsonObj = (JSONObject) jsonArray.opt(0);
					String imgPath = jsonObj.getString("IMGPATH");
					View view = goodsViewGroup.getChildAt(pos);
					ImageView imgView = (ImageView) view
							.findViewById(R.id.iv_recomd_item);
					if (imgPath != null && imgPath.length() > 0) {
						imageLoader.displayImage("http://" + imgPath, imgView,
								options);
					} else {
						imgView.setImageResource(R.drawable.picture_noimg_200x200);
					}
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					Toast.makeText(getApplicationContext(),
							"ERROR:�Ƽ���ƷͼƬ��������:" + e.getMessage(),
							Toast.LENGTH_SHORT).show();
					e.printStackTrace();
				}

				break;
			}
			super.handleMessage(msg);
		}

	};

	private void showError(String msg) {
		mContentView.setVisibility(View.GONE);
		mLoadingView.setVisibility(View.GONE);
		mErrorView.setVisibility(View.VISIBLE);
		TextView tvMsg = (TextView) findViewById(R.id.errormessage);
		tvMsg.setText(msg);
	}

	/**
	 * ������������ת����Ʒ����ҳ
	 * 
	 * @param barCode
	 */
	private void showGoodsDetail(String barCode) {
		Intent intent = new Intent(GoodsDetailActivity.this,
				GoodsDetailActivity.class);
		Bundle b = new Bundle();
		b.putString("barCode", barCode);
		intent.putExtras(b);
		startActivity(intent);
		finish();
	}

	private void initGoods(Goods goods) {
		TextView tv_goods_name = (TextView) findViewById(R.id.tv_goods_name);
		TextView tv_goods_spec = (TextView) findViewById(R.id.tv_goods_spec);
		TextView tv_goods_curprice_ms = (TextView) findViewById(R.id.tv_goods_curprice_ms);
		TextView tv_goods_curprice = (TextView) findViewById(R.id.tv_goods_curprice);
		TextView tv_goods_oriprice_ms = (TextView) findViewById(R.id.tv_goods_oriprice_ms);
		TextView tv_goods_oriprice = (TextView) findViewById(R.id.tv_goods_oriprice);
		TextView tv_goods_info_unit = (TextView) findViewById(R.id.tv_goods_info_unit);
		TextView tv_goods_info_brand = (TextView) findViewById(R.id.tv_goods_info_brand);
		TextView tv_goods_info_origin = (TextView) findViewById(R.id.tv_goods_info_origin);
		TextView tv_noeva = (TextView) findViewById(R.id.tv_noeva);
		TextView tv_discount = (TextView) findViewById(R.id.tv_discount);
		RatingBar rb_evarate = (RatingBar) findViewById(R.id.rb_evarate);
		ImageView ivMain = (ImageView) findViewById(R.id.iv_goods_detail_main);
		TextView tvCartNum = (TextView) findViewById(R.id.tv_cartnum);

		SharedPreferences sharedPreferences = getSharedPreferences(
				"suntownshop", 0);
		ShopCartDb db = new ShopCartDb(this, userId);
		int count = db.getCount();
		db.Close();
		if (count > 0) {
			tvCartNum.setVisibility(View.VISIBLE);
			tvCartNum.setText(count + "");
		} else {
			tvCartNum.setVisibility(View.GONE);
		}

		SharedPreferences mSharedPreferences = getSharedPreferences(
				"suntownshop", 0);
		boolean isVip = mSharedPreferences.getBoolean("isvip", false);
		double oriPrice = goods.getOriPriceInNumc();
		curPrice = goods.getCurPrice(isVip);
		if (goods.getPriceType() != 0 && goods.getOriPriceInNumc() > 0
				&& curPrice < oriPrice) {

			tv_goods_oriprice.setText(String.format("%.2f", oriPrice));
			tv_goods_oriprice_ms.getPaint().setFlags(
					Paint.STRIKE_THRU_TEXT_FLAG);
			tv_goods_oriprice.getPaint().setFlags(Paint.STRIKE_THRU_TEXT_FLAG);
			tv_goods_oriprice.setTextColor(getResources().getColor(
					R.color.greyfont));
			// holder.ivPriceType.setVisibility(View.VISIBLE);
			// tv_goods_curprice.setVisibility(View.VISIBLE);
			tv_goods_oriprice_ms.setVisibility(View.VISIBLE);
			tv_goods_oriprice.setVisibility(View.VISIBLE);

			tv_goods_curprice.setText(String.format("%.2f", curPrice));
			double discount = curPrice * 10 / oriPrice;
			if (discount < 10) {
				tv_discount.setVisibility(View.VISIBLE);
				tv_discount.setText(String.format("%.1f", discount) + "��");
				tv_goods_name.setText("\u3000\u3000" + goods.getName());
			} else {
				tv_discount.setVisibility(View.GONE);
				tv_goods_name.setText(goods.getName());
			}
			/*
			 * if(goods.getPriceType()==2){
			 * holder.ivPriceType.setImageResource(R.drawable.mini_channel_vip);
			 * }else{
			 * holder.ivPriceType.setImageResource(R.drawable.mini_channel_hui);
			 * }
			 */
		} else {
			tv_goods_curprice.setText(String.format("%.2f", curPrice));
			tv_goods_name.setText(goods.getName());
			tv_goods_oriprice.getPaint().setFlags(0);
			tv_goods_oriprice.setTextColor(getResources().getColor(
					R.color.btn_color));
			tv_discount.setVisibility(View.GONE);
			tv_goods_oriprice_ms.setVisibility(View.GONE);
			tv_goods_oriprice.setVisibility(View.GONE);
		}

		tv_goods_spec.setText(goods.getSpec());

		double evaluate = goods.getEvaluate();
		if (evaluate > 0) {
			rb_evarate.setVisibility(View.VISIBLE);
			tv_noeva.setVisibility(View.GONE);
			rb_evarate.setRating((float) evaluate);
		} else {
			rb_evarate.setVisibility(View.GONE);
			tv_noeva.setVisibility(View.VISIBLE);
		}
		// tv_goods_curprice.setText(String.format("%.2f", curPrice));
		tv_goods_info_unit.setText(goods.getUnit());
		tv_goods_info_brand.setText(goods.getBrand());
		tv_goods_info_origin.setText(goods.getOrigin());
		FavoriteDb fdb = new FavoriteDb(this, userId);
		isInFavorite = fdb.isInFavorite(goods.getBarCode());
		fdb.Close();
		if (isInFavorite) {
			ivFavoriteAdd.setImageResource(R.drawable.collect_positive);
		} else {
			ivFavoriteAdd.setImageResource(R.drawable.collect_negative);
		}
		String imgPath = goods.getImgPath();
		System.out.println("ImgPath------->" + imgPath);
		if (imgPath != null && imgPath.length() > 0) {
			imageLoader.displayImage("http://" + imgPath, ivMain, options);
		} else {
			ivMain.setImageResource(R.drawable.picture_noimg_200x200);
		}
		mDetailPath = detailPathMap.get(goods.getBarCode());
		if (mDetailPath == null || "".equals(mDetailPath)) {
			RadioButton rbDetail = (RadioButton) findViewById(R.id.rb_detail);
			rbDetail.setVisibility(View.GONE);
			mRgDetail.check(R.id.rb_param);
		} else {
			mWvDetail.loadUrl(mDetailPath);
		}
	}

	private void ChangeQuantity(int num) {
		EditText et = (EditText) findViewById(R.id.et_goods_detail_quantity);
		String text = et.getText().toString();
		int curNum = 1;
		if (FormatValidation.isNumeric(text)) {
			curNum = (text == null || text.length() <= 0) ? 1 : Integer
					.parseInt(text);
		}
		curNum = curNum + num;
		if (curNum > 99) {
			curNum = 99;
		} else if (curNum < 1) {
			curNum = 1;
		}
		et.setText(curNum + "");
	}

	public void close(View v) {
		finish();
	}

	private OnClickListener mBtnBack = new OnClickListener() {

		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			finish();
		}
	};

	/**
	 * ����������ť
	 */
	private OnClickListener mBtnSubClick = new OnClickListener() {

		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			ChangeQuantity(-1);
		}
	};

	private OnClickListener mBtnAddClick = new OnClickListener() {

		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			ChangeQuantity(1);
		}
	};
	/**
	 * ���չ������£�Ƽ���Ʒ
	 */
	private OnClickListener mTvRecomdClick = new OnClickListener() {

		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			if (mViewRecomd != null) {
				// Toast.makeText(getApplicationContext(), "click",
				// Toast.LENGTH_SHORT).show();
				int visibility = mViewRecomd.getVisibility();
				if (visibility == View.VISIBLE) {
					mViewRecomd.setVisibility(View.GONE);
					// mViewRecomd.invalidate();
				} else {
					mViewRecomd.setVisibility(View.VISIBLE);
					// mViewRecomd.invalidate();
				}
			}
		}
	};

	private OnClickListener mTvGoodsAprisClick = new OnClickListener() {

		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			if (mViewApris != null) {
				// Toast.makeText(getApplicationContext(), "click",
				// Toast.LENGTH_SHORT).show();
				int visibility = mViewApris.getVisibility();
				if (visibility == View.VISIBLE) {
					mViewApris.setVisibility(View.GONE);
					// mViewGoodsInfo.invalidate();
				} else {
					mViewApris.setVisibility(View.VISIBLE);
					// mViewGoodsInfo.invalidate();
				}
			}
		}
	};

	private OnClickListener mTvGoodsInfoClick = new OnClickListener() {

		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			if (mViewGoodsInfo != null) {
				// Toast.makeText(getApplicationContext(), "click",
				// Toast.LENGTH_SHORT).show();
				int visibility = mViewGoodsInfo.getVisibility();
				if (visibility == View.VISIBLE) {
					mViewGoodsInfo.setVisibility(View.GONE);
					// mViewGoodsInfo.invalidate();
				} else {
					mViewGoodsInfo.setVisibility(View.VISIBLE);
					// mViewGoodsInfo.invalidate();
				}
			}
		}
	};

	private OnClickListener mFavoriteAddinClick = new OnClickListener() {

		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			if (goods != null) {
				FavoriteDb fdb = new FavoriteDb(GoodsDetailActivity.this,
						userId);
				if (isInFavorite) {
					fdb.delete(goods.getBarCode());
					Toast.makeText(getApplicationContext(), "���Ƴ��ղ�",
							Toast.LENGTH_SHORT).show();
					((ImageView) v)
							.setImageResource(R.drawable.collect_negative);
					isInFavorite = false;
				} else {

					fdb.insert(goods.getBarCode(), goods.getName(),
							goods.getImgPath(), goods.getSpec(), curPrice,goods.getShelfId(),goods.getFloorName());

					Toast.makeText(getApplicationContext(), "�Ѽ����ղ�",
							Toast.LENGTH_SHORT).show();
					((ImageView) v)
							.setImageResource(R.drawable.collect_positive);
					isInFavorite = true;
				}
				fdb.Close();

			}

		}
	};

	private ProgressDialog mPDialog;

	public void showProgress(final boolean show) {
		if (show) {
			mPDialog = new ProgressDialog(this);
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

	private final static String URL_ADD_TO_PREPARE = Constants.DOMAIN_NAME
			+ "axis2/services/sunteslwebservice/addModelFormno";

	private OnCompleteCallback callback = new OnCompleteCallback() {

		@Override
		public void onComplete(boolean isOk, String msg) {
			// TODO Auto-generated method stub
			showProgress(false);
			if (isOk) {
				JSONObject jsonObj;
				try {
					msg = XmlParser.parse(msg, "UTF-8", "return");
					jsonObj = new JSONObject(msg);
					int sendState = jsonObj.getInt("RESULT");
					// ArrayList<Order> list;
					if (sendState == 0) {
						// ȡ�ö������ݣ���ʼ����
						Toast.makeText(getApplicationContext(), "����Ԥ���嵥�ɹ�",
								Toast.LENGTH_SHORT).show();
					} else if (sendState == 1) {
						Toast.makeText(getApplicationContext(),
								"��¼״̬���ڣ������µ�¼!", Toast.LENGTH_SHORT).show();
					} else {
						Toast.makeText(getApplicationContext(), "����Ԥ���嵥ʧ��",
								Toast.LENGTH_SHORT).show();
					}

				} catch (Exception e) {
					Toast.makeText(getApplicationContext(), "���������ش������Ժ�����...",
							Toast.LENGTH_SHORT).show();

					e.printStackTrace();
				}
			} else {
				Toast.makeText(getApplicationContext(), "���ӳ�ʱ�����Ժ�����...",
						Toast.LENGTH_SHORT).show();
			}

		}
	};

	private OnClickListener mBtnAddToPrepareClick = new OnClickListener() {

		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			if (goods != null) {
				showProgress(true);
				HashMap<String, String> params = new HashMap<String, String>();
				params.put("memid", userId);
				params.put("logintoken", mLoginToken);
				params.put("barcode", goods.getBarCode());
				PostAsyncTask postAsyncTask = new PostAsyncTask(
						URL_ADD_TO_PREPARE, callback);
				postAsyncTask.execute(params);
			}
		}
	};

	private OnClickListener mBtnAddToCartClick = new OnClickListener() {

		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			if (goods != null) {
				EditText et = (EditText) findViewById(R.id.et_goods_detail_quantity);
				String text = et.getText().toString();
				int curNum;// = (text == null || text.length() <= 0) ? 1 :
							// Integer.parseInt(text);
				if (text == null || "".endsWith(text)) {
					et.setText("1");
					curNum = 1;
				} else {
					curNum = Integer.parseInt(text);
				}
				try {

					ShopCartDb scdb = new ShopCartDb(GoodsDetailActivity.this,
							userId);
					text = goods.getOriPrice();
					double price = (text == null || text.length() <= 0) ? 0.0
							: Double.parseDouble(text);

					if (scdb.insertGoods(goods.getBarCode(), goods.getName(),
							goods.getImgPath(), goods.getSpec(), curPrice,
							curNum, goods.getDeliverType())) {
						Toast.makeText(getApplicationContext(), "���빺�ﳵ�ɹ�",
								Toast.LENGTH_SHORT).show();
					} else {
						Toast.makeText(getApplicationContext(), "���빺�ﳵʧ��",
								Toast.LENGTH_SHORT).show();
					}
					scdb.Close();
				} catch (Exception e) {
					// TODO: handle exception
					Toast.makeText(getApplicationContext(),
							"ERROR:���빺�ﳵ����" + e.getMessage(), Toast.LENGTH_LONG)
							.show();
				}

			}
		}
	};

	public void showEva(View v) {
		if (goods.getEvaluate() > 0) {
			Intent intent = new Intent(GoodsDetailActivity.this,
					GoodsEvaluateActivity.class);
			intent.putExtra("barcode", goods.getBarCode());
			startActivity(intent);
		}
	}

	public void navigate(View v) {
		LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View mView = inflater.inflate(R.layout.select_location_popup, null);

		if (pw == null) {
			// ����PopupWindow����
			pw = new PopupWindow(mView, LayoutParams.FILL_PARENT,
					LayoutParams.FILL_PARENT);
			pw.setOutsideTouchable(true);
		} else {
			pw.setContentView(mView);
		}
		View locationGate = mView.findViewById(R.id.location_gate);
		View locationScan = mView.findViewById(R.id.location_scan);
		View viewOk = mView.findViewById(R.id.view_ok);
		View viewCancel = mView.findViewById(R.id.view_cancel);
		final RadioButton rbGate = (RadioButton) mView
				.findViewById(R.id.rb_gate);
		final RadioButton rbScan = (RadioButton) mView
				.findViewById(R.id.rb_scan);
		rbGate.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				// TODO Auto-generated method stub
				if (isChecked) {
					rbScan.setChecked(false);
				}
			}
		});
		rbScan.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				// TODO Auto-generated method stub
				if (isChecked) {
					rbGate.setChecked(false);
				}
			}
		});
		viewOk.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				if (pw != null && pw.isShowing()) {
					pw.dismiss();
				}
				if (rbGate.isChecked()) {
					navigate("1�����", "1�����", "1F");
				} else {

					Intent openCameraIntent = new Intent(
							GoodsDetailActivity.this, CaptureActivity.class);
					startActivityForResult(openCameraIntent, 0);
				}
			}
		});
		viewCancel.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				if (pw != null && pw.isShowing()) {
					pw.dismiss();
				}
			}
		});
		pw.setFocusable(true);

		pw.setBackgroundDrawable(new ColorDrawable(0x7f000000));

		pw.showAtLocation(mContentView, Gravity.CENTER, 0, 0);

	}

	private void navigate(String title, String location, String floorName) {
		if (title == null || location == null || floorName == null
				|| "".equals(title) || "".equals(location)
				|| "".equals(floorName)) {
			Toast.makeText(this, "�Ҳ�������Ʒ��λ�ã��������ʼ������...", Toast.LENGTH_SHORT)
					.show();
			return;
		}
		Intent i = new Intent(this, MapActivity.class);
		ParcelableGoods pGoods = new ParcelableGoods(goods);
		ArrayList<ParcelableGoods> goodsList = new ArrayList<ParcelableGoods>();
		goodsList.add(pGoods);
		i.putParcelableArrayListExtra("goodslist", goodsList);
		i.putExtra("location", location);
		i.putExtra("title", title);
		i.putExtra("floor", floorName);
		startActivity(i);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		// TODO Auto-generated method stub
		super.onActivityResult(requestCode, resultCode, data);
		System.out.println("requestCode:" + requestCode + " resultCode:"
				+ resultCode);
		if (resultCode == Activity.RESULT_OK) {

			Bundle bundle = data.getExtras();
			String scanResult = bundle.getString("result");
			String format = bundle.getString("format");
			System.out.println("Scan Result---------->" + scanResult);
			if (format.equalsIgnoreCase("QR_CODE")) { // ��ά��
				String UTF_Str = "";
				String GB_Str = "";
				boolean is_cN = false;
				try {
					UTF_Str = new String(scanResult.getBytes("ISO-8859-1"),
							"UTF-8");
					System.out.println("����ת��UTF-8��" + UTF_Str);
					is_cN = IsChineseOrNot.isChineseCharacter(UTF_Str);
					// ��ֹ��������ʹ�����������ɶ�ά�����жϵ����
					boolean b = IsChineseOrNot.isSpecialCharacter(scanResult);
					if (b) {
						is_cN = true;
					}
					System.out.println("��Ϊ:" + is_cN);
					if (!is_cN) {
						GB_Str = new String(scanResult.getBytes("ISO-8859-1"),
								"GB2312");
						System.out.println("����ת��GB2312��" + GB_Str);
					}
				} catch (UnsupportedEncodingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				if (is_cN) {
					scanResult = UTF_Str;
				} else {
					scanResult = GB_Str;
				}

				int l = scanResult.indexOf("BC:");
				// int len = scanResult.length();
				int r = scanResult.indexOf(";"); //
				String barCode = "";
				if (l >= 0 && r > l) {
					barCode = scanResult.substring(l + 3, r);
					findLocation(barCode);
				} else {
					Toast.makeText(this, getString(R.string.qrcode_cantfind),
							Toast.LENGTH_SHORT).show();
				}
			} else { // ����
				findLocation(scanResult);
			}

		}
	}

	private void findLocation(String barcode) {
		showProgress(true);
		com.suntown.suntownshop.runnable.GetJsonRunnable getJsonRunnable = new com.suntown.suntownshop.runnable.GetJsonRunnable(
				URL + barcode, MSG_GET_LOCATION_GOODS, handler);
		new Thread(getJsonRunnable).start();
	}

	@Override
	public void onCheckedChanged(RadioGroup arg0, int checkId) {
		// TODO Auto-generated method stub

		switch (checkId) {
		case R.id.rb_detail:
			mViewRecomd.setVisibility(View.GONE);
			mViewGoodsInfo.setVisibility(View.GONE);
			mWvDetail.setVisibility(View.VISIBLE);
			break;
		case R.id.rb_param:
			mViewRecomd.setVisibility(View.GONE);
			mViewGoodsInfo.setVisibility(View.VISIBLE);
			mWvDetail.setVisibility(View.GONE);
			break;
		case R.id.rb_recommend:
			mViewRecomd.setVisibility(View.VISIBLE);
			mViewGoodsInfo.setVisibility(View.GONE);
			mWvDetail.setVisibility(View.GONE);
			break;
		}
	}
}