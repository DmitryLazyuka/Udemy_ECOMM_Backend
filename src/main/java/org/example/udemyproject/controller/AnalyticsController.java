package org.example.udemyproject.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.example.udemyproject.payload.AnalyticsResponse;
import org.example.udemyproject.service.AnalyticsService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@Tag(name = "Analytics", description = "Administrative analytics endpoints")
@SecurityRequirement(name = "bearer")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/admin/app/analytics")
    @Operation(summary = "Get application analytics", description = "Returns aggregate application metrics for the admin dashboard.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Analytics returned successfully",
                    content = @Content(schema = @Schema(implementation = AnalyticsResponse.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required")
    })
    public ResponseEntity<AnalyticsResponse> getAnalytics(){
        AnalyticsResponse response = analyticsService.getAnalyticsData();
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
