package com.example.fcalarmapp

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.fcalarmapp.databinding.ActivityMainBinding
import java.util.*
import kotlin.math.min

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        changeOnOffButton()
        changeAlarmTimeButton()

        val model = fetchDataFromSharedPreferences()
        renderView(model)

    }

    private fun changeAlarmTimeButton(){
        binding.changeAlarmTimeBt.setOnClickListener {
            val calendar = Calendar.getInstance()
            TimePickerDialog(this,{ picker, hour, minute ->

                val model = saveAlarmModel(hour, minute,false)
                renderView(model)
                cancelAlarm()
            },calendar.get(Calendar.HOUR_OF_DAY),calendar.get(Calendar.MINUTE), false).show()
        }
    }

    private fun changeOnOffButton(){
        binding.changeOnOffBt.setOnClickListener {
            val model = it.tag as? AlarmDisplayModel?: return@setOnClickListener
            val newModel = saveAlarmModel(model.hour,model.min,model.onOff.not())
            renderView(newModel)

            if (newModel.onOff){
                //알람 등록
                val calendar = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY,newModel.hour)
                    set(Calendar.MINUTE,newModel.min)
                    if (before(Calendar.getInstance())){
                        add(Calendar.DATE,1)
                    }
                }

                val alarmManager  = getSystemService(Context.ALARM_SERVICE) as AlarmManager
                val intent = Intent(this,AlarmReceiver::class.java)
                val pendingIntent = PendingIntent.getBroadcast(
                    this,
                    ALARM_REQUEST_CODE,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT)

                alarmManager.setInexactRepeating(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    AlarmManager.INTERVAL_DAY,
                    pendingIntent
                )
            } else {
                //알람 제거
                cancelAlarm()
            }
        }
    }


    private fun saveAlarmModel(
        hour: Int,
        minute: Int,
        onOff: Boolean
    ):AlarmDisplayModel {
        val model = AlarmDisplayModel(
            hour,minute,onOff
        )

        val sharedPreferences = getSharedPreferences("time",Context.MODE_PRIVATE)

        with(sharedPreferences.edit()){
            putString("ALARM_KEY",model.makeDataForDB())
            putBoolean("onOff_KEY",model.onOff)
            commit()
        }


        return model
    }

    private fun cancelAlarm(){
        val pendingIntent = PendingIntent.getBroadcast(this,
            ALARM_REQUEST_CODE,
            Intent(this,AlarmReceiver::class.java),
            PendingIntent.FLAG_NO_CREATE)
        pendingIntent?.cancel()
    }

    private fun fetchDataFromSharedPreferences(): AlarmDisplayModel{
        val sharedPreferences = getSharedPreferences(SHARED_PREFERENCES_NAME, MODE_PRIVATE)

        val timeDBValue = sharedPreferences.getString(ALARM_KEY,"9:30") ?: "09:30"
        val onOffDBValue = sharedPreferences.getBoolean(onOff_KEY,false)
        val alarmData = timeDBValue.split(":")
        val alarmModel = AlarmDisplayModel(
            hour = alarmData[0].toInt(),
            min = alarmData[1].toInt(),
            onOff = onOffDBValue
        )

        val pendingIntent = PendingIntent.getBroadcast(this,
            ALARM_REQUEST_CODE,
            Intent(this,AlarmReceiver::class.java),
            PendingIntent.FLAG_NO_CREATE)

        if ((pendingIntent == null) and alarmModel.onOff){
            //알람은 꺼져있는데 데이터는 켜져있음
            alarmModel.onOff = false
        }else if((pendingIntent != null) and alarmModel.onOff.not()){
            //알림은 켜져있는데, 데이터는 꺼져있음
            // 알람 취소
            pendingIntent.cancel()
        }

        return alarmModel
    }

    private fun renderView(model: AlarmDisplayModel){
        binding.amPmTv.text = model.amPmText
        binding.timeTv.text = model.timeText
        binding.changeOnOffBt.text = model.onOffText
        binding.changeOnOffBt.tag = model
    }

    companion object{
        private const val SHARED_PREFERENCES_NAME = "time"
        private const val ALARM_KEY = "alarm"
        private const val onOff_KEY = "onOff"
        private const val ALARM_REQUEST_CODE = 1000
    }
}