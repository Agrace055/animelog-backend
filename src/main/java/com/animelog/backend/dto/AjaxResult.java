package com.animelog.backend.dto;

import java.util.HashMap;
import java.util.Objects;

/**
 * 统一返回结果类
 */
public class AjaxResult extends HashMap<String, Object> {
    private static final long serialVersionUID = 1L;

    /** 状态码 */
    public static final String CODE_TAG = "code";

    /** 返回内容 */
    public static final String MSG_TAG = "msg";

    /** 数据对象 */
    public static final String DATA_TAG = "data";

    /**
     * 初始化一个新创建的 AjaxResult 对象，使其表示一个空消息。
     */
    public AjaxResult() {
    }

    /**
     * 初始化一个新创建的 AjaxResult 对象
     *
     * @param code 状态码
     * @param msg 返回内容
     */
    public AjaxResult(int code, String msg) {
        super.put(CODE_TAG, code);
        super.put(MSG_TAG, msg);
    }

    /**
     * 初始化一个新创建的 AjaxResult 对象
     *
     * @param code 状态码
     * @param msg 返回内容
     * @param data 数据对象
     */
    public AjaxResult(int code, String msg, Object data) {
        super.put(CODE_TAG, code);
        super.put(MSG_TAG, msg);
        super.put(DATA_TAG, data);
    }

    /**
     * 返回成功消息（默认200）
     *
     * @return 成功消息
     */
    public static AjaxResult success() {
        return AjaxResult.success("操作成功");
    }

    /**
     * 返回成功数据（默认200）
     *
     * @return 成功消息
     */
    public static AjaxResult success(Object data) {
        return AjaxResult.success("操作成功", data);
    }

    /**
     * 返回成功消息（默认200）
     *
     * @param msg 返回内容
     * @return 成功消息
     */
    public static AjaxResult success(String msg) {
        return AjaxResult.success(msg, null);
    }

    /**
     * 返回成功消息（默认200）
     *
     * @param msg 返回内容
     * @param data 数据对象
     * @return 成功消息
     */
    public static AjaxResult success(String msg, Object data) {
        return new AjaxResult(HttpStatus.SUCCESS, msg, data);
    }

    /**
     * 返回成功消息（自定义状态码）
     *
     * @param code 状态码
     * @param msg 返回内容
     * @return 成功消息
     */
    public static AjaxResult success(int code, String msg) {
        return new AjaxResult(code, msg, null);
    }

    /**
     * 返回成功消息（自定义状态码）
     *
     * @param code 状态码
     * @param msg 返回内容
     * @param data 数据对象
     * @return 成功消息
     */
    public static AjaxResult success(int code, String msg, Object data) {
        return new AjaxResult(code, msg, data);
    }

    /**
     * 返回错误消息（必须指定状态码）
     *
     * @param code 状态码
     * @param msg 返回内容
     * @return 错误消息
     */
    public static AjaxResult error(int code, String msg) {
        return new AjaxResult(code, msg, null);
    }

    /**
     * 返回错误消息（必须指定状态码）
     *
     * @param code 状态码
     * @param msg 返回内容
     * @param data 数据对象
     * @return 错误消息
     */
    public static AjaxResult error(int code, String msg, Object data) {
        return new AjaxResult(code, msg, data);
    }

    /**
     * 是否为成功消息
     *
     * @return 结果
     */
    public boolean isSuccess() {
        // 通过 code 与 SUCCESS 常量比较判断是否成功
        return Objects.equals(this.get(CODE_TAG), HttpStatus.SUCCESS);
    }

    /**
     * 是否为错误消息
     *
     * @return 结果
     */
    public boolean isError() {
        // 错误定义为非成功状态
        return !isSuccess();
    }

    /**
     * 方便链式调用
     *
     * @param key 键
     * @param value 值
     * @return 数据对象
     */
    @Override
    public AjaxResult put(String key, Object value) {
        // 1) 保持 HashMap 原始写入行为
        super.put(key, value);

        // 2) 返回自身支持链式调用
        return this;
    }
    
    /**
     * 获取状态码
     * 
     * @return 状态码
     */
    public Integer getCode() {
        return (Integer) this.get(CODE_TAG);
    }
    
    /**
     * 获取消息
     * 
     * @return 消息
     */
    public String getMsg() {
        return (String) this.get(MSG_TAG);
    }
    
    /**
     * 获取数据对象
     * 
     * @param <T> 数据类型
     * @return 数据对象
     */
    @SuppressWarnings("unchecked")
    public <T> T getData() {
        return (T) this.get(DATA_TAG);
    }
    
    /**
     * 获取数据对象（带类型检查）
     * 
     * @param <T> 数据类型
     * @param clazz 数据类型Class
     * @return 数据对象
     */
    @SuppressWarnings("unchecked")
    public <T> T getData(Class<T> clazz) {
        // 1) 读取 data 字段原始值
        Object data = this.get(DATA_TAG);

        // 2) 类型匹配才返回，避免 ClassCastException
        if (data != null && clazz.isInstance(data)) {
            return (T) data;
        }

        // 3) 类型不匹配或为空时返回 null
        return null;
    }

}
