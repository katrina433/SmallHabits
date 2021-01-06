package com.keepsimple.smallhabits

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.text.InputType
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.children
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.android.synthetic.main.edit_rewards.*
import java.util.*
import kotlin.collections.ArrayList

class EditRewards : AppCompatActivity() {

    private lateinit var gson: Gson
    private lateinit var taskManagementDB: SharedPreferences
    private var credit = 0
    private lateinit var rewards: ArrayList<Pair<String, Int>>
    private lateinit var illegalNameException: Exception
    private lateinit var illegalCreditException: Exception

    override fun onCreate(savedInstanceState: Bundle?) {
        taskManagementDB = getSharedPreferences(
            resources.getString(R.string.task_management_database), Context.MODE_PRIVATE)
        val themeName = taskManagementDB.getString(resources.getString(R.string.theme_key), "BrownTheme")
        val themeId =
            try {
                resources.getIdentifier(themeName, "style", packageName)
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
                0
            }
        setTheme(themeId)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.edit_rewards)

        initVariables()
        displayRewards()

        addReward.setOnClickListener {
            addRewardLayout()
        }

        confirmRewards.setOnClickListener {
            try {
                rewards.clear()
                for (view in rewardsLayout.children) {
                    val rewardCredit =
                        try {
                            ((view as LinearLayout).children.elementAt(0) as EditText).text.toString().toInt()
                        } catch (e: Exception) {
                            throw illegalCreditException
                        }
                    if (rewardCredit <= 0) {
                        throw illegalCreditException
                    }
                    val rewardName = (view.children.elementAt(1) as EditText).text.toString()
                    if (rewardName.isBlank()) {
                        throw illegalNameException
                    }
                    rewards.add(Pair(rewardName, rewardCredit))
                }
                Collections.sort(rewards,
                    kotlin.Comparator { pairOne, pairTwo -> pairOne.second - pairTwo.second })
                taskManagementDB.edit().apply {
                    putString(resources.getString(R.string.rewards_key), gson.toJson(rewards))
                }.apply()
                setResult(Activity.RESULT_OK)
                finish()
            } catch (e: Exception) {
                Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun initVariables() {
        gson = Gson()
        val rewardsJson = taskManagementDB.getString(
            resources.getString(R.string.rewards_key),
            resources.getString(R.string.failed_to_retrieve_rewards)
        )
        credit = taskManagementDB.getInt(
            resources.getString(R.string.credit_key), 0)
        rewards = if (rewardsJson == resources.getString(R.string.failed_to_retrieve_rewards)) {
            ArrayList()
        } else {
            val type = object: TypeToken<ArrayList<Pair<String, Int>>>(){}.type
            gson.fromJson(rewardsJson, type)
        }
        illegalNameException = Exception(resources.getString(R.string.illegal_reward_name_exception))
        illegalCreditException = Exception(resources.getString(R.string.illegal_credit_exception))
    }

    private fun displayRewards() {
        val creditText = "${resources.getString(R.string.current_credit)} $credit"
        currentCreditText.text = creditText
        for (entry in rewards) {
            val inputs = addRewardLayout()
            inputs.first.setText(entry.first)
            inputs.second.setText(entry.second.toString())
        }
    }

    private fun addRewardLayout(): Pair<EditText, EditText> {
        val layout = LinearLayout(this)
        layout.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        layout.orientation = LinearLayout.HORIZONTAL
        val rewardCreditInput = EditText(this)
        rewardCreditInput.inputType = InputType.TYPE_CLASS_NUMBER
        rewardCreditInput.hint = resources.getString(R.string.credit)
        val rewardNameInput = EditText(this)
        rewardNameInput.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1.0f
        )
        rewardNameInput.hint = resources.getString(R.string.reward)
        val deleteRewards = ImageButton(this)
        deleteRewards.setImageDrawable(
            ContextCompat.getDrawable(this, R.drawable.delete)
        )
        layout.addView(rewardCreditInput)
        layout.addView(rewardNameInput)
        layout.addView(deleteRewards)
        rewardsLayout.addView(layout)

        deleteRewards.setOnClickListener {
            rewardsLayout.removeView(layout)
        }
        return Pair(rewardNameInput, rewardCreditInput)
    }
}