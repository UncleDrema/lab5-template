package ru.uncledrema.privileges.web;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.OAuth2ResourceServerProperties;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import ru.uncledrema.privileges.dto.BalanceOperationDto;
import ru.uncledrema.privileges.dto.PrivilegeHistoryItemDto;
import ru.uncledrema.privileges.dto.PrivilegeInfoDto;
import ru.uncledrema.privileges.services.PrivilegeService;
import ru.uncledrema.privileges.types.Privilege;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("/privilege")
public class PrivilegeController {
    private final PrivilegeService privilegeService;
    private final RestTemplate restTemplate;
    private final OAuth2ResourceServerProperties oAuthProps;
    // Локальный кэш userinfo: Authorization -> name
    private final Map<String, String> userInfoCache = new ConcurrentHashMap<>();

    @GetMapping
    public ResponseEntity<PrivilegeInfoDto> getPrivilege() {
        String username = getUsernameFromUserInfo();
        Privilege privilege = privilegeService.getPrivilegeByUsername(username);

        var dto = mapToDto(privilege);

        return ResponseEntity.ok(dto);
    }

    @PostMapping("/withdraw")
    public ResponseEntity<PrivilegeInfoDto> withdraw(
            @RequestBody BalanceOperationDto balanceOperation
    ) {
        String username = getUsernameFromUserInfo();
        if (balanceOperation.amount() <= 0) {
            return ResponseEntity.badRequest().build();
        }

        var privilege = privilegeService.withdraw(username, balanceOperation.ticketUid(), balanceOperation.amount());

        var dto = mapToDto(privilege);

        return ResponseEntity.ok(dto);
    }

    @PostMapping("/deposit")
    public ResponseEntity<PrivilegeInfoDto> deposit(
            @RequestBody BalanceOperationDto balanceOperation
    ) {
        String username = getUsernameFromUserInfo();
        if (balanceOperation.amount() <= 0) {
            return ResponseEntity.badRequest().build();
        }

        var privilege = privilegeService.deposit(username, balanceOperation.ticketUid(), balanceOperation.amount());

        var dto = mapToDto(privilege);

        return ResponseEntity.ok(dto);
    }

    @PostMapping("/cancel/{ticketUid}")
    public ResponseEntity<PrivilegeInfoDto> cancel(
            @PathVariable UUID ticketUid
    ) {
        String username = getUsernameFromUserInfo();
        var privilege = privilegeService.cancel(username, ticketUid);

        var dto = mapToDto(privilege);

        return ResponseEntity.ok(dto);
    }

    private PrivilegeInfoDto mapToDto(Privilege privilege) {
        return new PrivilegeInfoDto(
                privilege.getBalance(),
                privilege.getStatus(),
                privilege.getHistory().stream().map(
                        entry -> new PrivilegeHistoryItemDto(
                                entry.getDatetime(),
                                entry.getTicketUid(),
                                entry.getBalanceDiff(),
                                entry.getOperationType()
                        )
                ).toList()
        );
    }

    private String getUsernameFromUserInfo() {
        String userInfoUrl = oAuthProps.getJwt().getIssuerUri() + "userinfo";
        String authHeader = null;
        try {
            var attrs = org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
            if (attrs instanceof org.springframework.web.context.request.ServletRequestAttributes sra) {
                authHeader = sra.getRequest().getHeader("Authorization");
            }
        } catch (Exception ignored) {
        }

        if (authHeader != null) {
            String cached = userInfoCache.get(authHeader);
            if (cached != null) return cached;
        }

        var userInfo = restTemplate.exchange(URI.create(userInfoUrl), HttpMethod.GET, HttpEntity.EMPTY, Map.class);
        String name = (String) userInfo.getBody().get("name");
        if (authHeader != null && name != null) userInfoCache.put(authHeader, name);
        return name;
    }
}
