package com.keju.baby.activity.doctor;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.TextView;

import com.keju.baby.AsyncImageLoader;
import com.keju.baby.AsyncImageLoader.ImageCallback;
import com.keju.baby.CommonApplication;
import com.keju.baby.Constants;
import com.keju.baby.R;
import com.keju.baby.SystemException;
import com.keju.baby.activity.baby.BabyDetailActivity;
import com.keju.baby.activity.base.BaseActivity;
import com.keju.baby.bean.AddBabyBean;
import com.keju.baby.bean.BabyBean;
import com.keju.baby.bean.ResponseBean;
import com.keju.baby.db.DataBaseAdapter;
import com.keju.baby.helper.BusinessHelper;
import com.keju.baby.util.AndroidUtil;
import com.keju.baby.util.NetUtil;
import com.keju.baby.util.SharedPrefUtil;

/**
 * 医生首页界面
 * 
 * @author Zhoujun
 * @version 创建时间：2013-10-25 下午2:51:05
 */

public class DoctorHomeActivity extends BaseActivity implements OnCheckedChangeListener, OnItemClickListener,
		OnClickListener {
	private RadioGroup doctorHomeRadioGroup; // 主页面radiogroup
	private ListView listView; //
	private List<BabyBean> allList;// 所有的list
	private View vFooterAll;
	private ProgressBar pbFooterAll;
	private TextView tvFooterMoreAll;
	private boolean isAllLoad = false;// 是否正在加载数据
	private boolean isAllLoadMore = false;
	private boolean isAllComplete = false;// 是否加载完了；

	private int allPageIndex = 1;
	private ListView listViewCollect; // 收藏的婴儿列表
	private List<BabyBean> collectList;// 收藏的list
	private View vFooterCollect;
	private ProgressBar pbFooterCollect;
	private TextView tvFooterMoreCollect;

	private List<BabyBean> list; // adapter数据源
	private HomeAdapter adapter;

	private boolean isCollectLoad = false;// 是否正在加载数据
	private boolean isCollectLoadMore = false;
	private boolean isCollectComplete = false;// 是否加载完了；
	private int collectPageIndex = 1;

	private long exitTime;
	private ImageView btnLeft, btnRight;
	private TextView tvTitle;
	private boolean isRefresh = false;
	private RefreshReceiver receiver;

	private DataBaseAdapter dba;
	private static final String TABLE_NAME_ADDBABY = "addBaby";
	private static final String TABLE_NAME_BABYLIST = "baby_list";

	private Boolean isMycollect = false;// 是不是收藏界面的取消和收藏

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.doctor_home);
		dba = ((CommonApplication) getApplicationContext()).getDbAdapter();
		findView();
		fillData();
		IntentFilter filter = new IntentFilter(Constants.REQUEST_CREATE_BABY + "");
		receiver = new RefreshReceiver();
		this.registerReceiver(receiver, filter);// 注册一个广播
	}

	private class RefreshReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(Constants.REQUEST_CREATE_BABY + "")) {
				if (NetUtil.checkNet(DoctorHomeActivity.this)) {
					refreshAllBabyData();
				} else {
					loadCache();
				}
			}
		}

	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		unregisterReceiver(receiver);
	}

	private void findView() {

		btnLeft = (ImageView) findViewById(R.id.btnLeft);
		btnLeft.setImageResource(R.drawable.btn_create_account_selector);
		btnRight = (ImageView) findViewById(R.id.btnRight);
		btnRight.setImageResource(R.drawable.btn_search_selector);
		tvTitle = (TextView) findViewById(R.id.tvTitle);
		btnRight.setOnClickListener(this);
		btnLeft.setOnClickListener(this);

		// 加载更多footer
		vFooterAll = getLayoutInflater().inflate(R.layout.footer, null);
		pbFooterAll = (ProgressBar) vFooterAll.findViewById(R.id.progressBar);
		tvFooterMoreAll = (TextView) vFooterAll.findViewById(R.id.tvMore);
		// 加载收藏更多footer
		vFooterCollect = getLayoutInflater().inflate(R.layout.footer, null);
		pbFooterCollect = (ProgressBar) vFooterCollect.findViewById(R.id.progressBar);
		tvFooterMoreCollect = (TextView) vFooterCollect.findViewById(R.id.tvMore);
		listView = (ListView) findViewById(R.id.listView);
		listViewCollect = (ListView) findViewById(R.id.listViewCollect);

		doctorHomeRadioGroup = (RadioGroup) findViewById(R.id.dochome_radio_group);

		if (NetUtil.checkNet(DoctorHomeActivity.this)) {
			if (dba.tabbleIsExist(TABLE_NAME_ADDBABY) == true) {
				List<AddBabyBean> addBabyList = new ArrayList<AddBabyBean>();
				addBabyList = dba.getNewBabyList();
				for (AddBabyBean bean : addBabyList) {
					new CreatBabyAccountTask(bean.getBaby_name(), bean.getIncepting_password(),
							bean.getPatriarch_tel(), bean.getSex(), bean.getExpected_data(), bean.getBorn_data(),
							bean.getWeight(), bean.getHeight(), bean.getHead_size(), bean.getDelivery_way(),
							bean.getComplication(), bean.getStandardStr()).execute();
				}
				if (dba.clearTableData(TABLE_NAME_ADDBABY)) {
					// showShortToast("新增婴儿表已清空");
				}
			}
		}

	}

	/**
	 * 数据填充
	 */
	private void fillData() {
		tvTitle.setText("营养随访体系");

		adapter = new HomeAdapter();
		list = new ArrayList<BabyBean>();
		allList = new ArrayList<BabyBean>();
		collectList = new ArrayList<BabyBean>();
		listView.addFooterView(vFooterAll);
		listView.setAdapter(adapter);
		listView.setOnScrollListener(LoadListener);
		listView.setOnItemClickListener(itemListener);

		listViewCollect.addFooterView(vFooterCollect);
		listViewCollect.setAdapter(adapter);
		listViewCollect.setOnScrollListener(LoadListener);
		listViewCollect.setOnItemClickListener(itemListener);

		doctorHomeRadioGroup.setOnCheckedChangeListener(this);

		if (NetUtil.checkNet(this)) {
			new GetBabyListTask().execute();
		} else {
			loadCache();
		}
	}

	/**
	 * 加載緩存或数据库数据
	 */
	private void loadCache() {
		// 数据库加载数据
		List<BabyBean> babyList = dba.getBabyListData();
		allList.clear();
		allList.addAll(babyList);
		list.clear();
		list.addAll(babyList);
		adapter.notifyDataSetChanged();
	}

	/**
	 * 刷新数据
	 */
	private void refreshAllBabyData() {
		if (NetUtil.checkNet(this)) {
			isRefresh = true;
			allPageIndex = 1;
			new GetBabyListTask().execute();
		} else {
			showShortToast(R.string.NoSignalException);
		}
	}

	/**
	 * 刷新数据收藏婴儿数据
	 */
	private void refreshCollectBabyData() {
		if (NetUtil.checkNet(this)) {
			isRefresh = true;
			collectPageIndex = 1;
			new GetCollectBabyListTask().execute();
		} else {
			showShortToast(R.string.NoSignalException);
		}
	}

	/**
	 * listview点击事件
	 */
	OnItemClickListener itemListener = new OnItemClickListener() {

		@Override
		public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
			if (list != null && list.size() > 0) {
				if(NetUtil.checkNet(DoctorHomeActivity.this)){
					Bundle b = new Bundle();
					b.putSerializable(Constants.EXTRA_DATA, list.get(arg2));
					openActivity(BabyDetailActivity.class, b);
				}else{
					List<BabyBean> babyList = dba.getBabyListData();
					Bundle b = new Bundle();
					b.putSerializable(Constants.EXTRA_DATA, babyList.get(arg2));
					openActivity(BabyDetailActivity.class, b);
				}
				
			}
		}
	};
	/**
	 * 滚动监听器
	 */
	OnScrollListener LoadListener = new OnScrollListener() {
		@Override
		public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
			if (firstVisibleItem + visibleItemCount == totalItemCount) {
				isAllLoadMore = true;
				isCollectLoadMore = true;
			} else {
				isAllLoadMore = false;
				isCollectLoadMore = false;
			}
		}

		@Override
		public void onScrollStateChanged(AbsListView view, int scrollState) {
			// 滚动到最后，默认加载下一页
			if (scrollState == OnScrollListener.SCROLL_STATE_IDLE && isAllLoadMore) {
				if (NetUtil.checkNet(context)) {
					if (doctorHomeRadioGroup.getCheckedRadioButtonId() == R.id.dochome_allbaby) {
						if (!isAllLoad && !isAllComplete) {
							new GetBabyListTask().execute();
						}
					} else {
						if (!isCollectLoad && !isCollectComplete) {
							new GetCollectBabyListTask().execute();
						}
					}
				} else {
					showShortToast(R.string.NoSignalException);
				}
			} else {

			}
		}
	};

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN) {
			if ((System.currentTimeMillis() - exitTime) > 2000) {
				showShortToast(R.string.try_again_logout);
				exitTime = System.currentTimeMillis();
			} else {
				AndroidUtil.exitApp(this);
				finish();
			}
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

	}

	@Override
	public void onCheckedChanged(RadioGroup group, int checkedId) {
		switch (checkedId) {
		case R.id.dochome_allbaby:
			isMycollect = false;
			isAllComplete = false;
			list.clear();
			allList.clear();
			if (NetUtil.checkNet(DoctorHomeActivity.this)) {
				refreshAllBabyData();
			} else {
				loadCache();
			}
			listView.setVisibility(View.VISIBLE);
			listViewCollect.setVisibility(View.GONE);
			break;
		case R.id.dochome_mycollect:
			isMycollect = true;
			isCollectComplete = false;
			list.clear();
			collectList.clear();
			refreshCollectBabyData();
			listView.setVisibility(View.GONE);
			listViewCollect.setVisibility(View.VISIBLE);
			break;
		}
	}

	private class HomeAdapter extends BaseAdapter {

		@Override
		public int getCount() {
			return list.size();
		}

		@Override
		public Object getItem(int position) {
			return position;
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			final BabyBean bean = list.get(position);
			ViewHolder holder = null;
			if (convertView == null) {
				holder = new ViewHolder();
				convertView = getLayoutInflater().inflate(R.layout.doctor_home_item, null);
				holder.ivAvatar = (ImageView) convertView.findViewById(R.id.ivAvatar);
				holder.ivCollect = (ImageView) convertView.findViewById(R.id.ivCollect);
				holder.tvName = (TextView) convertView.findViewById(R.id.tvName);
				holder.tvAge = (TextView) convertView.findViewById(R.id.tvAge);
				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}
			String url = bean.getAvatarUrl();
			holder.ivAvatar.setTag(url);
			Drawable cacheDrawble = AsyncImageLoader.getInstance().loadDrawable(url, new ImageCallback() {

				@Override
				public void imageLoaded(Drawable imageDrawable, String imageUrl) {
					ImageView image = (ImageView) listView.findViewWithTag(imageUrl);
					if (image != null) {
						if (imageDrawable != null) {
							image.setImageDrawable(imageDrawable);
						} else {
							image.setImageResource(R.drawable.item_lion);
						}
					}
				}
			});
			if (cacheDrawble != null) {
				holder.ivAvatar.setImageDrawable(cacheDrawble);
			} else {
				holder.ivAvatar.setImageResource(R.drawable.item_lion);
			}
			if (bean.isCollect()) {
				holder.ivCollect.setImageResource(R.drawable.ic_collected);
			} else {
				holder.ivCollect.setImageResource(R.drawable.ic_collect_not);
			}
			holder.ivCollect.setOnClickListener(new View.OnClickListener() {

				@Override
				public void onClick(View v) {
					if (NetUtil.checkNet(DoctorHomeActivity.this)) {
						new CollectTask(bean).execute();
					} else {
						showShortToast("当前无网络连接无法收藏");
					}
				}
			});

			holder.tvName.setText(bean.getName());
			holder.tvAge.setText(bean.getAge());
			return convertView;
		}

	}

	class ViewHolder {
		public ImageView ivAvatar, ivCollect;
		public TextView tvName, tvAge;
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.btnRight:
			openActivity(SearchActivity.class);
			break;
		case R.id.btnLeft:
			// Intent intent = new
			// Intent(this,DoctorCreatBabyAccountActivity.class);
			// startActivityForResult(intent, Constants.REQUEST_CREATE_BABY);
			openActivity(DoctorCreatBabyAccountActivity.class);
			break;
		default:
			break;
		}

	}

	// @Override
	// protected void onActivityResult(int requestCode, int resultCode, Intent
	// data) {
	// super.onActivityResult(requestCode, resultCode, data);
	// if(resultCode == RESULT_OK && requestCode ==
	// Constants.REQUEST_CREATE_BABY ){
	// refreshAllBabyData();
	// }
	// }
	/**
	 * 获取婴儿列表
	 * 
	 * @author Zhoujun
	 * 
	 */
	private class GetBabyListTask extends AsyncTask<Void, Void, ResponseBean<BabyBean>> {

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			if (isAllLoadMore) {
				isAllLoad = true;
				pbFooterAll.setVisibility(View.VISIBLE);
				tvFooterMoreAll.setText(R.string.loading);
			}
			if (isRefresh) {
				showPd(R.string.loading);
			}
		}

		@Override
		protected ResponseBean<BabyBean> doInBackground(Void... params) {
			int doctor_id = SharedPrefUtil.getUid(context);
			return new BusinessHelper().getBabyList(allPageIndex, doctor_id);
		}

		@Override
		protected void onPostExecute(ResponseBean<BabyBean> result) {
			super.onPostExecute(result);
			pbFooterAll.setVisibility(View.GONE);
			dismissPd();
			if (result.getStatus() == Constants.REQUEST_SUCCESS) {
				List<BabyBean> tempList = result.getObjList();
				boolean isLastPage = false;
				list.clear();
				if (isRefresh) {
					allList.clear();
				}
				if (tempList.size() > 0) {
					allList.addAll(tempList);
					adapter.notifyDataSetChanged();
					if(dba.qryCacheIsExist(allPageIndex)){
						dba.updateCache(tempList,allPageIndex);
					}else{
						dba.inserBabyListData(tempList,allPageIndex);
					}
					allPageIndex++;
				} else {
//					dba.clearTableData(TABLE_NAME_BABYLIST); // 当后台将所有的婴儿给删除时，要将婴儿表数据清空
					isLastPage = true;
				}
				if (isLastPage) {
					pbFooterAll.setVisibility(View.GONE);
					tvFooterMoreAll.setText(R.string.load_all);
					isAllComplete = true;
				} else {
					if (tempList.size() > 0 && tempList.size() < Constants.PAGE_SIZE) {
						pbFooterAll.setVisibility(View.GONE);
						tvFooterMoreAll.setText("");
						isAllComplete = true;
					} else {
						pbFooterAll.setVisibility(View.GONE);
						tvFooterMoreAll.setText("上拉查看更多");
					}
				}
				if (allPageIndex == 1 && tempList.size() == 0) {
					tvFooterMoreAll.setText("");
				}

			} else {
				tvFooterMoreAll.setText("");
				showShortToast(result.getError());
			}
			list.clear();
			list.addAll(allList);
			adapter.notifyDataSetChanged();
			isAllLoad = false;
			isRefresh = false;
		}

	}

	/**
	 * 获取收藏的婴儿列表
	 * 
	 * @author Zhoujun
	 * 
	 */
	private class GetCollectBabyListTask extends AsyncTask<Void, Void, ResponseBean<BabyBean>> {

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			if (isCollectLoadMore) {
				isCollectLoad = true;
				pbFooterCollect.setVisibility(View.VISIBLE);
				tvFooterMoreCollect.setText(R.string.loading);
			}
			if (isRefresh) {
				showPd(R.string.loading);
			}
		}

		@Override
		protected ResponseBean<BabyBean> doInBackground(Void... params) {
			int doctor_id = SharedPrefUtil.getUid(context);
			return new BusinessHelper().getCollectBabyList(collectPageIndex, doctor_id);
		}

		@Override
		protected void onPostExecute(ResponseBean<BabyBean> result) {
			super.onPostExecute(result);
			pbFooterCollect.setVisibility(View.GONE);
			dismissPd();
			if (result.getStatus() == Constants.REQUEST_SUCCESS) {
				List<BabyBean> tempList = result.getObjList();
				boolean isLastPage = false;
				if (tempList.size() > 0) {
//					SharedPrefUtil.setCollectBaby(DoctorHomeActivity.this, result.getJsonData());
					collectList.addAll(tempList);
					collectPageIndex++;
				} else {
					isLastPage = true;
				}
				if (isLastPage) {
					pbFooterCollect.setVisibility(View.GONE);
					tvFooterMoreCollect.setText(R.string.load_all);
					isCollectComplete = true;
				} else {
					if (tempList.size() > 0 && tempList.size() < Constants.PAGE_SIZE) {
						pbFooterCollect.setVisibility(View.GONE);
						tvFooterMoreCollect.setText("");
						isCollectComplete = true;
					} else {
						pbFooterCollect.setVisibility(View.GONE);
						tvFooterMoreCollect.setText("上拉查看更多");
					}
				}
				if (collectPageIndex == 1 && tempList.size() == 0) {
					tvFooterMoreCollect.setText("");
				}

			} else {
				tvFooterMoreCollect.setText("");
				showShortToast(result.getError());
			}
			list.clear();
			list.addAll(collectList);
			adapter.notifyDataSetChanged();
			isCollectLoad = false;
			isRefresh = false;
		}

	}

	/**
	 * 收藏取消收藏
	 * 
	 * @author Zhoujun
	 * 
	 */
	private class CollectTask extends AsyncTask<Void, Void, JSONObject> {
		private BabyBean bean;

		public CollectTask(BabyBean bean) {
			super();
			this.bean = bean;
		}

		@Override
		protected JSONObject doInBackground(Void... params) {
			int doctorId = SharedPrefUtil.getUid(DoctorHomeActivity.this);
			try {
				return new BusinessHelper().collectBaby(bean.getId(), doctorId);
			} catch (SystemException e) {
				return null;
			}
		}

		@Override
		protected void onPostExecute(JSONObject result) {
			super.onPostExecute(result);
			if (result != null) {
				try {
					int status = result.getInt("code");
					if (status == Constants.REQUEST_SUCCESS) {
						boolean isCollect = bean.isCollect();
						bean.setCollect(!isCollect);
						adapter.notifyDataSetChanged();
						if (isCollect) {
							if (isMycollect) {
								collectList.clear();
								refreshCollectBabyData();
							}
							showShortToast("取消收藏成功");
						} else {
							showShortToast("收藏成功");
						}
					} else {
						showShortToast(result.getString("message"));
					}
				} catch (JSONException e) {
					showShortToast(R.string.json_exception);
				}
			} else {
				showShortToast(R.string.connect_server_exception);
			}
		}
	}

	/**
	 * 创建婴儿账户
	 * 
	 * @author Zhoujun
	 * 
	 */
	private class CreatBabyAccountTask extends AsyncTask<Void, Void, JSONObject> {

		private String baby_name;
		private String baby_pass;
		private String patriarch_tel;
		private String gender;
		private String due_date;
		private String born_birthday;
		private String born_weight;
		private String born_height;
		private String born_head;
		private String childbirth_style;
		private String complication_id;
		private String standardStr;

		public CreatBabyAccountTask(String baby_name, String baby_pass, String patriarch_tel, String gender,
				String due_date, String born_birthday, String born_weight, String born_height, String born_head,
				String childbirth_style, String complication_id, String standardStr) {
			super();
			this.baby_name = baby_name;
			this.baby_pass = baby_pass;
			this.patriarch_tel = patriarch_tel;
			this.gender = gender;
			this.due_date = due_date;
			this.born_birthday = born_birthday;
			this.born_weight = born_weight;
			this.born_height = born_height;
			this.born_head = born_head;
			this.childbirth_style = childbirth_style;
			this.complication_id = complication_id;
			this.standardStr = standardStr;
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			showPd("正在提交中...");
		}

		@Override
		protected JSONObject doInBackground(Void... params) {
			int doctor_id = SharedPrefUtil.getUid(DoctorHomeActivity.this);
			try {
				return new BusinessHelper().creatBabyAccount(doctor_id, baby_name, baby_pass, patriarch_tel, gender,
						due_date, born_birthday, born_weight, born_height, born_head, childbirth_style,
						complication_id, standardStr);
			} catch (SystemException e) {
				return null;
			}
		}

		@Override
		protected void onPostExecute(JSONObject result) {
			super.onPostExecute(result);
			dismissPd();
			if (result != null) {
				try {
					int status = result.getInt("code");
					if (status == Constants.REQUEST_SUCCESS) {
						showShortToast("创建婴儿账户成功");
					} else {
						showShortToast(result.getString("message"));
					}
				} catch (JSONException e) {
					showShortToast(R.string.json_exception);
				}
			} else {
				showShortToast(R.string.connect_server_exception);
			}
		}
	}

}
