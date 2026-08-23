package com.valiantgaming.databaseserver.database.entity.server;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

@NamedStoredProcedureQueries
({
    @NamedStoredProcedureQuery
    (
        name = "GetServerInfo",
        procedureName = "GetServerInfo",
        resultClasses = ServerInfo.class,
        parameters =
        {
            @StoredProcedureParameter
            (
                name = "isAllServers",
                type = boolean.class,
                mode = ParameterMode.IN
            ),
            @StoredProcedureParameter
            (
                name = "serverName",
                type = String.class,
                mode = ParameterMode.IN
            )
        }
    )
})
@Table @Entity
@Getter @Setter
@NoArgsConstructor
public class ServerInfo implements Serializable
{
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "ServerID")
    private int serverID;

    @NotBlank
    @Column(name = "Name")
    private String serverName;

    @Column(name = "PublicIP")
    private String publicIP;

    @Column(name = "PublicPort")
    private String publicPort;

    @Column(name = "IPv4")
    private String ipv4;

    @Column(name = "IPv4Port")
    private String ipv4Port;

    @Column(name = "LocalIP")
    private String localIP;

    @Column(name = "LocalPort")
    private String localPort;

    @NotBlank
    @Column(name = "IsPublicEnabled")
    private boolean publicEnabled;

    @NotBlank
    @Column(name = "IsIPv4Enabled")
    private boolean ipv4Enabled;

    @NotBlank
    @Column(name = "IsLocalEnabled")
    private boolean localEnabled;

    @NotBlank
    @Column(name = "CreateDate")
    private LocalDateTime createDate;

    @Column(name = "ModifiedDate")
    private LocalDateTime modifiedDate;

    @Override
    public String toString() {
        return "ServerInfo{" +
                "serverID=" + serverID +
                ", serverName='" + serverName + '\'' +
                ", publicIP='" + publicIP + '\'' +
                ", publicPort='" + publicPort + '\'' +
                ", ipv4='" + ipv4 + '\'' +
                ", ipv4Port='" + ipv4Port + '\'' +
                ", localIP='" + localIP + '\'' +
                ", localPort='" + localPort + '\'' +
                ", publicEnabled=" + publicEnabled +
                ", ipv4Enabled=" + ipv4Enabled +
                ", localEnabled=" + localEnabled +
                ", createDate=" + createDate +
                ", modifiedDate=" + modifiedDate +
                '}';
    }
}