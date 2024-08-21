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
public class ChannelInfo
{
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "ChannelID")
    private int channelID;

    @NotBlank
    @Column(name = "GameServerID")
    private int gameServerID;

    @NotBlank
    @Column(name = "Name")
    private String channelName;

    @NotBlank
    @Column(name = "IpAddress")
    private String ipAddress;

    @NotBlank
    @Column(name = "Port")
    private short port;

    @NotBlank
    @Column(name = "CreateDate")
    private LocalDateTime createDate;

    @Column(name = "ModifiedDate")
    private LocalDateTime modifiedDate;

    @Override
    public String toString()
    {
        return "ChannelInfo{" +
                "channelID=" + channelID +
                ", gameServerID=" + gameServerID +
                ", channelName='" + channelName + '\'' +
                ", ipAddress='" + ipAddress + '\'' +
                ", port=" + port +
                ", createDate=" + createDate +
                ", modifiedDate=" + modifiedDate +
                '}';
    }
}