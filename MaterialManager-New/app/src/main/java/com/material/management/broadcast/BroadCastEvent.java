package com.material.management.broadcast;

public class BroadCastEvent {
    public static final int BROADCAST_EVENT_NONE = -1;
    public static final int BROADCAST_EVENT_TYPE_LOC_UPDATE = 0;
    public static final int BROADCAST_EVENT_TYPE_RESOLVE_CONNECTION_REQUEST = 1;
    public static final int BROADCAST_EVENT_TYPE_BACKUP_RESTORE_PROGRESS_UPDATE = 2;
    public static final int BROADCAST_EVENT_TYPE_BACKUP_RESTORE_FINISHED = 3;

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
