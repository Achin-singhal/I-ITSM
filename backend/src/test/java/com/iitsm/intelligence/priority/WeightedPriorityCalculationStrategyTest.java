package com.iitsm.intelligence.priority;
import com.iitsm.incident.entity.*;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.core.env.MapPropertySource;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class WeightedPriorityCalculationStrategyTest {
 private final WeightedPriorityCalculationStrategy strategy=new WeightedPriorityCalculationStrategy(80,60,35);
 @Test void assignsP4AtMinimumAndP1AtMaximum(){
  var low=strategy.calculate(new PriorityInput(ImpactLevel.VERY_LOW,UrgencyLevel.LOW,1,0,"Docs",1));
  var high=strategy.calculate(new PriorityInput(ImpactLevel.EXTENSIVE,UrgencyLevel.CRITICAL,5,1000,"Payments",5));
  assertEquals(IncidentPriority.LOW,low.priority());assertEquals(0,low.score());
  assertEquals(IncidentPriority.CRITICAL,high.priority());assertEquals(100,high.score());
 }
 @Test void mapsConfiguredBoundaryScoresToHigherBand(){
  var exact=new WeightedPriorityCalculationStrategy(81,69.5,35).calculate(new PriorityInput(ImpactLevel.HIGH,UrgencyLevel.HIGH,4,250,"Payments",4));
  var below=new WeightedPriorityCalculationStrategy(81,69.51,35).calculate(new PriorityInput(ImpactLevel.HIGH,UrgencyLevel.HIGH,4,250,"Payments",4));
  assertEquals(69.5,exact.score());assertEquals(IncidentPriority.HIGH,exact.priority());assertEquals(IncidentPriority.MEDIUM,below.priority());
 }
 @Test void validatesCriticalityAndUserCount(){
  assertThrows(IllegalArgumentException.class,()->new PriorityInput(ImpactLevel.HIGH,UrgencyLevel.HIGH,6,0,"Service",3));
  assertThrows(IllegalArgumentException.class,()->new PriorityInput(ImpactLevel.HIGH,UrgencyLevel.HIGH,3,-1,"Service",3));
 }
 @Test void rejectsInvalidThresholdOrderAndRangeAtConstruction(){
  assertThrows(IllegalArgumentException.class,()->new WeightedPriorityCalculationStrategy(60,60,35));
  assertThrows(IllegalArgumentException.class,()->new WeightedPriorityCalculationStrategy(101,60,35));
  assertThrows(IllegalArgumentException.class,()->new WeightedPriorityCalculationStrategy(Double.NaN,60,35));
 }
 @Test void rejectsInvalidWeightsAtConstruction(){
  assertThrows(IllegalArgumentException.class,()->new WeightedPriorityCalculationStrategy(80,60,35,.3,.3,.3,.1,.1));
  assertThrows(IllegalArgumentException.class,()->new WeightedPriorityCalculationStrategy(80,60,35,-.1,.4,.3,.2,.2));
 }
 @Test void invalidEnvironmentThresholdsFailSpringStartup(){
  var context=new AnnotationConfigApplicationContext();
  context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("invalid-priority", Map.of(
          "app.priority.thresholds.p1","60", "app.priority.thresholds.p2","60")));
  context.register(WeightedPriorityCalculationStrategy.class);
  assertThrows(BeanCreationException.class,context::refresh);
  context.close();
 }
}
