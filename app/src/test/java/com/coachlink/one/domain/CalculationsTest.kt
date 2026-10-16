package com.coachlink.one.domain

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class CalculationsTest {
    @Test fun bmiWorks(){ assertEquals(25.0,bmi(81.0,180.0)!!,0.001) }
    @Test fun needsAreConsistent(){ val n=estimateNeeds(80.0,180.0,35,"M",1.55,"LOSS")!!;assertTrue(n.targetKcal<n.maintenanceKcal);assertTrue(n.proteinG>0) }
    @Test fun portionMath(){ val n=toNutrition(200.0,10.0,20.0,5.0,150.0);assertEquals(300.0,n.kcal,0.01);assertEquals(15.0,n.protein,0.01) }
    @Test fun progressCountsBothDirections(){assertEquals(0.5f,progress(100.0,95.0,90.0),0.01f);assertEquals(0.5f,progress(80.0,85.0,90.0),0.01f)}
    @Test fun countdownUsesExactDate(){assertEquals(15L,countdown("2027-01-16",LocalDate.of(2027,1,1)))}
}
