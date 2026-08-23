package com.valiantgaming.launcher.network.session;

import io.netty.channel.ChannelHandlerContext;
import lombok.Getter;
import lombok.Setter;

/**
 * State for the launcher's single outbound connection to AuthServer.
 *
 * <p>Deliberately not the shared {@code commons.network.session.ClientSession} - that class
 * models a game client's session (account/character selection) once it's logged in, which
 * doesn't apply to the launcher's own version-check handshake. See {@code auth-server}'s own
 * (currently stub) {@code network.session.client.ClientSession} for the same per-module
 * split.
 */
@Getter @Setter
public class ClientSession
{
    private ChannelHandlerContext ctx;

    /** Set once {@code A2L_ansReady} is received. Format/length unconfirmed - see Protocol. */
    private byte[] teaKey;

    /**
     * Whether {@code teaKey} has been received and packet encryption should be considered
     * active. {@link com.valiantgaming.commons.security.crypt.TEA} isn't implemented yet, so
     * this only gates logging/intent for now - the encoder/decoder don't actually encrypt.
     */
    private boolean messageCryptEnabled;
}
