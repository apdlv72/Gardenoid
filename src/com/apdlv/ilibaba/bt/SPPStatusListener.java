package com.apdlv.ilibaba.bt;

import com.apdlv.ilibaba.bt.SPPDataHandler.Device;

public interface SPPStatusListener 
{
    void onConnect(Device device);
    void onDisconnect(Device device);
    void setStatus(boolean connected);
}