package com.example.EDIP.Dashboard.service;

import com.example.EDIP.Auth.model.User;
import com.example.EDIP.Auth.repository.DepartmentRepository;
import com.example.EDIP.Auth.repository.UserRepository;
import com.example.EDIP.Dashboard.dto.*;
import com.example.EDIP.Dashboard.repository.DashboardRepository;
import com.example.EDIP.Dashboard.repository.UserDashboardRepository;
import com.example.EDIP.Dashboard.repository.projection.DeptDocStatsProjection;
import com.example.EDIP.Dashboard.repository.projection.StatusCountProjection;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final DashboardRepository     dashboardRepository;
    private final UserDashboardRepository userDashboardRepository;
    private final DepartmentRepository    departmentRepository;
    private final UserRepository          userRepository;

    private LocalDateTime[] resolveRange(DateRange range, Integer year) {
        if (year != null) {
            return new LocalDateTime[]{
                    LocalDateTime.of(year, 1, 1, 0, 0, 0),
                    LocalDateTime.of(year, 12, 31, 23, 59, 59)
            };
        }
        return switch (range) {
            case LAST_WEEK  -> new LocalDateTime[]{ LocalDateTime.now().minusDays(7),  null };
            case LAST_MONTH -> new LocalDateTime[]{ LocalDateTime.now().minusDays(30), null };
            case ALL        -> new LocalDateTime[]{ null, null };
        };
    }

    private Map<String, Long> toStatusMap(List<StatusCountProjection> projections) {
        return projections.stream()
                .collect(Collectors.toMap(
                        StatusCountProjection::getStatus,
                        StatusCountProjection::getCount));
    }

    public AdminDashboardDTO getAdminDashboard(UUID departmentId,
                                               DateRange range,
                                               SortOrder sort,
                                               Integer year) {

        var department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new RuntimeException("Department not found"));

        LocalDateTime[] r = resolveRange(range, year);
        LocalDateTime from = r[0];
        LocalDateTime to   = r[1];

        Map<String, Long> counts = toStatusMap(
                dashboardRepository.countByStatusForDepartment(departmentId, from, to));

        long submitted  = counts.getOrDefault("SUBMITTED",   0L);
        long inProgress = counts.getOrDefault("IN_PROGRESS", 0L);
        long pending    = counts.getOrDefault("PENDING",     0L);
        long completed  = counts.getOrDefault("COMPLETED",   0L);
        long total      = counts.values().stream().mapToLong(Long::longValue).sum();

        long totalEmployees = userDashboardRepository
                .countByDepartmentIdAndRoleAndIsActiveTrue(departmentId, "EMPLOYEE");

        var rawStats = (sort == SortOrder.BOTTOM)
                ? dashboardRepository.findEmployeeStatsBottom(departmentId, from, to)
                : dashboardRepository.findEmployeeStatsTop(departmentId, from, to);

        List<AdminDashboardDTO.EmployeeStat> employeeStats = rawStats.stream()
                .map(e -> AdminDashboardDTO.EmployeeStat.builder()
                        .userId(e.getUserId())
                        .username(e.getUsername())
                        .email(e.getEmail())
                        .documents(AdminDashboardDTO.EmployeeDocStats.builder()
                                .assigned(coalesce(e.getAssignedDocuments()))
                                .completed(coalesce(e.getCompletedDocuments()))
                                .pending(coalesce(e.getPendingDocuments()))
                                .inProgress(coalesce(e.getInProgressDocuments()))
                                .build())
                        .build())
                .collect(Collectors.toList());

        return AdminDashboardDTO.builder()
                .departmentId(departmentId)
                .departmentName(department.getDepartmentName())
                .generatedAt(LocalDateTime.now().toString())
                .range(year != null ? "YEAR_" + year : range.name())
                .sortOrder(sort.name())
                .documents(AdminDashboardDTO.DocumentStats.builder()
                        .total(total).submitted(submitted)
                        .inProgress(inProgress).pending(pending).completed(completed)
                        .build())
                .employees(AdminDashboardDTO.EmployeeGroup.builder()
                        .total(totalEmployees)
                        .stats(employeeStats)
                        .build())
                .build();
    }

    public HeadDashboardDTO getHeadDashboard(UUID departmentId,
                                             DateRange range,
                                             SortOrder sort,
                                             Integer year) {

        var department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new RuntimeException("Department not found"));

        LocalDateTime[] r = resolveRange(range, year);
        LocalDateTime from = r[0];
        LocalDateTime to   = r[1];

        Map<String, Long> counts = toStatusMap(
                dashboardRepository.countByStatusForDepartment(departmentId, from, to));

        long submitted  = counts.getOrDefault("SUBMITTED",   0L);
        long inProgress = counts.getOrDefault("IN_PROGRESS", 0L);
        long pending    = counts.getOrDefault("PENDING",     0L);
        long completed  = counts.getOrDefault("COMPLETED",   0L);
        long total      = counts.values().stream().mapToLong(Long::longValue).sum();

        var rawStats = (sort == SortOrder.BOTTOM)
                ? dashboardRepository.findEmployeeStatsBottom(departmentId, from, to)
                : dashboardRepository.findEmployeeStatsTop(departmentId, from, to);

        List<HeadDashboardDTO.EmployeeStat> employeeStats = rawStats.stream()
                .map(e -> HeadDashboardDTO.EmployeeStat.builder()
                        .userId(e.getUserId())
                        .username(e.getUsername())
                        .email(e.getEmail())
                        .documents(HeadDashboardDTO.EmployeeDocStats.builder()
                                .assigned(coalesce(e.getAssignedDocuments()))
                                .completed(coalesce(e.getCompletedDocuments()))
                                .pending(coalesce(e.getPendingDocuments()))
                                .inProgress(coalesce(e.getInProgressDocuments()))
                                .build())
                        .build())
                .collect(Collectors.toList());

        return HeadDashboardDTO.builder()
                .departmentId(departmentId)
                .departmentName(department.getDepartmentName())
                .generatedAt(LocalDateTime.now().toString())
                .range(year != null ? "YEAR_" + year : range.name())
                .sortOrder(sort.name())
                .documents(HeadDashboardDTO.DocumentStats.builder()
                        .total(total).submitted(submitted)
                        .inProgress(inProgress).pending(pending).completed(completed)
                        .build())
                .employees(HeadDashboardDTO.EmployeeGroup.builder()
                        .stats(employeeStats)
                        .build())
                .build();
    }

    public EmployeeDashboardDTO getEmployeeDashboard(String email,
                                                     DateRange range,
                                                     Integer year) {

        User employee = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String departmentName = employee.getDepartmentId() != null
                ? departmentRepository.findById(employee.getDepartmentId())
                .map(d -> d.getDepartmentName())
                .orElse("N/A")
                : "N/A";

        LocalDateTime[] r = resolveRange(range, year);
        LocalDateTime from = r[0];
        LocalDateTime to   = r[1];

        Map<String, Long> counts = toStatusMap(
                dashboardRepository.countByStatusForEmployee(employee.getId(), from, to));

        long total      = counts.values().stream().mapToLong(Long::longValue).sum();
        long completed  = counts.getOrDefault("COMPLETED",   0L);
        long pending    = counts.getOrDefault("PENDING",     0L);
        long submitted  = counts.getOrDefault("SUBMITTED",   0L);
        long inProgress = counts.getOrDefault("IN_PROGRESS", 0L);

        return EmployeeDashboardDTO.builder()
                .userId(employee.getId())
                .username(employee.getUsername())
                .departmentName(departmentName)
                .generatedAt(LocalDateTime.now().toString())
                .range(year != null ? "YEAR_" + year : range.name())
                .documents(EmployeeDashboardDTO.DocumentStats.builder()
                        .total(total).completed(completed)
                        .pending(pending).submitted(submitted).inProgress(inProgress)
                        .build())
                .build();
    }

    public DepartmentComparisonDTO getDepartmentComparison(DateRange range,
                                                           SortOrder sort,
                                                           Integer year) {

        LocalDateTime[] r = resolveRange(range, year);
        LocalDateTime from = r[0];
        LocalDateTime to   = r[1];

        Map<UUID, DeptDocStatsProjection> docStatsMap = dashboardRepository
                .countDocsByAllDepartments(from, to)
                .stream()
                .collect(Collectors.toMap(
                        DeptDocStatsProjection::getDepartmentId,
                        p -> p));

        Map<UUID, String> topPerformerMap = new LinkedHashMap<>();
        dashboardRepository.findTopPerformerPerDepartment(from, to)
                .forEach(p -> topPerformerMap
                        .putIfAbsent(p.getDepartmentId(), p.getUsername()));

        List<DepartmentComparisonDTO.DeptStat> deptStats = departmentRepository
                .findAll()
                .stream()
                .map(dept -> {
                    UUID deptId = dept.getDepartmentId();
                    DeptDocStatsProjection docs = docStatsMap.get(deptId);

                    long totalEmployees = userDashboardRepository
                            .countByDepartmentIdAndRoleAndIsActiveTrue(deptId, "EMPLOYEE");

                    return DepartmentComparisonDTO.DeptStat.builder()
                            .departmentId(deptId)
                            .departmentName(dept.getDepartmentName())
                            .totalDocuments(docs == null ? 0L : coalesce(docs.getTotalDocuments()))
                            .submitted(docs      == null ? 0L : coalesce(docs.getSubmitted()))
                            .inProgress(docs     == null ? 0L : coalesce(docs.getInProgress()))
                            .pending(docs        == null ? 0L : coalesce(docs.getPending()))
                            .completed(docs      == null ? 0L : coalesce(docs.getCompleted()))
                            .totalEmployees(totalEmployees)
                            .topPerformer(topPerformerMap.getOrDefault(deptId, "Not Found"))
                            .build();
                })
                .sorted(sort == SortOrder.BOTTOM
                        ? Comparator.comparingLong(DepartmentComparisonDTO.DeptStat::getCompleted)
                        : Comparator.comparingLong(DepartmentComparisonDTO.DeptStat::getCompleted).reversed())
                .collect(Collectors.toList());

        return DepartmentComparisonDTO.builder()
                .generatedAt(LocalDateTime.now().toString())
                .range(year != null ? "YEAR_" + year : range.name())
                .sortOrder(sort.name())
                .totalDepartments(deptStats.size())
                .departments(deptStats)
                .build();
    }

    private long coalesce(Long value) {
        return value == null ? 0L : value;
    }
}