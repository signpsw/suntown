package com.suntown.suntownshop;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.suntown.suntownshop.db.ShopCartDb;
import com.suntown.suntownshop.listener.OnShopSelectListener;
import com.suntown.suntownshop.model.Goods;
import com.suntown.suntownshop.model.Store;
import com.suntown.suntownshop.runnable.GetJsonRunnable;
import com.suntown.suntownshop.utils.ImageUtil;
import com.suntown.suntownshop.utils.IsChineseOrNot;
import com.suntown.suntownshop.utils.MyMath;
import com.suntown.suntownshop.widget.PopMenuShopSelect;
import com.suntown.zxing.activity.CaptureActivity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.PaintDrawable;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.provider.Settings;
import android.support.v4.app.Fragment;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;
import android.widget.Toast;
/**
 * ��ҳ
 *
 * @author Ǯ��
 * @version 2014��12��18�� ����4:13:43
 *
 */
public class FragmentPage1 extends Fragment implements OnClickListener {
	private View mFragmentView = null;
	private final static int MSG_GETGOODDETAIL_COMPLETE = 1;
	private final static int MSG_GETGOODSLIST_COMPLETE = 2;
	private final static int MSG_ERR_NETWORKERR = -1;
	private final static int MSG_GETKINDS_COMPLETE = 3;
	private final static int MSG_GET_UPT_COMPLETE = 4;
	private final static int MSG_GET_VIP_COMPLETE = 5;
	private final static int MSG_GET_RECOMMEND_COMPLETE = 8;
	private final static int MSG_BANNER_CHANGED = 6;
	private final static int MSG_GET_ALLSHOP_COMPLETE = 7;

	private boolean isUptOk = false;
	private boolean isVipOk = false;
	private boolean isRecommendOk = false;
	private View mLoading;
	private View mMask;
	private View mMain;
	private TextView tvShopName;
	private ScheduledExecutorService scheduledExecutorService;
	private ViewPager viewPager;
	// ͼƬID
	// private int[] imageResId = new int[] { R.drawable.b1, R.drawable.b2,
	// R.drawable.b3, R.drawable.b4, R.drawable.b5 };
	// �ֲ�ͼƬ����
	private String[] images = new String[] { "b1.jpg", "b2.jpg", "b3.jpg",
			"b4.jpg", "b5.jpg" };
	private List<View> dots;
	private int currentItem = 0; // ��ǰͼƬ��������
	private ImageView imageView;
	private int mTimer = 0;
	private String URL = Constants.DOMAIN_NAME
			+ "axis2/services/sunteslwebservice/Getgoods_info?Barcode=";
	private String URL_UPT = Constants.DOMAIN_NAME
			+ "axis2/services/sunteslwebservice/getgoods_upt_day?startIndex=1&length=6";
	private String URL_RECOMMEND = Constants.DOMAIN_NAME
			+ "axis2/services/sunteslwebservice/getgoods_upt_day?startIndex=6&length=6";
	private String URL_VIP = Constants.DOMAIN_NAME
			+ "axis2/services/sunteslwebservice/getgoods_mem_day?startIndex=1&length=6";
	private String URL_GET_ALLSHOP = Constants.DOMAIN_NAME
			+ "axis2/services/sunteslwebservice/getAllShop";
	private boolean loadSuccess = false;
	private View shopSelect;
	private View viewSearch;
	private LayoutInflater mInflater;
	private PopMenuShopSelect popMenuShopSelect;
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

	private void initBanner() {

		// ��ʼ��ָʾ��
		dots = new ArrayList<View>();
		dots.add(mFragmentView.findViewById(R.id.v_dot0));
		dots.add(mFragmentView.findViewById(R.id.v_dot1));
		dots.add(mFragmentView.findViewById(R.id.v_dot2));
		dots.add(mFragmentView.findViewById(R.id.v_dot3));
		dots.add(mFragmentView.findViewById(R.id.v_dot4));

		viewPager = (ViewPager) mFragmentView.findViewById(R.id.vp);
		ViewGroup.LayoutParams params = viewPager.getLayoutParams();
		params.height = Constants.displayWidth * 394 / 720;
		viewPager.setLayoutParams(params);
		// ����һ������������ViewPager�е�ҳ��ı�ʱ����
		viewPager.setOnPageChangeListener(new MyPageChangeListener());

		// �첽��������
		// new Thread(loadBannerRunnable).start();
		// �������ViewPagerҳ���������
		viewPager.setAdapter(new MyAdapter());

		// ʵ��banner����
		scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
		scheduledExecutorService.scheduleAtFixedRate(new ScrollTask(), 0, 100,
				TimeUnit.MILLISECONDS);
	}
	/**
	 * ˢ�µ�ǰ�ŵ���ʾ
	 * @param shopName
	 */
	public void refreshLocation(String shopName) {
		tvShopName.setText(shopName);
	}

	/**
	 * �����ֲ�����л�����
	 * 
	 * @author goria
	 * 
	 */
	private class ScrollTask implements Runnable {

		public void run() {
			if (images.length == 0) {
				mTimer = 0;
				return;
			}
			if (mTimer < 40) {
				mTimer++;
				return;
			}

			synchronized (viewPager) {
				// ͨ��Handler�л�ͼƬ
				currentItem = (currentItem + 1) % images.length;
				handler.sendEmptyMessage(MSG_BANNER_CHANGED);
			}
			mTimer = 0;
		}

	}

	/**
	 * ��ViewPager��ҳ���״̬�����ı�ʱ����
	 * 
	 * @author Administrator
	 * 
	 */
	private class MyPageChangeListener implements OnPageChangeListener {
		private int oldPosition = 0;

		/**
		 * This method will be invoked when a new page becomes selected.
		 * position: Position index of the new selected page. ʵ��ָʾ�����ֶ�����
		 */
		public void onPageSelected(int position) {
			currentItem = position;
			mTimer = 0;
			dots.get(oldPosition).setBackgroundResource(R.drawable.dot_normal);
			dots.get(position).setBackgroundResource(R.drawable.dot_focused);
			oldPosition = position;
		}

		public void onPageScrollStateChanged(int arg0) {

		}

		public void onPageScrolled(int arg0, float arg1, int arg2) {

		}
	}
	/**
	 * �ֲ����������
	 *
	 * @author Ǯ��
	 * @version 2015��1��11�� ����7:42:09
	 *
	 */
	private class MyAdapter extends PagerAdapter {

		@Override
		public int getCount() {
			return images.length;
			// return data.size();

		}

		@Override
		public Object instantiateItem(View arg0, int position) {
			View imagelayout = getActivity().getLayoutInflater().inflate(
					R.layout.banner_item, null);
			ImageView iv = (ImageView) imagelayout
					.findViewById(R.id.imageView1);
			// Bitmap bm =
			// ImageUtil.readBitMap(getActivity(),imageResId[position]);
			// iv.setImageBitmap(bm);
			imageLoader.displayImage("assets://image/" + images[position], iv);

			((ViewPager) arg0).addView(imagelayout);

			return imagelayout;
		}

		@Override
		public void destroyItem(View arg0, int arg1, Object arg2) {

			((ViewGroup) arg0).removeView((View) arg2);
			/*
			 * ImageView imageView = (ImageView) ((View) arg2)
			 * .findViewById(R.id.imageView1); if (imageView != null &&
			 * imageView.getDrawable() != null) { Bitmap oldBitmap =
			 * ((BitmapDrawable) imageView.getDrawable()) .getBitmap();
			 * imageView.setImageDrawable(null); if (oldBitmap != null) {
			 * oldBitmap.recycle(); oldBitmap = null; }
			 * 
			 * }
			 */
		}

		@Override
		public boolean isViewFromObject(View arg0, Object arg1) {
			return arg0 == arg1;
		}

		@Override
		public void restoreState(Parcelable arg0, ClassLoader arg1) {

		}

		@Override
		public Parcelable saveState() {
			return null;
		}

		@Override
		public void startUpdate(View arg0) {

		}

		@Override
		public void finishUpdate(View arg0) {

		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		mInflater = inflater;
		if (mFragmentView == null || !loadSuccess) {
			mFragmentView = inflater.inflate(R.layout.fragment_1, null);
			mMask = mFragmentView.findViewById(R.id.mask);
			mMask.setVisibility(View.GONE);
			mLoading = mFragmentView.findViewById(R.id.loading);
			mMain = mFragmentView.findViewById(R.id.view_main);
			tvShopName = (TextView) mFragmentView
					.findViewById(R.id.tv_shopname);
			shopSelect = mFragmentView
					.findViewById(R.id.view_main_header_center);
			shopSelect.setOnClickListener(this);
			ImageView ivUptMore = (ImageView) mFragmentView
					.findViewById(R.id.upt_more);
			ImageView ivVipMore = (ImageView) mFragmentView
					.findViewById(R.id.vip_more);
			ImageView ivRecommendMore = (ImageView) mFragmentView
					.findViewById(R.id.recommend_more);
			ivUptMore.setOnClickListener(this);
			ivVipMore.setOnClickListener(this);
			ivRecommendMore.setOnClickListener(this);
			viewSearch = (LinearLayout) mFragmentView
					.findViewById(R.id.view_main_header_right);
			viewSearch.setOnClickListener(this);
			initOptions();
			initViews();
			initBanner();
		}
		SharedPreferences sharedPreferences = getActivity()
				.getSharedPreferences("suntownshop", 0);

		String storeName = sharedPreferences.getString("shopfullname", "");
		if (storeName != null && !"".equals(storeName)) {
			tvShopName.setText(storeName);
		}
		return mFragmentView;
	}

	@Override
	public void onDestroyView() {
		// TODO Auto-generated method stub
		//�־û�Fragment��destoryʱ��Fragment��Parent���Ƴ���CreateViewʱ��������
		if (mFragmentView != null) {
			((ViewGroup) mFragmentView.getParent()).removeView(mFragmentView);
		}
		super.onDestroyView();
	}
	/**
	 * ��ʼ���ؼ�
	 */
	private void initViews() {
		loadSuccess = false;

		GetJsonRunnable getJsonRunnable = new GetJsonRunnable(URL_UPT,
				MSG_GET_UPT_COMPLETE, handler);
		new Thread(getJsonRunnable).start();

		GetJsonRunnable getJsonRunnable2 = new GetJsonRunnable(URL_VIP,
				MSG_GET_VIP_COMPLETE, handler);
		new Thread(getJsonRunnable2).start();

		GetJsonRunnable getJsonRunnable3 = new GetJsonRunnable(URL_RECOMMEND,
				MSG_GET_RECOMMEND_COMPLETE, handler);
		new Thread(getJsonRunnable3).start();
	}
	/**
	 * ������Ʒ������빺�ﳵ���첽�����Ʒ��Ϣ�󱾵����ݿ���¹��ﳵ��
	 * @param barCode
	 */
	private void add2Cart(String barCode) {
		showProgress(true);
		GetJsonRunnable getJsonRunnable = new GetJsonRunnable(URL + barCode,
				MSG_GETGOODDETAIL_COMPLETE, handler);
		new Thread(getJsonRunnable).start();
	}

	/**
	 * ���ÿ�ջ�Ա��Ʒ������� ģ�⣬�ӿ���ɺ���Ҫ����
	 * 
	 * @param jsonArray
	 * @throws JSONException
	 */
	private void fillAdvTableVip(JSONArray jsonArray) throws JSONException {
		// ��һ��
		View adv1 = mFragmentView.findViewById(R.id.view_vip_adv1);
		ImageView ivAdvText1 = (ImageView) mFragmentView
				.findViewById(R.id.iv_vip_adv1_advtext);
		TextView tvAdvName1 = (TextView) mFragmentView
				.findViewById(R.id.tv_vip_adv1_name);
		TextView tvAdvPrice1 = (TextView) mFragmentView
				.findViewById(R.id.tv_vip_adv1_price);
		ImageView ivAdvMain1 = (ImageView) mFragmentView
				.findViewById(R.id.iv_vip_adv1_main);
		JSONObject jsonObj = (JSONObject) jsonArray.opt(3);
		String name = jsonObj.getString("GNAME");
		String vipPrice = jsonObj.getString("MEMPRICE");
		String imgPath = jsonObj.getString("IMGPATH");
		int priceType = jsonObj.getInt("PRICETYPE");
		final String barCode1 = jsonObj.getString("BARCODE");

		imageLoader.displayImage("assets://image/adv_text_vip1.png",
				ivAdvText1, options);
		tvAdvName1.setText(name);
		tvAdvPrice1.setText(vipPrice);
		imageLoader.displayImage("http://" + imgPath, ivAdvMain1, options);
		adv1.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				showGoodsDetail(barCode1);
			}
		});

		// �ڶ���
		View adv2 = mFragmentView.findViewById(R.id.view_vip_adv2);
		ImageView ivAdvText2 = (ImageView) mFragmentView
				.findViewById(R.id.iv_vip_adv2_advtext);
		TextView tvAdvName2 = (TextView) mFragmentView
				.findViewById(R.id.tv_vip_adv2_name);
		TextView tvAdvPrice2 = (TextView) mFragmentView
				.findViewById(R.id.tv_vip_adv2_price);
		ImageView ivAdvMain2 = (ImageView) mFragmentView
				.findViewById(R.id.iv_vip_adv2_main);
		jsonObj = (JSONObject) jsonArray.opt(4);
		name = jsonObj.getString("GNAME");
		vipPrice = jsonObj.getString("MEMPRICE");
		imgPath = jsonObj.getString("IMGPATH");
		priceType = jsonObj.getInt("PRICETYPE");
		final String barCode2 = jsonObj.getString("BARCODE");

		imageLoader.displayImage("assets://image/adv_text_vip2.png",
				ivAdvText2, options);
		tvAdvName2.setText(name);
		tvAdvPrice2.setText(vipPrice);
		imageLoader.displayImage("http://" + imgPath, ivAdvMain2, options);
		adv2.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				showGoodsDetail(barCode2);
			}
		});
	}

	/**
	 * ���ÿ�մ���������� ģ�⣬�ӿ���ɺ���Ҫ����
	 * 
	 * @param jsonArray
	 */
	private void fillAdvTableUtp(JSONArray jsonArray) throws JSONException {
		// ��һ��
		View adv1 = mFragmentView.findViewById(R.id.view_adv1);
		ImageView ivAdvText1 = (ImageView) mFragmentView
				.findViewById(R.id.iv_adv1_advtext);
		TextView tvAdvName1 = (TextView) mFragmentView
				.findViewById(R.id.tv_adv1_name);
		TextView tvAdvPrice1 = (TextView) mFragmentView
				.findViewById(R.id.tv_adv1_price);
		ImageView ivAdvMain1 = (ImageView) mFragmentView
				.findViewById(R.id.iv_adv1_main);
		JSONObject jsonObj = (JSONObject) jsonArray.opt(3);
		String name = jsonObj.getString("GNAME");
		String uptPrice = jsonObj.getString("UPTPRICE");
		String imgPath = jsonObj.getString("IMGPATH");
		int priceType = jsonObj.getInt("PRICETYPE");
		final String barCode1 = jsonObj.getString("BARCODE");

		imageLoader.displayImage("assets://image/adv_text_upt.png", ivAdvText1,
				options);
		tvAdvName1.setText(name);
		tvAdvPrice1.setText(uptPrice + "Ԫ�Ϳ��Դ���");
		imageLoader.displayImage("http://" + imgPath, ivAdvMain1, options);
		adv1.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				showGoodsDetail(barCode1);
			}
		});
		// �ڶ���
		View adv2 = mFragmentView.findViewById(R.id.view_adv2);
		ImageView ivAdvText2 = (ImageView) mFragmentView
				.findViewById(R.id.iv_adv2_advtext);
		TextView tvAdvName2 = (TextView) mFragmentView
				.findViewById(R.id.tv_adv2_name);
		TextView tvAdvPrice2 = (TextView) mFragmentView
				.findViewById(R.id.tv_adv2_price);
		ImageView ivAdvMain2 = (ImageView) mFragmentView
				.findViewById(R.id.iv_adv2_main);
		jsonObj = (JSONObject) jsonArray.opt(4);
		name = jsonObj.getString("GNAME");
		uptPrice = jsonObj.getString("UPTPRICE");
		imgPath = jsonObj.getString("IMGPATH");
		priceType = jsonObj.getInt("PRICETYPE");
		final String barCode2 = jsonObj.getString("BARCODE");

		imageLoader.displayImage("assets://image/adv_text_hugediscount.png",
				ivAdvText2, options);
		tvAdvName2.setText(name);
		tvAdvPrice2.setText(uptPrice);
		imageLoader.displayImage("http://" + imgPath, ivAdvMain2, options);
		adv2.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				showGoodsDetail(barCode2);
			}
		});
		// ������
		View adv3 = mFragmentView.findViewById(R.id.view_adv3);
		ImageView ivAdvText3 = (ImageView) mFragmentView
				.findViewById(R.id.iv_adv3_advtext);
		TextView tvAdvName3 = (TextView) mFragmentView
				.findViewById(R.id.tv_adv3_name);
		TextView tvAdvPrice3 = (TextView) mFragmentView
				.findViewById(R.id.tv_adv3_price);
		ImageView ivAdvMain3 = (ImageView) mFragmentView
				.findViewById(R.id.iv_adv3_main);
		jsonObj = (JSONObject) jsonArray.opt(5);
		name = jsonObj.getString("GNAME");
		uptPrice = jsonObj.getString("UPTPRICE");
		imgPath = jsonObj.getString("IMGPATH");
		priceType = jsonObj.getInt("PRICETYPE");
		final String barCode3 = jsonObj.getString("BARCODE");

		imageLoader.displayImage("assets://image/adv_text_hot.png", ivAdvText3,
				options);
		tvAdvName3.setText(name);
		tvAdvPrice3.setText(uptPrice);
		imageLoader.displayImage("http://" + imgPath, ivAdvMain3, options);
		adv3.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				showGoodsDetail(barCode3);
			}
		});
	}

	/**
	 * �����Ʒ�б�
	 * 
	 * @param jsonArray
	 * @param parent
	 * @param rows
	 * @param cols
	 * @throws JSONException
	 */
	private void fillGoodsTable(JSONArray jsonArray, LinearLayout parent,
			int rows, int cols) throws JSONException {

		// LinearLayout viewUpt = (LinearLayout)
		// mFragmentView.findViewById(R.id.view_upt);
		for (int j = 0; j < rows; j++) {
			LinearLayout linearlayout = new LinearLayout(getActivity());
			int height = (int) (Constants.displayWidth * 1.3 / cols);

			LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT,
					height);
			params.setMargins(0, MyMath.dip2px(getActivity(), 0.5f), 0, 0);
			linearlayout.setLayoutParams(params);
			linearlayout.setOrientation(LinearLayout.HORIZONTAL);
			JSONObject jsonObj;
			for (int i = 0; i < cols; i++) {
				jsonObj = (JSONObject) jsonArray.opt(j * cols + i);
				String name = jsonObj.getString("GNAME");
				String oriPrice = jsonObj.getString("ORIPRICE");
				String uptPrice = jsonObj.getString("UPTPRICE");
				String vipPrice = jsonObj.getString("MEMPRICE");
				String imgPath = jsonObj.getString("IMGPATH");
				int priceType = jsonObj.getInt("PRICETYPE");

				final String barCode = jsonObj.getString("BARCODE");
				View view = LayoutInflater.from(getActivity()).inflate(
						R.layout.main_promo_item, null);
				params = new LayoutParams(LayoutParams.MATCH_PARENT,
						LayoutParams.MATCH_PARENT, 1.0f);
				if (i != 0) {
					params.setMargins(MyMath.dip2px(getActivity(), 0.5f), 0, 0,
							0);
				}
				view.setLayoutParams(params);
				TextView tvName = (TextView) view.findViewById(R.id.tv_name);
				TextView tvOriprice = (TextView) view
						.findViewById(R.id.tv_oriprice);
				tvOriprice.getPaint().setFlags(Paint.STRIKE_THRU_TEXT_FLAG);
				TextView tvUptPrice = (TextView) view
						.findViewById(R.id.tv_uptprice);
				ImageView ivMain = (ImageView) view.findViewById(R.id.iv_main);
				tvName.setText(name);
				if (priceType == 2) {
					tvOriprice.setText("��" + uptPrice);
					tvUptPrice.setText(vipPrice);
				} else {
					if (Double.valueOf(uptPrice) > Double.valueOf(oriPrice)) {
						tvOriprice.setVisibility(View.INVISIBLE);
					} else {
						tvOriprice.setText("��" + oriPrice);
					}
					tvUptPrice.setText(uptPrice);
				}
				if (imgPath != null && imgPath.length() > 0) {
					imageLoader.displayImage("http://" + imgPath, ivMain,
							options);
				} else {
					ivMain.setImageResource(R.drawable.picture_noimg_200x200);
				}
				view.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View v) {
						// TODO Auto-generated method stub
						showGoodsDetail(barCode);
					}
				});
				linearlayout.addView(view);
			}
			parent.addView(linearlayout);
		}
	}

	private Handler handler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			// TODO Auto-generated method stub
			if (getActivity() == null || getActivity().isFinishing()) {
				return;
			}
			showProgress(false);
			Bundle bundle;
			String strMsg;
			JSONObject jsonObj;
			JSONArray jsonArray;
			boolean isDone = false;
			switch (msg.what) {
			// ȡ�����г���
			case MSG_GET_ALLSHOP_COMPLETE:
				bundle = msg.getData();
				strMsg = bundle.getString("MSG_JSON");
				try {
					jsonObj = new JSONObject(strMsg);
					if (jsonObj != null) {
						int state = jsonObj.getInt("RESULT");
						if (state == 0) {
							jsonArray = jsonObj.getJSONArray("SHOP");
							String storeId;
							String storeSName;
							String storeFName;
							String floors;
							String floorNames;
							JSONArray jsonFloors;
							listStore.clear();
							for (int i = 0; i < jsonArray.length(); i++) {
								jsonObj = jsonArray.optJSONObject(i);
								storeId = jsonObj.getString("SID");
								storeSName = jsonObj.getString("SNAME");
								storeFName = jsonObj.getString("FNAME");
								jsonFloors = jsonObj.getJSONArray("FLOOR");
								floors = "";
								floorNames = "";
								if (jsonFloors != null
										|| jsonFloors.length() > 0) {
									for (int j = 0; j < jsonFloors.length(); j++) {
										jsonObj = jsonFloors.optJSONObject(j);
										if (j > 0) {
											floors += ";";
											floorNames += ";";
										}
										floors += jsonObj.getString("FLOORNO");
										floorNames += jsonObj
												.getString("FLOORNAME");
									}
								}
								Store store = new Store(storeId, storeSName,
										storeFName, floors, floorNames);
								listStore.add(store);
							}
							if (listStore.size() > 0) {
								selectStore();
							}
						}
					}
				} catch (Exception e) {
					// TODO Auto-generated catch block
					Toast.makeText(getActivity(), "ERROR:" + e.getMessage(),
							Toast.LENGTH_SHORT).show();
					e.printStackTrace();
				}
				break;
			// ͼƬ�ֲ��ź�
			case MSG_BANNER_CHANGED:
				if (viewPager == null || "".equals(viewPager)) {
					return;
				} else {
					viewPager.setCurrentItem(currentItem);// �л���ǰ��ʾ��ͼƬ
				}
				break;
			// ��ò���ϲ����Ʒ
			case MSG_GET_RECOMMEND_COMPLETE:
				loadSuccess = true;
				bundle = msg.getData();
				strMsg = bundle.getString("MSG_JSON");
				try {
					jsonObj = new JSONObject(strMsg);
					jsonArray = jsonObj.getJSONArray("INFO");
					LinearLayout viewRecommend = (LinearLayout) mFragmentView
							.findViewById(R.id.view_recommend);
					fillGoodsTable(jsonArray, viewRecommend, 2, 3);
					isRecommendOk = true;
				} catch (Exception e) {
					// TODO Auto-generated catch block
					Toast.makeText(getActivity(), "ERROR:" + e.getMessage(),
							Toast.LENGTH_SHORT).show();
					e.printStackTrace();
				}

				break;
			// ��õ��մ�����Ʒ
			case MSG_GET_UPT_COMPLETE:
				loadSuccess = true;
				bundle = msg.getData();
				strMsg = bundle.getString("MSG_JSON");
				try {
					jsonObj = new JSONObject(strMsg);
					jsonArray = jsonObj.getJSONArray("INFO");
					LinearLayout viewUpt = (LinearLayout) mFragmentView
							.findViewById(R.id.view_upt);
					fillAdvTableUtp(jsonArray);
					fillGoodsTable(jsonArray, viewUpt, 1, 3);
					isUptOk = true;
				} catch (Exception e) {
					// TODO Auto-generated catch block
					Toast.makeText(getActivity(), "ERROR:" + e.getMessage(),
							Toast.LENGTH_SHORT).show();
					e.printStackTrace();
				}
				break;
			// ���VIP��Ʒ
			case MSG_GET_VIP_COMPLETE:
				loadSuccess = true;
				bundle = msg.getData();
				strMsg = bundle.getString("MSG_JSON");
				try {
					jsonObj = new JSONObject(strMsg);
					jsonArray = jsonObj.getJSONArray("INFO");
					LinearLayout viewUpt = (LinearLayout) mFragmentView
							.findViewById(R.id.view_vip);
					fillAdvTableVip(jsonArray);
					fillGoodsTable(jsonArray, viewUpt, 1, 3);
					isVipOk = true;
				} catch (Exception e) {
					// TODO Auto-generated catch block
					Toast.makeText(getActivity(), "ERROR:" + e.getMessage(),
							Toast.LENGTH_SHORT).show();
					e.printStackTrace();
				}
				break;
			// �����Ʒ����
			case MSG_GETGOODDETAIL_COMPLETE:

				bundle = msg.getData();
				strMsg = bundle.getString("MSG_JSON");
				try {
					jsonObj = new JSONObject(strMsg);
					jsonArray = jsonObj.getJSONArray("INFO");
					if (jsonArray.length() > 0) {
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
						int deliverType = jsonObj.getInt("DELIVERYMODE");
						Goods goods = new Goods(barCode, gCode, gName, gKind,
								gUnit, gOriPrice, gMemPrice, gUptPrice, gSpec,
								gClass, gProvider, gBrand, gOrigin, gImgPath,
								priceType, deliverType);
						try {
							SharedPreferences mSharedPreferences = getActivity()
									.getSharedPreferences("suntownshop", 0);
							String userId = mSharedPreferences.getString(
									"userId", "");
							boolean isVip = mSharedPreferences.getBoolean(
									"isvip", false);
							ShopCartDb scdb = new ShopCartDb(getActivity(),
									userId);

							double curPrice = goods.getCurPrice(isVip);

							if (scdb.insertGoods(goods.getBarCode(),
									goods.getName(), goods.getImgPath(),
									goods.getSpec(), curPrice, 1,
									goods.getDeliverType())) {
								Toast.makeText(getActivity(), "���빺�ﳵ�ɹ�",
										Toast.LENGTH_SHORT).show();
								isDone = true;
							} else {
								Toast.makeText(getActivity(), "���빺�ﳵʧ��",
										Toast.LENGTH_SHORT).show();
							}
							scdb.Close();
						} catch (Exception e) {
							// TODO: handle exception
							Toast.makeText(getActivity(),
									"ERROR:���빺�ﳵ����" + e.getMessage(),
									Toast.LENGTH_LONG).show();
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
			case MSG_ERR_NETWORKERR:
				loadSuccess = false;
				bundle = msg.getData();
				strMsg = bundle.getString("MSG_ERR");
				Toast.makeText(getActivity(), "���ӳ�ʱ�����Ժ�����...",
						Toast.LENGTH_LONG).show();
				break;
			}

			if (isDone) {
				Intent intent = new Intent(getActivity(), MainTabActivity.class);
				Bundle b = new Bundle();
				b.putInt("gototab", 2);
				intent.putExtras(b);
				startActivity(intent);
			}
			if (isUptOk && isVipOk && isRecommendOk) {
				mLoading.setVisibility(View.GONE);
				mMain.setVisibility(View.VISIBLE);
			}
			super.handleMessage(msg);
		}

	};

	private ProgressDialog mPDialog;

	public void showProgress(final boolean show) {
		if (show) {
			mPDialog = new ProgressDialog(getActivity());
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

	/**
	 * ������������ת����Ʒ����ҳ
	 * 
	 * @param barCode
	 */
	private void showGoodsDetail(String barCode) {
		Intent intent = new Intent(getActivity(), GoodsDetailActivity.class);
		Bundle b = new Bundle();
		b.putString("barCode", barCode);
		intent.putExtras(b);
		startActivity(intent);
	}

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub

		switch (v.getId()) {
		case R.id.upt_more:
			showSpecialGoods(0);
			break;
		case R.id.vip_more:
			showSpecialGoods(1);
			break;
		case R.id.recommend_more:
			showSpecialGoods(0);
			break;
		case R.id.view_main_header_center:
			if (listStore.size() > 0) {
				selectStore();
			} else {
				showProgress(true);
				GetJsonRunnable getJsonRunnable = new GetJsonRunnable(
						URL_GET_ALLSHOP, MSG_GET_ALLSHOP_COMPLETE, handler);
				new Thread(getJsonRunnable).start();
			}
			break;
		case R.id.view_main_header_right:
			Intent intent = new Intent(getActivity(), SearchActivity.class);
			startActivity(intent);
			break;
		}

	}

	private void showSpecialGoods(int type) {
		Intent intent = new Intent(getActivity(),
				SpecialGoodsListActivity.class);
		intent.putExtra("type", type);
		startActivity(intent);
	}

	private PopupWindow pw;
	private ArrayList<Store> listStore = new ArrayList<Store>();

	static class ViewHolder {
		TextView tvShopName;
	}

	private BaseAdapter adapterStore = new BaseAdapter() {

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			// TODO Auto-generated method stub
			ViewHolder viewHolder;
			if (convertView == null) {
				convertView = mInflater.inflate(R.layout.shop_item, null);
				viewHolder = new ViewHolder();
				viewHolder.tvShopName = (TextView) convertView
						.findViewById(R.id.tv_shop);
				convertView.setTag(viewHolder);
			} else {
				viewHolder = (ViewHolder) convertView.getTag();
			}
			viewHolder.tvShopName
					.setText(listStore.get(position).getFullName());
			return convertView;
		}

		@Override
		public long getItemId(int position) {
			// TODO Auto-generated method stub
			return position;
		}

		@Override
		public Object getItem(int position) {
			// TODO Auto-generated method stub
			return listStore.get(position);
		}

		@Override
		public int getCount() {
			// TODO Auto-generated method stub
			return listStore.size();
		}
	};

	private OnShopSelectListener shopSelectListener = new OnShopSelectListener() {

		@Override
		public void onShopSelect(Store store) {
			// TODO Auto-generated method stub
			SharedPreferences mSharedPreferences = getActivity()
					.getSharedPreferences("suntownshop", 0);
			SharedPreferences.Editor mEditor = mSharedPreferences.edit();
			mEditor.putString("shopid", store.getId());
			mEditor.putString("shopfullname", store.getFullName());
			mEditor.putString("floors", store.getFloors());
			mEditor.putString("floornames", store.getFloorNames());
			mEditor.commit();
			tvShopName.setText(store.getFullName());
		}
	};

	private void selectStore() {
		if (popMenuShopSelect == null) {
			popMenuShopSelect = new PopMenuShopSelect(getActivity(),
					R.drawable.frame_multi_window_left,
					R.drawable.frame_multi_window_mid,
					R.drawable.frame_multi_window_right, listStore,
					shopSelectListener);
		}
		popMenuShopSelect.show(shopSelect, getActivity());

		/*
		 * listStore.clear(); String floors = "1;2"; String floorNames =
		 * "1F;2F"; Store store = new Store("571002001", "姼ҵ�",
		 * "�������(姼ҵ�)",floors,floorNames); listStore.add(store); store = new
		 * Store("571002002", "ʩ���ŵ�", "�������(ʩ���ŵ�)",floors,floorNames);
		 * listStore.add(store); store = new Store("571002003", "���͵�",
		 * "�������(���͵�)",floors,floorNames); listStore.add(store);
		 */
		/*
		 * View mView = mInflater.inflate(R.layout.select_shop_popup, null);
		 * 
		 * if (pw == null) { // ����PopupWindow���� pw = new PopupWindow(mView,
		 * LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
		 * pw.setOutsideTouchable(true); } else { pw.setContentView(mView); }
		 * pw.setFocusable(true);
		 * 
		 * pw.setBackgroundDrawable(new ColorDrawable(0x7f000000)); ListView lv
		 * = (ListView) mView.findViewById(R.id.lv_shop);
		 * 
		 * lv.setOnItemClickListener(new OnItemClickListener() {
		 * 
		 * @Override public void onItemClick(AdapterView<?> arg0, View arg1, int
		 * position, long arg3) { // TODO Auto-generated method stub if (pw !=
		 * null && pw.isShowing()) { pw.dismiss(); } Store store =
		 * listStore.get(position); SharedPreferences mSharedPreferences =
		 * getActivity() .getSharedPreferences("suntownshop", 0);
		 * SharedPreferences.Editor mEditor = mSharedPreferences.edit();
		 * mEditor.putString("shopid", store.getId());
		 * mEditor.putString("shopfullname", store.getFullName());
		 * mEditor.putString("floors", store.getFloors());
		 * mEditor.putString("floornames", store.getFloorNames());
		 * mEditor.commit(); tvShopName.setText(store.getFullName()); } });
		 * 
		 * lv.setAdapter(adapterStore); // ��ָ��λ�õ�������
		 * mView.setOnClickListener(new OnClickListener() {
		 * 
		 * @Override public void onClick(View v) { // TODO Auto-generated method
		 * stub if (pw != null && pw.isShowing()) { pw.dismiss(); } } });
		 * 
		 * pw.showAtLocation(mFragmentView, Gravity.CENTER, 0, 0);
		 */
	}
}