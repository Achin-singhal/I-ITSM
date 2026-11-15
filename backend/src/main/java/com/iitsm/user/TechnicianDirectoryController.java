package com.iitsm.user;

import com.iitsm.assignment.service.AssignmentService;
import com.iitsm.common.ApiResponse;
import com.iitsm.report.dto.TechnicianDirectoryResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
public class TechnicianDirectoryController {
    private final AssignmentService assignments;
    public TechnicianDirectoryController(AssignmentService assignments){this.assignments=assignments;}
    @GetMapping("/technicians") @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<TechnicianDirectoryResponse>> technicians(){return ApiResponse.ok(assignments.technicianDirectory());}
}
