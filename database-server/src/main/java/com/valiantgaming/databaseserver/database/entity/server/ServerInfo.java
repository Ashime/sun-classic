package com.valiantgaming.databaseserver.database.entity.server;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Table @Entity
@Getter @Setter
@NoArgsConstructor
public class ServerInfo
{
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
    private short publicPort;

    @Column(name = "IPv4")
    private String ipv4;

    @Column(name = "IPv4Port")
    private short ipv4Port;

    @Column(name = "LocalIP")
    private String localIP;

    @Column(name = "LocalPort")
    private short localPort;

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
}