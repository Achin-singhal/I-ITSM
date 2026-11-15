package com.iitsm.incident;
import com.iitsm.incident.entity.*;
import com.iitsm.intelligence.priority.PriorityDecision;
import com.iitsm.user.entity.Role;
import com.iitsm.user.entity.User;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class IncidentLifecycleTest {
 private Incident incident(){var i=new Incident("Service down","Service unavailable","SOFTWARE",new User("e@x.test","hash","Employee",Role.EMPLOYEE),ImpactLevel.HIGH,UrgencyLevel.HIGH,4,100,"Core",4);i.classify(new PriorityDecision(80,IncidentPriority.CRITICAL,"test"));return i;}
 @Test void permitsFullLifecycle(){var i=incident();var tech=new User("t@x.test","hash","Tech",Role.TECHNICIAN);i.assignTo(tech);i.acknowledge(tech);i.transitionTo(IncidentStatus.IN_PROGRESS);i.resolveWith("Restarted service");i.transitionTo(IncidentStatus.VERIFIED);i.close();assertEquals(IncidentStatus.CLOSED,i.getStatus());assertNotNull(i.getResolvedAt());assertEquals("Restarted service",i.getResolutionDetails());}
 @Test void rejectsSkippingAndUnauthorizedAcknowledgement(){var i=incident();assertThrows(IllegalStateException.class,()->i.close());var tech=new User("t@x.test","hash","Tech",Role.TECHNICIAN);i.assignTo(tech);assertThrows(IllegalStateException.class,()->i.acknowledge(new User("x@x.test","hash","Other",Role.TECHNICIAN)));assertThrows(IllegalStateException.class,()->i.transitionTo(IncidentStatus.RESOLVED));}
 @Test void rejectsAssignmentAfterClosure(){var i=incident();var tech=new User("t@x.test","hash","Tech",Role.TECHNICIAN);i.assignTo(tech);i.acknowledge(tech);i.transitionTo(IncidentStatus.IN_PROGRESS);i.resolveWith("Restored");i.transitionTo(IncidentStatus.VERIFIED);i.close();assertThrows(IllegalStateException.class,()->i.assignTo(new User("n@x.test","hash","New Tech",Role.TECHNICIAN)));}
 @Test void assignsLegacyOpenIncidentDirectlyToAssigned(){var i=incident();try{var field=Incident.class.getDeclaredField("status");field.setAccessible(true);field.set(i,IncidentStatus.OPEN);}catch(ReflectiveOperationException ex){throw new AssertionError(ex);}i.assignTo(new User("t@x.test","hash","Tech",Role.TECHNICIAN));assertEquals(IncidentStatus.ASSIGNED,i.getStatus());}
}
