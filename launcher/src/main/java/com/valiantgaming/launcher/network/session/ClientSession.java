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
 * {@code network.session.client.ClientSession} for the same per-module split.
 */
@Getter @Setter
public class ClientSession
{
    private ChannelHandlerContext ctx;

    /**
     * Set once {@code A2U_ansReady} is received - the packet AuthServer sends unconditionally
     * to every new connection, before {@code A2L_ansReady} (which carries no payload - see
     * {@code GetReady}'s class comment). Used to TEA-encrypt the password field of
     * {@code U2A_askAuthUser} - see {@code AskAuthUser}/{@code LoginController}.
     *
     * <p>{@code volatile}: written on the Netty event-loop thread when the reply arrives, read
     * on the FX Application Thread when the user submits the login form - without this, the
     * write has no guaranteed visibility to the FX thread (a plain field can appear to stay
     * null there indefinitely even after the Netty thread has set it).
     */
    private volatile byte[] teaKey;

    /**
     * Whether {@code teaKey} has been received. {@link com.valiantgaming.commons.security.crypt.TEA}
     * is only used to decrypt a password field, not whole packets, so this doesn't gate
     * general packet decryption for this connection.
     */
    private volatile boolean messageCryptEnabled;

    /**
     * Set true once {@code A2U_ansAuthUser} comes back accepted - mirrors the same pair of
     * fields on auth-server's own {@code network.session.client.ClientSession}. Written from
     * {@code LoginController} on the JavaFX thread and read there again by
     * {@code LauncherController} once the login modal closes, hence {@code volatile}.
     */
    private volatile boolean authenticated;

    /** Username this session authenticated as, once {@link #authenticated} is true. */
    private volatile String username;

    /**
     * Password this session authenticated with, held only to hand to the game client on
     * START GAME (see {@code GameClientLauncher}).
     *
     * <p><b>Stopgap.</b> The real client expects a short-lived authorization token from the
     * Webzen Web Starter rather than the account password - see {@code Protocol}'s header note.
     * Keeping the password in memory is what fills that gap today; drop this field as soon as
     * auth-server can issue a proper handoff token. It is deliberately never logged.
     */
    private volatile String password;
}
