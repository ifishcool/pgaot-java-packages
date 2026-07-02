package com.pgaot.account.auth.common.code;

/** 结果码接口 — 所有错误码枚举实现此接口 */
public interface IResultCode {
    int getCode();
    String getMessage();
}
