package ru.uncledrema.tickets.web;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.OAuth2ResourceServerProperties;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import ru.uncledrema.tickets.dto.BoughtTicketDto;
import ru.uncledrema.tickets.dto.BuyTicketDto;
import ru.uncledrema.tickets.dto.TicketDto;
import ru.uncledrema.tickets.services.TicketService;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("/tickets")
public class TicketsController {
    private final TicketService ticketService;
    private final Mapper mapper;
    private final RestTemplate restTemplate;
    private final OAuth2ResourceServerProperties oAuthProps;

    @GetMapping
    public ResponseEntity<List<TicketDto>> getAllForUser() {
        String username = getUsernameFromUserInfo();
        var tickets = ticketService.findAllByUsername(username);
        return ResponseEntity.ok(tickets.stream().map(mapper::toDto).toList());
    }

    @PostMapping
    public ResponseEntity<BoughtTicketDto> buyTicket(@RequestBody BuyTicketDto buyTicketDto) {
        String username = getUsernameFromUserInfo();
        var boughtTicket = ticketService.buyTicket(buyTicketDto.flightNumber(), buyTicketDto.price(), buyTicketDto.paidFromBalance(), username);
        return ResponseEntity.ok(boughtTicket);
    }

    @GetMapping("/{ticketUid}")
    public ResponseEntity<TicketDto> getByUid(@PathVariable UUID ticketUid) {
        String username = getUsernameFromUserInfo();
        var ticket = ticketService.findByTicketUid(username, ticketUid);
        return ResponseEntity.ok(mapper.toDto(ticket));
    }

    @DeleteMapping("/{ticketUid}")
    public ResponseEntity<Void> cancelByUid(@PathVariable UUID ticketUid) {
        String username = getUsernameFromUserInfo();
        ticketService.cancelTicket(username, ticketUid);
        return ResponseEntity.noContent().build();
    }

    private String getUsernameFromUserInfo() {
        String userInfoUrl = oAuthProps.getJwt().getIssuerUri() + "userinfo";
        var userInfo = restTemplate.exchange(URI.create(userInfoUrl), HttpMethod.GET, new HttpEntity<>(new HttpHeaders()), Map.class);

        return (String) userInfo.getBody().get("name");
    }
}
