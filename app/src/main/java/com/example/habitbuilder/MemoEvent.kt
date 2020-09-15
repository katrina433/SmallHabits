package com.example.habitbuilder

import android.graphics.Color
import com.github.sundeepk.compactcalendarview.domain.Event

class MemoEvent(date: Long, val memoList: ArrayList<MemoLog>) : Event(Color.GRAY, date) {}