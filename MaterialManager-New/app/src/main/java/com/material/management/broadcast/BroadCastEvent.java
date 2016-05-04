package com.material.management.broadcast;

public class BroadCastEvent {

    public static final int BROADCAST_EVENT_TYPE__LOC_UPDATE = 0;

    private int mEventType;
    private Object mData;

    public BroadCastEvent(int eventType, Object data) {
        this.mEventType = eventType;
        this.mData = data;
    }

    public int getEventType() {
        return mEventType;
    }

    public Object getData() {
        return mData;
    }
}
