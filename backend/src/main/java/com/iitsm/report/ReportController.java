package com.iitsm.report;

import com.iitsm.common.ApiResponse;
import com.iitsm.incident.dto.IncidentSearchCriteria;
import com.iitsm.incident.entity.IncidentPriority;
import com.iitsm.incident.entity.IncidentStatus;
import com.iitsm.report.dto.*;
import com.iitsm.report.service.ReportExportService;
import com.iitsm.report.service.ReportService;
import com.iitsm.user.repository.UserRepository;
import com.iitsm.sla.entity.SlaStatus;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1/reports")
public class ReportController {
    private final ReportService reports;
    private final ReportExportService exports;
    private final UserRepository users;
    public ReportController(ReportService reports,ReportExportService exports,UserRepository users){this.reports=reports;this.exports=exports;this.users=users;}

    @GetMapping("/summary") @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<AnalyticsSummaryResponse> summary(@RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME) Instant to){return ApiResponse.ok(reports.getSummary(from,to));}

    @GetMapping("/analytics") @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<ReportAnalyticsResponse> analytics(@RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME) Instant to){return ApiResponse.ok(reports.getAnalytics(from,to));}

    @GetMapping("/technicians") @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<TechnicianMetricsResponse>> technicians(@RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME) Instant to){return ApiResponse.ok(reports.getTechnicianReports(from,to));}

    @GetMapping("/technicians/me") @PreAuthorize("hasRole('TECHNICIAN')")
    public ApiResponse<TechnicianMetricsResponse> myTechnicianReport(Authentication authentication,
            @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME) Instant to){
        Long id=users.findByEmail(authentication.getName()).orElseThrow().getId();
        return ApiResponse.ok(reports.getTechnicianReport(id,from,to).orElseThrow(()->new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND,"Technician profile not found")));
    }

    @GetMapping(value="/exports/incidents.csv",produces="text/csv") @PreAuthorize("hasRole('ADMIN')")
    public StreamingResponseBody exportIncidents(@RequestParam(required=false) Long id,@RequestParam(required=false) String query,
            @RequestParam(required=false) IncidentStatus status,@RequestParam(required=false) IncidentPriority priority,
            @RequestParam(required=false) String category,@RequestParam(required=false) Long assigneeId,
            @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME) Instant createdFrom,
            @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME) Instant createdTo,
            @RequestParam(required=false) SlaStatus slaStatus,HttpServletResponse response){
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=incident-report.csv");response.setContentType("text/csv;charset=UTF-8");
        IncidentSearchCriteria criteria=new IncidentSearchCriteria(id,query,status,priority,category,assigneeId,createdFrom,createdTo,slaStatus);
        return output->exports.writeIncidents(criteria,output);
    }

    @GetMapping(value="/exports/sla.csv",produces="text/csv") @PreAuthorize("hasRole('ADMIN')")
    public StreamingResponseBody exportSla(@RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME) Instant to,HttpServletResponse response){
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=sla-report.csv");response.setContentType("text/csv;charset=UTF-8");
        return output->exports.writeSla(from,to,output);
    }

    @GetMapping(value="/exports/technicians.csv",produces="text/csv") @PreAuthorize("hasRole('ADMIN')")
    public StreamingResponseBody exportTechnicians(@RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME) Instant to,HttpServletResponse response){
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=technician-workload.csv");response.setContentType("text/csv;charset=UTF-8");
        return output->exports.writeTechnicians(from,to,output);
    }
}
