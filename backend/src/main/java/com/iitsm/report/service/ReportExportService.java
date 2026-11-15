package com.iitsm.report.service;

import com.iitsm.assignment.repository.TechnicianProfileRepository;
import com.iitsm.incident.dto.IncidentSearchCriteria;
import com.iitsm.incident.repository.IncidentRepository;
import com.iitsm.report.dto.TechnicianMetricsResponse;
import com.iitsm.sla.repository.SlaTrackingRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.function.Function;

@Service
public class ReportExportService {
    private static final int BATCH_SIZE=500;
    private final IncidentRepository incidents;
    private final SlaTrackingRepository sla;
    private final ReportService reports;

    public ReportExportService(IncidentRepository incidents,SlaTrackingRepository sla,ReportService reports) {
        this.incidents=incidents;this.sla=sla;this.reports=reports;
    }

    @Transactional(readOnly=true)
    public void writeIncidents(IncidentSearchCriteria criteria,OutputStream output) throws IOException {
        IncidentSearchCriteria f=criteria==null?new IncidentSearchCriteria(null,null,null,null,null,null,null,null,null):criteria;
        try(BufferedWriter writer=writer(output)){
            row(writer,"id","ticket_number","title","description","status","priority","category","assignee","created_at");
            int page=0;
            while(true){
                var result=incidents.searchIncidents(f.incidentId(),blank(f.query()),f.status(),f.priority(),blank(f.category()),
                        f.assigneeId(),null,null,f.createdFrom(),f.createdTo(),f.slaStatus(),PageRequest.of(page++,BATCH_SIZE,Sort.by(Sort.Direction.DESC,"createdAt")));
                for(var i:result.getContent()) row(writer,i.getId(),i.getTicketNumber(),i.getTitle(),i.getDescription(),i.getStatus(),i.getPriority(),i.getCategory(),i.getAssignee()==null?"":i.getAssignee().getFullName(),i.getCreatedAt());
                writer.flush();if(!result.hasNext())break;
            }
        }
    }

    @Transactional(readOnly=true)
    public void writeSla(Instant from,Instant to,OutputStream output)throws IOException{
        ReportService.validateRange(from,to);
        try(BufferedWriter writer=writer(output)){
            row(writer,"incident_id","ticket_number","incident_status","priority","policy","sla_status","response_deadline","resolution_deadline","response_warning","resolution_warning","response_breached","resolution_breached");
            int page=0;
            while(true){
                var result=sla.findReportPage(from,to,PageRequest.of(page++,BATCH_SIZE,Sort.by(Sort.Direction.ASC,"id")));
                for(var t:result.getContent())row(writer,t.getIncident().getId(),t.getIncident().getTicketNumber(),t.getIncident().getStatus(),t.getIncident().getPriority(),t.getPolicy().getName(),t.getStatus(),t.getResponseDeadline(),t.getResolutionDeadline(),t.isResponseWarningSent(),t.isResolutionWarningSent(),t.isResponseViolationRecorded(),t.isResolutionViolationRecorded());
                writer.flush();if(!result.hasNext())break;
            }
        }
    }

    @Transactional(readOnly=true)
    public void writeTechnicians(Instant from,Instant to,OutputStream output)throws IOException{
        ReportService.validateRange(from,to);
        try(BufferedWriter writer=writer(output)){
            row(writer,"technician_id","name","email","available","active_workload","max_active_workload","assigned_incidents_in_range","resolved_incidents_in_range","average_resolution_seconds");
            for(TechnicianMetricsResponse t:reports.getTechnicianReports(from,to))row(writer,t.technicianId(),t.name(),t.email(),t.available(),t.activeWorkload(),t.maxActiveWorkload(),t.assignedIncidentCount(),t.resolvedIncidentCount(),t.averageResolutionSeconds());
        }
    }

    private static BufferedWriter writer(OutputStream output){return new BufferedWriter(new OutputStreamWriter(output,StandardCharsets.UTF_8));}
    private static String blank(String value){return value==null||value.isBlank()?null:value.trim();}
    private static void row(BufferedWriter writer,Object... values)throws IOException{
        for(int i=0;i<values.length;i++){if(i>0)writer.write(',');writer.write(csv(values[i]));}
        writer.write("\r\n");
    }
    static String csv(Object value){
        String text=value==null?"":String.valueOf(value);
        String trimmed=text.stripLeading();
        if(!trimmed.isEmpty()&&"=+-@".indexOf(trimmed.charAt(0))>=0)text="'"+text;
        return "\""+text.replace("\"","\"\"")+"\"";
    }
}
