package com.valiantgaming.databaseserver.network.server.service;

public interface MessageService
{
    byte[] processMessage(byte[] message);
}