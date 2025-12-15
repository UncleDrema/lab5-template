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
import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("/privilege")
public class PrivilegeController {
    private final PrivilegeService privilegeService;
    private final RestTemplate restTemplate;
    private final OAuth2ResourceServerProperties oAuthProps;

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
        var userInfo = restTemplate.exchange(URI.create(userInfoUrl), HttpMethod.GET, new HttpEntity<>(new HttpHeaders()), Map.class);

        return (String) userInfo.getBody().get("name");
    }
}
