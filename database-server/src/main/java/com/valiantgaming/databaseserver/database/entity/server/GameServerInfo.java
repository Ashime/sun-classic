package com.valiantgaming.databaseserver.database.entity.server;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Table @Entity
@Getter @Setter
@NoArgsConstructor
public class GameServerInfo
{
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "GameServerID")
    private int gameServerID;

    @NotBlank
    @Column(name = "Name")
    private String serverName;

    @NotBlank
    @Column(name = "IpAddress")
    private String ipAddress;

    @NotBlank
    @Column(name = "Port")
    private short port;

    @NotNull
    @Column(name = "IsPvE")
    private boolean pve;

    @NotNull
    @Column(name = "IsPvP")
    private boolean pvp;

    @NotBlank
    @Column(name = "CreateDate")
    private LocalDateTime createDate;

    @Column(name = "ModifiedDate")
    private LocalDateTime modifiedDate;

    @Override
    public String toString()
    {
        return "GameServerInfo{" +
                "gameServerID=" + gameServerID +
                ", serverName='" + serverName + '\'' +
                ", ipAddress='" + ipAddress + '\'' +
                ", port=" + port +
                ", pve=" + pve +
                ", pvp=" + pvp +
                ", createDate=" + createDate +
                ", modifiedDate=" + modifiedDate +
                '}';
    }
}