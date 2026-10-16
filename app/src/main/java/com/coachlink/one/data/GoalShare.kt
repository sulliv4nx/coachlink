package com.coachlink.one.data

import android.util.Base64
import org.json.JSONObject

/** Deliberate, user-mediated offline exchange: never exports meals or body measurements. */
object GoalShare {
    private const val PREFIX = "COACHLINK_ONE_GOAL_V1:"
    fun encode(p: Profile): String {
        val json = JSONObject().put("n",p.name.take(60)).put("o",p.objective)
            .put("w",p.goalWeightKg).put("d",p.goalDate)
        return PREFIX + Base64.encodeToString(json.toString().toByteArray(Charsets.UTF_8),Base64.NO_WRAP or Base64.URL_SAFE)
    }
    fun decode(input: String): FriendGoal? = runCatching {
        val code = input.substringAfter(PREFIX,missingDelimiterValue="").trim().takeWhile { !it.isWhitespace() }
        require(code.isNotEmpty() && code.length<2048)
        val json = JSONObject(String(Base64.decode(code,Base64.NO_WRAP or Base64.URL_SAFE),Charsets.UTF_8))
        val name = json.getString("n").trim()
        val objective = json.getString("o")
        val weight = json.getDouble("w")
        val date = json.getString("d")
        require(name.length in 2..60 && objective in listOf("LOSS","GAIN","MAINTAIN") && weight in 25.0..350.0)
        java.time.LocalDate.parse(date)
        FriendGoal(name=name,objective=objective,targetKg=weight,goalDate=date)
    }.getOrNull()
}
