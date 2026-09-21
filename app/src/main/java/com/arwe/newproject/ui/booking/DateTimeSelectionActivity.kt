package com.arwe.newproject.ui.booking

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.arwe.newproject.R
import com.arwe.newproject.databinding.ActivityDateTimeSelectionBinding
import com.arwe.newproject.databinding.ItemCalendarDayBinding
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

class DateTimeSelectionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDateTimeSelectionBinding
    private var selectedDate: LocalDate? = null
    private var selectedTimeSlot: TimeSlot? = null
    private var displayedMonth: YearMonth = YearMonth.now()
    private val calendarDayViews = mutableListOf<Pair<LocalDate, TextView>>()

    private val timeSlotViews: List<Pair<TimeSlot, TextView>> by lazy {
        val slots = DateTimeCatalog.timeSlots
        listOf(
            slots[0] to binding.timeSlot9am.root,
            slots[1] to binding.timeSlot10am.root,
            slots[2] to binding.timeSlot11am.root,
            slots[3] to binding.timeSlot12pm.root,
            slots[4] to binding.timeSlot2pm.root,
            slots[5] to binding.timeSlot3pm.root,
            slots[6] to binding.timeSlot4pm.root,
            slots[7] to binding.timeSlot5pm.root,
            slots[8] to binding.timeSlot6pm.root
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDateTimeSelectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val categoryId = intent.getStringExtra(ServiceCategoryActivity.EXTRA_SELECTED_CATEGORY)
        val productId = intent.getStringExtra(ProductSelectionActivity.EXTRA_SELECTED_PRODUCT)
        val problemId = intent.getStringExtra(ProblemSelectionActivity.EXTRA_SELECTED_PROBLEM)
        val addressId = intent.getStringExtra(AddressSelectionActivity.EXTRA_SELECTED_ADDRESS)
        val addressLabel = intent.getStringExtra(AddressSelectionActivity.EXTRA_SELECTED_ADDRESS_LABEL)
        val addressDetails = intent.getStringExtra(AddressSelectionActivity.EXTRA_SELECTED_ADDRESS_DETAILS)

        binding.backButton.setOnClickListener { finish() }

        binding.monthPrevButton.setOnClickListener {
            if (displayedMonth > YearMonth.now()) {
                displayedMonth = displayedMonth.minusMonths(1)
                renderCalendar()
            }
        }
        binding.monthNextButton.setOnClickListener {
            displayedMonth = displayedMonth.plusMonths(1)
            renderCalendar()
        }
        renderCalendar()

        timeSlotViews.forEach { (slot, view) ->
            view.text = slot.displayLabel
            view.setOnClickListener { selectTime(slot) }
        }
        updateTimeSlotAvailability()

        binding.continueButton.setOnClickListener {
            val date = selectedDate ?: return@setOnClickListener
            val time = selectedTimeSlot ?: return@setOnClickListener
            val intent = Intent(this, ConfirmBookingActivity::class.java)
                .putExtra(ServiceCategoryActivity.EXTRA_SELECTED_CATEGORY, categoryId)
                .putExtra(ProductSelectionActivity.EXTRA_SELECTED_PRODUCT, productId)
                .putExtra(ProblemSelectionActivity.EXTRA_SELECTED_PROBLEM, problemId)
                .putExtra(AddressSelectionActivity.EXTRA_SELECTED_ADDRESS, addressId)
                .putExtra(AddressSelectionActivity.EXTRA_SELECTED_ADDRESS_LABEL, addressLabel)
                .putExtra(AddressSelectionActivity.EXTRA_SELECTED_ADDRESS_DETAILS, addressDetails)
                .putExtra(EXTRA_SELECTED_DATE, DateTimeCatalog.preferredDateValue(date))
                .putExtra(EXTRA_SELECTED_TIME, time.value)
            startActivity(intent)
        }
    }

    /**
     * Rebuilds the 7-column day grid for [displayedMonth] from scratch. Row count is derived
     * from the month's actual length (4-6 rows) rather than a fixed 6, keeping the calendar
     * compact - see the "no unnecessary complex calendar logic" requirement.
     */
    private fun renderCalendar() {
        binding.monthLabel.text = displayedMonth.format(MONTH_LABEL_FORMATTER)
        val canGoBack = displayedMonth > YearMonth.now()
        binding.monthPrevButton.isEnabled = canGoBack
        binding.monthPrevButton.alpha = if (canGoBack) 1f else 0.3f

        binding.calendarGrid.removeAllViews()
        calendarDayViews.clear()

        val firstOfMonth = displayedMonth.atDay(1)
        // DayOfWeek.value is Monday=1..Sunday=7; %7 remaps Sunday to 0 so the grid's first
        // column (Sunday) lines up with the weekday header built in the layout.
        val leadingBlanks = firstOfMonth.dayOfWeek.value % 7
        val daysInMonth = displayedMonth.lengthOfMonth()
        val totalRows = (leadingBlanks + daysInMonth + 6) / 7

        var dayCounter = 1
        for (row in 0 until totalRows) {
            val rowLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }
            for (col in 0 until 7) {
                val cellIndex = row * 7 + col
                val date = if (cellIndex >= leadingBlanks && dayCounter <= daysInMonth) {
                    displayedMonth.atDay(dayCounter).also { dayCounter++ }
                } else {
                    null
                }
                rowLayout.addView(
                    buildDayCell(date),
                    LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                )
            }
            binding.calendarGrid.addView(rowLayout)
        }

        updateCalendarSelectionVisuals()
    }

    private fun buildDayCell(date: LocalDate?): View {
        val cellBinding = ItemCalendarDayBinding.inflate(layoutInflater, binding.calendarGrid, false)
        if (date == null) {
            cellBinding.calendarDayText.text = ""
            cellBinding.calendarDayText.isClickable = false
            cellBinding.calendarDayText.isFocusable = false
        } else {
            cellBinding.calendarDayText.text = date.dayOfMonth.toString()
            val isPast = !DateTimeCatalog.isDateSelectable(date)
            cellBinding.calendarDayText.isClickable = !isPast
            cellBinding.calendarDayText.isFocusable = !isPast
            if (!isPast) {
                cellBinding.calendarDayText.setOnClickListener { selectDate(date) }
            }
            calendarDayViews.add(date to cellBinding.calendarDayText)
        }
        return cellBinding.root
    }

    private fun selectDate(date: LocalDate) {
        selectedDate = date
        updateCalendarSelectionVisuals()
        updateTimeSlotAvailability()
        updateContinueButtonState()
    }

    private fun updateCalendarSelectionVisuals() {
        val today = LocalDate.now()
        calendarDayViews.forEach { (date, view) ->
            when {
                date == selectedDate -> {
                    view.background = ContextCompat.getDrawable(this, R.drawable.bg_calendar_day_selected)
                    view.setTextColor(ContextCompat.getColor(this, R.color.white))
                }
                !DateTimeCatalog.isDateSelectable(date, today) -> {
                    view.background = null
                    view.setTextColor(ContextCompat.getColor(this, R.color.calendar_day_disabled_text))
                }
                else -> {
                    view.background = null
                    view.setTextColor(ContextCompat.getColor(this, R.color.text_primary))
                }
            }
        }
    }

    private fun selectTime(slot: TimeSlot) {
        selectedTimeSlot = slot
        updateSelectedTimeVisualState()
    }

    // Time slots are re-evaluated against the device's current date/time on every
    // date change: a slot is only disabled when the selected date is today AND the
    // slot's time has already passed "now". Any other date leaves all slots enabled.
    private fun updateTimeSlotAvailability() {
        val referenceDate = selectedDate ?: LocalDate.now()

        timeSlotViews.forEach { (slot, view) ->
            val isPast = DateTimeCatalog.isTimeSlotPast(slot, referenceDate)
            view.isEnabled = !isPast
            if (isPast && selectedTimeSlot == slot) {
                selectedTimeSlot = null
            }
        }
        updateSelectedTimeVisualState()
    }

    private fun updateSelectedTimeVisualState() {
        timeSlotViews.forEach { (slot, view) -> view.isSelected = selectedTimeSlot == slot }
        updateContinueButtonState()
    }

    private fun updateContinueButtonState() {
        binding.continueButton.isEnabled = selectedDate != null && selectedTimeSlot != null
    }

    companion object {
        const val EXTRA_SELECTED_DATE = "extra_selected_date"
        const val EXTRA_SELECTED_TIME = "extra_selected_time"
        private val MONTH_LABEL_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)
    }
}
