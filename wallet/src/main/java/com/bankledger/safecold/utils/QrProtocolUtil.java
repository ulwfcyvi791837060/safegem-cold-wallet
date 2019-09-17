package com.bankledger.safecold.utils;

import com.bankledger.protobuf.bean.CoinBalance;
import com.bankledger.protobuf.bean.CommonMsg;
import com.bankledger.protobuf.bean.EthToken;
import com.bankledger.protobuf.bean.TransBalance;
import com.bankledger.protobuf.bean.TransDate;
import com.bankledger.protobuf.bean.TransRetEos;
import com.bankledger.protobuf.bean.TransSignParam;
import com.bankledger.protobuf.utils.ProtoUtils;
import com.bankledger.safecold.R;
import com.bankledger.safecold.SafeColdApplication;
import com.bankledger.safecoldj.Currency;
import com.bankledger.safecoldj.SafeColdSettings;
import com.bankledger.safecoldj.core.HDAddressManager;
import com.bankledger.safecoldj.entity.UriDecode;
import com.bankledger.safecoldj.qrcode.QRCodeUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * $desc
 *
 * @author bankledger
 * @time 2018/11/5 20:07
 */
public class QrProtocolUtil {
    private static final Logger log = LoggerFactory.getLogger(QrProtocolUtil.class);

    private IBalanceDecode mIBalanceDecode;
    private IDateDecode mIDateDecode;
    private IActiveEthTokenDecode mIActiveEthTokenDecode;
    private IDecodeFail mIDecodeFail;
    private ICoinAddressDecode mICoinAddressDecode;
    private ISignParam mISignParam;
    private IRetEos mIRetEos;

    public final void decode(String content) {
        CommonMsg msg = ProtoUtils.decodeCommonMsg(content);
        if (msg != null) {//内部协议解析
            if (msg.isEnable()) {//协议版本可用
                if (mIBalanceDecode != null && msg.getHeaderType() == ProtoUtils.HeaderType.TYPE_BALANCE) {
                    TransBalance transBalance = ProtoUtils.decodeBalance(msg.body);
                    if (transBalance != null) {
                        if (WalletInfoManager.checkWalletNumber(transBalance.walletSeqNumber)) {
                            mIBalanceDecode.onBalanceDecode(transBalance.coinBalance);
                        } else {
                            exeDecodeFail(R.string.synchronous_balance_err);
                        }
                    } else {
                        exeDecodeFail(R.string.unidentifiable_qr_code);
                    }
                    return;
                }
                if (mIDateDecode != null && msg.getHeaderType() == ProtoUtils.HeaderType.TYPE_DATE) {
                    TransDate transDate = ProtoUtils.decodeDate(msg.body);
                    if (transDate != null) {
                        mIDateDecode.onDateDecode(transDate);
                    } else {
                        exeDecodeFail(R.string.unidentifiable_qr_code);
                    }
                    return;
                }
                if (mIActiveEthTokenDecode != null && msg.getHeaderType() == ProtoUtils.HeaderType.TYPE_ACTIVE) {
                    EthToken ethToken = ProtoUtils.decodeActiveERC20(msg.body);
                    if (ethToken != null) {
                        mIActiveEthTokenDecode.onActiveEthTokenDecode(ethToken);
                    } else {
                        exeDecodeFail(R.string.unidentifiable_qr_code);
                    }
                    return;
                }
                if (mISignParam != null && msg.getHeaderType() == ProtoUtils.HeaderType.TYPE_EOSSP) {
                    TransSignParam signParam = ProtoUtils.decodeEosSignParam(msg.body);
                    if (signParam != null) {
                        mISignParam.onSignParamDecode(signParam);
                    } else {
                        exeDecodeFail(R.string.unidentifiable_qr_code);
                    }
                    return;
                }
                if (mIRetEos != null && msg.getHeaderType() == ProtoUtils.HeaderType.TYPE_RETEOS) {
                    TransRetEos retEos = ProtoUtils.decodeRetEos(msg.body);
                    if (retEos != null) {
                        if (WalletInfoManager.checkWalletNumber(retEos.walletSeqNumber)) {
                            mIRetEos.onRetEos(retEos);
                        } else {
                            exeDecodeFail(R.string.eos_ret_info_err);
                        }
                    } else {
                        exeDecodeFail(R.string.unidentifiable_qr_code);
                    }
                    return;
                }
                exeDecodeFail(R.string.unidentifiable_qr_code);
            } else if (mIDecodeFail != null) {//协议版本不可用
                if (msg.checkLocalProtocolUpdate()) {
                    mIDecodeFail.onProtocolUpgrade(true);
                } else {
                    mIDecodeFail.onProtocolUpgrade(false);
                }
            } else {
                exeDecodeFail(R.string.unidentifiable_qr_code);
            }
        } else if (mICoinAddressDecode != null) {
            try {
                UriDecode uriDecode = QRCodeUtil.decodeUri(content);
                if (uriDecode == null) {
                    exeDecodeFail(R.string.unidentifiable_qr_code);
                } else {
                    mICoinAddressDecode.onCoinAddressDecode(uriDecode);
                }
            } catch (Exception e) {
                e.printStackTrace();
                exeDecodeFail(R.string.unidentifiable_qr_code);
            }
        } else {
            exeDecodeFail(R.string.unidentifiable_qr_code);
        }
    }

    private void exeDecodeFail(int failMsgR) {
        if (mIDecodeFail != null) {
            mIDecodeFail.onQrDecodeFail(SafeColdApplication.mContext.getString(failMsgR));
        }
    }

    public QrProtocolUtil setICoinAddressDecode(ICoinAddressDecode iCoinAddressDecode) {
        this.mICoinAddressDecode = iCoinAddressDecode;
        return this;
    }

    public QrProtocolUtil setIBalanceDecode(IBalanceDecode iBalanceDecode) {
        this.mIBalanceDecode = iBalanceDecode;
        return this;
    }

    public QrProtocolUtil setIDateDecode(IDateDecode iDateDecode) {
        this.mIDateDecode = iDateDecode;
        return this;
    }

    public QrProtocolUtil setIActiveEthTokenDecode(IActiveEthTokenDecode iActiveEthTokenDecode) {
        this.mIActiveEthTokenDecode = iActiveEthTokenDecode;
        return this;
    }

    public QrProtocolUtil setIDecodeFail(IDecodeFail iDecodeFail) {
        this.mIDecodeFail = iDecodeFail;
        return this;
    }

    public QrProtocolUtil setISignParamDecode(ISignParam mISignParam) {
        this.mISignParam = mISignParam;
        return this;
    }

    public QrProtocolUtil setRetEos(IRetEos iRetEos) {
        this.mIRetEos = iRetEos;
        return this;
    }

    //########### 提供interface ###########

    public interface ICoinAddressDecode {
        void onCoinAddressDecode(UriDecode uriDecode);
    }

    public interface IBalanceDecode {
        void onBalanceDecode(List<CoinBalance> coinBalanceList);
    }

    public interface IDateDecode {
        void onDateDecode(TransDate transDate);
    }

    public interface IActiveEthTokenDecode {
        void onActiveEthTokenDecode(EthToken ethToken);
    }

    public interface IDecodeFail {
        void onQrDecodeFail(String errMsg);

        void onProtocolUpgrade(boolean isSelf);
    }

    public interface IRetEos {
        void onRetEos(TransRetEos uriDecode);
    }

    public interface ISignParam {
        void onSignParamDecode(TransSignParam uriDecode);
    }
}
