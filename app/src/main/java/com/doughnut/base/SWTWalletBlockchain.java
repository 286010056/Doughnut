package com.doughnut.base;

import android.content.Context;
import android.text.TextUtils;

import com.doughnut.config.Constant;
import com.doughnut.net.api.GetWalletTokenList;
import com.doughnut.net.api.jtrequest.JTTransactionDetailsRequest;
import com.doughnut.net.api.jtrequest.JTTransactionListRequest;
import com.doughnut.net.api.jtrequest.JTTransactionRequest;
import com.doughnut.net.load.RequestPresenter;
import com.doughnut.utils.FileUtil;
import com.doughnut.utils.GsonUtil;
import com.doughnut.utils.Util;


public class SWTWalletBlockchain implements BaseWalletUtil {

    private final static String TAG = "SWTWalletBlockchain";

    @Override
    public void init() {
    }

    @Override
    public void createWallet(final String walletName, final String walletPassword, int blockType, final WCallback callback) {
        if (!checkInit(callback)) {
            return;
        }
        GsonUtil json = new GsonUtil("{}");
        json.putInt("blockType", blockType);
        JSUtil.getInstance().callJS("createJtWallet", json, callback);
    }

    @Override
    public void importWallet(String privateKey, int blockType, int type, WCallback callback) {
        if (!checkInit(callback)) {
            return;
        }
        GsonUtil json = new GsonUtil("{}");
        json.putInt("blockType", blockType);

        if (type == 1) {
            json.putString("words", privateKey);
            JSUtil.getInstance().callJS("importWalletWithWords", json, callback);
        } else if (type == 2) {
            json.putString("privateKey", privateKey);
            JSUtil.getInstance().callJS("retrieveWalletFromPk", json, callback);
        }
    }

    @Override
    public void toIban(String ethAddress, WCallback callback) {
    }

    @Override
    public void fromIban(String ibanAddress, WCallback callback) {
    }

    @Override
    public void gasPrice(WCallback callback) {
        double gasPrice = 1.0f;
        GsonUtil gasPriceJson = new GsonUtil("{}");
        gasPriceJson.putDouble("gasPrice", gasPrice);
        callback.onGetWResult(0, gasPriceJson);
    }

    @Override
    public void signedTransaction(GsonUtil data, WCallback callback) {
        if (!checkInit(callback)) {
            return;
        }
        JSUtil.getInstance().callJS("jtSingtx", data, callback);
    }

    @Override
    public void sendSignedTransaction(String rawTransaction, final WCallback callback) {
        new RequestPresenter().loadJcData(new JTTransactionRequest(rawTransaction), new RequestPresenter.RequestCallback() {
            @Override
            public void onRequesResult(int ret, GsonUtil json) {
                if (ret == 0) {
                    callback.onGetWResult(0, json);
                    return;
                }
                callback.onGetWResult(ret, json);
            }
        });
    }

    @Override
    public boolean isWalletLegal(String pk, String address) {
        //TODO, 这里再做严格限制
        if (!TextUtils.isEmpty(pk) && !TextUtils.isEmpty(address)) {
            return true;
        }
        return false;
    }

    @Override
    public void generateReceiveAddress(String walletAddress, double amount, String token, WCallback callback) {
        if (TextUtils.isEmpty(walletAddress) || TextUtils.isEmpty(token)) {
            callback.onGetWResult(-1, new GsonUtil("{}"));
            return;
        }
        final double tmpAmount = amount < 0 ? 0.0f : amount;
        final GsonUtil address = new GsonUtil("{}");
        String receiveStr = String.format("jingtum:%s?amount=%f&token=%s", walletAddress, tmpAmount, token);
        address.putString("receiveAddress", receiveStr);
        callback.onGetWResult(0, address);
    }

    @Override
    public void calculateGasInToken(double gas, double gasPrice, boolean defaultToken, WCallback callback) {
        GsonUtil gasJson = new GsonUtil("{}");
        gasJson.putString("gas", "0.01 SWT");
        gasJson.putDouble("gasPrice", 0.01f);
        callback.onGetWResult(0, gasJson);
    }

    @Override
    public void gasSetting(Context context, double gasPrice, boolean defaultToken, WCallback callback) {
        GsonUtil gas = new GsonUtil("{}");
        gas.putString("gas", "0.01 SWT");
        gas.putDouble("gasPrice", 0.01f);
        callback.onGetWResult(0, gas);
    }

    @Override
    public double getRecommendGas(double gas, boolean defaultToken) {
        return 0.01;
    }

    @Override
    public String getDefaultTokenSymbol() {
        return "SWT";
    }

    @Override
    public int getDefaultDecimal() {
        return 0;
    }

    @Override
    public void getTokenInfo(String token, long blockChainId, WCallback callback) {

    }

    @Override
    public void translateAddress(String sourceAddress, WCallback callback) {
        GsonUtil addressJson = new GsonUtil("{}");
        if (TextUtils.isEmpty(sourceAddress)) {
            sourceAddress = "";
        }
        addressJson.putString("receive_address", sourceAddress);
        callback.onGetWResult(0, addressJson);
    }

    @Override
    public boolean checkWalletAddress(String receiveAddress) {
        if (TextUtils.isEmpty(receiveAddress) || receiveAddress.length() != 34) {
            return false;
        }
        return true;
    }

    @Override
    public boolean checkWalletPk(String privateKey) {
        //todo
        return true;
    }

    @Override
    public void queryTransactionDetails(String hash, final WCallback callback) {
        if (TextUtils.isEmpty(hash)) {
            callback.onGetWResult(-1, new GsonUtil("{}"));
            return;
        }
        new RequestPresenter().loadJtData(new JTTransactionDetailsRequest(hash), new RequestPresenter.RequestCallback() {
            @Override
            public void onRequesResult(int ret, GsonUtil json) {
                if (ret == 0) {
                    GsonUtil extraJson = new GsonUtil("{}");
                    GsonUtil translatedData = new GsonUtil("{}");
                    translatedData.putLong("timeStamp", json.getLong("date", 0l));
                    translatedData.putString("input", "");
                    translatedData.putString("hash", json.getString("hash", ""));
                    translatedData.putDouble("fee", json.getDouble("fee", 0.0f));
                    translatedData.putDouble("real_value", json.getObject("amount", "{}").getDouble("value", 0.0f));
                    translatedData.putString("tokenSymbol", json.getObject("amount", "{}").getString("currency", ""));
                    int status = 5; //未知状态S
                    if (TextUtils.equals(json.getString("result", ""), "tesSUCCESS")) {
                        status = 1;
                    }
                    translatedData.putInt("txreceipt_status", status);
                    String type = json.getString("type", "");
                    if (TextUtils.equals(type, "sent")) {
                        translatedData.putString("from", WalletInfoManager.getInstance().getWAddress());
                        translatedData.putString("to", json.getString("counterparty", ""));
                    } else if (TextUtils.equals(type, "received")) {
                        translatedData.putString("to", WalletInfoManager.getInstance().getWAddress());
                        translatedData.putString("from", json.getString("counterparty", ""));
                    }
                    extraJson.put("data", translatedData);
                    callback.onGetWResult(0, extraJson);
                } else {
                    callback.onGetWResult(ret, json);
                }
            }
        });
    }

    @Override
    public void queryTransactionList(final GsonUtil params, final WCallback callback) {
        int pagesize = params.getInt("pagesize", 10);
        String token = params.getString("token", "");
        String marker = params.getString("marker", "");
        new RequestPresenter().loadJtData(new JTTransactionListRequest(pagesize, token, marker), new RequestPresenter.RequestCallback() {
            @Override
            public void onRequesResult(int ret, GsonUtil json) {
                if (ret == 0) {
                    GsonUtil translatedData = new GsonUtil("{}");
                    GsonUtil dataList = new GsonUtil("[]");
                    GsonUtil payments = json.getArray("payments", "[]");
                    int len = payments.getLength();
                    for (int i = 0; i < len; i++) {
                        GsonUtil payment = payments.getObject(i, "{}");
                        GsonUtil item = new GsonUtil("{}");
                        item.putDouble("fee", payment.getDouble("fee", 0.0f));
                        item.putString("hash", payment.getString("hash", ""));
                        item.putString("tokenSymbol", payment.getObject("amount", "{}").getString("currency", ""));
                        item.putString("real_value", payment.getObject("amount", "{}").getString("value", ""));
                        item.putLong("timeStamp", payment.getLong("date", 0l));
                        String type = payment.getString("type", "");
                        if (TextUtils.equals(type, "sent")) {
                            item.putString("from", WalletInfoManager.getInstance().getWAddress());
                            item.putString("to", payment.getString("counterparty", ""));
                            dataList.add(item);
                        } else if (TextUtils.equals(type, "received")) {
                            item.putString("to", WalletInfoManager.getInstance().getWAddress());
                            item.putString("from", payment.getString("counterparty", ""));
                            dataList.add(item);
                        }
                        //todo 对于type 类型未知，暂不加入记录列表，以免引起困惑
                    }
                    translatedData.put("data", dataList);
                    translatedData.putString("marker", json.getObject("marker", "{}").toString());
                    callback.onGetWResult(0, translatedData);
                } else {
                    callback.onGetWResult(-1, new GsonUtil("{}"));
                }
            }
        });


    }

    @Override
    public double getValue(int decimal, double originValue) {

        if (decimal <= 0) {
            decimal = getDefaultDecimal();
        }
        return Util.formatDouble(3, Util.translateValue(decimal, originValue));
    }

    @Override
    public void queryBalance(String address, int type, final WCallback callback) {
        new RequestPresenter().loadJcData(new GetWalletTokenList(address, type), new RequestPresenter.RequestCallback() {
            @Override
            public void onRequesResult(int ret, GsonUtil json) {
                GsonUtil formatData = new GsonUtil("{}");
                GsonUtil arrays = new GsonUtil("[]");

                if (ret == 0) {
                    GsonUtil datas = json.getArray("data");
                    int len = datas.getLength();
                    for (int i = 0; i < len; i++) {
                        GsonUtil data = new GsonUtil("{}");
                        data.putLong("blockchain_id", Long.parseLong("" + TBController.SWT_INDEX));
                        data.putString("icon_url", "http://state.jingtum.com/favicon.ico");
                        data.putString("bl_symbol", datas.getObject(i).getString("currency", ""));
                        data.putInt("decimal", 0);
                        data.putString("balance", datas.getObject(i).getString("value", "0"));
                        data.putString("asset", "0");
                        arrays.put(i, data);
                    }
                } else {
                    GsonUtil data = new GsonUtil("{}");
                    data.putLong("blockchain_id", Long.parseLong("" + TBController.SWT_INDEX));
                    data.putString("icon_url", "http://state.jingtum.com/favicon.ico");
                    data.putString("bl_symbol", "SWT");
                    data.putInt("decimal", 0);
                    data.putString("balance", "0");
                    data.putString("asset", "0");
                    arrays.put(data);
                }
                formatData.put("data", arrays);
                callback.onGetWResult(0, formatData);
            }
        });
    }

    @Override
    public String getTransactionSearchUrl(String hash) {
        return Constant.swt_transaction_search_url + hash;
    }

    @Override
    public GsonUtil loadTransferTokens(Context context) {
        String data = FileUtil.getConfigFile(context, "jingtumTokens.json");
        return new GsonUtil(data);
    }

    private boolean checkInit(WCallback callback) {
        return JSUtil.getInstance().checkInit(callback);
    }
}
