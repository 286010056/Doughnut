package com.doughnut.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.doughnut.R;
import com.doughnut.base.BaseWalletUtil;
import com.doughnut.base.TBController;
import com.doughnut.base.WalletInfoManager;
import com.doughnut.config.AppConfig;
import com.doughnut.utils.FileUtil;
import com.doughnut.utils.GsonUtil;
import com.doughnut.utils.ViewUtil;
import com.doughnut.view.TitleBar;
import com.scwang.smartrefresh.layout.SmartRefreshLayout;
import com.scwang.smartrefresh.layout.api.RefreshLayout;
import com.scwang.smartrefresh.layout.listener.OnLoadMoreListener;
import com.scwang.smartrefresh.layout.listener.OnRefreshListener;
import com.stealthcopter.networktools.Ping;
import com.stealthcopter.networktools.ping.PingResult;
import com.stealthcopter.networktools.ping.PingStats;

import java.math.BigDecimal;


public class JtNodeRecordActivity extends BaseActivity implements
        TitleBar.TitleBarClickListener {

    private SmartRefreshLayout mSmartRefreshLayout;
    private TitleBar mTitleBar;

    private RecyclerView mRecyclerView;
    private TransactionRecordAdapter mAdapter;
    private BaseWalletUtil mWalletUtil;
    private GsonUtil publicNodes;
    final private BigDecimal PING_QUICK = new BigDecimal("80");
    final private BigDecimal PING_LOW = new BigDecimal("160");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_jtnode_record);

        initView();
    }

    @Override
    protected void onResume() {
        super.onResume();
        int blockId = WalletInfoManager.getInstance().getWalletType();
        mWalletUtil = TBController.getInstance().getWalletUtil(blockId);
        if (mWalletUtil == null) {
            this.finish();
            return;
        }
        if (mAdapter != null) {
            mSmartRefreshLayout.setEnableRefresh(true);
        }
        mTitleBar.setTitle(WalletInfoManager.getInstance().getWname());


    }

    @Override
    public void onLeftClick(View view) {
        this.finish();
    }

    @Override
    public void onRightClick(View view) {
        ChangeWalletActivity.startChangeWalletActivity(this);
    }

    @Override
    public void onMiddleClick(View view) {
    }

    private void initView() {

        mTitleBar = findViewById(R.id.title_bar);
        mTitleBar.setLeftDrawable(R.drawable.ic_back);
        mTitleBar.setLeftTextColor(R.color.white);
        mTitleBar.setTitleTextColor(R.color.white);
        mTitleBar.setRightDrawable(R.drawable.ic_changewallet);
        mTitleBar.setBackgroundColor(getResources().getColor(R.color.common_blue));
        mTitleBar.setTitleBarClickListener(this);

        mAdapter = new TransactionRecordAdapter();
        mRecyclerView = findViewById(R.id.view_recycler);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.setAdapter(mAdapter);
        mSmartRefreshLayout = (SmartRefreshLayout) findViewById(R.id.layout_refresh);
        mSmartRefreshLayout.autoRefresh();
        mSmartRefreshLayout.setOnRefreshListener(new OnRefreshListener() {
            @Override
            public void onRefresh(RefreshLayout refreshlayout) {
                refreshlayout.finishRefresh();
                mAdapter.notifyDataSetChanged();
            }
        });
        mSmartRefreshLayout.setOnLoadMoreListener(new OnLoadMoreListener() {
            @Override
            public void onLoadMore(RefreshLayout refreshlayout) {
                refreshlayout.finishLoadMore();
                mAdapter.notifyDataSetChanged();
            }
        });
        getPublicNode();
    }

    public static void startJtNodeRecordActivity(Context context) {
        Intent intent = new Intent(context, JtNodeRecordActivity.class);
        intent.addFlags(context instanceof BaseActivity ? 0 : Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    private boolean isReadyForPullEnd() {
        try {
            int lastVisiblePosition = mRecyclerView.getChildAdapterPosition(
                    mRecyclerView.getChildAt(mRecyclerView.getChildCount() - 1));
            if (lastVisiblePosition >= mRecyclerView.getAdapter().getItemCount() - 1) {
                return mRecyclerView.getChildAt(mRecyclerView.getChildCount() - 1)
                        .getBottom() <= mRecyclerView.getBottom();
            }
        } catch (Throwable e) {
        }

        return false;
    }

    class TransactionRecordAdapter extends RecyclerView.Adapter<TransactionRecordAdapter.VH> {

        class VH extends RecyclerView.ViewHolder {
            RelativeLayout mLayoutItem;
            TextView mTvNodeUrl;
            TextView mTvNodeName;
            TextView mTvNodePing;
            ImageView mImgLoad;
            RadioButton mRadioSelected;

            public VH(View v) {
                super(v);
                mLayoutItem = itemView.findViewById(R.id.layout_item);
                mTvNodeUrl = itemView.findViewById(R.id.tv_node_url);
                mTvNodeName = itemView.findViewById(R.id.tv_node_name);
                mTvNodePing = itemView.findViewById(R.id.tv_ping);
                mImgLoad = itemView.findViewById(R.id.img_ping);
                mRadioSelected = itemView.findViewById(R.id.radio_selected);
                mLayoutItem.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mRadioSelected.setChecked(true);
                    }
                });
            }
        }

        @Override
        public VH onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = ViewUtil.inflatView(parent.getContext(), parent, R.layout.layout_item_node, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(VH holder, int position) {
            if (publicNodes == null || publicNodes.getLength() == 0) {
                return;
            }
            GsonUtil item = publicNodes.getObject(position);
            holder.mTvNodeName.setText(item.getString("name", ""));
            String url = item.getString("node", "");
            holder.mTvNodeUrl.setText("wss://" + item.getString("node", ""));
            new Thread(new Runnable() {
                @Override
                public void run() {
                    String host = url.split(":")[0];
                    Ping.onAddress(host).setTimeOutMillis(1000).setTimes(5).doPing(new Ping.PingListener() {
                        @Override
                        public void onResult(PingResult pingResult) {
                        }

                        @Override
                        public void onFinished(PingStats pingStats) {
                            AppConfig.postOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    String ping = String.format("%.2f", pingStats.getAverageTimeTaken());
                                    holder.mTvNodePing.setText(ping + "ms");
                                    BigDecimal pingBig = new BigDecimal(ping);
                                    if (pingBig.compareTo(PING_QUICK) == -1) {
                                        holder.mTvNodePing.setTextColor(getResources().getColor(R.color.color_ping_quick));
                                    } else if (pingBig.compareTo(PING_LOW) == -1) {
                                        holder.mTvNodePing.setTextColor(getResources().getColor(R.color.color_ping_normal));
                                    } else {
                                        holder.mTvNodePing.setTextColor(getResources().getColor(R.color.color_ping_low));
                                    }
                                    holder.mTvNodePing.setVisibility(View.VISIBLE);
                                    holder.mImgLoad.setVisibility(View.GONE);
                                }
                            });
                        }

                        @Override
                        public void onError(Exception e) {
                        }
                    });
                }
            }).start();
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public int getItemCount() {
            return publicNodes.getLength();
        }
    }


    private void getPublicNode() {
        publicNodes = new GsonUtil(FileUtil.getConfigFile(this, "publicNode.json"));
        if (mAdapter != null) {
            mAdapter.notifyDataSetChanged();
        }
    }
}