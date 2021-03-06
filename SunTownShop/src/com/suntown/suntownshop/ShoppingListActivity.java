package com.suntown.suntownshop;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONObject;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.suntown.suntownshop.asynctask.PostAsyncTask;
import com.suntown.suntownshop.asynctask.PostAsyncTask.OnCompleteCallback;
import com.suntown.suntownshop.db.OrderDb;
import com.suntown.suntownshop.db.ShopCartDb;
import com.suntown.suntownshop.model.CartGoods;
import com.suntown.suntownshop.model.Order;
import com.suntown.suntownshop.model.OrderGoods;
import com.suntown.suntownshop.utils.JsonParser;
import com.suntown.suntownshop.utils.XmlParser;
import com.suntown.suntownshop.widget.JustifyTextView;
import com.suntown.zxing.encoding.EncodingHandler;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
/**
 * 购物清单页面类
 *
 * @author 钱凯
 * @version 2015年4月23日 下午4:15:05
 *
 */
public class ShoppingListActivity extends Activity {
	Order mOrder;
	View viewLoading;
	View viewOrder;
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
		setContentView(R.layout.activity_shopping_list);
		Intent intent = getIntent();
		viewLoading = findViewById(R.id.loading);
		viewOrder = findViewById(R.id.view_order);
		viewOrder.setVisibility(View.GONE);
		viewLoading.setVisibility(View.VISIBLE);
		if(intent.hasExtra("orderno")){
			Bundle b = intent.getExtras();
			String orderNo = b.getString("orderno");
			HashMap<String, String> params = new HashMap<String, String>();
			params.put("formno", orderNo);
			PostAsyncTask postAsyncTask = new PostAsyncTask(URL, callback);
			postAsyncTask.execute(params);
		}else{
			Toast.makeText(this, getString(R.string.orderno_err), Toast.LENGTH_SHORT).show();
			finish();
		}
		

	}

	
	private final static String URL = Constants.DOMAIN_NAME+"axis2/services/sunteslwebservice/getHistoryOrderDetail";

	private OnCompleteCallback callback = new OnCompleteCallback() {

		@Override
		public void onComplete(boolean isOk, String msg) {
			// TODO Auto-generated method stub
			//showProgress(false);
			if (isOk) {
				JSONObject jsonObj;
				try {
					msg = XmlParser.parse(msg, "UTF-8", "return");
					jsonObj = new JSONObject(msg);
					int sendState = jsonObj.getInt("RESULT");
					if (sendState == 0) {
						// 取得订单数据，开始解析
						mOrder = JsonParser.orderParse(jsonObj);
						initView();
					} else {
						Toast.makeText(getApplicationContext(), "该订单不存在或已取消!",
								Toast.LENGTH_SHORT).show();
						finish();
					}
				} catch (Exception e) {
					Toast.makeText(getApplicationContext(), "服务器返回错误，请稍后重试...",
							Toast.LENGTH_SHORT).show();

					e.printStackTrace();
				}
			} else {
				Toast.makeText(getApplicationContext(), "连接超时，请稍后重试...",
						Toast.LENGTH_SHORT).show();
			}

		}
	};
	
	private void initView() {
		try {
			
			
			ArrayList<OrderGoods> listDBC = mOrder.getOrderGoodsDBC();
			ArrayList<OrderGoods> listDBM = mOrder.getOrderGoodsDBM();
			TextView tvStore = (TextView)findViewById(R.id.tv_confirm_order_store);
			tvStore.setText(mOrder.getStoreName());
			String time = mOrder.getDate();
			TextView tvTime = (TextView) findViewById(R.id.tv_confirm_order_time);
			tvTime.setText(getString(R.string.date_text) + time);
			//format = new SimpleDateFormat(" 00000yyMMddHHmmss");
			//String orderNo = format.format(date);
			TextView tvOrderNo = (TextView) findViewById(R.id.tv_confirm_order_no);
			tvOrderNo.setText(getString(R.string.order_no) + mOrder.getOrderNo());
			ImageView ivQrCode = (ImageView) findViewById(R.id.iv_qrcode);
			Bitmap qrCodeBitmap = EncodingHandler.createQRCode(mOrder.getOrderNo(), 350);
			ivQrCode.setImageBitmap(qrCodeBitmap);
			ImageView ivBarCode = (ImageView) findViewById(R.id.iv_barcode);
			qrCodeBitmap = EncodingHandler.CreateBarDCode(mOrder.getOrderNo());
			ivBarCode.setImageBitmap(qrCodeBitmap);
			JustifyTextView tvBarCode = (JustifyTextView)findViewById(R.id.tv_barcode);
			tvBarCode.setText(mOrder.getOrderNo());
			double amount = mOrder.getAmount();
			TextView tvAmount = (TextView) findViewById(R.id.tv_shopping_amount);
			tvAmount.setText("总金额:￥"
					+ String.format("%.2f", amount));
			LinearLayout llGoodsDeliverSelf = (LinearLayout) findViewById(R.id.ll_goodslist_dbc);
			LinearLayout llGoodsDeliverMarket = (LinearLayout) findViewById(R.id.ll_goodslist_dbm);
			if (listDBC.size() > 0) {
				llGoodsDeliverSelf.setVisibility(View.VISIBLE);
				fillGoods(llGoodsDeliverSelf, listDBC);
			} else {
				llGoodsDeliverSelf.setVisibility(View.GONE);
			}
			if (listDBM.size() > 0) {
				llGoodsDeliverMarket.setVisibility(View.VISIBLE);
				fillGoods(llGoodsDeliverMarket, listDBM);
			} else {
				llGoodsDeliverMarket.setVisibility(View.GONE);
			}
		} catch (Exception e) {
			// TODO: handle exception
			Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
		}
		viewLoading.setVisibility(View.GONE);
		viewOrder.setVisibility(View.VISIBLE);
	}

	private void fillGoods(LinearLayout ll, ArrayList<OrderGoods> list){
		int count = list.size();
		double amount = 0.0;
		for (int i = 0; i < count; i++) {
			OrderGoods goods = list.get(i);
			int resoure = (i % 2) == 0 ? R.layout.confirm_order_item3
					: R.layout.confirm_order_item2;
			
			View view = LayoutInflater.from(getApplicationContext())
					.inflate(resoure, null);
			TextView tvName = (TextView) view
					.findViewById(R.id.tv_goods_name);
			TextView tvSpec = (TextView) view
					.findViewById(R.id.tv_goods_spec);
			TextView tvPrice = (TextView) view
					.findViewById(R.id.tv_goods_price);
			TextView tvQuantity = (TextView) view
					.findViewById(R.id.tv_goods_quantity);

			tvName.setText(goods.getName());
			tvSpec.setText(goods.getSpec());
			
			tvPrice.setText(String.format("%.2f", goods.getPrice()));
			tvQuantity.setText(goods.getQuantity()+"");
			ll.addView(view);
			amount = amount + goods.getPrice() * goods.getQuantity();
		}
		int resoure = (count % 2) == 0 ? R.layout.confirm_order_item3
				: R.layout.confirm_order_item2;
		View view = LayoutInflater.from(getApplicationContext()).inflate(
				resoure, null);
		TextView tvName = (TextView) view.findViewById(R.id.tv_goods_name);
		TextView tvPrice = (TextView) view
				.findViewById(R.id.tv_goods_price);
		tvName.setText(getString(R.string.amount_nocolon_text));
		tvPrice.setText(String.format("%.2f", amount));
		ll.addView(view);
		
	}
	
	public void close(View v){
		finish();
	}
	
	private OnClickListener mCloseClick = new OnClickListener() {

		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			finish();
		}
	};
}
