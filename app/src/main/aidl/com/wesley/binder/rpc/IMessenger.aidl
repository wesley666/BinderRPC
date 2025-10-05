// IMessenger.aidl
package com.wesley.binder.rpc;

// Declare any non-default types here with import statements

interface IMessenger {
    void send(in Message msg);
}