package com.valiantgaming.authserver.database.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor
public class ServerInfo implements Serializable
{
    private int serverID;

    private String serverName;

    private String publicIP;

    private String publicPort;

    private String ipv4;

    private String ipv4Port;

    private String localIP;

    private String localPort;

    private boolean publicEnabled;

    private boolean ipv4Enabled;

    private boolean localEnabled;

    private LocalDateTime createDate;

    private LocalDateTime modifiedDate;

    @Override
    public String toString() {
        return "ServerInfo{" +
                "serverID=" + serverID +
                ", serverName='" + serverName + '\'' +
                ", publicIP='" + publicIP + '\'' +
                ", publicPort=" + publicPort +
                ", ipv4='" + ipv4 + '\'' +
                ", ipv4Port=" + ipv4Port +
                ", localIP='" + localIP + '\'' +
                ", localPort=" + localPort +
                ", publicEnabled=" + publicEnabled +
                ", ipv4Enabled=" + ipv4Enabled +
                ", localEnabled=" + localEnabled +
                ", createDate=" + createDate +
                ", modifiedDate=" + modifiedDate +
                '}';
    }
}