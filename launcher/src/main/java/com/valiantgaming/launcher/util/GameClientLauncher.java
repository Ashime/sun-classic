package com.valiantgaming.launcher.util;

import lombok.extern.log4j.Log4j2;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

/**
 * Starts the game client executable with the credentials of the account that just logged in.
 *
 * <p>The argument form ({@code -User:<name> -Password:<token>}) comes from the capture noted in
 * {@code Protocol}'s header comment, which found that adding those two flags to a Sungame.exe
 * shortcut is enough to get the client to launch.
 *
 * <p><b>Known gap:</b> that same note records that the real client also expects an authorization
 * token issued by the Webzen Web Starter, which this stack has no equivalent for yet - so the
 * account's own password is passed through in the token's place. Revisit once auth-server can
 * issue a real handoff token (see {@code LauncherController#onStartGame}).
 */
@Log4j2
public final class GameClientLauncher
{
    /** Windows {@code CreateProcess} status for "this executable's manifest demands admin rights". */
    private static final String ELEVATION_REQUIRED = "error=740";

    private GameClientLauncher()
    {
    }

    /**
     * @param clientPath absolute path to the client executable (Launcher.ini {@code [CLIENT] PATH})
     * @throws FileNotFoundException if {@code clientPath} doesn't point at an existing file, so the
     *                               caller can tell a misconfigured path apart from a failed start
     */
    public static void launch(String clientPath, String username, String password) throws IOException
    {
        if(clientPath == null || clientPath.isBlank())
            throw new FileNotFoundException("No client path configured - set [CLIENT] PATH in Config/Launcher/Launcher.ini");

        File executable = new File(clientPath);

        if(!executable.isFile())
            throw new FileNotFoundException("Game client not found at: " + executable.getAbsolutePath());

        List<String> arguments = List.of(
                "-User:" + username,
                "-Password:" + (password == null ? "" : password));

        // The client resolves Data/, GameData.dat and 3dsetup.ini relative to its working
        // directory, so it has to start in its own folder rather than inheriting the launcher's.
        File workingDirectory = executable.getParentFile();

        try
        {
            ProcessBuilder builder = new ProcessBuilder();
            builder.command().add(executable.getAbsolutePath());
            builder.command().addAll(arguments);
            builder.directory(workingDirectory);

            builder.start();
        }
        catch(IOException e)
        {
            // Sungame.exe ships a manifest requiring administrator rights, and CreateProcess (what
            // ProcessBuilder uses) cannot elevate - it just fails with error 740. Only ShellExecute
            // can raise the UAC prompt, so fall back to it via PowerShell's Start-Process -Verb RunAs.
            // Tried direct-first so an already-elevated launcher never triggers a needless prompt.
            if(e.getMessage() == null || !e.getMessage().contains(ELEVATION_REQUIRED))
                throw e;

            log.info("Game client requires elevation - relaunching through UAC.");
            launchElevated(executable, workingDirectory, arguments);
        }
    }

    private static void launchElevated(File executable, File workingDirectory, List<String> arguments) throws IOException
    {
        StringBuilder argumentList = new StringBuilder();
        for(String argument : arguments)
        {
            if(!argumentList.isEmpty())
                argumentList.append(',');

            argumentList.append(quote(argument));
        }

        // Built entirely from single-quoted PowerShell strings so that the surrounding double
        // quotes Java adds when handing -Command over as one argument can't collide with anything
        // inside it.
        String command = "Start-Process -FilePath " + quote(executable.getAbsolutePath())
                + " -WorkingDirectory " + quote(workingDirectory.getAbsolutePath())
                + " -ArgumentList " + argumentList
                + " -Verb RunAs";

        new ProcessBuilder("powershell.exe", "-NoProfile", "-Command", command).start();
    }

    /** Wraps a value as a PowerShell single-quoted string, where an embedded {@code '} is doubled. */
    private static String quote(String value)
    {
        return "'" + value.replace("'", "''") + "'";
    }
}
