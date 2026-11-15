package com.iitsm.assignment.strategy;
import com.iitsm.assignment.entity.TechnicianProfile;
import com.iitsm.assignment.service.TechnicianCandidate;
import com.iitsm.incident.entity.*;
import com.iitsm.user.entity.*;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class AssignmentStrategyTest {
 private final Incident incident=new Incident("Network down","VPN unavailable",IncidentPriority.HIGH,"NETWORK",new User("e@x.test","hash","Employee",Role.EMPLOYEE));
 private TechnicianProfile profile(long id,String email,boolean available,int years,int max,Set<String> skills){User u=new User(email,"hash",email,Role.TECHNICIAN);u.setId(id);return new TechnicianProfile(u,available,years,max,skills);}
 @Test void skillStrategyMatchesCategoryAndPrefersExperienceAfterEligibility(){
  var unskilled=new TechnicianCandidate(profile(1,"a@x.test",true,20,10,Set.of("DATABASE")),0);
  var lessExperienced=new TechnicianCandidate(profile(2,"b@x.test",true,2,10,Set.of("NETWORK")),0);
  var expert=new TechnicianCandidate(profile(3,"c@x.test",true,8,10,Set.of("NETWORK")),0);
  assertEquals(3,new SkillBasedAssignmentStrategy().recommend(incident,List.of(unskilled,lessExperienced,expert)).orElseThrow().getUser().getId());
 }
 @Test void loadStrategyUsesActualWorkloadFiltersAvailabilityAndCapacityAndBreaksTiesById(){
  var lowButUnavailable=new TechnicianCandidate(profile(1,"a@x.test",false,8,10,Set.of("NETWORK")),0);
  var atCapacity=new TechnicianCandidate(profile(2,"b@x.test",true,20,2,Set.of("NETWORK")),2);
  var tieHigherId=new TechnicianCandidate(profile(4,"d@x.test",true,3,10,Set.of("NETWORK")),1);
  var tieLowerId=new TechnicianCandidate(profile(3,"c@x.test",true,3,10,Set.of("NETWORK")),1);
  assertEquals(3,new WorkloadAssignmentStrategy().recommend(incident,List.of(lowButUnavailable,atCapacity,tieHigherId,tieLowerId)).orElseThrow().getUser().getId());
  assertTrue(new WorkloadAssignmentStrategy().recommend(incident,List.of(lowButUnavailable,atCapacity)).isEmpty());
 }
 @Test void rankedWorkloadRecommendationsPreferSkillMatchAndExplainCapacity(){
  var matching=new TechnicianCandidate(profile(6,"skill@x.test",true,2,5,Set.of("NETWORK")),2);
  var experiencedButUnmatched=new TechnicianCandidate(profile(7,"other@x.test",true,15,5,Set.of("DATABASE")),0);
  var unavailable=new TechnicianCandidate(profile(8,"offline@x.test",false,20,5,Set.of("NETWORK")),0);
  var atCapacity=new TechnicianCandidate(profile(9,"full@x.test",true,20,2,Set.of("NETWORK")),2);
  var ranked=new WorkloadAssignmentStrategy().rank(incident,List.of(experiencedButUnmatched,matching,unavailable,atCapacity));
  assertEquals(List.of(6L,7L),ranked.stream().map(c->c.profile().getUser().getId()).toList());
 }
}
