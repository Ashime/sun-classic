package com.valiantgaming.webserver.web;

import com.valiantgaming.webserver.network.PendingCreateAccountRequests;
import com.valiantgaming.webserver.network.packet.server.AskCreateAccount;
import com.valiantgaming.webserver.network.session.server.ServerSessionManager;
import io.netty.channel.Channel;
import jakarta.validation.Valid;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Log4j2
@RestController
public class RegistrationController
{
    @PostMapping("/api/register")
    public ResponseEntity<Map<String, String>> register(@Valid @RequestBody RegisterAccountRequest request)
    {
        Channel dbChannel = ServerSessionManager.getChannel();

        if(dbChannel == null || !dbChannel.isActive())
        {
            log.error("No active connection to Database Server - cannot register {}", request.username());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("message", "Registration is temporarily unavailable. Please try again shortly."));
        }

        CompletableFuture<String> future = new CompletableFuture<>();
        int requestId = PendingCreateAccountRequests.register(future);

        dbChannel.writeAndFlush(new AskCreateAccount().createPacket(requestId, request));

        try
        {
            String message = future.get(10, TimeUnit.SECONDS);
            HttpStatus status = message.equals("SUCCESS!") ? HttpStatus.CREATED : HttpStatus.CONFLICT;

            return ResponseEntity.status(status).body(Map.of("message", message));
        }
        catch(TimeoutException e)
        {
            PendingCreateAccountRequests.cancel(requestId);
            log.error("Timed out waiting for CreateAccount response for {}", request.username());

            return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT)
                    .body(Map.of("message", "Registration timed out. Please try again."));
        }
        catch(Exception e)
        {
            log.error("Unexpected error while registering {}", request.username(), e);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Registration failed due to an unexpected error."));
        }
    }
}
