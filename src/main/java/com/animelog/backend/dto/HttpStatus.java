package com.animelog.backend.dto;

/**
 * 返回状态码常量定义。
 */
public final class HttpStatus {

    private HttpStatus() {
    }

    /** 操作成功 */
    public static final int SUCCESS = 200;

    /** 参数错误 */
    public static final int BAD_REQUEST = 400;

    /** 未认证 */
    public static final int UNAUTHORIZED = 401;

    /** 无权限 */
    public static final int FORBIDDEN = 403;

    /** 资源不存在 */
    public static final int NOT_FOUND = 404;

    /** 请求冲突（如重复操作） */
    public static final int CONFLICT = 409;

    /** 参数校验失败 */
    public static final int UNPROCESSABLE_ENTITY = 422;

    /** 服务内部错误 */
    public static final int ERROR = 500;

    /** 服务不可用 */
    public static final int SERVICE_UNAVAILABLE = 503;
}
