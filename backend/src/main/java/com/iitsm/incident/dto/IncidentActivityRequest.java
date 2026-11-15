package com.iitsm.incident.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
public class IncidentActivityRequest {
 @NotBlank @Pattern(regexp="INVESTIGATION_NOTE|INFORMATION_REQUEST|EMPLOYEE_RESPONSE") private String activityType;
 @NotBlank @Size(max=4000) private String message;
 public String getActivityType(){return activityType;} public void setActivityType(String v){activityType=v;}
 public String getMessage(){return message;} public void setMessage(String v){message=v;}
}
