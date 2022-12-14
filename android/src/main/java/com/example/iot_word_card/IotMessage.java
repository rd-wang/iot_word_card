package com.example.iot_word_card;

import com.google.gson.Gson;

import java.util.Map;

public class IotMessage {
    public IotMessage(int code) {
        this.code = code;
    }

    public IotMessage(int code, String desc, Message message) {
        this.code = code;
        this.desc = desc;
        this.message = message;
    }

    public IotMessage(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public IotMessage(int code, int subCode, String desc) {
        this.code = code;
        this.subCode = subCode;
        this.desc = desc;
    }

    public IotMessage(int code, Message message) {
        this.code = code;
        this.message = message;
    }


    int code;
    int subCode;
    String desc;
    Message message;

    public String toJson() {
        return new Gson().toJson(this);
    }
}

