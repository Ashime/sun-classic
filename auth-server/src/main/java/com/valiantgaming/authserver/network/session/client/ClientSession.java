package com.valiantgaming.authserver.network.session.client;

import io.netty.channel.ChannelHandlerContext;
import lombok.Getter;
import lombok.Setter;

/**
 * State for one game client's connection to AuthServer, from {@code L2A_askUnknown1}/
 * {@code A2U_ansReady} through to server-list selection. Distinct from the S2S
 * {@code ServerSession} (which tracks RSA/AES key material for the database-server
 * connection) - this only needs a single TEA key, per the {@code Protocol} flow comment.
 */
@Getter @Setter
public class ClientSession
{
    private ChannelHandlerContext ctx;

    /** Set once {@code A2U_ansReady} is sent. See {@code AnsReady}'s class comment. */
    private byte[] teaKey;

    /**
     * Whether {@code teaKey} has been handed out. {@link com.valiantgaming.commons.security.crypt.TEA}
     * is only used to decrypt the password field in {@code U2A_askAuthUser}, not whole
     * packets, so this doesn't gate general packet decryption - see
     * {@code ClientPacketDecoder}/{@code Encoder}'s class comments.
     */
    private boolean messageCryptEnabled;

    /** Set true once {@code U2A_askAuthUser} succeeds. Gates {@code U2A_askSrvList}/{@code askSrvSelect}. */
    private boolean authenticated;

    /** Username this session authenticated as, once {@link #authenticated} is true. */
    private String username;
}
