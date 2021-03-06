package com.suntown.suntownshop;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.handmark.pulltorefresh.library.ILoadingLayout;
import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.PullToRefreshGridView;
import com.handmark.pulltorefresh.library.PullToRefreshBase.OnRefreshListener2;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.suntown.suntownshop.adapter.GridGoodsListAdapter;
import com.suntown.suntownshop.db.RouteGoodsDb;
import com.suntown.suntownshop.listener.OnImageMoveListener;
import com.suntown.suntownshop.listener.OnMoveViewListener;
import com.suntown.suntownshop.listener.ShakeListener;
import com.suntown.suntownshop.listener.ShakeListener.OnShakeListener;
import com.suntown.suntownshop.model.Goods;
import com.suntown.suntownshop.model.ParcelableGoods;
import com.suntown.suntownshop.runnable.GetJsonRunnable;
import com.suntown.suntownshop.service.LocalService;
import com.suntown.suntownshop.service.LocalService.OnBeaconFoundListener;
import com.suntown.suntownshop.utils.ImageMoveAnimation;
import com.suntown.suntownshop.utils.IsChineseOrNot;
import com.suntown.suntownshop.utils.JsonParser;
import com.suntown.suntownshop.utils.MyMath;
import com.suntown.suntownshop.utils.Utility;
import com.suntown.suntownshop.widget.ConfirmDialog;
import com.suntown.suntownshop.widget.UnScrollGridView;
import com.suntown.zxing.activity.CaptureActivity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Paint;
import android.graphics.drawable.ColorDrawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.view.animation.TranslateAnimation;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.LinearLayout.LayoutParams;
/**
 * 热门
 *
 * @author 钱凯
 * @version 2015年7月21日 上午9:37:20
 *
 */
public class FragmentPage3 extends Fragment implements OnLongClickListener,
		OnClickListener {
	private UnScrollGridView gridView;
	private ArrayList<Goods> list;
	private GridGoodsListAdapter adapter;
	private TextView tvTitle;
	private View mLoading;
	private View mShaking;
	private View mMain;
	private final static int MSG_GETGOODSLIST_COMPLETE = 1;
	private final static int MSG_ERR_NETWORKERR = -1;
	private final static int MSG_GET_LOCATION_GOODS = 4;
	private final static int LOAD_ONCE_LEN = 30;
	private int mLoadTimes = 0;
	private final static String URL = Constants.DOMAIN_NAME
			+ "axis2/services/sunteslwebservice/getgoods_upt_day?length="
			+ LOAD_ONCE_LEN + "&startIndex=";
	private View mFragmentView = null;

	private String userId;
	private boolean mIsVip = false;
	
	private Goods moveGoods;
	private ImageMoveAnimation imageMoveAnim;
	private TextView tvRouteNum;
	private ImageView ivRoute;
	private ArrayList<ParcelableGoods> routeList;
	private PopupWindow pw;
	private final String URL_GOODSDETAIL = Constants.DOMAIN_NAME
			+ "axis2/services/sunteslwebservice/Getgoods_info?Barcode=";
	/**
	 * imageloader相关
	 */
	DisplayImageOptions options;
	protected ImageLoader imageLoader = ImageLoader.getInstance();

	private void initOptions() {
		options = new DisplayImageOptions.Builder()
				.showImageOnLoading(R.drawable.picture_loading_200x200)
				.showImageForEmptyUri(R.drawable.picture_noimg_200x200)
				.showImageOnFail(R.drawable.picture_holder_200x200)
				.cacheInMemory(true).cacheOnDisk(true).build();
	}

	private void initViews() {
		list = new ArrayList<Goods>();
		adapter = new GridGoodsListAdapter(getActivity(), list);
		adapter.setOnAddToRouterListener(listener);
		gridView.setAdapter(adapter);
		mMain.setVisibility(View.GONE);
		gridView.setVisibility(View.GONE);
		loadGoodsMore();
	}

	private void loadGoodsMore() {
		GetJsonRunnable getJsonRunnable = new GetJsonRunnable(URL
				+ (mLoadTimes * LOAD_ONCE_LEN + 1), MSG_GETGOODSLIST_COMPLETE,
				handler);
		new Thread(getJsonRunnable).start();
		mLoadTimes++;
	}

	private Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			// TODO Auto-generated method stub
			showProgress(false);
			if (getActivity() == null || getActivity().isFinishing()) {
				return;
			}
			Bundle bundle;
			String strMsg;
			JSONObject jsonObj;
			JSONArray jsonArray;
			mLoading.setVisibility(View.GONE);
			mMain.setVisibility(View.VISIBLE);
			gridView.setVisibility(View.VISIBLE);
			switch (msg.what) {
			case MSG_GET_LOCATION_GOODS:
				bundle = msg.getData();
				strMsg = bundle.getString("MSG_JSON");
				try {
					jsonObj = new JSONObject(strMsg);
					jsonArray = jsonObj.getJSONArray("INFO");
					if (jsonArray.length() > 0) {
						jsonObj = (JSONObject) jsonArray.opt(0);
						String barCode = jsonObj.getString("BARCODE");
						String gName = jsonObj.getString("GNAME");
						String shelfId = jsonObj.getString("SFID");
						String floorName = jsonObj.getString("FLOORNAME");
						ConfirmDialog dialog = new ConfirmDialog(getActivity(),
								"确定要从" + gName + "附近开始导航吗?",
								getString(R.string.tips_text),
								getString(R.string.confirm_text),
								getString(R.string.cancel_text));
						if (dialog.ShowDialog()) {
							navigate(gName, shelfId, floorName);
						}

					} else {
						Toast.makeText(getActivity(),
								getString(R.string.qrcode_cantfind),
								Toast.LENGTH_SHORT).show();
					}
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					Toast.makeText(getActivity(), "ERROR:" + e.getMessage(),
							Toast.LENGTH_SHORT).show();
					e.printStackTrace();
				}
				break;
			case MSG_GETGOODSLIST_COMPLETE:

				bundle = msg.getData();
				strMsg = bundle.getString("MSG_JSON");
				int count = 0;
				System.out.println(strMsg);
				try {
					jsonObj = new JSONObject(strMsg);
					jsonArray = jsonObj.getJSONArray("INFO");
					// count = jsonArray.length();
					ArrayList<Goods> goodsList = JsonParser
							.goodsParse(jsonArray);

					if (goodsList != null && goodsList.size() > 0) {
						count = goodsList.size();
						/*
						 * for (int i = 0; i < count; i++) { jsonObj =
						 * (JSONObject) jsonArray.opt(i); String barCode =
						 * jsonObj.getString("BARCODE"); String gCode =
						 * jsonObj.getString("GCODE"); String gName =
						 * jsonObj.getString("GNAME"); String gKind =
						 * jsonObj.getString("KIND"); String gUnit =
						 * jsonObj.getString("UNIT"); String gOriPrice =
						 * jsonObj.getString("ORIPRICE"); String gMemPrice =
						 * jsonObj.getString("MEMPRICE"); String gUptPrice =
						 * jsonObj.getString("UPTPRICE"); String gSpec =
						 * jsonObj.getString("SPEC"); String gClass =
						 * jsonObj.getString("GCLASS"); String gProvider =
						 * jsonObj.getString("PROVIDER"); String gBrand =
						 * jsonObj.getString("BRAND"); String gOrigin =
						 * jsonObj.getString("ORIGIN"); String gImgPath =
						 * jsonObj.getString("IMGPATH"); int priceType =
						 * jsonObj.getInt("PRICETYPE"); int deliverType =
						 * jsonObj.getInt("DELIVERYMODE"); Goods goods = new
						 * Goods(barCode, gCode, gName, gKind, gUnit, gOriPrice,
						 * gMemPrice, gUptPrice, gSpec, gClass, gProvider,
						 * gBrand, gOrigin, gImgPath, priceType, deliverType);
						 * adapter.goodsList.add(goods); }
						 */
						adapter.goodsList.addAll(goodsList);
						adapter.notifyDataSetChanged();

						if (mLoadTimes == 1) {
							initAdViews();
						}
					} else {
						Toast.makeText(getActivity(), "找不到商品",
								Toast.LENGTH_SHORT).show();
					}
					if (count < LOAD_ONCE_LEN) {
						// gridView.setMode(com.handmark.pulltorefresh.library.PullToRefreshBase.Mode.DISABLED);
					}
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					Toast.makeText(getActivity(),
							"ERROR:分类商品解析错误:" + e.getMessage(),
							Toast.LENGTH_SHORT).show();
					e.printStackTrace();
				}
				break;

			case MSG_ERR_NETWORKERR:
				bundle = msg.getData();
				strMsg = bundle.getString("MSG_ERR");
				Toast.makeText(getActivity(), "连接超时，请稍后重试...",
						Toast.LENGTH_SHORT).show();
				break;
			}
			super.handleMessage(msg);
		}

	};

	/**
	 * 根据条形码跳转到商品详情页
	 * 
	 * @param barCode
	 */
	private void showGoodsDetail(String barCode) {
		Intent intent = new Intent(getActivity(), GoodsDetailActivity.class);
		Bundle b = new Bundle();
		b.putString("barCode", barCode);
		intent.putExtras(b);
		getActivity().startActivity(intent);
	}

	private void fillGoodsInfo(TextView tvName, TextView tvCurPrice,
			TextView tvOriPrice, TextView tvOriSymbol, ImageView ivRoute,
			final ImageView ivMain, final Goods goods) {
		tvName.setText(goods.getName());
		if (goods.getShelfId() == null || goods.getFloorName() == null
				|| "".equals(goods.getShelfId())
				|| "".equals(goods.getFloorName())) {
			ivRoute.setVisibility(View.GONE);
		} else {
			ivRoute.setVisibility(View.VISIBLE);
			ivRoute.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					// TODO Auto-generated method stub
					moveGoods = goods;
					if (imageMoveAnim == null) {
						imageMoveAnim = new ImageMoveAnimation(getActivity());
					}
					imageMoveAnim.setAnim(ivMain, FragmentPage3.this.ivRoute, imageMoveListener);
				}
			});
		}

		if (goods.getPriceType() != 0 && goods.getOriPriceInNumc() > 0) {
			double oriPrice = goods.getOriPriceInNumc();
			double curPrice = goods.getCurPrice(mIsVip);
			tvOriPrice.setText(String.format("%.2f", oriPrice));
			tvOriSymbol.getPaint().setFlags(Paint.STRIKE_THRU_TEXT_FLAG);
			tvOriPrice.getPaint().setFlags(Paint.STRIKE_THRU_TEXT_FLAG);
			tvOriPrice.setTextColor(getActivity().getResources().getColor(
					R.color.greyfont));
			// holder.ivPriceType.setVisibility(View.VISIBLE);
			tvCurPrice.setVisibility(View.VISIBLE);
			tvOriSymbol.setVisibility(View.VISIBLE);
			tvOriPrice.setVisibility(View.VISIBLE);

			tvCurPrice.setText(String.format("%.2f", curPrice));
			double discount = curPrice * 10 / oriPrice;

		} else {
			tvCurPrice
					.setText(String.format("%.2f", goods.getCurPrice(mIsVip)));
			tvOriPrice.getPaint().setFlags(0);
			tvOriPrice.setTextColor(getActivity().getResources().getColor(
					R.color.btn_color));
			tvOriSymbol.setVisibility(View.GONE);
			tvOriPrice.setVisibility(View.GONE);
		}
		String imgPath = goods.getImgPath();
		if (imgPath != null && imgPath.length() > 0) {
			imageLoader.displayImage("http://" + imgPath, ivMain, options);
		} else {
			ivMain.setImageResource(R.drawable.picture_noimg_200x200);
		}
		ivMain.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				showGoodsDetail(goods.getBarCode());
			}
		});
	}

	private void initAdViews() {
		// ad top
		Goods goods = adapter.goodsList.get(0);
		TextView tvName = (TextView) mFragmentView
				.findViewById(R.id.tv_advtop_name);
		TextView tvCurPrice = (TextView) mFragmentView
				.findViewById(R.id.tv_advtop_curprice);
		TextView tvOriPrice = (TextView) mFragmentView
				.findViewById(R.id.tv_advtop_oriprice);
		TextView tvOriSymbol = (TextView) mFragmentView
				.findViewById(R.id.tv_advtop_oriprice_symbol);
		ImageView ivMain = (ImageView) mFragmentView
				.findViewById(R.id.iv_advtop);
		ImageView ivRoute = (ImageView) mFragmentView
				.findViewById(R.id.iv_advtop_addinrouter);
		fillGoodsInfo(tvName, tvCurPrice, tvOriPrice, tvOriSymbol, ivRoute,
				ivMain, goods);
		// ad mid1
		goods = adapter.goodsList.get(1);
		tvName = (TextView) mFragmentView.findViewById(R.id.tv_advmid1_name);
		tvCurPrice = (TextView) mFragmentView
				.findViewById(R.id.tv_advmid1_curprice);
		tvOriPrice = (TextView) mFragmentView
				.findViewById(R.id.tv_advmid1_oriprice);
		tvOriSymbol = (TextView) mFragmentView
				.findViewById(R.id.tv_advmid1_oriprice_symbol);
		ivMain = (ImageView) mFragmentView.findViewById(R.id.iv_advmid1_main);
		ivRoute = (ImageView) mFragmentView
				.findViewById(R.id.iv_advmid1_addinrouter);
		fillGoodsInfo(tvName, tvCurPrice, tvOriPrice, tvOriSymbol, ivRoute,
				ivMain, goods);
		// ad mid2
		goods = adapter.goodsList.get(2);
		tvName = (TextView) mFragmentView.findViewById(R.id.tv_advmid2_name);
		tvCurPrice = (TextView) mFragmentView
				.findViewById(R.id.tv_advmid2_curprice);
		tvOriPrice = (TextView) mFragmentView
				.findViewById(R.id.tv_advmid2_oriprice);
		tvOriSymbol = (TextView) mFragmentView
				.findViewById(R.id.tv_advmid2_oriprice_symbol);
		ivMain = (ImageView) mFragmentView.findViewById(R.id.iv_advmid2_main);
		ivRoute = (ImageView) mFragmentView
				.findViewById(R.id.iv_advmid2_addinrouter);
		fillGoodsInfo(tvName, tvCurPrice, tvOriPrice, tvOriSymbol, ivRoute,
				ivMain, goods);
		// ad mid3
		goods = adapter.goodsList.get(3);
		tvName = (TextView) mFragmentView.findViewById(R.id.tv_advmid3_name);
		tvCurPrice = (TextView) mFragmentView
				.findViewById(R.id.tv_advmid3_curprice);
		tvOriPrice = (TextView) mFragmentView
				.findViewById(R.id.tv_advmid3_oriprice);
		tvOriSymbol = (TextView) mFragmentView
				.findViewById(R.id.tv_advmid3_oriprice_symbol);
		ivMain = (ImageView) mFragmentView.findViewById(R.id.iv_advmid3_main);
		ivRoute = (ImageView) mFragmentView
				.findViewById(R.id.iv_advmid3_addinrouter);
		fillGoodsInfo(tvName, tvCurPrice, tvOriPrice, tvOriSymbol, ivRoute,
				ivMain, goods);
		// ad bottom1
		goods = adapter.goodsList.get(4);
		tvName = (TextView) mFragmentView.findViewById(R.id.tv_advbottom1_name);
		tvCurPrice = (TextView) mFragmentView
				.findViewById(R.id.tv_advbottom1_curprice);
		tvOriPrice = (TextView) mFragmentView
				.findViewById(R.id.tv_advbottom1_oriprice);
		tvOriSymbol = (TextView) mFragmentView
				.findViewById(R.id.tv_advbottom1_oriprice_symbol);
		ivMain = (ImageView) mFragmentView
				.findViewById(R.id.iv_advbottom1_main);
		ivRoute = (ImageView) mFragmentView
				.findViewById(R.id.iv_advbottom1_addinrouter);
		fillGoodsInfo(tvName, tvCurPrice, tvOriPrice, tvOriSymbol, ivRoute,
				ivMain, goods);
		// ad bottom2
		goods = adapter.goodsList.get(5);
		tvName = (TextView) mFragmentView.findViewById(R.id.tv_advbottom2_name);
		tvCurPrice = (TextView) mFragmentView
				.findViewById(R.id.tv_advbottom2_curprice);
		tvOriPrice = (TextView) mFragmentView
				.findViewById(R.id.tv_advbottom2_oriprice);
		tvOriSymbol = (TextView) mFragmentView
				.findViewById(R.id.tv_advbottom2_oriprice_symbol);
		ivMain = (ImageView) mFragmentView
				.findViewById(R.id.iv_advbottom2_main);
		ivRoute = (ImageView) mFragmentView
				.findViewById(R.id.iv_advbottom2_addinrouter);
		fillGoodsInfo(tvName, tvCurPrice, tvOriPrice, tvOriSymbol, ivRoute,
				ivMain, goods);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		if (mFragmentView == null) {
			mFragmentView = inflater.inflate(R.layout.fragment_3, null);
			SharedPreferences mSharedPreferences = getActivity()
					.getSharedPreferences("suntownshop", 0);
			mIsVip = mSharedPreferences.getBoolean("isvip", false);
			userId = mSharedPreferences.getString("userId", "");
			initOptions();
			mMain = mFragmentView.findViewById(R.id.view_main);
			gridView = (UnScrollGridView) mFragmentView
					.findViewById(R.id.gv_goodslist);

			tvTitle = (TextView) mFragmentView.findViewById(R.id.tv_head_title);
			mLoading = mFragmentView.findViewById(R.id.loading);
			mShaking = mFragmentView.findViewById(R.id.shaking);
			mShaking.setVisibility(View.GONE);
			ivRoute = (ImageView) mFragmentView.findViewById(R.id.iv_route);
			ivRoute.setOnClickListener(this);
			ivRoute.setOnLongClickListener(this);
			tvRouteNum = (TextView) mFragmentView
					.findViewById(R.id.tv_route_num);
			initViews();
			RouteGoodsDb db = new RouteGoodsDb(getActivity(),
					userId);
			routeList = db.getAll();
			db.Close();
			refreshRouteGoods();
		}

		return mFragmentView;

	}

	@Override
	public void onDestroyView() {
		// TODO Auto-generated method stub

		if (mFragmentView != null) {

			((ViewGroup) mFragmentView.getParent()).removeView(mFragmentView);
		}

		super.onDestroyView();
	}

	@Override
	public void onResume() {
		// TODO Auto-generated method stub

		super.onResume();
	}

	@Override
	public void onPause() {
		// TODO Auto-generated method stub

		super.onPause();
	}

	
	private OnMoveViewListener listener = new OnMoveViewListener() {

		@Override
		public void onMove(View v, Goods goods) {
			// TODO Auto-generated method stub
			moveGoods = goods;
			if (imageMoveAnim == null) {
				imageMoveAnim = new ImageMoveAnimation(getActivity());
			}
			imageMoveAnim.setAnim(v, ivRoute, imageMoveListener);
			// setAnim(v);
		}
	};

	private OnImageMoveListener imageMoveListener = new OnImageMoveListener() {

		@Override
		public void onMoveEnd() {
			// TODO Auto-generated method stub
			if (moveGoods != null) {
				RouteGoodsDb db = new RouteGoodsDb(getActivity(), userId);
				db.insertGoods(moveGoods.getBarCode(), moveGoods.getName(),
						moveGoods.getShelfId(), moveGoods.getFloorName());
				routeList = db.getAll();
				db.Close();
				refreshRouteGoods();
			}
		}
	};

	private void refreshRouteGoods() {
		if (routeList.size() > 0) {
			tvRouteNum.setVisibility(View.VISIBLE);
			tvRouteNum.setText("" + routeList.size());
		} else {
			tvRouteNum.setVisibility(View.GONE);
		}
	}

	@Override
	public boolean onLongClick(View v) {
		// TODO Auto-generated method stub
		switch (v.getId()) {
		case R.id.iv_route:
			ConfirmDialog dialog = new ConfirmDialog(getActivity(),
					"确定要清除导航任务中的所有商品吗?", getString(R.string.tips_text),
					getString(R.string.confirm_text),
					getString(R.string.cancel_text));
			if (dialog.ShowDialog()) {
				RouteGoodsDb db = new RouteGoodsDb(getActivity(), userId);
				db.clearAll();
				routeList = db.getAll();
				db.Close();
				refreshRouteGoods();
			}

			break;
		}
		return false;
	}

	public void navigate(View v) {
		if (routeList == null || routeList.size() == 0) {
			return;
		}
		LayoutInflater inflater = (LayoutInflater) getActivity()
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View mView = inflater.inflate(R.layout.select_location_popup, null);

		if (pw == null) {
			// 生成PopupWindow对象
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
					navigate("1号入口", "1号入口", "1F");
				} else {

					Intent openCameraIntent = new Intent(getActivity(),
							CaptureActivity.class);
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

		pw.showAtLocation(mMain, Gravity.CENTER, 0, 0);
	}

	private void navigate(String title, String location, String floorName) {
		if (title == null || location == null || floorName == null
				|| "".equals(title) || "".equals(location)
				|| "".equals(floorName)) {
			Toast.makeText(getActivity(), "找不到该商品的位置，请更换起始地重试...",
					Toast.LENGTH_SHORT).show();
			return;
		}
		Intent i = new Intent(getActivity(), MapActivity.class);
		i.putParcelableArrayListExtra("goodslist", routeList);
		i.putExtra("location", location);
		i.putExtra("title", title);
		i.putExtra("floor", floorName);
		startActivity(i);
		RouteGoodsDb db = new RouteGoodsDb(getActivity(), userId);
		db.clearAll();
		routeList = db.getAll();
		db.Close();
		refreshRouteGoods();
	}

	private void findLocation(String barcode) {
		showProgress(true);
		com.suntown.suntownshop.runnable.GetJsonRunnable getJsonRunnable = new com.suntown.suntownshop.runnable.GetJsonRunnable(
				URL_GOODSDETAIL + barcode, MSG_GET_LOCATION_GOODS, handler);
		new Thread(getJsonRunnable).start();
	}

	private ProgressDialog mPDialog;

	public void showProgress(final boolean show) {
		if (show) {
			mPDialog = new ProgressDialog(getActivity());
			// 实例化
			mPDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			// 设置进度条风格，风格为圆形，旋转的
			// pDialog.setTitle("Google");
			// 设置ProgressDialog 标题
			mPDialog.setMessage(getString(R.string.wait_a_minute));
			// 设置ProgressDialog 提示信息
			// pDialog.setIcon(R.drawable.ic_launcher);
			// 设置ProgressDialog 标题图标
			// mypDialog.setButton();
			// 设置ProgressDialog 的一个Button
			mPDialog.setIndeterminate(false);
			// 设置ProgressDialog 的进度条是否不明确
			mPDialog.setCancelable(false);
			// 设置ProgressDialog 是否可以按退回按键取消
			mPDialog.show();
		} else {
			if (mPDialog != null && mPDialog.isShowing()) {
				mPDialog.dismiss();
				mPDialog = null;
			}
		}
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
			if (format.equalsIgnoreCase("QR_CODE")) { // 二维码
				String UTF_Str = "";
				String GB_Str = "";
				boolean is_cN = false;
				try {
					UTF_Str = new String(scanResult.getBytes("ISO-8859-1"),
							"UTF-8");
					System.out.println("这是转了UTF-8的" + UTF_Str);
					is_cN = IsChineseOrNot.isChineseCharacter(UTF_Str);
					// 防止有人特意使用乱码来生成二维码来判断的情况
					boolean b = IsChineseOrNot.isSpecialCharacter(scanResult);
					if (b) {
						is_cN = true;
					}
					System.out.println("是为:" + is_cN);
					if (!is_cN) {
						GB_Str = new String(scanResult.getBytes("ISO-8859-1"),
								"GB2312");
						System.out.println("这是转了GB2312的" + GB_Str);
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
					Toast.makeText(getActivity(),
							getString(R.string.qrcode_cantfind),
							Toast.LENGTH_SHORT).show();
				}
			} else { // 条码
				findLocation(scanResult);
			}

		}
	}

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		switch (v.getId()) {
		case R.id.iv_route:
			navigate(v);
			break;
		}
	}

}