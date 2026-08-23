package com.valiantgaming.commons.network.session;

public abstract class SessionManager
{
    public abstract void addSession(Object object);
    public abstract Object getSession(Object object);
    public abstract void updateSession(Object object);
    public abstract void removeSession(Object object);
    public abstract void clearSessions();
}