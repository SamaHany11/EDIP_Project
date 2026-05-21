package com.example.EDIP.search.strategy;

import com.example.EDIP.Auth.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SearchStrategyFactory {

    private final Map<String, SearchStrategy> strategies;

    public SearchStrategy get(User user) {
        return switch (user.getRole()) {
            case "ADMIN"    -> strategies.get("adminSearchStrategy");
            case "HEAD"     -> strategies.get("headSearchStrategy");
            case "EMPLOYEE" -> strategies.get("employeeSearchStrategy");
            default         -> strategies.get("externalSearchStrategy");
        };
    }
}