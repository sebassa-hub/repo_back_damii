package com.rutasproyect.damii.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.rutasproyect.damii.model.Report;
import com.rutasproyect.damii.repository.ReportRepository;

@Service
public class ReportService {

    private final ReportRepository reportRepository;

    public ReportService(ReportRepository reportRepository) {
        this.reportRepository = reportRepository;
    }

    public Report createReport(Report report) {
        if (report.getReportTime() == null) {
            report.setReportTime(LocalDateTime.now());
        }
        return reportRepository.save(report);
    }
}
